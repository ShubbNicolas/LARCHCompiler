package util;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;

import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import javax.swing.text.TabSet;
import javax.swing.text.TabStop;

/**
 * 程序文本编辑区。
 *
 * <p>只负责编辑体验本身（字体、Tab 宽度、按行定位）。历史实现里曾在每次文档变更时
 * 重建 {@link javax.swing.text.StyledDocument} 且不复制原文，导致任何编辑（含粘贴）
 * 都会把内容清空；该逻辑已移除。</p>
 */
public class StyleEditor extends JTextPane {

	private static final long serialVersionUID = 1L;

	private final CodeStyle codeStyle = new CodeStyle();

	private final StyleContext context = new StyleContext();

	private final Style style = context.getStyle(StyleContext.DEFAULT_STYLE);

	/** 合并同一轮事件里的多次编辑，避免每敲一个字符就重排整篇。 */
	private boolean highlightPending;

	public StyleEditor() {
		setDocument(new DefaultStyledDocument());
		setFont(new Font("Courier New", Font.PLAIN, 15));
		setTabs(this, 4);
		setMargin(new Insets(2, 0, 0, 0));
		setEditable(true);
		getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				scheduleHighlight();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				scheduleHighlight();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				// 着色本身产生的属性变化不需要再次触发
			}
		});
		scheduleHighlight();
	}

	public StyleEditor(boolean cmm) {
		this();
	}

	public int getLineOfOffset(int paramInt) throws BadLocationException {
		Document localDocument = getDocument();
		if (paramInt < 0)
			throw new BadLocationException("Can't translate offset to line", -1);
		if (paramInt > localDocument.getLength()) {
			throw new BadLocationException("Can't translate offset to line",
					localDocument.getLength() + 1);
		}
		Element localElement = getDocument().getDefaultRootElement();
		return localElement.getElementIndex(paramInt);
	}

	public int getLineCount() {
		Element localElement = getDocument().getDefaultRootElement();
		return localElement.getElementCount();
	}

	public int getLineStartOffset(int paramInt) throws BadLocationException {
		int i = getLineCount();
		if (paramInt < 0)
			throw new BadLocationException("Negative line", -1);
		if (paramInt >= i)
			throw new BadLocationException("No such line", getDocument()
					.getLength() + 1);
		Element localElement1 = getDocument().getDefaultRootElement();
		Element localElement2 = localElement1.getElement(paramInt);
		return localElement2.getStartOffset();
	}

	public void setTabs(JTextPane textPane, int charactersPerTab) {
		FontMetrics fm = textPane.getFontMetrics(textPane.getFont());
		int charWidth = fm.charWidth('w');
		int tabWidth = charWidth * charactersPerTab;
		codeStyle.setCharWidth(charWidth);
		TabStop[] tabs = new TabStop[1000];

		for (int j = 0; j < tabs.length; j++) {
			int tab = j + 1;
			tabs[j] = new TabStop(tab * tabWidth);
		}
		TabSet tabSet = new TabSet(tabs);
		StyleConstants.setTabSet(style, tabSet);
	}

	/** 在 EDT 上合并触发一次语法着色。 */
	private void scheduleHighlight() {
		if (highlightPending) {
			return;
		}
		highlightPending = true;
		SwingUtilities.invokeLater(() -> {
			highlightPending = false;
			Document document = getDocument();
			if (document instanceof StyledDocument) {
				codeStyle.highlight((StyledDocument) document);
			}
		});
	}

	@Override
	public boolean getScrollableTracksViewportWidth() {
		return false;
	}

	@Override
	public void setSize(Dimension d) {
		if (getParent() != null) {
			int parentWidth = getParent().getWidth();
			if (parentWidth > d.width) {
				d.width = parentWidth;
			}
		}
		super.setSize(d);
	}
}
