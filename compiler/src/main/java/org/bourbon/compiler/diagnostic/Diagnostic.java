package org.bourbon.compiler.diagnostic;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.bourbon.compiler.Label;
import org.bourbon.compiler.SourceSpan;
import org.bourbon.compiler.SourceSpan.SourceName;
import org.bourbon.compiler.advisory.Advisory;
import org.bourbon.compiler.diagnostic.code.DiagnosticCode;
import org.bourbon.compiler.diagnostic.code.InternalErrorCode;
import org.bourbon.compiler.diagnostic.code.SyntaxErrorCode;
import org.bourbon.compiler.effects.Effect;
import org.bourbon.compiler.effects.Effects;

/// Low-level factual compiler diagnostic emitted during pipeline execution.
///
/// Diagnostics represent raw compilation facts (syntax violations, type mismatches,
/// or broken invariants). They are reported up the compiler stack via the {@link Diagnostic.Handler}
/// effect, passed through a {@link DiagnosticPipeline}, and ultimately enriched into
/// user-facing {@link Advisory} instances.
///
/// ## Diagnostic vs. Advisory
/// - A **Diagnostic** is a factual event encountered in the compiler pipeline.
/// - An **Advisory** is actionable presentation content surfaced to the developer or tools.
@Effect
public record Diagnostic(
        DiagnosticCode code,
        Severity severity,
        String message,
        List<Label> labels) {

    @Override public boolean equals(Object obj) {
        if (this == obj) return true;
        //noinspection DeconstructionCanBeUsed
        if (!(obj instanceof Diagnostic other)) return false;
        return DiagnosticCode.equals(this.code, other.code)
                && this.severity == other.severity
                && Objects.equals(this.message, other.message)
                && Objects.equals(this.labels, other.labels);
    }

    @Override public int hashCode() {
        return Objects.hash(code.id(), severity, message, labels);
    }

    @Effect.Handler
    public interface Handler {
        /// Reports a diagnostic fact to the active compiler diagnostic pipeline.
        ///
        /// @param diagnostic the diagnostic to report
        void report(Diagnostic diagnostic);
    }

    /// Performs the {@link Diagnostic.Handler} effect to report a diagnostic fact up the compiler pipeline.
    ///
    /// @param diagnostic the diagnostic to report
    public static void report(Diagnostic diagnostic) {
        Effects.get(Diagnostic.Handler.class).report(diagnostic);
    }

    public boolean isError() {
        return severity == Severity.ERROR;
    }

    public enum Severity {
        ERROR, WARNING, INFO, NOTE;
    }

    /// Constructs and immediately reports an {@link Severity#ERROR} diagnostic using the default code title.
    ///
    /// @param code the diagnostic error code
    /// @param labels the annotated source labels highlighting error locations
    /// @return the reported {@link Diagnostic} instance
    public static Diagnostic error(DiagnosticCode code, List<Label> labels) {
        return error(code, code.title(), labels);
    }

    /// Constructs and immediately reports an {@link Severity#ERROR} diagnostic with an explanatory message.
    ///
    /// @param code the diagnostic error code
    /// @param message the explanatory error message
    /// @param labels the annotated source labels highlighting error locations
    /// @return the reported {@link Diagnostic} instance
    public static Diagnostic error(DiagnosticCode code, String message, List<Label> labels) {
        var error = new Diagnostic(code, Severity.ERROR, message, labels);
        Diagnostic.report(error);
        return error;
    }

    /// Constructs and immediately reports a {@link Severity#WARNING} diagnostic with an explanatory message.
    ///
    /// @param code the diagnostic warning code
    /// @param message the explanatory warning message
    /// @param labels the annotated source labels highlighting warning locations
    /// @return the reported {@link Diagnostic} instance
    public static Diagnostic warning(DiagnosticCode code, String message, List<Label> labels) {
        var warning = new Diagnostic(code, Severity.WARNING, message, labels);
        Diagnostic.report(warning);
        return warning;
    }

    @SuppressWarnings({ "unused", "UnusedReturnValue" })
    public static final class InternalError {
        private InternalError() { /* sealed */}

        public static Diagnostic notImplemented(SourceSpan sourceSpan, String message) {
            return error(InternalErrorCode.CompilerBug, "Not implemented!",
                    List.of(new Label(sourceSpan, message, true)));
        }

        public static Diagnostic unexpectedCompilerError(SourceSpan sourceSpan, String message) {
            return error(InternalErrorCode.CompilerBug, "Unexpected compiler error!",
                    List.of(new Label(sourceSpan, message, true)));
        }

        public static Diagnostic ioError(Path path, IOException exception) {
            var sourceSpan = new SourceSpan(SourceName.of(path), 0, 0, 0, 0);
            return ioError(path, sourceSpan, exception);
        }

        public static Diagnostic ioError(Path path, SourceSpan sourceSpan, IOException exception) {
            return switch (exception) {
                case FileNotFoundException fileNotFound -> fileNotFound(path, sourceSpan, fileNotFound);
                default -> unknownIoError(path, sourceSpan, exception);
            };
        }

        public static Diagnostic fileNotFound(Path path, SourceSpan sourceSpan, FileNotFoundException exception) {
            return error(InternalErrorCode.IoFileNotFound, Objects.requireNonNull(exception.getMessage()),
                    List.of(new Label(sourceSpan, "File not found", true)));
        }

        public static Diagnostic unknownIoError(Path path, SourceSpan sourceSpan, IOException exception) {
            return error(InternalErrorCode.IoError, List.of(new Label(sourceSpan, Objects.requireNonNull(exception.getMessage()), true)));
        }

    }

    @SuppressWarnings("UnusedReturnValue")
    public static final class ScannerDiagnostic {
        private ScannerDiagnostic() {/* sealed */}

        public static Diagnostic unexpectedCharacter(SourceSpan span) {
            return error(SyntaxErrorCode.UnexpectedCharacter,
                    List.of(Label.primaryOf(span, "Unexpected symbol")));
        }

        public static Diagnostic unbalancedMultilineComment(SourceSpan startSpan, SourceSpan endSpan) {
            return error(SyntaxErrorCode.UnbalancedMultilineComment,
                    List.of(Label.primaryOf(startSpan, "Multi-line comment starts here"),
                            Label.of(endSpan, "Still no end of comment")));
        }

        public static Diagnostic numericLiteralError(String message, Label ... labels) {
            return numericLiteralError(message, List.of(labels));
        }

        public static Diagnostic numericLiteralError(String message, List<Label> labels) {
            return error(SyntaxErrorCode.NumericLiteralError, message, labels);
        }

    }

}
