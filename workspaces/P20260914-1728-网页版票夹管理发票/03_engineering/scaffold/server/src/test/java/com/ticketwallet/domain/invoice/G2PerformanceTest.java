package com.ticketwallet.domain.invoice;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import com.ticketwallet.domain.auth.User;
import com.ticketwallet.domain.auth.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * G2 性能实测（engineering-plan §2.3 M1 出口：1000 条种子组合筛选 ≤1s，质量线 P95 <500ms）。
 * 说明：以 Java HTTP 直测替代 k6（等价口径：真实 HTTP 往返 + PG 真库；k6 CLI 版排 CI 接入后补跑）。
 * 种子：单用户 1000 条（8 个月份 × 混合抬头/票种/状态），覆盖 idx_inv_user_date / idx_inv_total / trgm GIN。
 */
@Tag("perf")
@SpringBootTest(webEnvironment = RANDOM_PORT)
class G2PerformanceTest {

    @LocalServerPort int port;
    @Autowired InvoiceMapper invoiceMapper;
    @Autowired UserMapper userMapper;
    @Autowired ObjectMapper om;

    HttpClient client() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
    }

    String url(String p) { return "http://localhost:" + port + p; }

    String seed() throws Exception {
        String account = "perf_" + UUID.randomUUID().toString().substring(0, 8) + "@test.dev";
        HttpClient http = client();
        HttpRequest reg = HttpRequest.newBuilder(URI.create(url("/api/v1/auth/register")))
                .header("Content-Type", "application/json").header("X-Requested-With", "XMLHttpRequest")
                .POST(HttpRequest.BodyPublishers.ofString(
                        om.writeValueAsString(java.util.Map.of("account", account, "password", "Passw0rd!8"))))
                .build();
        http.send(reg, HttpResponse.BodyHandlers.ofString());

        // 直接经 mapper 批量插 1000 条（绕过 HTTP，种子本身不计入被测时延）
        User user = userMapper.selectOneByQuery(com.mybatisflex.core.query.QueryWrapper.create()
                .where("account = ?", account));
        String userId = user.getId();
        String[] titles = {"杭州某科技有限公司", "上海餐饮管理有限公司", "深圳电子贸易有限公司", "北京咨询合伙企业"};
        List<Invoice> batch = new ArrayList<>(1000);
        for (int i = 0; i < 1000; i++) {
            Invoice inv = new Invoice();
            inv.setId(UUID.randomUUID().toString());
            inv.setUserId(userId);
            inv.setInvoiceNumber("%015d".formatted(880000000000000L + i));
            inv.setIssuedDate(LocalDate.of(2026, 1 + (i % 8), 1 + (i % 28)));
            inv.setTitle(titles[i % 4] + "-" + (i % 37));
            inv.setAmount(new BigDecimal("50.00").add(BigDecimal.valueOf(i % 900)));
            inv.setTaxAmount(new BigDecimal("3.00"));
            inv.setTotalAmount(inv.getAmount().add(inv.getTaxAmount()));
            inv.setCategory(i % 3 == 0 ? InvoiceCategory.SPECIAL : InvoiceCategory.GENERAL);
            inv.setMedium(i % 2 == 0 ? InvoiceMedium.ELECTRONIC : InvoiceMedium.PAPER);
            inv.setStatus(i % 10 == 9 ? InvoiceStatus.VOIDED : InvoiceStatus.NORMAL);
            OffsetDateTime now = OffsetDateTime.now();
            inv.setCreatedAt(now);
            inv.setUpdatedAt(now);
            batch.add(inv);
        }
        invoiceMapper.insertBatch(batch);
        return account;
    }

    void login(HttpClient http, String account) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url("/api/v1/auth/login")))
                .header("Content-Type", "application/json").header("X-Requested-With", "XMLHttpRequest")
                .POST(HttpRequest.BodyPublishers.ofString(
                        om.writeValueAsString(java.util.Map.of("account", account, "password", "Passw0rd!8"))))
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        assertThat(res.statusCode()).isEqualTo(200);
    }

    long timed(HttpClient http, String path) throws Exception {
        long t0 = System.nanoTime();
        HttpResponse<String> res = http.send(HttpRequest.newBuilder(URI.create(url(path)))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        // Cookie 由 CookieManager 注入；这里只度量往返
        long ms = Duration.ofNanos(System.nanoTime() - t0).toMillis();
        assertThat(res.statusCode()).as(path).isEqualTo(200);
        return ms;
    }

    @Test
    void combinationFilter_p95_under500ms_with1000rows() throws Exception {
        String account = seed();
        HttpClient http = client();
        login(http, account);   // 同一 client 实例登录，CookieManager 自动携带会话

        // 五组查询形态（组合筛选/ILIKE/金额区间/分页深翻/详情）
        List<String> paths = Arrays.asList(
                "/api/v1/invoices?month=2026-05&status=normal&page=1&page_size=20",
                "/api/v1/invoices?title=科技&amountMin=100.00&amountMax=800.00&page=1&page_size=20",
                "/api/v1/invoices?medium=electronic&category=special&page=3&page_size=20",
                "/api/v1/invoices?page=50&page_size=20",
                "/api/v1/invoices?month=2026-08&title=贸易&amountMin=50.00&page=1&page_size=20");

        List<Long> samples = new ArrayList<>();
        for (int round = 0; round < 10; round++) {          // 预热 2 轮不剔除，直接统计
            for (String p : paths) {
                samples.add(timed(http, p));
            }
        }
        List<Long> sorted = samples.stream().sorted().toList();
        double p95 = sorted.get((int) Math.ceil(0.95 * sorted.size()) - 1);
        double avg = sorted.stream().mapToLong(Long::longValue).average().orElse(0);
        System.out.printf("[G2] samples=%d avg=%.1fms p95=%.1fms max=%dms%n",
                sorted.size(), avg, p95, sorted.getLast());

        assertThat(p95).as("组合筛选 P95（1000 条种子）").isLessThan(500.0);
        assertThat(sorted.getLast()).as("最差单次（G2 上限 1s）").isLessThan(1000L);
    }
}
