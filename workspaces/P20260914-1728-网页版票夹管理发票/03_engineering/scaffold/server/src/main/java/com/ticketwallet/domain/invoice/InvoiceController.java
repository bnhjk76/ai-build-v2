package com.ticketwallet.domain.invoice;

import com.mybatisflex.core.paginate.Page;
import com.ticketwallet.common.security.SecurityConfig.AppPrincipal;
import com.ticketwallet.common.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 发票接口（api-design §4.2，W2 范围：POST / GET 列表 / GET 详情）。 */
@Tag(name = "invoices", description = "发票：录入/列表五维筛选/详情")
@RestController
@RequestMapping("/api/v1/invoices")
public class InvoiceController {

    public record CreateRequest(
            String invoiceCode,
            @NotBlank(message = "发票号码不能为空") String invoiceNumber,
            @NotBlank(message = "开票日期不能为空") String issuedDate,
            @NotBlank(message = "抬头不能为空") String title,
            @NotBlank(message = "金额不能为空") String amount,
            @NotBlank(message = "税额不能为空") String taxAmount,
            InvoiceCategory category,
            InvoiceMedium medium,
            InvoiceStatus status,
            String remark) {}

    public record InvoiceView(String id, String invoiceCode, String invoiceNumber, String issuedDate,
                              String title, String amount, String taxAmount, String totalAmount,
                              String category, String medium, String status, String remark,
                              String createdAt, String updatedAt, int attachmentCount) {}

    public record ListView(InvoiceView item, long total, int page, int pageSize) {}

    private final InvoiceService service;
    private final com.ticketwallet.domain.recycle.RecycleService recycleService;
    private final InvoiceExportService exportService;

    public InvoiceController(InvoiceService service,
                             com.ticketwallet.domain.recycle.RecycleService recycleService,
                             InvoiceExportService exportService) {
        this.service = service;
        this.recycleService = recycleService;
        this.exportService = exportService;
    }

    @Operation(operationId = "createInvoice", summary = "新增发票（F04，G1）", description = "totalAmount 服务端重算；issuedDate 晚于今天 422 INV_002；成功 201 返回完整资源（不脱敏）")
    @PostMapping
    public ResponseEntity<ApiResponse<InvoiceView>> create(@jakarta.validation.Valid @RequestBody CreateRequest req) {
        Invoice inv = service.create(principal(), new InvoiceService.CreateInput(
                req.invoiceCode(), req.invoiceNumber(), req.issuedDate(), req.title(),
                req.amount(), req.taxAmount(), req.category(), req.medium(), req.status(), req.remark()));
        return ResponseEntity.status(201).body(ApiResponse.ok(view(inv, true)));  // 新建返回全号（api-design §4.2）
    }

    @Operation(operationId = "listInvoices", summary = "列表+五维筛选+分页（F07/F08）",
            description = "month/title(ILIKE)/amountMin-Max(作用 totalAmount)/category/medium/status 可多值逗号分隔；条件 AND、同维 OR；invoiceNumber 默认掩码，show_sensitive=1 全号")
    @GetMapping
    public ApiResponse<Page<InvoiceView>> list(
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String amountMin,
            @RequestParam(required = false) String amountMax,
            @RequestParam(required = false) List<String> category,
            @RequestParam(required = false) List<String> medium,
            @RequestParam(required = false) List<String> status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int page_size,
            @RequestParam(defaultValue = "0") int show_sensitive) {
        InvoiceService.ListQuery q = new InvoiceService.ListQuery(month, title, amountMin, amountMax,
                category, medium, status, page, Math.min(page_size, 100), show_sensitive == 1);
        Page<Invoice> result = service.page(principal(), q);
        List<InvoiceView> items = result.getRecords().stream()
                .map(inv -> view(inv, q.showSensitive()))
                .toList();
        // 展示层 Page（避免直接序列化实体集合）
        Page<InvoiceView> view = new Page<>(items, result.getPageNumber(), result.getPageSize(), result.getTotalRow());
        return ApiResponse.ok(view);
    }

    @Operation(operationId = "patchInvoice", summary = "编辑发票（F05，M2）",
            description = "部分字段合并；保留 createdAt、刷新 updatedAt；校验/totalAmount 重算同创建")
    @org.springframework.web.bind.annotation.PatchMapping("/{id}")
    public ApiResponse<InvoiceView> patch(@PathVariable String id,
                                          @jakarta.validation.Valid @RequestBody CreateRequest req) {
        return ApiResponse.ok(view(service.patch(principal(), id, new InvoiceService.CreateInput(
                req.invoiceCode(), req.invoiceNumber(), req.issuedDate(), req.title(),
                req.amount(), req.taxAmount(), req.category(), req.medium(), req.status(), req.remark())), true));
    }

    @Operation(operationId = "deleteInvoice", summary = "删除→回收站（F06，M2）",
            description = "软删除（置 deletedAt），附件随行保留；再删 404 INV_001")
    @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        recycleService.softDelete(principal(), id);
        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "exportInvoices", summary = "CSV 导出（F10，G3）",
            description = "query 同列表筛选；预检 >5000 → 422 EXP_001；200 text/csv 流式 + BOM；内容不脱敏；文件名 票夹通导出_时间戳.csv")
    @GetMapping("/export")
    public ResponseEntity<org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody> export(
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String amountMin,
            @RequestParam(required = false) String amountMax,
            @RequestParam(required = false) List<String> category,
            @RequestParam(required = false) List<String> medium,
            @RequestParam(required = false) List<String> status) {
        InvoiceService.ListQuery q = new InvoiceService.ListQuery(month, title, amountMin, amountMax,
                category, medium, status, 1, 20, true);
        // 预检+取数在响应头之前（>5000 此处抛 EXP_001 → 422，不产生半截文件）
        java.util.List<Invoice> rows = exportService.filteredForExport(principal(), q);
        String encoded = java.net.URLEncoder.encode(exportService.filename(),
                java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
        var body = (org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody)
                out -> {
                    try {
                        exportService.writeCsv(out, rows);
                    } catch (java.io.IOException ioe) {
                        throw ioe;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                };
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv; charset=utf-8")
                .header("Content-Disposition", "attachment; filename*=UTF-8''" + encoded)
                .body(body);
    }

    @Operation(operationId = "getInvoiceDetail", summary = "发票详情（F07）", description = "完整字段不脱敏（详情页口径）；非本人/不存在 404 INV_001")
    @GetMapping("/{id}")
    public ApiResponse<InvoiceView> detail(@PathVariable String id) {
        return ApiResponse.ok(view(service.loadOwned(principal(), id), true));
    }

    private AppPrincipal principal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (AppPrincipal) auth.getPrincipal();
    }

    private InvoiceView view(Invoice inv, boolean showSensitive) {
        String number = showSensitive ? inv.getInvoiceNumber()
                : com.ticketwallet.common.web.MaskingUtils.maskInvoiceNumber(inv.getInvoiceNumber());
        return new InvoiceView(inv.getId(), inv.getInvoiceCode(), number, inv.getIssuedDate().toString(),
                inv.getTitle(), inv.getAmount().toPlainString(), inv.getTaxAmount().toPlainString(),
                inv.getTotalAmount().toPlainString(),
                inv.getCategory().value(), inv.getMedium().value(), inv.getStatus().value(),
                inv.getRemark(),
                inv.getCreatedAt() == null ? null : inv.getCreatedAt().toString(),
                inv.getUpdatedAt() == null ? null : inv.getUpdatedAt().toString(),
                0);   // attachmentCount：M2 附件上线后回填真实计数
    }
}
