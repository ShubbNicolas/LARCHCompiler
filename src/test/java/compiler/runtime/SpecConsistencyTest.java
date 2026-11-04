package compiler.runtime;

import compiler.parser.Ast;
import compiler.parser.ParseResult;
import compiler.parser.Parser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 规格一致性检查：asserts 定义自洽 + implies 推论成立。
 * 用用户提供的 Container 案例作为基准。
 */
class SpecConsistencyTest {

    private static final String CONTAINER = """
            Container(E, C): trait
              includes InsertGenerated, Integer
              introduces
                isEmpty: C → Bool
                count: E, C → Int
                __ ∈ __: E, C → Bool
                head: C → E
                tail: C → C
              asserts
                C partitioned by isEmpty, head, tail
                e, e1: E, c: C
                isEmpty(empty);
                ~isEmpty(insert(e, c));
                count(e, empty) == 0;
                count(e, insert(e1, c)) ==
                  count(e, c) + (if e = e1 then 1 else 0);
                (e ∈ c) == count(e, c) > 0;
                count(e, insert(head(c), tail(c))) == count(e, c)
              implies
                c: C
                ~isEmpty(c) ⇒ count(head(c), c) > 0;
              converts isEmpty, count
            """;

    private static EquationChecker.SpecReport spec(String source) {
        ParseResult parsed = Parser.parse(source);
        assertFalse(parsed.hasErrors(), () -> "语法错误: " + parsed.errors());
        return EquationChecker.checkSpec(parsed.trait());
    }

    @Test
    @DisplayName("Container：asserts 与 implies 全部成立，判定为自洽")
    void containerIsConsistent() {
        EquationChecker.SpecReport report = spec(CONTAINER);
        assertEquals(EquationChecker.SpecReport.Verdict.CONSISTENT, report.verdict(),
                () -> report.render());
        assertEquals(6, report.asserts().size());
        assertEquals(1, report.implies().size());
        assertTrue(report.asserts().stream()
                .allMatch(r -> r.status() == CheckResult.Status.PASS), () -> report.render());
        assertEquals(CheckResult.Status.PASS, report.implies().get(0).status(),
                () -> report.implies().get(0).summarize());
    }

    @Test
    @DisplayName("报告区分 introduces / asserts / implies / converts")
    void reportSections() {
        String text = spec(CONTAINER).render();
        assertTrue(text.contains("introduces: isEmpty/1, count/2, __ ∈ __/2, head/1, tail/1"), text);
        assertTrue(text.contains("asserts（定义自洽性）: 6 条"), text);
        assertTrue(text.contains("implies（推论）: 1 条"), text);
        assertTrue(text.contains("converts: isEmpty, count"), text);
        assertTrue(text.contains("规格自洽"), text);
    }

    @Test
    @DisplayName("asserts 被破坏时判定为矛盾")
    void brokenAssertsIsContradiction() {
        String broken = CONTAINER.replace("count(e, empty) == 0;", "count(e, empty) == 1;");
        EquationChecker.SpecReport report = spec(broken);
        assertEquals(EquationChecker.SpecReport.Verdict.CONTRADICTION, report.verdict());
        assertTrue(report.asserts().stream()
                .anyMatch(r -> r.status() == CheckResult.Status.FAIL));
    }

    @Test
    @DisplayName("implies 不成立时判定为矛盾")
    void brokenImplicationIsContradiction() {
        String broken = CONTAINER.replace(
                "~isEmpty(c) ⇒ count(head(c), c) > 0;",
                "~isEmpty(c) ⇒ count(head(c), c) > 1;");
        EquationChecker.SpecReport report = spec(broken);
        assertEquals(EquationChecker.SpecReport.Verdict.CONTRADICTION, report.verdict(),
                () -> report.render());
        assertEquals(CheckResult.Status.FAIL, report.implies().get(0).status(),
                () -> report.implies().get(0).summarize());
    }

    @Test
    @DisplayName("未实现算子导致无法验证，而不是自洽")
    void unimplementedOperatorIsUnverifiable() {
        String withUnknown = CONTAINER.replace("count(e, empty) == 0;", "foo(e) == 0;");
        assertEquals(EquationChecker.SpecReport.Verdict.UNVERIFIABLE,
                spec(withUnknown).verdict());
    }

    @Test
    @DisplayName("没有 implies 段时只看 asserts")
    void withoutImplies() {
        String source = """
                T: trait
                introduces
                  new: → Tab
                  size: Tab → Int
                asserts t: Tab
                  size(new) == 0
                """;
        EquationChecker.SpecReport report = spec(source);
        assertTrue(report.implies().isEmpty());
        assertEquals(EquationChecker.SpecReport.Verdict.CONSISTENT, report.verdict());
    }
}
