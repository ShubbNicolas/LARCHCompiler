package compiler.runtime;

import compiler.parser.Ast;
import compiler.parser.ParseResult;
import compiler.parser.Parser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EquationCheckerTest {

    private static final String SAMPLE = """
            Table1: trait
            introduces
              new: → Tab
              add: Tab, Ind, Val → Tab
              __ ∈ __: Ind, Tab → Bool
              lookup: Tab, Ind → Val
              isEmpty: Tab → Bool
              size: Tab → Int
              0, 1: → Int
              __ + __: Int, Int → Int
            asserts i, i1: Ind, val: Val, t: Tab
              (i ∉ new);
              i ∉ add(t, i1, val) == i ≠ i1 ∧ i ∉ t;
              lookup(add(t, i, val), i1) ==
                if i = i1 then val else lookup(t, i1);
              size(new) == 0;
              size(add(t, i, val)) ==
                if i ∈ t then size(t) else size(t) + 1;
              isEmpty(t) == size(t) = 0
            """;

    private static List<CheckResult> check(String source) {
        ParseResult parsed = Parser.parse(source);
        assertFalse(parsed.hasErrors(), () -> "语法错误: " + parsed.errors());
        return EquationChecker.check(parsed.trait());
    }

    @Test
    @DisplayName("Table1 的 6 条等式全部通过")
    void sampleAllPass() {
        List<CheckResult> results = check(SAMPLE);
        assertEquals(6, results.size());
        List<CheckResult> failed = results.stream()
                .filter(r -> r.status() != CheckResult.Status.PASS)
                .toList();
        assertTrue(failed.isEmpty(), () -> "未通过的等式: " + EquationChecker.report(failed));
    }

    @Test
    @DisplayName("错误等式能给出反例")
    void wrongEquationFails() {
        String src = SAMPLE.replace("size(new) == 0;", "size(new) == 1;");
        List<CheckResult> results = check(src);
        CheckResult bad = results.get(3);
        assertEquals(CheckResult.Status.FAIL, bad.status());
        assertEquals("0", bad.witness().get("t") == null ? "0" : "0");
        assertTrue(bad.message().contains("左 = 0"));
    }

    @Test
    @DisplayName("未实现算子记为无法求值，而不是通过")
    void unknownOperator() {
        String src = SAMPLE.replace("size(new) == 0;", "foo(new) == 0;");
        List<CheckResult> results = check(src);
        CheckResult unknown = results.get(3);
        assertEquals(CheckResult.Status.UNKNOWN, unknown.status());
        assertTrue(unknown.message().contains("foo"));
    }

    @Test
    @DisplayName("没有 asserts 段时返回空结果")
    void noAsserts() {
        String src = """
                Simple: trait
                introduces
                  new: → Tab
                """;
        assertTrue(check(src).isEmpty());
    }

    @Test
    @DisplayName("报告包含汇总统计")
    void reportSummary() {
        String report = EquationChecker.report(check(SAMPLE));
        assertTrue(report.contains("共 6 条等式"));
        assertTrue(report.contains("通过 6"));
    }

    @Test
    @DisplayName("等式两侧都无定义时视为相等")
    void undefinedOnBothSides() {
        String src = """
                T: trait
                introduces
                  new: → Tab
                  lookup: Tab, Ind → Val
                asserts t: Tab, i: Ind
                  lookup(new, i) == lookup(new, i)
                """;
        List<CheckResult> results = check(src);
        assertEquals(1, results.size());
        assertEquals(CheckResult.Status.PASS, results.get(0).status());
    }

    @Test
    @DisplayName("枚举覆盖全部变量组合")
    void enumerationCoversAllVariables() {
        // 若 i 与 i1 未被区分枚举，这条等式会被误判
        String src = """
                T: trait
                introduces
                  new: → Tab
                  add: Tab, Ind, Val → Tab
                asserts i, i1: Ind, val: Val, t: Tab
                  add(add(t, i, val), i1, val) == add(add(t, i1, val), i, val)
                """;
        List<CheckResult> results = check(src);
        assertEquals(CheckResult.Status.PASS, results.get(0).status());
    }
}
