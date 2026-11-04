package util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 回归测试：编辑区在文本变更后必须保留内容。
 *
 * <p>历史实现在每次 insert/remove 时重建文档却不复制原文，导致粘贴、输入的内容立刻消失。
 * 这里不依赖系统剪贴板（headless 环境不可用），直接验证文档内容在编辑后仍然存在。</p>
 */
class StyleEditorTest {

    @Test
    @DisplayName("setText 后内容保留")
    void setTextKeepsContent() {
        StyleEditor editor = new StyleEditor();
        editor.setText("hello");
        assertEquals(5, editor.getDocument().getLength());
        assertEquals("hello", editor.getText());
    }

    @Test
    @DisplayName("模拟粘贴（replaceSelection）后内容追加而不是清空")
    void replaceSelectionAppends() {
        StyleEditor editor = new StyleEditor();
        editor.setText("abc");
        editor.setCaretPosition(3);
        editor.replaceSelection("XYZ");
        assertEquals("abcXYZ", editor.getText());
        assertEquals(6, editor.getDocument().getLength());
    }

    @Test
    @DisplayName("多次编辑后内容持续累积")
    void multipleEditsAccumulate() {
        StyleEditor editor = new StyleEditor();
        editor.setText("line1\n");
        editor.setCaretPosition(editor.getDocument().getLength());
        editor.replaceSelection("line2\n");
        // headless 下 caret 不一定自动前移，显式定位，保证断言只检验「内容是否被清空」
        editor.setCaretPosition(editor.getDocument().getLength());
        editor.replaceSelection("line3");
        assertEquals("line1\nline2\nline3", editor.getText());
        assertEquals(3, editor.getLineCount());
    }

    @Test
    @DisplayName("编辑区默认可编辑（否则粘贴会被忽略）")
    void editableByDefault() {
        StyleEditor editor = new StyleEditor();
        assertTrue(editor.isEditable());
    }
}
