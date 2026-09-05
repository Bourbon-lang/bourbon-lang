package org.bourbon.compiler.advisory;

/// Actionable remediation advice or explanatory note attached to an {@link Advisory}.
///
/// Suggestions help developers resolve diagnostic issues, ranging from human-readable
/// hints to structured code replacements for automated tooling.
public sealed interface Suggestion {

    /// Simple textual suggestion for human consumption
    record Note(String message) implements Suggestion {}

    // TODO: More structured suggestions for automated fixes or refactorings
}
