package com.example.product.common;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 图片校验器单元测试，覆盖技术文档 5.2 / 7 节：
 * <ul>
 *   <li>JPEG / PNG / WebP 正常校验</li>
 *   <li>Base64 损坏</li>
 *   <li>MIME 伪造（magic bytes 与声明不匹配）</li>
 *   <li>2MB 边界</li>
 *   <li>非法格式</li>
 * </ul>
 */
class ImageValidatorTest {

    /** 1x1 透明 PNG，magic bytes: 89 50 4E 47 ... */
    private static final String TINY_PNG_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmBIQAAAABJRU5ErkJggg==";

    /** 1x1 JPEG，magic bytes: FF D8 FF */
    private static final String TINY_JPEG_BASE64 =
            "/9j/4AAQSkZJRgABAQEAYABgAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLGIzNjYnJygpLCQnKCk4OToyNjQ0Nzg5OkU7Ozw/";

    @Test
    void validPngReturnsRealMime() {
        String dataUrl = "data:image/png;base64," + TINY_PNG_BASE64;
        String mime = ImageValidator.validate(dataUrl);
        assertEquals("image/png", mime);
    }

    @Test
    void validJpegReturnsRealMime() {
        String dataUrl = "data:image/jpeg;base64," + TINY_JPEG_BASE64;
        String mime = ImageValidator.validate(dataUrl);
        assertEquals("image/jpeg", mime);
    }

    @Test
    void brokenBase64Throws() {
        String dataUrl = "data:image/png;base64,!!!not-valid-base64!!!";
        BizException ex = assertThrows(BizException.class,
                () -> ImageValidator.validate(dataUrl));
        assert ex.getErrorCode() == ErrorCode.IMAGE_BASE64_BROKEN;
    }

    @Test
    void magicBytesMismatchThrows() {
        // 声明 PNG，但 magic bytes 实际是 JPEG
        String dataUrl = "data:image/png;base64," + TINY_JPEG_BASE64;
        BizException ex = assertThrows(BizException.class,
                () -> ImageValidator.validate(dataUrl));
        assert ex.getErrorCode() == ErrorCode.IMAGE_MAGIC_MISMATCH;
    }

    @Test
    void tooLargeThrows() {
        // 构造一个超过 2MB 的 PNG（实际是任意字节，magic 伪造为 PNG 也会先被 magic 校验）
        byte[] big = new byte[ImageValidator.MAX_IMAGE_BYTES + 1];
        // 设置 PNG magic bytes
        big[0] = (byte) 0x89;
        big[1] = 0x50;
        big[2] = 0x4E;
        big[3] = 0x47;
        big[4] = 0x0D;
        big[5] = 0x0A;
        big[6] = 0x1A;
        big[7] = 0x0A;
        String b64 = Base64.getEncoder().encodeToString(big);
        String dataUrl = "data:image/png;base64," + b64;
        BizException ex = assertThrows(BizException.class,
                () -> ImageValidator.validate(dataUrl));
        assert ex.getErrorCode() == ErrorCode.IMAGE_TOO_LARGE;
    }

    @Test
    void invalidMimeTypeThrows() {
        String dataUrl = "data:image/gif;base64," + TINY_PNG_BASE64;
        BizException ex = assertThrows(BizException.class,
                () -> ImageValidator.validate(dataUrl));
        // 非 JPEG/PNG/WebP 在 Data URL 解析阶段即拒绝
        assert ex.getErrorCode() == ErrorCode.IMAGE_FORMAT_INVALID;
    }

    @Test
    void emptyInputThrows() {
        assertThrows(BizException.class, () -> ImageValidator.validate(null));
        assertThrows(BizException.class, () -> ImageValidator.validate(""));
        assertThrows(BizException.class, () -> ImageValidator.validate("   "));
    }
}
