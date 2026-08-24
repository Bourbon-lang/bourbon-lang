# Bourbon Language Discussions: 05. Algebraic Effects & Java Interop

## 1. Set-Theoretic Effect Algebra
Bourbon models functions and capability requirements through set-theoretic operations:
* **Union (`performs A | B`)**: The expression can invoke operations from capability `A` or capability `B`. The outer context must provide handlers for both.
* **Intersection (`satisfies A & B`)**: Handlers or objects satisfying the intersection of multiple effect interfaces provide all operations from both.
* **Capability Sandboxing (`performs E & Mask`)**: Restricts untrusted callbacks. If a plugin requires capability set `E`, intersecting it with `Mask` strips all unauthorized operations (such as file or network IO).
* **Relative Complement (`E \ H`)**: Subtraction of handled capabilities. If an expression performs `Database | Logger`, and is enclosed in a `handle` block for `Database`, the outer expression only performs `Logger` (`(Database | Logger) \ Database = Logger`).

## 2. Mirroring Effect Calling Conventions in Java Host Runtime

### Evaluation of Options

#### Selected Pattern: Ambient Dynamic Context (Variant 1)
```java
@Effect
public final class DiagnosticReport {
    public interface Handler {
        void report(Diagnostic diagnostic);
    }
    
    public static void report(Diagnostic diagnostic) {
        EffectContext.get(DiagnosticReport.Handler.class).report(diagnostic);
    }
}
```
* **Call Site**: `DiagnosticReport.report(diagnostic);`
* **Mounting Handlers**:
```java
Effects.handle(() -> scanner.scanTokens())
       .with(DiagnosticReport.Handler.class, new ConsoleDiagnosticHandler())
       .run();
```
* **Advantages**:
  * Zero coupling at the call site — caller methods do not need to implement or pass capability interfaces.
  * Preserves ambient dynamic effect semantics up the call stack.
  * Backed by Java 21+ `ScopedValue` and Virtual Threads (Project Loom) for thread-safe, fiber-local resolution.

#### Rejected Pattern: Explicit Capability Interface Passing (Variant 2)
```java
public class Scanner implements PerformsReportDiagnostic {
    public List<Token> scanTokens() {
        Reporter().report(Diagnostic.error(...));
    }
}
```
* **Why Rejected**: Degrades algebraic effects into standard OOP interface passing. Introduces rigid coupling, function coloring, and breaks ambient dynamic resolution.

## 3. Delimited Continuations & Resumptions
* Handlers can be configured as **one-shot** or **multi-shot** resumptions (`resume(value)`), **aborts** (`abort()`), or **generators/streams**.
* Return types can represent single values, iterators, or asynchronous futures seamlessly.
