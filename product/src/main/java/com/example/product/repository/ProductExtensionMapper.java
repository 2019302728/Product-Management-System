package com.example.product.repository;

import com.example.product.model.ProductMaterialExt;
import com.example.product.model.ProductPhysicalExt;
import com.example.product.model.ProductVirtualExt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 商品扩展表 Mapper，统一管理三张扩展表的数据访问。
 *
 * <ul>
 *   <li>{@code product_physical_ext}（product_type=1）</li>
 *   <li>{@code product_virtual_ext}（product_type=2）</li>
 *   <li>{@code product_material_ext}（product_type=3）</li>
 * </ul>
 *
 * <p>Service 在发布时按 productType 只调用一组 insert；编辑时只调用同组的
 * update + 其他两组不会触发；查询时按原 productType 选择对应 select。</p>
 */
@Mapper
public interface ProductExtensionMapper {

    // ---- 实物（product_type=1） ----
    int insertPhysical(ProductPhysicalExt ext);

    int updatePhysical(ProductPhysicalExt ext);

    ProductPhysicalExt selectPhysical(@Param("productId") long productId);

    int deletePhysical(@Param("productId") long productId);

    // ---- 虚拟（product_type=2） ----
    int insertVirtual(ProductVirtualExt ext);

    int updateVirtual(ProductVirtualExt ext);

    ProductVirtualExt selectVirtual(@Param("productId") long productId);

    int deleteVirtual(@Param("productId") long productId);

    // ---- 素材（product_type=3） ----
    int insertMaterial(ProductMaterialExt ext);

    int updateMaterial(ProductMaterialExt ext);

    ProductMaterialExt selectMaterial(@Param("productId") long productId);

    int deleteMaterial(@Param("productId") long productId);
}
