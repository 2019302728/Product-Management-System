package com.example.product.repository;

import com.example.product.dto.ProductListItem;
import com.example.product.model.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * product_1 分片 Mapper（creator_id 为奇数时路由到此表）。
 *
 * <p>注意：本 Mapper 不再重复定义 UNION ALL 全局列表，那份 SQL 只在
 * {@link ProductShard0Mapper} 中存在一份，避免重复维护。</p>
 */
@Mapper
public interface ProductShard1Mapper {

    int insert(Product product);

    Product selectByCreatorAndId(@Param("creatorId") long creatorId,
                                 @Param("productId") long productId);

    List<ProductListItem> selectListByCreator(@Param("creatorId") long creatorId,
                                              @Param("offset") int offset,
                                              @Param("limit") int limit);

    long countByCreator(@Param("creatorId") long creatorId);

    int update(Product product);

    int updateStatus(@Param("creatorId") long creatorId,
                     @Param("productId") long productId,
                     @Param("status") int status,
                     @Param("updatedAt") LocalDateTime updatedAt);

    int softDelete(@Param("creatorId") long creatorId,
                   @Param("productId") long productId,
                   @Param("deletedAt") LocalDateTime deletedAt);
}
