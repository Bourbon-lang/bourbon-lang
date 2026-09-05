package org.bourbon.compiler.junit;

import static org.junit.jupiter.api.AssertionFailureBuilder.assertionFailure;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bourbon.compiler.Token;
import org.bourbon.compiler.diagnostic.Diagnostic;
import org.bourbon.compiler.junit.diff.DiagnosticDiffEngine;
import org.bourbon.compiler.junit.diff.DiffEntry;
import org.bourbon.compiler.junit.diff.FieldChange;
import org.bourbon.compiler.junit.diff.TokenDiffEngine;
import org.jline.utils.AttributedString;
import org.junit.jupiter.api.function.Executable;
import org.opentest4j.MultipleFailuresError;

public final class CompilerAssertions {

    private CompilerAssertions() {
        // Utility class for compiler test assertions
    }

    // =========================================================================
    // Executable Producers for use inside Assertions.assertAll(...)
    // =========================================================================

    public static Executable diagnosticSequenceMatch(
            List<Diagnostic> expectedDiagnostics,
            List<Diagnostic> actualDiagnostics) {
        return () -> assertDiagnosticsMatch(expectedDiagnostics, actualDiagnostics);
    }

    public static Executable tokenSequenceMatch(
            List<Token> expectedTokens,
            List<Token> actualTokens) {
        return () -> assertTokensMatch(expectedTokens, actualTokens);
    }

    // =========================================================================
    // Direct Assertion Methods (Hierarchical MultipleFailuresError)
    // =========================================================================

    public static void assertDiagnosticsMatch(List<Diagnostic> expectedDiagnostics, List<Diagnostic> actualDiagnostics) {

        var diffEntries = DiagnosticDiffEngine.compare(expectedDiagnostics, actualDiagnostics);
        var itemFailures = new ArrayList<Throwable>();

        for (var diffEntry : diffEntries) {
            switch (diffEntry) {
                case DiffEntry.Unchanged<Diagnostic> _ -> {}

                case DiffEntry.Added<Diagnostic> added ->
                    itemFailures.add(new AssertionError(String.format(
                            "Unexpected diagnostic at index %d: %s",
                            added.actualIndex(),
                            added.item())));

                case DiffEntry.Deleted<Diagnostic> deleted ->
                    itemFailures.add(new AssertionError(String.format(
                            "Missing expected diagnostic at index %d: %s",
                            deleted.expectedIndex(),
                            deleted.item())));

                case DiffEntry.Modified<Diagnostic>(int expectedIndex, int actualIndex, Diagnostic expected, Diagnostic actual, double similarity, List<FieldChange> fieldChanges) -> {
                    var fieldFailures = new ArrayList<Throwable>();

                    for (var fieldChange : fieldChanges) {
                        switch (fieldChange) {
                            case FieldChange.ModifiedField(var fieldName, var expectedValue, var actualValue) ->
                                    fieldFailures.add(assertionFailure()
                                            .message(String.format("field '%s': ", fieldName))
                                            .expected(expectedValue)
                                            .actual(actualValue)
                                            .build());
                            case FieldChange.AddedField(var fieldName, var actualValue) ->
                                fieldFailures.add(assertionFailure()
                                        .message("Unexpected field '%s'".formatted(fieldName))
                                        .actual(actualValue)
                                        .build());

                            case FieldChange.RemovedField(var fieldName, var expectedValue) ->
                                fieldFailures.add(assertionFailure()
                                        .message("Missing field '%s'".formatted(fieldName))
                                        .expected(expectedValue)
                                        .build());
                            case null -> {}
                        }
                    }

                    if (!fieldFailures.isEmpty()) {
                        var heading = String.format("Diagnostic <%s[%s] %s>", expected.severity().name().toLowerCase(Locale.ROOT), expected.code().id(), expected.message()) +
                                ((expectedIndex == actualIndex)
                                        ? String.format(" at index %d", expectedIndex)
                                        : String.format(" at index %d (expected index %d)", actualIndex, expectedIndex));
                        itemFailures.add(new MultipleFailuresError(heading, fieldFailures));
                    }
                }
                case null -> {}
            }
        }

        if (!itemFailures.isEmpty()) {
            throw new MultipleFailuresError("Diagnostic Sequence Assertion Failure", itemFailures);
        }
    }

