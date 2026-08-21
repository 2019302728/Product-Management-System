package com.example.product.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商业化素材扩展实体（product_type=3，product_material_ext）。
 */
@Data
public class ProductMaterialExt {
    /** 关联商品 ID */
    private Long productId;
    /** 媒体类型：1=图片 2=视频 3=音频 */
    private Integer mediaType;
    /** 素材文件 URL */
    private String mediaUrl;
    /** 授权结束时间，NULL 表示永久 */
    private LocalDateTime licenseEndTime;
    /** 低频临时扩展字段，L0 保持 NULL */
    private String extensionJson;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}
