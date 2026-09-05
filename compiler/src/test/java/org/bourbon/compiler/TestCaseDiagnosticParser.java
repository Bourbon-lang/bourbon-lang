package org.bourbon.compiler;

import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import org.bourbon.compiler.ScannerTestCaseParser.SourceLineIndex;
import org.bourbon.compiler.SourceSpan.SourceName;
import org.bourbon.compiler.advisory.Advisory;
import org.bourbon.compiler.diagnostic.Diagnostic;
import org.bourbon.compiler.diagnostic.Diagnostic.Severity;
import org.bourbon.compiler.diagnostic.code.Catalog;
import org.bourbon.compiler.diagnostic.code.DiagnosticCode;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.TestInstantiationException;

public class TestCaseDiagnosticParser {

    private final Source source;
    private final int lineNumber;
    private final SourceLineIndex sourceLineIndex;

    private final DiagnosticReportWrapper Report = new DiagnosticReportWrapper();

    public TestCaseDiagnosticParser(Source source, int lineNumber, SourceLineIndex sourceLineIndex) {
        this.source = source;
        this.lineNumber = lineNumber;
        this.sourceLineIndex = sourceLineIndex;
    }

    public static Diagnostic parse(Source source, int lineNumber, SourceLineIndex sourceLineIndex) {
        return new TestCaseDiagnosticParser(source, lineNumber, sourceLineIndex).parseDiagnostic();
    }

    public static boolean isDiagnosticStart(Source source) {
        for (var severity : Advisory.Severity.values()) {
            var severityName = severity.name().toLowerCase(Locale.ROOT);
            if (source.peek(severityName)) return true;
        }

        return false;
    }

    private Diagnostic parseDiagnostic() {
        var severity = consumeSeverity();
        var code = consumeCode();
        var message = consumeMessage();

        String sourceFileName;
        int primaryLine = lineNumber;
        int primaryColumn;

        var labels = new ArrayList<Label>();
        while (!source.isAtEnd()) {
            if (!consumeArrow()) break;
            sourceFileName = consumeSourceFileName();
            requireCharacter(':', () -> "Expecting ':' after source file name");
            primaryLine = consumeInteger();
            requireCharacter(':', () -> "Expecting ':' after primary line number");
            primaryColumn = consumeInteger();
            skipWhitespace();
            requireNewline();

            sourceLines:
            while (!source.isAtEnd()) {
                switch (consumeSourceLabel()) {
                    case LabelResult.Some(int labelLine, int labelColumn, int spanLength, String labelMessage) -> {
                        var startOffset = sourceLineIndex.lineOffsetAt(sourceFileName, labelLine) + labelColumn - 1;
                        var sourceSpan = new SourceSpan(SourceName.of(sourceFileName), labelLine, labelColumn, startOffset, spanLength);
                        var isPrimary = labelLine == primaryLine && labelColumn == primaryColumn;
                        labels.add(new Label(sourceSpan, labelMessage, isPrimary));
                    }

                    case LabelResult.Empty.EMPTY_SOURCE_LINE -> {
                        continue;
                    }

                    case LabelResult.Empty.BLANK_LINE, LabelResult.Empty.NOT_LABEL -> {
                        break sourceLines;
                    }
                };

            }

            skipBlankLines();
        }

        if (labels.isEmpty()) throw Report.expectLabels();
        return new Diagnostic(code, severity, message, labels);
    }

    private int consumeInteger() {
        advanceInteger();
        try {
            int integer = Integer.parseInt(source.lexeme());
            source.tokenStart();
            return integer;
        } catch (NumberFormatException e) {
            throw Report.expecInteger();
        }
    }

    private void advanceInteger() {
        while (!isAtEndOfLine() && Character.isDigit(source.peek()))
            source.advance();
    }

    private String consumeSourceFileName() {
        advanceUntilMatch(':');
        var sourceFileName = source.lexeme();
        if (sourceFileName.isBlank())
            throw Report.expectSourceFileName();

        return sourceFileName.trim();
    }

    private boolean consumeArrow() {
        advanceWhitespace();
        if (source.match("--> ")) {
            skipWhitespace();
            return true;
        }

        source.tokenReset();
        return false;
    }

