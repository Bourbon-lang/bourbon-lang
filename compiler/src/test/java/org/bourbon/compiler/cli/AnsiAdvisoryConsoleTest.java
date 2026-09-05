package org.bourbon.compiler.cli;

import static org.bourbon.compiler.advisory.Advisory.Severity.ERROR;
import static org.bourbon.compiler.advisory.AdvisoryConsole.Part.Code;
import static org.bourbon.compiler.advisory.AdvisoryConsole.Part.Gutter;
import static org.bourbon.compiler.advisory.AdvisoryConsole.Part.Location;
import static org.bourbon.compiler.advisory.AdvisoryConsole.Part.Message;
import static org.bourbon.compiler.advisory.AdvisoryConsole.Part.Pointer;
import static org.bourbon.compiler.advisory.AdvisoryConsole.Part.Severity;
import static org.bourbon.compiler.advisory.AdvisoryConsole.Part.Snippet;
import static org.bourbon.compiler.advisory.AdvisoryConsole.Part.Text;
import static org.bourbon.compiler.advisory.AdvisoryConsole.Part.Underline;
import static org.bourbon.compiler.junit.CompilerAssertions.assertAnsiEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringWriter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bourbon.compiler.advisory.Advisory;
import org.bourbon.compiler.advisory.AdvisoryConsole;
import org.bourbon.compiler.advisory.AdvisoryConsole.Part;
import org.bourbon.compiler.advisory.AsciiAdvisoryConsole;
import org.bourbon.compiler.diagnostic.code.InternalErrorCode;
import org.bourbon.compiler.diagnostic.code.SyntaxErrorCode;
import org.bourbon.compiler.effects.Effects;
import org.bourbon.compiler.effects.io.PrintWriter;
import org.bourbon.compiler.util.Records;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

public class AnsiAdvisoryConsoleTest {

    // ANSI Escape Code Constants matching JLine's standard ECMA-48 output
    private static final String RESET = "\u001B[m";
    private static final String BOLD = "\u001B[1m";
    private static final String BOLD_RED = "\u001B[1;31m";
    private static final String BOLD_YELLOW = "\u001B[1;33m";
    private static final String BOLD_GREEN = "\u001B[1;32m";
    private static final String BOLD_CYAN = "\u001B[1;36m";
    private static final String BOLD_BLUE = "\u001B[1;34m";
    private static final String BOLD_WHITE = "\u001B[1;37m";

