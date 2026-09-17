package com.ticketwallet.domain.recycle;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.ticketwallet.common.error.ApiException;
import com.ticketwallet.common.error.ErrorCode;
import com.ticketwallet.common.security.SecurityConfig.AppPrincipal;
import com.ticketwallet.domain.attachment.AttachmentService;
import com.ticketwallet.domain.invoice.Invoice;
import com.ticketwallet.domain.invoice.InvoiceMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 回收站（api-design §4.5）：软删/恢复/彻底删 + 每小时到期清理（30 天）。
 * daysLeft = 30 − 已过天数（服务端计算，Asia/Shanghai 业务口径）。
 */
@Service
public class RecycleService {

    private static final Logger log = LoggerFactory.getLogger(RecycleService.class);
    private static final ZoneId BIZ = ZoneId.of("Asia/Shanghai");
    private static final Duration RETENTION = Duration.ofDays(30);

    private final InvoiceMapper invoiceMapper;
    private final AttachmentService attachmentService;

    public RecycleService(InvoiceMapper invoiceMapper, AttachmentService attachmentService) {
        this.invoiceMapper = invoiceMapper;
        this.attachmentService = attachmentService;
    }

    /** 软删除（F06）：置 deletedAt；附件随行保留（恢复后可用）。 */
    @Transactional
    public void softDelete(AppPrincipal principal, String invoiceId) {
        Invoice inv = invoiceMapper.selectOneByQuery(QueryWrapper.create()
                .where("id = ?", invoiceId)
                .and("user_id = ?", principal.userId())
                .and("deleted_at IS NULL"));
        if (inv == null) {
            throw new ApiException(ErrorCode.INV_001);
        }
        OffsetDateTime now = OffsetDateTime.now();
        inv.setDeletedAt(now);
        inv.setUpdatedAt(now);
        invoiceMapper.update(inv);
    }

    public record RecycleItem(Invoice invoice, OffsetDateTime deletedAt, long daysLeft) {}

    public Page<RecycleItem> page(AppPrincipal principal, int page, int pageSize) {
        Page<Invoice> result = invoiceMapper.paginate(page, pageSize, QueryWrapper.create()
                .where("user_id = ?", principal.userId())
                .and("deleted_at IS NOT NULL")
                .orderBy("deleted_at", false));
        List<RecycleItem> items = result.getRecords().stream()
                .map(inv -> new RecycleItem(inv, inv.getDeletedAt(), daysLeft(inv.getDeletedAt())))
                .toList();
        return new Page<>(items, result.getPageNumber(), result.getPageSize(), result.getTotalRow());
    }

    /** 恢复：置空 deletedAt（列表按开票日期排序天然回原位）。 */
    @Transactional
    public void restore(AppPrincipal principal, String invoiceId) {
        Invoice inv = ownedDeleted(principal, invoiceId);
        inv.setDeletedAt(null);
        inv.setUpdatedAt(OffsetDateTime.now());
        invoiceMapper.update(inv, false);   // ignoreNulls=false：显式置空 deleted_at（spike F6）
    }

    /** 彻底删除（二次确认后）：事务删 DB 行（级联附件行）+ 删存储文件；不可恢复。 */
    @Transactional
    public void purge(AppPrincipal principal, String invoiceId) {
        Invoice inv = ownedDeleted(principal, invoiceId);
        attachmentService.purgeByInvoice(invoiceId);
        invoiceMapper.deleteById(inv.getId());
    }

    /** 每小时到期清理（F12）：deletedAt < now−30d 的行物理清除。 */
    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpired() {
        OffsetDateTime threshold = OffsetDateTime.now(BIZ).minus(RETENTION);
        List<Invoice> expired = invoiceMapper.selectListByQuery(QueryWrapper.create()
                .where("deleted_at IS NOT NULL").and("deleted_at < ?", threshold));
        expired.forEach(inv -> {
            attachmentService.purgeByInvoice(inv.getId());
            invoiceMapper.deleteById(inv.getId());
        });
        if (!expired.isEmpty()) {
            log.info("回收站清理：{} 条已过 30 天保留期", expired.size());
        }
    }

    private Invoice ownedDeleted(AppPrincipal principal, String invoiceId) {
        Invoice inv = invoiceMapper.selectOneByQuery(QueryWrapper.create()
                .where("id = ?", invoiceId)
                .and("user_id = ?", principal.userId())
                .and("deleted_at IS NOT NULL"));
        if (inv == null) {
            throw new ApiException(ErrorCode.INV_001);   // 含已被 cron 清理（daysLeft≤0）的情形
        }
        return inv;
    }

    private long daysLeft(OffsetDateTime deletedAt) {
        long elapsed = Duration.between(deletedAt, OffsetDateTime.now(BIZ)).toDays();
        return Math.max(0, 30 - elapsed);
    }
}
