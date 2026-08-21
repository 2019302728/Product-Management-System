package com.example.product.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品图片实体（共享表 product_image，不分片）。
 *
 * <p>image_type=1 为主图（必填，仅一张），image_type=2 为副图（最多 5 张）。
 * 按 Snowflake product_id 关联主表，可保证跨分片全局唯一。</p>
 */
@Data
public class ProductImage {
    /** 自增主键 */
    private Long id;
    /** 关联商品 ID */
    private Long productId;
    /** 图片类型：1=主图 2=副图 */
    private Integer imageType;
    /** 实际 MIME 类型（由 magic bytes 判定，不信任 Data URL 声明） */
    private String mimeType;
    /** Base64 编码图片，含 data:image/...;base64, 前缀 */
    private String imageBase64;
    /** 副图排序，主图固定 0 */
    private Integer sort;
    /** 创建时间 */
    private LocalDateTime createdAt;
}
