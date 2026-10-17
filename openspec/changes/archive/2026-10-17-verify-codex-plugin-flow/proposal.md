# Proposal

## Why

The standalone generator and generated apps have runtime evidence, but the real
Codex plugin installation, discovery and bootstrap journey have not been tested.
The user requests the full installed-plugin flow and fixes for demonstrated failures.

## What Changes

- Install the local marketplace/plugin with the actual Codex client and inspect
  installed files and native skill discovery.
- Exercise terminal init and the agent-compatible catalog/config/plan/generate
  route from the installed skill, including compatibility and no-overwrite guards.
- Build, test and launch a generated project; correct discovered integration bugs
  and document reproducible installation/use with explicit evidence and limits.

## Capabilities

### New Capabilities

- `codex-plugin-integration`: installed Codex plugin discovery and bootstrap
  execution from cached, self-contained skill resources.

### Modified Capabilities

None.

## Impact

Codex marketplace/manifest, portable skill instructions, installation docs,
integration verification, and any generator defects found by the journey. Existing
CLI choices/checks and Claude plugin compatibility remain supported. No publishing,
unrelated plugin changes, credential handling or global runtime replacement.
