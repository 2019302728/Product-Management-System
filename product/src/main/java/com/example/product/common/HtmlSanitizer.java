package com.example.product.common;

import java.util.regex.Pattern;

/**
 * HTML 净化器，用于商品详情 detail 字段。
 *
 * <p>技术文档 5.2 节要求："去除 HTML 标签后最多 2000 字；入库前过滤脚本、事件属性等危险 HTML。"
 * L0 不引入第三方库（如 OWASP Java HTML Sanitizer），使用以下两层处理：</p>
 * <ol>
 *   <li>剥离 {@code <script>...</script>} 整段，避免 XSS 脚本执行</li>
 *   <li>剥离所有 HTML 标签，得到纯文本内容</li>
 *   <li>截断到 2000 字</li>
 * </ol>
 *
 * <p>这是一种保守方案：直接丢弃所有 HTML 标签，不会保留富文本样式。
 * 若需要保留允许的标签（如 {@code <p> <b> <img>}），应引入专业 HTML Sanitizer 库。</p>
 */
public final class HtmlSanitizer {

    /** 详情纯文本最大长度 */
    public static final int DETAIL_MAX_LENGTH = 2000;

    private static final Pattern SCRIPT_BLOCK = Pattern.compile(
            "<script[^>]*>[\\s\\S]*?</script>",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern STYLE_BLOCK = Pattern.compile(
            "<style[^>]*>[\\s\\S]*?</style>",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern ANY_TAG = Pattern.compile(
            "<[^>]+>");

    private HtmlSanitizer() {
    }

    /**
     * 净化并截断 detail。
     *
     * @param raw 原始 detail，可能为 null
     * @return 净化后的纯文本，长度不超过 {@value #DETAIL_MAX_LENGTH}；输入为空时返回 null
     */
    public static String sanitize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw;
        s = SCRIPT_BLOCK.matcher(s).replaceAll("");
        s = STYLE_BLOCK.matcher(s).replaceAll("");
        s = ANY_TAG.matcher(s).replaceAll("");
        s = s.trim();
        if (s.isEmpty()) {
            return null;
        }
        if (s.length() > DETAIL_MAX_LENGTH) {
            throw new BizException(ErrorCode.DETAIL_TOO_LONG,
                    "detail 去除 HTML 标签后 " + s.length() + " 字，超过 " + DETAIL_MAX_LENGTH + " 字上限");
        }
        return s;
    }
}
