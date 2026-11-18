package util;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

/**
 * LSL 语法着色规则。
 *
 * <p>只维护「样式 + 匹配规则」：{@link #highlight(StyledDocument)} 会把文档内容
 * 重新着色为默认/关键字/数字/字符串/注释五类。着色结果写入文档字符属性，
 * 因此撤销栈需要过滤掉这些属性修改（见 {@code CompilerFrame#attachUndo}）。</p>
 */
public class CodeStyle {

	/** 注释颜色。 */
	private static final Color COMMENT_FOREGROUND = new Color(57, 150, 48);
	/** 关键字颜色。 */
	private static final Color KEYWORDS_FOREGROUND = new Color(127, 0, 85);
	/** 数字颜色。 */
	private static final Color NUMBER_FOREGROUND = new Color(0, 102, 153);
	/** 字符串颜色。 */
	private static final Color STRING_FOREGROUND = new Color(255, 0, 0);
	/** 默认字体颜色。 */
	private static final Color DEFAULT_FOREGROUND = new Color(0, 0, 0);

	/** LSL / LARCH 关键字（与词法分析器的关键字集合保持一致）。 */
	private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
			"trait", "sort", "operators", "introduces", "includes", "asserts", "assumes", "implies",
			"generated", "by", "partitioned", "converts", "exempting", "for", "using", "applied",
			"when", "if", "then", "else", "new", "add", "lookup", "belong", "true", "false"));

	private static final Pattern KEYWORD_PATTERN =
			Pattern.compile("\\b(?:" + String.join("|", KEYWORDS) + ")\\b");
	private static final Pattern NUMBER_PATTERN = Pattern.compile("\\b\\d+\\b");
	private static final Pattern STRING_PATTERN = Pattern.compile("\"(?:\\\\.|[^\"\\\\])*\"");
	private static final Pattern COMMENT_PATTERN = Pattern.compile("(?:--|//)[^\\n]*");

	/** 每个字符的宽度。 */
	private int charWidth;
	/** 每个字符的高度。 */
	private int charHeight;

	private final StyleContext styleContext = new StyleContext();
	private final Style normal = colorStyle("none", DEFAULT_FOREGROUND);
	private final Style keyword = colorStyle("keyword", KEYWORDS_FOREGROUND);
	private final Style number = colorStyle("number", NUMBER_FOREGROUND);
	private final Style string = colorStyle("string", STRING_FOREGROUND);
	private final Style comment = colorStyle("comment", COMMENT_FOREGROUND);

	public CodeStyle() {
		// 样式在字段初始化时注册，构造器保持为空以便后续扩展
	}

	private Style colorStyle(String name, Color color) {
		Style style = styleContext.addStyle(name, null);
		StyleConstants.setForeground(style, color);
		return style;
	}

	/**
	 * 按 LSL 语法给整篇文档重新着色。
	 *
	 * <p>顺序为「默认 → 数字 → 关键字 → 字符串 → 注释」，后应用的规则覆盖前面的，
	 * 因此字符串里的关键字、注释里的任何符号都会显示为各自颜色。</p>
	 */
	public void highlight(StyledDocument document) {
		if (document == null) {
			return;
		}
		try {
			String text = document.getText(0, document.getLength());
			document.setCharacterAttributes(0, text.length(), normal, true);
			apply(document, text, NUMBER_PATTERN, number);
			apply(document, text, KEYWORD_PATTERN, keyword);
			apply(document, text, STRING_PATTERN, string);
			apply(document, text, COMMENT_PATTERN, comment);
		} catch (BadLocationException ignored) {
			// 文档在读取过程中被替换：下一次编辑会重新着色
		}
	}

	private static void apply(StyledDocument document, String text, Pattern pattern, Style style) {
		Matcher matcher = pattern.matcher(text);
		while (matcher.find()) {
			document.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), style, true);
		}
	}

	public int getCharWidth() {
		return charWidth;
	}

	public void setCharWidth(int charWidth) {
		this.charWidth = charWidth;
	}

	public int getCharHeight() {
		return charHeight;
	}

	public void setCharHeight(int charHeight) {
		this.charHeight = charHeight;
	}
}