    public static void assertTokensMatch(List<Token> expectedTokens, List<Token> actualTokens) {

        var diffEntries = TokenDiffEngine.compare(expectedTokens, actualTokens);
        var itemFailures = new ArrayList<Throwable>();

        for (var diffEntry : diffEntries) {
            switch (diffEntry) {
                case DiffEntry.Unchanged<Token> _ -> {}

                case DiffEntry.Added<Token>(int actualIndex, Token item) -> itemFailures.add(assertionFailure()
                        .message(String.format("Unexpected actual token at index %d", actualIndex))
                        .actual(item)
                        .build());

                case DiffEntry.Deleted<Token>(int expectedIndex, Token item) -> itemFailures.add(assertionFailure()
                        .message(String.format("Missing expected token at index %d", expectedIndex))
                        .expected(item)
                        .build());

                case DiffEntry.Modified<Token>(int expectedIndex, int actualIndex, Token expected, Token actual, double similarity, List<FieldChange> fieldChanges) -> {
                    var fieldFailures = new ArrayList<Throwable>();

                    for (var fieldChange : fieldChanges) {
                        switch (fieldChange) {
                            case FieldChange.ModifiedField(var fieldName, var expectedValue, var actualValue) -> fieldFailures.add(assertionFailure()
                                    .message(String.format("field '%s'", fieldName))
                                    .expected(expectedValue)
                                    .actual(actualValue)
                                    .build());

                            case FieldChange.AddedField(var fieldName, var actualValue) -> fieldFailures.add(assertionFailure()
                                    .message(String.format("field '%s'", fieldName))
                                    .actual(actualValue)
                                    .build());

                            case FieldChange.RemovedField(var fieldName, var expectedValue) ->
                                fieldFailures.add(assertionFailure()
                                        .message(String.format("field '%s'", fieldName))
                                        .expected(expectedValue)
                                        .build());

                            case null -> {}
                        }
                    }

                    if (!fieldFailures.isEmpty()) {
                        var heading = String.format("Token <%s @ %d:%d [%d..%d]>",
                                expected.type(), expected.line(), expected.column(), expected.startOffset(), expected.end()) +
                                ((expectedIndex == actualIndex)
                                        ? String.format(" at index %d", expectedIndex)
                                        : String.format(" at index %d (expected index %d)", actualIndex, expectedIndex));
                        itemFailures.add(new MultipleFailuresError(heading, fieldFailures));
                    }
                }
                case null -> {}
            }
        }

        if (!itemFailures.isEmpty()) {
            throw new MultipleFailuresError("Token Sequence Assertion Failure", itemFailures);
        }
    }

    // =========================================================================
    // ANSI Escape Sequence Assertions
    // =========================================================================

    private static final Pattern SGR_PATTERN = Pattern.compile("\u001B\\[([0-9;]*)m");

    /// Normalizes ANSI Select Graphic Rendition (SGR) escape sequences into a canonical form:
    /// - Sorts semicolon-delimited parameter codes numerically (e.g. `\u001B[31;1m` -> `\u001B[1;31m`)
    /// - Normalizes empty reset codes (`\u001B[m` -> `\u001B[0m`)
    ///
    /// @param input the ANSI string to canonicalize
    /// @return canonicalized ANSI string
    public static String canonicalizeAnsi(String input) {
        if (input == null) {
            return null;
        }
        var matcher = SGR_PATTERN.matcher(input);
        var sb = new StringBuilder();
        while (matcher.find()) {
            String params = matcher.group(1);
            String replacement;
            if (params.isEmpty() || params.equals("0")) {
                replacement = "\u001B[0m";
            } else {
                String sorted = Arrays.stream(params.split(";"))
                        .filter(s -> !s.isBlank())
                        .map(Integer::parseInt)
                        .distinct()
                        .sorted()
                        .map(String::valueOf)
                        .collect(Collectors.joining(";"));
                replacement = "\u001B[" + (sorted.isEmpty() ? "0" : sorted) + "m";
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /// Asserts that two strings with ANSI escape sequences are semantically equivalent.
    ///
    /// Uses JLine's {@link AttributedString#fromAnsi(String)} for order-insensitive attribute
    /// comparison, and falls back to asserting on canonicalized strings so test runners and IDEs
    /// display a clear, side-by-side string diff when the assertion fails.
    ///
    /// @param expected expected ANSI string
    /// @param actual actual ANSI string
    public static void assertAnsiEquals(String expected, String actual) {
        assertAnsiEquals(null, expected, actual);
    }

    /// Asserts that two strings with ANSI escape sequences are semantically equivalent.
    ///
    /// Uses JLine's {@link AttributedString#fromAnsi(String)} for order-insensitive attribute
    /// comparison, and falls back to asserting on canonicalized strings so test runners and IDEs
    /// display a clear, side-by-side string diff when the assertion fails.
    ///
    /// @param message assertion failure message
    /// @param expected expected ANSI string
    /// @param actual actual ANSI string
    public static void assertAnsiEquals(String message, String expected, String actual) {
        var exp = AttributedString.fromAnsi(expected);
        var act = AttributedString.fromAnsi(actual);
        if (!exp.equals(act)) {
            if (message != null) {
                assertEquals(canonicalizeAnsi(expected), canonicalizeAnsi(actual), message);
            } else {
                assertEquals(canonicalizeAnsi(expected), canonicalizeAnsi(actual));
            }
        }
    }
}