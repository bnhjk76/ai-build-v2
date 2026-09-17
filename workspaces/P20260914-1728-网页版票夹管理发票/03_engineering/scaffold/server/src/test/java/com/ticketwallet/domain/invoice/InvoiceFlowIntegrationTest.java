package com.ticketwallet.domain.invoice;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/** W2 DoD 集成测试：创建/五维筛选/掩码/越权 404/INV_002/INV_003。真实 HTTP + 双用户越权矩阵。 */
@Tag("integration")
@SpringBootTest(webEnvironment = RANDOM_PORT)
class InvoiceFlowIntegrationTest {

    @LocalServerPort int port;
    @Autowired ObjectMapper om;

    record Client(HttpClient http, String account) {}

    String url(String path) { return "http://localhost:" + port + path; }

    /** 返回带 status 的响应：以 __status 字段携带。 */
    JsonNode call(HttpClient http, String method, String path, String body, boolean xrw) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url(path)))
                .header("Content-Type", "application/json");
        if (xrw) b.header("X-Requested-With", "XMLHttpRequest");
        HttpRequest req = switch (method) {
            case "GET" -> b.GET().build();
            default -> b.method(method, HttpRequest.BodyPublishers.ofString(body == null ? "" : body)).build();
        };
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        String bodyStr = res.body() == null || res.body().isEmpty() ? "{}" : res.body();
        JsonNode node = om.readTree(bodyStr);
        return om.valueToTree(java.util.Map.of("__node", node, "__status", res.statusCode()));
    }

    int statusOf(JsonNode wrapper) { return wrapper.get("__status").asInt(); }
    JsonNode bodyOf(JsonNode wrapper) { return wrapper.get("__node"); }

    Client registerAndLogin() throws Exception {
        String account = "it_" + UUID.randomUUID().toString().substring(0, 8) + "@test.dev";
        HttpClient http = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
        call(http, "POST", "/api/v1/auth/register",
                om.writeValueAsString(java.util.Map.of("account", account, "password", "Passw0rd!8")), true);
        return new Client(http, account);
    }

    String invoiceJson(String number, String date, String title, String amount, String tax) throws Exception {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("invoiceNumber", number);
        m.put("issuedDate", date);
        m.put("title", title);
        m.put("amount", amount);
        m.put("taxAmount", tax);
        m.put("category", "general");
        m.put("medium", "electronic");
        return om.writeValueAsString(m);
    }

    @Test
    void create_detail_list_masking_fullFlow() throws Exception {
        Client user = registerAndLogin();
        String number = "12345678" + (1000 + (int) (Math.random() * 8999));

        JsonNode created = call(user.http(), "POST", "/api/v1/invoices",
                invoiceJson(number, "2026-08-21", "杭州某科技有限公司", "3000.00", "180.00"), true);
        assertThat(statusOf(created)).isEqualTo(201);
        JsonNode data = bodyOf(created).path("data");
        assertThat(data.path("invoiceNumber").asText()).isEqualTo(number);        // 新建返回全号
        assertThat(data.path("totalAmount").asText()).isEqualTo("3180.00");       // 服务端重算
        assertThat(data.path("status").asText()).isEqualTo("normal");              // 默认状态

        // 详情：不脱敏
        JsonNode detail = call(user.http(), "GET", "/api/v1/invoices/" + data.path("id").asText(), null, false);
        assertThat(statusOf(detail)).isEqualTo(200);
        assertThat(bodyOf(detail).path("data").path("invoiceNumber").asText()).isEqualTo(number);

        // 列表：默认掩码 ****5678
        JsonNode list = call(user.http(), "GET", "/api/v1/invoices?page=1&page_size=10", null, false);
        assertThat(statusOf(list)).isEqualTo(200);
        JsonNode first = bodyOf(list).path("data").path("records").get(0);
        assertThat(first.path("invoiceNumber").asText()).isEqualTo("****" + number.substring(number.length() - 4));
        // show_sensitive=1 全号
        JsonNode full = call(user.http(), "GET", "/api/v1/invoices?show_sensitive=1", null, false);
        assertThat(bodyOf(full).path("data").path("records").get(0).path("invoiceNumber").asText()).isEqualTo(number);
    }

    @Test
    void fiveDimensionFilter() throws Exception {
        Client user = registerAndLogin();
        String tag = UUID.randomUUID().toString().substring(0, 6);
        call(user.http(), "POST", "/api/v1/invoices", invoiceJson("1111222233334441", "2026-08-01", "FilterTech-" + tag + "公司", "1000.00", "60.00"), true);
        call(user.http(), "POST", "/api/v1/invoices", invoiceJson("1111222233334442", "2026-09-02", "Filter餐饮-" + tag, "200.00", "12.00"), true);
        call(user.http(), "POST", "/api/v1/invoices", invoiceJson("1111222233334443", "2026-09-10", "FilterTech-" + tag + "实验室", "5000.00", "300.00"), true);

        // month + status 默认 normal
        JsonNode r1 = bodyOf(call(user.http(), "GET", "/api/v1/invoices?month=2026-09", null, false)).path("data");
        assertThat(r1.path("totalRow").asLong()).isEqualTo(2);

        // title ILIKE（大小写不敏感：filtertech 小写命中 FilterTech）
        JsonNode r2 = bodyOf(call(user.http(), "GET", "/api/v1/invoices?title=filtertech-" + tag, null, false)).path("data");
        assertThat(r2.path("totalRow").asLong()).isEqualTo(2);

        // 金额区间（作用 totalAmount）
        JsonNode r3 = bodyOf(call(user.http(), "GET", "/api/v1/invoices?amountMin=300.00&amountMax=2000.00", null, false)).path("data");
        assertThat(r3.path("totalRow").asLong()).isEqualTo(1);

        // 同维多值 OR：medium=electronic,paper → 全部
        JsonNode r4 = bodyOf(call(user.http(), "GET", "/api/v1/invoices?medium=electronic,paper", null, false)).path("data");
        assertThat(r4.path("totalRow").asLong()).isEqualTo(3);

        // 组合 AND：month=2026-09 AND title
        JsonNode r5 = bodyOf(call(user.http(), "GET", "/api/v1/invoices?month=2026-09&title=餐饮", null, false)).path("data");
        assertThat(r5.path("totalRow").asLong()).isEqualTo(1);
    }

    @Test
    void crossUserAccess_returns404_INV_001() throws Exception {
        Client alice = registerAndLogin();
        Client bob = registerAndLogin();
        JsonNode created = call(alice.http(), "POST", "/api/v1/invoices",
                invoiceJson("5555666677778881", "2026-08-15", "越权测试抬头A", "900.00", "54.00"), true);
        String id = bodyOf(created).path("data").path("id").asText();

        JsonNode bobView = call(bob.http(), "GET", "/api/v1/invoices/" + id, null, false);
        assertThat(statusOf(bobView)).isEqualTo(404);
        assertThat(bodyOf(bobView).path("error").path("code").asText()).isEqualTo("INV_001");
        assertThat(bodyOf(bobView).toString()).doesNotContain("越权测试抬头A");   // 无他人数据痕迹
    }

    @Test
    void futureDate_returns422_INV_002_fieldIssuedDate() throws Exception {
        Client user = registerAndLogin();
        JsonNode res = call(user.http(), "POST", "/api/v1/invoices",
                invoiceJson("9999888877776661", "2100-01-01", "未来日期", "100.00", "6.00"), true);
        assertThat(statusOf(res)).isEqualTo(422);
        assertThat(bodyOf(res).path("error").path("code").asText()).isEqualTo("INV_002");
        assertThat(bodyOf(res).path("error").path("details").get(0).get("field").asText()).isEqualTo("issuedDate");
    }

    @Test
    void badNumber_returns422_INV_002() throws Exception {
        Client user = registerAndLogin();
        JsonNode res = call(user.http(), "POST", "/api/v1/invoices",
                invoiceJson("12345", "2026-08-01", "号码过短", "100.00", "6.00"), true);
        assertThat(statusOf(res)).isEqualTo(422);
        assertThat(bodyOf(res).path("error").path("code").asText()).isEqualTo("INV_002");
        assertThat(bodyOf(res).path("error").path("details").get(0).get("field").asText()).isEqualTo("invoiceNumber");
    }

    @Test
    void minGreaterThanMax_returns422_INV_003() throws Exception {
        Client user = registerAndLogin();
        JsonNode res = call(user.http(), "GET", "/api/v1/invoices?amountMin=500.00&amountMax=100.00", null, false);
        assertThat(statusOf(res)).isEqualTo(422);
        assertThat(bodyOf(res).path("error").path("code").asText()).isEqualTo("INV_003");
    }

    @Test
    void badMonth_returns422_INV_003() throws Exception {
        Client user = registerAndLogin();
        JsonNode res = call(user.http(), "GET", "/api/v1/invoices?month=2026-13", null, false);
        assertThat(statusOf(res)).isEqualTo(422);
        assertThat(bodyOf(res).path("error").path("code").asText()).isEqualTo("INV_003");
    }
}
