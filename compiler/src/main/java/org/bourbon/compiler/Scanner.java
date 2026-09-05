package org.bourbon.compiler;

import static org.bourbon.compiler.TokenType.AMPERSAND;
import static org.bourbon.compiler.TokenType.AMPERSAND_AMPERSAND;
import static org.bourbon.compiler.TokenType.AMPERSAND_AMPERSAND_EQUAL;
import static org.bourbon.compiler.TokenType.AMPERSAND_EQUAL;
import static org.bourbon.compiler.TokenType.BACKTICK;
import static org.bourbon.compiler.TokenType.BANG;
import static org.bourbon.compiler.TokenType.BANG_EQUAL;
import static org.bourbon.compiler.TokenType.BANG_EQUAL_EQUAL;
import static org.bourbon.compiler.TokenType.CARET;
import static org.bourbon.compiler.TokenType.COLON;
import static org.bourbon.compiler.TokenType.COMMA;
import static org.bourbon.compiler.TokenType.DOT;
import static org.bourbon.compiler.TokenType.DOT_DOT;
import static org.bourbon.compiler.TokenType.EOF;
import static org.bourbon.compiler.TokenType.EQUAL;
import static org.bourbon.compiler.TokenType.EQUAL_EQUAL;
import static org.bourbon.compiler.TokenType.ERROR;
import static org.bourbon.compiler.TokenType.FAT_ARROW;
import static org.bourbon.compiler.TokenType.GREATER;
import static org.bourbon.compiler.TokenType.GREATER_EQUAL;
import static org.bourbon.compiler.TokenType.LEFT_BRACE;
import static org.bourbon.compiler.TokenType.LEFT_BRACKET;
import static org.bourbon.compiler.TokenType.LEFT_PAREN;
import static org.bourbon.compiler.TokenType.LESS;
import static org.bourbon.compiler.TokenType.LESS_EQUAL;
import static org.bourbon.compiler.TokenType.LESS_EQUAL_GREATER;
import static org.bourbon.compiler.TokenType.MINUS;
import static org.bourbon.compiler.TokenType.MINUS_EQUAL;
import static org.bourbon.compiler.TokenType.MINUS_MINUS;
import static org.bourbon.compiler.TokenType.NUMBER;
import static org.bourbon.compiler.TokenType.PERCENT;
import static org.bourbon.compiler.TokenType.PERCENT_EQUAL;
import static org.bourbon.compiler.TokenType.PIPE;
import static org.bourbon.compiler.TokenType.PIPE_EQUAL;
import static org.bourbon.compiler.TokenType.PIPE_PIPE;
import static org.bourbon.compiler.TokenType.PIPE_PIPE_EQUAL;
import static org.bourbon.compiler.TokenType.PLUS;
import static org.bourbon.compiler.TokenType.PLUS_EQUAL;
import static org.bourbon.compiler.TokenType.PLUS_PLUS;
import static org.bourbon.compiler.TokenType.QUESTION;
import static org.bourbon.compiler.TokenType.QUESTION_DOT;
import static org.bourbon.compiler.TokenType.RIGHT_BRACE;
import static org.bourbon.compiler.TokenType.RIGHT_BRACKET;
import static org.bourbon.compiler.TokenType.RIGHT_PAREN;
import static org.bourbon.compiler.TokenType.SEMICOLON;
import static org.bourbon.compiler.TokenType.SLASH;
import static org.bourbon.compiler.TokenType.SLASH_EQUAL;
import static org.bourbon.compiler.TokenType.STAR;
import static org.bourbon.compiler.TokenType.STAR_DOT;
import static org.bourbon.compiler.TokenType.STAR_EQUAL;
import static org.bourbon.compiler.TokenType.STAR_STAR;
import static org.bourbon.compiler.TokenType.THIN_ARROW;
import static org.bourbon.compiler.TokenType.TILDE;
import static org.bourbon.compiler.TokenType.TILDE_EQUAL;
import static org.bourbon.compiler.TokenType.TRIPLE_DOT;
import static org.bourbon.compiler.TokenType.TRIPLE_EQUAL;

import java.util.ArrayList;
import java.util.List;

