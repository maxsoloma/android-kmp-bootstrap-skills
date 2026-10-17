# Proposal

## Why

The Kotlin generator already supports modern and hardened profiles, but users and agents must translate project choices into flags themselves. A shared configuration and compatibility model will make a guided init reliable without changing the existing generated applications.

## What Changes

- Add a terminal `init` with presets, editable steps, review, cancellation and explicit creation.
- Add a versioned ProjectSpec, strict config validation, compatibility diagnostics, `catalog --json`, `plan --config`, and `generate --config`.
- Preserve all modern target combinations, hardened optional module closure, SDK/agent options, and existing flag interfaces and output bytes.
- Emit portable spec/lock files only for the new config/init flows; retain existing metadata and verification contracts.
- Make agent skills drive the same noninteractive config/plan flow rather than a hidden terminal session.
- Do not add new UI/library recipes, install SDKs, or initialize nonempty repositories in this iteration.

## Capabilities

### New Capabilities

- `project-configuration`: Portable project specification, profile catalog, compatibility resolution and deterministic generation planning.
- `guided-bootstrap`: Interactive init and equivalent noninteractive agent workflow with safe review and generation.

### Modified Capabilities

None; there are no existing main specs. Existing CLI behavior is a compatibility constraint.

## Impact

Kotlin CLI routing and new configuration/wizard classes, tests, skill documentation, bundled JARs and README. Existing renderers and publication guards are reused. No new runtime dependencies or template/toolchain changes are required.
