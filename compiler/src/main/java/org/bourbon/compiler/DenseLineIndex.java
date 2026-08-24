package org.bourbon.compiler;

import java.util.Arrays;
import java.util.Objects;

/// Simple dense line index backed by an array of line offsets.
public final class DenseLineIndex implements LineIndex {
    private final int inputSize;
    private final int[] lineOffsets;

    public DenseLineIndex(int inputSize, int[] lineOffsets) {
        this.inputSize = inputSize;
        this.lineOffsets = lineOffsets;
    }

    @Override public int lineOffset(int line) {
        LineIndex.checkLineNumber(line, lineOffsets.length);
        return lineOffsets[line - 1];
    }

    @Override public int lineNumberOf(int offset) {
        Objects.checkIndex(offset, inputSize + 1); // allows offset == inputSize (EOF)
        int index = Arrays.binarySearch(lineOffsets, offset);
        return index < 0 ? -index - 1 : index + 1;
    }

    @Override public int columnNumberOf(int offset) {
        Objects.checkIndex(offset, inputSize + 1); // allows offset == inputSize (EOF)
        int line = lineNumberOf(offset);
        int lineStart = lineOffsets[line - 1];
        return offset - lineStart + 1;
    }

    @Override public int lines() {
        return lineOffsets.length;
    }

}
