package compiler.parser;

import java.util.List;

/**
 * 语法分析结果。一个文件里可以包含多个 trait。
 *
 * @param traits 解析出的 trait 列表
 * @param errors 语法错误（可能为空）
 */
public record ParseResult(List<Ast.Trait> traits, List<ParseError> errors) {

    public ParseResult {
        traits = List.copyOf(traits);
        errors = List.copyOf(errors);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean isSuccess() {
        return !traits.isEmpty();
    }

    /** 第一个 trait；没有时返回 {@code null}。 */
    public Ast.Trait trait() {
        return traits.isEmpty() ? null : traits.get(0);
    }
}
