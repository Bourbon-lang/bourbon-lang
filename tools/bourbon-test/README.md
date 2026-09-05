# Bourbon Scanner Test Case Syntax Highlighter

A TextMate bundle (`.tmbundle`) that provides syntax highlighting for Bourbon token scanner test case files (`*.bourbon.txt`).

It highlights headers, separators, comments, expected tokens, and diagnostic message assertions while keeping the actual source code block under test unhighlighted.

---

## Installation & Loading Guide

### 1. IntelliJ IDEA / PyCharm / WebStorm

IntelliJ IDEA supports native TextMate bundles out-of-the-box.

1. Open **Settings** (or **Preferences** on macOS) via `Cmd + ,` or `Ctrl + Alt + S`.
2. Go to **Editor** -> **TextMate Bundles**.
3. Click the **+** (Add) button at the top right of the bundle list.
4. Choose the path to this `bourbon-test` directory.
5. Click **Apply** and **OK**.
6. Open any `*.bourbon.txt` scanner test case file to see it in action.

---

### 2. VS Code & Antigravity IDE

Since this directory contains `package.json`, it is a valid VS Code / Antigravity IDE extension out-of-the-box!

To install it locally:
1. Copy or symlink this entire `bourbon-test` directory to your editor's extensions folder:
   - **VS Code (macOS/Linux)**: `~/.vscode/extensions/BourbonTest.tmbundle`
   - **VS Code (Windows)**: `%USERPROFILE%\.vscode\extensions\BourbonTest.tmbundle`
   - **Antigravity IDE (macOS/Linux)**: `~/.antigravity-ide/extensions/BourbonTest.tmbundle`
   - **Antigravity IDE (Windows)**: `%USERPROFILE%\.antigravity-ide\extensions\BourbonTest.tmbundle`
2. Restart or reload your editor.

---

## Grammar Highlighting Details

| Section | Regex Pattern/Logic | Highlighted Scope |
| :--- | :--- | :--- |
| **Header Separator** | `\A(={3,})\s*$` | `punctuation.definition.separator.header` |
| **Header Title** | Line between `===` boundaries | `entity.name.section` |
| **Source Section** | Block from Header end to first `---` | *None (plain/unhighlighted)* |
| **Separators** | `^(-{3,})\s*$` | `punctuation.definition.separator` |
| **Comments** | `^\s*(#.*)$` | `comment.line.number-sign` |
| **Token Type** | `[A-Z_][A-Z0-9_]*` (before `@`) | `support.type.token` |
| **Lexeme/Literal** | Quoted/raw token value before `@` | `string.unquoted.lexeme` |
| **At Sign** | `@` | `punctuation.separator.at` |
| **Column/Range** | Column number, offsets `[start..end]` | `constant.numeric` |
| **Diagnostic Errors** | `✗ error[CODE]: message` | `invalid.illegal` |
| **Diagnostic Warnings** | `⚠ warning[CODE]: message` | `invalid.deprecated` |
| **Diagnostic Gutter** | `\|` line column indicator | `punctuation.separator.gutter` |
| **Label caret mark** | `^^^` underline | `markup.underline.diagnostic` |
