package org.bourbon.compiler.diagnostic.code;

import java.util.Objects;
import java.util.stream.Stream;

/// Unique identifier and title for a recognized compiler diagnostic code.
///
/// Implementations are typically domain-specific enums (such as {@link SyntaxErrorCode}
/// or {@link InternalErrorCode}) conforming to the standard 4-digit taxonomy.
public interface DiagnosticCode {
    String id();
    String title();

    static Stream<DiagnosticCode> standardErrorCodes() {
        return Stream.of(InternalErrorCode.class, SyntaxErrorCode.class)
                .flatMap(e -> Stream.of(e.getEnumConstants()));
    }

    static boolean equals(DiagnosticCode a, DiagnosticCode b) {
        return Objects.equals(a.id(), b.id());
    }
}
