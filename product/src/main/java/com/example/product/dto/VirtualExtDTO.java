package com.example.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 虚拟商品扩展字段（product_type=2）。
 *
 * <ul>
 *   <li>validDays：有效期天数，>= 0</li>
 *   <li>verificationType：核销方式 1=扫码 2=密码 3=链接</li>
 *   <li>storeIds：适用门店 ID 数组，必须非空</li>
 * </ul>
 */
@Data
public class VirtualExtDTO {
    @NotNull
    @Min(0)
    private Integer validDays;

    @NotNull
    private Integer verificationType;

    @NotEmpty
    private List<Long> storeIds;
}
