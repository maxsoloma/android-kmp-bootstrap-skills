# Design

## Context

See proposal.md. The package has a Codex manifest, repo marketplace, one portable
skill and a self-contained JAR. Prior tests bypassed actual plugin installation.

## Goals / Non-Goals

Validate installed discovery, terminal and agent flows, compatibility feedback,
publication guards and a runnable generated project. Preserve prior platform/UI
acceptance; do not publish, remove unrelated plugins, or change credentials.

## Decisions

Use the installed Codex CLI's marketplace/add/list and native skill-discovery
interfaces, with a fresh temporary workspace outside this repository. Invoke only
the cached skill JAR there. This detects repository-relative path assumptions that
a source-tree smoke test misses. Keep the requested plugin installed for the user.

Run init in an actual PTY: select a multi-platform Compose recipe with Koin,
Navigation 3, Ktor and Settings; inspect review, edit choices, save config, replay
through plan/generate. Also cover cancellation/non-TTY refusal and incompatibility.
Exercise Codex's installed-skill agent route with a bounded explicit test request
when available; do not replace actual discovery with a copied skill prompt.

Build/test and launch generated hosts using the pinned wrapper. Validate app UI
through the existing browser/device interfaces and retain transcripts. Fix only
demonstrated failures, rerun affected flows, and rebuild/bundle/regress the CLI
after generator/packaging changes. Record exact client versions and any limits.

## Risks / Trade-offs

- A running conversation cannot acquire new skills retroactively → inspect fresh
  native discovery and use a fresh test invocation without restarting this chat.
- Codex/runtime network dependencies → retain command outcomes separately from
  generation and avoid interpreting unavailable service checks as passes.
- Installation copies cached files → compare JAR bytes and refresh installation
  after packaging edits before final testing.
