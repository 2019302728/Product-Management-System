package com.example.product.sharding;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Snowflake ID 生成器单元测试：
 * <ul>
 *   <li>单线程递增、唯一</li>
 *   <li>多线程并发下无重复</li>
 *   <li>非法 workerId / datacenterId 抛异常</li>
 * </ul>
 */
class SnowflakeIdGeneratorTest {

    @Test
    void generatesUniqueIds() {
        SnowflakeIdGenerator gen = new SnowflakeIdGenerator(1, 1);
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            long id = gen.nextId();
            assertTrue(ids.add(id), "duplicate id detected: " + id);
        }
        assertEquals(10_000, ids.size());
    }

    @Test
    void trendIncreasing() {
        SnowflakeIdGenerator gen = new SnowflakeIdGenerator(1, 1);
        long a = gen.nextId();
        long b = gen.nextId();
        long c = gen.nextId();
        assertTrue(a < b);
        assertTrue(b < c);
    }

    @Test
    void concurrentNoDuplicate() throws InterruptedException {
        final SnowflakeIdGenerator gen = new SnowflakeIdGenerator(1, 1);
        final Set<Long> ids = ConcurrentHashMap.newKeySet();
        int threads = 16;
        int perThread = 1000;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    for (int j = 0; j < perThread; j++) {
                        ids.add(gen.nextId());
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        assertTrue(latch.await(30, TimeUnit.SECONDS));
        pool.shutdown();
        assertEquals(threads * perThread, ids.size(), "concurrent id duplicate detected");
    }

    @Test
    void differentWorkerProducesDifferentIds() {
        SnowflakeIdGenerator g1 = new SnowflakeIdGenerator(1, 1);
        SnowflakeIdGenerator g2 = new SnowflakeIdGenerator(2, 1);
        // 同一时刻两个 worker 生成的 id 最低位 workerId 段不同
        assertNotEquals(g1.nextId(), g2.nextId());
    }

    @Test
    void invalidWorkerIdThrows() {
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdGenerator(-1, 1));
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdGenerator(32, 1));
    }

    @Test
    void invalidDatacenterIdThrows() {
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdGenerator(1, -1));
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdGenerator(1, 32));
    }
}
