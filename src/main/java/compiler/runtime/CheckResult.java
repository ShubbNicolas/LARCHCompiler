package compiler.runtime;

import java.util.Map;

/**
 * 单条等式的检查结果。
 *
 * @param index   等式序号（从 1 开始）
 * @param status  结论
 * @param witness 反例：变量名 → 取值（仅 FAIL / UNKNOWN 时非空）
 * @param message 说明
 */
public record CheckResult(int index, Status status, Map<String, String> witness, String message) {

    public enum Status {
        /** 所有枚举取值下等式两边都相等 */
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
        return switch (status) {
            case PASS -> "等式 " + index + ": 通过" + (message.isEmpty() ? "" : "（" + message + "）");
            case FAIL -> "等式 " + index + ": 失败 " + witness + " — " + message;
            case UNKNOWN -> "等式 " + index + ": 无法求值 " + witness + " — " + message;
        };
    }
}
