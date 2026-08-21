package com.example.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 实物商品扩展字段（product_type=1）。
 *
 * <ul>
 *   <li>stock：库存数量，>= 0</li>
 *   <li>deliveryType：发货方式 1=快递 2=EMS 3=自配送</li>
 *   <li>refundRule：退货规则 1=支持 2=不支持</li>
 * </ul>
 */
@Data
public class PhysicalExtDTO {
    @NotNull
    @Min(0)
    private Integer stock;

    @NotNull
    private Integer deliveryType;

    @NotNull
    private Integer refundRule;
}
