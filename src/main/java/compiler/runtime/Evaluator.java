package compiler.runtime;

import compiler.parser.Ast;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 表达式求值器。
 *
 * <p>内置参考模型：</p>
 * <ul>
 *   <li>{@code new} → 空 Tab</li>
 *   <li>{@code add(t, i, v)} → 插入/覆盖后的 Tab</li>
 *   <li>{@code lookup(t, i)} → 命中返回值，未命中返回 {@link Value.Undefined}</li>
 *   <li>{@code isEmpty(t)} / {@code size(t)} → 布尔 / 整数</li>
 *   <li>{@code ∈ ∉ ∧ ∨ = ≠ &lt; &gt; &lt;= &gt;= + - * /} 直接按运算语义求值</li>
 * </ul>
 */
public final class Evaluator {

    private Evaluator() {
    }

    public static Value eval(Ast.Expr expr, Map<String, Value> env) {
        if (expr instanceof Ast.Num n) {
            return new Value.IntVal(n.value());
        }
        if (expr instanceof Ast.Text t) {
            return new Value.Unknown("不支持字符串值: " + t.value());
        }
        if (expr instanceof Ast.Name n) {
            if (env.containsKey(n.id())) {
                return env.get(n.id());
            }
            if ("new".equals(n.id())) {
                return new Value.MapVal(Map.of());
            }
            if ("true".equals(n.id())) {
                return new Value.BoolVal(true);
            }
            if ("false".equals(n.id())) {
                return new Value.BoolVal(false);
            }
            return new Value.Unknown("未定义的标识符 " + n.id());
        }
        if (expr instanceof Ast.Apply a) {
            return apply(a, env);
        }
        if (expr instanceof Ast.Infix i) {
            return infix(i, env);
        }
        if (expr instanceof Ast.Negate n) {
            Value inner = eval(n.operand(), env);
            if (inner instanceof Value.BoolVal b) {
                return new Value.BoolVal(!b.value());
            }
            return new Value.Unknown("~ 需要布尔操作数");
        }
        if (expr instanceof Ast.Conditional c) {
            Value cond = eval(c.condition(), env);
            if (cond instanceof Value.BoolVal b) {
                return eval(b.value() ? c.whenTrue() : c.whenFalse(), env);
            }
            return new Value.Unknown("if 条件不是布尔值");
        }
        return new Value.Unknown("未知表达式节点");
    }

    private static Value apply(Ast.Apply apply, Map<String, Value> env) {
        List<Value> args = new ArrayList<>();
        for (Ast.Expr arg : apply.arguments()) {
            Value v = eval(arg, env);
            if (v instanceof Value.Unknown) {
                return v;
            }
            args.add(v);
        }
        switch (apply.function()) {
            case "add" -> {
                if (args.size() != 3 || !(args.get(0) instanceof Value.MapVal tab)
                        || !(args.get(1) instanceof Value.IntVal key)
                        || !(args.get(2) instanceof Value.IntVal value)) {
                    return new Value.Unknown("add 参数类型不匹配");
                }
                Map<Integer, Integer> copy = new LinkedHashMap<>(tab.entries());
                copy.put(key.value(), value.value());
                return new Value.MapVal(copy);
            }
            case "lookup" -> {
                if (args.size() != 2 || !(args.get(0) instanceof Value.MapVal tab)
                        || !(args.get(1) instanceof Value.IntVal key)) {
                    return new Value.Unknown("lookup 参数类型不匹配");
                }
                Integer found = tab.entries().get(key.value());
                return found == null
                        ? new Value.Undefined("lookup(" + key.value() + ")")
                        : new Value.IntVal(found);
            }
            case "isEmpty" -> {
                if (args.size() != 1 || !(args.get(0) instanceof Value.MapVal tab)) {
                    return new Value.Unknown("isEmpty 参数类型不匹配");
                }
                return new Value.BoolVal(tab.entries().isEmpty());
            }
            case "size" -> {
                if (args.size() != 1 || !(args.get(0) instanceof Value.MapVal tab)) {
                    return new Value.Unknown("size 参数类型不匹配");
                }
                return new Value.IntVal(tab.entries().size());
            }
            default -> {
                return new Value.Unknown("未实现的算子 " + apply.function());
            }
        }
    }

    private static Value infix(Ast.Infix infix, Map<String, Value> env) {
        Value left = eval(infix.left(), env);
        if (left instanceof Value.Unknown) {
            return left;
        }
        Value right = eval(infix.right(), env);
        if (right instanceof Value.Unknown) {
            return right;
        }

        String op = infix.operator();
        switch (op) {
            case "∧", "∨" -> {
                if (left instanceof Value.BoolVal l && right instanceof Value.BoolVal r) {
                    return new Value.BoolVal("∧".equals(op) ? l.value() && r.value() : l.value() || r.value());
                }
                return new Value.Unknown(op + " 需要布尔操作数");
            }
            case "=", "≠" -> {
                boolean equal = same(left, right);
                return new Value.BoolVal("=".equals(op) == equal);
            }
            case "∈", "∉" -> {
                if (left instanceof Value.IntVal key && right instanceof Value.MapVal tab) {
                    boolean contains = tab.entries().containsKey(key.value());
                    return new Value.BoolVal("∈".equals(op) == contains);
                }
                return new Value.Unknown(op + " 需要 (整数, Tab)");
            }
            case "<", ">", "<=", ">=" -> {
                if (left instanceof Value.IntVal l && right instanceof Value.IntVal r) {
                    boolean result = switch (op) {
                        case "<" -> l.value() < r.value();
                        case ">" -> l.value() > r.value();
                        case "<=" -> l.value() <= r.value();
                        default -> l.value() >= r.value();
                    };
                    return new Value.BoolVal(result);
                }
                return new Value.Unknown(op + " 需要整数操作数");
            }
            case "+", "-", "*", "/" -> {
                if (left instanceof Value.IntVal l && right instanceof Value.IntVal r) {
                    int result = switch (op) {
                        case "+" -> l.value() + r.value();
                        case "-" -> l.value() - r.value();
                        case "*" -> l.value() * r.value();
                        default -> r.value() == 0 ? 0 : l.value() / r.value();
                    };
                    return new Value.IntVal(result);
                }
                return new Value.Unknown(op + " 需要整数操作数");
            }
            default -> {
                return new Value.Unknown("未实现的运算符 " + op);
            }
        }
    }

    /** 结构相等：Undefined 只与 Undefined 相等。 */
    public static boolean same(Value a, Value b) {
        if (a.getClass() != b.getClass()) {
            return false;
        }
        return a.equals(b);
    }
}
