package compiler.parser;

import compiler.lexer.LexError;
import compiler.lexer.LexResult;
import compiler.lexer.Lexer;
import compiler.lexer.Token;
import compiler.lexer.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * LSL 子集（以 {@code Table1: trait} 样例为准）的递归下降语法分析器。
 *
 * <pre>
 * trait       := IDENT ':' 'trait' section+
 * section     := 'introduces' signature*
 *              | 'asserts' varDecl? equation*
 *              | 'includes' IDENT+
 * signature   := opName ':' sortList? '→' sort
 * opName      := (IDENT | NUMBER | OPERATOR | ',')+
 * varDecl     := IDENT (',' IDENT)* ':' sort (',' IDENT (',' IDENT)* ':' sort)*
 * equation    := expr (('==' ) expr)? ';'?
 * expr        := conditional
 * conditional := 'if' expr 'then' expr 'else' expr | orExpr
 * orExpr      := andExpr ('∨' andExpr)*
 * andExpr     := relExpr ('∧' relExpr)*
 * relExpr     := addExpr (('='|'≠'|'∈'|'∉'|'&lt;'|'&gt;'|'&lt;='|'&gt;=') addExpr)*
 * addExpr     := mulExpr (('+'|'-') mulExpr)*
 * mulExpr     := unary (('*'|'/') unary)*
 * unary       := '~' unary | primary
 * primary     := NUMBER | STRING | IDENT ('(' expr (',' expr)* ')')? | '(' expr ')'
 * </pre>
 */
public final class Parser {

    private static final Set<String> SECTION_KEYWORDS = Set.of("introduces", "asserts", "includes",
            "implies", "assumes", "sort", "operators");

    private final List<Token> tokens;
    private final List<ParseError> errors = new ArrayList<>();
    private int index;

    private Parser(List<Token> tokens) {
        this.tokens = tokens.stream()
                .filter(t -> t.type() != TokenType.COMMENT)
                .toList();
    }

    public static ParseResult parse(String source) {
        LexResult lex = Lexer.tokenize(source);
        Parser parser = new Parser(lex.tokens());
        for (LexError e : lex.errors()) {
            parser.errors.add(new ParseError(e.line(), e.column(), "词法错误: " + e.message()));
        }
        Ast.Trait trait = parser.parseTrait();
        return new ParseResult(trait, parser.errors);
    }

    // ------------------------------------------------------------------
    // 结构层
    // ------------------------------------------------------------------

    private Ast.Trait parseTrait() {
        try {
            if (!check(TokenType.IDENT)) {
                error(peek(), "期望 trait 名称");
                return null;
            }
            String name = advance().lexeme();

            List<String> parameters = new ArrayList<>();
            if (check(TokenType.LPAREN)) {
                advance();
                if (!check(TokenType.RPAREN)) {
                    parameters.add(expect(TokenType.IDENT, "期望 trait 形参").lexeme());
                    while (check(TokenType.COMMA)) {
                        advance();
                        parameters.add(expect(TokenType.IDENT, "期望 trait 形参").lexeme());
                    }
                }
                expect(TokenType.RPAREN, "期望 ')'");
            }

            expect(TokenType.COLON, "期望 ':'");
            expectKeyword("trait", "期望关键字 'trait'");

            List<Ast.Section> sections = new ArrayList<>();
            List<Ast.Ref> implied = new ArrayList<>();
            while (!check(TokenType.EOF)) {
                Token token = peek();
                if (keywordIs(token, "introduces")) {
                    sections.add(parseIntroduces());
                } else if (keywordIs(token, "asserts")) {
                    sections.add(parseAsserts());
                } else if (keywordIs(token, "includes")) {
                    sections.add(parseIncludes());
                } else if (keywordIs(token, "assumes")) {
                    sections.add(parseAssumes());
                } else if (keywordIs(token, "implies")) {
                    advance();
                    implied.add(parseRef());
                    while (check(TokenType.COMMA)) {
                        advance();
                        implied.add(parseRef());
                    }
                } else {
                    error(token, "期望段关键字（introduces / asserts / includes / assumes / implies）");
                    advance();
                }
            }
            return new Ast.Trait(name, parameters, sections, implied);
        } catch (ParseException e) {
            errors.add(new ParseError(e.token.line(), e.token.column(), e.getMessage()));
            return null;
        }
    }

