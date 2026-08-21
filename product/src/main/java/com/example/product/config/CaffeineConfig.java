package com.example.product.config;

import com.example.product.dto.ProductMetadata;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Caffeine 缓存配置。
 *
 * <p>按技术文档 8 节：</p>
 * <ul>
 *   <li>maximumSize = 10_000</li>
 *   <li>expireAfterWrite = 5 分钟</li>
 *   <li>recordStats()：开启命中率统计，便于观测</li>
 * </ul>
 *
 * <p>缓存 key 格式：{@code product:metadata:{creatorId}:{productId}}</p>
 * <p>缓存值类型：{@link ProductMetadata}（仅元数据 + 类型扩展，不缓存图片 Base64）</p>
 *
 * <p>L0 默认只部署一个应用实例；多实例间的缓存失效不在本方案范围。</p>
 */
@Configuration
public class CaffeineConfig {

    /** 缓存键前缀 */
    public static final String CACHE_KEY_PREFIX = "product:metadata:";

    @Bean
    public Cache<String, ProductMetadata> productMetadataCache() {
        return Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofMinutes(5))
                .recordStats()
                .build();
    }

    /**
     * 构造缓存键：product:metadata:{creatorId}:{productId}
     */
    public static String cacheKey(Long creatorId, Long productId) {
        return CACHE_KEY_PREFIX + creatorId + ":" + productId;
    }
}
