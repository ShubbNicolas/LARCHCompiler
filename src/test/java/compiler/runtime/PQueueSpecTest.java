package compiler.runtime;

import compiler.parser.ParseResult;
import compiler.parser.Parser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PQueue：优先队列。head 取最小元素、tail 删除最小元素。
 * 模型里 insert 采用有序插入，使 Container 与 PQueue 两套等式同时成立。
 */
class PQueueSpecTest {

    private static final String PQUEUE = """
            PQueue(E, Q): trait
              assumes TotalOrder(E)
              includes Container(Q for C)
              asserts e, e1: E, q: Q
                head(insert(e, q)) ==
                  if isEmpty(q) ∨ e ≤ head(q) then e else head(q);
                tail(insert(e, q)) ==
                  if isEmpty(q) ∨ e ≤ head(q) then q else insert(e, tail(q))
              implies
                q: Q, e: E
                e ∈ q ⇒ e ≥ head(q);
              converts head, tail, isEmpty, count
              exempting head(empty), tail(empty)
            """;

    private static EquationChecker.SpecReport spec() {
        ParseResult parsed = Parser.parse(PQUEUE);
        assertFalse(parsed.hasErrors(), () -> "语法错误: " + parsed.errors());
        return EquationChecker.checkSpec(parsed.trait());
    }

    @Test
    @DisplayName("PQueue 的 asserts 与 implies 全部成立，判定为自洽")
    void pqueueIsConsistent() {
        EquationChecker.SpecReport report = spec();
        assertEquals(EquationChecker.SpecReport.Verdict.CONSISTENT, report.verdict(),
                () -> report.render());
        assertEquals(2, report.asserts().size());
        assertEquals(1, report.implies().size());
        assertTrue(report.asserts().stream()
                .allMatch(r -> r.status() == CheckResult.Status.PASS), () -> report.render());
        assertEquals(CheckResult.Status.PASS, report.implies().get(0).status(),
                () -> report.implies().get(0).summarize());
    }

    @Test
    @DisplayName("head 取最小元素：e ≤ head(q) 时新元素成为 head")
    void headTakesMinimum() {
        // 若 insert 是简单头插，这条等式在 e > head(q) 时会出现反例
        EquationChecker.SpecReport report = spec();
        assertEquals(0, report.asserts().stream()
                .filter(r -> r.status() == CheckResult.Status.FAIL).count(),
                () -> report.render());
    }

    @Test
    @DisplayName("把 head 等式改成取最大元素会立即出现反例")
    void wrongHeadDirectionFails() {
        String broken = PQUEUE.replace(
                "if isEmpty(q) ∨ e ≤ head(q) then e else head(q);",
                "if isEmpty(q) ∨ e ≥ head(q) then e else head(q);");
        ParseResult parsed = Parser.parse(broken);
        assertFalse(parsed.hasErrors(), () -> String.valueOf(parsed.errors()));
        EquationChecker.SpecReport report = EquationChecker.checkSpec(parsed.trait());
        assertEquals(EquationChecker.SpecReport.Verdict.CONTRADICTION, report.verdict(),
                () -> report.render());
    }

    @Test
    @DisplayName("推论被破坏时判定为矛盾")
    void brokenImplication() {
        String broken = PQUEUE.replace(
                "e ∈ q ⇒ e ≥ head(q);",
                "e ∈ q ⇒ e ≤ head(q);");
        ParseResult parsed = Parser.parse(broken);
        assertFalse(parsed.hasErrors(), () -> String.valueOf(parsed.errors()));
        EquationChecker.SpecReport report = EquationChecker.checkSpec(parsed.trait());
        assertEquals(EquationChecker.SpecReport.Verdict.CONTRADICTION, report.verdict(),
                () -> report.render());
    }

    @Test
    @DisplayName("exempting 与 converts 被记录")
    void convertsAndExemptingPresent() {
        ParseResult parsed = Parser.parse(PQUEUE);
        assertTrue(parsed.traits().get(0).sections().stream()
                .anyMatch(compiler.parser.Ast.Exempting.class::isInstance));
        EquationChecker.SpecReport report = EquationChecker.checkSpec(parsed.traits().get(0));
        assertEquals(java.util.List.of("head", "tail", "isEmpty", "count"), report.converts());
    }
}
