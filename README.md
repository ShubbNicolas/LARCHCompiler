# LARCH 编译器

基于 Java Swing 的 LARCH 语言桌面编辑环境。提供类 IDE 的界面（多标签编辑、行号、文件树、控制台、错误列表），
并内置了一套 LARCH 语句的读取与执行代码。

> 课程设计项目，未声明开源许可。

---

## 一、功能现状

界面完整，但「分析」和「运行」两部分**尚未接线**，这一点与菜单里展示的按钮不一致，使用前需要了解：

| 功能 | 状态 | 说明 |
|---|---|---|
| 新建 / 打开 / 保存 / 退出 | ✅ 可用 | 新建自动补 `.lsl` 后缀 |
| 多标签编辑、关闭标签、行号 | ✅ 可用 | `JCloseableTabbedPane` + `TextLineNumber` |
| 撤销 / 重做 / 复制 / 剪切 / 粘贴 / 删除 / 全选 | ✅ 可用 | 基于 `UndoManager` |
| 查找 | ✅ 可用 | 简易实现：从上次位置继续向后匹配，红色高亮 |
| 字体设置 | ✅ 可用 | 自定义 `JFontDialog` |
| 光标行列状态栏 | ✅ 可用 | `JStatusBar` |
| 项目文件树 | ✅ 可用 | 左侧 `JFileTree` |
| 词法分析 | 🟡 引擎已完成 | `compiler.lexer` 已实现，13 个单测通过；界面按钮仍被注释，未接线 |
| 语法分析 | 🟡 引擎已完成 | `compiler.parser` 已实现，8 个单测通过；界面按钮仍被注释，未接线 |
| 运行（F5） | 🟡 引擎已完成 | `compiler.runtime` 等式检查器已实现，Table1 的 6 条等式全部通过；`run()` 仍是空实现，未接线 |
| LARCH 语句解析 | 🟡 仅命令行 | 旧 `ReaderHelper` 只被 `test` 包调用；新引擎 `compiler.parser` 尚未接入界面 |

对应代码位置：`CompilerFrame.java:728-737`（lex/parse 监听器注释块）、`CompilerFrame.java:821-827`（`run()` 空实现）。

## 二、目录结构

采用 Maven 标准布局：

```
LARCHCompiler/
├── pom.xml                        Maven 构建描述
├── mvnw / mvnw.cmd               Maven Wrapper（无需预装 Maven）
├── .mvn/wrapper/                 Wrapper 配置（Maven 3.9.9）
├── src/main/java/
│   ├── compiler/                 主界面与执行逻辑
│   │   ├── CompilerFrame.java       主窗口：菜单、工具栏、编辑区、控制台（1044 行）
│   │   ├── ExcuteProgram.java       LARCH 语句执行：new / Ind / Val / add / lookup
│   │   └── OpenFile.java            文件选择对话框（*.larch / *.txt）
│   ├── util/
│   │   ├── ReaderHelper.java        按行读取并归类 trait/new/add/lookup/belong/asserts
│   │   ├── IntroducesHelper.java    数组式键值存储与算子求值
│   │   ├── AssertsHelper.java       断言字符串的切分与函数名识别
│   │   ├── StyleEditor.java         带 Tab 与行首行尾处理的文本面板
│   │   ├── CodeStyle.java           语法高亮样式定义（关键字表已被注释）
│   │   ├── JCloseableTabbedPane.java / JFileTree.java / JFontDialog.java
│   │   ├── JStatusBar.java / TextLineNumber.java
│   └── exceptions/               自定义异常（FileNotTxtException 等）
├── src/main/resources/images/    图标资源（打包后位于 jar 的 /images/）
└── src/test/java/test/           无界面的手工驱动程序
```

## 三、LARCH 语句（从解析代码反推）

`ReaderHelper` 逐行用 `indexOf` / `split` 匹配下列关键字，**不是**真正的词法/语法分析：

| 语句 | 解析方式 | 数据去向 |
|---|---|---|
| `trait ...` | 取首段作为 Trait 名 | `Traitname` |
| `new X` | 取第 2 段 | `newStack` |
| `add(a, b, c)` | 取第 2~5 段 | `addStack` |
| `lookup(a, b, c)` | 取第 2~4 段 | `lookupStack` |
| `/belong a b c` | 取第 2~4 段 | `/belongStack` |
| `asserts` | 该行之后全部内容 | `assertContent[]` |
| `Ind(a,b,c)` / `Val(a,b,c)` | 截取括号内、去空格和单引号后按 `,` 切分 | `Ind[]` / `Val[]`（字符数组） |

`ExcuteProgram.Run()` 依次处理每行：`new` 记录类型名，`add` 调用 `Lclass_operate2` 写入键值，
`lookup` 调用 `Lclass_opreate1` 查值。底层存储是长度为 10 的 `char[]` 平行数组。

## 四、构建与运行

**环境要求**：JDK 17+（已在 JDK 21 上验证）。**不需要预装 Maven** —— 项目自带 Wrapper。

```powershell
.\mvnw.cmd package                 # 编译 + 打包
.\mvnw.cmd clean package           # 清理后重新构建
.\mvnw.cmd test                    # 运行测试
.\mvnw.cmd compile                 # 只编译
```

VS Code 中按 `Ctrl+Shift+B` 执行默认任务（`Larch: package (jar)`）。

产物：`target\LarchCompiler.jar`，主类 `compiler.CompilerFrame`。

```powershell
java -jar target\LarchCompiler.jar
```

首次构建会通过 Wrapper 下载 Maven 与本项目依赖，之后走本地仓库缓存。

### 关于编码

源码**统一为 UTF-8**，`pom.xml` 中 `project.build.sourceEncoding=UTF-8`、`maven.compiler.release=17`。
Eclipse 侧的 `.settings/org.eclipse.core.resources.prefs` 与 `.classpath` 也同步为 UTF-8 + JavaSE-17。

## 五、使用说明

1. **新建**：`Ctrl+N`，输入文件名（会自动追加 `.lsl`）
2. **打开**：`Ctrl+O`，支持 `*.larch`、`*.txt`
3. **编辑**：撤销重做、查找、字体设置均可用
4. **运行**：`F5` —— 当前版本仅清空控制台，不会实际解释程序

## 六、已知限制

- **分析/运行未接入**：词法与语法分析按钮的监听器被注释，`run()` 为空实现；真正的解析代码只被 `test` 包的
  `Test` / `Test2` / `testRun` 调用（其中 `testRun` 硬编码了 `C:/Users/Janki/Desktop/Larch/Run.txt`）
- **解析容错差**：逐行字符串匹配，缺少行号定位与错误恢复
- **容量固定**：`Ind`/`Val`/`Funname` 长度为 10，读取缓冲区长度 15，超出行数或元素会数组越界
- **无语法高亮**：`CodeStyle` 的关键字列表在构造函数中被整段注释
- **窗口固定**：`setResizable(false)`，尺寸锁定 1240×742
- **静态计数器**：`ReaderHelper.linecount` 未在解析结束后重置，`asserts` 分支还会多减一次；
  同一进程内重复调用 `Assign()` 会错乱

## 七、开发约定

- 源码统一 **UTF-8**
- `.vscode/`、`bin/`、`target/` 已在 `.gitignore` 中忽略
- 提交前请确认 `.\mvnw.cmd package` 通过
