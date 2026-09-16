package com.ticketwallet.common.web;

/**
 * 统一成功响应包络（api-design §2）：成功一律 { "data": ... }。
 * 失败包络（{ "error": {code,message,details} }）在 W1 server 骨架的统一异常处理中补齐。
 */
public record ApiResponse<T>(T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(data);
    }
}
