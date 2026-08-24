# Bourbon Language Discussions: 06. Test Harness & Diagnostics

## 1. Structural Test Case Specification Format
Compiler scanner and parser tests are written in a concise multi-section `.test` / `.bourbon.txt` format:
```text
========
Test Title
========
// Input Bourbon Source Code
1234 1k #FF $01
---
---
// Expected Tokens / AST / Diagnostics
NUMBER 1234  1234.0 @ 1
NUMBER 1k    1.0e+3 @ 6
NUMBER #FF   255.0  @ 10
NUMBER $01   1.0    @ 14
```
* **Section Markers**: `========` defines title boundaries; `---` separates input source code, test metadata, expected token streams, diagnostics, and AST trees.

## 2. Structural Data Comparison & Diffing Algorithms
* To provide human-readable test failure reports comparing actual vs. expected token streams and ASTs:
  * **Myers Diff / LCS**: Computes the minimal edit sequence for line-by-line and token-by-token diffing.
  * **Sort-Merge Diff**: Linear memory-efficient streaming diffing for sorted outputs.
  * **Zhang-Shasha (Tree Edit Distance)**: Evaluated for structural AST tree node comparisons.

## 3. JUnit 5 Dynamic Test Source Mapping
* **Problem**: In standard JUnit 5 `@TestTemplate` setups, IDE test trees report the test class runner as the source location.
* **Solution**: Implemented a custom `TestSourceProvider` that maps dynamic test invocations to `UriSource.from(testResourceUri)`. This allows developers in IntelliJ IDEA and VSCode to jump directly to the exact failing line in the `.test` file on disk.

## 4. Terminal Formatting & REPL Styling
* **Bourbon Elephant Prompt**: Padded elephant icon (`🐘`) displayed in the interactive REPL prompt.
* **JLine 3 & ANSI Styling**:
  * Diagnostic reports styled with ANSI color coding (`[ERROR]`, `[WARN]`, `[INFO]`).
  * Source code snippets dynamically aligned with column offsets.
  * Exact error spans underlined with carets (`^^^^^`).
