---
name: orchestrate-feature
description: Implement or resume features and bug fixes in existing Android and Kotlin Multiplatform Kotlin Toolchain apps, using dependency tasks, bounded attempts, durable recovery, platform checks and independent review.
---

# Orchestrate a Kotlin app feature

Own the requested change in the current conversation through implementation,
applicable checks, repairs and review. A plan or successful dispatch is not a
finished feature. This skill edits existing applications; never run the bootstrap
generator against them. Read local AGENTS.md and preserve the app's architecture,
wrapper bytes, dependencies and unrelated work. Do not migrate Gradle projects;
explain that this workflow targets Kotlin Toolchain and leave their files intact.

Use the standalone `bin/bootstrap.jar` inside THIS skill with Java 17+.
Resolve it relative to this skill, not a sibling skill or source checkout.
No RoadTower, Python, Beads, cloud account or model API key is required.
The CLI is a cooperative journal/admission helper; the current agent executes work.

## Start or recover

1. Inspect the requested app, Git status/branch, local rules, project.yaml,
   module.yaml files and bootstrap.json if present. Identify platforms, affected
   callers and observable acceptance criteria. Read only relevant product sources.
   Use Context7 resolve-library-id then query-docs for library/API/CLI decisions
   when available; otherwise consult current official docs and report the fallback.
2. Inspect existing `.feature-runs/runs/*.json` and recover the run matching the actual
   task and branch, including active owners and artifacts. Do not pick the latest
   run just because it is active. Reconcile ambiguous prior effects before retries.
   Read [ledger operations](references/ledger.md) for the exact CLI contract.
3. For new work, record a compact goal, non-goals, acceptance, branch/worktree,
   platform checks and task dependencies. Build a plan with disjoint author scopes,
   a test task for behavioral work, and one final reviewer. Keep product/interface
   decisions ahead of dependent implementation. Use an existing plan when useful;
   OpenSpec is optional and existing artifacts remain authoritative.
4. Select finite shared attempt, repair, time and packet limits for the task.
   Suggested starting limits: 12 attempts, 6 repairs, 7200 seconds, 16384 packet
   bytes. Honor explicit user limits. `feature init` freezes the plan and limits;
   it does not create/overwrite application code. Keep state, packets and logs
   local; exclude `.feature-runs/` using the project's existing local ignore
   mechanism. Never put credentials or private user data in packets or evidence.

## Carry the change

- Read `feature status`, then claim a ready task BEFORE starting its work or
  dispatching a worker. Packets contain goal, requirements/interfaces, owned files,
  checks and reference paths, not a whole conversation. Include all changed source,
  config and tests in plan scopes; evidence fingerprints cover those scopes only.
- When managed subagents are available, delegate behavioral code and tests to
  separate authors, plus one independent final reviewer. Use fresh no-history
  workers (`fork_turns="none"` or equivalent) and inherit the user's configured
  model. Give the tester requirements/interfaces before implementation details.
  Pass the bundled [testing role guide](references/testing.md) to every tester;
  require a requirement-to-case map and actual applied skill paths in its result.
  Discover optional specialist skills from the current environment before naming
  them in packets; no-history workers may have a different catalog.
  Use separate worktrees/branches or disjoint scopes; the host integrates and runs
  checks on the combined candidate. A worktree does not narrow OS permissions.
  Record actual worker IDs and branches in packets/evidence immediately. Only the
  host writes the journal; workers return artifacts and actual check outcomes.
- Do not create or message user chats as a substitute for subagents. If delegation
  is unavailable, implement and check sequentially here. Record the authorship gap;
  self-review cannot satisfy independent review. Continue useful work, then report
  the missing reviewer instead of claiming reviewed completion. Never silently
  launch a paid nested CLI or change provider to obtain more capacity.
- For documentation/mechanical changes use direct validation, without invented
  behavioral tests. For behavioral work exercise outcomes, failure states and
  affected callers. Add slice review for concrete auth, payments, data migration,
  secrets or contract risks, without importing a service-wide audit process.
- Use the app's checked-in Kotlin Toolchain commands from project documentation
  and existing metadata. Read [platform verification](references/verification.md)
  when choosing checks. Capture real argv, exit code, log path and environment
  limitations. Run focused checks while implementing and the applicable selected
  checks on the combined candidate before final review. Generation is not a build;
  a build is not executed tests or device/UI acceptance.
- Settle each attempt with actual passed/failed/blocked evidence. Record findings
  and route repairs to the responsible author; preserve unaffected work. A repair
  needs a concrete reason and changed source or packet. Diagnose identical failures
  instead of repeating the same dispatch. Reserve time/attempts for final checks
  and review. Shared exhaustion stops new work, including parent-agent repairs;
  never reset the run, rename tasks or switch executor to bypass it.
- Review the actual combined diff and candidate, requirements and check receipts.
  Reviewer identity must differ from authors. Fix concrete findings, rerun affected
  checks and refresh source-bound test/review evidence after any source change.
  An evidence declaration is not proof by itself: inspect actual artifacts/logs.

## Finish or hand off

Use `feature finish --outcome completed` only when all requirements and applicable
checks pass and independent review approves the current candidate. It validates
journal transitions and source fingerprints; it cannot prove authorship, semantic
correctness or the truth of submitted evidence. Missing required platform checks
are gaps, not passes. Keep their task blocked; continue independent ready work.

After interruption, resume the same ledger and recorded owners; do not start a
competing writer or reset attempts. Read [recovery](references/recovery.md) before
replacing a worker, resolving unknown sends or stopping an incomplete run.

Report changed behavior, checks and actual review, remaining gaps and the run
path. Implementation readiness, PR creation, merge, deployment and store release
are separate outcomes. Respect existing authorization for external actions;
invoking this skill alone does not authorize merge, publishing or deployment.
