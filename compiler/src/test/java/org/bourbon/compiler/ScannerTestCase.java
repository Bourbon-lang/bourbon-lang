package org.bourbon.compiler;

import java.util.List;

import org.bourbon.compiler.diagnostic.Diagnostic;

public record ScannerTestCase(Source input, List<Token> expectedTokens, List<Diagnostic> expectedDiagnostics) {}
