# Bourbon Language Discussions: 04. Lexer, Numeric Literals & Operators

## 1. Ceylon & Bourbon Numeric Literal Formats
Bourbon provides rich numeric literal syntax:
1. **Plain Decimal**: Integers (`1234`), floating-point decimals (`123.45`).
2. **SI Magnitude Suffixes**:
   * **Integer Magnitudes (powers of $10^3$)**:
     * `k` (kilo, $10^3$) $\rightarrow$ `1k` = $1,000$
     * `M` (Mega, $10^6$) $\rightarrow$ `2M` = $2,000,000$
     * `G` (Giga, $10^9$) $\rightarrow$ `3G` = $3,000,000,000$
     * `T` (Tera, $10^{12}$) $\rightarrow$ `4T` = $4,000,000,000,000$
     * `P` (Peta, $10^{15}$) $\rightarrow$ `5P` = $5,000,000,000,000,000$
   * **Fractional Magnitudes (powers of $10^{-3}$)**:
     * `m` (milli, $10^{-3}$) $\rightarrow$ `1m` = $0.001$
     * `u` (micro, $10^{-6}$) $\rightarrow$ `2u` = $0.000002$
     * `n` (nano, $10^{-9}$) $\rightarrow$ `3n` = $0.000000003$
     * `p` (pico, $10^{-12}$) $\rightarrow$ `4p` = $0.000000000004$
     * `f` (femto, $10^{-15}$) $\rightarrow$ `5f` = $0.000000000000005$
3. **Scientific E Notation**: `123.45E+5`, `1.0e-3`.
4. **Hexadecimal Literals**: Prefix `#` (e.g. `#0123`, `#FF`, `#5a_FB`, `#1234_5678`).
5. **Binary Literals**: Prefix `$` (e.g. `$01`, `$1010_1010`).
6. **Digit Grouping**: Underscores (`_`) allowed anywhere between digits (e.g. `1_234_567_890`).

## 2. Tokenization Architecture: The Unified `NUMBER` Token
* **Design Decision**: All five numeric representations emit a single `TokenType.NUMBER` token.
* **Benefits**:
  * Expression grammar remains compact and uniform without redundant rules for `HEX_INT`, `BINARY_INT`, `FLOAT`, etc.
  * The scanner normalizes text into a structured `NumberLiteral` record containing `BigDecimal` and `Magnitude`.
  * Radix conversion, underscore stripping, and magnitude evaluation are decoupled from the parser and type checker.

## 3. Cursor Sequence Matching Optimization
* When matching multi-character symbols (`...`, `?.`, `===`, `=>`, `&&=`) at the current scanner position:
  * Hand-written nested character switch statements (equivalent to a trie) provide the highest performance on the JVM.
  * Ensures zero intermediate string allocation and optimal branch prediction.

## 4. Operator Token Taxonomy
* **Question Mark Variants**:
  * `?` $\rightarrow$ `QUESTION` (Optionality `String?`, ternary condition)
  * `?.` $\rightarrow$ `QUESTION_DOT` (Safe navigation member access)
  * `?:` $\rightarrow$ Default/elvis operator
* **Set & Arrow Operators**:
  * `|`, `||`, `|=`, `||=` $\rightarrow$ `PIPE`, `PIPE_PIPE`, `PIPE_EQUAL`, `PIPE_PIPE_EQUAL` (Set union, logical OR, union assignment)
  * `&`, `&&`, `&=`, `&&=` $\rightarrow$ `AMPERSAND`, `AMPERSAND_AMPERSAND`, `AMPERSAND_EQUAL`, `AMPERSAND_AMPERSAND_EQUAL` (Set intersection, logical AND)
  * `=>` $\rightarrow$ `FAT_ARROW` (Fat arrow for lambda returns / handler mappings)
  * `->` $\rightarrow$ `THIN_ARROW` (Key-value entry constructor)
  * `\ ` $\rightarrow$ Relative complement / set subtraction
