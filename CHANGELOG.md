# Changelog

## 3.2.0 — 2026-10-29

- Add the portable `orchestrate-feature` skill for implementing and resuming
  features in existing Kotlin Toolchain apps without regenerating them.
- Add Kotlin CLI feature journals with dependency validation, exclusive active
  scopes, bounded attempts/repairs/time/packets, retained history, independent
  review admission and current-source test/review evidence.
- Bundle the same standalone Java 17+ runtime in both skills; no Python,
  RoadTower checkout, central tracker or model API key is required.
- Add independent tester guidance, requirement-to-case mapping, skill discovery
  and portable reference checks, recovery instructions and usage examples.
- Verify a real Android/iOS/Web shared-Compose feature with managed authors and
  independent review: all six build/test commands passed, eight tests per
  platform, and actual emulator/simulator/browser UI checks. Repository: 77 tests
  passed. See [live acceptance](docs/verification/feature-orchestrator-live-20261029/README.md).
- Include updated installation, upgrade, CLI/workflow, privacy and release docs
  in the plugin distribution. Token/billing limits and evidence authenticity
  remain explicitly outside the cooperative journal's enforcement boundary.

## 3.1.0 — 2026-10-28

- Update Navigation 3 to stable 1.2.0 and serialization to 1.12.0.
- Recommend Navigation 3 in Compose menus, retain explicit None and direct-config
  defaults, decorate per-entry saveable state and test ordered back-stack replay.
- Add optional native Firebase Analytics, Cloud Messaging and Remote Config in
  Android/iOS hosts: validated public SDK config, offline replay, notification
  permissions, FID registration callbacks, Apple APNs mapping and SwiftUI delegate handling.
- Extend chat/terminal configuration and project/app registration guidance with
  explicit CONFIG_MISSING / CONFIGURED / live NOT_RUN states. Crashlytics upload
  tooling and desktop/web Firebase recipes remain unsupported.
- Add fresh-context bounded worker guidance to the portable skill and generated
  agent instructions. Validate the wizard through independent forward testing.
- Recheck current Kotlin/Compose/Toolchain pins and AGP 9 KMP module ownership;
  preserve the Toolchain backend and bundled wrapper bytes. See
  [stack review and verification](docs/STACK-REVIEW.md).

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
