package com.example.product.service;

import com.example.product.common.BizException;
import com.example.product.common.ErrorCode;
import com.example.product.common.HtmlSanitizer;
import com.example.product.common.ImageValidator;
import com.example.product.dto.MaterialExtDTO;
import com.example.product.dto.PhysicalExtDTO;
import com.example.product.dto.ProductCreateRequest;
import com.example.product.dto.VirtualExtDTO;
import com.example.product.model.ProductMaterialExt;
import com.example.product.model.ProductPhysicalExt;
import com.example.product.model.ProductVirtualExt;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 商品字段校验器，集中处理发布 / 编辑请求中的参数校验和 DTO 到实体的转换。
 *
 * <p>校验规则遵循技术文档 5.2 节。</p>
 */
@Component
public class ProductValidator {

    /** 商品类型常量 */
    public static final int TYPE_PHYSICAL = 1;
    public static final int TYPE_VIRTUAL = 2;
    public static final int TYPE_MATERIAL = 3;

    /** 状态常量 */
    public static final int STATUS_ONLINE = 1;
    public static final int STATUS_OFFLINE = 2;
    public static final int STATUS_PUNISHED = 3;

    /**
     * 校验商品类型合法。
     */
    public void validateProductType(Integer type) {
        if (type == null
                || type != TYPE_PHYSICAL && type != TYPE_VIRTUAL && type != TYPE_MATERIAL) {
            throw new BizException(ErrorCode.PRODUCT_TYPE_INVALID);
        }
    }

    /**
     * 校验发布时的 status：只允许 1=上架 / 2=下架；3=处罚只能管理员操作（调用方负责权限）。
     */
    public void validatePublishStatus(Integer status) {
        if (status == null
                || status != STATUS_ONLINE && status != STATUS_OFFLINE && status != STATUS_PUNISHED) {
            throw new BizException(ErrorCode.STATUS_NOT_ALLOWED,
                    "status 只允许 1=上架、2=下架、3=处罚");
        }
    }

    /**
     * 校验状态修改请求：1=上架 / 2=下架 / 3=处罚（3 由调用方校验管理员）。
     */
    public void validateStatus(Integer status) {
        if (status == null
                || status != STATUS_ONLINE && status != STATUS_OFFLINE && status != STATUS_PUNISHED) {
            throw new BizException(ErrorCode.STATUS_NOT_ALLOWED);
        }
    }

    /**
     * 校验请求只携带与 productType 匹配的一组扩展字段，且其他两组为空。
     */
    public void validateExtensionMatch(ProductCreateRequest req) {
        int count = 0;
        if (req.getPhysicalExt() != null) count++;
        if (req.getVirtualExt() != null) count++;
        if (req.getMaterialExt() != null) count++;
        if (count != 1) {
            throw new BizException(ErrorCode.EXT_NOT_MATCH_TYPE,
                    "必须且只能携带一组扩展字段（physicalExt/virtualExt/materialExt），当前数量=" + count);
        }
        Integer t = req.getProductType();
        if (t == TYPE_PHYSICAL && req.getPhysicalExt() == null) {
            throw new BizException(ErrorCode.EXT_NOT_MATCH_TYPE, "productType=1 必须携带 physicalExt");
        }
        if (t == TYPE_VIRTUAL && req.getVirtualExt() == null) {
            throw new BizException(ErrorCode.EXT_NOT_MATCH_TYPE, "productType=2 必须携带 virtualExt");
        }
        if (t == TYPE_MATERIAL && req.getMaterialExt() == null) {
            throw new BizException(ErrorCode.EXT_NOT_MATCH_TYPE, "productType=3 必须携带 materialExt");
        }
    }

