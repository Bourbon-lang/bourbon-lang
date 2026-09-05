package org.bourbon.compiler.advisory;

import java.util.Locale;

import org.bourbon.compiler.advisory.Advisory.Severity;
import org.bourbon.compiler.diagnostic.code.DiagnosticCode;
import org.bourbon.compiler.effects.io.PrintWriter;

public record AsciiAdvisoryConsole() implements AdvisoryConsole.Handler {

    @Override public void printNewLine() {
        PrintWriter.println();
    }

    @Override public void println(AdvisoryConsole.Part... parts) {
        for (AdvisoryConsole.Part part : parts) {
            switch (part) {
                case AdvisoryConsole.Part.Severity(var severity) -> PrintWriter.print(severity.name().toLowerCase(Locale.ROOT));
                case AdvisoryConsole.Part.Code(var code) -> PrintWriter.printf("[%s]", code.id());
                case AdvisoryConsole.Part.Message(var message) -> PrintWriter.printf(": %s".formatted(message));
                case AdvisoryConsole.Part.Pointer(int width) -> PrintWriter.printf("%" + Math.max(1, width - 2) + "s--> ", "");
                case AdvisoryConsole.Part.Location(var name, int line, int column) -> PrintWriter.printf("%s:%d:%d",  name, line, column);
                case AdvisoryConsole.Part.Gutter(var w, var line) when line == null -> PrintWriter.printf("%" + w + "s| ", "");
                case AdvisoryConsole.Part.Gutter(var w, var line) -> PrintWriter.printf("%" + Math.max(1, w - 1) + "d | ", line);
                case AdvisoryConsole.Part.Snippet(var content) -> PrintWriter.print(content);
                case AdvisoryConsole.Part.Underline(int offset, int width, var _) -> {
                    var indent = offset > 0 ? " ".repeat(offset) : "";
                    var underline = "^".repeat(Math.max(1, width));
                    PrintWriter.printf("%s%s ", indent, underline);
                }
                case AdvisoryConsole.Part.Text(var text, var _) -> PrintWriter.print(text);
            }
        }
        PrintWriter.println();
    }

}