    static List<Arguments.ArgumentSet> ansiPartsOutput = List.of(
            // Severities with ASCII style: name in bold color (no prefix symbol)
            argumentSet(parts(Severity(ERROR)), BOLD_RED + "error" + RESET),
            argumentSet(parts(Severity(Advisory.Severity.WARNING)), BOLD_YELLOW + "warning" + RESET),
            argumentSet(parts(Severity(Advisory.Severity.INFO)), BOLD_CYAN + "info" + RESET),
            argumentSet(parts(Severity(Advisory.Severity.NOTE)), BOLD_GREEN + "note" + RESET),

            // Diagnostic Codes: bold brackets and code
            argumentSet(parts(Code(InternalErrorCode.CompilerBug)), BOLD + "[INT0000]" + RESET),
            argumentSet(parts(Code(InternalErrorCode.IoError)), BOLD + "[INT0001]" + RESET),
            argumentSet(parts(Code(InternalErrorCode.IoFileNotFound)), BOLD + "[INT0002]" + RESET),
            argumentSet(parts(Code(SyntaxErrorCode.UnexpectedCharacter)), BOLD + "[SYN0001]" + RESET),
            argumentSet(parts(Code(SyntaxErrorCode.UnbalancedMultilineComment)), BOLD + "[SYN0002]" + RESET),
            argumentSet(parts(Code(SyntaxErrorCode.NumericLiteralError)), BOLD + "[SYN0003]" + RESET),

            // Pointer: bold blue arrow with leading spaces
            argumentSet(parts(Pointer(5)), BOLD_BLUE + "   --> " + RESET),
            argumentSet(parts(Pointer(4)), BOLD_BLUE + "  --> " + RESET),
            argumentSet(parts(Pointer(3)), BOLD_BLUE + " --> " + RESET),
            argumentSet(parts(Pointer(2)), BOLD_BLUE + " --> " + RESET),
            argumentSet(parts(Pointer(1)), BOLD_BLUE + " --> " + RESET),
            argumentSet(parts(Pointer(0)), BOLD_BLUE + " --> " + RESET),

            // Location: default unstyled text
            argumentSet(parts(Location("<test>", 12, 3)), "<test>:12:3"),

            // Gutter: empty gutter (bold blue spaces + pipe)
            argumentSet(parts(Gutter(5)), BOLD_BLUE + "     | " + RESET),

            // Gutter: with line number (bold blue padded number + pipe)
            argumentSet(parts(Gutter(5, 1)), BOLD_BLUE + "   1 | " + RESET),
            argumentSet(parts(Gutter(5, 12)), BOLD_BLUE + "  12 | " + RESET),
            argumentSet(parts(Gutter(5, 123)), BOLD_BLUE + " 123 | " + RESET),

            // Snippet: unstyled source line
            argumentSet(parts(Snippet("    value foo = 1.2M;")), "    value foo = 1.2M;"),

            // Underline: spaces + carets styled with severity color, followed by space before message
            argumentSet(parts(Underline(0, 5, ERROR)), BOLD_RED + "^^^^^" + RESET + " "),
            argumentSet(parts(Underline(1, 4, ERROR)), " " + BOLD_RED + "^^^^" + RESET + " "),
            argumentSet(parts(Underline(2, 3, ERROR)), "  " + BOLD_RED + "^^^" + RESET + " "),
            argumentSet(parts(Underline(3, 2, ERROR)), "   " + BOLD_RED + "^^" + RESET + " "),
            argumentSet(parts(Underline(4, 1, ERROR)), "    " + BOLD_RED + "^" + RESET + " "),
            argumentSet(parts(Underline(5, 0, ERROR)), "     " + BOLD_RED + "^" + RESET + " "),

            // Underline with other severities
            argumentSet(parts(Underline(0, 3, Advisory.Severity.WARNING)), BOLD_YELLOW + "^^^" + RESET + " "),
            argumentSet(parts(Underline(0, 3, Advisory.Severity.NOTE)), BOLD_GREEN + "^^^" + RESET + " "),
            argumentSet(parts(Underline(0, 3, Advisory.Severity.INFO)), BOLD_CYAN + "^^^" + RESET + " "),

            // Text: styled with severity
            argumentSet(parts(Text("Invalid decimal literal!", ERROR)), BOLD_RED + "Invalid decimal literal!" + RESET),
            argumentSet(parts(Text("Consider removing this", Advisory.Severity.NOTE)), BOLD_GREEN + "Consider removing this" + RESET),

            // Advisory header compound line
            argumentSet(
                    parts(Severity(ERROR), Code(InternalErrorCode.CompilerBug), Message("This is test message")),
                    BOLD_RED + "error" + RESET + BOLD + "[INT0000]" + BOLD_WHITE + ": This is test message" + RESET
            ),

            // Pointer + Location compound line
            argumentSet(
                    parts(Pointer(4), Location("<test>", 12, 3)),
                    BOLD_BLUE + "  --> " + RESET + "<test>:12:3"
            ),

            // Gutter + Snippet compound line
            argumentSet(
                    parts(Gutter(4, 12), Snippet("    value foo = 1.2M;")),
                    BOLD_BLUE + " 12 | " + RESET + "    value foo = 1.2M;"
            ),

            // Gutter + Underline compound line
            argumentSet(
                    parts(Gutter(4), Underline(10, 3, ERROR)),
                    BOLD_BLUE + "    | " + RESET + "          " + BOLD_RED + "^^^" + RESET + " "
            ),

            // Gutter + Underline + Text annotation line
            argumentSet(
                    parts(Gutter(4), Underline(10, 3, ERROR), Text("test annotation", ERROR)),
                    BOLD_BLUE + "    | " + RESET + "          " + BOLD_RED + "^^^" + RESET + " " + BOLD_RED + "test annotation" + RESET
            )
    );

    @ParameterizedTest(name = "[index] {argumentSetName} should output {1}")
    @FieldSource
    void ansiPartsOutput(Part[] parts, String expectedOutput) {
        var actualOutput = withEffects(AnsiAdvisoryConsole.Style.ASCII, () -> AdvisoryConsole.println(parts));
        assertAnsiEquals(expectedOutput + '\n', actualOutput);
    }

    @Test
    void unicodeStyleSeverity() {
        var output = withEffects(AnsiAdvisoryConsole.Style.Unicode, () -> AdvisoryConsole.println(Severity(ERROR)));
        assertAnsiEquals(BOLD_RED + "✗ error" + RESET + '\n', output);
    }

    @Test
    void nerdFontStyleSeverity() {
        var output = withEffects(AnsiAdvisoryConsole.Style.NerdFont, () -> AdvisoryConsole.println(Severity(ERROR)));
        assertAnsiEquals(BOLD_RED + "\uF057 error" + RESET + '\n', output);
    }

    @Test
    void emptyPrintlnShouldProduceSingleNewline() {
        var output = withEffects(AnsiAdvisoryConsole.Style.ASCII, AdvisoryConsole::println);
        assertEquals("\n", output);
    }

