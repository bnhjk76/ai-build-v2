package com.ticketwallet.domain.auth;

import com.mybatisflex.core.query.QueryWrapper;
import com.ticketwallet.common.error.ApiException;
import com.ticketwallet.common.error.ErrorCode;
import com.ticketwallet.common.security.SecurityConfig.AppPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.regex.Pattern;

/**
 * 认证服务（api-design §4.1）：注册/登录/登出/当前会话。
 * 登录锁定：连续 5 次失败锁 10 分钟（users.failed_attempts/locked_until，tech-stack §3.5）。
 */
@Service
public class AuthService {

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final Duration LOCK_DURATION = Duration.ofMinutes(10);

    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE = Pattern.compile("^1\\d{10}$");

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public record AccountType(int code, String name) {
        public static final AccountType EMAIL = new AccountType(1, "email");
        public static final AccountType PHONE = new AccountType(2, "phone");
    }

    @Transactional
    public User register(String account, String password, HttpServletRequest request) {
        AccountType type = detectAccountType(account);
        if (type == null) {
            throw new ApiException(ErrorCode.AUTH_005);
        }
        String normalized = account.trim().toLowerCase();
        if (findByAccount(normalized) != null) {
            throw new ApiException(ErrorCode.AUTH_004);
        }
        User user = new User();
        user.setId(java.util.UUID.randomUUID().toString());   // Flex 显式插列（spike F6），DB 默认值不生效
        user.setAccount(normalized);
        user.setAccountType(type.code());
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFailedAttempts(0);
        user.setCreatedAt(OffsetDateTime.now());
        userMapper.insert(user);
        establishSession(user, request);
        return user;
    }

    // 注意：本方法不加 @Transactional——失败计数更新必须随 AUTH_001 抛出独立提交，
    // 包一层事务会在抛异常时回滚 failed_attempts（锁定永远不生效）
    public User login(String account, String password, HttpServletRequest request) {
        AccountType type = detectAccountType(account);
        if (type == null) {
            throw new ApiException(ErrorCode.AUTH_005);
        }
        User user = findByAccount(account.trim().toLowerCase());
        if (user == null) {
            // 不存在账号也统一 AUTH_001（不泄露哪个字段错）
            throw new ApiException(ErrorCode.AUTH_001);
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            long minutes = Duration.between(now, user.getLockedUntil()).toMinutes() + 1;
            throw new ApiException(ErrorCode.AUTH_002,
                    "账号已锁定，请 " + minutes + " 分钟后重试",
                    java.util.List.of(new ApiException.FieldError("retryAfterMinutes", String.valueOf(minutes))));
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            registerFailure(user, now);
            throw new ApiException(ErrorCode.AUTH_001);
        }
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        userMapper.update(user);
        establishSession(user, request);
        return user;
    }

    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();   // Spring Session JDBC：删 SPRING_SESSION 行，登出即失效
        }
        SecurityContextHolder.clearContext();
    }

    private void registerFailure(User user, OffsetDateTime now) {
        int attempts = (user.getFailedAttempts() == null ? 0 : user.getFailedAttempts()) + 1;
        user.setFailedAttempts(attempts);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(now.plus(LOCK_DURATION));
        }
        userMapper.update(user);
    }

    private void establishSession(User user, HttpServletRequest request) {
        AppPrincipal principal = new AppPrincipal(user.getId(), user.getAccount());
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, java.util.List.of());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        // 与 HttpSessionSecurityContextRepository.saveContext 等价的主路径：
        // 写 SPRING_SECURITY_CONTEXT 到会话（Spring Session JDBC 落库由过滤器提交）
        request.getSession(true).setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    }

    private AccountType detectAccountType(String account) {
        if (account == null || account.isBlank()) return null;
        String a = account.trim();
        if (EMAIL.matcher(a).matches()) return AccountType.EMAIL;
        if (PHONE.matcher(a).matches()) return AccountType.PHONE;
        return null;
    }

    private User findByAccount(String normalizedAccount) {
        return userMapper.selectOneByQuery(QueryWrapper.create().where("account = ?", normalizedAccount));
    }
}
