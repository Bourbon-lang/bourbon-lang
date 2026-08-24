# Bourbon Language Discussions: 07. Tooling, Build System & Rebranding

## 1. Rebranding from Zeylan to Bourbon
* **Origin**: The language was initially named **Zeylan** (tracing Ceylon's history).
* **Rebranding Decision**: Renamed to **Bourbon** to convey richness, distinct identity, and modern software engineering focus.
* **Migration Actions**:
  * Root namespace migrated to `org.bourbon.compiler`.
  * CLI tool renamed to `bourbon` (with `bourbon-jvm` for fast JIT testing).
  * Build definitions, Bazel targets, and documentation site paths updated.

## 2. Hermetic Build System with Bazel & Mise
* **Bazel 9.2 (via Bazelisk)**:
  * Manages compilation of compiler Java modules and GraalVM native image generation (`//compiler:bourbon`).
  * Manages Antora documentation website build.
* **`rules_jvm_external`**:
  * Pinned Maven dependencies locked via `maven_install.json` for reproducible builds.
* **`mise.toml`**: Polyglot environment management:
  * Java: GraalVM Community 25.0 (Java 25)
  * Bazel: 9.2 (Bazelisk 1.29)
  * Node.js: 22 (for Antora UI and site compilation)

## 3. Editor Tooling & Integrations
* **Zed Text Editor**: Integrating Google Gemini agent assistant for developer workflow automation.
* **TOML Support**: Evaluated *Tombi TOML* vs. *Even Better TOML* for editing language configuration and tool manifests.
