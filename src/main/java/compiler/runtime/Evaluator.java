package compiler.runtime;

import compiler.parser.Ast;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 表达式求值器。
 *
 * <p>内置两套参考模型：</p>
 * <ul>
 *   <li>Tab（映射）：{@code new} / {@code add} / {@code lookup} / {@code size} / {@code isEmpty}</li>
 *   <li>序列（容器与字符串）：{@code empty} / {@code insert} / {@code head} / {@code tail} /
 *       {@code count} / {@code -|} / {@code ∈} / {@code ∉}</li>
 * </ul>
 *
 * <p>未实现的算子返回 {@link Value.Unknown}，不会被当作通过。</p>
 */
public final class Evaluator {

    private Evaluator() {
    }

    public static Value eval(Ast.Expr expr, Map<String, Value> env) {
        if (expr instanceof Ast.Num n) {
            return new Value.IntVal(n.value());
        }
        if (expr instanceof Ast.Text t) {
            return new Value.Unknown("不支持字符串字面量: " + t.value());
        }
        if (expr instanceof Ast.Name n) {
            return evalName(n.id(), env);
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
            if (cond instanceof Value.Undefined) {
                return cond;
            }
            return new Value.Unknown("if 条件不是布尔值");
        }
        return new Value.Unknown("未知表达式节点");
    }

    private static Value evalName(String id, Map<String, Value> env) {
        if (env.containsKey(id)) {
            return env.get(id);
        }
        return switch (id) {
            case "true" -> new Value.BoolVal(true);
            case "false" -> new Value.BoolVal(false);
            case "new" -> new Value.MapVal(Map.of());
            case "empty" -> new Value.ListVal(List.of());
            default -> new Value.Unknown("未定义的标识符 " + id);
        };
    }

    private static Value apply(Ast.Apply apply, Map<String, Value> env) {
        List<Value> args = new ArrayList<>();
        for (Ast.Expr arg : apply.arguments()) {
            Value v = eval(arg, env);
            if (v instanceof Value.Unknown) {
                return v;
            }
            if (v instanceof Value.Undefined) {
                return v;   // 未定义向调用结果传播
            }
            args.add(v);
        }
        try {
            return switch (apply.function()) {
                case "add" -> add(args);
                case "lookup" -> lookup(args);
                case "insert" -> insert(args);
                case "head" -> head(args);
                case "tail" -> tail(args);
                case "count" -> count(args);
                case "isEmpty" -> isEmpty(args);
                case "size" -> size(args);
                default -> new Value.Unknown("未实现的算子 " + apply.function());
            };
        } catch (ClassCastException | IndexOutOfBoundsException e) {
            return new Value.Unknown(apply.function() + " 参数类型不匹配");
        }
    }

    // ---- Tab 模型 ----

    private static Value add(List<Value> args) {
        if (args.size() != 3) {
            return new Value.Unknown("add 需要 3 个参数");
        }
        Value.MapVal tab = (Value.MapVal) args.get(0);
        int key = ((Value.IntVal) args.get(1)).value();
        int value = ((Value.IntVal) args.get(2)).value();
        Map<Integer, Integer> copy = new LinkedHashMap<>(tab.entries());
        copy.put(key, value);
        return new Value.MapVal(copy);
    }

    private static Value lookup(List<Value> args) {
        if (args.size() != 2) {
            return new Value.Unknown("lookup 需要 2 个参数");
        }
        Value.MapVal tab = (Value.MapVal) args.get(0);
        int key = ((Value.IntVal) args.get(1)).value();
        Integer found = tab.entries().get(key);
        return found == null
                ? new Value.Undefined("lookup(" + key + ")")
                : new Value.IntVal(found);
    }

    // ---- 序列模型 ----

    private static Value insert(List<Value> args) {
        if (args.size() != 2) {
            return new Value.Unknown("insert 需要 2 个参数");
        }
        int item = ((Value.IntVal) args.get(0)).value();
        Value.ListVal list = (Value.ListVal) args.get(1);
        List<Integer> items = new ArrayList<>();
        items.add(item);
        items.addAll(list.items());
        return new Value.ListVal(items);
    }

    private static Value head(List<Value> args) {
        if (args.size() != 1) {
            return new Value.Unknown("head 需要 1 个参数");
        }
        Value.ListVal list = (Value.ListVal) args.get(0);
        return list.isEmpty()
                ? new Value.Undefined("head(empty)")
                : new Value.IntVal(list.head());
    }

    private static Value tail(List<Value> args) {
        if (args.size() != 1) {
            return new Value.Unknown("tail 需要 1 个参数");
        }
        Value.ListVal list = (Value.ListVal) args.get(0);
        return list.isEmpty()
                ? new Value.Undefined("tail(empty)")
                : list.tail();
    }

