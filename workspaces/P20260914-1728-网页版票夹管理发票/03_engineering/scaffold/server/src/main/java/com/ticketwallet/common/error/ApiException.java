package com.ticketwallet.common.error;

import java.util.List;

/** 业务异常：携带错误码 + 可选字段级错误列表（api-design §2 失败包络）。 */
public class ApiException extends RuntimeException {

    public record FieldError(String field, String message) {}

    private final ErrorCode code;
    private final String message;          // 允许覆盖默认口径（如 AUTH_002 带分钟数）
    private final List<FieldError> details;

    public ApiException(ErrorCode code) {
        this(code, code.message(), List.of());
    }

    public ApiException(ErrorCode code, String message) {
        this(code, message, List.of());
    }

    public ApiException(ErrorCode code, String message, List<FieldError> details) {
        super(message);
        this.code = code;
        this.message = message;
        this.details = details;
    }

    public ErrorCode code() { return code; }
    @Override public String getMessage() { return message; }
    public List<FieldError> details() { return details; }
}
