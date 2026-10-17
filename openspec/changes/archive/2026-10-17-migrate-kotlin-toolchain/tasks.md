# Tasks

## 1. Toolchain foundation

- [x] 1.1 Pin released Kotlin Toolchain wrappers and migrate root CLI build, resource preparation, tests and standalone JAR packaging; verify real Toolchain build/test/package and copied JAR execution, and document reproducible commands.

## 2. Generation contract and integrations

- [x] 2.1 Replace Gradle rendering with Toolchain YAML/source layouts and Xcode integration for all modern UI/DI/navigation/data choices; retain meaningful generated tests, update template documentation, and compile representative Android/iOS/desktop/Wasm projects.
- [x] 2.2 Remove hardened options/presets/renderers/templates/skills and Gradle infrastructure, update versioned config/catalog/plan/wizard/diagnostics/plugins/docs/CI, and verify strict retired-option rejection, no-overwrite, terminal behavior, all compatible platform subsets and locked standalone replay.

## 3. Acceptance and default

- [x] 3.1 Execute and record the actual stack matrix across Android, iOS, desktop and Wasm, including native/browser tests and Xcode hosts; resolve integration failures without dropping supported choices or tests, and make Toolchain the shipped default only after all required checks pass.
- [x] 3.2 Rebuild the final standalone skill, verify packaging/license/discovery consistency and absence of active Gradle/hardened files, run final regression/OpenSpec checks and record exact versions, logs, matrix results and remaining runtime limits.
