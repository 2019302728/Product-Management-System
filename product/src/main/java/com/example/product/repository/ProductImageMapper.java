package com.example.product.repository;

import com.example.product.model.ProductImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品图片共享表 Mapper（product_image，不分片）。
 *
 * <p>一次发布最多包含 1 张主图和 5 张副图；编辑时先按 product_id 全删再批量插入。</p>
 */
@Mapper
public interface ProductImageMapper {

    /** 批量插入图片（主图 + 副图） */
    int insertBatch(@Param("images") List<ProductImage> images);

    /** 编辑时先全删旧图，再插入新图（同一事务内） */
    int deleteByProductId(@Param("productId") long productId);

    /** 查询某商品的全部图片（主图 + 副图），按 sort 升序 */
    List<ProductImage> selectByProductId(@Param("productId") long productId);

    /** 查询主图（image_type=1） */
    ProductImage selectMainImage(@Param("productId") long productId);

    /** 查询副图列表（image_type=2），按 sort 升序 */
    List<ProductImage> selectSubImages(@Param("productId") long productId);
}
