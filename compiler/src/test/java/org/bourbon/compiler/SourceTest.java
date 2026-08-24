package org.bourbon.compiler;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.random.RandomGenerator;
import java.util.stream.Stream;

import org.bourbon.compiler.SourceSpan.SourceName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.SAME_THREAD)
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class SourceTest {

    public static final String SOURCE = """
            1234567890
            abcdefghijklmnopqrstuvwxyz
            """;
    public static final String SOURCE_NAME = "<org.bourbon.compiler.SourceTest.SOURCE>";

    @TestFactory
    Stream<DynamicTest> sourceOperations() {
        Source source = Source.named(SOURCE_NAME).of(SOURCE);
        return Stream.of(
                dynamicTest("Step 0: Initial source state", () -> {
                    assertAll("Source",
                            () -> assertEquals(SOURCE.length(), source.length(), "length()"),
                            () -> assertEquals(0, source.start(), "start()"),
                            () -> assertEquals(0, source.current(), "current()"),
                            () -> assertEquals(1, source.currentLine(), "currentLine()"),
                            () -> assertEquals(1, source.column(), "column()"),
                            () -> assertEquals('1', source.peek(), "peek()"),
                            () -> assertEquals('2', source.peekNext(), "peekNext()"),
                            () -> assertEquals("", source.lexeme(), "lexeme()"),
                            () -> assertEquals(span(1, 1, 0, 0), source.currentSpan(), "currentSpan()"),
                            () -> assertFalse(source.isAtEnd(), "isAtEnd()")
                    );
                }),
                dynamicTest("Step 1: Advancing source cursor", () -> {
                    var span = source.currentSpan();
                    assertAll("Source",
                            () -> assertEquals('1', source.advance(), "advance()"),
                            () -> assertFalse(source.peek('1'), "peek('1')"),
                            () -> assertFalse(source.match('1'), "match('1')"),
                            () -> assertEquals(span.withLength(length -> length + 1), source.currentSpan(), "currentSpan()"),
                            () -> assertEquals("1", source.lexeme(), "lexeme()"));
                }),
                dynamicTest("Step 2: Match on next character :'2'", () -> {
                    var span = source.currentSpan();
                    assertAll("Source",
                            () -> assertEquals('2', source.peek(), "peek()"),
                            () -> assertTrue(source.peek('2'), "peek('2')"),
                            () -> assertTrue(source.match('2'), "match('2')"),
                            () -> assertEquals(span.withLength(length -> length + 1), source.currentSpan(), "currentSpan()"),
                            () -> assertEquals("12", source.lexeme(), "lexeme()"));
                }),
                dynamicTest("Step 3: Use tokenStart() to start lexer at a new position", () -> {
                    source.tokenStart();
                    assertAll("Source",
                            () -> assertEquals(span(1, 3, 2, 0), source.currentSpan(), "currentSpan()"),
                            () -> assertEquals("", source.lexeme(), "lexeme()"));
                }),
                dynamicTest("Step 4: Advance once: '3'", () -> {
                    assertAll("Source",
                            () -> assertEquals('3', source.advance(), "advance()"),
                            () -> assertEquals(span(1, 3, 2, 1), source.currentSpan(), "currentSpan()"),
                            () -> assertEquals("3", source.lexeme(), "lexeme()"));
                }),
                dynamicTest("Step 5: Peek and match sequence of chars: \"456789\"", () -> {
                    var span = source.currentSpan();
                    assertAll("Source",
                            () -> assertTrue(source.peek("456789"), "peek('456789')"),
                            () -> assertEquals(span, source.currentSpan(), "peek('4567890'); currentSpan()"),
                            () -> assertTrue(source.match("4567890"), "match('456789')"),
                            () -> assertEquals(span.withLength(length -> length + 7), source.currentSpan(), "currentSpan()"),
                            () -> assertEquals("34567890", source.lexeme(), "lexeme()"));
                }),
                dynamicTest("Step 6: Advance past newline", () -> {
                    source.tokenStart();
                    var span = source.currentSpan();
                    assertAll("Source",
                            () -> assertEquals('\n', source.advance(), "advance()"),
                            () -> assertEquals(span.withLength(length -> length + 1), source.currentSpan(), "currentSpan()"),
                            () -> assertEquals("\n", source.lexeme(), "lexeme()"),
                            () -> consume(source),
                            () -> assertEquals(span(2, 1, 11, 0), source.currentSpan(), "currentSpan()"));
                }),
                dynamicTest("Step 7: Use resetToken() to backtrack", () -> {
                    var span = source.currentSpan();
                    assertAll("Source",
                            () -> assertEquals('a', source.advance(), "advance()"),
                            () -> assertEquals(span.withLength(_ -> 1), source.currentSpan(), "currentSpan()"),
                            () -> assertEquals("a", source.lexeme(), "lexeme()"),
                            () -> assertTrue(source.match("bcdefghijklmnopqrstuvwxyz"), "match('bcd..xyz')"),
                            () -> backtrack(source),
                            () -> assertEquals(span, source.currentSpan(), "currentSpan()"),
                            () -> assertEquals("", source.lexeme(), "lexeme()"));
                }),
                dynamicTest("Step 8: Backtrack past line break", () -> {
                    var span = source.currentSpan();
                    assertAll("Source",
                            () -> assertEquals(2, span.line(), "currentSpan().line()"),
                            () -> assertTrue(source.match("abcdefghijklmnopqrstuvwxyz"), "source.match(\"a..z\")"),
                            () -> assertEquals('\n', source.advance(), "advance()"),
                            () -> assertEquals(span.withLength(_ -> 27), source.currentSpan(), "currentSpan()"),
                            () -> backtrack(source),
                            () -> assertEquals(span, source.currentSpan(), "backtrack(); currentSpan()"));
                }),
                dynamicTest("Step 9: Advance past newline again", () -> {
                    var span = source.currentSpan();
                    assertAll("Source",
                            () -> assertEquals(2, span.line(), "currentSpan().line()"),
                            () -> assertTrue(source.match("abcdefghijklmnopqrstuvwxyz\n"), "match(\"a..z\\n\")"),
                            () -> assertEquals(span.withLength(_ -> 27), source.currentSpan(), "currentSpan()"),
                            () -> assertEquals("abcdefghijklmnopqrstuvwxyz\n", source.lexeme(), "lexeme()"),
                            () -> consume(source),
                            () -> assertEquals(3, source.currentSpan().line(), "currentSpan().line()"));
                }),
                dynamicTest("Step 10: Going past end of input", () -> {
                    var span = source.currentSpan();
                    assertAll("Source",
                            () -> assertEquals(3, span.line(), "currentSpan().line()"),
                            () -> assertEquals('\0', source.advance(), "advance()"),
                            () -> assertFalse(source.match(randomCharacter()), "match(?)"),
                            () -> assertEquals(span, source.currentSpan(), "currentSpan()"),
                            () -> assertEquals("", source.lexeme(), "lexeme()"));
                })
        );
    }

    private char randomCharacter() {
        return (char) RandomGenerator.getDefault().nextInt(' ', 'z');
    }

    private static void backtrack(Source source) {
        source.tokenReset();
    }

    private static void consume(Source source) {
        source.tokenStart();
    }

    private SourceSpan span(int line, int column, int startOffset, int length) {
        return new SourceSpan(SourceName.of(SOURCE_NAME), line, column, startOffset, length);
    }
}
