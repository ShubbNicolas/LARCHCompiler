package compiler.lexer;

/**
 * 词法单元类型。
 *
 * <p>只区分语法类别，具体关键字/运算符用 {@link Token#lexeme()} 判断，
 * 这样后续文法调整不需要改动枚举。</p>
 */
public enum TokenType {

    /** 关键字：trait / new / add / lookup / belong / asserts / Ind / Val 等 */
    KEYWORD,

    /** 标识符 */
    IDENT,

    /** 十进制整数 */
    NUMBER,

    /** 双引号字符串 */
    STRING,

    /** 运算符：== = ~ ∈ ∧ ∨ -&gt; =&gt; &lt; &lt;= &gt; &gt;= != + - * / */
    OPERATOR,

    LPAREN,
    RPAREN,
    COMMA,
    COLON,
    SEMICOLON,

    /** 注释（保留在流中，便于分析结果显示与高亮） */
    COMMENT,

    /** 输入结束标记，保证至少有一个 token */
    EOF
}