    sealed interface LabelResult {
        enum Empty implements LabelResult { EMPTY_SOURCE_LINE, BLANK_LINE, NOT_LABEL }
        record Some(int lineNumber, int columnNumber, int spanLength, String message) implements LabelResult {

        }
    }
    private LabelResult consumeSourceLabel() {
        skipWhitespace();
        if (isAtEndOfLine()) {
            requireNewline();
            return LabelResult.Empty.BLANK_LINE;
        }

        if (source.match('|')) {
            skipWhitespace();
            requireNewline();
            return LabelResult.Empty.EMPTY_SOURCE_LINE;
        }

        if (!Character.isDigit(source.peek())) {
            source.tokenReset();
            return LabelResult.Empty.NOT_LABEL;
        }

        var lineNumber = consumeInteger();
        skipWhitespace();
        requireCharacter('|', () -> "Expecting '|' after source line number");
        if (!isAtEndOfLine()) {
            requireCharacter(' ', () -> "Expecting at least one space indent after line number gutter separator");
        }
        while (!isAtEndOfLine()) source.advance();
        requireNewline();

        skipWhitespace();
        requireCharacter('|', () -> "Expecting '|' before diagnostic label message");
        int columnNumber = consumeUnderlineIndent();
        var spanLength = consumeLabelUnderline();
        skipWhitespace();
        var message = consumeLabelMessage();

        return new LabelResult.Some(lineNumber, columnNumber, spanLength, message);
    }

    private @NonNull String consumeLabelMessage() {
        while (!isAtEndOfLine()) source.advance();
        var message = source.lexeme().trim();
        requireNewline();
        return message;
    }

    private int consumeLabelUnderline() {
        while (!isAtEndOfLine() && source.peek('^')) source.advance();
        var spanLength = source.lexeme().length();
        source.tokenStart();
        return spanLength;
    }

    private int consumeUnderlineIndent() {
        var indent = advanceWhitespace();
        if (indent.isEmpty())
            throw Report.expectingSourceLineIndent();
        int columnNumber = source.lexeme().length();
        source.tokenStart();
        return columnNumber;
    }

    private String consumeMessage() {
        requireCharacter(':', () -> "Expecting ':' before message");
        skipWhitespace();

        if (isAtEndOfLine())
            throw Report.expectingDiagnosticMessage();

        while (!isAtEndOfLine()) source.advance();
        var message = source.lexeme();
        source.tokenStart();

        requireNewline();
        return message;
    }

    private DiagnosticCode consumeCode() {
        requireCharacter('[', () -> "Expecting '['");
        while (!source.isAtEnd() && Character.isLetterOrDigit(source.peek())) source.advance();
        var code = parseCode(source.lexeme());
        requireCharacter(']', () -> "Expecting ']' after diagnostic code");
        skipWhitespace();
        return code;
    }

    private DiagnosticCode parseCode(String value) {
        DiagnosticCode code = Catalog.find(value).orElseGet(() -> new ParseDiagnosticCode(value));
        source.tokenStart();
        return code;
    }

    private void requireCharacter(char c, Supplier<String> message) {
        if (!source.match(c)) {
            throw Report.expectingCharacter(message.get());
        }
        source.tokenStart();
    }

    private void requireNewline() {
        if (!source.isAtEnd() && !source.match('\n')) {
            throw Report.expectingCharacter("Expecting newline");
        }
        source.tokenStart();
    }

    private Severity consumeSeverity() {
        skipWhitespace();
        for (var severity : Severity.values()) {
            if (source.match(severity.name().toLowerCase(Locale.ROOT))) {
                skipWhitespace();
                return severity;
            }
        }

        throw Report.severityExpected();
    }

    private void skipWhitespace() {
        advanceWhitespace();
        source.tokenStart();
    }

    private String advanceWhitespace() {
        while (!isAtEndOfLine() && Character.isWhitespace(source.peek())) source.advance();
        return source.lexeme();
    }

