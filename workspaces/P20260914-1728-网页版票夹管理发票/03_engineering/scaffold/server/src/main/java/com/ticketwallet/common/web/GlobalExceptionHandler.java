package com.ticketwallet.common.web;

import com.ticketwallet.common.error.ApiException;
import com.ticketwallet.common.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

/**
 * 统一异常处理（api-design §2）：失败一律 { "error": { code, message, details[] } }。
 * 5x message 固定文案，绝不泄露堆栈/SQL/异常类名（§6 细则 1）。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public record ErrorEnvelope(ErrorBody error) {}
    public record ErrorBody(String code, String message, List<ApiException.FieldError> details) {}

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorEnvelope> apiException(ApiException e) {
        return ResponseEntity.status(e.code().httpStatus())
                .body(new ErrorEnvelope(new ErrorBody(e.code().name(), e.getMessage(), e.details())));
    }

    /** Bean Validation 失败 → 字段级 details；auth 路径归 AUTH_005，业务路径归 INV_002（api-design §6）。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorEnvelope> validation(MethodArgumentNotValidException e) {
        List<ApiException.FieldError> details = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiException.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        ErrorCode code = isAuthPath() ? ErrorCode.AUTH_005 : ErrorCode.INV_002;
        return ResponseEntity.status(code.httpStatus())
                .body(new ErrorEnvelope(new ErrorBody(code.name(), code.message(), details)));
    }

    /** 请求体不可读（JSON 格式错）按同规则归 422。 */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorEnvelope> unreadable(Exception e) {
        ErrorCode code = isAuthPath() ? ErrorCode.AUTH_005 : ErrorCode.INV_002;
        return ResponseEntity.status(code.httpStatus())
                .body(new ErrorEnvelope(new ErrorBody(code.name(), code.message(), List.of())));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorEnvelope> unexpected(Exception e) {
        log.error("SYS_001 requestId={}", org.slf4j.MDC.get("requestId"), e);
        ErrorCode code = ErrorCode.SYS_001;
        return ResponseEntity.status(code.httpStatus())
                .body(new ErrorEnvelope(new ErrorBody(code.name(), code.message(), List.of())));
    }

    private boolean isAuthPath() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest req = attrs.getRequest();
            return req.getRequestURI() != null && req.getRequestURI().startsWith("/api/v1/auth");
        }
        return false;
    }
}
