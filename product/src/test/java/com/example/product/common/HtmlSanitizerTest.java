package com.example.product.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * HTML 净化器单元测试，覆盖技术文档 5.2 节：
 * <ul>
 *   <li>剥离 script/style 块</li>
 *   <li>剥离所有 HTML 标签</li>
 *   <li>超过 2000 字抛 BizException</li>
 *   <li>空输入返回 null</li>
 * </ul>
 */
class HtmlSanitizerTest {

    @Test
    void stripsScriptBlock() {
        String raw = "<p>hello</p><script>alert('xss')</script><p>world</p>";
        String result = HtmlSanitizer.sanitize(raw);
        assertEquals("helloworld", result);
    }

    @Test
    void stripsStyleBlock() {
        String raw = "<style>body { color: red; }</style><p>text</p>";
        String result = HtmlSanitizer.sanitize(raw);
        assertEquals("text", result);
    }

    @Test
    void stripsAllTags() {
        String raw = "<div><b>bold</b> and <i>italic</i></div>";
        String result = HtmlSanitizer.sanitize(raw);
        assertEquals("bold and italic", result);
    }

    @Test
    void nullInputReturnsNull() {
        assertEquals(null, HtmlSanitizer.sanitize(null));
        assertEquals(null, HtmlSanitizer.sanitize(""));
        assertEquals(null, HtmlSanitizer.sanitize("   "));
        assertEquals(null, HtmlSanitizer.sanitize("<script></script>"));
    }

    @Test
    void tooLongThrows() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= HtmlSanitizer.DETAIL_MAX_LENGTH; i++) {
            sb.append("a");
        }
        // 超过 2000 字抛 BizException，不允许静默截断
        assertThrows(BizException.class, () -> HtmlSanitizer.sanitize(sb.toString()));
    }

    @Test
    void exactlyMaxLengthPasses() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < HtmlSanitizer.DETAIL_MAX_LENGTH; i++) {
            sb.append("a");
        }
        String result = HtmlSanitizer.sanitize(sb.toString());
        assertEquals(HtmlSanitizer.DETAIL_MAX_LENGTH, result.length());
    }
}
