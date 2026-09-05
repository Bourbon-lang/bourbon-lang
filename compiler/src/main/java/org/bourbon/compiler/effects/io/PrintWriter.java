package org.bourbon.compiler.effects.io;

import java.util.Locale;

import org.bourbon.compiler.advisory.AdvisoryConsole;
import org.bourbon.compiler.effects.Effect;
import org.bourbon.compiler.effects.Effects;

/// An algebraic effect for character-level text output streaming.
///
/// `PrintWriter` serves as the foundational I/O sink capability in the compiler effect system.
/// Higher-level presentation effects—such as {@link AdvisoryConsole}—perform
/// `PrintWriter` to emit their styled character streams without coupling themselves to concrete
/// physical streams or destinations.
///
/// ## Purpose and Decoupling
/// Unlike {@link java.io.PrintWriter}, this type is not a concrete stream wrapper, but an ambient
/// effect capability. Decoupling character emission from concrete destinations allows:
///
/// - **Interactive CLI / REPL**: Handlers sink characters directly to `System.err`, `System.out`, or a TTY.
/// - **Test Suites & Snapshots**: Handlers capture output into in-memory buffers (`StringBuilder`) for assertions.
/// - **Daemon & Build Tooling**: Handlers redirect output to log files, IPC pipes, or IDE integration sockets.
/// - **Stateless Formatters**: Consoles and formatters remain stateless singletons because they do not
///   hold references to mutable streams.
@Effect
public final class PrintWriter {

    /// The effect handler interface for intercepting and sinking character output.
    @Effect.Handler
    public interface Handler {

        /// Emits the specified string message to the underlying sink.
        ///
        /// @param message the string to print
        void print(CharSequence message);

        default void println() {
            print("\n");
        }

        default void print(Object obj) {
            print(String.valueOf(obj));
        }

        default void print(boolean b) {
            print(String.valueOf(b));
        }

        default void print(char c) {
            print(String.valueOf(c));
        }

        default void print(int i) {
            print(String.valueOf(i));
        }

        default void print(long l) {
            print(String.valueOf(l));
        }

        default void print(float f) {
            print(String.valueOf(f));
        }

        default void print(double d) {
            print(String.valueOf(d));
        }

        default void print(char[] s) {
            print(new String(s));
        }

        default void println(boolean x) {
            print(x);
            println();
        }

        default void println(char x) {
            print(x);
            println();
        }

        default void println(int x) {
            print(x);
            println();
        }

        default void println(long x) {
            print(x);
            println();
        }

        default void println(float x) {
            print(x);
            println();
        }

        default void println(double x) {
            print(x);
            println();
        }

        default void println(char[] x) {
            print(x);
            println();
        }

        default void println(String message) {
            print(message);
            println();
        }

        default void println(Object x) {
            print(x);
            println();
        }

        default void printf(String format, Object... args) {
            print(String.format(format, args));
        }

        default void printf(Locale l, String format, Object... args) {
            print(String.format(l, format, args));
        }

    }

    /// Prints a string to the ambient {@link PrintWriter.Handler}.
    ///
    /// @param message the string to print
    public static void print(String message) {
        Effects.get(PrintWriter.Handler.class).print(message);
    }

    /// Prints the string representation of an object to the ambient {@link PrintWriter.Handler}.
    ///
    /// @param obj the object to print
    public static void print(Object obj) {
        Effects.get(PrintWriter.Handler.class).print(obj);
    }

    /// Terminates the current line by writing the line separator string to the ambient {@link PrintWriter.Handler}.
    public static void println() {
        Effects.get(PrintWriter.Handler.class).println();
    }

    /// Prints a string and terminates the line on the ambient {@link PrintWriter.Handler}.
    ///
    /// @param message the string to print
    public static void println(String message) {
        Effects.get(PrintWriter.Handler.class).println(message);
    }

    /// Prints the string representation of an object and terminates the line on the ambient {@link PrintWriter.Handler}.
    ///
    /// @param obj the object to print
    public static void println(Object obj) {
        Effects.get(PrintWriter.Handler.class).println(obj);
    }

    /// Writes a formatted string to the ambient {@link PrintWriter.Handler} using the specified format string and arguments.
    ///
    /// @param format a format string as described in {@link String#format(String, Object...)}
    /// @param args arguments referenced by the format specifiers in the format string
    public static void printf(String format, Object... args) {
        Effects.get(PrintWriter.Handler.class).printf(format, args);
    }

    /// Writes a formatted string to the ambient {@link PrintWriter.Handler} using the specified locale, format string, and arguments.
    ///
    /// @param l the {@link Locale} to apply during formatting
    /// @param format a format string as described in {@link String#format(Locale, String, Object...)}
    /// @param args arguments referenced by the format specifiers in the format string
    public static void printf(Locale l, String format, Object... args) {
        Effects.get(PrintWriter.Handler.class).printf(l, format, args);
    }

    /// Creates an effect handler that delegates all printing operations to the given {@link java.io.PrintWriter}.
    ///
    /// @param writer the underlying Java PrintWriter to delegate to
    /// @return a {@link PrintWriter.Handler} delegating to {@code writer}
    public static PrintWriter.Handler of(java.io.PrintWriter writer) {
        return new JavaPrintWriter(writer);
    }

    /// Creates an effect handler that delegates all printing operations to the given {@link java.io.PrintStream}.
    ///
    /// @param stream the underlying Java PrintStream to delegate to
    /// @return a {@link PrintWriter.Handler} delegating to {@code writer}
    public static PrintWriter.Handler of(java.io.PrintStream stream) {
        return new JavaPrintWriter(new java.io.PrintWriter(stream));
    }

    public static PrintWriter.Handler stdOut() {
        return PrintWriter.of(System.out);
    }

    public static PrintWriter.Handler stdErr() {
        return PrintWriter.of(System.err);
    }

    /// Adapter record wrapping a {@link java.io.PrintWriter} as a {@link PrintWriter.Handler}.
    record JavaPrintWriter(java.io.PrintWriter writer) implements PrintWriter.Handler {

        @Override public void print(CharSequence message) {
            writer.print(message);
        }

        @Override public void print(Object obj) {
            writer.print(obj);
        }

        @Override public void println() {
            writer.println();
        }

        @Override public void println(boolean x) {
            writer.println(x);
        }

        @Override public void println(char x) {
            writer.println(x);
        }

        @Override public void println(int x) {
            writer.println(x);
        }

        @Override public void println(long x) {
            writer.println(x);
        }

        @Override public void println(float x) {
            writer.println(x);
        }

        @Override public void println(double x) {
            writer.println(x);
        }

        @Override public void println(char[] x) {
            writer.println(x);
        }

        @Override
        public void println(String message) {
            writer.println(message);
        }

        @Override public void println(Object x) {
            writer.println(x);
        }

        @Override public void printf(String format, Object... args) {
            writer.printf(format, args);
        }

        @Override public void printf(Locale l, String format, Object... args) {
            writer.printf(l, format, args);
        }

        @Override public void print(boolean b) {
            writer.print(b);
        }

        @Override public void print(char c) {
            writer.print(c);
        }

        @Override public void print(int i) {
            writer.print(i);
        }

        @Override public void print(long l) {
            writer.print(l);
        }

        @Override public void print(float f) {
            writer.print(f);
        }

        @Override public void print(double d) {
            writer.print(d);
        }

        @Override public void print(char[] s) {
            writer.print(s);
        }

    }

}
