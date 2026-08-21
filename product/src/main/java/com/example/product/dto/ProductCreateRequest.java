package com.example.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 商品发布请求。
 *
 * <p>creatorId 不在请求体中携带，由登录上下文取得。</p>
 *
 * <p>productType 决定使用哪一组扩展字段：</p>
 * <ul>
 *   <li>1=实物 -> physicalExt</li>
 *   <li>2=虚拟 -> virtualExt</li>
 *   <li>3=素材 -> materialExt</li>
 * </ul>
 * <p>请求只能携带与商品类型匹配的一组扩展字段，由 Service 在校验阶段强制约束。</p>
 */
@Data
public class ProductCreateRequest {

    @NotNull
    private Integer productType;

    @NotBlank
    @Size(min = 1, max = 60)
    private String title;

    @Size(max = 120)
    private String shortTitle;

    @NotNull
    @Min(1)
    private Long price;

    /** 商品详情富文本，去除 HTML 标签后最多 2000 字；由 Service 端过滤危险 HTML */
    @Size(max = 4000)
    private String detail;

    /** 发布时只允许 1=上架、2=下架；3=处罚只能由管理员操作 */
    @NotNull
    private Integer status;

    /** 主图 Base64，必填，解码后不超过 2MB；格式：data:image/...;base64,... */
    @NotBlank
    private String mainImageBase64;

    /** 副图 Base64 列表，选填，最多 5 张，每张解码后不超过 2MB */
    @Size(max = 5)
    private List<String> subImageBase64List;

    private PhysicalExtDTO physicalExt;
    private VirtualExtDTO virtualExt;
    private MaterialExtDTO materialExt;
}