import org.bourbon.compiler.Source.CharPredicate;
import org.bourbon.compiler.diagnostic.Diagnostic.InternalError;
import org.bourbon.compiler.diagnostic.Diagnostic.ScannerDiagnostic;
import org.bourbon.compiler.literal.NumberLiteral;
import org.bourbon.compiler.literal.NumberLiteralFormatError;
import org.bourbon.compiler.literal.NumberLiteralFormatError.Detail.InvalidDigit;
import org.bourbon.compiler.literal.NumberLiteralFormatError.Detail.InvalidExponentDigits;
import org.bourbon.compiler.literal.NumberLiteralFormatError.Detail.MisplacedUnderscore;
import org.bourbon.compiler.literal.NumberLiteralFormatError.Detail.MissingDecimalDigits;
import org.bourbon.compiler.literal.NumberLiteralFormatError.Detail.MissingDigitsAfterDecimalPoint;
import org.bourbon.compiler.literal.NumberLiteralFormatError.Detail.MissingDigitsAfterPrefix;
import org.bourbon.compiler.literal.NumberLiteralFormatError.Detail.MissingExponentDigits;
import org.bourbon.compiler.literal.NumberLiteralFormatError.Detail.MissingExponentSign;
import org.bourbon.compiler.literal.NumberLiteralFormatError.Detail.MultipleDecimalPoints;
import org.bourbon.compiler.literal.NumberLiteralFormatError.Detail.MultipleExponents;

/**
 * The Scanner class is responsible for tokenizing the source code.
 * It reads the input stream and breaks it down into tokens.
 */
public class Scanner {

    private final Source source;
    private final List<Token> tokens = new ArrayList<>();

    public static List<Token> scanTokens(Source source) {
        var scanner = new Scanner(source);
        return scanner.scanTokens();
    }

    public Scanner(Source source) {
        this.source = source;
    }

