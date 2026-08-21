package com.example.product.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 分页查询参数。
 *
 * <p>page 从 1 开始；size 默认 20，最大 100，避免一次拉取过多行。</p>
 */
@Data
public class PageRequest {

    @Min(1)
    private Integer page = 1;

    @Min(1)
    @Max(100)
    private Integer size = 20;

    /** 转换为 SQL LIMIT 偏移量 */
    public int offset() {
        return (page - 1) * size;
    }

    /** 转换为 SQL LIMIT 行数 */
    public int limit() {
        return size;
    }
}
