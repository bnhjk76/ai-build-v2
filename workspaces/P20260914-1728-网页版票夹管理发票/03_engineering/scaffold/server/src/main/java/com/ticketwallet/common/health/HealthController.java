package com.ticketwallet.common.health;

import com.ticketwallet.common.web.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 健康检查（tech-stack §3.10）：自写 controller + DB ping，语义与 v1.0 一致，不暴露公网 Actuator。 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final JdbcTemplate jdbc;

    public HealthController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public ApiResponse<Map<String, String>> health() {
        Integer one = jdbc.queryForObject("SELECT 1", Integer.class);
        String db = (one != null && one == 1) ? "up" : "down";
        return ApiResponse.ok(Map.of("status", "ok", "db", db));
    }
}
