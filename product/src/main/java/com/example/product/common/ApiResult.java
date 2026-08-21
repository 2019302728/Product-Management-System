package com.example.product.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 统一 API 响应包装类。
 *
 * <pre>
 * 成功：{"code":0,"message":"ok","data":...}
 * 失败：{"code":40001,"message":"参数校验失败","data":null}
 * </pre>
 *
 * @param <T> 业务数据类型
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResult<T> {

    /** 0 表示成功，非 0 表示失败（对应 ErrorCode.code） */
    private int code;
    /** 提示信息 */
    private String message;
    /** 业务数据 */
    private T data;

    public ApiResult(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(0, "ok", data);
    }

    public static <T> ApiResult<T> fail(ErrorCode errorCode) {
        return new ApiResult<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> ApiResult<T> fail(ErrorCode errorCode, String customMessage) {
        return new ApiResult<>(errorCode.getCode(), customMessage, null);
    }
}
