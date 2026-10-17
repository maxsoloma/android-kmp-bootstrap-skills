# Changelog

## 3.0.0 — 2026-10-17

- A standalone Kotlin/JVM CLI and terminal init wizard power the same portable
  skill in Codex and Claude Code. Java 17+ runs the generator; no Python or global
  Kotlin CLI is required.
- Kotlin Toolchain 0.13.0 is the sole build foundation for generated Android,
  iOS, desktop JVM and browser Wasm applications.
- Platform-aware choices cover shared Compose, native Android and SwiftUI,
  manual/Koin DI, Navigation 3/native navigation, optional Ktor and Settings.
- Config/plan/lock workflows preserve explicit choices, show compatibility
  conflicts, reproduce generation and refuse to overwrite existing projects.
- Actual acceptance includes a 48-stack build/test matrix, platform-only builds,
  emulator/simulator/desktop/browser UI checks and an installed Codex plugin flow.
- Fixed missing Compose compiler configuration in the KMP Android host and
  ambiguous iOS simulator launch instructions. Completed OpenSpec records are
  archived and eight current capabilities are synchronized.
- Directory packaging includes icons, local-execution metadata, public support
  and data-handling information.

### Breaking changes

Gradle project generation and the two hardened skills/profiles were removed.
Retired flags and configurations fail before writes. Existing generated projects
are untouched; use their corresponding prior release for old locks and builds.
