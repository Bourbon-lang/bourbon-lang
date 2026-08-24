# Bourbon Language Discussions: 01. Vision, Lineage & Manifesto

## 1. Identity & Heritage
* **Succession to Ceylon**: Bourbon (originally named *Zeylan*) is designed as the modern spiritual successor to Gavin King's Ceylon programming language.
* **Preserved & Evolved Ceylon Constructs**:
  * Set-theoretic type algebra: Union types (`A | B`), Intersection types (`A & B`), and Nullable shorthand (`T?` $\equiv$ `T | Null`).
  * Structural subtyping via `satisfies` vs nominal class inheritance via `extends`.
  * Flow-sensitive type narrowing with smart casting (`if (is String x)`, `if (exists name)`).
  * First-class modifiers without Java `@` annotation clutter (`shared`, `formal`, `actual`, `variable`, `pure`).
  * Named argument & declarative block initialization (`User { name = "Roland"; age = 51; }`).
  * Tuples and guaranteed non-empty sequences (`[T+]`).
* **The Core Innovation: Algebraic Effects on Set-Theoretic Types**:
  Bourbon elevates Ceylon's mathematical foundation by applying set-theoretic operations directly to **algebraic effects and capabilities** (`performs Database | Logger`, `performs E & Mask`, `performs E \ H`).

## 2. The Core Design Pillars
1. **Side-Effects by Default, Opt-in Purity**:
   * Functions can freely perform side-effects without boilerplate signatures; the compiler automatically infers capabilities.
   * `pure` functions are strictly validated by the compiler to guarantee mathematical purity, unlocking safe optimizations, concurrency, and caching.
2. **Decoupled Actions (Effortless Testing & Mocking)**:
   * Functions declare abstract capabilities (`performs Database | Clock`) instead of depending on concrete implementations or dependency injection frameworks.
   * Callers mount handlers dynamically (`handle { ... } with { Database => mockDb; }`).
3. **Fearless Refactoring (No Colored Functions)**:
   * In traditional languages, adding `async/await` splits the language into synchronous and asynchronous functions ("colored functions").
   * Bourbon treats async scheduling, error propagation, and state as standard algebraic effects handled uniformly.
4. **Compiler-Enforced Supply-Chain Security**:
   * Zero-trust sandboxed third-party modules by default.
   * Module descriptors (`module.bourbon`) strictly govern capability permissions (`grant performs Network`).

## 3. Replaced Enterprise Design Patterns
* **Dependency Injection / Service Locators** $\rightarrow$ Native `performs` capability declarations and `handle ... with` blocks.
* **Middleware, Interceptors & Decorators** $\rightarrow$ First-class composable handler functions.
* **Thread-Locals & Context Drilling** $\rightarrow$ Ambient typed effects (`performs Tracing | RequestContext`).
* **Async/Await & Error Monads** $\rightarrow$ Standard library algebraic effects (`std.async`, `std.core.Error`).

## 4. Phased Language Roadmap (Layers 1 to 4)
* **Layer 1: Core Pure Dialect & Set-Theoretic Type System**: Pure functions, primitive types, structs, interfaces (`satisfies`), union types (`A | B`), intersection types (`A & B`), flow narrowing (`is`, `exists`).
* **Layer 2: Local Algebraic Effects & Handlers**: `performs` effect rows, `handle ... with` expressions, first-class handlers, local effect subtraction (`E \ H`).
* **Layer 3: Concurrency, Async & System I/O**: Fibers, delimited continuations, channels, streams (`std.io`, `std.async`).
* **Layer 4: Capability Boundaries & Ecosystem Security**: Module capability grants, compile-time taint tracking (`Tainted<T>` vs `Sanitized<T>`), zero-trust sandboxing.
