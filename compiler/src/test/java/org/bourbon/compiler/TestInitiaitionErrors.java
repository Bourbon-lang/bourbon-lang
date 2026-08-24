package org.bourbon.compiler;


import java.util.Arrays;

import org.junit.jupiter.api.extension.TestInstantiationException;

/// Parser errors factory
public class TestInitiaitionErrors {

    private TestInitiaitionErrors(Source source) {
        throw new UnsupportedOperationException("Do not instantiate!");
    }

    public static TestInstantiationException toException(Diagnostic diagnostic) {
        var label = diagnostic.labels().stream().filter(Label::isPrimary).findFirst()
                .orElseGet(diagnostic.labels()::getFirst);

        var suggestion = diagnostic.suggestions().isEmpty() ? null : diagnostic.suggestions().getFirst();
        var message = "%s: %s on line %d, column %d".formatted(
                diagnostic.message(), label.message(), label.span().line(), label.span().column());

        if (suggestion != null)
            message += "\nSuggestion: " + suggestion;

        var exception = new TestInstantiationException(message);

        var stackTrace = exception.getStackTrace();
        int i = 0;
        while (i < stackTrace.length) {
            var element = stackTrace[i];
            if (!element.getClassName().equals(TestCaseDiagnosticParser.DiagnosticReportWrapper.class.getName())) {
                break;
            }
            i++;
        }
        exception.setStackTrace(Arrays.copyOfRange(stackTrace, i, stackTrace.length));
        return exception;
    }

}
