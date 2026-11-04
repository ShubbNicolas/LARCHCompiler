package compiler.runtime;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 各类排序的取值域。等式检查器在这些取值上枚举变量，逐条验证等式。
 *
 * <p>排序名按 LSL 常见命名识别；未识别的排序给一个 {@link Value.Unknown}，
 * 相应等式会记为无法求值，而不是误判为通过。</p>
 */
public final class Domains {

    private Domains() {
    }

    public static List<Value> of(String sort) {
        return switch (sort) {
            case "Int", "Nat", "Ind", "Val", "E" -> List.of(
                    new Value.IntVal(0),
                    new Value.IntVal(1),
                    new Value.IntVal(2));
            case "Bool" -> List.of(
                    new Value.BoolVal(false),
                    new Value.BoolVal(true));
            case "Tab" -> List.of(
                    tab(),
                    tab(0, 0),
                    tab(0, 1),
                    tab(1, 0));
            case "C", "Container", "Q", "B", "Bag" -> List.of(
                    list(),
                    list(0),
                    list(1),
                    list(0, 0),
                    list(0, 1));
            case "Str" -> List.of(
                    list(),
                    list(0),
                    list(1),
                    list(0, 0),
                    list(0, 1));
            default -> List.of(new Value.Unknown("未知排序 " + sort));
        };
    }

    private static Value.MapVal tab() {
        return new Value.MapVal(Map.of());
    }

    private static Value.MapVal tab(int key, int value) {
        Map<Integer, Integer> map = new LinkedHashMap<>();
        map.put(key, value);
        return new Value.MapVal(map);
    }

    private static Value.ListVal list(int... items) {
        List<Integer> values = new java.util.ArrayList<>();
        for (int item : items) {
            values.add(item);
        }
        return new Value.ListVal(values);
    }
}
