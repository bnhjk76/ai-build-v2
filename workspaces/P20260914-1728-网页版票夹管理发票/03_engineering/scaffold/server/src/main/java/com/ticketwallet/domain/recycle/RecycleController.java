package com.ticketwallet.domain.recycle;

import com.mybatisflex.core.paginate.Page;
import com.ticketwallet.common.security.SecurityConfig.AppPrincipal;
import com.ticketwallet.common.web.ApiResponse;
import com.ticketwallet.domain.invoice.Invoice;
import com.ticketwallet.domain.invoice.InvoiceController.InvoiceView;
import com.ticketwallet.domain.invoice.InvoiceMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

/** 回收站接口（api-design §4.5，F06/F12）。 */
@Tag(name = "recycle", description = "回收站：列表/恢复/彻底删除")
@RestController
@RequestMapping("/api/v1/recycle")
public class RecycleController {

    public record RecycleItemView(InvoiceView invoice, String deletedAt, long daysLeft) {}

    private final RecycleService service;
    private final InvoiceMapper invoiceMapper;

    public RecycleController(RecycleService service, InvoiceMapper invoiceMapper) {
        this.service = service;
        this.invoiceMapper = invoiceMapper;
    }

    @Operation(operationId = "listRecycle", summary = "回收站列表（daysLeft 服务端计算）")
    @GetMapping("/items")
    public ApiResponse<Page<RecycleItemView>> list(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "20") int page_size) {
        Page<RecycleService.RecycleItem> result = service.page(principal(), page, Math.min(page_size, 100));
        var items = result.getRecords().stream().map(this::view).toList();
        return ApiResponse.ok(new Page<>(items, result.getPageNumber(), result.getPageSize(), result.getTotalRow()));
    }

    @Operation(operationId = "restoreRecycleItem", summary = "恢复（置空 deletedAt）")
    @PostMapping("/items/{id}/restore")
    public ResponseEntity<Void> restore(@PathVariable String id) {
        service.restore(principal(), id);
        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "purgeRecycleItem", summary = "彻底删除（级联附件+文件，不可恢复）")
    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> purge(@PathVariable String id) {
        service.purge(principal(), id);
        return ResponseEntity.noContent().build();
    }

    private AppPrincipal principal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (AppPrincipal) auth.getPrincipal();
    }

    private RecycleItemView view(RecycleService.RecycleItem item) {
        Invoice inv = item.invoice();
        InvoiceView v = new InvoiceView(inv.getId(), inv.getInvoiceCode(), inv.getInvoiceNumber(),
                inv.getIssuedDate().toString(), inv.getTitle(), inv.getAmount().toPlainString(),
                inv.getTaxAmount().toPlainString(), inv.getTotalAmount().toPlainString(),
                inv.getCategory().value(), inv.getMedium().value(), inv.getStatus().value(),
                inv.getRemark(), inv.getCreatedAt().toString(),
                inv.getUpdatedAt().toString(), 0);
        return new RecycleItemView(v, item.deletedAt().toString(), item.daysLeft());
    }
}
