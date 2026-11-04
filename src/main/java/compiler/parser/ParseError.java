package compiler.parser;

/**
 * 语法错误，定位到行列，供「错误列表」展示。
 *
 * @param line    行号（从 1 开始）
 * @param column  列号（从 1 开始）
 * @param message 可读描述
 */
public record ParseError(int line, int column, String message) {

    @Override
    public String toString() {
        return String.format("第 %d 行第 %d 列: %s", line, column, message);
    }
}
