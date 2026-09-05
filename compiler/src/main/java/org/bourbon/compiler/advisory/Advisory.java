package org.bourbon.compiler.advisory;

import java.util.List;

import org.bourbon.compiler.SourceSpan;
import org.bourbon.compiler.diagnostic.Diagnostic;
import org.bourbon.compiler.diagnostic.code.DiagnosticCode;
import org.bourbon.compiler.effects.Effect;
import org.bourbon.compiler.effects.Effects;

/// A user-facing compiler advisory comprising a diagnostic fact and actionable suggestions.
///
/// Suggestions may be as simple as explanations of the error so that the user can understand the problem and how to fix it.
/// Or in more complex cases, contain structured data to allow automated tools to apply fixes with minimal or no human oversight.
///
/// In more complicated cases, an advisory may group multiple compiler diagnostic messages under a single advisory message.
/// Or offer multiple advisories to the same diagnostic.
///
/// ## Advisory as an effect
///
/// An Advisory also acts as an algebraic effect to surface compiler feedback and suggestions to the consumer.
/// You perform Advisory effect by calling `Advisory.submit(Advisory)`.
///
/// Submission is meant to surface the advisory to the consumer of the compiler.
/// The typical way to surface advisories to the user is by printing formatted output to the terminal console.
/// Or surfacing them to LSP as problem markers, or use suggested fixes to autofix the source code, etc.
@Effect
public sealed interface Advisory {

    static Advisory of(Diagnostic diagnostic) {
        return new DiagnosticAdvisory(diagnostic, List.of());
    }

    enum Severity {
        ERROR,
        WARNING,
        INFO,
        NOTE,
    }

    record SourceAnnotation(SourceSpan span, String message, boolean isPrimary) {
        public int line() { return span.line(); }
        public int column() { return span.column(); }
        public String sourceName() { return span.name().name(); }
    }

    DiagnosticCode code();
    Severity severity();
    String message();
    List<SourceAnnotation> annotations();
    default List<Suggestion> suggestions() {
        return List.of();
    }

    @Effect.Handler
    interface Handler {
        /// Handle advisory submission effect.
        ///
        /// What this means is up to the handler implementation.
        /// The presentation format and destination are determined by the handler implementation.
        /// In the case of a CLI or REPL command line, this means printing formatted advisory messages to the terminal console.
        /// In LSP, or IDE, it will be presented as a problem marker in the editor.
        void submit(Advisory advisory);
    }

    /// Performs the {@link Advisory.Handler} effect to submit a user-facing advisory for presentation.
    ///
    /// Surfaces the advisory to the active consumer (e.g. terminal character-grid renderer,
    /// language server problem markers, or autofix pipelines).
    ///
    /// @param advisory the advisory to submit
    static void submit(Advisory advisory) {
        Effects.get(Advisory.Handler.class).submit(advisory);
    }

    /// Simplest advisory derived directly from an existing diagnostic.
    record DiagnosticAdvisory(Diagnostic diagnostic, List<Suggestion> suggestions) implements Advisory {

        public DiagnosticCode code() {
            return diagnostic.code();
        }

        public Severity severity() {
            return switch (diagnostic.severity()) {
                case Diagnostic.Severity.ERROR -> Severity.ERROR;
                case Diagnostic.Severity.WARNING -> Severity.WARNING;
                case Diagnostic.Severity.INFO -> Severity.INFO;
                case Diagnostic.Severity.NOTE -> Severity.NOTE;
            };
        }

        public String message() {
            return diagnostic.message();
        }

        @Override public List<SourceAnnotation> annotations() {
            return diagnostic.labels().stream()
                    .map(label -> new SourceAnnotation(label.span(), label.message(), label.isPrimary()))
                    .toList();
        }

    }
}
