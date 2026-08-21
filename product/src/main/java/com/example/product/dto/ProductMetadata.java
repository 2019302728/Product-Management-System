package com.example.product.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品元数据响应（单商品查询）。
 *
 * <p>包含主表元数据和与 productType 匹配的类型扩展信息。
 * 主图/副图 Base64 不在元数据中返回，需通过独立接口获取。</p>
 *
 * <p>该对象是 Caffeine 缓存的值类型；缓存键：
 * {@code product:metadata:{creatorId}:{productId}}。</p>
 */
@Data
public class ProductMetadata {

    private Long productId;
    private Long creatorId;
    private Integer productType;
    private String title;
    private String shortTitle;
    private Long price;
    private String detail;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 与 productType 匹配的类型扩展（physicalExt / virtualExt / materialExt） */
    private PhysicalExtDTO physicalExt;
    private VirtualExtDTO virtualExt;
    private MaterialExtDTO materialExt;
}
