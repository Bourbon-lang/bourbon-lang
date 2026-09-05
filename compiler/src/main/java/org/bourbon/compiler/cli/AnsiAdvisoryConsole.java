package org.bourbon.compiler.cli;

import org.bourbon.compiler.advisory.Advisory;
import org.bourbon.compiler.advisory.Advisory.Severity;
import org.bourbon.compiler.advisory.AdvisoryConsole;
import org.bourbon.compiler.advisory.AdvisoryConsole.Part;
import org.bourbon.compiler.advisory.AdvisoryConsole.Part.Code;
import org.bourbon.compiler.advisory.AdvisoryConsole.Part.Gutter;
import org.bourbon.compiler.advisory.AdvisoryConsole.Part.Location;
import org.bourbon.compiler.advisory.AdvisoryConsole.Part.Message;
import org.bourbon.compiler.advisory.AdvisoryConsole.Part.Pointer;
import org.bourbon.compiler.advisory.AdvisoryConsole.Part.Snippet;
import org.bourbon.compiler.advisory.AdvisoryConsole.Part.Text;
import org.bourbon.compiler.advisory.AdvisoryConsole.Part.Underline;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jspecify.annotations.Nullable;

public record AnsiAdvisoryConsole(Style style) implements AdvisoryConsole.Handler {

    public enum Style {
        NerdFont {
            @Override String getSymbol(Advisory.Severity severity) {
                return switch (severity) {
                    case ERROR -> "\uF057 ";
                    case WARNING -> "\uF071 ";
                    case INFO, NOTE -> "\uF05A ";
                };
            }
        },

        Unicode {
            @Override String getSymbol(Advisory.Severity severity) {
                return switch (severity) {
                    case ERROR -> "✗ ";
                    case WARNING -> "⚠ ";
                    case INFO, NOTE -> "ℹ ";
                };
            }
        },

        ASCII {
            @Override String getSymbol(Advisory.Severity severity) {
                return "";
            }
        };

        abstract String getSymbol(Advisory.Severity severity);

        AttributedStyle getSeverityStyle(Advisory.Severity severity) {
            return switch (severity) {
                case ERROR -> boldRed();
                case WARNING -> boldYellow();
                case NOTE -> boldGreen();
                case INFO -> boldCyan();
            };
        }
    }

    @Override public void printNewLine() {
        Terminal.println();
    }

    @Override public void println(Part... parts) {
        if (parts.length == 0) {
            printNewLine();
            return;
        }

        var sb = new AttributedStringBuilder();
        for (var part : parts) {
            render(sb, part);
        }

        Terminal.println(sb.style(AttributedStyle.DEFAULT).toAttributedString());
    }

    private void render(AttributedStringBuilder sb, Part part) {
        switch (part) {
            case Part.Severity(var severity) ->
                    sb.style(style(severity)).append(symbol(severity)).append(severity.name().toLowerCase());
            case Code(var code) ->
                    sb.style(bold()).append("[").append(code.id()).append("]");
            case Message(var message) ->
                    sb.style(boldWhite()).append(": ").append(message);
            case Pointer(var width) ->
                    sb.style(boldBlue()).append(" ".repeat(Math.max(1, width - 2))).append("--> ");
            case Location(var name, int line, int column) ->
                    sb.style(AttributedStyle.DEFAULT).append(name)
                            .append(":").append(String.valueOf(line))
                            .append(":").append(String.valueOf(column));
            case Gutter(var w, var line) when line == null ->
                    sb.style(boldBlue()).append(" ".repeat(w)).append("| ");
            case Gutter(var w, var lineNumber) ->
                    sb.style(boldBlue())
                            .append(" ".repeat(Math.max(1, w - 2 - (int)Math.floor(Math.log10(lineNumber)))))
                            .append(String.valueOf(lineNumber)).append(" | ");
            case Snippet(var sourceLine) ->
                    sb.style(AttributedStyle.DEFAULT).append(sourceLine);
            case Underline(int offset, int width, var severity) ->
                    sb.style(AttributedStyle.DEFAULT).append(" ".repeat(Math.max(0, offset)))
                            .style(style(severity)).append("^".repeat(Math.max(1, width)))
                            .style(AttributedStyle.DEFAULT).append(" ");
            case Text(var text, var severity) ->
                    sb.style(style(severity)).append(text);
        }
    }

    private AttributedStyle style(Severity severity) {
        return style.getSeverityStyle(severity);
    }

    private String symbol(Severity severity) {
        return style.getSymbol(severity);
    }


    private static AttributedStyle boldCyan() {
        return AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.CYAN);
    }

    private static AttributedStyle boldGreen() {
        return AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.GREEN);
    }

    private static AttributedStyle boldYellow() {
        return AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.YELLOW);
    }

    private static AttributedStyle boldRed() {
        return AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.RED);
    }

    private static AttributedStyle boldBlue() {
        return AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.BLUE);
    }

    private static @Nullable AttributedStyle boldWhite() {
        return AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.WHITE);
    }

    private static AttributedStyle bold() {
        return AttributedStyle.DEFAULT.bold();
    }
}
