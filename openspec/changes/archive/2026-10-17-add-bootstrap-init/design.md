# Design

## Context

See proposal.md. Main.kt parses flat flags into Options; Modern.kt and Hardened.kt return file maps; publish stages files and refuses nonempty destinations. Modern metadata drives verify; hardened recipes retain their documented Gradle verification procedures. Twelve tests include pre-migration golden hashes. Each skill packages the same standalone JAR.

## Goals / Non-Goals

Goals: one compatibility boundary for config, terminal and agents; no generated-template changes; backward-compatible flag behavior; offline operation with no new runtime dependencies.
Non-goals: new SwiftUI/DI/navigation/storage implementations, arbitrary third-party recipes, adding components to existing projects, SDK installation or automatic build execution during init.

## Decisions

- Introduce a versioned ProjectSpec represented by a strict JSON object, normalized through current flag validation into Options. Reject unknown/nested fields and incorrect JSON types. Keep name/package required; expose missing fields and structured issues with field/code/message. Legacy flags stay on their current route to preserve golden output.
- Use profile+kind rather than an opaque preset ID in the portable spec. Presets are convenience defaults for modern Android, modern mobile, modern universal and the two hardened profiles. `ui` reflects the current fixed UI for the selected kind; unsupported requests fail with a reason. SDK defaults remain profile-aware. The catalog derives optional modules/requires from packaged manifests, sharing that logic with Hardened.kt.
- Add `catalog --json`, `plan --config PATH --output PATH [--lock PATH]`, `generate --config PATH --output PATH [--lock PATH] [--dry-run]`. Config mode rejects mixing legacy overrides. Plan returns validity, missing fields/issues and, on success, normalized config, file inventory, Gradle modules, optional closure, actual pinned catalog/wrapper, platform requirements and verification commands/scope. Errors do not create output parents.
- Emit bootstrap.spec.json and bootstrap.lock.json only in new flows. Lock records schema/generator version, canonical spec SHA-256 and bundled recipe SHA-256. Reject an explicit stale lock; automatically use a sibling bootstrap.lock.json when present, so replay cannot silently ignore pins. No runtime timestamps or absolute output paths are persisted. Resource fingerprint excludes the index and includes sorted resource names/bytes; versioned generator identifies code. Altered recipes require updated identity/version or explicit regeneration without the old lock.
- Implement a plain numbered text wizard using Java Console with injectable IO for tests. Avoid a TUI dependency and ANSI-only controls. Steps: basics, recipe, targets, SDKs, optional modules/feature, agents, review. A fixed UI/architecture summary explains existing profiles rather than pretending libraries are freely configurable. `back` and `cancel` work throughout; review can revisit each step. Defaults are suggested but previously explicit choices are retained when recipes change; resulting compatibility issues remain visible until resolved.
- Review permits create, edit, save config, or cancel. Save uses CREATE_NEW and rejects paths inside the intended output. Create revalidates just before publishing. Non-TTY init returns structured guidance for agent/config mode without consuming stdin. Builds remain explicit subsequent verify commands.
- Agent skills use catalog → conversation → config outside destination → plan → generate. No requirement for client-specific forms or mandatory additional approval after an authorized creation request. Hardened guidance remains separate and uses the same current Gradle tasks.

## Risks / Trade-offs

- [Combinatorial growth] → Only current recipes are selectable; native SwiftUI and freely mixed components are unavailable entries with reasons.
- [Future pins drift] → Lock validation plus resource fingerprint; emitted portable spec is independent of host paths.
- [Backtracking invalidates choices] → Preserve explicit answers, show field errors, allow targeted editing; never silently replace targets/SDK/modules.
- [Hidden terminal input blocks agents] → Require Console for init; noninteractive structured protocol for agents.
- [Golden output changes] → Add config/lock files only to new entry points, run all existing hashes unchanged.
- [Cross-platform terminal differences] → Plain text and Java Console, scripted state-machine tests, real PTY smoke on local macOS; Windows/Linux runs belong to existing CI.

## Migration Plan

Add commands without replacing existing flags, update all skill guides and README, rebuild identical JARs, validate OpenSpec and the full Kotlin suite. No new app templates or toolchain pins are introduced; generation equivalence tests substantiate that boundary. Check one generated modern build through the new flow and report unavailable host checks. Keep this change active for review rather than auto-archiving it.
