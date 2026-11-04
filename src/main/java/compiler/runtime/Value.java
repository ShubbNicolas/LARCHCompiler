package compiler.runtime;

import java.util.Map;

/**
 * 求值过程中出现的值。
 *
 * <p>LSL 没有执行语义，这里用一个内置参考模型给算子赋值：
 * {@code Tab} 用 {@code Map<Integer,Integer>} 表示，其余排序用整数/布尔。</p>
 */
public sealed interface Value
        permits Value.IntVal, Value.BoolVal, Value.MapVal, Value.Undefined, Value.Unknown {

    /** 整数（Int / Ind / Val 都按整数枚举）。 */
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

    /**
     * 未定义（如 lookup 查询不存在的键）。
     * 两个 Undefined 视为相等，用于「等式的两边都无定义」这种合法情况。
     */
    record Undefined(String what) implements Value {
    }

    /** 无法求值（未实现算子、类型不匹配等），检查结果记为 UNKNOWN。 */
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
        if (value instanceof Undefined u) {
            return "⊥(" + u.what() + ")";
        }
        if (value instanceof Unknown u) {
            return "?" + u.reason();
        }
        return String.valueOf(value);
    }
}
