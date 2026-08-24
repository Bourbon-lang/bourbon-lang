package org.bourbon.compiler;

import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPathExpressionException;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.TestInstantiationException;

class ScannerTestCaseParser {

    public static final String HEADER_SEPARATOR_REGEX = "^={3,}$";
    public static final String TEST_CASE_SEPARATOR_REGEX = "^-{3,}$";

    private final Source source;

    private final List<Token> expectedTokens = new ArrayList<>();
    private final List<Diagnostic> expectedDiagnostics = new ArrayList<>();
    private final ScannerParserReporter Report;

    @Nullable
    private String displayName = null;

    ScannerTestCaseParser(Source source) {
        this.source = source;
        this.Report = new ScannerParserReporter(source);
    }

    public String getDisplayName() {
        return displayName != null && !displayName.isBlank() ? displayName : source.name();
    }

    @FunctionalInterface
    public interface SourceLineIndex {
        int lineOffsetAt(String sourceName, int lineNumber);
    }

    public ScannerTestCase parseTestCase() {
        displayName = consumeHeader();

        var source = consumeSource();
        var testCaseInput = source.source();
        var lineOffsets = source.lineOffsets();

        if (!isAtEnd()) {
            consumeExpectations(lineOffsets);
        }

        var namedSource = Source.named(this.source.name()).of(testCaseInput);
        return new ScannerTestCase(namedSource, expectedTokens(source), expectedDiagnostics);
    }

    private List<Token> expectedTokens(TestCaseSource source) {
        var lineOffsets = source.lineOffsets();
        if (expectedTokens.isEmpty() || expectedTokens.getLast().type() != TokenType.EOF) {
            int lineNumber = lineOffsets.lines();
            expectedTokens.add(new Token(TokenType.EOF, "", null,
                    lineNumber, 1, source.source().length(), 0));
        }

        return expectedTokens;
    }

    private void consumeExpectations(LineIndex lineIndex) {
        int lineNumber = 0;
        int lineOffset = 0;
        while (!isAtEnd()) {
            var line = skipEmptyLines();
            if (line.matches(TEST_CASE_SEPARATOR_REGEX)) {
                lineOffset = lineIndex.lineOffset(++lineNumber);
                source.tokenStart();
                continue;
            }

            if (line.stripLeading().startsWith("#")) {
                // This is a line comment. Skip it.
                source.tokenStart();
                continue;
            }

            if (lineNumber == 0) throw Report.expectLineSeparator();

            // Reset scanner position to start of the line
            source.tokenReset();
            if (TestCaseDiagnosticParser.isDiagnosticStart(source)) {
                expectedDiagnostics.add(consumeDiagnostic(lineNumber, (_, number) -> lineIndex.lineOffset(number)));
            } else {
                Token token = consumeToken(lineNumber, lineOffset);
                if (token != null)
                    expectedTokens.add(token);
            }
        }
    }

    private @Nullable Token consumeToken(int lineNumber, int lineOffset) {
        return TestCaseTokenParser.parse(source, lineNumber, lineOffset);
    }

    private Diagnostic consumeDiagnostic(int lineNumber, SourceLineIndex sourceLineIndex) {
        return TestCaseDiagnosticParser.parse(source, lineNumber, sourceLineIndex);
    }

    private String consumeHeader() {
        requireHeaderSeparator();
        var displayName = requireDisplayName().trim();
        requireHeaderSeparator();

        return displayName;
    }

    private String requireDisplayName() {
        String line;
        line = skipEmptyLines();
        if (line.matches(HEADER_SEPARATOR_REGEX)) {
            throw Report.expectDisplayName();
        }
        return line;
    }

    private void requireHeaderSeparator() {
        var line = skipEmptyLines();
        if (!line.matches(HEADER_SEPARATOR_REGEX)) {
            throw Report.expectHeaderSeparator();
        }
    }

    record TestCaseSource(String source, LineIndex lineOffsets) {}

    private TestCaseSource consumeSource() {
        source.tokenStart();

        var startSpan = source.currentSpan();
        var startLine = startSpan.line();
        var startOffset = startSpan.startOffset();

        while (!isAtEnd()) {
            var line = nextLine();
            if (line.matches(TEST_CASE_SEPARATOR_REGEX)) {
                source.tokenReset();
                break;
            }
        }

        var sourceText = source.subSequence(startOffset, source.current());
        var lineOffsets = source.lineOffsets().slice(startLine, source.currentLine());

        return new TestCaseSource(sourceText.toString(), lineOffsets);
    }

    private String skipEmptyLines() {
        var line = nextLine();
        while (!isAtEnd() && line.isBlank()) {
            line = nextLine();
        }
        return line;
    }

    private String nextLine() {
        source.tokenStart();
        return advanceLine();
    }

    private String advanceLine() {
        while (!isAtEnd() && !source.peek('\n'))
            source.advance();
        // Return the line without trailing newline
        var line = source.lexeme();
        if (!isAtEnd())
            source.advance();
        return line;
    }

    private boolean isAtEnd() {
        return source.isAtEnd();
    }

    private record ScannerParserReporter(Source source) {
        private Diagnostic report(Diagnostic diagnostic) {
            DiagnosticReporter.report(diagnostic);
            return diagnostic;
        }

        private TestInstantiationException error(Diagnostic diagnostic) {
            return TestInitiaitionErrors.toException(report(diagnostic));
        }

        private TestInstantiationException error(String message, Label label) {
            return error(Diagnostic.error(Diagnostic.Code.ScannerTestCaseParserError, message, List.of(label)));
        }

        TestInstantiationException expectLineSeparator() {
            return error("Failed to parse test case specification " + source.name(),
                    Label.primaryOf(source.currentSpan(), "Expected at least one line separator: \"---\""));
        }

        TestInstantiationException expectHeaderSeparator() {
            return error("Failed to parse test case specification " + source.name(),
                    Label.primaryOf(source.currentSpan(), "Expected test header separator (a line of three or more '=' characters)"));
        }

        TestInstantiationException expectDisplayName() {
            return error("Failed to parse test case specification " + source.name(),
                    Label.primaryOf(source.currentSpan(), "Expected test case display name"));
        }
    }
}
