# Design

## Context

See proposal.md. The current Kotlin CLI has strict config/lock replay, six wizard
presets and three standalone skills, backed by Gradle rendering and packaging.
Modern native/data integrations already pass local Gradle/Xcode/browser checks.
The user explicitly requests removing Gradle and hardened, superseding their
preservation in prior changes and repository guidance. Historical OpenSpec
records remain history; existing generated projects are never migrated in place.

## Goals / Non-Goals

Goals: one pinned Kotlin Toolchain backend for the generator and generated apps;
preserve modern behavior, validation and standalone delivery; prove real builds.
Non-goals: retaining an alternate Gradle backend, silently mapping hardened
feature modules to compact recipes, app-store publication or changing live apps.

## Decisions

- Start from released Toolchain 0.13.0 with upstream wrappers/checksums preserved.
  Pin Kotlin, Compose and selected Maven dependencies explicitly rather than
  inheriting moving defaults. Reassess a pin only on demonstrated incompatibility.
- Keep one Kotlin/JVM CLI and one portable skill. Root build uses Toolchain JVM
  application packaging and Kotlin build-time tooling for bundled resources;
  no Python, Node or repository sibling runtime is introduced for generation.
- Use project/module YAML and Toolchain source-set directories. Native Android
  remains a single app module; KMP has shared logic/UI and selected app hosts.
  SwiftUI keeps common logic free of Compose and includes a JVM test target.
- Reuse application source templates, replacing build configuration and platform
  layouts. Toolchain iOS integration uses the supported Xcode wrapper mechanism
  and KotlinModules framework import. Preserve deployment targets and ARM64 scope.
- Remove retired options from discovery and presets. Retain a useful explicit
  rejection for old hardened configs/flags; old locks cannot silently replay
  against the new renderer. Bump the breaking generator/plugin version together.
- Replace Gradle task assumptions in plan/doctor/verify with Toolchain commands
  and safe module/platform selectors. Keep bounded execution, logs and partial
  results; include real Xcode host compilation in each iOS platform build and
  retain its output separately from native test execution.
- Coordinate simulator tests across verifier processes with a bounded host file
  lock; Toolchain's own 0.13.0 lock only covers one JVM. Direct Xcode commands
  explicitly pass the physical PROJECT_DIR to avoid symlink-alias mismatches.
- Generate all compatible platform subsets in regression tests. Actual matrix:
  full DI × navigation × network × storage combinations for shared Compose,
  native Android and native SwiftUI mobile recipes; include platform-only smoke
  builds to expose source-set assumptions. Exercise Android compilation/tests,
  desktop tests/build, Wasm build/browser tests, iOS native tests and Xcode hosts.
  This covers current stack choices, not every possible future external library.
- Switch the shipped default only after the integration matrix is green. Any
  unsupported scenario remains an explicit failed acceptance criterion requiring
  resolution; do not hide it by dropping a supported stack or test.

## Risks / Trade-offs

- Alpha APIs and stale docs → use versioned release sources, CLI help and actual
  builds. Release 0.13.0 implements Playwright Wasm tests despite older docs.
- Kotlin source-set/JVM/Android differences → platform-only builds and full stack
  matrix; preserve NSUserDefaults/localStorage/file tests and mock transport tests.
- Toolchain packaging differs from flat fat JAR → test copied executable alone,
  bundle every required resource and preserve upstream license notices.
- First builds provision SDKs/runtime dependencies → record host, exact versions,
  commands/logs, actual failures and resource limits; generation is not evidence.

## Migration Plan

Implement and validate in this checkout without touching generated applications.
Remove retired files and update manifests/docs/CI only within the repository.
Keep prior OpenSpec changes as provenance, mark the new change complete only
after verification, and do not commit, publish or archive automatically.