    /**
     * 校验主图和副图列表，返回真实 MIME 列表（按 main + sub 顺序）。
     *
     * @param mainImage 主图 data URL，必填
     * @param subImages 副图 data URL 列表，可为空
     * @return 真实 MIME 列表，第 0 项为主图
     */
    public List<String> validateImages(String mainImage, List<String> subImages) {
        List<String> mimes = new ArrayList<>();
        mimes.add(ImageValidator.validate(mainImage));
        if (subImages != null && !subImages.isEmpty()) {
            if (subImages.size() > 5) {
                throw new BizException(ErrorCode.TOO_MANY_SUB_IMAGES,
                        "副图最多 5 张，当前 " + subImages.size() + " 张");
            }
            for (String sub : subImages) {
                mimes.add(ImageValidator.validate(sub));
            }
        }
        return mimes;
    }

    /**
     * 净化 detail 字段。
     */
    public String sanitizeDetail(String raw) {
        return HtmlSanitizer.sanitize(raw);
    }

    /**
     * 校验实物扩展并转换为实体。
     */
    public ProductPhysicalExt toPhysicalExt(Long productId, PhysicalExtDTO dto, LocalDateTime now) {
        if (dto.getStock() == null || dto.getStock() < 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "stock 必须 >= 0");
        }
        Integer dt = dto.getDeliveryType();
        if (dt == null || (dt != 1 && dt != 2 && dt != 3)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "deliveryType 只允许 1/2/3");
        }
        Integer rr = dto.getRefundRule();
        if (rr == null || (rr != 1 && rr != 2)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "refundRule 只允许 1/2");
        }
        ProductPhysicalExt ext = new ProductPhysicalExt();
        ext.setProductId(productId);
        ext.setStock(dto.getStock());
        ext.setDeliveryType(dt);
        ext.setRefundRule(rr);
        ext.setExtensionJson(null); // L0 保持 NULL
        ext.setCreatedAt(now);
        ext.setUpdatedAt(now);
        return ext;
    }

    /**
     * 校验虚拟扩展并转换为实体。
     */
    public ProductVirtualExt toVirtualExt(Long productId, VirtualExtDTO dto, LocalDateTime now) {
        if (dto.getValidDays() == null || dto.getValidDays() < 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "validDays 必须 >= 0");
        }
        Integer vt = dto.getVerificationType();
        if (vt == null || (vt != 1 && vt != 2 && vt != 3)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "verificationType 只允许 1/2/3");
        }
        if (dto.getStoreIds() == null || dto.getStoreIds().isEmpty()) {
            throw new BizException(ErrorCode.STORE_IDS_EMPTY);
        }
        ProductVirtualExt ext = new ProductVirtualExt();
        ext.setProductId(productId);
        ext.setValidDays(dto.getValidDays());
        ext.setVerificationType(vt);
        ext.setStoreIds(dto.getStoreIds());
        ext.setExtensionJson(null);
        ext.setCreatedAt(now);
        ext.setUpdatedAt(now);
        return ext;
    }

    /**
     * 校验素材扩展并转换为实体。
     */
    public ProductMaterialExt toMaterialExt(Long productId, MaterialExtDTO dto, LocalDateTime now) {
        Integer mt = dto.getMediaType();
        if (mt == null || (mt != 1 && mt != 2 && mt != 3)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "mediaType 只允许 1/2/3");
        }
        if (dto.getMediaUrl() == null || dto.getMediaUrl().isBlank()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "mediaUrl 必填");
        }
        if (dto.getLicenseEndTime() != null
                && !dto.getLicenseEndTime().isAfter(now)) {
            throw new BizException(ErrorCode.LICENSE_EXPIRED);
        }
        ProductMaterialExt ext = new ProductMaterialExt();
        ext.setProductId(productId);
        ext.setMediaType(mt);
        ext.setMediaUrl(dto.getMediaUrl());
        ext.setLicenseEndTime(dto.getLicenseEndTime());
        ext.setExtensionJson(null);
        ext.setCreatedAt(now);
        ext.setUpdatedAt(now);
        return ext;
    }
}
