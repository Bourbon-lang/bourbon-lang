package org.bourbon.compiler;

import org.bourbon.compiler.diagnostic.code.DiagnosticCode;

public enum TestCaseError implements DiagnosticCode {
    TestCaseParserError("TST0001", "Test Case Parser Error");

    private final String id;
    private final String title;

    TestCaseError(String id, String title) {
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
