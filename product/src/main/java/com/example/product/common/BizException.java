package com.example.product.common;

import lombok.Getter;

/**
 * 业务异常，用于在 Service 层抛出可预期的业务错误。
 *
 * <p>由 {@code GlobalExceptionHandler} 捕获并转换为统一 {@link ApiResult} 响应。</p>
 */
@Getter
public class BizException extends RuntimeException {

    private final ErrorCode errorCode;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BizException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }
}
