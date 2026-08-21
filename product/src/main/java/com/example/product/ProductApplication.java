package com.example.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 商品管理后端启动类（L0 简单分片版）。
 *
 * <p>技术依赖仅包含 Spring Boot、MyBatis、MySQL 和 Caffeine。
 * 不引入 Redis、MQ、Elasticsearch 或分布式数据库中间件。</p>
 */
@SpringBootApplication
@MapperScan("com.example.product.repository")
public class ProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductApplication.class, args);
    }
}
