package org.bourbon.compiler;

import java.nio.file.Path;
import java.util.function.IntUnaryOperator;

public record SourceSpan(
        SourceName name,
        int line,
        int column,
        int startOffset,
        int length) {

    public SourceSpan withLength(IntUnaryOperator length) {
        return new SourceSpan(name, line, column, startOffset, length.applyAsInt(this.length));
    }

    public interface Provider {
        SourceSpan at(int line, int column, int startOffset, int length);
    }

    public sealed interface SourceName {
        String name();

        static SourceName of(String name) {
            return new Name(name);
        }

        static SourceName of(Path path) {
            return new FilePath(path);
        }

        record Name(String name) implements SourceName {

            @Override public String toString() {
                return name();
            }

        }
        record FilePath(Path path) implements SourceName {
            public String name() {
                return path.toString();
            }

            @Override public String toString() {
                return name();
            }

        }
    }
}
