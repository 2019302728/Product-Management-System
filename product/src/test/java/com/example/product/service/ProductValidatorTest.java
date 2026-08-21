package com.example.product.service;

import com.example.product.common.BizException;
import com.example.product.dto.MaterialExtDTO;
import com.example.product.dto.PhysicalExtDTO;
import com.example.product.dto.ProductCreateRequest;
import com.example.product.dto.VirtualExtDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 商品字段校验器单元测试，覆盖技术文档 5.2 节的类型扩展校验。
 */
class ProductValidatorTest {

    private final ProductValidator validator = new ProductValidator();

    @Test
    void validateProductTypeAcceptsOnly123() {
        assertDoesNotThrow(() -> validator.validateProductType(1));
        assertDoesNotThrow(() -> validator.validateProductType(2));
        assertDoesNotThrow(() -> validator.validateProductType(3));
        assertThrows(BizException.class, () -> validator.validateProductType(null));
        assertThrows(BizException.class, () -> validator.validateProductType(0));
        assertThrows(BizException.class, () -> validator.validateProductType(4));
    }

    @Test
    void validatePublishStatusAccepts123() {
        assertDoesNotThrow(() -> validator.validatePublishStatus(1));
        assertDoesNotThrow(() -> validator.validatePublishStatus(2));
        assertDoesNotThrow(() -> validator.validatePublishStatus(3));
        assertThrows(BizException.class, () -> validator.validatePublishStatus(null));
        assertThrows(BizException.class, () -> validator.validatePublishStatus(0));
    }

    @Test
    void extensionMustMatchProductType() {
        ProductCreateRequest req = new ProductCreateRequest();
        req.setProductType(1);

        // type=1 必须携带 physicalExt
        req.setPhysicalExt(new PhysicalExtDTO());
        assertDoesNotThrow(() -> validator.validateExtensionMatch(req));

        // 错配：type=1 携带 virtualExt
        req.setPhysicalExt(null);
        req.setVirtualExt(new VirtualExtDTO());
        assertThrows(BizException.class, () -> validator.validateExtensionMatch(req));

        // 携带多组扩展也非法
        req.setPhysicalExt(new PhysicalExtDTO());
        req.setVirtualExt(new VirtualExtDTO());
        assertThrows(BizException.class, () -> validator.validateExtensionMatch(req));

        // 一组都不携带也非法
        req.setPhysicalExt(null);
        req.setVirtualExt(null);
        req.setMaterialExt(null);
        assertThrows(BizException.class, () -> validator.validateExtensionMatch(req));
    }

    @Test
    void physicalExtValidation() {
        LocalDateTime now = LocalDateTime.now();
        PhysicalExtDTO dto = new PhysicalExtDTO();
        dto.setStock(0);
        dto.setDeliveryType(2);
        dto.setRefundRule(1);
        assertDoesNotThrow(() -> validator.toPhysicalExt(1L, dto, now));

        // stock < 0
        dto.setStock(-1);
        assertThrows(BizException.class, () -> validator.toPhysicalExt(1L, dto, now));
        dto.setStock(0);

        // deliveryType 非法
        dto.setDeliveryType(4);
        assertThrows(BizException.class, () -> validator.toPhysicalExt(1L, dto, now));
        dto.setDeliveryType(2);

        // refundRule 非法
        dto.setRefundRule(3);
        assertThrows(BizException.class, () -> validator.toPhysicalExt(1L, dto, now));
    }

    @Test
    void virtualExtValidation() {
        LocalDateTime now = LocalDateTime.now();
        VirtualExtDTO dto = new VirtualExtDTO();
        dto.setValidDays(30);
        dto.setVerificationType(1);
        dto.setStoreIds(List.of(100L, 200L));
        assertDoesNotThrow(() -> validator.toVirtualExt(1L, dto, now));

        // storeIds 为空抛异常
        dto.setStoreIds(List.of());
        assertThrows(BizException.class, () -> validator.toVirtualExt(1L, dto, now));
        dto.setStoreIds(List.of(100L));

        // verificationType 非法
        dto.setVerificationType(4);
        assertThrows(BizException.class, () -> validator.toVirtualExt(1L, dto, now));
        dto.setVerificationType(1);

        // validDays < 0
        dto.setValidDays(-1);
        assertThrows(BizException.class, () -> validator.toVirtualExt(1L, dto, now));
    }

    @Test
    void materialExtValidation() {
        LocalDateTime now = LocalDateTime.now();
        MaterialExtDTO dto = new MaterialExtDTO();
        dto.setMediaType(1);
        dto.setMediaUrl("https://example.com/a.png");
        dto.setLicenseEndTime(null); // 永久
        assertDoesNotThrow(() -> validator.toMaterialExt(1L, dto, now));

        // 过去的 licenseEndTime
        dto.setLicenseEndTime(now.minusDays(1));
        assertThrows(BizException.class, () -> validator.toMaterialExt(1L, dto, now));
        dto.setLicenseEndTime(now.plusDays(30));
        assertDoesNotThrow(() -> validator.toMaterialExt(1L, dto, now));

        // mediaType 非法
        dto.setMediaType(4);
        assertThrows(BizException.class, () -> validator.toMaterialExt(1L, dto, now));
        dto.setMediaType(1);

        // mediaUrl 空
        dto.setMediaUrl("  ");
        assertThrows(BizException.class, () -> validator.toMaterialExt(1L, dto, now));
    }

    @Test
    void subImageLimit5ThrowsOn6() {
        // 第一个参数是主图（合法 PNG），第二个参数是 6 张副图
        String main = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmBIQAAAABJRU5ErkJggg==";
        List<String> subs = List.of(main, main, main, main, main, main); // 6 张
        BizException ex = assertThrows(BizException.class,
                () -> validator.validateImages(main, subs));
        assertEquals(com.example.product.common.ErrorCode.TOO_MANY_SUB_IMAGES,
                ex.getErrorCode());
    }

    @Test
    void subImageLimit5Accepts5() {
        String main = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmBIQAAAABJRU5ErkJggg==";
        List<String> subs = List.of(main, main, main, main, main); // 5 张，刚好在上限
        assertDoesNotThrow(() -> validator.validateImages(main, subs));
    }
}
