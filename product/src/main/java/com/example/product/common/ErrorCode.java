package com.example.product.common;

/**
 * 业务错误码。
 *
 * <p>统一错误码命名规则：HTTP 4xx 段为客户端可修正错误，5xx 段为服务端内部错误。
 * code 不与 HTTP 状态码一一对应，由 GlobalExceptionHandler 决定映射关系。</p>
 */
public enum ErrorCode {

    PARAM_INVALID(40001, "参数校验失败"),
    PRODUCT_TYPE_INVALID(40002, "商品类型非法，仅允许 1=实物 2=虚拟 3=素材"),
    PRODUCT_TYPE_IMMUTABLE(40003, "商品类型创建后不允许修改"),
    EXT_NOT_MATCH_TYPE(40004, "扩展字段与商品类型不匹配"),
    STATUS_NOT_ALLOWED(40005, "状态非法或无权限"),
    PRODUCT_NOT_FOUND(40401, "商品不存在"),
    IMAGE_TOO_LARGE(40006, "图片超过 2MB 上限"),
    IMAGE_FORMAT_INVALID(40007, "图片格式非法，仅允许 JPEG/PNG/WebP"),
    IMAGE_BASE64_BROKEN(40008, "Base64 图片损坏"),
    IMAGE_MAGIC_MISMATCH(40009, "图片 magic bytes 与声明的 MIME 不匹配"),
    TOO_MANY_SUB_IMAGES(40010, "副图最多 5 张"),
    DETAIL_TOO_LONG(40011, "商品详情超过 2000 字上限"),
    TITLE_TOO_LONG(40012, "商品标题超过 60 字"),
    SHORT_TITLE_TOO_LONG(40013, "商品短标题超过 120 字"),
    LICENSE_EXPIRED(40014, "授权结束时间必须大于当前时间"),
    STORE_IDS_EMPTY(40015, "适用门店 ID 数组不能为空"),
    CREATOR_ID_REQUIRED(40016, "创建人 ID 必填"),
    FORBIDDEN(40301, "无权限"),
    INTERNAL_ERROR(50000, "服务内部错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
