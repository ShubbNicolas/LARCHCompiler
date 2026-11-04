package compiler.lexer;

import java.util.List;

/**
 * 词法分析结果。
 *
 * @param tokens 词法单元序列（最后一个是 {@link TokenType#EOF}）
 * @param errors 词法错误（可能为空）
 */
public record LexResult(List<Token> tokens, List<LexError> errors) {

    public LexResult {
        tokens = List.copyOf(tokens);
        errors = List.copyOf(errors);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
