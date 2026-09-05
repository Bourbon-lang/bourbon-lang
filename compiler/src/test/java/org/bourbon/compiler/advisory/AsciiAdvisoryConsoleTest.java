package org.bourbon.compiler.advisory;

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

import java.io.StringWriter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bourbon.compiler.advisory.AdvisoryConsole.Part;
import org.bourbon.compiler.diagnostic.code.DiagnosticCode;
import org.bourbon.compiler.diagnostic.code.InternalErrorCode;
import org.bourbon.compiler.diagnostic.code.SyntaxErrorCode;
import org.bourbon.compiler.effects.Effects;
import org.bourbon.compiler.effects.io.PrintWriter;
import org.bourbon.compiler.util.Records;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

public class AsciiAdvisoryConsoleTest {

    static List<Arguments.ArgumentSet> asciiPartsOutput = List.of(
            argumentSet(ERROR),
            argumentSet(Advisory.Severity.WARNING),
            argumentSet(Advisory.Severity.INFO),
            argumentSet(Advisory.Severity.NOTE),

            argumentSet(InternalErrorCode.CompilerBug),
            argumentSet(InternalErrorCode.IoError),
            argumentSet(InternalErrorCode.IoFileNotFound),

            argumentSet(SyntaxErrorCode.UnexpectedCharacter),
            argumentSet(SyntaxErrorCode.UnbalancedMultilineComment),
            argumentSet(SyntaxErrorCode.NumericLiteralError),

            argumentSet(parts(Pointer(5)), "   --> "),
            argumentSet(parts(Pointer(4)), "  --> "),
            argumentSet(parts(Pointer(3)), " --> "),
            argumentSet(parts(Pointer(2)), " --> "),
            argumentSet(parts(Pointer(1)), " --> "),
            argumentSet(parts(Pointer(0)), " --> "),

            argumentSet(parts(Gutter(5)), "     | "),
            argumentSet(parts(Gutter(5, 1)), "   1 | "),
            argumentSet(parts(Gutter(5, 12)), "  12 | "),
            argumentSet(parts(Gutter(5, 123)), " 123 | "),

            argumentSet(parts(Underline(0, 5)), "^^^^^ "),
            argumentSet(parts(Underline(1, 4)), " ^^^^ "),
            argumentSet(parts(Underline(2, 3)), "  ^^^ "),
            argumentSet(parts(Underline(3, 2)), "   ^^ "),
            argumentSet(parts(Underline(4, 1)), "    ^ "),
            argumentSet(parts(Underline(5, 0)), "     ^ "),

            // advisory header
            argumentSet(parts(Severity(ERROR), Code(InternalErrorCode.CompilerBug), Message("This is test message")), "error[INT0000]: This is test message"),
            argumentSet(parts(Pointer(4), Location("<test>", 12, 3)), "  --> <test>:12:3"),
            argumentSet(parts(Gutter(4, 12), Snippet("    value foo = 1.2M;")), " 12 |     value foo = 1.2M;"),
            argumentSet(parts(Gutter(4), Underline(10, 3)), "    |           ^^^ ")
    );


    @ParameterizedTest(name = "[index] {argumentSetName} should output {1}")
    @FieldSource
    void asciiPartsOutput(Part[] parts, String expectedOutput) {
        var actualOutput = withEffects(() -> AdvisoryConsole.println(parts));
        Assertions.assertEquals(expectedOutput + '\n', actualOutput);
    }


    @Test
    void completeAdvisoryOutput() {
        var expectedOutput = """
                error[SYN0003]: Multiple decimal points in numeric literal
                  --> <test>:12:4
                    |\s
                 12 | 1.2.3
                    | ^^^^^ Invalid decimal literal!
                    |\s
                 12 | 1.2.3
                    |    ^ Second decimal point '.' is not allowed
                    |\s
               
                """;

        var actualOutput = withEffects(() -> {
            AdvisoryConsole.println(Severity(ERROR), Code(SyntaxErrorCode.NumericLiteralError), Message("Multiple decimal points in numeric literal"));
            AdvisoryConsole.println(Pointer(4), Location("<test>", 12, 4));
            AdvisoryConsole.println(Gutter(4));
            AdvisoryConsole.println(Gutter(4, 12), Snippet("1.2.3"));
            AdvisoryConsole.println(Gutter(4), Underline(0, 5), Text("Invalid decimal literal!"));
            AdvisoryConsole.println(Gutter(4));
            AdvisoryConsole.println(Gutter(4, 12), Snippet("1.2.3"));
            AdvisoryConsole.println(Gutter(4), Underline(3, 1), Text("Second decimal point '.' is not allowed"));
            AdvisoryConsole.println(Gutter(4));
            AdvisoryConsole.println();
        });

        Assertions.assertEquals(expectedOutput, actualOutput);
    }

    private String withEffects(@NonNull Runnable runnable) {
        var writer = new StringWriter();
        Effects.handle(runnable)
                .with(AdvisoryConsole.Handler.class, new AsciiAdvisoryConsole())
                .with(PrintWriter.Handler.class, PrintWriter.of(new java.io.PrintWriter(writer)))
                .call();
        return writer.toString();
    }

    private static @NonNull Runnable getRunnable(Part[] parts) {
        return () -> AdvisoryConsole.println(parts);
    }

    private static Arguments.ArgumentSet argumentSet(Advisory.Severity severity) {
        var part = Severity(severity);
        return argumentSet(parts(part), severity.name().toLowerCase(Locale.ROOT));
    }

    private static Arguments.ArgumentSet argumentSet(DiagnosticCode code) {
        return argumentSet(parts(Code(code)), "[" + code.id() + "]");
    }

    private static Arguments.ArgumentSet argumentSet(Part[] parts, String expectedOutput) {
        var argumentSetName = Stream.of(parts).map(Records::format).collect(Collectors.joining(" + "));
        return Arguments.argumentSet(argumentSetName, parts, expectedOutput);
    }

    private static Part[] parts(Part... parts) {
        return parts;
    }

}
