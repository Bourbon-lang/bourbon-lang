# Bourbon Language Discussions: 02. Bootstrapping & Runtime Architecture

## 1. The 3-Stage Bootstrapping Strategy
To develop a self-hosted programming language without being bogged down by low-level assembly generation on day one, a staged bootstrapping architecture was agreed upon:

```
[ STAGE 1: Bourbon 0 ] ──────► [ STAGE 2: Bourbon 1 ] ──────► [ STAGE 3: Bourbon 2 ]
Frontend: Java 25/26           Frontend: Bourbon 0            Frontend: Bourbon 1
Runtime : GraalVM + Truffle    Runtime : GraalVM + Truffle    Runtime : LLVM Native AOT / Standalone
```

### Stage 1: Bourbon 0 (Bootstrap Prototype)
* **Frontend (Lexer, Parser, AST, Diagnostic Reporter, Type Checker)**: Implemented in modern Java (Java 25+).
  * Uses Java sealed interfaces, records, and pattern matching for clean AST definitions.
* **Runtime**: GraalVM + Truffle AST interpreter.
* **Goal**: Prove out the language syntax, EBNF grammar, set-theoretic typing lattice, and effect handling semantics on real programs.

### Stage 2: Bourbon 1 (Frontend Self-Hosting)
* **Frontend**: Rewritten entirely in Bourbon (`lexer.bourbon`, `parser.bourbon`, `typechecker.bourbon`).
* **Compiler**: Compiled by Bourbon 0 into executable Truffle bytecode/nodes.
* **Goal**: Validate language developer ergonomics and robustness by implementing a production compiler in Bourbon itself.

### Stage 3: Bourbon 2 (Runtime Independence & Native Backend)
* **Frontend**: Bourbon 1.
* **Execution Target**: Native AOT compilation targeting **LLVM IR** (producing zero-dependency single-binary CLI executables with sub-millisecond startup) or self-hosted Truffle bytecode.

## 2. Technical Evaluation of GraalVM + Truffle
* **Automatic JIT via AST Partial Evaluation**: Truffle compiles dynamic AST interpreter trees directly into optimized machine code at runtime, eliminating the need to write an LLVM backend during early stages.
* **Monomorphic Specialization for Set-Theoretic Types (`@Specialization`)**:
  * Set-theoretic unions (`String | Float`) and flow checks normally incur type-inspection overhead.
  * Truffle's runtime specialization eliminates union dispatch overhead once types stabilize.
* **Delimited Continuations & Virtual Threads**: Truffle's control-flow mechanisms integrate naturally with Project Loom Virtual Threads for fiber-based effect handlers and non-local resumptions.
* **Built-in Tooling**: Automatic support for Language Server Protocol (LSP), Chrome DevTools debugging, and memory/CPU profiling.

## 3. Programming Language Specification Framework ("arc42 for PLs")
Bourbon adopts an 8-part specification framework modeled after standard language references:
1. *Philosophy & Vision*
2. *Lexical & EBNF Grammar*
3. *Type System & Static Semantics*
4. *Dynamic Semantics & Execution Model*
5. *Modularity & Capability Model (O-Cap)*
6. *Compiler Pipeline Architecture*
7. *Standard Library Specification*
8. *Tooling & Ecosystem Architecture*

## 4. Diátaxis Documentation Architecture
Documentation is strictly partitioned along the Diátaxis 2x2 matrix:
* **Tutorials**: Guided learning onboarding (`docs/modules/tutorials/`).
* **How-To Guides**: Practical problem-solving recipes (`docs/modules/how-to/`).
* **Reference**: Authoritative formal specifications (`docs/modules/reference/`).
* **Explanation**: Theoretical architecture and vision papers (`docs/modules/explanation/`).
* **RFCs**: Formal proposals for upcoming features (`docs/modules/rfcs/`).
