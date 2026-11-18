package util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Color;

import javax.swing.text.AttributeSet;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 语法着色规则测试：验证五类样式各自命中，且「字符串/注释覆盖关键字」的优先级正确。
 */
class CodeStyleTest {

	private static final Color KEYWORD = new Color(127, 0, 85);
	private static final Color NUMBER = new Color(0, 102, 153);
	private static final Color STRING = new Color(255, 0, 0);
	private static final Color COMMENT = new Color(57, 150, 48);
	private static final Color DEFAULT = new Color(0, 0, 0);

	private static StyledDocument documentWith(String text) throws Exception {
		StyledDocument document = new DefaultStyledDocument();
		document.insertString(0, text, null);
		new CodeStyle().highlight(document);
		return document;
	}

	private static Color colorAt(StyledDocument document, int offset) {
		Element element = document.getCharacterElement(offset);
		AttributeSet attributes = element.getAttributes();
		return StyleConstants.getForeground(attributes);
	}

	@Test
	@DisplayName("关键字着色")
	void highlightsKeywords() throws Exception {
		String text = "trait Foo introduces";
		StyledDocument document = documentWith(text);
		assertEquals(KEYWORD, colorAt(document, text.indexOf("trait")));
		assertEquals(KEYWORD, colorAt(document, text.indexOf("introduces")));
	}

	@Test
	@DisplayName("数字与普通标识符")
	void highlightsNumbersAndKeepsIdentifiersDefault() throws Exception {
		String text = "size 42 Foo";
		StyledDocument document = documentWith(text);
		assertEquals(NUMBER, colorAt(document, text.indexOf("42")));
		assertEquals(DEFAULT, colorAt(document, text.indexOf("Foo")));
	}

	@Test
	@DisplayName("字符串覆盖其中的关键字")
	void stringWinsOverKeyword() throws Exception {
		String text = "if \"if\"";
		StyledDocument document = documentWith(text);
		int first = text.indexOf("if");
		int quoted = text.indexOf("\"if\"") + 1;
		assertEquals(KEYWORD, colorAt(document, first));
		assertEquals(STRING, colorAt(document, quoted));
	}

	@Test
	@DisplayName("注释覆盖其中的关键字与数字")
	void commentWinsOverEverything() throws Exception {
		String text = "-- trait 42\nx";
		StyledDocument document = documentWith(text);
		assertEquals(COMMENT, colorAt(document, text.indexOf("trait")));
		assertEquals(COMMENT, colorAt(document, text.indexOf("42")));
		assertEquals(DEFAULT, colorAt(document, text.indexOf("x")));
	}

	@Test
	@DisplayName("着色不改变文本内容")
	void highlightDoesNotChangeText() throws Exception {
		String text = "trait Foo\n  asserts x == 1;\n";
		StyledDocument document = documentWith(text);
		assertEquals(text, document.getText(0, document.getLength()));
	}
}
