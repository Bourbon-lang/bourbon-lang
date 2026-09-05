package org.bourbon.compiler.diagnostic.code;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.bourbon.compiler.effects.Effect;
import org.bourbon.compiler.effects.Effects;

/// An algebraic effect providing the active registry of valid {@link DiagnosticCode}s.
///
/// Used by {@link org.bourbon.compiler.diagnostic.DiagnosticLayer#validating()} to detect
/// unregistered diagnostic codes and treat them as internal compiler bugs.
@Effect
public final class Catalog {

    @Effect.Handler
    public interface Handler {
        /// Finds a diagnostic code by its string identifier.
        ///
        /// @param code the diagnostic code identifier
        /// @return an {@link Optional} containing the resolved {@link DiagnosticCode}, or empty if not registered
        Optional<DiagnosticCode> find(String code);

        /// Checks if a diagnostic code identifier is registered in this catalog.
        ///
        /// @param code the diagnostic code identifier
        /// @return {@code true} if registered; {@code false} otherwise
        boolean contains(String code);
    }

    /// Looks up a diagnostic code by its string identifier in the ambient {@link Catalog.Handler}.
    ///
    /// @param code the diagnostic code string identifier (e.g. {@code "SYN0001"})
    /// @return an {@link Optional} containing the resolved {@link DiagnosticCode}, or empty if not registered
    public static Optional<DiagnosticCode> find(String code) {
        return Effects.get(Catalog.Handler.class).find(code);
    }

    /// Checks if a diagnostic code identifier is registered in the ambient {@link Catalog.Handler}.
    ///
    /// @param code the diagnostic code string identifier (e.g. {@code "SYN0001"})
    /// @return {@code true} if registered; {@code false} otherwise
    public static boolean contains(String code) {
        return Effects.get(Catalog.Handler.class).contains(code);
    }

    /// Checks if the specified diagnostic code is registered in the ambient {@link Catalog.Handler}.
    ///
    /// @param code the diagnostic code instance
    /// @return {@code true} if registered; {@code false} otherwise
    public static boolean contains(DiagnosticCode code) {
        return contains(code.id());
    }

    /// Creates a fluent builder for constructing a new {@link Catalog.Handler}.
    ///
    /// @return a new {@link CatalogBuilder} instance
    public static CatalogBuilder builder() {
        return new CatalogBuilder();
    }

    public static final class CatalogBuilder {
        private final Map<String, DiagnosticCode> codes = new HashMap<>();

        public <D extends DiagnosticCode> CatalogBuilder add(Supplier<Stream<D>> supplier) {
            supplier.get().forEach(this::add);
            return this;
        }

        public <E extends Enum<E> & DiagnosticCode> CatalogBuilder add(Class<E> enumClass) {
            return add(enumClass.getEnumConstants());
        }

        @SafeVarargs
        public final <D extends DiagnosticCode> CatalogBuilder add(D... codes) {
            for (var code : codes) {
                var existing = this.codes.putIfAbsent(code.id(), code);
                if (existing != null && existing != code) {
                    throw new IllegalStateException(
                            "Diagnostic code collision detected: '%s' is shared by %s and %s"
                                    .formatted(code.id(), existing, code)
                    );
                }
            }
            return this;
        }

        public Catalog.Handler build() {
            final var frozenMap = Map.copyOf(codes);
            return new Catalog.Handler() {
                @Override
                public Optional<DiagnosticCode> find(String code) {
                    return Optional.ofNullable(frozenMap.get(code));
                }

                @Override
                public boolean contains(String code) {
                    return frozenMap.containsKey(code);
                }
            };
        }
    }

}
