package com.example.product.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品列表项（不包含 image_base64）。
 *
 * <p>列表查询只返回 ID、创建人、类型、标题、价格、状态，
 * 避免列表查询一次返回大量 Base64。图片通过独立接口获取。</p>
 */
@Data
public class ProductListItem {

    private Long productId;
    private Long creatorId;
    private Integer productType;
    private String title;
    private String shortTitle;
    private Long price;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
