package org.bourbon.compiler;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayNameGeneration(DisplayNameGenerator.IndicativeSentences.class)
public class LineIndexTest {

    private static final String SOURCE = """
            1234567890
            abcdefghijklmnopqrstuvwxyz
            
            /* */
            () [] {} ~!@#$%^&*-_+=
            
            """;

    private static final int[] LINE_OFFSETS = LineOffsetScanner.scanSimple(SOURCE);

    @Test
    public void testScannedIndexes() {
        assertArrayEquals(new int[]{0, 11, 38, 39, 45, 68, SOURCE.length()}, LINE_OFFSETS);
    }

    private final DenseLineIndex denseLineIndex = new DenseLineIndex(SOURCE.length(), LINE_OFFSETS);

    @Nested
    class DenseLineIndexTest {
        @ParameterizedTest(name = "Characters at offset {0}-{1} are all on line {3}", quoteTextArguments = false)
        @CsvSource({
                "0, 10, '1234567890', 1",
                "11, 37, 'abcdefghijklmnopqrstuvwxyz', 2",
                "39, 44, '/* */', 4",
                "45, 67, () [] {} ~!@#$%^&*-_+=, 5",
        })
        void lineNumberOf(int startOffset, int endOffset, String expectedChars, int expectedLine) {
            Assumptions.assumeTrue(expectedChars.length() == endOffset - startOffset, "Expected length " + expectedChars.length() + " does not match offset range " + startOffset + "-" + endOffset);
            Assumptions.assumeTrue(expectedChars.equals(SOURCE.substring(startOffset, endOffset)), "Expected characters " + expectedChars + " do not match source substring " + SOURCE.substring(startOffset, endOffset));

            var assertions = Stream.<Executable>builder();
            for (int offset = startOffset; offset <= endOffset; offset++) {
                int charOffset = offset;
                int expectedColumn = charOffset - startOffset + 1;
                int actualColumn = denseLineIndex.columnNumberOf(charOffset);
                int actualLineNumber = denseLineIndex.lineNumberOf(charOffset);
                assertions.add(() -> assertEquals(expectedLine, actualLineNumber, () -> "Char '" + visual(charAt(charOffset)) + "' at offset " + charOffset + " is on line " + expectedLine));
                assertions.add(() -> assertEquals(expectedColumn, actualColumn, () -> "Char '" + visual(charAt(charOffset)) + "' at offset " + charOffset + " is on column " + expectedColumn));
            }
            assertions.add(() -> assertEquals('\n', charAt(endOffset)));
            assertAll(assertions.build());
        }

        @Test
        @DisplayName("Invalid line numbers throw IndexOutOfBoundsException")
        void invalidLineNumber() {
            assertAll("DenseLineIndex",
                throwsExactlyIndexOutOfBoundsException(() -> denseLineIndex.lineOffset(-1), "lineOffset(-1)"),
                throwsExactlyIndexOutOfBoundsException(() -> denseLineIndex.lineOffset(0), "lineOffset(-1)"),
                throwsExactlyIndexOutOfBoundsException(() -> denseLineIndex.lineOffset(LINE_OFFSETS.length + 1),
                        "lineOffset(" + LINE_OFFSETS.length + " + 1)"
                ));
        }

        private char charAt(int offset) {
            Objects.checkIndex(offset, SOURCE.length() + 1);
            if (offset < SOURCE.length())
                return SOURCE.charAt(offset);

            return '\0';
        }
    }

    private <E extends Throwable> Executable throwsExactlyIndexOutOfBoundsException(Executable executable, String message) {
        return () -> {
            var exception = assertThrows(IndexOutOfBoundsException.class, executable, message);
            assertEquals(IndexOutOfBoundsException.class, exception.getClass(), () ->  message + " => throws exactly IndexOutOfBoundsException");
        };
    }

    @Nested
    class LineIndexSliceTest {
        private final LineIndex sliceIndex = denseLineIndex.slice(4, 5);
        private final String source = SOURCE.substring(39, 68);

        @Test void fullSubSlice() {
            var fullSlice = denseLineIndex.slice(1, denseLineIndex.lines());
            assertAll("All offsets match",
                () -> assertAll("All original line offsets match slice offsets",
                        IntStream.range(1, denseLineIndex.lines() + 1).mapToObj(line ->
                                () -> assertEquals(denseLineIndex.lineOffset(line), fullSlice.lineOffset(line), String.valueOf(line)))),
                () -> assertAll("All slice offsets match original line offsets",
                        IntStream.range(1, fullSlice.lines() + 1).mapToObj(line ->
                                () -> assertEquals(fullSlice.lineOffset(line), denseLineIndex.lineOffset(line), String.valueOf(line))))
            );
        }

