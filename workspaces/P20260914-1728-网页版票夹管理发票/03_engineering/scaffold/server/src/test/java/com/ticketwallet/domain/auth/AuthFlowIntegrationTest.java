package com.ticketwallet.domain.auth;

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

/**
 * auth 集成测试（engineering-plan W1 DoD：注册→登录→登出绿；锁定 10 分钟绿；AUTH_001~006 断言齐全）。
 * 走真实 HTTP（RANDOM_PORT + CookieManager）——MockMvc 下 Spring Session 过滤器疑似重复注册导致
 * SPRING_SESSION 重复插入（W1 交付说明记录）。连 dev PG（Testcontainers 升级排 W1 验收轮）。
 */
@Tag("integration")
@SpringBootTest(webEnvironment = RANDOM_PORT)
class AuthFlowIntegrationTest {

    @LocalServerPort int port;
    @Autowired ObjectMapper om;

    final HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();

    record Resp(int status, JsonNode json) {}

    String url(String path) { return "http://localhost:" + port + path; }

    Resp post(String path, String json, boolean withXrw) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url(path)))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));
        if (withXrw) b.header("X-Requested-With", "XMLHttpRequest");
        HttpResponse<String> res = client.send(b.build(), HttpResponse.BodyHandlers.ofString());
        JsonNode node = res.body() == null || res.body().isEmpty() ? null : om.readTree(res.body());
        return new Resp(res.statusCode(), node);
    }

    Resp get(String path) throws Exception {
        HttpResponse<String> res = client.send(
                HttpRequest.newBuilder(URI.create(url(path))).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        JsonNode node = res.body() == null || res.body().isEmpty() ? null : om.readTree(res.body());
        return new Resp(res.statusCode(), node);
    }

    String accountJson(String account, String password) throws Exception {
        return om.writeValueAsString(new AuthController.AuthRequest(account, password));
    }

    String newAccount() { return "it_" + UUID.randomUUID().toString().substring(0, 8) + "@test.dev"; }

    @Test
    void register_login_me_logout_fullFlow() throws Exception {
        String account = newAccount();
        Resp reg = post("/api/v1/auth/register", accountJson(account, "Passw0rd!8"), true);
        assertThat(reg.status()).isEqualTo(201);
        assertThat(reg.json().path("data").path("account").asText()).contains("***");
        assertThat(reg.json().path("data").path("accountType").asText()).isEqualTo("email");

        Resp me = get("/api/v1/auth/me");
        assertThat(me.status()).isEqualTo(200);
        assertThat(me.json().path("data").path("expiresAt").isString()).isTrue();

        Resp logout = post("/api/v1/auth/logout", "", true);
        assertThat(logout.status()).isEqualTo(204);

        Resp meAfter = get("/api/v1/auth/me");
        assertThat(meAfter.status()).isEqualTo(401);
        assertThat(meAfter.json().path("error").path("code").asText()).isEqualTo("AUTH_003");
    }

    @Test
    void duplicateRegister_returns409_AUTH_004() throws Exception {
        String account = newAccount();
        post("/api/v1/auth/register", accountJson(account, "Passw0rd!8"), true);
        Resp dup = post("/api/v1/auth/register", accountJson(account, "Passw0rd!8"), true);
        assertThat(dup.status()).isEqualTo(409);
        assertThat(dup.json().path("error").path("code").asText()).isEqualTo("AUTH_004");
        assertThat(dup.json().path("error").path("message").asText())
                .isEqualTo("该邮箱/手机号已注册，请直接登录");
    }

    @Test
    void badAccountFormat_returns422_AUTH_005() throws Exception {
        Resp res = post("/api/v1/auth/register", accountJson("not-an-account", "Passw0rd!8"), true);
        assertThat(res.status()).isEqualTo(422);
        assertThat(res.json().path("error").path("code").asText()).isEqualTo("AUTH_005");
    }

    @Test
    void shortPassword_returns422_AUTH_005_details() throws Exception {
        Resp res = post("/api/v1/auth/register", accountJson(newAccount(), "short"), true);
        assertThat(res.status()).isEqualTo(422);
        assertThat(res.json().path("error").path("code").asText()).isEqualTo("AUTH_005");
        assertThat(res.json().path("error").path("details").isArray()).isTrue();
    }

    @Test
    void wrongPassword_AUTH_001_then_locked_AUTH_002() throws Exception {
        String account = newAccount();
        post("/api/v1/auth/register", accountJson(account, "Passw0rd!8"), true);

        for (int i = 1; i <= 5; i++) {
            Resp bad = post("/api/v1/auth/login", accountJson(account, "WrongPass!9"), true);
            assertThat(bad.status()).isEqualTo(401);
            assertThat(bad.json().path("error").path("code").asText()).isEqualTo("AUTH_001");
            assertThat(bad.json().path("error").path("message").asText()).isEqualTo("账号或密码错误");
        }

        // 第 6 次：正确密码也被锁定 → 423 AUTH_002 + retryAfterMinutes
        Resp locked = post("/api/v1/auth/login", accountJson(account, "Passw0rd!8"), true);
        assertThat(locked.status()).isEqualTo(423);
        assertThat(locked.json().path("error").path("code").asText()).isEqualTo("AUTH_002");
        JsonNode details = locked.json().path("error").path("details");
        assertThat(details.isArray()).isTrue();
        assertThat(details.get(0).get("field").asText()).isEqualTo("retryAfterMinutes");
        assertThat(details.get(0).get("message").asLong()).isBetween(1L, 10L);
    }

    @Test
    void writeWithoutXRequestedWith_returns403_AUTH_006() throws Exception {
        Resp res = post("/api/v1/auth/login", accountJson(newAccount(), "Passw0rd!8"), false);
        assertThat(res.status()).isEqualTo(403);
        assertThat(res.json().path("error").path("code").asText()).isEqualTo("AUTH_006");
        assertThat(res.json().path("error").path("message").asText()).isEqualTo("请从票夹通页面发起操作");
    }

    @Test
    void loginSuccess_returnsMaskedAccount_notLeakingRaw() throws Exception {
        String account = newAccount();
        post("/api/v1/auth/register", accountJson(account, "Passw0rd!8"), true);
        Resp res = post("/api/v1/auth/login", accountJson(account, "Passw0rd!8"), true);
        assertThat(res.status()).isEqualTo(200);
        String masked = res.json().path("data").path("account").asText();
        assertThat(masked).contains("***");
        assertThat(masked).doesNotContain(account.substring(1));
    }
}
