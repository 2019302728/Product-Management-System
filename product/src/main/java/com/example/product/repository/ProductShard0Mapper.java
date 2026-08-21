package com.example.product.repository;

import com.example.product.dto.ProductListItem;
import com.example.product.model.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * product_0 分片 Mapper（creator_id 为偶数时路由到此表）。
 *
 * <p>SQL 表名不动态拼接，避免 SQL 注入：每个分片对应独立的 Mapper 与 XML 语句，
 * Service 根据路由枚举选择具体方法调用。</p>
 *
 * <p>跨分片的 UNION ALL 管理员列表只在本 Mapper 中定义一份 SQL，
 * 因为 UNION ALL 是单条 SQL，复用一份即可。</p>
 */
@Mapper
public interface ProductShard0Mapper {

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

    /** 管理员跨分片列表：UNION ALL product_0 + product_1，已排除 deleted_at */
    List<ProductListItem> selectListAllForAdmin(@Param("offset") int offset,
                                                @Param("limit") int limit);

    /** 管理员跨分片计数 */
    long countAllForAdmin();
}
