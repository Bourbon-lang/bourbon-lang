package org.bourbon.compiler.cli;

import static org.jline.terminal.Terminal.TYPE_DUMB;

import org.bourbon.compiler.effects.Effect;
import org.bourbon.compiler.effects.Effects;
import org.bourbon.compiler.effects.io.PrintWriter;
import org.jline.utils.AttributedCharSequence;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

/// An algebraic effect representing an interactive or virtual terminal display.
///
/// Unlike {@link PrintWriter}, which represents an unstyled character streaming sink,
/// `Terminal` provides styled output operations accepting {@link AttributedCharSequence}
/// and exposes environment geometry (width, height) and color capabilities.
@Effect
public final class Terminal {

    /// Default virtual terminal width in columns.
    public static final int DEFAULT_WIDTH = 80;

    /// Default virtual terminal height in rows.
    public static final int DEFAULT_HEIGHT = 24;

    private Terminal() {
        // Static utility and effect definition class
    }

    /// The effect handler interface for terminal output operations and environment queries.
    @Effect.Handler
    public interface Handler {

        /// Returns the terminal width in columns.
        ///
        /// @return the display width in columns
        int width();

        /// Returns the terminal height in rows.
        ///
        /// @return the display height in rows
        int height();

        /// Returns whether the terminal supports styled color output.
        ///
        /// @return `true` if colors are supported, `false` otherwise
        boolean hasColor();

        /// Emits an attributed character sequence without terminating the line.
        ///
        /// @param message the styled character sequence to print
        void print(AttributedCharSequence message);

        /// Emits an attributed character sequence followed by a line separator.
        ///
        /// @param message the styled character sequence to print
        default void println(AttributedCharSequence message) {
            print(message);
            println();
        }

        /// Terminates the current line.
        void println();

        /// Emits an unstyled character sequence using {@link AttributedStyle#DEFAULT}.
        ///
        /// @param message the plain character sequence to print
        default void print(CharSequence message) {
            print(new AttributedString(message, AttributedStyle.DEFAULT));
        }

        /// Emits an unstyled character sequence using {@link AttributedStyle#DEFAULT} followed by a line separator.
        ///
        /// @param message the plain character sequence to print
        default void println(CharSequence message) {
            print(message);
            println();
        }
    }

    /// Returns the display width in columns from the ambient {@link Terminal.Handler}.
    ///
    /// @return the terminal width in columns
    public static int width() {
        return Effects.get(Handler.class).width();
    }

    /// Returns the display height in rows from the ambient {@link Terminal.Handler}.
    ///
    /// @return the terminal height in rows
    public static int height() {
        return Effects.get(Handler.class).height();
    }

    /// Returns whether the ambient {@link Terminal.Handler} supports color output.
    ///
    /// @return `true` if colors are supported, `false` otherwise
    public static boolean hasColor() {
        return Effects.get(Handler.class).hasColor();
    }

    /// Emits an attributed character sequence to the ambient {@link Terminal.Handler}.
    ///
    /// @param message the styled character sequence to print
    public static void print(AttributedCharSequence message) {
        Effects.get(Handler.class).print(message);
    }

    /// Emits an attributed character sequence and terminates the line on the ambient {@link Terminal.Handler}.
    ///
    /// @param message the styled character sequence to print
    public static void println(AttributedCharSequence message) {
        Effects.get(Handler.class).println(message);
    }

    /// Terminates the current line on the ambient {@link Terminal.Handler}.
    public static void println() {
        Effects.get(Handler.class).println();
    }

    /// Emits an unstyled character sequence to the ambient {@link Terminal.Handler}.
    ///
    /// @param message the plain character sequence to print
    public static void print(CharSequence message) {
        Effects.get(Handler.class).print(message);
    }

    /// Emits an unstyled character sequence and terminates the line on the ambient {@link Terminal.Handler}.
    ///
    /// @param message the plain character sequence to print
    public static void println(CharSequence message) {
        Effects.get(Handler.class).println(message);
    }

    /// Creates a virtual terminal handler that redirects output to the ambient {@link PrintWriter} effect.
    ///
    /// @param width the virtual terminal width in columns
    /// @param height the virtual terminal height in rows
    /// @param hasColor whether to emit ANSI escape sequences or strip formatting to plain text
    /// @return a {@link Terminal.Handler} delegating to {@link PrintWriter}
    public static Handler virtual(int width, int height, boolean hasColor) {
        return new VirtualTerminal(width, height, hasColor);
    }

    /// Creates a virtual terminal handler with default 80x24 geometry that redirects output
    /// to the ambient {@link PrintWriter} effect.
    ///
    /// @param hasColor whether to emit ANSI escape sequences or strip formatting to plain text
    /// @return a {@link Terminal.Handler} delegating to {@link PrintWriter}
    public static Handler virtual(boolean hasColor) {
        return virtual(DEFAULT_WIDTH, DEFAULT_HEIGHT, hasColor);
    }

    /// Creates a terminal handler backed by an interactive JLine {@link org.jline.terminal.Terminal}.
    ///
    /// @param terminal the underlying JLine terminal instance
    /// @return a {@link Terminal.Handler} delegating to {@code terminal}
    public static Handler of(org.jline.terminal.Terminal terminal) {
        return new JLineTerminal(terminal);
    }

    /// Virtual terminal implementation delegating character emission to the ambient {@link PrintWriter} effect.
    record VirtualTerminal(int width, int height, boolean hasColor) implements Handler {

        @Override
        public void print(AttributedCharSequence message) {
            PrintWriter.print(hasColor ? message.toAnsi() : message.toString());
        }

        @Override
        public void println(AttributedCharSequence message) {
            PrintWriter.println(hasColor ? message.toAnsi() : message.toString());
        }

        @Override
        public void println() {
            PrintWriter.println();
        }
    }

    /// Terminal implementation wrapping a JLine {@link org.jline.terminal.Terminal}.
    record JLineTerminal(org.jline.terminal.Terminal terminal) implements Handler {

        @Override
        public int width() {
            return terminal.getColumns();
        }

        @Override
        public int height() {
            return terminal.getRows();
        }

        @Override
        public boolean hasColor() {
            return !TYPE_DUMB.equals(terminal.getType());
        }

        @Override
        public void print(AttributedCharSequence message) {
            terminal.writer().print(message.toAnsi(terminal));
        }

        @Override
        public void println(AttributedCharSequence message) {
            terminal.writer().println(message.toAnsi(terminal));
        }

        @Override
        public void println() {
            terminal.writer().println();
        }
    }
}
