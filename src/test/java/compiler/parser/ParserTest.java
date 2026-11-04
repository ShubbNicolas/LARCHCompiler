package compiler.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParserTest {

    /** 用户提供的 Table1 trait 样例（补回传输中丢失的符号）。 */
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

    @Test
    @DisplayName("解析 Table1 样例：trait 名与段结构")
    void parseTraitHeader() {
        ParseResult result = Parser.parse(SAMPLE);
        assertFalse(result.hasErrors(), () -> "意外的语法错误: " + result.errors());
        assertNotNull(result.trait());
        assertEquals("Table1", result.trait().name());
        assertEquals(2, result.trait().sections().size());
        assertInstanceOf(Ast.Introduces.class, result.trait().sections().get(0));
        assertInstanceOf(Ast.Asserts.class, result.trait().sections().get(1));
    }

    @Test
    @DisplayName("introduces 段：算子签名解析")
    void parseSignatures() {
        ParseResult result = Parser.parse(SAMPLE);
        Ast.Introduces intro = (Ast.Introduces) result.trait().sections().get(0);
        assertEquals(8, intro.signatures().size());

        Ast.Signature newSig = intro.signatures().get(0);
        assertEquals("new", newSig.name());
        assertTrue(newSig.argumentSorts().isEmpty());
        assertEquals("Tab", newSig.resultSort());

        Ast.Signature addSig = intro.signatures().get(1);
        assertEquals("add", addSig.name());
        assertEquals(java.util.List.of("Tab", "Ind", "Val"), addSig.argumentSorts());
        assertEquals("Tab", addSig.resultSort());

        Ast.Signature plus = intro.signatures().get(7);
        assertEquals("__ + __", plus.name());
        assertEquals(java.util.List.of("Int", "Int"), plus.argumentSorts());
        assertEquals("Int", plus.resultSort());
    }

    @Test
    @DisplayName("asserts 段：变量声明")
    void parseVarDecls() {
        ParseResult result = Parser.parse(SAMPLE);
        Ast.Asserts asserts = (Ast.Asserts) result.trait().sections().get(1);
        assertEquals(3, asserts.variables().size());
        assertEquals(java.util.List.of("i", "i1"), asserts.variables().get(0).names());
        assertEquals("Ind", asserts.variables().get(0).sort());
        assertEquals(java.util.List.of("val"), asserts.variables().get(1).names());
        assertEquals("Val", asserts.variables().get(1).sort());
        assertEquals("Tab", asserts.variables().get(2).sort());
    }

    @Test
    @DisplayName("asserts 段：等式数量与跨行等式")
    void parseEquations() {
        ParseResult result = Parser.parse(SAMPLE);
        Ast.Asserts asserts = (Ast.Asserts) result.trait().sections().get(1);
        assertEquals(6, asserts.equations().size());

        // size(new) == 0
        Ast.Equation sizeNew = asserts.equations().get(3);
        assertInstanceOf(Ast.Apply.class, sizeNew.left());
        assertInstanceOf(Ast.Num.class, sizeNew.right());

        // 跨行: lookup(...) == if i = i1 then val else lookup(t, i1)
        Ast.Equation lookupEq = asserts.equations().get(2);
        assertInstanceOf(Ast.Conditional.class, lookupEq.right());
    }

    @Test
    @DisplayName("关系运算符与逻辑运算符进入 AST")
    void parseOperators() {
        ParseResult result = Parser.parse(SAMPLE);
        Ast.Asserts asserts = (Ast.Asserts) result.trait().sections().get(1);
        // i ∉ add(t, i1, val) == i ≠ i1 ∧ i ∉ t
        Ast.Expr right = asserts.equations().get(1).right();
        assertInstanceOf(Ast.Infix.class, right);
        Ast.Infix and = (Ast.Infix) right;
        assertEquals("∧", and.operator());
        assertEquals("≠", ((Ast.Infix) and.left()).operator());
        assertEquals("∉", ((Ast.Infix) and.right()).operator());
    }

    @Test
    @DisplayName("语法树文本渲染可输出")
    void renderTree() {
        ParseResult result = Parser.parse(SAMPLE);
        String tree = TreePrinter.render(result.trait());
        assertTrue(tree.startsWith("Trait Table1"));
        assertTrue(tree.contains("introduces"));
        assertTrue(tree.contains("add : Tab, Ind, Val → Tab"));
        assertTrue(tree.contains("asserts"));
    }

    @Test
    @DisplayName("缺少 trait 关键字时报错并定位")
    void missingTraitKeyword() {
        ParseResult result = Parser.parse("Table1: xyz");
        assertTrue(result.hasErrors());
        assertNull(result.trait());
        assertTrue(result.errors().get(0).message().contains("trait"));
    }

    @Test
    @DisplayName("空输入报错")
    void emptyInput() {
        ParseResult result = Parser.parse("");
        assertTrue(result.hasErrors());
    }
}
