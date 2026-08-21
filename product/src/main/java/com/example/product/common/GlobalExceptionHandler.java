package com.example.product.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器，统一封装为 {@link ApiResult}。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResult<Void>> handleBiz(BizException ex) {
        log.warn("biz exception: code={}, msg={}", ex.getErrorCode().getCode(), ex.getMessage());
        HttpStatus status = mapHttpStatus(ex.getErrorCode().getCode());
        return ResponseEntity.status(status).body(ApiResult.fail(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("validation failed: {}", detail);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.fail(ErrorCode.PARAM_INVALID, detail));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResult<Void>> handleBind(BindException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("bind failed: {}", detail);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.fail(ErrorCode.PARAM_INVALID, detail));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResult<Void>> handleUploadSize(MaxUploadSizeExceededException ex) {
        log.warn("upload size exceeded: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResult.fail(ErrorCode.IMAGE_TOO_LARGE, "请求体超过上限"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResult<Void>> handleIllegalArg(IllegalArgumentException ex) {
        log.warn("illegal argument: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.fail(ErrorCode.PARAM_INVALID, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleAny(Exception ex) {
        log.error("unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.fail(ErrorCode.INTERNAL_ERROR, ex.getMessage()));
    }

    /**
     * 将业务错误码映射到 HTTP 状态码：
     * <ul>
     *   <li>40xxx -> 400 / 404 / 403</li>
     *   <li>50xxx -> 500</li>
     * </ul>
     */
    private HttpStatus mapHttpStatus(int code) {
        if (code == ErrorCode.PRODUCT_NOT_FOUND.getCode()) {
            return HttpStatus.NOT_FOUND;
        }
        if (code == ErrorCode.FORBIDDEN.getCode()) {
            return HttpStatus.FORBIDDEN;
        }
        if (code == ErrorCode.INTERNAL_ERROR.getCode()) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        if (code == ErrorCode.IMAGE_TOO_LARGE.getCode()) {
            return HttpStatus.PAYLOAD_TOO_LARGE;
        }
        return HttpStatus.BAD_REQUEST;
    }
}
