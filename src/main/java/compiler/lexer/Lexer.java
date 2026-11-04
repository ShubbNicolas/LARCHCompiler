package compiler.lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * LARCH 子集的词法分析器（手写扫描器）。
 *
 * <p>支持的词法单元：</p>
 * <ul>
 *   <li>注释：{@code -- ...} 或 {@code // ...}，到行尾</li>
 *   <li>关键字：trait / sort / operators / includes / asserts / assumes / implies /
 *       new / add / lookup / belong / Ind / Val（{@code /belong} 也算关键字）</li>
 *   <li>标识符：字母、下划线、汉字开头，后接字母/数字/下划线/汉字</li>
 *   <li>数字：十进制整数</li>
 *   <li>字符串：双引号，支持 {@code \"} {@code \\} {@code \n} {@code \t} 转义</li>
 *   <li>运算符：{@code == => -> <= >= != = ~ ∈ ∧ ∨ < > + - * /}</li>
 *   <li>标点：{@code ( ) , : ;}</li>
 * </ul>
 *
 * <p>实现为单遍扫描，不回溯；无法识别的字符会记入错误列表并跳过，
 * 以保证一次分析能报告多处问题。</p>
 */
public final class Lexer {

    private static final Set<String> KEYWORDS = Set.of(
            "trait", "sort", "introduces", "operators", "includes", "asserts",
            "assumes", "implies", "generated", "by", "partitioned",
            "converts", "exempting", "for",
            "using", "applied", "when", "if", "then", "else",
            "new", "add", "lookup", "belong");

    private final String src;
    private int pos;
    private int line = 1;
    private int column = 1;

    public Lexer(String source) {
        this.src = source == null ? "" : source;
    }

    public static LexResult tokenize(String source) {
        return new Lexer(source).tokenize();
    }

    public LexResult tokenize() {
        List<Token> tokens = new ArrayList<>();
        List<LexError> errors = new ArrayList<>();

        while (!atEnd()) {
            char c = peek();

            if (isWhitespace(c)) {
                advance();
                continue;
            }

            int startLine = line;
            int startColumn = column;

            // 注释
            if ((c == '-' && peek(1) == '-') || (c == '/' && peek(1) == '/')) {
                tokens.add(readComment(startLine, startColumn));
                continue;
            }

            // /belong 是一个整体关键字
            if (c == '/' && src.startsWith("/belong", pos)
                    && !isIdentPart(peek(7))) {
                advanceBy(7);
                tokens.add(new Token(TokenType.KEYWORD, "/belong", startLine, startColumn));
                continue;
            }

            if (Character.isDigit(c)) {
                tokens.add(readNumber(startLine, startColumn));
                continue;
            }

            if (isIdentStart(c)) {
                tokens.add(readIdentifier(startLine, startColumn));
                continue;
            }

            if (c == '"') {
                Token token = readString(startLine, startColumn, errors);
                if (token != null) {
                    tokens.add(token);
                }
                continue;
            }

            if (c == '(') { advance(); tokens.add(new Token(TokenType.LPAREN, "(", startLine, startColumn)); continue; }
            if (c == ')') { advance(); tokens.add(new Token(TokenType.RPAREN, ")", startLine, startColumn)); continue; }
            if (c == '{') { advance(); tokens.add(new Token(TokenType.LBRACE, "{", startLine, startColumn)); continue; }
            if (c == '}') { advance(); tokens.add(new Token(TokenType.RBRACE, "}", startLine, startColumn)); continue; }
            if (c == ',') { advance(); tokens.add(new Token(TokenType.COMMA, ",", startLine, startColumn)); continue; }
            if (c == ':') { advance(); tokens.add(new Token(TokenType.COLON, ":", startLine, startColumn)); continue; }
            if (c == ';') { advance(); tokens.add(new Token(TokenType.SEMICOLON, ";", startLine, startColumn)); continue; }

            String op = matchOperator();
            if (op != null) {
                tokens.add(new Token(TokenType.OPERATOR, op, startLine, startColumn));
                continue;
            }

            errors.add(new LexError(startLine, startColumn,
                    "无法识别的字符 '" + c + "'"));
            advance();
        }

        tokens.add(new Token(TokenType.EOF, "<EOF>", line, column));
        return new LexResult(tokens, errors);
    }

    // ------------------------------------------------------------------
    // 各类词法单元
    // ------------------------------------------------------------------

    private Token readComment(int startLine, int startColumn) {
        int start = pos;
        while (!atEnd() && peek() != '\n') {
            advance();
        }
        return new Token(TokenType.COMMENT, src.substring(start, pos).stripTrailing(),
                startLine, startColumn);
    }

    private Token readNumber(int startLine, int startColumn) {
        int start = pos;
        while (!atEnd() && Character.isDigit(peek())) {
            advance();
        }
        return new Token(TokenType.NUMBER, src.substring(start, pos), startLine, startColumn);
    }

    private Token readIdentifier(int startLine, int startColumn) {
        int start = pos;
        while (!atEnd() && isIdentPart(peek())) {
            advance();
        }
        String word = src.substring(start, pos);
        TokenType type = KEYWORDS.contains(word.toLowerCase()) ? TokenType.KEYWORD : TokenType.IDENT;
        return new Token(type, word, startLine, startColumn);
    }

    /**
     * 读取字符串字面量。未闭合时记录错误并返回 {@code null}（该 token 被丢弃）。
     */
    private Token readString(int startLine, int startColumn, List<LexError> errors) {
        advance(); // 跳过起始引号
        StringBuilder sb = new StringBuilder();
        while (!atEnd()) {
            char c = peek();
            if (c == '"') {
                advance();
                return new Token(TokenType.STRING, sb.toString(), startLine, startColumn);
            }
            if (c == '\n') {
                break;
            }
            if (c == '\\') {
                advance();
                if (atEnd()) {
                    break;
                }
                char esc = peek();
                advance();
                sb.append(switch (esc) {
                    case 'n' -> '\n';
                    case 't' -> '\t';
                    case '"' -> '"';
                    case '\\' -> '\\';
                    default -> esc;
                });
            } else {
                sb.append(c);
                advance();
            }
        }
        errors.add(new LexError(startLine, startColumn, "字符串字面量未闭合"));
        return null;
    }

    /**
     * 匹配运算符：优先匹配双字符运算符，再退化为单字符。
     */
    private String matchOperator() {
        String[] twoChar = {"==", "=>", "->", "-|", "<=", ">=", "!="};
        for (String op : twoChar) {
            if (src.startsWith(op, pos)) {
                advanceBy(2);
                return op;
            }
        }
        char c = peek();
        String single = switch (c) {
            case '=', '~', '∈', '∉', '≠', '→', '∧', '∨', '≤', '≥', '⇒', '⇔',
                 '⊕', '⊗', '<', '>', '+', '-', '*', '/', '|', '&' -> String.valueOf(c);
            default -> null;
        };
        if (single != null) {
            advance();
        }
        return single;
    }

    // ------------------------------------------------------------------
    // 基础工具
    // ------------------------------------------------------------------

    private boolean atEnd() {
        return pos >= src.length();
    }

    private char peek() {
        return peek(0);
    }

    private char peek(int offset) {
        int i = pos + offset;
        return i < src.length() ? src.charAt(i) : '\0';
    }

    private void advance() {
        char c = src.charAt(pos);
        pos++;
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
    }

    private void advanceBy(int count) {
        for (int i = 0; i < count; i++) {
            advance();
        }
    }

    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == '\f';
    }

    private static boolean isIdentStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private static boolean isIdentPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
