package com.example.product.common;

import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base64 图片校验器。
 *
 * <p>校验流程严格遵循技术文档 5.2 / 7 节：</p>
 * <ol>
 *   <li>解析 data URL 形式 {@code data:image/...;base64,...}；仅允许 jpeg/png/webp</li>
 *   <li>解码 Base64（损坏时抛 IMAGE_BASE64_BROKEN）</li>
 *   <li>按解码后的真实字节数判断 2MB 上限（不信任 Data URL 声明的 MIME）</li>
 *   <li>检查 magic bytes（文件头），与声明的 MIME 比对，伪造时抛 IMAGE_MAGIC_MISMATCH</li>
 *   <li>返回真实 MIME，供入库使用</li>
 * </ol>
 */
public class ImageValidator {

    /** 单张图片解码后最大字节数：2 MB */
    public static final int MAX_IMAGE_BYTES = 2 * 1024 * 1024;

    /** data URL 形式：data:image/jpeg;base64,xxxx */
    private static final Pattern DATA_URL = Pattern.compile(
            "^data:image/(?<mime>jpeg|jpg|png|webp);base64,(?<data>.+)");

    private ImageValidator() {
    }

    /**
     * 校验单张图片并返回真实 MIME 类型。
     *
     * @param dataUrl 完整 data URL 字符串
     * @return 真实 MIME 类型（如 image/jpeg），由 magic bytes 判定
     * @throws BizException 当格式非法、过大、Base64 损坏或 magic bytes 不匹配时
     */
    public static String validate(String dataUrl) {
        if (dataUrl == null || dataUrl.isBlank()) {
            throw new BizException(ErrorCode.IMAGE_BASE64_BROKEN, "图片 Base64 为空");
        }
        Matcher matcher = DATA_URL.matcher(dataUrl.trim());
        if (!matcher.matches()) {
            throw new BizException(ErrorCode.IMAGE_FORMAT_INVALID,
                    "图片 Base64 必须为 data:image/...;base64, 形式，且仅支持 jpeg/png/webp");
        }
        String declaredMime = "image/" + matcher.group("mime").toLowerCase();
        String base64Data = matcher.group("data");

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64Data);
        } catch (IllegalArgumentException e) {
            throw new BizException(ErrorCode.IMAGE_BASE64_BROKEN, "Base64 解码失败");
        }
        if (bytes.length == 0) {
            throw new BizException(ErrorCode.IMAGE_BASE64_BROKEN, "解码后字节数为 0");
        }
        if (bytes.length > MAX_IMAGE_BYTES) {
            throw new BizException(ErrorCode.IMAGE_TOO_LARGE,
                    "图片解码后 " + bytes.length + " 字节，超过 " + MAX_IMAGE_BYTES + " 字节上限");
        }

        String actualMime = detectMime(bytes);
        if (actualMime == null) {
            throw new BizException(ErrorCode.IMAGE_FORMAT_INVALID,
                    "无法识别图片 magic bytes，仅允许 JPEG/PNG/WebP");
        }
        if (!normalizeMime(actualMime).equals(normalizeMime(declaredMime))) {
            throw new BizException(ErrorCode.IMAGE_MAGIC_MISMATCH,
                    "声明的 MIME=" + declaredMime + "，实际 magic bytes=" + actualMime);
        }
        return actualMime;
    }

    /**
     * 通过文件头 magic bytes 判定真实 MIME 类型。
     *
     * <ul>
     *   <li>JPEG: FF D8 FF</li>
     *   <li>PNG:  89 50 4E 47 0D 0A 1A 0A</li>
     *   <li>WebP: 52 49 46 46 ?? ?? ?? ?? 57 45 42 50（RIFF....WEBP）</li>
     * </ul>
     */
    private static String detectMime(byte[] bytes) {
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47
                && bytes[4] == 0x0D
                && bytes[5] == 0x0A
                && bytes[6] == 0x1A
                && bytes[7] == 0x0A) {
            return "image/png";
        }
        if (bytes.length >= 12
                && bytes[0] == 0x52 // R
                && bytes[1] == 0x49 // I
                && bytes[2] == 0x46 // F
                && bytes[3] == 0x46 // F
                && bytes[8] == 0x57 // W
                && bytes[9] == 0x45 // E
                && bytes[10] == 0x42 // B
                && bytes[11] == 0x50) { // P
            return "image/webp";
        }
        return null;
    }

    /** 将 image/jpg 归一化为 image/jpeg，便于比对 */
    private static String normalizeMime(String mime) {
        return "image/jpg".equalsIgnoreCase(mime) ? "image/jpeg" : mime.toLowerCase();
    }
}
