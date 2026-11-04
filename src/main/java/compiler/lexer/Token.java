package compiler.lexer;

/**
 * 一个词法单元。
 *
 * @param type   语法类别
 * @param lexeme 原始文本
 * @param line   起始行号（从 1 开始）
 * @param column 起始列号（从 1 开始）
 */
public record Token(TokenType type, String lexeme, int line, int column) {

    public boolean is(TokenType other) {
        return type == other;
    }

    public boolean isKeyword(String word) {
        return type == TokenType.KEYWORD && lexeme.equalsIgnoreCase(word);
    }

    public boolean isOperator(String op) {
        return type == TokenType.OPERATOR && lexeme.equals(op);
    }

    /** 便于调试与「词法分析」结果展示。 */
    @Override
    public String toString() {
        return String.format("%-9s %-12s %d:%d", type, lexeme, line, column);
    }
}
