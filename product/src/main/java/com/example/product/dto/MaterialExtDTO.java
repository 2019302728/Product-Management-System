package com.example.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商业化素材扩展字段（product_type=3）。
 *
 * <ul>
 *   <li>mediaType：媒体类型 1=图片 2=视频 3=音频</li>
 *   <li>mediaUrl：素材文件 URL，必填</li>
 *   <li>licenseEndTime：授权结束时间，为空表示永久</li>
 * </ul>
 */
@Data
public class MaterialExtDTO {
    @NotNull
    private Integer mediaType;

    @NotBlank
    private String mediaUrl;

    /** 授权结束时间，为空表示永久；非空时必须大于当前时间 */
    private LocalDateTime licenseEndTime;
}