        @Test void parityWithFreshScan() {
            var scanned = LineIndex.scan(source); // source = SOURCE.substring(39, 68)
            int eofOffset = source.length();
            assertAll("scanned line index",
                () -> assertEquals(scanned.lines(), sliceIndex.lines(), "lines()"),
                () -> assertEquals(scanned.lineNumberOf(eofOffset), sliceIndex.lineNumberOf(eofOffset), () -> "lineNumber(" + eofOffset + ")"),
                () -> assertEquals(scanned.columnNumberOf(eofOffset), sliceIndex.columnNumberOf(eofOffset), () -> "columnNumberOf(" + eofOffset + ")"),
                () -> assertEquals(scanned.lineOffset(scanned.lines()), sliceIndex.lineOffset(sliceIndex.lines()), () -> "lineOffset(" + sliceIndex.lines() + ")"),
                () -> assertDoesNotThrow(() -> sliceIndex.lineOffset(sliceIndex.lineNumberOf(eofOffset)), () -> "lineOffset(lineNumberOf(" + eofOffset + "))"));
        }

        @Test void itStartsAtZeroLineOffsetForFirstLine() {
            assertEquals(0, sliceIndex.lineOffset(1));
        }

        @Test void itStartsAtLineOne() {
            assertEquals(1, sliceIndex.lineNumberOf(0));
        }

        @Test void itStartsAtColumnOne() {
            assertEquals(1, sliceIndex.columnNumberOf(0));
        }
        @ParameterizedTest(name = "Characters at offset {0}-{1} are all on line {3}", quoteTextArguments = false)
        @CsvSource({
                "0, 5, '/* */', 1",
                "6, 28, '() [] {} ~!@#$%^&*-_+=', 2",
        })
        void lineNumberOf(int startOffset, int endOffset, String expectedChars, int expectedLine) {
            Assumptions.assumeTrue(expectedChars.length() == endOffset - startOffset, "Expected length " + expectedChars.length() + " does not match offset range " + startOffset + "-" + endOffset);
            Assumptions.assumeTrue(expectedChars.equals(source.substring(startOffset, endOffset)), "Expected characters " + expectedChars + " do not match source substring " + SOURCE.substring(startOffset, endOffset));

            var assertions = Stream.<Executable>builder();
            for (int offset = startOffset; offset <= endOffset; offset++) {
                int charOffset = offset;
                int expectedColumn = charOffset - startOffset + 1;
                int actualColumn = sliceIndex.columnNumberOf(charOffset);
                int actualLineNumber = sliceIndex.lineNumberOf(charOffset);
                assertions.add(() -> assertEquals(expectedLine, actualLineNumber, () -> "Char '" + visual(charAt(charOffset)) + "' at offset " + charOffset + " is on line " + expectedLine));
                assertions.add(() -> assertEquals(expectedColumn, actualColumn, () -> "Char '" + visual(charAt(charOffset)) + "' at offset " + charOffset + " is on column " + expectedColumn));
            }
            assertions.add(() -> assertEquals('\n', charAt(endOffset)));
            assertAll(assertions.build());
        }

        @Test
        @DisplayName("Invalid line numbers throw IndexOutOfBoundsException")
        void invalidLineNumber() {
            assertAll("LineIndex.Slice",
                    throwsExactlyIndexOutOfBoundsException(() -> sliceIndex.lineOffset(-1), "lineOffset(-1)"),
                    throwsExactlyIndexOutOfBoundsException(() -> sliceIndex.lineOffset(0), "lineOffset(-1)"),
                    throwsExactlyIndexOutOfBoundsException(() -> sliceIndex.lineOffset(4), "lineOffset(4)"));
        }

        private char charAt(int offset) {
            Objects.checkIndex(offset, source.length() + 1);
            if (offset < source.length())
                return source.charAt(offset);

            return '\0';
        }
    }

    @Nested
    class SimpleLineOffsetScannerTest {

        @Test void emptyInput() {
            assertArrayEquals(new int[]{0}, LineOffsetScanner.scanSimple(""));
        }

        @Test void singleLineInput() {
            assertArrayEquals(new int[]{0, 1}, LineOffsetScanner.scanSimple("a"));
        }

        @Test void multiLineInput() {
            assertArrayEquals(new int[]{0, 2, 4, 5}, LineOffsetScanner.scanSimple("a\nb\nc"));
        }
        @Test void emptyLinesInput() {
            assertArrayEquals(new int[]{0, 2, 3, 5, 6}, LineOffsetScanner.scanSimple("a\n\nb\nc"));
        }
        @Test void emptyLinesInputWithTerminalNewline() {
            assertArrayEquals(new int[]{0, 2, 3, 5, 7}, LineOffsetScanner.scanSimple("a\n\nb\nc\n"));
        }
    }

    private String visual(char c) {
        return switch (c) {
            case '\t' -> "␉";
            case '\n' -> "␊";
            case '\r' -> "␍";
            case '\0' -> "␀";
            default -> String.valueOf(c);
        };
    }
}
