package compiler.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 复杂案例：Container + PQueue 双 trait 文件，覆盖 partitioned by、converts、
 * exempting、implies 公式块、includes Container(Q for C) 重命名实例化、≤ 与 ⇒。
 */
class ContainerPQueueParsingTest {

    private static final String SAMPLE = """
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
              converts isEmpty, count, ∈

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
                e ∈ q ⇒ e ≤ head(q);
              converts head, tail, isEmpty, count
              exempting head(empty), tail(empty)
            """;

    private static ParseResult parsed;

    private static ParseResult result() {
        if (parsed == null) {
            parsed = Parser.parse(SAMPLE);
        }
        return parsed;
    }

    @Test
    @DisplayName("一个文件解析出两个 trait")
    void twoTraits() {
        ParseResult result = result();
        assertFalse(result.hasErrors(), () -> "语法错误: " + result.errors());
        assertEquals(2, result.traits().size());
        assertEquals("Container", result.traits().get(0).name());
        assertEquals("PQueue", result.traits().get(1).name());
    }

    @Test
    @DisplayName("Container: includes 两个 trait、partitioned by 子句")
    void containerSections() {
        Ast.Trait container = result().traits().get(0);
        Ast.Includes includes = (Ast.Includes) container.sections().get(0);
        assertEquals(List.of("InsertGenerated", "Integer"),
                includes.traits().stream().map(Ast.Ref::name).toList());

        Ast.Asserts asserts = (Ast.Asserts) container.sections().get(2);
        assertEquals("C", asserts.partitionedSort());
        assertEquals(List.of("isEmpty", "head", "tail"),
                asserts.partitionedBy().stream().map(Ast.Ref::name).toList());
        assertEquals(6, asserts.equations().size());
    }

    @Test
    @DisplayName("Container: implies 公式块与 converts")
    void containerImpliesAndConverts() {
        Ast.Trait container = result().traits().get(0);
        Ast.Implies implies = (Ast.Implies) container.sections().get(3);
        assertEquals(1, implies.variables().size());
        assertEquals("c", implies.variables().get(0).names().get(0));
        assertEquals(1, implies.equations().size());

        Ast.Converts converts = (Ast.Converts) container.sections().get(4);
        assertEquals(3, converts.operators().size());
        assertEquals("∈", converts.operators().get(2).name());
    }

    @Test
    @DisplayName("PQueue: includes 重命名实例化 Q for C")
    void pqueueRenaming() {
        Ast.Trait pqueue = result().traits().get(1);
        Ast.Includes includes = (Ast.Includes) pqueue.sections().get(1);
        Ast.Ref ref = includes.traits().get(0);
        assertEquals("Container", ref.name());
        assertEquals(1, ref.renames().size());
        assertEquals("Q", ref.renames().get(0).from());
        assertEquals("C", ref.renames().get(0).to());
        assertEquals("Container(Q for C)", ref.display());
    }

    @Test
    @DisplayName("PQueue: ≤ 与 ⇒ 进入 AST")
    void pqueueOperators() {
        Ast.Trait pqueue = result().traits().get(1);
        Ast.Asserts asserts = (Ast.Asserts) pqueue.sections().get(2);
        // tail(insert(e, q)) == if ... else ...
        Ast.Equation tailEq = asserts.equations().get(1);
        assertInstanceOf(Ast.Conditional.class, tailEq.right());

        Ast.Implies implies = (Ast.Implies) pqueue.sections().get(3);
        Ast.Infix implication = (Ast.Infix) implies.equations().get(0).left();
        assertEquals("⇒", implication.operator());
        assertInstanceOf(Ast.Infix.class, implication.right());
        assertEquals("≤", ((Ast.Infix) implication.right()).operator());
    }

    @Test
    @DisplayName("PQueue: exempting 子句")
    void pqueueExempting() {
        Ast.Trait pqueue = result().traits().get(1);
        Ast.Exempting exempting = (Ast.Exempting) pqueue.sections().get(5);
        assertEquals(2, exempting.cases().size());
        assertEquals("head(empty)", TreePrinter.expr(exempting.cases().get(0)));
    }

    @Test
    @DisplayName("双 trait 语法树渲染")
    void renderBoth() {
        String tree = String.join("\n", result().traits().stream()
                .map(TreePrinter::render).toList());
        assertTrue(tree.contains("Trait Container(E, C)"));
        assertTrue(tree.contains("C partitioned by isEmpty, head, tail"));
        assertTrue(tree.contains("Trait PQueue(E, Q)"));
        assertTrue(tree.contains("includes Container(Q for C)"));
        assertTrue(tree.contains("exempting head(empty), tail(empty)"));
    }
}
