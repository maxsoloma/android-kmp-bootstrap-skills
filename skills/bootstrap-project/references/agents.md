# Bounded delegation

Use a worker for an independent research question, isolated implementation slice
or review when it reduces coordination cost. A simple wizard/catalog/generation
flow fits one agent. Discover the host's tools; do not invent agent APIs.

With Codex collaboration, use `spawn_agent` with `fork_turns: "none"`. On other
hosts use the equivalent fresh-context worker if available. Do not copy the
parent transcript, full skill tree, config credentials or long logs. Give a brief:

```text
Objective: <one observable result>
Workspace/skill: <absolute paths>
Read: applicable AGENTS.md, <specific references/files/symbols>
Allowed writes: <owned paths, or read-only>
Constraints: <platforms, pins, no existing-project overwrite, relevant scope>
Check: <specific command or behavior and evidence location>
Return: findings, changed files, command/status, blockers; keep under 20 lines.
```

Pass just the accepted decisions that affect that slice. Supply documentation
links or paths plus a short finding; the worker fetches what it needs. Tools such
as Context7 may differ between workers; use official docs as fallback and report
which source was checked. A worker's conclusion does not grant cloud authority.

Assign one writer per file. The coordinator keeps project/platform/account
choices, cloud actions and integration. Do not have workers generate into the
same destination or drive the same simulator/device concurrently. Use isolated
temporary projects for independent checks. With no workers available, do the
same bounded checks sequentially. Review artifacts and actual test output before
claiming completion; ask for a targeted follow-up instead of sending more history.