    private Ast.Introduces parseIntroduces() {
        advance(); // introduces
        List<Ast.Signature> signatures = new ArrayList<>();
        while (!check(TokenType.EOF) && !isSectionStart(peek())) {
            try {
                signatures.add(parseSignature());
            } catch (ParseException e) {
                errors.add(new ParseError(e.token.line(), e.token.column(), e.getMessage()));
                synchronize();
            }
        }
        return new Ast.Introduces(signatures);
    }

    private Ast.Includes parseIncludes() {
        advance(); // includes
        List<Ast.Ref> refs = new ArrayList<>();
        refs.add(parseRef());
        while (check(TokenType.COMMA)) {
            advance();
            refs.add(parseRef());
        }
        return new Ast.Includes(refs);
    }

    private Ast.Assumes parseAssumes() {
        advance(); // assumes
        List<Ast.Ref> refs = new ArrayList<>();
        refs.add(parseRef());
        while (check(TokenType.COMMA)) {
            advance();
            refs.add(parseRef());
        }
        return new Ast.Assumes(refs);
    }

    /** 解析一个可能带实参的名称引用，如 {@code TotalOrder(E)}。 */
    private Ast.Ref parseRef() {
        Token token = peek();
        if (token.type() != TokenType.IDENT
                && token.type() != TokenType.KEYWORD
                && token.type() != TokenType.OPERATOR) {
            throw new ParseException(token, "期望名称");
        }
        advance();
        List<String> args = new ArrayList<>();
        if (check(TokenType.LPAREN)) {
            advance();
            if (!check(TokenType.RPAREN)) {
                args.add(expect(TokenType.IDENT, "期望实参").lexeme());
                while (check(TokenType.COMMA)) {
                    advance();
                    args.add(expect(TokenType.IDENT, "期望实参").lexeme());
                }
            }
            expect(TokenType.RPAREN, "期望 ')'");
        }
        return new Ast.Ref(token.lexeme(), args);
    }

    private Ast.Asserts parseAsserts() {
        advance(); // asserts

        // 生成子句：Str generated by empty, -|
        List<Ast.Ref> generatedBy = new ArrayList<>();
        while (check(TokenType.IDENT) && keywordIs(peek(1), "generated")) {
            advance(); // 排序名
            advance(); // generated
            expectKeyword("by", "期望 'by'");
            generatedBy.add(parseRef());
            while (check(TokenType.COMMA)) {
                advance();
                generatedBy.add(parseRef());
            }
            if (check(TokenType.SEMICOLON)) {
                advance();
            }
        }

        List<Ast.VarDecl> variables = new ArrayList<>();
        if (looksLikeVarDecl()) {
            variables.addAll(parseVarDecl());
        }
        List<Ast.Equation> equations = new ArrayList<>();
        while (!check(TokenType.EOF) && !isSectionStart(peek())) {
            try {
                equations.add(parseEquation());
            } catch (ParseException e) {
                errors.add(new ParseError(e.token.line(), e.token.column(), e.getMessage()));
                skipToEquationEnd();
            }
        }
        return new Ast.Asserts(generatedBy, variables, equations);
    }

    private List<Ast.VarDecl> parseVarDecl() {
        List<Ast.VarDecl> decls = new ArrayList<>();
        while (looksLikeVarDecl()) {
            List<String> names = new ArrayList<>();
            names.add(advance().lexeme());
            while (check(TokenType.COMMA) && check(1, TokenType.IDENT)) {
                advance(); // ,
                names.add(advance().lexeme());
            }
            expect(TokenType.COLON, "期望 ':'");
            String sort = expect(TokenType.IDENT, "期望排序名").lexeme();
            decls.add(new Ast.VarDecl(names, sort));
            if (check(TokenType.COMMA)) {
                advance();
            } else {
                break;
            }
        }
        return decls;
    }

