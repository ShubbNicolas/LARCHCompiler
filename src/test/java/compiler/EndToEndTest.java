package compiler;

import compiler.lexer.LexResult;
import compiler.lexer.Lexer;
import compiler.parser.Ast;
import compiler.parser.ParseResult;
import compiler.parser.Parser;
import compiler.runtime.CheckResult;
import compiler.runtime.EquationChecker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 端到端：从 {@code src/main/resources/samples} 读取真实 .lsl 文件，
 * 走完 词法 → 语法 → 等式检查 全链路。
 */
class EndToEndTest {

    private static String sample(String name) throws IOException {
        try (InputStream in = EndToEndTest.class.getResourceAsStream("/samples/" + name)) {
            assertNotNull(in, "样例不存在: " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("四个样例文件全部通过词法与语法分析")
    void allSamplesParse() throws IOException {
        for (String name : List.of("Table1.lsl", "OrderedString.lsl",
                "ContainerPQueue.lsl", "PairwiseExtension.lsl")) {
            String source = sample(name);

            LexResult lex = Lexer.tokenize(source);
            assertFalse(lex.hasErrors(), () -> name + " 词法错误: " + lex.errors());

            ParseResult parsed = Parser.parse(source);
            assertFalse(parsed.hasErrors(), () -> name + " 语法错误: " + parsed.errors());
            assertFalse(parsed.traits().isEmpty(), name + " 没有解析出 trait");
        }
    }

    @Test
    @DisplayName("ContainerPQueue 解析出两个 trait 及其全部段")
    void containerPQueueStructure() throws IOException {
        ParseResult parsed = Parser.parse(sample("ContainerPQueue.lsl"));
        assertEquals(2, parsed.traits().size());

        Ast.Trait container = parsed.traits().get(0);
        assertEquals("Container", container.name());
        assertTrue(container.sections().stream().anyMatch(Ast.Asserts.class::isInstance));
        assertTrue(container.sections().stream().anyMatch(Ast.Implies.class::isInstance));
        assertTrue(container.sections().stream().anyMatch(Ast.Converts.class::isInstance));

        Ast.Trait pqueue = parsed.traits().get(1);
        assertEquals("PQueue", pqueue.name());
        assertTrue(pqueue.sections().stream().anyMatch(Ast.Exempting.class::isInstance));
        assertTrue(pqueue.sections().stream().anyMatch(Ast.Implies.class::isInstance));
    }

    @Test
    @DisplayName("Table1 的等式在模型下全部通过")
    void table1ChecksPass() throws IOException {
        ParseResult parsed = Parser.parse(sample("Table1.lsl"));
        List<CheckResult> results = EquationChecker.check(parsed.trait());
        assertEquals(6, results.size());
        List<CheckResult> notPassed = results.stream()
                .filter(r -> r.status() != CheckResult.Status.PASS)
                .toList();
        assertTrue(notPassed.isEmpty(), () -> EquationChecker.report(notPassed));
    }

    @Test
    @DisplayName("Container 的等式在模型下没有反例")
    void containerChecksHaveNoCounterexample() throws IOException {
        ParseResult parsed = Parser.parse(sample("ContainerPQueue.lsl"));
        Ast.Trait container = parsed.traits().get(0);
        List<CheckResult> results = EquationChecker.check(container);
        assertFalse(results.isEmpty());
        List<CheckResult> failed = results.stream()
                .filter(r -> r.status() == CheckResult.Status.FAIL)
                .toList();
        assertTrue(failed.isEmpty(), () -> EquationChecker.report(failed));
    }

    @Test
    @DisplayName("报告文本包含汇总统计")
    void reportText() throws IOException {
        ParseResult parsed = Parser.parse(sample("Table1.lsl"));
        String report = EquationChecker.report(EquationChecker.check(parsed.trait()));
        assertTrue(report.contains("条等式"));
        assertTrue(report.contains("通过"));
    }
}
