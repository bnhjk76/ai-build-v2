package com.ticketwallet.domain.spike;

import com.mybatisflex.core.paginate.Page;
import com.ticketwallet.common.web.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** spike 冒烟入口（W1 结束后整包删除，不进任何业务语义）。 */
@RestController
@RequestMapping("/api/v1/spike")
public class SpikeController {

    private final SpikeService service;

    public SpikeController(SpikeService service) {
        this.service = service;
    }

    public record CreateRequest(String title, SpikeKind kind, String amount) {}

    /** 冒烟②连PG + 方言用例③enum 写入：Flex insert → PG enum 列。 */
    @PostMapping("/items")
    public ApiResponse<SpikeItem> create(@RequestBody CreateRequest req) {
        return ApiResponse.ok(service.create(req.title(), req.kind(), new java.math.BigDecimal(req.amount())));
    }

    /** 冒烟③事务回滚：服务内插入后抛异常，捕获后由调用方用 /count 核对行数不变。 */
    @PostMapping("/tx-rollback")
    public ApiResponse<Map<String, Object>> txRollback(@RequestBody CreateRequest req) {
        long before = service.count();
        String error = null;
        try {
            service.createThenRollback(req.title(), req.kind(), new java.math.BigDecimal(req.amount()));
        } catch (IllegalStateException e) {
            error = e.getMessage();
        }
        long after = service.count();
        return ApiResponse.ok(Map.of(
                "before", before,
                "after", after,
                "rolledBack", before == after,
                "error", String.valueOf(error)));
    }

    /** 冒烟④分页 + 方言用例②：mode=flex 观察 QueryWrapper.like 大小写；mode=ilike 走原生 ILIKE。 */
    @GetMapping("/items")
    public ApiResponse<Object> list(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "2") int size,
                                    @RequestParam(required = false) String titleLike,
                                    @RequestParam(defaultValue = "flex") String mode) {
        if ("ilike".equalsIgnoreCase(mode)) {
            List<SpikeItem> rows = service.ilike(titleLike, size, (page - 1) * size);
            return ApiResponse.ok(Map.of("mode", "ilike", "rows", rows, "count", rows.size()));
        }
        Page<SpikeItem> result = service.page(page, size, titleLike);
        return ApiResponse.ok(Map.of("mode", "flex", "pageNumber", result.getPageNumber(),
                "pageSize", result.getPageSize(), "totalRow", result.getTotalRow(), "records", result.getRecords()));
    }

    @GetMapping("/count")
    public ApiResponse<Map<String, Object>> count() {
        return ApiResponse.ok(Map.of("count", service.count()));
    }

    /** 冒烟⑤session 落库：写入属性后返回 sessionId，SPRING_SESSION 行数由 psql 侧核对。 */
    @GetMapping("/session")
    public ApiResponse<Map<String, Object>> session(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        session.setAttribute("spikeAt", Instant.now().toString());
        return ApiResponse.ok(Map.of(
                "sessionId", session.getId(),
                "spikeAt", session.getAttribute("spikeAt")));
    }
}
