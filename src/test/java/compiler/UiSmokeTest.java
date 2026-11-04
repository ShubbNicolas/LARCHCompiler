package compiler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import util.JCloseableTabbedPane;
import util.StyleEditor;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 真实窗口级的 UI 冒烟测试：构造 CompilerFrame，走菜单项触发各功能。
 *
 * <p>需要图形环境；无显示时自动跳过。</p>
 */
class UiSmokeTest {

    private static final String SPEC = """
            Container(E, C): trait
              introduces
                isEmpty: C → Bool
                count: E, C → Int
              asserts
                e: E, c: C
                isEmpty(empty);
                count(e, empty) == 0
            """;

    private CompilerFrame frame;

    @BeforeEach
    void setUp() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "无图形环境，跳过 UI 测试");
        SwingUtilities.invokeAndWait(() -> {
            frame = new CompilerFrame("UI测试");
            frame.setSize(1240, 742);
            frame.layoutPanels();
            frame.setVisible(true);
        });
    }

    @AfterEach
    void tearDown() throws Exception {
        if (frame != null) {
            SwingUtilities.invokeAndWait(frame::dispose);
            frame = null;
        }
    }

    // ---------- 组件查找 ----------

    private static <T> List<T> findAll(Container root, Class<T> type) {
        List<T> found = new ArrayList<>();
        for (Component c : root.getComponents()) {
            if (type.isInstance(c)) {
                found.add(type.cast(c));
            }
            if (c instanceof Container child) {
                found.addAll(findAll(child, type));
            }
        }
        return found;
    }

    private JTabbedPane editTabs() {
        return findAll(frame.getContentPane(), JTabbedPane.class).stream()
                .filter(JCloseableTabbedPane.class::isInstance)
                .findFirst().orElseThrow();
    }

    private StyleEditor editor() {
        JScrollPane scroll = (JScrollPane) editTabs().getComponentAt(0);
        return (StyleEditor) scroll.getViewport().getView();
    }

    private JMenuItem menuItem(String menuText, String itemText) {
        for (int i = 0; i < frame.getJMenuBar().getMenuCount(); i++) {
            JMenu menu = frame.getJMenuBar().getMenu(i);
            for (int j = 0; j < menu.getItemCount(); j++) {
                JMenuItem item = menu.getItem(j);
                if (item != null && item.getText() != null
                        && item.getText().replace(" ", "").equals(itemText.replace(" ", ""))) {
                    return item;
                }
            }
        }
        throw new IllegalStateException("找不到菜单项: " + itemText);
    }

    /** 直接调用菜单项的监听器：doClick() 内部用 Timer，异步执行，断言会抢跑。 */
    private void click(String menu, String item) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JMenuItem target = menuItem(menu, item);
            for (java.awt.event.ActionListener listener : target.getActionListeners()) {
                listener.actionPerformed(new java.awt.event.ActionEvent(
                        target, java.awt.event.ActionEvent.ACTION_PERFORMED, "click"));
            }
        });
    }

    /** 按标签标题定位右侧结果页（词法分析 / 语法分析）。 */
    private JTabbedPane resultTabs() {
        return findAll(frame.getContentPane(), JTabbedPane.class).stream()
                .filter(t -> t.getTabCount() == 2)
                .filter(t -> "词法分析".equals(t.getTitleAt(0)))
                .findFirst().orElseThrow();
    }

    /** 按标签标题定位下方控制台 / 错误列表。 */
    private JTabbedPane consoleTabs() {
        return findAll(frame.getContentPane(), JTabbedPane.class).stream()
                .filter(t -> t.getTabCount() == 2)
                .filter(t -> "控制台".equals(t.getTitleAt(0)))
                .findFirst().orElseThrow();
    }

    private static String textOf(JTabbedPane tabs, int index) {
        JScrollPane scroll = (JScrollPane) tabs.getComponentAt(index);
        return ((javax.swing.text.JTextComponent) scroll.getViewport().getView()).getText();
    }

    private String consoleText() {
        return textOf(consoleTabs(), 0);
    }

    private JTable lexTable() {
        JScrollPane scroll = (JScrollPane) resultTabs().getComponentAt(0);
        return (JTable) scroll.getViewport().getView();
    }

    private String parseText() {
        return textOf(resultTabs(), 1);
    }

    // ---------- 用例 ----------

    @Test
    @DisplayName("窗口可创建，三块主区域布局正确")
    void frameLayout() {
        assertTrue(frame.isVisible());
        assertTrue(editTabs().getTabCount() >= 1);
        assertTrue(editor().isEditable());
    }

    @Test
    @DisplayName("编辑区可输入，撤销/重做可用")
    void typingUndoRedo() throws Exception {
        SwingUtilities.invokeAndWait(() -> editor().setText("abc"));
        click("编辑(E)", "撤销");
        assertTrue(editor().getText().isEmpty(), "撤销后应为空，实际: " + editor().getText());
        click("编辑(E)", "重做");
        assertEquals("abc", editor().getText());
    }

    @Test
    @DisplayName("全选 / 复制 / 粘贴 往返")
    void copyPaste() throws Exception {
        SwingUtilities.invokeAndWait(() -> editor().setText("hello"));
        click("编辑(E)", "全选");
        click("编辑(E)", "复制");
        SwingUtilities.invokeAndWait(() -> editor().setCaretPosition(editor().getDocument().getLength()));
        click("编辑(E)", "粘贴");
        assertEquals("hellohello", editor().getText());
    }

    @Test
    @DisplayName("词法分析填充 token 表")
    void lexFillsTable() throws Exception {
        SwingUtilities.invokeAndWait(() -> editor().setText(SPEC));
        click("运行(R)", "词法分析");
        assertTrue(lexTable().getRowCount() > 0, "token 表应有数据");
    }

    @Test
    @DisplayName("语法分析输出语法树")
    void parseShowsTree() throws Exception {
        SwingUtilities.invokeAndWait(() -> editor().setText(SPEC));
        click("运行(R)", "语法分析");
        assertTrue(parseText().contains("Trait"), "应包含 Trait，实际: " + parseText());
    }

    @Test
    @DisplayName("运行输出规格检查报告")
    void runShowsReport() throws Exception {
        SwingUtilities.invokeAndWait(() -> editor().setText(SPEC));
        click("运行(R)", "运行");
        String text = consoleText();
        assertTrue(text.contains("Container"), text);
        assertTrue(text.contains("规格"), text);
    }

    @Test
    @DisplayName("关闭标签后执行运行不抛异常")
    void runWithoutTabs() throws Exception {
        SwingUtilities.invokeAndWait(() -> editTabs().removeTabAt(0));
        click("运行(R)", "运行");          // 无标签时应有提示而不是崩溃
        assertTrue(consoleText().contains("没有") || consoleText().isEmpty());
    }

    @Test
    @DisplayName("打开 UTF-8 文件：内容正确且重复打开不新增标签")
    void openUtf8File() throws Exception {
        java.io.File tmp = java.io.File.createTempFile("larch-open", ".lsl");
        tmp.deleteOnExit();
        java.nio.file.Files.writeString(tmp.toPath(), SPEC,
                java.nio.charset.StandardCharsets.UTF_8);

        java.lang.reflect.Method openFile = CompilerFrame.class
                .getDeclaredMethod("openFile", java.io.File.class);
        openFile.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                openFile.invoke(frame, tmp);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        assertEquals(2, editTabs().getTabCount(), "应新增一个标签");
        JScrollPane opened = (JScrollPane) editTabs().getSelectedComponent();
        StyleEditor openedEditor = (StyleEditor) opened.getViewport().getView();
        assertTrue(openedEditor.getText().contains("Container"), openedEditor.getText());

        SwingUtilities.invokeAndWait(() -> {
            try {
                openFile.invoke(frame, tmp);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        assertEquals(2, editTabs().getTabCount(), "同一文件不应重复开标签");
    }

    @Test
    @DisplayName("非 UTF-8 文件回退到系统编码读取")
    void gbkFallback() throws Exception {
        java.io.File tmp = java.io.File.createTempFile("larch-gbk", ".lsl");
        tmp.deleteOnExit();
        String original = "规格 spec";
        java.nio.file.Files.write(tmp.toPath(), original.getBytes("GBK"));

        java.lang.reflect.Method readText = CompilerFrame.class
                .getDeclaredMethod("readText", java.io.File.class);
        readText.setAccessible(true);
        String decoded = (String) readText.invoke(null, tmp);
        assertEquals(original, decoded);
    }

    @Test
    @DisplayName("窗口菜单：开始页切回第一个标签")
    void startPageSelectsFirstTab() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JScrollPane scroll = new JScrollPane(new StyleEditor());
            editTabs().add(scroll, "second.lsl");
            editTabs().setSelectedIndex(1);
        });
        click("窗口(W)", "开始页");
        assertEquals(0, editTabs().getSelectedIndex());
    }

    @Test
    @DisplayName("窗口缩放后编辑区按新尺寸重排")
    void resizeRelayouts() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            frame.setSize(1000, 700);
            frame.layoutPanels();
        });
        int contentWidth = frame.getContentPane().getWidth();
        JTabbedPane tabs = editTabs();
        assertTrue(tabs.getWidth() > 0 && tabs.getWidth() < contentWidth,
                "编辑区宽度应在 (0, 内容宽度) 之间，实际 " + tabs.getWidth());
        assertTrue(tabs.getHeight() > 0);
    }

    @Test
    @DisplayName("空编辑区执行词法/语法/运行不抛异常")
    void emptySourceIsSafe() throws Exception {
        SwingUtilities.invokeAndWait(() -> editor().setText(""));
        click("运行(R)", "词法分析");
        click("运行(R)", "语法分析");
        click("运行(R)", "运行");
        // 空输入只产生一个 EOF token，键是不抛异常
        assertTrue(lexTable().getRowCount() <= 1);
    }

    @Test
    @DisplayName("新建标签页并切换，互不干扰")
    void multipleTabs() throws Exception {
        SwingUtilities.invokeAndWait(() -> editor().setText("tab1"));
        SwingUtilities.invokeAndWait(() -> {
            JScrollPane scroll = new JScrollPane(new StyleEditor());
            editTabs().add(scroll, "second.lsl");
            editTabs().setSelectedIndex(1);
        });
        assertEquals(2, editTabs().getTabCount());
        SwingUtilities.invokeAndWait(() -> editTabs().setSelectedIndex(0));
        assertEquals("tab1", editor().getText());
    }
}
