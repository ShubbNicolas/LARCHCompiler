package compiler.lexer;

/**
 * 词法错误：定位到具体行列，供「错误列表」页签展示。
 *
 * @param line    行号（从 1 开始）
 * @param column  列号（从 1 开始）
 * @param message 可读的错误描述
 */
public record LexError(int line, int column, String message) {

    @Override
    public String toString() {
        return String.format("第 %d 行第 %d 列: %s", line, column, message);
    }
}
