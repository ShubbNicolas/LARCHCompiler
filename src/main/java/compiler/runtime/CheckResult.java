package compiler.runtime;

import java.util.Map;

/**
 * 单条等式（或推论）的检查结果。
 *
 * @param index   序号（从 1 开始）
 * @param status  结论
 * @param witness 反例：变量名 → 取值（仅 FAIL / UNKNOWN 时非空）
 * @param message 说明
 * @param skipped 因术语未定义而跳过的取值组合数（LSL 中未定义情形不要求等式成立）
 */
public record CheckResult(int index, Status status, Map<String, String> witness,
                          String message, int skipped) {

    public enum Status {
        /** 所有有定义的取值下两边都相等 */
        PASS,
        /** 存在反例 */
        FAIL,
        /** 无法求值（未实现算子、未知排序等） */
        UNKNOWN
    }

    public CheckResult {
        witness = Map.copyOf(witness);
    }

    public String summarize() {
        String skip = skipped > 0 ? "（跳过 " + skipped + " 个未定义取值）" : "";
        return switch (status) {
            case PASS -> "等式 " + index + ": 通过" + skip;
            case FAIL -> "等式 " + index + ": 失败 " + witness + " — " + message + skip;
            case UNKNOWN -> "等式 " + index + ": 无法求值 " + witness + " — " + message;
        };
    }
}
