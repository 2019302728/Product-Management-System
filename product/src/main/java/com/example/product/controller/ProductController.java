package com.example.product.controller;

import com.example.product.common.ApiResult;
import com.example.product.dto.PageRequest;
import com.example.product.dto.PageResponse;
import com.example.product.dto.ProductCreateRequest;
import com.example.product.dto.ProductImageResponse;
import com.example.product.dto.ProductListItem;
import com.example.product.dto.ProductMetadata;
import com.example.product.dto.ProductStatusRequest;
import com.example.product.dto.ProductUpdateRequest;
import com.example.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 商品管理 REST 接口。
 *
 * <p>路由依赖 creator_id：除创建外所有单商品接口都必须携带 creatorId 路径参数。</p>
 *
 * <table>
 *   <tr><th>方法</th><th>路径</th><th>功能</th></tr>
 *   <tr><td>POST</td><td>/api/v1/products</td><td>发布商品</td></tr>
 *   <tr><td>GET</td><td>/api/v1/creators/{creatorId}/products</td><td>查询某创建人的商品列表</td></tr>
 *   <tr><td>GET</td><td>/api/v1/creators/{creatorId}/products/{productId}</td><td>查询单商品元数据 + 类型扩展</td></tr>
 *   <tr><td>GET</td><td>/api/v1/creators/{creatorId}/products/{productId}/images</td><td>查询主图和副图 Base64</td></tr>
 *   <tr><td>PUT</td><td>/api/v1/creators/{creatorId}/products/{productId}</td><td>编辑商品</td></tr>
 *   <tr><td>PATCH</td><td>/api/v1/creators/{creatorId}/products/{productId}/status</td><td>修改状态</td></tr>
 *   <tr><td>DELETE</td><td>/api/v1/creators/{creatorId}/products/{productId}</td><td>软删除商品</td></tr>
 *   <tr><td>GET</td><td>/api/v1/admin/products</td><td>管理员跨创建人列表（UNION ALL）</td></tr>
 * </table>
 */
@RestController
@RequestMapping("/api/v1")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * 发布商品。creatorId 从登录上下文取得（请求头 X-User-Id）。
     */
    @PostMapping("/products")
    public ApiResult<Map<String, Long>> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        Long productId = productService.createProduct(request);
        Map<String, Long> data = new HashMap<>();
        data.put("productId", productId);
        return ApiResult.ok(data);
    }

    /**
     * 查询某创建人的商品列表，只查对应分表。
     */
    @GetMapping("/creators/{creatorId}/products")
    public ApiResult<PageResponse<ProductListItem>> listByCreator(
            @PathVariable Long creatorId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        PageRequest pageRequest = new PageRequest();
        pageRequest.setPage(page);
        pageRequest.setSize(size);
        return ApiResult.ok(productService.listByCreator(creatorId, pageRequest));
    }

    /**
     * 查询单商品元数据和类型扩展（带 Caffeine 缓存）。
     */
    @GetMapping("/creators/{creatorId}/products/{productId}")
    public ApiResult<ProductMetadata> getProduct(@PathVariable Long creatorId,
                                                  @PathVariable Long productId) {
        return ApiResult.ok(productService.getProduct(creatorId, productId));
    }

    /**
     * 单独查询主图和副图 Base64。
     */
    @GetMapping("/creators/{creatorId}/products/{productId}/images")
    public ApiResult<ProductImageResponse> getImages(@PathVariable Long creatorId,
                                                      @PathVariable Long productId) {
        return ApiResult.ok(productService.getImages(creatorId, productId));
    }

    /**
     * 编辑商品。creator_id 和 product_type 不允许修改。
     */
    @PutMapping("/creators/{creatorId}/products/{productId}")
    public ApiResult<Void> updateProduct(@PathVariable Long creatorId,
                                          @PathVariable Long productId,
                                          @Valid @RequestBody ProductUpdateRequest request) {
        productService.updateProduct(creatorId, productId, request);
        return ApiResult.ok(null);
    }

    /**
     * 修改状态：1=上架 2=下架 3=处罚（仅管理员）。
     */
    @PatchMapping("/creators/{creatorId}/products/{productId}/status")
    public ApiResult<Void> updateStatus(@PathVariable Long creatorId,
                                        @PathVariable Long productId,
                                        @Valid @RequestBody ProductStatusRequest request) {
        productService.updateStatus(creatorId, productId, request.getStatus());
        return ApiResult.ok(null);
    }

    /**
     * 软删除商品：只写入 deleted_at，不物理删除。
     */
    @DeleteMapping("/creators/{creatorId}/products/{productId}")
    public ApiResult<Void> softDelete(@PathVariable Long creatorId,
                                       @PathVariable Long productId) {
        productService.softDelete(creatorId, productId);
        return ApiResult.ok(null);
    }

    /**
     * 管理员跨创建人列表，使用 UNION ALL 合并两张分表。
     */
    @GetMapping("/admin/products")
    public ApiResult<PageResponse<ProductListItem>> listAllForAdmin(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        PageRequest pageRequest = new PageRequest();
        pageRequest.setPage(page);
        pageRequest.setSize(size);
        return ApiResult.ok(productService.listAllForAdmin(pageRequest));
    }
}
