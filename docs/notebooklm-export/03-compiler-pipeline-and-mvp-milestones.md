# Bourbon Language Discussions: 03. Compiler Pipeline & MVP Milestones

## 1. Compiler Pipeline Design (Lessons from *Crafting Interpreters*)
The Bourbon compiler pipeline is structured around clean, decoupled passes:

```
Source (.bourbon)
       │
       ▼
   Scanner / Lexer (SourceSpan, UTF-8 char stream, Token stream)
       │
       ▼
Recursive Descent Parser (AST expressions, statements, type syntax)
       │
       ▼
Symbol Resolution & Scope Binder (Lexical scoping, shadow rules)
       │
       ▼
Set-Theoretic Type Checker (Union/Intersection normalization, subtyping lattice)
       │
       ▼
Effect Inference Engine (Row polymorphism, capability bubbling)
       │
       ▼
Execution / Code Generation (Truffle AST Nodes / Bytecode / LLVM IR)
```

## 2. Incremental Dialect Milestones (M0 to M4)
To build the language incrementally and validate each layer with runnable code:

### Milestone M0: Lexical Substrate & Test Harness
* Hand-written character scanner supporting Unicode, line offsets, and source spans.
* Token definition covering all keywords, single/multi-char operators, and literal formats.
* Diagnostic reporter supporting ANSI color styling, Nerd Font icons (`🐘`), and source span underlines.
* Interactive CLI REPL with dynamic prompt alignment.
* Multi-section structural test case runner (`.test` files).

### Milestone M1: Arithmetic Expressions & Literals
* Support for all numeric literals (Integers, Floats, Hex, Binary, SI magnitude suffixes).
* Binary and unary arithmetic operators (`+`, `-`, `*`, `/`, `%`, `^`).
* AST expression nodes, tree-walk evaluator, and constant folding.

### Milestone M2: Statements, Variables & Control Flow
* Variable bindings (`value` for immutability, `variable` for mutable state).
* Conditional branches (`if/else`), blocks, and loops.
* First-class pure function declarations (`shared pure`).

### Milestone M3: Set-Theoretic Types & Flow Narrowing
* Type algebra kernel supporting union types (`A | B`) and intersection types (`A & B`).
* Structural subtyping verification via `satisfies`.
* Flow-sensitive type queries (`if (is String x)`, `if (exists opt)`) with automatic type narrowing.

### Milestone M4: First-Class Algebraic Effects & Handlers
* Effect declarations and capability requirement signatures (`performs Database | Logger`).
* Handler blocks (`handle { ... } with { ... }`).
* Delimited continuation execution and resumption mechanics.
