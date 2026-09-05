package org.bourbon.compiler.diagnostic;

import java.util.Locale;

import org.bourbon.compiler.diagnostic.code.Catalog;
import org.bourbon.compiler.diagnostic.code.InternalErrorCode;

/// A composable interception layer in the diagnostic reporting pipeline.
///
/// Layers wrap a downstream {@link Diagnostic.Handler}, allowing diagnostics
/// to be intercepted, validated, transformed, or suppressed.
@FunctionalInterface
public interface DiagnosticLayer {
    Diagnostic.Handler apply(Diagnostic.Handler next);

    static DiagnosticLayer validating() {
        return next -> diagnostic -> {
            if (!Catalog.contains(diagnostic.code())) {
                var originalSeverity = diagnostic.severity().name().toLowerCase(Locale.ROOT);
                var originalCode = diagnostic.code().id();
                var bugDiagnostic = new Diagnostic(
                        InternalErrorCode.CompilerBug,
                        Diagnostic.Severity.ERROR,
                        "Compiler Bug: Unregistered diagnostic code %s[%s]. Original message: %s"
                                .formatted(originalSeverity, originalCode, diagnostic.message()),
                        diagnostic.labels());
                next.report(bugDiagnostic);
                return;
            }
            next.report(diagnostic);
        };
    }
}
