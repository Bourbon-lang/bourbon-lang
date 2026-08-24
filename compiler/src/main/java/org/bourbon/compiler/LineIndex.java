package org.bourbon.compiler;

import java.util.Objects;

/// Zero-based index of line offsets in the source file.
///
/// When reading a source file, the line index is build ahead of time to speed up line/ and column number lookups.
///
/// Worth noting that line- and column numbers are all 1-based as they are presented in editor- and diagnostic
/// messages for human consumption.
public sealed interface LineIndex permits DenseLineIndex, LineIndex.Slice {

    int lineOffset(int line);

    int lineNumberOf(int offset);

    int columnNumberOf(int offset);
    /// Return a number of lines in the source file
    int lines();

    static LineIndex scan(CharSequence input) {
        var lineOffsets = LineOffsetScanner.scanSimple(input);
        return new DenseLineIndex(input.length(), lineOffsets);
    }

    /// Return a separate line index based on a sublist of lines in the source file
    default LineIndex slice(int firstLine, int lastLine) {
        return new Slice(this, firstLine, lastLine);
    }

    static void checkLineNumber(int lineNumber, int lineCount) {
        if (lineNumber < 1 || lineNumber >= lineCount + 1)
            throw new IndexOutOfBoundsException("Line number " + lineNumber + " is out of bounds [1.." + lineCount + "]");
    }

    /// A delimited partial view of LineIndex.
    final class Slice implements LineIndex {
        private final LineIndex original;
        private final int firstLine;
        private final int lineCount;
        private final int startOffset;
        private final int endOffset;

        public Slice(LineIndex original, int firstLine, int lastLine) {
            this.original = original;
            this.firstLine = firstLine;

            boolean hasPhantom = lastLine == original.lines();
            this.lineCount = (lastLine - firstLine + 1) + (hasPhantom ? 0 : 1);

            this.startOffset = original.lineOffset(firstLine);
            this.endOffset = original.lineOffset(hasPhantom ? lastLine : lastLine + 1);
        }

        @Override public int lineOffset(int line) {
            LineIndex.checkLineNumber(line,  lineCount);
            return original.lineOffset(firstLine + line - 1) - startOffset;
        }

        @Override public int lineNumberOf(int offset) {
            Objects.checkIndex(offset, size() + 1);
            return original.lineNumberOf(startOffset + offset) - firstLine + 1;
        }

        @Override public int columnNumberOf(int offset) {
            Objects.checkIndex(offset, size() + 1);
            return original.columnNumberOf(offset + startOffset);
        }
        @Override public int lines() {
            return lineCount;
        }

        private int size() {
            return endOffset - startOffset;
        }

    }

}
