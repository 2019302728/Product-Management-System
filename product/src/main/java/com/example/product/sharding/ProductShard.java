package com.example.product.sharding;

/**
 * 商品主表分片枚举。
 *
 * <p>路由规则：{@code creator_id % 2 == 0 -> PRODUCT_0}，否则 {@code PRODUCT_1}。
 * 路由必须使用 creator_id，不能使用 product_id：
 * Snowflake product_id 的奇偶不保证与创建人一致。</p>
 */
public enum ProductShard {
    PRODUCT_0,
    PRODUCT_1
}
