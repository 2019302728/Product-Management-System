package com.example.product.dto;

import lombok.Data;

import java.util.List;

/**
 * 分页响应。
 *
 * @param <T> 列表元素类型
 */
@Data
public class PageResponse<T> {

    /** 当前页（从 1 开始） */
    private Integer page;
    /** 每页大小 */
    private Integer size;
    /** 总条数 */
    private Long total;
    /** 当前页数据 */
    private List<T> items;

    public PageResponse(Integer page, Integer size, Long total, List<T> items) {
        this.page = page;
        this.size = size;
        this.total = total;
        this.items = items;
    }
}