    @Test
    void completeAdvisoryOutput() {
        var expectedOutput =
                BOLD_RED + "error" + RESET + BOLD + "[SYN0003]" + BOLD_WHITE + ": Multiple decimal points in numeric literal" + RESET + "\n" +
                BOLD_BLUE + "  --> " + RESET + "<test>:12:4\n" +
                BOLD_BLUE + "    | " + RESET + "\n" +
                BOLD_BLUE + " 12 | " + RESET + "1.2.3\n" +
                BOLD_BLUE + "    | " + RESET + BOLD_RED + "^^^^^" + RESET + " " + BOLD_RED + "Invalid decimal literal!" + RESET + "\n" +
                BOLD_BLUE + "    | " + RESET + "\n" +
                BOLD_BLUE + " 12 | " + RESET + "1.2.3\n" +
                BOLD_BLUE + "    | " + RESET + "   " + BOLD_RED + "^" + RESET + " " + BOLD_RED + "Second decimal point '.' is not allowed" + RESET + "\n" +
                BOLD_BLUE + "    | " + RESET + "\n" +
                "\n";

        var actualOutput = withEffects(AnsiAdvisoryConsole.Style.ASCII, () -> {
            AdvisoryConsole.println(Severity(ERROR), Code(SyntaxErrorCode.NumericLiteralError), Message("Multiple decimal points in numeric literal"));
            AdvisoryConsole.println(Pointer(4), Location("<test>", 12, 4));
            AdvisoryConsole.println(Gutter(4));
            AdvisoryConsole.println(Gutter(4, 12), Snippet("1.2.3"));
            AdvisoryConsole.println(Gutter(4), Underline(0, 5, ERROR), Text("Invalid decimal literal!", ERROR));
            AdvisoryConsole.println(Gutter(4));
            AdvisoryConsole.println(Gutter(4, 12), Snippet("1.2.3"));
            AdvisoryConsole.println(Gutter(4), Underline(3, 1, ERROR), Text("Second decimal point '.' is not allowed", ERROR));
            AdvisoryConsole.println(Gutter(4));
            AdvisoryConsole.println();
        });

        assertAnsiEquals(expectedOutput, actualOutput);
    }

    @Test
    void disabledColorStripsAnsiSequences() {
        var output = withEffects(AnsiAdvisoryConsole.Style.ASCII, false, () -> AdvisoryConsole.println(Severity(ERROR)));
        assertEquals("error\n", output);
    }

    @Test
    void pureTextOutputMatchesAsciiConsole() {
        Runnable render = () -> {
            AdvisoryConsole.println(Severity(ERROR), Code(SyntaxErrorCode.NumericLiteralError), Message("Multiple decimal points in numeric literal"));
            AdvisoryConsole.println(Pointer(4), Location("<test>", 12, 4));
            AdvisoryConsole.println(Gutter(4));
            AdvisoryConsole.println(Gutter(4, 12), Snippet("1.2.3"));
            AdvisoryConsole.println(Gutter(4), Underline(0, 5, ERROR), Text("Invalid decimal literal!", ERROR));
            AdvisoryConsole.println(Gutter(4));
            AdvisoryConsole.println(Gutter(4, 12), Snippet("1.2.3"));
            AdvisoryConsole.println(Gutter(4), Underline(3, 1, ERROR), Text("Second decimal point '.' is not allowed", ERROR));
            AdvisoryConsole.println(Gutter(4));
            AdvisoryConsole.println();
        };

        var asciiOutput = withAsciiEffects(render);
        var ansiFilteredOutput = stripAnsi(withEffects(AnsiAdvisoryConsole.Style.ASCII, true, render));
        var ansiNoColorOutput = withEffects(AnsiAdvisoryConsole.Style.ASCII, false, render);

        assertAll("Pure text output",
                () -> assertEquals(asciiOutput, ansiNoColorOutput, "no-color terminal"),
                () -> assertEquals(asciiOutput, ansiFilteredOutput, "ansi-filtered")
        );
    }

    private static String stripAnsi(String input) {
        return input.replaceAll("\u001B\\[[;?0-9]*[a-zA-Z]", "");
    }

    private String withAsciiEffects(@NonNull Runnable runnable) {
        var writer = new StringWriter();
        Effects.handle(runnable)
                .with(AdvisoryConsole.Handler.class, new AsciiAdvisoryConsole())
                .with(PrintWriter.Handler.class, PrintWriter.of(new java.io.PrintWriter(writer)))
                .call();
        return writer.toString();
    }

    private String withEffects(AnsiAdvisoryConsole.Style style, @NonNull Runnable runnable) {
        return withEffects(style, true, runnable);
    }

    private String withEffects(AnsiAdvisoryConsole.Style style, boolean hasColor, @NonNull Runnable runnable) {
        var writer = new StringWriter();
        Effects.handle(runnable)
                .with(AdvisoryConsole.Handler.class, new AnsiAdvisoryConsole(style))
                .with(Terminal.Handler.class, Terminal.virtual(hasColor))
                .with(PrintWriter.Handler.class, PrintWriter.of(new java.io.PrintWriter(writer)))
                .call();
        return writer.toString();
    }

    private static Arguments.ArgumentSet argumentSet(Part[] parts, String expectedOutput) {
        var argumentSetName = Stream.of(parts).map(Records::format).collect(Collectors.joining(" + "));
        return Arguments.argumentSet(argumentSetName, parts, expectedOutput);
    }

    private static Part[] parts(Part... parts) {
        return parts;
    }
}