    private void skipBlankLines() {
        while (!source.isAtEnd()) {
            advanceWhitespace();
            if (!isAtEndOfLine()) {
                source.tokenReset();
                return;
            }
            requireNewline();
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void advanceUntilMatch(char end) {
        while (!isAtEndOfLine() && source.peek() != end) {
            source.advance();
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isAtEndOfLine() {
        return source.isAtEnd() || source.peek('\n');
    }

    @SuppressWarnings("SameParameterValue")
    class DiagnosticReportWrapper {

        private @NonNull TestInstantiationException exception(Diagnostic diagnostic, Label label) {
            return exception(diagnostic, label, null);
        }

        private @NonNull TestInstantiationException exception(Diagnostic diagnostic, Label label, @Nullable String suggestion) {
            var message = "%s: %s on line %d, column %d".formatted(
                    diagnostic.message(), label.message(), label.span().line(), label.span().column());
            if (suggestion != null)
                message += "\nSuggestion: " + suggestion;

            var exception = new TestInstantiationException(message);

            var stackTrace = exception.getStackTrace();
            int i = 0;
            while (i < stackTrace.length) {
                var element = stackTrace[i];
                if (!element.getClassName().equals(DiagnosticReportWrapper.class.getName())) {
                    break;
                }
                i++;
            }
            exception.setStackTrace(Arrays.copyOfRange(stackTrace, i, stackTrace.length));
            return exception;
        }

        TestInstantiationException error(String message, Label label) {
            var error = Diagnostic.error(TestCaseError.TestCaseParserError, message, List.of(label));
            return exception(error, label);
        }

        TestInstantiationException error(String message, Label label, Label suggestion) {
            var error = Diagnostic.error(TestCaseError.TestCaseParserError, message, List.of(label, suggestion));
            return exception(error, label, suggestion.message());
        }

        @SuppressWarnings("UnusedReturnValue")
        Diagnostic warning(String message, Label label) {
            return Diagnostic.warning(TestCaseError.TestCaseParserError, message, List.of(label));
        }

        TestInstantiationException expectingCharacter(String message) {
            return error("Failed to parse diagnostic message!", Label.primaryOf(source.currentSpan(), message));
        }

        TestInstantiationException severityExpected() {
            var severityNames = Arrays.stream(Severity.values())
                    .map(Severity::name)
                    .map(String::toLowerCase)
                    .collect(joining("|"));

            return error("Failed to parse diagnostic message header!",
                    Label.primaryOf(source.currentSpan(), "Expected diagnostic message severity"),
                    Label.of(source.currentSpan(), "Must be one of " + severityNames));
        }

        TestInstantiationException expectingDiagnosticCode() {
            return error("Failed to parse diagnostic message header!",
                    Label.primaryOf(source.currentSpan(), "Expected diagnostic message code"));
        }

        TestInstantiationException expectingDiagnosticMessage() {
            return error("Failed to parse diagnostic message header!",
                    Label.primaryOf(source.currentSpan(), "Expected diagnostic message"));
        }

        TestInstantiationException expectLabels() {
            return error("Failed to parse diagnostic message!",
                    Label.primaryOf(source.currentSpan(), "Expected one or more diagnostic labels"));
        }

        TestInstantiationException expectSourceFileName() {
            return error("Failed to parse diagnostic message!",
                    Label.primaryOf(source.currentSpan(), "Expected source file name"));
        }

        TestInstantiationException expecInteger() {
            return error("Failed to parse diagnostic message!",
                    Label.primaryOf(source.currentSpan(), "Expected an integer"));
        }

        void primaryLineNumberMismatch(int primaryLine, int lineNumber) {
            warning("Primary line number mismatch! Expected %d but found %d".formatted(primaryLine, lineNumber),
                    Label.primaryOf(source.currentSpan(), "Expected primary line number"));
        }

        TestInstantiationException expectingSourceLineIndent() {
            return error("Failed to parse diagnostic message!",
                    Label.primaryOf(source.currentSpan(), "Expected at least one character source line indent"));
        }

    }

    @NullMarked
    private record ParseDiagnosticCode(String id) implements DiagnosticCode {
        @Override public String title() {
            return "Unregistered diagnostic code";
        }

    }

}
