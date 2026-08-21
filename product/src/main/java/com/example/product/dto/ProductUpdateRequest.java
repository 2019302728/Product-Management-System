package com.example.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 商品编辑请求。
 *
 * <p>creatorId 和 productType 不允许修改，因此不在此 DTO 中携带：
 * <ul>
 *   <li>creatorId 由路径参数提供</li>
 *   <li>productType 由 Service 从原分表读取后保留原值</li>
 * </ul>
 * </p>
 *
 * <p>请求可同时更新主图、副图以及与原 productType 匹配的扩展字段。</p>
 */
@Data
public class ProductUpdateRequest {

    @NotBlank
    @Size(min = 1, max = 60)
    private String title;

    @Size(max = 120)
    private String shortTitle;

    @NotNull
    @Min(1)
    private Long price;

    @Size(max = 4000)
    private String detail;

    /** 编辑时不允许改状态，状态走 PATCH /status 接口 */
    private Integer status;

    @NotBlank
    private String mainImageBase64;

    @Size(max = 5)
    private List<String> subImageBase64List;

    private PhysicalExtDTO physicalExt;
    private VirtualExtDTO virtualExt;
    private MaterialExtDTO materialExt;
}