    private Ast.Signature parseSignature() {
        List<Token> nameTokens = new ArrayList<>();
        while (!check(TokenType.COLON)) {
            if (check(TokenType.EOF) || isSectionStart(peek())) {
                throw new ParseException(peek(), "算子签名缺少 ':'");
            }
            nameTokens.add(advance());
        }
        expect(TokenType.COLON, "期望 ':'");

        List<String> args = new ArrayList<>();
        if (!checkOperator("→")) {
            args.add(expect(TokenType.IDENT, "期望参数排序").lexeme());
            while (check(TokenType.COMMA)) {
                advance();
                args.add(expect(TokenType.IDENT, "期望参数排序").lexeme());
            }
        }
        expectOperator("→", "期望 '→'");
        String result = expect(TokenType.IDENT, "期望结果排序").lexeme();
        return new Ast.Signature(joinName(nameTokens), args, result);
    }

    private Ast.Equation parseEquation() {
        Ast.Expr left = parseExpr();
        Ast.Expr right;
        if (checkOperator("==")) {
            advance();
            right = parseExpr();
        } else {
            right = new Ast.Name("true");
        }
        if (check(TokenType.SEMICOLON)) {
            advance();
        }
        return new Ast.Equation(left, right);
    }

    // ------------------------------------------------------------------
    // 表达式
    // ------------------------------------------------------------------

    private Ast.Expr parseExpr() {
        return parseConditional();
    }

    private Ast.Expr parseConditional() {
        if (keywordIs(peek(), "if")) {
            advance();
            Ast.Expr cond = parseExpr();
            expectKeyword("then", "期望 'then'");
            Ast.Expr whenTrue = parseExpr();
            expectKeyword("else", "期望 'else'");
            Ast.Expr whenFalse = parseExpr();
            return new Ast.Conditional(cond, whenTrue, whenFalse);
        }
        return parseOr();
    }

    private Ast.Expr parseOr() {
        Ast.Expr left = parseAnd();
        while (checkOperator("∨")) {
            advance();
            left = new Ast.Infix("∨", left, parseAnd());
        }
        return left;
    }

    private Ast.Expr parseAnd() {
        Ast.Expr left = parseRelation();
        while (checkOperator("∧")) {
            advance();
            left = new Ast.Infix("∧", left, parseRelation());
        }
        return left;
    }

    private Ast.Expr parseRelation() {
        Ast.Expr left = parseCustomInfix();
        while (isRelationOperator(peek())) {
            String op = advance().lexeme();
            left = new Ast.Infix(op, left, parseCustomInfix());
        }
        return left;
    }

    /**
     * 用户自定义的中缀算子（如 {@code -|}），优先级高于关系运算符、低于算术。
     * 未在内置集合里的运算符都按左结合处理。
     */
    private Ast.Expr parseCustomInfix() {
        Ast.Expr left = parseAdditive();
        while (check(TokenType.OPERATOR) && isCustomOperator(peek().lexeme())) {
            String op = advance().lexeme();
            left = new Ast.Infix(op, left, parseAdditive());
        }
        return left;
    }

    private static boolean isCustomOperator(String op) {
        return switch (op) {
            case "=", "≠", "∈", "∉", "<", ">", "<=", ">=",
                 "+", "-", "*", "/", "∧", "∨", "~", "→", "==", "=>", "->" -> false;
            default -> true;
        };
    }

    private Ast.Expr parseAdditive() {
        Ast.Expr left = parseMultiplicative();
        while (checkOperator("+") || checkOperator("-")) {
            String op = advance().lexeme();
            left = new Ast.Infix(op, left, parseMultiplicative());
        }
        return left;
    }

    private Ast.Expr parseMultiplicative() {
        Ast.Expr left = parseUnary();
        while (checkOperator("*") || checkOperator("/")) {
            String op = advance().lexeme();
            left = new Ast.Infix(op, left, parseUnary());
        }
        return left;
    }

    private Ast.Expr parseUnary() {
        if (checkOperator("~")) {
            advance();
            return new Ast.Negate(parseUnary());
        }
        return parsePrimary();
    }

