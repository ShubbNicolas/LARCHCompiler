package compiler.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 复杂案例：PairwiseExtension / PairwiseSum，覆盖运算符作 trait 参数、
 * 空 implies 块、exempting 带量化变量与表达式、多重命名实参。
 */
class PairwiseExtensionParsingTest {

    private static final String SAMPLE = """
            PairwiseExtension(⊕, ⊗, E, C): trait
              assumes Container(E, C)
              introduces
                __ ⊕ __: E, E → E
                __ ⊗ __: C, C → C
              asserts e1, e2: E, c1, c2: C
                empty ⊗ empty == empty;
                (~isEmpty(c1) ∧ ~isEmpty(c2)) ⇒
                  c1 ⊗ c2 == insert(head(c1) ⊕ head(c2),
                                    tail(c1) ⊗ tail(c2));
              implies
              converts ⊗
              exempting e: E, c: C
                empty ⊗ insert(e, c),
                insert(e, c) ⊗ empty

            PairwiseSum(C): trait
              assumes Container(Int, C)
              includes Integer, PairwiseExtension(+, *, Int, C)
              implies Associative(⊗, C),
                Commutative(⊗ for ⊕, C for T, C for Range)
            """;


    private static <T> T section(Ast.Trait trait, Class<T> type) {
        return trait.sections().stream()
                .filter(type::isInstance)
                .map(type::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("缺少段: " + type.getSimpleName()));
    }
    private static ParseResult parsed;

    private static ParseResult result() {
        if (parsed == null) {
            parsed = Parser.parse(SAMPLE);
        }
        return parsed;
    }

    @Test
    @DisplayName("运算符作为 trait 参数")
    void operatorParameters() {
        ParseResult result = result();
        assertFalse(result.hasErrors(), () -> "语法错误: " + result.errors());
        assertEquals(2, result.traits().size());
        assertEquals(List.of("⊕", "⊗", "E", "C"), result.traits().get(0).parameters());
    }

    @Test
    @DisplayName("__ ⊕ __ / __ ⊗ __ 算子签名")
    void operatorSignatures() {
        Ast.Trait trait = result().traits().get(0);
        Ast.Introduces intro = (Ast.Introduces) trait.sections().get(1);
        assertEquals("__ ⊕ __", intro.signatures().get(0).name());
        assertEquals("__ ⊗ __", intro.signatures().get(1).name());
    }

    @Test
    @DisplayName("空 implies 块也能被识别")
    void emptyImpliesBlock() {
        Ast.Trait trait = result().traits().get(0);
        Ast.Implies implies = trait.sections().stream()
                .filter(Ast.Implies.class::isInstance)
                .map(Ast.Implies.class::cast)
                .findFirst()
                .orElseThrow();
        assertTrue(implies.variables().isEmpty());
        assertTrue(implies.equations().isEmpty());
    }

    @Test
    @DisplayName("converts 接受运算符")
    void convertsOperator() {
        Ast.Trait trait = result().traits().get(0);
        Ast.Converts converts = section(trait, Ast.Converts.class);
        assertEquals("⊗", converts.operators().get(0).name());
    }

    @Test
    @DisplayName("exempting 带量化变量与表达式列表")
    void exemptingWithVariables() {
        Ast.Trait trait = result().traits().get(0);
        Ast.Exempting exempting = section(trait, Ast.Exempting.class);
        assertEquals(2, exempting.variables().size());
        assertEquals("e", exempting.variables().get(0).names().get(0));
        assertEquals(2, exempting.cases().size());
        assertInstanceOf(Ast.Infix.class, exempting.cases().get(0));
    }

    @Test
    @DisplayName("includes 的参数可以是运算符与排序")
    void includesWithOperatorArguments() {
        Ast.Trait sum = result().traits().get(1);
        Ast.Includes includes = section(sum, Ast.Includes.class);
        Ast.Ref ref = includes.traits().get(1);
        assertEquals("PairwiseExtension", ref.name());
        assertEquals(List.of("+", "*", "Int", "C"), ref.arguments());
    }

    @Test
    @DisplayName("implies 引用带运算符实参与多重命名")
    void impliesReferencesWithRenames() {
        Ast.Trait sum = result().traits().get(1);
        assertEquals(2, sum.implied().size());
        assertEquals("Associative(⊗, C)", sum.implied().get(0).display());
        assertEquals("Commutative(⊗ for ⊕, C for T, C for Range)",
                sum.implied().get(1).display());
    }
}
