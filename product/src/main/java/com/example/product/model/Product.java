package com.example.product.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品主表实体（product_0 / product_1 共用）。
 *
 * <p>不区分具体分表，写入时由 Service 根据路由结果选择对应 Mapper；
 * 读取时同样由路由决定查询目标表。</p>
 */
@Data
public class Product {
    /** 商品 ID（Snowflake 全局唯一） */
    private Long productId;
    /** 创建人 ID，路由依据 creator_id % 2 */
    private Long creatorId;
    /** 商品类型：1=实物 2=虚拟 3=素材 */
    private Integer productType;
    /** 商品标题（1-60 字） */
    private String title;
    /** 商品短标题（最多 120 字） */
    private String shortTitle;
    /** 价格（以分为单位的整数） */
    private Long price;
    /** 商品详情富文本，最多 2000 字 */
    private String detail;
    /** 状态：1=上架 2=下架 3=处罚 */
    private Integer status;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
    /** 软删除时间，NULL 表示未删除 */
    private LocalDateTime deletedAt;
}
