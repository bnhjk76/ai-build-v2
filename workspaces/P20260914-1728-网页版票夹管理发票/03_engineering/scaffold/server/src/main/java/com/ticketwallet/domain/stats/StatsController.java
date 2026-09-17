package com.ticketwallet.domain.stats;

import com.ticketwallet.common.security.SecurityConfig.AppPrincipal;
import com.ticketwallet.common.web.ApiResponse;
import com.ticketwallet.domain.stats.StatsService.Summary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 汇总接口（api-design §4.4，F09/G3）：无参，固定东八区当前自然月/年。 */
@Tag(name = "stats", description = "汇总统计（口径 A3 单点）")
@RestController
@RequestMapping("/api/v1/stats")
public class StatsController {

    private final StatsService service;

    public StatsController(StatsService service) {
        this.service = service;
    }

    @Operation(operationId = "getStatsSummary", summary = "月/年卡片 + 票种×介质分布",
            description = "validAmount=Σ正常−Σ红冲（可为负）；作废只计张数；空数据全 0")
    @GetMapping("/summary")
    public ApiResponse<Summary> summary() {
        return ApiResponse.ok(service.summary(principal()));
    }

    private AppPrincipal principal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (AppPrincipal) auth.getPrincipal();
    }
}
