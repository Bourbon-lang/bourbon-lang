package org.bourbon.compiler.util;

import java.lang.reflect.InvocationTargetException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Utility methods for inspecting and formatting Java {@link Record} instances in tests.
public final class Records {

    private Records() {
        throw new UnsupportedOperationException("Cannot instantiate " + Records.class);
    }

    /// Formats a record instance into a string representation in the form:
    /// `RecordSimpleName(comp1, comp2, ...)`
    ///
    /// For non-record objects or null, falls back to their string representation.
    public static @NonNull String format(@Nullable Object object) {
        if (object instanceof Record record) {
            var componentValues = Stream.of(record.getClass().getRecordComponents())
                    .map(rc -> {
                        try {
                            return format(rc.getAccessor().invoke(record));
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw Exceptions.sneakyThrow(e);
                        }
                    })
                    .collect(Collectors.joining(", "));
            return record.getClass().getSimpleName() + "(" + componentValues + ")";
        }
        return String.valueOf(object);
    }
}
