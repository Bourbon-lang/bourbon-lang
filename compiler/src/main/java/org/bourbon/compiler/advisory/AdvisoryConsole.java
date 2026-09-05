package org.bourbon.compiler.advisory;

import org.bourbon.compiler.diagnostic.code.DiagnosticCode;
import org.bourbon.compiler.effects.Effect;
import org.bourbon.compiler.effects.Effects;
import org.jspecify.annotations.Nullable;

/// An algebraic effect that renders compiler advisories to textual, grid-based output media.
///
/// `AdvisoryConsole` serves as the drawing target for {@link AdvisoryLayout}. Handlers define
/// how abstract grid primitives (gutters, source lines, carets, annotations) are presented:
///
/// - **Interactive Terminals**: Enriched with ANSI colors, bold weights, and Unicode/NerdFont glyphs.
/// - **Headless & Test Environments**: Clean, unstyled ASCII/UTF-8 text matching test specifications.
/// - **In-Memory Buffers**: Captured string streams for compiler assertions or embedded REPLs.
@Effect
public final class AdvisoryConsole {
    private AdvisoryConsole() {
        throw new UnsupportedOperationException("Cannot instantiate " + AdvisoryConsole.class);
    }

    public sealed interface Part {

        static Part Severity(Advisory.Severity severity) { return new Severity(severity); }
        static Part Code(DiagnosticCode code) { return new Code(code); }
        static Part Message(CharSequence message) { return new Message(message); }

        static Part Pointer(int width) { return new Pointer(width); }
        static Part Location(CharSequence name, int line, int column) { return new Location(name, line, column); }

        static Part Gutter(int width) { return new Gutter(width); }
        static Part Gutter(int width, int lineNumber) { return new Gutter(width, lineNumber); }
        static Part Snippet(CharSequence sourceLine) { return new Snippet(sourceLine); }
        static Part Underline(int offset, int length) { return new Underline(offset, length, Advisory.Severity.NOTE); }
        static Part Underline(int offset, int length, Advisory.Severity severity) { return new Underline(offset, length, severity); }
        static Part Text(CharSequence text) { return new Text(text, Advisory.Severity.NOTE); }
        static Part Text(CharSequence text, Advisory.Severity severity) { return new Text(text, severity); }

        /// Advisory severity part
        record Severity(Advisory.Severity severity) implements Part {}

        /// Diagnostic code part
        record Code(DiagnosticCode code) implements Part {}

        /// Diagnostic message part
        record Message(CharSequence message) implements Part {}

        /// Pointer arrow part
        record Pointer(int width) implements Part {}

        /// Source location part (references source name/path with line and column of the primary annotation)
        record Location(CharSequence name, int line, int column) implements Part {}

        /// Source code snippet part
        record Snippet(CharSequence sourceLine) implements Part {}

        /// Gutter at the start of the source code block
        record Gutter(int width, @Nullable Integer lineNumber) implements Part {
            public Gutter(int width) { this(width, null); }
        }

        /// Underline a span of text in the preceding source line.
        record Underline(int offset, int width, Advisory.Severity severity) implements Part {}

        /// Text to be displayed styled at the specified severity level
        record Text(CharSequence text, Advisory.Severity severity) implements Part {}
    }

    @Effect.Handler
    public interface Handler {
        void printNewLine();

        void println(Part... parts);
    }

    public static void printNewLine() {
        Effects.get(AdvisoryConsole.Handler.class).printNewLine();
    }

    public static void println(Part... parts) {
        Effects.get(AdvisoryConsole.Handler.class).println(parts);
    }
}