    public List<Token> scanTokens() {
        if (!tokens.isEmpty() && !isAtEnd()) {
            // Calling scanner scanTokens() repeatedly on the same Source input
            // (Like in REPL session multi-line input evaluation), the
            // Source content may have grown compared to the previous call.
            // If so, remove the trailing EOF from the token list and resume
            // scanning as if the input was always the longer version.

            // Source cannot be modified other than by appending content,
            // so this is safe by construction.
            var lastToken = tokens.getLast();
            if (lastToken.type() == EOF) {
                tokens.removeLast();
            }
        }

        while (!source.isAtEnd()) {
            source.tokenStart();
            scanToken();
        }

        source.tokenStart();
        addToken(EOF);
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            //<editor-fold desc="2.2 Comments">

            case '/' -> {
                if (match('/')) {
                    while (peek() != '\n' && !isAtEnd())
                        advance();
                    // ignore comment tokens for now
                } else if (match('*')) {
                    // Multi line comments
                    multiLineComment();
                } else if (match('=')) {
                    addToken(SLASH_EQUAL);
                } else {
                    addToken(SLASH);
                }
            }

            case '#' -> hexadecimalNumber();
            case '$' -> binaryNumber();

            //</editor-fold>

            // <editor-fold desc="2.5 Operators and delimiters (from Ceylon spec)">

            // <editor-fold desc="Single char tokens: , : ; { } ( ) [ ] ` ^ ">
            case ',' -> addToken(COMMA);
            case ':' -> addToken(COLON);
            case ';' -> addToken(SEMICOLON);
            case '{' -> addToken(LEFT_BRACE);
            case '}' -> addToken(RIGHT_BRACE);
            case '(' -> addToken(LEFT_PAREN);
            case ')' -> addToken(RIGHT_PAREN);
            case '[' -> addToken(LEFT_BRACKET);
            case ']' -> addToken(RIGHT_BRACKET);
            case '`' -> addToken(BACKTICK);
            case '^' -> addToken(CARET);
            // </editor-fold>

            // <editor-fold desc="Multi-char operators">

            // <editor-fold desc="Operators: ? ?. % %= ~ ~= ">
            case '?' -> addDoubleToken(QUESTION, '.', QUESTION_DOT);
            case '%' -> addDoubleToken(PERCENT, '=', PERCENT_EQUAL);
            case '~' -> addDoubleToken(TILDE, '=', TILDE_EQUAL);
            // </editor-fold>

            // <editor-fold desc="Operators: < > <= >= <=>">
            case '>' -> addDoubleToken(GREATER, '=', GREATER_EQUAL);
            case '<' -> {
                if (match('=')) {
                    addToken(match('>') ? LESS_EQUAL_GREATER : LESS_EQUAL);
                } else {
                    addToken(LESS);
                }
            }
            // </editor-fold>

            // <editor-fold desc="Operators: . .. ... ">
            case '.' -> {
                if (match('.')) {
                    if (match('.')) {
                        addToken(TRIPLE_DOT);
                    } else {
                        addToken(DOT_DOT);
                    }
                } else {
                    addToken(DOT);
                }
            }
            // </editor-fold>

            // <editor-fold desc="Operators: + ++ += ">
            case '+' -> {
                if (match('+')) {
                    addToken(PLUS_PLUS);
                } else if (match('=')) {
                    addToken(PLUS_EQUAL);
                } else {
                    addToken(PLUS);
                }
            }
            // </editor-fold>

            // <editor-fold desc="Operators: - -- -= ">
            case '-' -> {
                if (match('-')) {
                    addToken(MINUS_MINUS);
                } else if (match('=')) {
                    addToken(MINUS_EQUAL);
                } else if (match('>')) {
                    addToken(THIN_ARROW);
                } else {
                    addToken(MINUS);
                }
            }
            // </editor-fold>

            // <editor-fold desc="Operators: & && &&= &= ">
            case '&' -> {
                if (match('&')) {
                    if (match('=')) {
                        addToken(AMPERSAND_AMPERSAND_EQUAL);
                    } else {
                        addToken(AMPERSAND_AMPERSAND);
                    }
                } else if (match('=')) {
                    addToken(AMPERSAND_EQUAL);
                } else {
                    addToken(AMPERSAND);
                }
            }
            // </editor-fold>

            // <editor-fold desc="Operators: | || ||= |= ">
            case '|' -> {
                if (match('|')) {
                    if (match('=')) {
                        addToken(PIPE_PIPE_EQUAL);
                    } else {
                        addToken(PIPE_PIPE);
                    }
                } else if (match('=')) {
                    addToken(PIPE_EQUAL);
                } else {
                    addToken(PIPE);
                }
            }
            // </editor-fold>

            // <editor-fold desc="Operators: = == === => ">
            case '=' -> {
                if (match('=')) {
                    if (match('=')) {
                        addToken(TRIPLE_EQUAL);
                    } else {
                        addToken(EQUAL_EQUAL);
                    }
                } else if (match('>')) {
                    addToken(FAT_ARROW);
                } else {
                    addToken(EQUAL);
                }
            }
            // </editor-fold>

            // <editor-fold desc="Operators: * ** *. *= ">
            case '*' -> {
                if (match('*')) {
                    addToken(STAR_STAR);
                } else if (match('.')) {
                    addToken(STAR_DOT);
                } else if (match('=')) {
                    addToken(STAR_EQUAL);
                } else {
                    addToken(STAR);
                }
            }
            // </editor-fold>

            // <editor-fold desc="Operators: ! != !== ">
            case '!' -> {
                if (match('=')) {
                    addToken(match('=') ? BANG_EQUAL_EQUAL : BANG_EQUAL);
                } else {
                    addToken(BANG);
                }
            }
            // </editor-fold>

            // </editor-fold>

            // </editor-fold>

            // <editor-fold desc="2.1 Whitespace">
            case ' ', '\r', '\t', '\n' -> {
                // Ignore whitespace for now
            }
            // </editor-fold>

            // Rest
            default -> {
                if (isDigit(c)) {
                    decimalNumber();
                } else {
                    ScannerDiagnostic.unexpectedCharacter(source.currentSpan());
                }
            }
        }
    }

    private void hexadecimalNumber() {
        consumeAlphanumericCharacters();
        parseNumberToken(source.lexeme());
    }

    private void binaryNumber() {
        consumeAlphanumericCharacters();
        parseNumberToken(source.lexeme());
    }

    private void decimalNumber() {
        greedyDecimalNumberScan();
        parseNumberToken(source.lexeme());
    }

    private void consumeAlphanumericCharacters() {
        while (!isAtEnd() && (isAlphaNumeric(peek()) || peek('_'))) advance();
    }

    private void consumeNumericCharacters() {
        while (!isAtEnd() && (isDigit(peek()) || peek('_'))) advance();
    }

    private void greedyDecimalNumberScan() {
        consumeNumericCharacters();

        while (!isAtEnd()) {
            // Case A: Decimal point followed by digit (e.g. .5 in 1.5, or .3 in 1.2.3)
            if (peek('.') && isDigit(peekNext())) {
                advance(); // consume '.'
                consumeNumericCharacters();
            }

            // Case B: Exponent with mandatory sign (e.g. e+5 in 1.2e+5, or e+3 in 1e+2e+3)
            else if ((peek('e') || peek('E')) && (peekNext() == '+' || peekNext() == '-')) {
                advance(); // consume 'e' or 'E'
                advance(); // consume '+' or '-'
                consumeNumericCharacters();
            }

            // Case C: Magnitude suffixes, attached letters, or misplaced digits (e.g. k in 1k, or ms in 100ms)
            else if (isAlphaNumeric(peek()) || peek('_')) {
                advance();
            }

            // Case D: Hit an operator (+, -, *, /), punctuation (,, ;), or whitespace -> STOP!
            else {
                break;
            }
        }
    }

    private void parseNumberToken(String string) {
        if (!string.isEmpty()) {
            try {
                var value = NumberLiteral.parse(string);
                if (value != null) {
                    addToken(NUMBER, value);
                    return;
                }
            } catch (NumberLiteralFormatError nlfe) {
                switch (nlfe.getDetail()) {
                    case MissingDigitsAfterPrefix detail -> {
                        var base = detail.base();
                        var prefix = source.currentSpan();
                        var primary = source.spanAt(prefix.startOffset() + detail.offset(), detail.length());
                        ScannerDiagnostic.numericLiteralError(
                                "Failed to parse " + base.toLowerCase() + " literal value",
                                Label.of(prefix, "Missing " + base.toLowerCase() + " digits after '" + detail.prefix() + "' prefix"),
                                Label.primaryOf(primary, "One or more " + base.toLowerCase() + " digits (" + base.validDigits() + ") expected!"));
                    }
                    case MissingDecimalDigits detail -> {
                        var literal = source.currentSpan();
                        var invalid = source.spanAt(literal.startOffset() + detail.offset(), detail.length());
                        ScannerDiagnostic.numericLiteralError(
                                "Failed to parse decimal literal value",
                                Label.of(literal, "Invalid decimal literal!"),
                                Label.primaryOf(invalid, "Missing decimal digits!"));
                    }
                    case InvalidDigit detail -> {
                        var base = detail.base();
                        var literal = source.currentSpan();
                        var digit = source.spanAt(literal.startOffset() + detail.offset(), detail.length());
                        ScannerDiagnostic.numericLiteralError(
                                "Failed to parse " + base.toLowerCase() + " literal value",
                                Label.of(literal, "Invalid " + base.toLowerCase() + " literal!"),
                                Label.primaryOf(digit, "'" + detail.character() + "' is not a valid " + base.toLowerCase() + " digit (" + base.validDigits() + ")!"));
                    }
                    case MultipleDecimalPoints detail -> {
                        var literal = source.currentSpan();
                        var digit = source.spanAt(literal.startOffset() + detail.offset(), detail.length());
                        ScannerDiagnostic.numericLiteralError(
                                "Multiple decimal points in numeric literal",
                                Label.of(literal, "Invalid decimal literal!"),
                                Label.primaryOf(digit, "Second decimal point '.' is not allowed"));
                    }
                    case MissingDigitsAfterDecimalPoint detail -> {
                        var literal = source.currentSpan();
                        var digit = source.spanAt(literal.startOffset() + detail.offset(), detail.length());
                        ScannerDiagnostic.numericLiteralError(
                                "Missing digits after decimal point",
                                Label.of(literal, "Invalid decimal literal!"),
                                Label.primaryOf(digit, "Expected digits after decimal point"));
                    }
                    case MissingExponentSign detail -> {
                        var literal = source.currentSpan();
                        var exponent = source.spanAt(literal.startOffset() + detail.exponentOffset(), detail.exponentLength());
                        var missing = source.spanAt(literal.startOffset() + detail.offset(), detail.length());
                        ScannerDiagnostic.numericLiteralError(
                                "Missing sign in decimal exponent",
                                Label.of(literal, "Invalid decimal literal!"),
                                Label.of(exponent, "Malformed exponent"),
                                Label.primaryOf(missing, "Missing exponent sign"));
                    }
                    case MissingExponentDigits detail -> {
                        var literal = source.currentSpan();
                        var exponent = source.spanAt(literal.startOffset() + detail.exponentOffset(), detail.exponentLength());
                        var missing = source.spanAt(literal.startOffset() + detail.offset(), detail.length());
                        ScannerDiagnostic.numericLiteralError(
                                "Missing digits in decimal exponent",
                                Label.of(literal, "Invalid decimal literal!"),
                                Label.of(exponent, "Malformed exponent"),
                                Label.primaryOf(missing, "Missing digits in decimal exponent"));
                    }
                    case MultipleExponents detail -> {
                        var literal = source.currentSpan();
                        var exponent = source.spanAt(literal.startOffset() + detail.exponentOffset(), detail.exponentLength());
                        var secondExponentStart = source.spanAt(literal.startOffset() + detail.offset(), 1);
                        ScannerDiagnostic.numericLiteralError(
                                "Multiple exponents in decimal literal",
                                Label.of(literal, "Invalid decimal literal!"),
                                Label.of(exponent, "Malformed exponent"),
                                Label.primaryOf(secondExponentStart, "Second exponent 'e' is not allowed"));
                    }
                    case InvalidExponentDigits detail -> {
                        var literal = source.currentSpan();
                        var exponent = source.spanAt(literal.startOffset() + detail.exponentOffset(), detail.exponentLength());
                        var invalid = source.spanAt(literal.startOffset() + detail.offset(), detail.length());
                        ScannerDiagnostic.numericLiteralError(
                                "Invalid exponent digits in decimal exponent",
                                Label.of(literal, "Invalid decimal literal!"),
                                Label.of(exponent, "Malformed exponent"),
                                Label.primaryOf(invalid, "Invalid exponent digits"));
                    }
                    case MisplacedUnderscore detail -> {
                        var base = detail.base();
                        var placement = detail.placement();
                        var message = switch (placement) {
                            case LEADING -> "Underscore is not allowed at the start of a " + base.toLowerCase() + " literal";
                            case TRAILING -> "Underscore is not allowed at the end of a " + base.toLowerCase() + " literal";
                            case DOUBLE -> "Double underscore is not allowed in a " + base.toLowerCase() + " literal";
                            case BEFORE_DECIMAL_POINT -> "Underscore is not allowed immediately before decimal point '.'";
                            case AFTER_DECIMAL_POINT -> "Underscore is not allowed immediately after decimal point '.'";
                        };
                        var literal = source.currentSpan();
                        var underscore = source.spanAt(literal.startOffset() + detail.offset(), detail.length());
                        ScannerDiagnostic.numericLiteralError(
                                message,
                                Label.of(literal, "Invalid " + base.toLowerCase() + " literal!"),
                                Label.primaryOf(underscore, switch (placement) {
                                    case BEFORE_DECIMAL_POINT -> "Invalid underscore before decimal point";
                                    case AFTER_DECIMAL_POINT -> "Invalid underscore after decimal point";
                                    default -> "Invalid " + placement.toLowerCase() + " underscore";
                                }));
                    }
                }

                addToken(ERROR);
                return;
            }
        }

        InternalError.unexpectedCompilerError(source.currentSpan(), "Failed to parse numeric literal");
        addToken(ERROR);
    }

    private void lineComment() {
        while (!peek('\n') && !isAtEnd()) advance();
    }

    private void multiLineComment() {
        var start = source.currentSpan();
        while (!isAtEnd()) {
            var c = advance();
            switch (c) {
                case '*' -> {
                    if (match('/')) {
                        return;
                    }
                }
                case '/' -> {
                    if (match('*')) {
                        multiLineComment();
                    }
                }
            }
        }

        ScannerDiagnostic.unbalancedMultilineComment(start, source.spanAt(source.current(), 1));
    }

    private void addToken(TokenType tokenType) {
        tokens.add(source.token(tokenType));
    }

    private void addToken(TokenType tokenType, Object value) {
        tokens.add(source.token(tokenType, value));
    }

    private void addDoubleToken(TokenType singleToken, char secondChar, TokenType doubleToken) {
        addToken(match(secondChar) ? doubleToken : singleToken);
    }

    private boolean isAlphaNumeric(char c) {
        return isDigit(c) || isAlpha(c);
    }

    private boolean isHexDigit(char c) {
        return isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    //<editor-fold desc="Scanner delegate methods">

    private boolean isAtEnd() {
        return source.isAtEnd();
    }

    private char advance() {
        return source.advance();
    }

    private boolean match(char c) {
        return source.match(c);
    }

    private boolean match(CharPredicate predicate) {
        return source.match(predicate);
    }

    private char peek() {
        return source.peek();
    }

    private boolean peek(char c) {
        return source.peek(c);
    }

    private char peekNext() {
        return source.peekNext();
    }

    //</editor-fold>

}