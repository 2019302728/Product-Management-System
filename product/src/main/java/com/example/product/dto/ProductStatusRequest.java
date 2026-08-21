package com.example.product.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 商品状态修改请求。
 *
 * <ul>
 *   <li>1=上架</li>
 *   <li>2=下架</li>
 *   <li>3=处罚（仅管理员可操作）</li>
 * </ul>
 */
@Data
public class ProductStatusRequest {

    @NotNull
    private Integer status;
}
