package com.example.product.sharding;

import org.springframework.stereotype.Component;

/**
 * 商品主表分片路由器。
 *
 * <p>路由函数：{@code Math.floorMod(creatorId, 2) == 0 ? PRODUCT_0 : PRODUCT_1}。
 * 使用 {@link Math#floorMod(long, int)} 而非 {@code %} 是为了兼容负数 creatorId，
 * 避免出现负数取模导致下标越界。</p>
 *
 * <p>该组件不依赖任何外部配置，纯函数式路由；可在单元测试中直接构造。</p>
 */
@Component
public class ProductShardRouter {

    /**
     * 根据 creatorId 计算目标分片。
     *
     * @param creatorId 创建人 ID
     * @return 目标分片枚举，永不为 null
     */
    public ProductShard route(long creatorId) {
        return Math.floorMod(creatorId, 2) == 0
                ? ProductShard.PRODUCT_0
                : ProductShard.PRODUCT_1;
    }
}
