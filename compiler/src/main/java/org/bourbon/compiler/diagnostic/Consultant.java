package org.bourbon.compiler.diagnostic;

import org.bourbon.compiler.advisory.Advisory;

/// Consultant is a {@link Diagnostic Diagnostic effect} handler that turns diagnostics into advisories.
public class Consultant implements Diagnostic.Handler {

    @Override public void report(Diagnostic diagnostic) {
        Advisory.submit(Advisory.of(diagnostic));
    }

}
