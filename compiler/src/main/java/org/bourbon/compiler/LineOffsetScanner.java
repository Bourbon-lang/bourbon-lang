package org.bourbon.compiler;

public sealed interface LineOffsetScanner permits SimpleLineOffsetScanner {
    int[] scan(CharSequence input);

    static int[] scanSimple(CharSequence input) {
        return new SimpleLineOffsetScanner().scan(input);
    }
}
