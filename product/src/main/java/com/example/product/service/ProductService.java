package com.example.product.service;

import com.example.product.common.BizException;
import com.example.product.common.ErrorCode;
import com.example.product.config.CaffeineConfig;
import com.example.product.context.LoginContext;
import com.example.product.dto.MaterialExtDTO;
import com.example.product.dto.PageRequest;
import com.example.product.dto.PageResponse;
import com.example.product.dto.PhysicalExtDTO;
import com.example.product.dto.ProductCreateRequest;
import com.example.product.dto.ProductImageResponse;
import com.example.product.dto.ProductListItem;
import com.example.product.dto.ProductMetadata;
import com.example.product.dto.ProductUpdateRequest;
import com.example.product.dto.VirtualExtDTO;
import com.example.product.model.Product;
import com.example.product.model.ProductImage;
import com.example.product.model.ProductMaterialExt;
import com.example.product.model.ProductPhysicalExt;
import com.example.product.model.ProductVirtualExt;
import com.example.product.repository.ProductExtensionMapper;
import com.example.product.repository.ProductImageMapper;
import com.example.product.repository.ProductShard0Mapper;
import com.example.product.repository.ProductShard1Mapper;
import com.example.product.sharding.ProductShard;
import com.example.product.sharding.ProductShardRouter;
import com.example.product.sharding.SnowflakeIdGenerator;
import com.github.benmanes.caffeine.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 商品服务核心实现，统一负责发布、查询、编辑、状态和软删除流程。
 *
 * <p>所有写操作都在 {@link Transactional} 中执行；主表、副图和扩展表
 * 在同一个 MySQL 本地事务中保证同时成功或同时回滚。</p>
 *
 * <p>缓存失效统一在事务提交后触发：使用 {@link TransactionSynchronizationManager}
 * 注册 afterCommit 回调；事务回滚时不会失效缓存。</p>
 */
