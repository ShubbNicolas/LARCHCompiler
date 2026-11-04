package compiler.parser;

import java.util.List;

/**
 * 语法分析结果。
 *
 * @param trait 解析出的 trait；整体失败时为 {@code null}
 * @param errors 语法错误（可能为空）
 */
public record ParseResult(Ast.Trait trait, List<ParseError> errors) {

    public ParseResult {
        errors = List.copyOf(errors);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean isSuccess() {
        return trait != null;
    }
}
