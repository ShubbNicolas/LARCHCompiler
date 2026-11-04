package compiler.runtime;

import compiler.parser.ParseResult;
import compiler.parser.Parser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bag2：袋上的区间计数。覆盖 { } 字面量、rangeCount 模型与「implies 单行声明」。
 */
class Bag2SpecTest {

    private static final String BAG2 = """
            Bag2(E): trait
              assumes TotalOrder(E)
              includes Bag0, Integer
              introduces
                rangeCount: E, E, B → Int
              asserts e1, e2, e3: E, b: B
                rangeCount(e1, e2, { }) == 0;
                rangeCount(e1, e2, insert(e3, b)) ==
                  rangeCount(e1, e2, b)
                  + (if e1 ≤ e3 ∧ e3 ≤ e2 then 1 else 0)
              implies e1, e2, e3: E, b: B
                e1 ≤ e2 ⇒
                  rangeCount(e3, e1, b) ≤ rangeCount(e3, e2, b)
            """;

    private static EquationChecker.SpecReport spec() {
        ParseResult parsed = Parser.parse(BAG2);
        assertFalse(parsed.hasErrors(), () -> "语法错误: " + parsed.errors());
        return EquationChecker.checkSpec(parsed.trait());
    }

    @Test
    @DisplayName("{ } 字面量与 rangeCount 让两条定义通过")
    void definitionsPass() {
        EquationChecker.SpecReport report = spec();
        assertEquals(2, report.asserts().size());
        assertTrue(report.asserts().stream()
                .allMatch(r -> r.status() == CheckResult.Status.PASS), () -> report.render());
    }

    @Test
    @DisplayName("区间单调性推论成立")
    void implicationHolds() {
        EquationChecker.SpecReport report = spec();
        assertEquals(1, report.implies().size());
        assertEquals(CheckResult.Status.PASS, report.implies().get(0).status(),
                () -> report.implies().get(0).summarize());
    }

    @Test
    @DisplayName("整份规格判定为自洽")
    void consistent() {
        assertEquals(EquationChecker.SpecReport.Verdict.CONSISTENT, spec().verdict(),
                () -> spec().render());
    }

    @Test
    @DisplayName("把空袋的 rangeCount 改成 1 会立即矛盾")
    void brokenEmptyCase() {
        String broken = BAG2.replace("rangeCount(e1, e2, { }) == 0;", "rangeCount(e1, e2, { }) == 1;");
        ParseResult parsed = Parser.parse(broken);
        assertFalse(parsed.hasErrors(), () -> String.valueOf(parsed.errors()));
        assertEquals(EquationChecker.SpecReport.Verdict.CONTRADICTION,
                EquationChecker.checkSpec(parsed.trait()).verdict());
    }

    @Test
    @DisplayName("推论反向（≥）会矛盾")
    void brokenImplication() {
        String broken = BAG2.replace(
                "rangeCount(e3, e1, b) ≤ rangeCount(e3, e2, b)",
                "rangeCount(e3, e1, b) ≥ rangeCount(e3, e2, b)");
        ParseResult parsed = Parser.parse(broken);
        assertFalse(parsed.hasErrors(), () -> String.valueOf(parsed.errors()));
        assertEquals(EquationChecker.SpecReport.Verdict.CONTRADICTION,
                EquationChecker.checkSpec(parsed.trait()).verdict());
    }
}
