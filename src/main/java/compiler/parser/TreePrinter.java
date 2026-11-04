package compiler.parser;

/**
 * 把 AST 渲染成缩进文本，用于界面右侧「语法分析」结果区。
 */
public final class TreePrinter {

    private TreePrinter() {
    }

    public static String render(Ast.Trait trait) {
        if (trait == null) {
            return "(无语法树)";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Trait ").append(trait.name()).append('\n');
        for (int i = 0; i < trait.sections().size(); i++) {
            boolean last = i == trait.sections().size() - 1;
            renderSection(sb, trait.sections().get(i), "", last);
        }
        return sb.toString();
    }

    private static void renderSection(StringBuilder sb, Ast.Section section, String indent, boolean last) {
        String branch = last ? "└─ " : "├─ ";
        String childIndent = indent + (last ? "   " : "│  ");

        if (section instanceof Ast.Introduces intro) {
            sb.append(indent).append(branch).append("introduces\n");
            for (int i = 0; i < intro.signatures().size(); i++) {
                Ast.Signature sig = intro.signatures().get(i);
                boolean sigLast = i == intro.signatures().size() - 1;
                sb.append(childIndent).append(sigLast ? "└─ " : "├─ ")
                        .append(sig.name()).append(" : ");
                if (!sig.argumentSorts().isEmpty()) {
                    sb.append(String.join(", ", sig.argumentSorts())).append(" → ");
                } else {
                    sb.append("→ ");
                }
                sb.append(sig.resultSort()).append('\n');
            }
            return;
        }

        if (section instanceof Ast.Asserts asserts) {
            sb.append(indent).append(branch).append("asserts\n");
            if (!asserts.variables().isEmpty()) {
                sb.append(childIndent).append("├─ vars: ");
                for (int i = 0; i < asserts.variables().size(); i++) {
                    Ast.VarDecl decl = asserts.variables().get(i);
                    if (i > 0) {
                        sb.append("; ");
                    }
                    sb.append(String.join(", ", decl.names())).append(": ").append(decl.sort());
                }
                sb.append('\n');
            }
            for (int i = 0; i < asserts.equations().size(); i++) {
                Ast.Equation eq = asserts.equations().get(i);
                boolean eqLast = i == asserts.equations().size() - 1;
                sb.append(childIndent).append(eqLast ? "└─ " : "├─ ")
                        .append(expr(eq.left())).append(" == ").append(expr(eq.right()))
                        .append('\n');
            }
            return;
        }

        if (section instanceof Ast.Includes includes) {
            sb.append(indent).append(branch).append("includes ")
                    .append(String.join(", ", includes.traits())).append('\n');
            return;
        }

        sb.append(indent).append(branch).append(section).append('\n');
    }

    public static String expr(Ast.Expr expression) {
        if (expression instanceof Ast.Num n) {
            return String.valueOf(n.value());
        }
        if (expression instanceof Ast.Text t) {
            return '"' + t.value() + '"';
        }
        if (expression instanceof Ast.Name n) {
            return n.id();
        }
        if (expression instanceof Ast.Apply a) {
            return a.function() + "(" + String.join(", ", a.arguments().stream()
                    .map(TreePrinter::expr).toList()) + ")";
        }
        if (expression instanceof Ast.Infix i) {
            return "(" + expr(i.left()) + " " + i.operator() + " " + expr(i.right()) + ")";
        }
        if (expression instanceof Ast.Negate n) {
            return "~" + expr(n.operand());
        }
        if (expression instanceof Ast.Conditional c) {
            return "if " + expr(c.condition()) + " then " + expr(c.whenTrue())
                    + " else " + expr(c.whenFalse());
        }
        return String.valueOf(expression);
    }
}
