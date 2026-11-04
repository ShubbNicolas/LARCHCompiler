package compiler.runtime;

import java.util.List;
import java.util.Map;

/**
 * 求值过程中出现的值。
 *
 * <p>LSL 没有执行语义，这里用内置参考模型给各排序赋值：</p>
 * <ul>
 *   <li>{@code Tab} → {@code Map<Integer,Integer>}</li>
 *   <li>{@code C} / {@code Container} → 整数列表</li>
 *   <li>{@code Str} → 整数列表（把字符编码成整数）</li>
 *   <li>{@code Int} / {@code Ind} / {@code Val} / {@code E} → 整数</li>
 *   <li>{@code Bool} → 布尔</li>
 * </ul>
 */
public sealed interface Value
        permits Value.IntVal, Value.BoolVal, Value.MapVal, Value.ListVal,
                Value.Undefined, Value.Unknown {

    /** 整数。 */
    record IntVal(int value) implements Value {
    }

    /** 布尔。 */
    record BoolVal(boolean value) implements Value {
    }

    /** Tab 的实现：键值对。 */
    record MapVal(Map<Integer, Integer> entries) implements Value {
        public MapVal {
            entries = Map.copyOf(entries);
        }
    }

    /** 序列（容器 / 字符串）的实现：整数列表。 */
    record ListVal(List<Integer> items) implements Value {
        public ListVal {
            items = List.copyOf(items);
        }

        public int size() {
            return items.size();
        }

        public boolean isEmpty() {
            return items.isEmpty();
        }

        public int head() {
            return items.get(0);
        }

        public ListVal tail() {
            return new ListVal(items.subList(1, items.size()));
        }
    }

    /** 未定义（lookup 未命中、head/tail 作用于空序列）。两个 Undefined 视为相等。 */
    record Undefined(String what) implements Value {
    }

    /** 无法求值，检查结果记为 UNKNOWN。 */
    record Unknown(String reason) implements Value {
    }

    /** 人类可读形式，用于反例展示。 */
    static String show(Value value) {
        if (value instanceof IntVal i) {
            return String.valueOf(i.value());
        }
        if (value instanceof BoolVal b) {
            return String.valueOf(b.value());
        }
        if (value instanceof MapVal m) {
            return m.entries().toString();
        }
        if (value instanceof ListVal l) {
            return l.items().toString();
        }
        if (value instanceof Undefined u) {
            return "⊥(" + u.what() + ")";
        }
        if (value instanceof Unknown u) {
            return "?" + u.reason();
        }
        return String.valueOf(value);
    }
}
