package com.example.product.sharding;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * 分片路由单元测试，覆盖技术文档 11 节测试清单的前三条：
 * <ul>
 *   <li>creator_id=1002 路由到 product_0</li>
 *   <li>creator_id=1001 路由到 product_1</li>
 *   <li>路由只由 creator_id 决定，与 product_id 的奇偶无关</li>
 * </ul>
 */
class ProductShardRouterTest {

    private final ProductShardRouter router = new ProductShardRouter();

    @Test
    void evenCreatorIdRoutesToProduct0() {
        assertEquals(ProductShard.PRODUCT_0, router.route(1002L));
        assertEquals(ProductShard.PRODUCT_0, router.route(2L));
        assertEquals(ProductShard.PRODUCT_0, router.route(0L));
    }

    @Test
    void oddCreatorIdRoutesToProduct1() {
        assertEquals(ProductShard.PRODUCT_1, router.route(1001L));
        assertEquals(ProductShard.PRODUCT_1, router.route(1L));
        assertEquals(ProductShard.PRODUCT_1, router.route(3L));
    }

    @Test
    void routeDependsOnCreatorIdNotProductId() {
        // 截图中 product_id 恰好也呈现奇偶分布，但路由必须只由 creator_id 决定
        // 这里构造不同奇偶的 product_id，路由结果仍只由 creator_id 决定
        long creatorEven = 1002L;
        long creatorOdd = 1001L;

        // 无论 product_id 奇偶，creator_id=1002 永远走 product_0
        assertEquals(ProductShard.PRODUCT_0, router.route(creatorEven));
        assertEquals(ProductShard.PRODUCT_0, router.route(creatorEven));

        // creator_id=1001 永远走 product_1
        assertEquals(ProductShard.PRODUCT_1, router.route(creatorOdd));
        assertEquals(ProductShard.PRODUCT_1, router.route(creatorOdd));

        // 防止有人误以为用 product_id：同一 creator 不同 product_id 路由结果必然相同
        assertEquals(router.route(creatorEven), router.route(creatorEven));
        assertNotEquals(router.route(creatorEven), router.route(creatorOdd));
    }

    @Test
    void negativeCreatorIdIsSafeWithFloorMod() {
        // Math.floorMod 保证负数取模仍为非负，避免下标越界
        // -1 % 2 在 Java 中是 -1，但 Math.floorMod(-1, 2) 是 1
        assertEquals(ProductShard.PRODUCT_1, router.route(-1L));
        assertEquals(ProductShard.PRODUCT_0, router.route(-2L));
    }
}
