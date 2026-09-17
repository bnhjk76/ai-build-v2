package com.ticketwallet;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/** M2 集成测试：附件三闸/回收站全流程/A3 口径(38000 用例)/CSV BOM 导出。 */
@Tag("integration")
@SpringBootTest(webEnvironment = RANDOM_PORT)
class M2FlowIntegrationTest {

    @LocalServerPort int port;
    @Autowired ObjectMapper om;

    record Client(HttpClient http, String account) {}

    String url(String p) { return "http://localhost:" + port + p; }

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
        return om.valueToTree(java.util.Map.of("__node", om.readTree(bodyStr), "__status", res.statusCode()));
    }

    byte[] getBytes(HttpClient http, String path) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(url(path))).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray()).body();
    }

    int statusOf(JsonNode w) { return w.get("__status").asInt(); }
    JsonNode bodyOf(JsonNode w) { return w.get("__node"); }

    Client registerAndLogin() throws Exception {
        String account = "it_" + UUID.randomUUID().toString().substring(0, 8) + "@test.dev";
        HttpClient http = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
        call(http, "POST", "/api/v1/auth/register",
                om.writeValueAsString(Map.of("account", account, "password", "Passw0rd!8")), true);
        return new Client(http, account);
    }

    JsonNode createInvoice(HttpClient http, String number, String date, String title,
                           String amount, String tax, String status) throws Exception {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("invoiceNumber", number);
        m.put("issuedDate", date);
        m.put("title", title);
        m.put("amount", amount);
        m.put("taxAmount", tax);
        m.put("category", "general");
        m.put("medium", "electronic");
        if (status != null) m.put("status", status);
        return bodyOf(call(http, "POST", "/api/v1/invoices", om.writeValueAsString(m), true)).path("data");
    }

    /** multipart 构造（最小实现：单文件字段）。 */
    String multipart(String boundary, String filename, byte[] content) {
        var head = ("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\""
                + filename + "\"\r\nContent-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8);
        var tail = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        var out = new ByteArrayOutputStream();
        try { out.writeBytes(head); out.write(content); out.writeBytes(tail); } catch (Exception ignored) {}
        return out.toString(java.nio.charset.StandardCharsets.ISO_8859_1);
    }

    JsonNode upload(HttpClient http, String boundary, String invoiceId, String filename, byte[] content) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url("/api/v1/invoices/" + invoiceId + "/attachments")))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("X-Requested-With", "XMLHttpRequest")
                .POST(HttpRequest.BodyPublishers.ofByteArray(
                        multipart(boundary, filename, content).getBytes(StandardCharsets.ISO_8859_1)))
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        String bodyStr = res.body() == null || res.body().isEmpty() ? "{}" : res.body();
        return om.valueToTree(java.util.Map.of("__node", om.readTree(bodyStr), "__status", res.statusCode()));
    }

    static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    static final byte[] FAKE_PNG_ZIP = {0x50, 0x4B, 0x03, 0x04, 0, 0, 0, 0, 0, 0, 0, 0};   // .png 后缀实为 zip

    @Test
    void attachments_threeGates_and_crud() throws Exception {
        Client user = registerAndLogin();
        String invId = createInvoice(user.http(), "7777888899990001", "2026-09-01", "附件测试抬头", "1000.00", "60.00", null).path("id").asText();
        String bd = "b1";

        // 正常上传（PNG 魔数）×3
        for (int i = 0; i < 3; i++) {
            var r = upload(user.http(), bd, invId, "a" + i + ".png", PNG);
            assertThat(statusOf(r)).isEqualTo(201);
        }
        // 第 4 个 → ATT_002
        var r4 = upload(user.http(), bd, invId, "a4.png", PNG);
        assertThat(statusOf(r4)).isEqualTo(422);
        assertThat(bodyOf(r4).path("error").path("code").asText()).isEqualTo("ATT_002");

        // 改后缀假图（zip 魔数 + .png）→ ATT_003
        var fake = upload(user.http(), bd, invId, "fake.png", FAKE_PNG_ZIP);
        assertThat(statusOf(fake)).isEqualTo(422);
        assertThat(bodyOf(fake).path("error").path("code").asText()).isEqualTo("ATT_003");

        // 他人访问附件 → 404
        Client other = registerAndLogin();
        JsonNode list = bodyOf(call(user.http(), "GET", "/api/v1/invoices/" + invId + "/attachments", null, false)).path("data");
        String attId = list.get(0).get("id").asText();
        HttpRequest req = HttpRequest.newBuilder(URI.create(url("/api/v1/attachments/" + attId + "/file"))).GET().build();
        HttpResponse<String> res = other.http().send(req, HttpResponse.BodyHandlers.ofString());
        assertThat(res.statusCode()).isEqualTo(404);

        // 下载（本人）：魔数回读
        byte[] got = getBytes(user.http(), "/api/v1/attachments/" + attId + "/file");
        assertThat(got).startsWith(PNG);
    }

    @Test
    void recycle_fullFlow_softDelete_restore_purge() throws Exception {
        Client user = registerAndLogin();
        JsonNode inv = createInvoice(user.http(), "7777888899990002", "2026-09-02", "回收站测试抬头", "500.00", "30.00", null);
        String id = inv.path("id").asText();

        // 软删 → 204；详情 404；列表不含
        assertThat(statusOf(call(user.http(), "DELETE", "/api/v1/invoices/" + id, null, true))).isEqualTo(204);
        assertThat(statusOf(call(user.http(), "GET", "/api/v1/invoices/" + id, null, false))).isEqualTo(404);
        JsonNode list = bodyOf(call(user.http(), "GET", "/api/v1/invoices", null, false)).path("data");
        assertThat(list.path("totalRow").asLong()).isEqualTo(0);

        // 回收站列表：daysLeft ∈ [0,30]
        JsonNode rc = bodyOf(call(user.http(), "GET", "/api/v1/recycle/items", null, false)).path("data");
        assertThat(rc.path("totalRow").asLong()).isEqualTo(1);
        assertThat(rc.path("records").get(0).path("daysLeft").asLong()).isBetween(0L, 30L);

        // 恢复 → 回到列表
        assertThat(statusOf(call(user.http(), "POST", "/api/v1/recycle/items/" + id + "/restore", "", true))).isEqualTo(204);
        assertThat(statusOf(call(user.http(), "GET", "/api/v1/invoices/" + id, null, false))).isEqualTo(200);

        // 再删 + 彻底删除 → 回收站空
        call(user.http(), "DELETE", "/api/v1/invoices/" + id, null, true);
        assertThat(statusOf(call(user.http(), "DELETE", "/api/v1/recycle/items/" + id, null, true))).isEqualTo(204);
        JsonNode rc2 = bodyOf(call(user.http(), "GET", "/api/v1/recycle/items", null, false)).path("data");
        assertThat(rc2.path("totalRow").asLong()).isEqualTo(0);
        // 恢复已彻底删除的 → 404
        assertThat(statusOf(call(user.http(), "POST", "/api/v1/recycle/items/" + id + "/restore", "", true))).isEqualTo(404);
    }

    @Test
    void stats_A3_formula() throws Exception {
        Client user = registerAndLogin();
        // engineering-plan W5 口径用例：8 正常(Σ4000) + 1 红冲(2000) + 1 作废 → validCount 9 / validAmount 2000? 
        // 此处独立校验公式：Σ正常 4800 − Σ红冲 800 = 4000.00；作废只计张数
        String month = java.time.YearMonth.now(java.time.ZoneId.of("Asia/Shanghai")).toString();
        String day = month + "-05";
        for (int i = 0; i < 8; i++) {
            createInvoice(user.http(), "66667777888800%02d".formatted(10 + i), day, "汇总正常票", "600.00", "0.00", "normal");
        }
        createInvoice(user.http(), "6666777788880090", day, "汇总红冲票", "800.00", "0.00", "reversed");
        createInvoice(user.http(), "6666777788880091", day, "汇总作废票", "999.00", "0.00", "voided");

        JsonNode s = bodyOf(call(user.http(), "GET", "/api/v1/stats/summary", null, false)).path("data").path("month");
        assertThat(s.path("validCount").asLong()).isEqualTo(9);              // 正常8+红冲1
        assertThat(s.path("validAmount").asText()).isEqualTo("4000.00");     // 4800−800（作废不计）
        assertThat(s.path("voidedCount").asLong()).isEqualTo(1);
        assertThat(s.path("totalCount").asLong()).isEqualTo(10);
        assertThat(s.path("reversedCount").asLong()).isEqualTo(1);
    }

    @Test
    void export_csvBOM_and_rows() throws Exception {
        Client user = registerAndLogin();
        String month = java.time.YearMonth.now(java.time.ZoneId.of("Asia/Shanghai")).toString();
        createInvoice(user.http(), "5555666677770091", month + "-06", "导出,含逗号抬头", "300.00", "18.00", null);
        createInvoice(user.http(), "5555666677770092", month + "-07", "导出第二张", "100.00", "6.00", null);

        HttpResponse<byte[]> res = user.http().send(HttpRequest.newBuilder(
                        URI.create(url("/api/v1/invoices/export"))).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());
        assertThat(res.statusCode()).isEqualTo(200);
        byte[] body = res.body();
        assertThat(body[0]).isEqualTo((byte) 0xEF);   // BOM 首字节
        assertThat(body[1]).isEqualTo((byte) 0xBB);
        assertThat(body[2]).isEqualTo((byte) 0xBF);
        String csv = new String(body, 3, body.length - 3, StandardCharsets.UTF_8);
        assertThat(csv.lines().count()).isEqualTo(3);   // 表头 + 2 行
        assertThat(csv).contains("\"导出,含逗号抬头\"");   // 逗号转义加引号
        assertThat(res.headers().firstValue("Content-Disposition").orElse("")).contains(".csv");
    }
}