    private static Value count(List<Value> args) {
        if (args.size() != 2) {
            return new Value.Unknown("count 需要 2 个参数");
        }
        int item = ((Value.IntVal) args.get(0)).value();
        Value.ListVal list = (Value.ListVal) args.get(1);
        int n = 0;
        for (int value : list.items()) {
            if (value == item) {
                n++;
            }
        }
        return new Value.IntVal(n);
    }

    private static Value isEmpty(List<Value> args) {
        if (args.size() != 1) {
            return new Value.Unknown("isEmpty 需要 1 个参数");
        }
        Value arg = args.get(0);
        if (arg instanceof Value.MapVal map) {
            return new Value.BoolVal(map.entries().isEmpty());
        }
        if (arg instanceof Value.ListVal list) {
            return new Value.BoolVal(list.isEmpty());
        }
        return new Value.Unknown("isEmpty 参数类型不支持");
    }

    private static Value size(List<Value> args) {
        if (args.size() != 1) {
            return new Value.Unknown("size 需要 1 个参数");
        }
        Value arg = args.get(0);
        if (arg instanceof Value.MapVal map) {
            return new Value.IntVal(map.entries().size());
        }
        if (arg instanceof Value.ListVal list) {
            return new Value.IntVal(list.size());
        }
        return new Value.Unknown("size 参数类型不支持");
    }

    // ---- 运算符 ----

    private static Value infix(Ast.Infix infix, Map<String, Value> env) {
        String op = infix.operator();

        Value left = eval(infix.left(), env);
        if (left instanceof Value.Unknown) {
            return left;
        }
        // 短路：false ⇒ x、false ∧ x、true ∨ x 都不需要求右侧
        if (left instanceof Value.BoolVal lb) {
            if ("⇒".equals(op) && !lb.value()) {
                return new Value.BoolVal(true);
            }
            if ("∧".equals(op) && !lb.value()) {
                return new Value.BoolVal(false);
            }
            if ("∨".equals(op) && lb.value()) {
                return new Value.BoolVal(true);
            }
        }

        Value right = eval(infix.right(), env);
        if (right instanceof Value.Unknown) {
            return right;
        }

        // 相等比较对「未定义」也成立：⊥ == ⊥
        if ("==".equals(op) || "=".equals(op) || "≠".equals(op)) {
            boolean equal = same(left, right);
            boolean wantEqual = !"≠".equals(op);
            return new Value.BoolVal(wantEqual == equal);
        }

        // 其余运算遇到未定义时向下传播
        if (left instanceof Value.Undefined || right instanceof Value.Undefined) {
            return new Value.Undefined("运算数未定义");
        }

        switch (op) {
            case "∧", "∨" -> {
                if (left instanceof Value.BoolVal l && right instanceof Value.BoolVal r) {
                    return new Value.BoolVal("∧".equals(op) ? l.value() && r.value() : l.value() || r.value());
                }
                return new Value.Unknown(op + " 需要布尔操作数");
            }
            case "⇒" -> {
                if (left instanceof Value.BoolVal l && right instanceof Value.BoolVal r) {
                    return new Value.BoolVal(!l.value() || r.value());
                }
                return new Value.Unknown("⇒ 需要布尔操作数");
            }
            case "∈", "∉" -> {
                boolean contains = contains(left, right);
                return new Value.BoolVal("∈".equals(op) == contains);
            }
            case "-|" -> {
                if (left instanceof Value.IntVal head && right instanceof Value.ListVal tail) {
                    List<Integer> items = new ArrayList<>();
                    items.add(head.value());
                    items.addAll(tail.items());
                    return new Value.ListVal(items);
                }
                return new Value.Unknown("-| 需要 (整数, 序列)");
            }
            case "<", ">", "<=", ">=", "≤", "≥" -> {
                if (left instanceof Value.IntVal l && right instanceof Value.IntVal r) {
                    boolean result = switch (op) {
                        case "<" -> l.value() < r.value();
                        case ">" -> l.value() > r.value();
                        case "<=", "≤" -> l.value() <= r.value();
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

    /** 成员关系：元素在序列/映射中，或序列是另一序列的子串。 */
    private static boolean contains(Value left, Value right) {
        if (left instanceof Value.IntVal element && right instanceof Value.MapVal map) {
            return map.entries().containsKey(element.value());
        }
        if (left instanceof Value.IntVal element && right instanceof Value.ListVal list) {
            return list.items().contains(element.value());
        }
        if (left instanceof Value.ListVal sub && right instanceof Value.ListVal list) {
            return isSubsequence(sub.items(), list.items());
        }
        return false;
    }

    private static boolean isSubsequence(List<Integer> sub, List<Integer> list) {
        if (sub.isEmpty()) {
            return true;
        }
        for (int start = 0; start + sub.size() <= list.size(); start++) {
            if (list.subList(start, start + sub.size()).equals(sub)) {
                return true;
            }
        }
        return false;
    }

    /** 结构相等：Undefined 只与 Undefined 相等。 */
    public static boolean same(Value a, Value b) {
        if (a.getClass() != b.getClass()) {
            return false;
        }
        return a.equals(b);
    }
}
