package compiler.parser;

import java.util.List;

/**
 * LARCH（LSL 子集）抽象语法树。
 *
 * <p>覆盖实际样例 {@code Table1: trait} 的结构：</p>
 * <pre>
 * Table1: trait
 * introduces
 *   new: → Tab
 *   add: Tab, Ind, Val → Tab
 * asserts  i, i1: Ind, val: Val, t: Tab
 *   i ∉ add(t, i1, val) == i ≠ i1 ∧ i ∉ t;
 * </pre>
 */
public final class Ast {

    private Ast() {
    }

    /** 一个完整的 trait 规格。 */
    public record Trait(String name, List<Section> sections) {
    }

    public sealed interface Section permits Introduces, Asserts, Includes {
    }

    /** {@code introduces} 段：算子签名列表。 */
    public record Introduces(List<Signature> signatures) implements Section {
    }

    /** {@code includes} 段：被包含的其它 trait 名。 */
    public record Includes(List<String> traits) implements Section {
    }

    /** {@code asserts} 段：变量声明 + 等式列表。 */
    public record Asserts(List<VarDecl> variables, List<Equation> equations) implements Section {
    }

    /**
     * 算子签名，例如 {@code add: Tab, Ind, Val → Tab}。
     *
     * @param name          算子名，可能是中缀占位形式（如 {@code __ + __}）
     * @param argumentSorts 参数排序
     * @param resultSort    结果排序
     */
    public record Signature(String name, List<String> argumentSorts, String resultSort) {
    }

    /** 变量声明，例如 {@code i, i1: Ind}。 */
    public record VarDecl(List<String> names, String sort) {
    }

    /** 等式，例如 {@code size(new) == 0}。 */
    public record Equation(Expr left, Expr right) {
    }

    public sealed interface Expr permits Num, Text, Name, Apply, Infix, Negate, Conditional {
    }

    public record Num(int value) implements Expr {
    }

    public record Text(String value) implements Expr {
    }

    public record Name(String id) implements Expr {
    }

    public record Apply(String function, List<Expr> arguments) implements Expr {
    }

    public record Infix(String operator, Expr left, Expr right) implements Expr {
    }

    public record Negate(Expr operand) implements Expr {
    }

    public record Conditional(Expr condition, Expr whenTrue, Expr whenFalse) implements Expr {
    }
}
