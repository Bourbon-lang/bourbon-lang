package org.bourbon.compiler.diagnostic.code;

/// Error codes related to compiler lexical structure and syntax.
public enum SyntaxErrorCode implements DiagnosticCode {

    /// Unexpected character while scanning the source
    UnexpectedCharacter("SYN0001", "Unexpected character"),

    /// Unbalanced multiline comment
    UnbalancedMultilineComment("SYN0002", "Unbalanced multiline comment"),

    /// Failure to parse numeric literal value
    NumericLiteralError("SYN0003", "Failed to parse numeric literal");

    private final String id;
    private final String title;

    SyntaxErrorCode(String id, String title) {
        this.id = id;
        this.title = title;
    }

    @Override public String id() {
        return id;
    }

    @Override public String title() {
        return title;
    }
}
