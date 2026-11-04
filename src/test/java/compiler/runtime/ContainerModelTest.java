package compiler.runtime;

import compiler.parser.ParseResult;
import compiler.parser.Parser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 序列模型（Container）的求值验证：count / insert / head / tail / isEmpty / ∈ 的内置实现。
 */
class ContainerModelTest {

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
            """;

    private static List<CheckResult> results() {
        ParseResult parsed = Parser.parse(CONTAINER);
        assertFalse(parsed.hasErrors(), () -> "语法错误: " + parsed.errors());
        return EquationChecker.check(parsed.trait());
    }

    @Test
    @DisplayName("Container 的等式在模型下不出现失败")
    void noFailures() {
        List<CheckResult> results = results();
        assertEquals(6, results.size());
        List<CheckResult> failed = results.stream()
                .filter(r -> r.status() == CheckResult.Status.FAIL)
                .toList();
        assertTrue(failed.isEmpty(), () -> "出现反例: " + EquationChecker.report(failed));
    }

    @Test
    @DisplayName("count 与 insert 的递推关系成立")
    void countInsertEquation() {
        List<CheckResult> results = results();
        assertEquals(CheckResult.Status.PASS, results.get(3).status(),
                () -> results.get(3).summarize());
    }

    @Test
    @DisplayName("成员关系与 count > 0 等价")
    void membershipMatchesCount() {
        List<CheckResult> results = results();
        assertEquals(CheckResult.Status.PASS, results.get(4).status(),
                () -> results.get(4).summarize());
    }

    @Test
    @DisplayName("head/tail 作用于空序列时按 LSL 惯例跳过该取值，而不是判失败")
    void headTailOnEmptyIsSkipped() {
        List<CheckResult> results = results();
        CheckResult last = results.get(5);
        assertEquals(CheckResult.Status.PASS, last.status(), () -> last.summarize());
        assertTrue(last.skipped() > 0, "应当有因未定义而跳过的取值");
    }

    @Test
    @DisplayName("isEmpty(empty) 与 ~isEmpty(insert(...)) 成立")
    void emptinessEquations() {
        List<CheckResult> results = results();
        assertEquals(CheckResult.Status.PASS, results.get(0).status());
        assertEquals(CheckResult.Status.PASS, results.get(1).status());
        assertEquals(CheckResult.Status.PASS, results.get(2).status());
    }
}