@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductShardRouter shardRouter;
    private final SnowflakeIdGenerator idGenerator;
    private final ProductShard0Mapper shard0Mapper;
    private final ProductShard1Mapper shard1Mapper;
    private final ProductImageMapper imageMapper;
    private final ProductExtensionMapper extMapper;
    private final Cache<String, ProductMetadata> metadataCache;
    private final ProductValidator validator;
    private final LoginContext loginContext;

    public ProductService(ProductShardRouter shardRouter,
                          SnowflakeIdGenerator idGenerator,
                          ProductShard0Mapper shard0Mapper,
                          ProductShard1Mapper shard1Mapper,
                          ProductImageMapper imageMapper,
                          ProductExtensionMapper extMapper,
                          Cache<String, ProductMetadata> metadataCache,
                          ProductValidator validator,
                          LoginContext loginContext) {
        this.shardRouter = shardRouter;
        this.idGenerator = idGenerator;
        this.shard0Mapper = shard0Mapper;
        this.shard1Mapper = shard1Mapper;
        this.imageMapper = imageMapper;
        this.extMapper = extMapper;
        this.metadataCache = metadataCache;
        this.validator = validator;
        this.loginContext = loginContext;
    }

    // ================= 发布 =================

    /**
     * 发布商品。
     *
     * <p>流程：</p>
     * <ol>
     *   <li>从登录上下文取得 creator_id，计算目标分表</li>
     *   <li>校验公共字段、Base64 图片、类型扩展字段</li>
     *   <li>生成 Snowflake product_id 和毫秒时间戳</li>
     *   <li>开启 MySQL 事务</li>
     *   <li>写入目标 product_0 或 product_1</li>
     *   <li>批量写入 product_image</li>
     *   <li>按 product_type 写入一张扩展表</li>
     *   <li>提交事务并返回 product_id</li>
     * </ol>
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createProduct(ProductCreateRequest request) {
        Long creatorId = loginContext.currentCreatorId();
        return doCreate(creatorId, request);
    }

    /**
     * 管理员代发的内部入口：creatorId 由调用方提供（通常从登录态或路径参数取得）。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createProductAs(Long creatorId, ProductCreateRequest request) {
        if (creatorId == null) {
            throw new BizException(ErrorCode.CREATOR_ID_REQUIRED);
        }
        return doCreate(creatorId, request);
    }

    private Long doCreate(Long creatorId, ProductCreateRequest request) {
        validator.validateProductType(request.getProductType());
        validator.validatePublishStatus(request.getStatus());
        validator.validateExtensionMatch(request);

        LocalDateTime now = LocalDateTime.now();
        Long productId = idGenerator.nextId();
        ProductShard shard = shardRouter.route(creatorId);

        String sanitizedDetail = validator.sanitizeDetail(request.getDetail());
        List<String> mimes = validator.validateImages(
                request.getMainImageBase64(), request.getSubImageBase64List());

        // 1) 写入主表
        Product product = new Product();
        product.setProductId(productId);
        product.setCreatorId(creatorId);
        product.setProductType(request.getProductType());
        product.setTitle(request.getTitle().trim());
        product.setShortTitle(request.getShortTitle() == null ? null : request.getShortTitle().trim());
        product.setPrice(request.getPrice());
        product.setDetail(sanitizedDetail);
        product.setStatus(request.getStatus());
        product.setCreatedAt(now);
        product.setUpdatedAt(now);
        product.setDeletedAt(null);
        insertIntoShard(shard, product);

        // 2) 批量写入图片（主图 + 副图）
        List<ProductImage> images = buildImages(productId, request, mimes, now);
        if (!images.isEmpty()) {
            imageMapper.insertBatch(images);
        }

        // 3) 按类型写入一张扩展表
        insertExtension(request, productId, now);

        log.info("product created: productId={}, creatorId={}, shard={}, type={}",
                productId, creatorId, shard, request.getProductType());
        return productId;
    }

    // ================= 查询 =================

    /**
     * 查询某创建人的商品列表，只查对应分表。
     */
    public PageResponse<ProductListItem> listByCreator(Long creatorId, PageRequest page) {
        if (creatorId == null) {
            throw new BizException(ErrorCode.CREATOR_ID_REQUIRED);
        }
        ProductShard shard = shardRouter.route(creatorId);
        List<ProductListItem> items = selectListByCreator(shard, creatorId,
                page.offset(), page.limit());
        long total = countByCreator(shard, creatorId);
        return new PageResponse<>(page.getPage(), page.getSize(), total, items);
    }

    /**
     * 查询单商品元数据和类型扩展（带 Caffeine 缓存）。
     *
     * <p>缓存未命中时查 MySQL 并回填；命中时直接返回。</p>
     */
    public ProductMetadata getProduct(Long creatorId, Long productId) {
        if (creatorId == null || productId == null) {
            throw new BizException(ErrorCode.CREATOR_ID_REQUIRED);
        }
        String cacheKey = CaffeineConfig.cacheKey(creatorId, productId);
        ProductMetadata cached = metadataCache.getIfPresent(cacheKey);
        if (cached != null) {
            log.debug("cache hit: {}", cacheKey);
            return cached;
        }
        ProductShard shard = shardRouter.route(creatorId);
        Product product = selectByCreatorAndId(shard, creatorId, productId);
        if (product == null) {
            throw new BizException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        ProductMetadata metadata = buildMetadata(product);
        metadataCache.put(cacheKey, metadata);
        return metadata;
    }

    /**
     * 单独查询主图和副图 Base64，不进缓存。
     */
    public ProductImageResponse getImages(Long creatorId, Long productId) {
        if (creatorId == null || productId == null) {
            throw new BizException(ErrorCode.CREATOR_ID_REQUIRED);
        }
        ProductShard shard = shardRouter.route(creatorId);
        // 校验商品确实存在且属于该创建人
        Product product = selectByCreatorAndId(shard, creatorId, productId);
        if (product == null) {
            throw new BizException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        ProductImage main = imageMapper.selectMainImage(productId);
        List<ProductImage> subs = imageMapper.selectSubImages(productId);
        ProductImageResponse resp = new ProductImageResponse();
        resp.setMainImageBase64(main == null ? null : main.getImageBase64());
        resp.setSubImageBase64List(subs.stream().map(ProductImage::getImageBase64).toList());
        return resp;
    }

    /**
     * 管理员跨创建人列表，UNION ALL 两张分表。
     */
    public PageResponse<ProductListItem> listAllForAdmin(PageRequest page) {
        if (!loginContext.isAdmin()) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅管理员可查询跨创建人列表");
        }
        List<ProductListItem> items = shard0Mapper.selectListAllForAdmin(
                page.offset(), page.limit());
        long total = shard0Mapper.countAllForAdmin();
        return new PageResponse<>(page.getPage(), page.getSize(), total, items);
    }

    // ================= 编辑 =================

    /**
     * 编辑商品。creator_id 和 product_type 不允许修改。
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateProduct(Long creatorId, Long productId, ProductUpdateRequest request) {
        if (creatorId == null || productId == null) {
            throw new BizException(ErrorCode.CREATOR_ID_REQUIRED);
        }
        ProductShard shard = shardRouter.route(creatorId);
        Product origin = selectByCreatorAndId(shard, creatorId, productId);
        if (origin == null) {
            throw new BizException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        // creator_id、product_type 不可改
        Integer productType = origin.getProductType();

        LocalDateTime now = LocalDateTime.now();
        String sanitizedDetail = validator.sanitizeDetail(request.getDetail());
        List<String> mimes = validator.validateImages(
                request.getMainImageBase64(), request.getSubImageBase64List());

        // 1) 更新主表
        Product toUpdate = new Product();
        toUpdate.setProductId(productId);
        toUpdate.setCreatorId(creatorId);
        toUpdate.setTitle(request.getTitle().trim());
        toUpdate.setShortTitle(request.getShortTitle() == null ? null : request.getShortTitle().trim());
        toUpdate.setPrice(request.getPrice());
        toUpdate.setDetail(sanitizedDetail);
        toUpdate.setUpdatedAt(now);
        updateIntoShard(shard, toUpdate);

        // 2) 替换副图：先全删旧图，再插入新图
        imageMapper.deleteByProductId(productId);
        List<ProductImage> images = buildImagesForUpdate(productId, productType, request, mimes, now);
        if (!images.isEmpty()) {
            imageMapper.insertBatch(images);
        }

        // 3) 更新对应扩展表
        updateExtension(request, productId, productType, now);

        invalidateAfterCommit(creatorId, productId);
        log.info("product updated: productId={}, creatorId={}", productId, creatorId);
    }

    // ================= 状态 =================

    /**
     * 修改状态。3=处罚仅管理员可操作，由本方法校验。
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long creatorId, Long productId, Integer status) {
        if (creatorId == null || productId == null) {
            throw new BizException(ErrorCode.CREATOR_ID_REQUIRED);
        }
        validator.validateStatus(status);
        if (status == ProductValidator.STATUS_PUNISHED && !loginContext.isAdmin()) {
            throw new BizException(ErrorCode.FORBIDDEN, "处罚状态仅管理员可设置");
        }
        ProductShard shard = shardRouter.route(creatorId);
        Product origin = selectByCreatorAndId(shard, creatorId, productId);
        if (origin == null) {
            throw new BizException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        LocalDateTime now = LocalDateTime.now();
        updateStatusIntoShard(shard, creatorId, productId, status, now);
        invalidateAfterCommit(creatorId, productId);
        log.info("product status changed: productId={}, status={}", productId, status);
    }

    // ================= 软删除 =================

    /**
     * 软删除：只写入 deleted_at，不物理删除主表、副图和扩展数据。
     */
    @Transactional(rollbackFor = Exception.class)
    public void softDelete(Long creatorId, Long productId) {
        if (creatorId == null || productId == null) {
            throw new BizException(ErrorCode.CREATOR_ID_REQUIRED);
        }
        ProductShard shard = shardRouter.route(creatorId);
        Product origin = selectByCreatorAndId(shard, creatorId, productId);
        if (origin == null) {
            throw new BizException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        LocalDateTime now = LocalDateTime.now();
        softDeleteIntoShard(shard, creatorId, productId, now);
        invalidateAfterCommit(creatorId, productId);
        log.info("product soft deleted: productId={}, creatorId={}", productId, creatorId);
    }

    // ================= 分片派发 =================

    private void insertIntoShard(ProductShard shard, Product product) {
        switch (shard) {
            case PRODUCT_0 -> shard0Mapper.insert(product);
            case PRODUCT_1 -> shard1Mapper.insert(product);
        }
    }

    private void updateIntoShard(ProductShard shard, Product product) {
        switch (shard) {
            case PRODUCT_0 -> shard0Mapper.update(product);
            case PRODUCT_1 -> shard1Mapper.update(product);
        }
    }

    private void updateStatusIntoShard(ProductShard shard, long creatorId, long productId,
                                       int status, LocalDateTime now) {
        switch (shard) {
            case PRODUCT_0 -> shard0Mapper.updateStatus(creatorId, productId, status, now);
            case PRODUCT_1 -> shard1Mapper.updateStatus(creatorId, productId, status, now);
        }
    }

    private void softDeleteIntoShard(ProductShard shard, long creatorId, long productId,
                                     LocalDateTime now) {
        switch (shard) {
            case PRODUCT_0 -> shard0Mapper.softDelete(creatorId, productId, now);
            case PRODUCT_1 -> shard1Mapper.softDelete(creatorId, productId, now);
        }
    }

    private Product selectByCreatorAndId(ProductShard shard, long creatorId, long productId) {
        return switch (shard) {
            case PRODUCT_0 -> shard0Mapper.selectByCreatorAndId(creatorId, productId);
            case PRODUCT_1 -> shard1Mapper.selectByCreatorAndId(creatorId, productId);
        };
    }

    private List<ProductListItem> selectListByCreator(ProductShard shard, long creatorId,
                                                       int offset, int limit) {
        return switch (shard) {
            case PRODUCT_0 -> shard0Mapper.selectListByCreator(creatorId, offset, limit);
            case PRODUCT_1 -> shard1Mapper.selectListByCreator(creatorId, offset, limit);
        };
    }

    private long countByCreator(ProductShard shard, long creatorId) {
        return switch (shard) {
            case PRODUCT_0 -> shard0Mapper.countByCreator(creatorId);
            case PRODUCT_1 -> shard1Mapper.countByCreator(creatorId);
        };
    }

    // ================= 扩展表派发 =================

    private void insertExtension(ProductCreateRequest req, Long productId, LocalDateTime now) {
        Integer t = req.getProductType();
        if (t == ProductValidator.TYPE_PHYSICAL) {
            extMapper.insertPhysical(validator.toPhysicalExt(productId, req.getPhysicalExt(), now));
        } else if (t == ProductValidator.TYPE_VIRTUAL) {
            extMapper.insertVirtual(validator.toVirtualExt(productId, req.getVirtualExt(), now));
        } else if (t == ProductValidator.TYPE_MATERIAL) {
            extMapper.insertMaterial(validator.toMaterialExt(productId, req.getMaterialExt(), now));
        }
    }

    private void updateExtension(ProductUpdateRequest req, Long productId, Integer productType,
                                 LocalDateTime now) {
        if (productType == ProductValidator.TYPE_PHYSICAL) {
            PhysicalExtDTO dto = req.getPhysicalExt();
            if (dto == null) {
                throw new BizException(ErrorCode.EXT_NOT_MATCH_TYPE, "productType=1 必须携带 physicalExt");
            }
            ProductPhysicalExt ext = validator.toPhysicalExt(productId, dto, now);
            extMapper.updatePhysical(ext);
        } else if (productType == ProductValidator.TYPE_VIRTUAL) {
            VirtualExtDTO dto = req.getVirtualExt();
            if (dto == null) {
                throw new BizException(ErrorCode.EXT_NOT_MATCH_TYPE, "productType=2 必须携带 virtualExt");
            }
            ProductVirtualExt ext = validator.toVirtualExt(productId, dto, now);
            extMapper.updateVirtual(ext);
        } else if (productType == ProductValidator.TYPE_MATERIAL) {
            MaterialExtDTO dto = req.getMaterialExt();
            if (dto == null) {
                throw new BizException(ErrorCode.EXT_NOT_MATCH_TYPE, "productType=3 必须携带 materialExt");
            }
            ProductMaterialExt ext = validator.toMaterialExt(productId, dto, now);
            extMapper.updateMaterial(ext);
        }
    }

    // ================= 图片构建 =================

    private List<ProductImage> buildImages(Long productId, ProductCreateRequest req,
                                           List<String> mimes, LocalDateTime now) {
        return buildImagesInternal(productId, req.getProductType(), req.getMainImageBase64(),
                req.getSubImageBase64List(), mimes, now);
    }

    private List<ProductImage> buildImagesForUpdate(Long productId, Integer productType,
                                                   ProductUpdateRequest req,
                                                   List<String> mimes, LocalDateTime now) {
        return buildImagesInternal(productId, productType, req.getMainImageBase64(),
                req.getSubImageBase64List(), mimes, now);
    }

    private List<ProductImage> buildImagesInternal(Long productId, Integer productType,
                                                   String mainImage, List<String> subImages,
                                                   List<String> mimes, LocalDateTime now) {
        List<ProductImage> images = new ArrayList<>();
        if (mimes == null || mimes.isEmpty()) {
            return images;
        }
        // 第 0 项为主图
        ProductImage main = new ProductImage();
        main.setProductId(productId);
        main.setImageType(1);
        main.setMimeType(mimes.get(0));
        main.setImageBase64(mainImage);
        main.setSort(0);
        main.setCreatedAt(now);
        images.add(main);

        if (subImages != null && !subImages.isEmpty()) {
            for (int i = 0; i < subImages.size(); i++) {
                ProductImage sub = new ProductImage();
                sub.setProductId(productId);
                sub.setImageType(2);
                sub.setMimeType(mimes.get(i + 1));
                sub.setImageBase64(subImages.get(i));
                sub.setSort(i + 1);
                sub.setCreatedAt(now);
                images.add(sub);
            }
        }
        return images;
    }

    // ================= 元数据构建 =================

    private ProductMetadata buildMetadata(Product product) {
        ProductMetadata m = new ProductMetadata();
        m.setProductId(product.getProductId());
        m.setCreatorId(product.getCreatorId());
        m.setProductType(product.getProductType());
        m.setTitle(product.getTitle());
        m.setShortTitle(product.getShortTitle());
        m.setPrice(product.getPrice());
        m.setDetail(product.getDetail());
        m.setStatus(product.getStatus());
        m.setCreatedAt(product.getCreatedAt());
        m.setUpdatedAt(product.getUpdatedAt());

        Long pid = product.getProductId();
        Integer t = product.getProductType();
        if (t == ProductValidator.TYPE_PHYSICAL) {
            ProductPhysicalExt ext = extMapper.selectPhysical(pid);
            if (ext != null) {
                PhysicalExtDTO dto = new PhysicalExtDTO();
                dto.setStock(ext.getStock());
                dto.setDeliveryType(ext.getDeliveryType());
                dto.setRefundRule(ext.getRefundRule());
                m.setPhysicalExt(dto);
            }
        } else if (t == ProductValidator.TYPE_VIRTUAL) {
            ProductVirtualExt ext = extMapper.selectVirtual(pid);
            if (ext != null) {
                VirtualExtDTO dto = new VirtualExtDTO();
                dto.setValidDays(ext.getValidDays());
                dto.setVerificationType(ext.getVerificationType());
                dto.setStoreIds(ext.getStoreIds() == null ? Collections.emptyList() : ext.getStoreIds());
                m.setVirtualExt(dto);
            }
        } else if (t == ProductValidator.TYPE_MATERIAL) {
            ProductMaterialExt ext = extMapper.selectMaterial(pid);
            if (ext != null) {
                MaterialExtDTO dto = new MaterialExtDTO();
                dto.setMediaType(ext.getMediaType());
                dto.setMediaUrl(ext.getMediaUrl());
                dto.setLicenseEndTime(ext.getLicenseEndTime());
                m.setMaterialExt(dto);
            }
        }
        return m;
    }

    // ================= 缓存失效 =================

    /**
     * 在事务提交后再失效缓存；事务回滚时不会失效缓存，保证数据与缓存一致。
     *
     * <p>当无事务上下文时（例如测试环境），直接失效。</p>
     */
    private void invalidateAfterCommit(Long creatorId, Long productId) {
        String cacheKey = CaffeineConfig.cacheKey(creatorId, productId);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    metadataCache.invalidate(cacheKey);
                    log.debug("cache invalidated after commit: {}", cacheKey);
                }
            });
        } else {
            metadataCache.invalidate(cacheKey);
        }
    }
}
