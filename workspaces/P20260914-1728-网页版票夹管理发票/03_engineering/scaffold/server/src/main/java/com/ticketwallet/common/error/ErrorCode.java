package com.ticketwallet.common.error;

import org.springframework.http.HttpStatus;

/**
 * 全站业务错误码（api-design §6：15 码 + AUTH_006 补充码 = 16 码，唯一实现点）。
 * message 为用户可读唯一口径；5xx 永不泄露内部细节。
 */
public enum ErrorCode {

    AUTH_001(HttpStatus.UNAUTHORIZED, "账号或密码错误"),
    AUTH_002(HttpStatus.LOCKED, "账号已锁定，请稍后重试"),
    AUTH_003(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录"),
    AUTH_004(HttpStatus.CONFLICT, "该邮箱/手机号已注册，请直接登录"),
    AUTH_005(HttpStatus.UNPROCESSABLE_ENTITY, "账号或密码格式不正确"),
    AUTH_006(HttpStatus.FORBIDDEN, "请从票夹通页面发起操作"),

    INV_001(HttpStatus.NOT_FOUND, "记录不存在或已删除"),
    INV_002(HttpStatus.UNPROCESSABLE_ENTITY, "提交内容有误，请检查后重试"),
    INV_003(HttpStatus.UNPROCESSABLE_ENTITY, "筛选条件有误"),

    ATT_001(HttpStatus.UNPROCESSABLE_ENTITY, "单个附件不能超过 10MB"),
    ATT_002(HttpStatus.UNPROCESSABLE_ENTITY, "每条发票最多 3 个附件"),
    ATT_003(HttpStatus.UNPROCESSABLE_ENTITY, "仅支持图片（jpg/png/webp）或 PDF"),

    EXP_001(HttpStatus.UNPROCESSABLE_ENTITY, "超过单次导出上限 5000 条，请缩小筛选范围"),

    RATE_001(HttpStatus.TOO_MANY_REQUESTS, "操作太频繁，请稍后再试"),

    SYS_001(HttpStatus.INTERNAL_SERVER_ERROR, "服务开小差了，请稍后重试"),
    SYS_002(HttpStatus.SERVICE_UNAVAILABLE, "服务暂不可用，请稍后重试");

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public HttpStatus httpStatus() { return httpStatus; }
    public String message() { return message; }
}
