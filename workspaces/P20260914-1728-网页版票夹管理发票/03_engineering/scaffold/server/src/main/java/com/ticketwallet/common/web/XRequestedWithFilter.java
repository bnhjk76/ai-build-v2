package com.ticketwallet.common.web;

import tools.jackson.databind.ObjectMapper;  // Boot 4 = Jackson 3（spike发现#9）
import com.ticketwallet.common.error.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * CSRF 双保险之第二道（tech-stack §3.5）：/api/v1 下非幂等写方法必须携带
 * X-Requested-With: XMLHttpRequest，缺失 → 403 AUTH_006（api-design §6 细则 4）。
 * 豁免：POST /events（sendBeacon 物理上无法带自定义头，api-design §4.6）。
 */
public class XRequestedWithFilter extends OncePerRequestFilter {

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PATCH", "DELETE", "PUT");
    private static final Set<String> EXEMPT_PATHS = Set.of("/api/v1/events");

    private final ObjectMapper objectMapper;

    public XRequestedWithFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path != null && path.startsWith("/api/v1/")
                && WRITE_METHODS.contains(request.getMethod())
                && !EXEMPT_PATHS.contains(path)
                && !"XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            response.setStatus(ErrorCode.AUTH_006.httpStatus().value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(
                    new GlobalExceptionHandler.ErrorEnvelope(new GlobalExceptionHandler.ErrorBody(
                            ErrorCode.AUTH_006.name(), ErrorCode.AUTH_006.message(), java.util.List.of()))));
            return;
        }
        chain.doFilter(request, response);
    }
}
