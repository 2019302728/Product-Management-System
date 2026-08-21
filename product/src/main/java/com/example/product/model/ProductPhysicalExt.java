package com.example.product.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 实物商品扩展实体（product_type=1，product_physical_ext）。
 */
@Data
public class ProductPhysicalExt {
    /** 关联商品 ID */
    private Long productId;
    /** 库存数量，>= 0 */
    private Integer stock;
    /** 发货方式：1=快递 2=EMS 3=自配送 */
    private Integer deliveryType;
    /** 退货规则：1=支持 2=不支持 */
    private Integer refundRule;
    /** 低频临时扩展字段，L0 保持 NULL */
    private String extensionJson;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}
