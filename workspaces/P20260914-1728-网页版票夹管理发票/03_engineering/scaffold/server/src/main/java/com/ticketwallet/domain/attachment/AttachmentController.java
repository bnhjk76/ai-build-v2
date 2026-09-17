package com.ticketwallet.domain.attachment;

import com.ticketwallet.common.security.SecurityConfig.AppPrincipal;
import com.ticketwallet.common.storage.StorageService;
import com.ticketwallet.common.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.nio.file.Path;
import java.util.List;

/** 附件接口（api-design §4.3，F11）。 */
@Tag(name = "attachments", description = "附件：上传/列表/下载/删除")
@RestController
public class AttachmentController {

    public record AttachmentView(String id, String invoiceId, String filename, String mimeType,
                                 int size, String createdAt) {}

    private final AttachmentService service;
    private final StorageService storage;

    public AttachmentController(AttachmentService service, StorageService storage) {
        this.service = service;
        this.storage = storage;
    }

    @Operation(operationId = "uploadAttachment", summary = "上传附件（F11）",
            description = "三校验：>10MB→ATT_001；已挂3个→ATT_002；格式/魔数不符→ATT_003")
    @PostMapping("/api/v1/invoices/{id}/attachments")
    public ResponseEntity<ApiResponse<AttachmentView>> upload(@PathVariable String id,
                                                              @RequestParam("file") MultipartFile file) throws Exception {
        Attachment att = service.upload(principal(), id, file);
        return ResponseEntity.status(201).body(ApiResponse.ok(view(att)));
    }

    @Operation(operationId = "listAttachments", summary = "附件列表")
    @GetMapping("/api/v1/invoices/{id}/attachments")
    public ApiResponse<List<AttachmentView>> list(@PathVariable String id) {
        return ApiResponse.ok(service.listOwned(principal(), id).stream().map(this::view).toList());
    }

    @Operation(operationId = "downloadAttachment", summary = "下载/预览（属主谓词）")
    @GetMapping("/api/v1/attachments/{id}/file")
    public ResponseEntity<FileSystemResource> download(@PathVariable String id) {
        Attachment att = service.loadOwned(principal(), id);
        Path path = storage.get(att.getStorageKey());
        String encoded = UriUtils.encode(att.getFilename(), java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType(att.getMimeType()))
                .body(new FileSystemResource(path));
    }

    @Operation(operationId = "deleteAttachment", summary = "删除单个附件（R7：不影响其他）")
    @DeleteMapping("/api/v1/attachments/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(principal(), id);
        return ResponseEntity.noContent().build();
    }

    private AppPrincipal principal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (AppPrincipal) auth.getPrincipal();
    }

    private AttachmentView view(Attachment a) {
        return new AttachmentView(a.getId(), a.getInvoiceId(), a.getFilename(), a.getMimeType(),
                a.getSize(), a.getCreatedAt().toString());
    }
}
