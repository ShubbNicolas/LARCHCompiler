package compiler.lexer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LexerTest {

    private static List<Token> tokens(String src) {
        return Lexer.tokenize(src).tokens();
    }

    private static List<Token> withoutEof(String src) {
        List<Token> all = tokens(src);
        return all.subList(0, all.size() - 1);
    }

    @Test
    @DisplayName("空输入只产生 EOF")
    void emptyInput() {
        LexResult result = Lexer.tokenize("");
        assertEquals(1, result.tokens().size());
        assertEquals(TokenType.EOF, result.tokens().get(0).type());
        assertFalse(result.hasErrors());
    }

    @Test
    @DisplayName("关键字与标识符区分")
    void keywordsAndIdentifiers() {
        var ts = withoutEof("trait IntSet new add");
        assertEquals(TokenType.KEYWORD, ts.get(0).type());
        assertEquals("trait", ts.get(0).lexeme());
        assertEquals(TokenType.IDENT, ts.get(1).type());
        assertEquals("IntSet", ts.get(1).lexeme());
        assertEquals(TokenType.KEYWORD, ts.get(2).type());
        assertEquals(TokenType.KEYWORD, ts.get(3).type());
    }

    @Test
    @DisplayName("关键字大小写不敏感，但保留原始文本")
    void keywordCaseInsensitive() {
        var ts = withoutEof("TRAIT Trait");
        assertEquals(TokenType.KEYWORD, ts.get(0).type());
        assertEquals("TRAIT", ts.get(0).lexeme());
        assertEquals(TokenType.KEYWORD, ts.get(1).type());
        assertEquals("Trait", ts.get(1).lexeme());
    }

    @Test
    @DisplayName("/belong 是独立关键字")
    void belongSlashKeyword() {
        var ts = withoutEof("/belong a b c");
        assertEquals(TokenType.KEYWORD, ts.get(0).type());
        assertEquals("/belong", ts.get(0).lexeme());
        assertEquals(4, ts.size());
    }

    @Test
    @DisplayName("普通除法仍是运算符")
    void divisionOperator() {
        var ts = withoutEof("a / b");
        assertEquals(TokenType.OPERATOR, ts.get(1).type());
        assertEquals("/", ts.get(1).lexeme());
    }

    @Test
    @DisplayName("运算符与标点")
    void operatorsAndPunctuation() {
        var ts = withoutEof("add(a,b)==~c <= d");
        assertEquals("add", ts.get(0).lexeme());
        assertEquals(TokenType.LPAREN, ts.get(1).type());
        assertEquals(TokenType.IDENT, ts.get(2).type());
        assertEquals(TokenType.COMMA, ts.get(3).type());
        assertTrue(ts.stream().anyMatch(t -> t.isOperator("==")));
        assertTrue(ts.stream().anyMatch(t -> t.isOperator("~")));
        assertTrue(ts.stream().anyMatch(t -> t.isOperator("<=")));
    }

    @Test
    @DisplayName("集合符号 ∈ 与逻辑符号")
    void unicodeOperators() {
        var ts = withoutEof("i ∈ s ∧ j ∨ k");
        assertTrue(ts.stream().anyMatch(t -> t.isOperator("∈")));
        assertTrue(ts.stream().anyMatch(t -> t.isOperator("∧")));
        assertTrue(ts.stream().anyMatch(t -> t.isOperator("∨")));
    }

    @Test
    @DisplayName("字符串转义")
    void stringEscapes() {
        var ts = withoutEof("\"a\\nb\\t\\\"c\\\\\"");
        assertEquals(TokenType.STRING, ts.get(0).type());
        assertEquals("a\nb\t\"c\\", ts.get(0).lexeme());
    }

    @Test
    @DisplayName("注释到行尾")
    void comments() {
        LexResult result = Lexer.tokenize("-- larch 注释\nx // 另一种\n");
        var ts = result.tokens();
        assertEquals(TokenType.COMMENT, ts.get(0).type());
        assertEquals("-- larch 注释", ts.get(0).lexeme());
        assertEquals(TokenType.IDENT, ts.get(1).type());
        assertEquals(TokenType.COMMENT, ts.get(2).type());
        assertFalse(result.hasErrors());
    }

    @Test
    @DisplayName("行列号定位准确")
    void lineAndColumnTracking() {
        var ts = withoutEof("add(\n  x\n)");
        assertEquals(1, ts.get(0).line());
        Token x = ts.get(2);
        assertEquals("x", x.lexeme());
        assertEquals(2, x.line());
        assertEquals(3, x.column());
    }

    @Test
    @DisplayName("字符串未闭合报告错误")
    void unterminatedString() {
        LexResult result = Lexer.tokenize("x \"abc");
        assertTrue(result.hasErrors());
        assertTrue(result.errors().get(0).message().contains("未闭合"));
    }

    @Test
    @DisplayName("非法字符报告错误并继续扫描")
    void unknownCharacter() {
        LexResult result = Lexer.tokenize("x # y");
        assertTrue(result.hasErrors());
        assertEquals(1, result.errors().size());
        // 错误之后仍能扫描出 y
        assertTrue(result.tokens().stream().anyMatch(t -> "y".equals(t.lexeme())));
    }

    @Test
    @DisplayName("中文标识符")
    void chineseIdentifier() {
        var ts = withoutEof("变量 trait");
        assertEquals(TokenType.IDENT, ts.get(0).type());
        assertEquals("变量", ts.get(0).lexeme());
    }
}
