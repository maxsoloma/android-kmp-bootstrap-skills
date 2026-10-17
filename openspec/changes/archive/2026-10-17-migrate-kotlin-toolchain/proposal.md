# Proposal

## Why

Kotlin Toolchain now provides a unified KMP project/build/run/test foundation.
The bootstrap should use it for current platform-aware recipes instead of
maintaining Gradle infrastructure, while retaining its wizard, agent flow,
reproducibility, compatibility validation and real application integrations.

## What Changes

- Generate pinned Kotlin Toolchain wrappers, project/module YAML, platform-aware
  source layouts and native Xcode integration for all modern stack choices.
- Migrate the Kotlin/JVM generator's own build, tests and standalone packaging.
- Preserve no-overwrite publication, strict configuration, locked replay,
  terminal/agent flow, UI/DI/navigation/network/storage behavior and tests.
- Gate the default switch on actual Android, iOS, desktop and browser builds/tests
  across the supported stacks. Report failures rather than claiming generation
  proves that a project builds.
- **BREAKING**: remove Gradle backend/build scripts/wrappers and hardened profiles,
  templates, optional modules and their two dedicated skills. Reject retired
  configuration explicitly; do not silently reinterpret hardened projects.
- Align the surviving portable skill, Claude/Codex plugin manifests, docs and CI.

## Capabilities

### New Capabilities

- `toolchain-projects`: portable Kotlin Toolchain generation, build/test planning,
  verified platform integration, standalone delivery and retired-backend rejection.

### Modified Capabilities

- `guided-bootstrap`: current Toolchain wizard choices and retired-profile rejection.
- `project-configuration`: current catalog/config contract without retired generators.
- `data-integrations`: preserve Toolchain integrations while rejecting retired profiles.
- `stack-selection`: current defaults and binary preservation without obsolete golden fixtures.

Prior changes retain the historical Gradle/hardened decisions; these deltas record
the explicit user-directed breaking migration in the synchronized main specs.

## Impact

All Kotlin CLI rendering/configuration/planning/diagnostics/wizard paths, root
build/packaging, resources and fixtures, skills, manifests, documentation and CI.
No existing generated application is migrated or overwritten. Kotlin Toolchain
and dependencies are pinned; no Python runtime or global CLI installation is needed.