    private Ast.Expr parsePrimary() {
        Token token = peek();
        if (check(TokenType.NUMBER)) {
            advance();
            return new Ast.Num(Integer.parseInt(token.lexeme()));
        }
        if (check(TokenType.STRING)) {
            advance();
            return new Ast.Text(token.lexeme());
        }
        if (check(TokenType.LPAREN)) {
            advance();
            Ast.Expr inner = parseExpr();
            expect(TokenType.RPAREN, "期望 ')'");
            return inner;
        }
        if (check(TokenType.IDENT) || check(TokenType.KEYWORD)) {
            advance();
            String name = token.lexeme();
            if (check(TokenType.LPAREN)) {
                advance();
                List<Ast.Expr> args = new ArrayList<>();
                if (!check(TokenType.RPAREN)) {
                    args.add(parseExpr());
                    while (check(TokenType.COMMA)) {
                        advance();
                        args.add(parseExpr());
                    }
                }
                expect(TokenType.RPAREN, "期望 ')'");
                return new Ast.Apply(name, args);
            }
            return new Ast.Name(name);
        }
        throw new ParseException(token, "无法解析的表达式起始符号 '" + token.lexeme() + "'");
    }

    // ------------------------------------------------------------------
    // 工具
    // ------------------------------------------------------------------

    private boolean isRelationOperator(Token token) {
        if (token.type() != TokenType.OPERATOR) {
            return false;
        }
        return switch (token.lexeme()) {
            case "=", "≠", "∈", "∉", "<", ">", "<=", ">=" -> true;
            default -> false;
        };
    }

    private boolean isSectionStart(Token token) {
        return token.type() == TokenType.KEYWORD && SECTION_KEYWORDS.contains(token.lexeme().toLowerCase());
    }

    private boolean keywordIs(Token token, String keyword) {
        return token.type() == TokenType.KEYWORD && token.lexeme().equalsIgnoreCase(keyword);
    }

    private boolean looksLikeVarDecl() {
        if (!check(TokenType.IDENT)) {
            return false;
        }
        return check(1, TokenType.COMMA) || check(1, TokenType.COLON);
    }

    private static String joinName(List<Token> nameTokens) {
        StringBuilder sb = new StringBuilder();
        for (Token t : nameTokens) {
            if (t.type() == TokenType.OPERATOR) {
                sb.append(' ').append(t.lexeme()).append(' ');
            } else {
                sb.append(t.lexeme());
            }
        }
        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    private void synchronize() {
        while (!check(TokenType.EOF)) {
            Token token = peek();
            if (token.type() == TokenType.SEMICOLON) {
                advance();
                return;
            }
            if (isSectionStart(token)) {
                return;
            }
            advance();
        }
    }

    private void skipToEquationEnd() {
        while (!check(TokenType.EOF) && !check(TokenType.SEMICOLON) && !isSectionStart(peek())) {
            advance();
        }
        if (check(TokenType.SEMICOLON)) {
            advance();
        }
    }

    private Token peek() {
        return peek(0);
    }

    private Token peek(int offset) {
        int i = Math.min(index + offset, tokens.size() - 1);
        return tokens.get(i);
    }

    private boolean check(TokenType type) {
        return peek().type() == type;
    }

    private boolean check(int offset, TokenType type) {
        return peek(offset).type() == type;
    }

    private boolean checkOperator(String op) {
        Token token = peek();
        return token.type() == TokenType.OPERATOR && token.lexeme().equals(op);
    }

    private Token advance() {
        Token token = peek();
        if (index < tokens.size() - 1) {
            index++;
        }
        return token;
    }

    private Token expect(TokenType type, String message) {
        if (!check(type)) {
            throw new ParseException(peek(), message);
        }
        return advance();
    }

    private void expectKeyword(String keyword, String message) {
        if (!keywordIs(peek(), keyword)) {
            throw new ParseException(peek(), message);
        }
        advance();
    }

    private void expectOperator(String op, String message) {
        if (!checkOperator(op)) {
            throw new ParseException(peek(), message);
        }
        advance();
    }

    private void error(Token token, String message) {
        errors.add(new ParseError(token.line(), token.column(), message));
    }

    /** 内部异常：携带出错位置。 */
    private static final class ParseException extends RuntimeException {
        private final transient Token token;

        ParseException(Token token, String message) {
            super(message);
            this.token = token;
        }
    }
}
