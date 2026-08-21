package com.example.product.dto;

import lombok.Data;

import java.util.List;

/**
 * 商品图片响应（独立接口）。
 *
 * <p>主图 mainImageBase64 必返回；副图列表按 sort 排序。
 * 均为完整 data URL：{@code data:image/...;base64,...}。</p>
 */
@Data
public class ProductImageResponse {

    private String mainImageBase64;
    private List<String> subImageBase64List;
}
