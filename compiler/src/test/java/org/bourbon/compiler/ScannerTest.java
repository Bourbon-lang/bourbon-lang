package org.bourbon.compiler;

import java.util.ArrayList;
import java.util.List;

import org.bourbon.compiler.advisory.Advisory;
import org.bourbon.compiler.advisory.AdvisoryConsole;
import org.bourbon.compiler.advisory.AdvisoryLayout;
import org.bourbon.compiler.advisory.AsciiAdvisoryConsole;
import org.bourbon.compiler.diagnostic.Diagnostic;
import org.bourbon.compiler.diagnostic.DiagnosticLayer;
import org.bourbon.compiler.diagnostic.DiagnosticPipeline;
import org.bourbon.compiler.diagnostic.code.Catalog;
import org.bourbon.compiler.diagnostic.code.DiagnosticCode;
import org.bourbon.compiler.effects.Effects;
import org.bourbon.compiler.effects.io.PrintWriter;
import org.bourbon.compiler.junit.CompilerAssertions;
import org.bourbon.compiler.junit.diff.DiffPrinter;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

@NullMarked
@DisplayName("Scanner test")
@TestMethodOrder(MethodOrderer.DisplayName.class)
class ScannerTest {

    @TestTemplate
    @DisplayName("Scanner recognizes sequence of tokens")
    @ExtendWith(ScannerTestContextProvider.class)
    public void scanTokens(TestInfo testInfo, ScannerTestCase testCase) {
        //noinspection ConstantValue
        if (testCase instanceof ScannerTestCase(Source source, List<Token> expectedTokens, List<Diagnostic> expectedDiagnostics)) {
            var actualDiagnostics = new ArrayList<Diagnostic>();

            var actualTokens = Effects.handle(() -> Scanner.scanTokens(source))
                    .with(Catalog.Handler.class, Catalog.builder()
                            .add(DiagnosticCode::standardErrorCodes)
                            .build())
                    .with(Diagnostic.Handler.class, DiagnosticPipeline.to(actualDiagnostics::add)
                            .layer(DiagnosticLayer.validating())
                            .build())
                    .with(Advisory.Handler.class, new AdvisoryLayout(name ->
                            // FIXME: Make source content dependent on source name
                            Source.named(name).of(source.content())))
                    .with(AdvisoryConsole.Handler.class, new AsciiAdvisoryConsole())
                    .with(PrintWriter.Handler.class, PrintWriter.of(System.err))
                    .get();

            new DiffPrinter(testInfo.getDisplayName()).verbose()
                    .withTokenDiff(expectedTokens, actualTokens)
                    .withDiagnosticDiff(expectedDiagnostics, actualDiagnostics)
                    .printDiff();

            Assertions.assertAll("Compiler Pipeline Verification",
                    CompilerAssertions.tokenSequenceMatch(expectedTokens, actualTokens),
                    CompilerAssertions.diagnosticSequenceMatch(expectedDiagnostics, actualDiagnostics)
            );
        }
    }
}
