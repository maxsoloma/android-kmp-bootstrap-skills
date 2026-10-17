## Why

The wizard can choose UI, DI and navigation but cannot start an application with
working networking or persisted local state. Teams need platform-aware data
integrations with lifecycle handling and tests rather than dependency-only stubs.

## What Changes

- Add modern network choices none/Ktor and local storage choices none/Settings.
- Generate a real user-triggered HTTPS request, visible loading/result/error
  state, timeouts, cancellation and platform-specific client engines.
- Persist the counter through native key-value adapters, including browser
  localStorage and a desktop file. Reopening the application restores its value.
- Extend the shared config/catalog/plan contract, terminal editing and agent flow
  with compatibility feedback and selected pins/adapters.
- Preserve all existing defaults, optional stacks, hardened templates, no-overwrite
  protections and verification commands. DataStore/Room/Retrofit modern recipes
  remain explicitly unavailable; hardened integrations retain their behavior.

## Capabilities

### New Capabilities

- `data-integrations`: selectable networking and persisted local state across
  terminal, config and agent flows, including generated behavior and verification.

### Modified Capabilities

None in the main spec store. This adds to the two completed, active bootstrap
changes without syncing or archiving their specifications.

## Impact

Kotlin CLI model/resolution, catalog and plan; additive modern data recipes,
platform adapters and tests; wizard menus, skills/docs and optional generated CI
checks. New generated dependencies: Ktor, coroutines and Multiplatform Settings.
No new CLI runtime, SDK installation or application migration.
