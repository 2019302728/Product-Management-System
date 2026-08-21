package com.example.product.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 虚拟商品扩展实体（product_type=2，product_virtual_ext）。
 *
 * <p>storeIds 在数据库以 JSON 数组保存，此处以 List&lt;Long&gt; 接收，
 * 由 MyBatis TypeHandler 负责序列化/反序列化。</p>
 */
@Data
public class ProductVirtualExt {
    /** 关联商品 ID */
    private Long productId;
    /** 有效期天数，>= 0 */
    private Integer validDays;
    /** 核销方式：1=扫码 2=密码 3=链接 */
    private Integer verificationType;
    /** 适用门店 ID 数组 */
    private List<Long> storeIds;
    /** 低频临时扩展字段，L0 保持 NULL */
    private String extensionJson;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}
