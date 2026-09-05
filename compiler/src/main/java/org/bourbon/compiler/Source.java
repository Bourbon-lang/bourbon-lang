package org.bourbon.compiler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Stateful source for scanner.
 */
@NullMarked
public class Source implements CharSequence {

    private final CharSequence content;
    private final LineIndex lineIndex;
    private final String name;

    private int start = 0;
    private int current = 0;

    private Source(CharSequence content, LineIndex lineIndex, String name) {
        this.content = content;
        this.lineIndex = lineIndex;
        this.name = name;
    }

    public CharSequence content() {
        return content;
    }

    public String name() {
        return name;
    }

    public CharSequence line(int line) {
        int startOffset = lineIndex.lineOffset(line);
        int endOffset = lineIndex.lineOffset(line + 1);
        return subSequence(startOffset, endOffset);
    }

    @FunctionalInterface
    public interface CharPredicate {
        boolean test(char c);
    }

    //<editor-fold desc="Current source positions">

    int column() {
        return column(start);
    }

    int column(int offset) {
        return lineIndex.columnNumberOf(offset);
    }

    int start() {
        return start;
    }

    int current() {
        return current;
    }

    int currentLine() {
        return lineIndex.lineNumberOf(current);
    }

    int startLine() {
        return lineIndex.lineNumberOf(start);
    }

    public LineIndex lineOffsets() {
        return lineIndex;
    }

    //</editor-fold>

    //<editor-fold desc="Scanner helpers">

    public boolean isAtEnd() {
        return current >= length();
    }

    public void tokenStart() {
        start = current;
    }

    public void tokenReset() {
        current = start;
    }

    Source resetToLine(int line) {
        current = lineIndex.lineOffset(line);
        tokenStart();
        return this;
    }

    public char advance() {
        if (isAtEnd()) return '\0';
        return charAt(current++);
    }

    public char peek() {
        if (isAtEnd()) return '\0';
        return charAt(current);
    }

    public boolean peek(char expected) {
        if (isAtEnd()) return false;
        return charAt(current) == expected;
    }

    public boolean peek(CharSequence expected) {
        if (isAtEnd()) return false;

        for (int i = 0; i < expected.length(); i++) {
            if (charAt(current + i) != expected.charAt(i))
                return false;
        }
        return true;
    }

    public char peekNext() {
        if (current + 1 >= length()) return '\0';
        return charAt(current + 1);
    }

    public boolean match(char expected) {
        if (peek(expected)) {
            advance();
            return true;
        }

        return false;
    }

    public boolean match(CharSequence expected) {
        if (peek(expected)) {
            // advance steps through each character,
            // counting line numbers and offsets
            expected.chars().forEach(_ -> advance());
            return true;
        }

        return false;
    }

    public boolean match(CharPredicate expected) {
        if (expected.test(peek())) {
            advance();
            return true;
        }

        return false;
    }

    public String lexeme() {
        return subSequence(start, current).toString();
    }

    public Token token(TokenType type) {
        return token(type, null);
    }

    public Token token(TokenType type, @Nullable Object literal) {
        return new Token(type, lexeme(), literal, startLine(), column(), start, current - start);
    }

    //</editor-fold>

    //<editor-fold desc="SourceSpan creation">

    protected SourceSpan.SourceName sourceName() {
        return SourceSpan.SourceName.of(name());
    }

    protected final SourceSpan.Provider sourceSpan() {
        return (int line, int column, int startOffset, int length) ->
                new SourceSpan(sourceName(), line, column, startOffset, length);
    }

    public final SourceSpan spanAt(int startOffset, int length) {
        if (startOffset == start) return sourceSpan().at(lineIndex.lineNumberOf(startOffset), column(start), start, length);
        if (startOffset == current) return sourceSpan().at(currentLine(), column(current), current, length);

        return sourceSpan().at(lineIndex.lineNumberOf(startOffset), column(startOffset), startOffset, length);
    }

    public final SourceSpan currentSpan() {
        return sourceSpan().at(startLine(), column(start), start, current - start);
    }

    //</editor-fold>

    //<editor-fold desc="CharSequence default implementation">

    @Override
    public final int length() {
        return content().length();
    }

    @Override
    public final char charAt(int index) {
        return content().charAt(index);
    }

    @Override
    public final CharSequence subSequence(int start, int end) {
        return content().subSequence(start, end);
    }

    public final String toString() {
        return content().toString();
    }

    //</editor-fold>

    //<editor-fold desc="Creating a source">

    public static Source of(CharSequence content) {
        if (content instanceof Source source) {
            return named(source.name()).of(source.content());
        }
        return named("<string>").of(content);
    }

    public static Source of(InputStream stream) throws IOException {
        if (stream == System.in) return stdIn();
        return named("<stream>").of(Content.read(stream));
    }

    public static Source of(Path filePath) throws IOException {
        String sourceName = filePath.getFileName().toString();
        return named(sourceName).of(Content.read(filePath));
    }

    public static Source stdIn() throws IOException {
        return named("<stdin>").of(Content.read(System.in));
    }

    public static SourceName named(String name) {
        return new SourceName(name);
    }

    public static class SourceName {
        private final String name;

        private SourceName(String name) {
            this.name = name;
        }

        public Source of(CharSequence content) {
            var lineIndex = LineIndex.scan(content);
            return new Source(content, lineIndex, name);
        }
    }

    static abstract class Content {
        public static CharSequence read(InputStream input) throws IOException {
            return read(input, StandardCharsets.UTF_8);
        }

        public static CharSequence read(InputStream input, Charset charset) throws IOException {;
            return new String(input.readAllBytes(), charset);
        }

        public static CharSequence read(Path filePath) throws IOException {
            return Files.readString(filePath);
        }
    }

    //</editor-fold>

    //<editor-fold desc="Debugger helpers">

    @SuppressWarnings("unused")
    private String debugCurrentSpan() {
        String s = "";
        if (start > 5) {
            s += charAt(0) + "/../" + charAt(start - 1);
        } else {
            s += subSequence(0, start);
        }

        if (start < current) {
            s += "⸢" + lexeme() + "⸣";
        }

        s += "⸤" + peek() + "⸥";

        if (current < length() - 5) {
            s += "/../" + charAt(length() - 1);
        } else {
            s += subSequence(current + 1, length());
        }

        return s;
    }

    @SuppressWarnings("unused")
    private String debugStartLine() {
        String s = "";
        int startLine = startLine();
        int startOffset = lineIndex.lineOffset(startLine);
        if (startOffset < start) s += subSequence(startOffset, start);

        int endOffset = lineIndex.lineOffset(startLine + 1);
        if (start < current && current < endOffset) {
            s += "⸢" + subSequence(start, current) + "⸣";
            s += "⸤" + charAt(current) + "⸥";
            s += subSequence(current + 1, endOffset);
        }
        else if (start == current && current < endOffset) {
            s += "⸤" + charAt(current) + "⸥";
            s += subSequence(current + 1, endOffset);
        }
        else if (start < endOffset && endOffset < current) {
            s += "⸢" + subSequence(start, endOffset) + "…";
        }

        return s;
    }

    //</editor-fold>
}
