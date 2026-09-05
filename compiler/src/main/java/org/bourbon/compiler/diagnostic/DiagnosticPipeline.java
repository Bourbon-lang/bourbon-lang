package org.bourbon.compiler.diagnostic;

import java.util.ArrayList;
import java.util.List;

/// Composable pipeline for reporting compiler diagnostics through an onion of {@link DiagnosticLayer}s.
public final class DiagnosticPipeline {

    private final List<DiagnosticLayer> layers = new ArrayList<>();
    private final Diagnostic.Handler sink;

    public DiagnosticPipeline(Diagnostic.Handler sink) {
        this.sink = sink;
    }

    public static DiagnosticPipeline to(Diagnostic.Handler sink) {
        return new DiagnosticPipeline(sink);
    }

    public DiagnosticPipeline layer(DiagnosticLayer layer) {
        layers.add(layer);
        return this;
    }

    /// Assembles the onion from inside out
    public Diagnostic.Handler build() {
        Diagnostic.Handler current = sink;
        for (int i = layers.size() - 1; i >= 0; i--) {
            current = layers.get(i).apply(current);
        }
        return current;
    }
}