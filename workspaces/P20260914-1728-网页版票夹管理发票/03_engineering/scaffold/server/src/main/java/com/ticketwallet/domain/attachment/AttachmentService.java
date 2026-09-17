package com.ticketwallet.domain.attachment;

import com.mybatisflex.core.query.QueryWrapper;
import com.ticketwallet.common.error.ApiException;
import com.ticketwallet.common.error.ErrorCode;
import com.ticketwallet.common.security.SecurityConfig.AppPrincipal;
import com.ticketwallet.common.storage.StorageService;
import com.ticketwallet.domain.invoice.Invoice;
import com.ticketwallet.domain.invoice.InvoiceMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 附件服务（api-design §4.3）：三校验（大小/数量/格式含魔数）→ 存储 → 元数据落库。
 * 属主谓词：invoice 属主 + user_id 冗余双保险（architecture §4.3）。
 */
@Service
public class AttachmentService {

    public static final long MAX_SIZE = 10L * 1024 * 1024;
    public static final int MAX_COUNT = 3;
    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp", "pdf");

    private final AttachmentMapper mapper;
    private final InvoiceMapper invoiceMapper;
    private final StorageService storage;

    public AttachmentService(AttachmentMapper mapper, InvoiceMapper invoiceMapper, StorageService storage) {
        this.mapper = mapper;
        this.invoiceMapper = invoiceMapper;
        this.storage = storage;
    }

    @Transactional
    public Attachment upload(AppPrincipal principal, String invoiceId, MultipartFile file) throws IOException {
        String ext = extOf(file.getOriginalFilename());        // 闸1：扩展名白名单
        String mime = sniffMime(file, ext);                     // 闸1：魔数校验（ATT_003）
        if (file.getSize() > MAX_SIZE) {                         // 闸2：大小（ATT_001）
            throw new ApiException(ErrorCode.ATT_001);
        }
        Invoice invoice = invoiceMapper.selectOneByQuery(QueryWrapper.create()
                .where("id = ?", invoiceId)
                .and("user_id = ?", principal.userId())
                .and("deleted_at IS NULL"));
        if (invoice == null) {
            throw new ApiException(ErrorCode.INV_001);
        }
        long count = mapper.selectCountByQuery(QueryWrapper.create().where("invoice_id = ?", invoiceId));
        if (count >= MAX_COUNT) {                                // 闸3：数量（ATT_002）
            throw new ApiException(ErrorCode.ATT_002);
        }
        String key = "%s/%s.%s".formatted(
                OffsetDateTime.now().toString().substring(0, 7).replace("-", ""), UUID.randomUUID(), ext);
        try (InputStream in = file.getInputStream()) {
            storage.put(key, in);
        }
        Attachment att = new Attachment();
        att.setId(UUID.randomUUID().toString());
        att.setInvoiceId(invoiceId);
        att.setUserId(principal.userId());
        att.setFilename(file.getOriginalFilename());
        att.setMimeType(mime);
        att.setSize((int) file.getSize());
        att.setStorageKey(key);
        att.setCreatedAt(OffsetDateTime.now());
        mapper.insert(att);
        return att;
    }

    public List<Attachment> listOwned(AppPrincipal principal, String invoiceId) {
        requireOwnedInvoice(principal, invoiceId);
        return mapper.selectListByQuery(QueryWrapper.create()
                .where("invoice_id = ?", invoiceId).orderBy("created_at", true));
    }

    /** 下载/预览：属主校验（记录属主 + user_id 冗余）。 */
    public Attachment loadOwned(AppPrincipal principal, String attachmentId) {
        Attachment att = mapper.selectOneByQuery(QueryWrapper.create()
                .where("id = ?", attachmentId)
                .and("user_id = ?", principal.userId()));
        if (att == null) {
            throw new ApiException(ErrorCode.INV_001);   // 越权/不存在统一 404（不暴露存在性）
        }
        return att;
    }

    /** 删除单个：存储删文件 + DB 删行；记录与其他附件不受影响（R7 单项失败设计）。 */
    @Transactional
    public void delete(AppPrincipal principal, String attachmentId) {
        Attachment att = loadOwned(principal, attachmentId);
        storage.delete(att.getStorageKey());
        mapper.deleteById(att.getId());
    }

    /** 彻底删除发票时的级联清理（recycle 调用，事务内）。 */
    @Transactional
    public int purgeByInvoice(String invoiceId) {
        List<Attachment> list = mapper.selectListByQuery(
                QueryWrapper.create().where("invoice_id = ?", invoiceId));
        list.forEach(att -> storage.delete(att.getStorageKey()));
        mapper.deleteByQuery(QueryWrapper.create().where("invoice_id = ?", invoiceId));
        return list.size();
    }

    private void requireOwnedInvoice(AppPrincipal principal, String invoiceId) {
        Invoice inv = invoiceMapper.selectOneByQuery(QueryWrapper.create()
                .where("id = ?", invoiceId).and("user_id = ?", principal.userId()));
        if (inv == null) throw new ApiException(ErrorCode.INV_001);
    }

    private String extOf(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new ApiException(ErrorCode.ATT_003);
        }
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new ApiException(ErrorCode.ATT_003);
        }
        return ext;
    }

    /** 扩展名白名单 + 魔数嗅探双校验（tech-stack §3.9）。 */
    private String sniffMime(MultipartFile file, String ext) throws IOException {
        byte[] head = new byte[12];
        try (InputStream in = file.getInputStream()) {
            int n = in.readNBytes(head, 0, 12);
            if (n < 4) throw new ApiException(ErrorCode.ATT_003);
        }
        boolean jpeg = (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF;
        boolean png = (head[0] & 0xFF) == 0x89 && head[1] == 0x50 && head[2] == 0x4E && head[3] == 0x47;
        boolean pdf = head[0] == 0x25 && head[1] == 0x50 && head[2] == 0x44 && head[3] == 0x46;   // %PDF
        boolean webp = head[0] == 0x52 && head[1] == 0x49 && head[2] == 0x46 && head[3] == 0x46   // RIFF
                && head[8] == 0x57 && head[9] == 0x45 && head[10] == 0x42 && head[11] == 0x50;   // WEBP
        if (Set.of("jpg", "jpeg").contains(ext) && jpeg) return "image/jpeg";
        if ("png".equals(ext) && png) return "image/png";
        if ("webp".equals(ext) && webp) return "image/webp";
        if ("pdf".equals(ext) && pdf) return "application/pdf";
        throw new ApiException(ErrorCode.ATT_003);   // 魔数与扩展名不符（改后缀假图）
    }
}
