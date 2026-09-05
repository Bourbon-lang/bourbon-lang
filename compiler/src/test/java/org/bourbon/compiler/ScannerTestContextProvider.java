package org.bourbon.compiler;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.bourbon.compiler.Source.Content;
import org.bourbon.compiler.advisory.Advisory;
import org.bourbon.compiler.advisory.AdvisoryConsole;
import org.bourbon.compiler.advisory.AdvisoryLayout;
import org.bourbon.compiler.advisory.AsciiAdvisoryConsole;
import org.bourbon.compiler.diagnostic.Consultant;
import org.bourbon.compiler.diagnostic.Diagnostic;
import org.bourbon.compiler.diagnostic.DiagnosticLayer;
import org.bourbon.compiler.diagnostic.DiagnosticPipeline;
import org.bourbon.compiler.diagnostic.code.Catalog;
import org.bourbon.compiler.diagnostic.code.DiagnosticCode;
import org.bourbon.compiler.effects.Effects;
import org.bourbon.compiler.effects.io.PrintWriter;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstantiationException;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.platform.commons.io.Resource;

@NullMarked
public class ScannerTestContextProvider implements TestTemplateInvocationContextProvider {
    public static final ExtensionContext.Namespace BOURBON = ExtensionContext.Namespace.create(new Object());

    @Override
    public boolean supportsTestTemplate(ExtensionContext unused) {
        return true;
    }

    @Override
    public Stream<? extends TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
        var testCaseFilter = System.getProperty("scanner.test.case");

        var testResources = TestCaseDiscovery.of(context)
                .streamResources("/scanner", this::isValidTestCase);

        if (testCaseFilter != null && !testCaseFilter.isBlank()) {
            testResources = testResources.filter(resource -> resource.getName().endsWith(testCaseFilter));
        }

        return testResources
                .sorted(Comparator.comparing(Resource::getName))
                .map(resource -> scannerTestContext(context, resource));
    }

    private boolean isValidTestCase(Resource resource) {
        return resource.getName().endsWith(".bourbon.txt");
    }

    private TestTemplateInvocationContext scannerTestContext(ExtensionContext context, Resource resource) {
        try (var in = resource.getInputStream()) {
            Source source = Source.named(resource.getName()).of(Content.read(in));
            context.getStore(BOURBON).put(resource.getName(), source);

            try {
                var parser = new ScannerTestCaseParser(source);
                var testCase = Effects.handle(parser::parseTestCase)
                        .with(Catalog.Handler.class, Catalog.builder()
                                .add(DiagnosticCode::standardErrorCodes)
                                .add(TestCaseError.class)
                                .build())
                        .with(Diagnostic.Handler.class, DiagnosticPipeline.to(new Consultant())
                                .layer(DiagnosticLayer.validating())
                                .build())
                        .with(Advisory.Handler.class, new AdvisoryLayout(name ->
                                Objects.requireNonNull(context.getStore(BOURBON).get(name, Source.class))))
                        .with(AdvisoryConsole.Handler.class, new AsciiAdvisoryConsole())
                        .with(PrintWriter.Handler.class, PrintWriter.of(System.err))
                        .get();

                return scannerTestContext(parser.getDisplayName(), testCase);
            } catch (IllegalArgumentException e) {
                var lexeme = source.lexeme();
                var span = source.currentSpan();
                throw new TestInstantiationException("Failure to parse scanner test case " + resource.getName() + ":" + span.line() + ":" + span.column() + " at '" + lexeme + "' : " + e.getMessage());
            }
        }
        catch (IOException e) {
            throw new TestInstantiationException("Failed to load scanner test case", e);
        }
    }

    private TestTemplateInvocationContext scannerTestContext(String displayName, ScannerTestCase testCase) {
        return new TestTemplateInvocationContext() {
            @Override
            public String getDisplayName(int invocationIndex) {
                return "[" + invocationIndex + "] " + displayName;
            }

            @Override
            public List<Extension> getAdditionalExtensions() {
                return List.of(new ParameterResolver() {
                    @Override
                    public boolean supportsParameter(ParameterContext param, ExtensionContext ext) {
                        return param.getParameter().getType().equals(ScannerTestCase.class);
                    }

                    @Override
                    public Object resolveParameter(ParameterContext ctx, ExtensionContext ext) {
                        return testCase;
                    }
                });
            }
        };
    }
}
