package com.ticketwallet.common.security;

import tools.jackson.databind.ObjectMapper;  // Boot 4 = Jackson 3（spike发现#9）
import com.ticketwallet.common.error.ErrorCode;
import com.ticketwallet.common.log.RequestIdFilter;
import com.ticketwallet.common.web.GlobalExceptionHandler;
import com.ticketwallet.common.web.XRequestedWithFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Map;

/**
 * 安全骨架（tech-stack §3.5）：Security 只承担登录态识别与登出；
 * 授权纪律 = 登录 + loadOwned 谓词（architecture §4.3）。
 * 未登录访问受保护接口 → 401 AUTH_003 包络（api-design §7.1）。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** 会话 Principal：userId 供业务谓词使用，account 供日志/审计（需可序列化以落 SPRING_SESSION_ATTRIBUTE）。 */
    public record AppPrincipal(String userId, String account) implements java.io.Serializable {}

    /**
     * Spring Session JDBC 的属性序列化（spike发现#10：SS 4.1 默认 ConversionService 缺
     * Object→byte[] 转换器，SecurityContext 落库 ConversionFailed）。按 Bean 名注入 JDK 序列化转换。
     */
    @Bean
    public org.springframework.core.convert.ConversionService springSessionConversionService() {
        org.springframework.core.convert.support.GenericConversionService cs =
                new org.springframework.core.convert.support.GenericConversionService();
        org.springframework.core.convert.support.DefaultConversionService.addDefaultConverters(cs);
        org.springframework.core.serializer.support.SerializingConverter serializer =
                new org.springframework.core.serializer.support.SerializingConverter();
        org.springframework.core.serializer.support.DeserializingConverter deserializer =
                new org.springframework.core.serializer.support.DeserializingConverter();
        cs.addConverter(Object.class, byte[].class, serializer::convert);
        cs.addConverter(byte[].class, Object.class, deserializer::convert);
        return cs;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // argon2id m=19MiB t=2 p=1 盐16B 哈希32B（OWASP 档）；bcrypt(12) 为降级路径，前缀共存渐进重哈希
        Argon2PasswordEncoder argon2 = new Argon2PasswordEncoder(16, 32, 1, 19456, 2);
        BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(12);
        return new DelegatingPasswordEncoder("argon2", Map.of("argon2", argon2, "bcrypt", bcrypt));
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        http.csrf(csrf -> csrf.disable())   // 双保险走 XRequestedWithFilter（契约语义，不用默认 token 机制）
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .logout(logout -> logout.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login",
                                 "/api/v1/events", "/api/v1/health",
                                 "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/api/v1/**").authenticated()
                .anyRequest().permitAll())
            .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authEx) -> {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(objectMapper.writeValueAsString(
                        new GlobalExceptionHandler.ErrorEnvelope(new GlobalExceptionHandler.ErrorBody(
                                ErrorCode.AUTH_003.name(), ErrorCode.AUTH_003.message(), java.util.List.of()))));
            }))
            .addFilterBefore(new XRequestedWithFilter(objectMapper),
                    org.springframework.security.web.access.intercept.AuthorizationFilter.class)
            .addFilterBefore(new RequestIdFilter(),
                    org.springframework.security.web.access.intercept.AuthorizationFilter.class);
        return http.build();
    }
}
