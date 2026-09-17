package com.ticketwallet.domain.auth;

import com.ticketwallet.common.security.SecurityConfig.AppPrincipal;
import com.ticketwallet.common.web.ApiResponse;
import com.ticketwallet.common.web.MaskingUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;

/** 认证四接口（api-design §4.1，F01–F03）。 */
@Tag(name = "auth", description = "认证：注册/登录/登出/当前会话")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    public record AuthRequest(
            @NotBlank(message = "账号不能为空") String account,
            @NotBlank(message = "密码不能为空") @Size(min = 8, max = 72, message = "密码长度需为 8–72 位") String password) {}

    public record AuthResponse(String account, String accountType) {}

    public record MeResponse(String account, String accountType, String expiresAt) {}

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "注册并自动登录", description = "重复注册 409 AUTH_004；格式错误 422 AUTH_005")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@jakarta.validation.Valid @RequestBody AuthRequest req,
                                                              HttpServletRequest request) {
        User user = authService.register(req.account(), req.password(), request);
        return ResponseEntity.status(201).body(ApiResponse.ok(toResponse(user)));
    }

    @Operation(summary = "登录", description = "凭据错误统一 401 AUTH_001；连续 5 次失败后锁定 423 AUTH_002（details.retryAfterMinutes）")
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@jakarta.validation.Valid @RequestBody AuthRequest req, HttpServletRequest request) {
        User user = authService.login(req.account(), req.password(), request);
        return ApiResponse.ok(toResponse(user));
    }

    @Operation(summary = "登出（需登录）", description = "删除会话行，204")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "当前会话（需登录）", description = "失效 401 AUTH_003；账号脱敏展示")
    @GetMapping("/me")
    public ApiResponse<MeResponse> me(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        AppPrincipal principal = (AppPrincipal) auth.getPrincipal();
        var session = request.getSession(false);
        OffsetDateTime expiry = session == null
                ? OffsetDateTime.now()
                : OffsetDateTime.ofInstant(Instant.ofEpochMilli(session.getLastAccessedTime())
                        .plusSeconds(session.getMaxInactiveInterval()), OffsetDateTime.now().getOffset());
        return ApiResponse.ok(new MeResponse(
                MaskingUtils.maskAccount(principal.account()),
                typeOf(principal.account()),
                expiry.toString()));
    }

    private AuthResponse toResponse(User user) {
        return new AuthResponse(MaskingUtils.maskAccount(user.getAccount()), typeOf(user.getAccount()));
    }

    private String typeOf(String account) {
        return account.contains("@") ? "email" : "phone";
    }
}
