# Implement features with the Kotlin orchestrator

Version 3.2 adds `orchestrate-feature` beside the project creation wizard. It is
for an existing Android/KMP Kotlin Toolchain app, including an app created
elsewhere. It reads the app's actual modules, architecture and instructions;
bootstrap.json is optional. Gradle migration is outside this workflow.

## Ask for the outcome

Open the existing app workspace and send, for example:

```text
$bootstrap-projects:orchestrate-feature
Add a theme selector. Persist the choice, restore it on launch, and show storage
errors. Check desktop and browser behavior; preserve the current navigation.
```

Claude Code uses `/bootstrap-projects:orchestrate-feature`. For a manual portable
copy, use `$orchestrate-feature` or `/orchestrate-feature` without the plugin prefix.
A bug-fix request works the same way:

```text
Use orchestrate-feature to fix the duplicated request when returning to Details.
Cover cancellation and navigation regression; keep the existing Ktor setup.
```

The agent does the work, including the journal operations. No JSON or terminal
commands are required from the user. It asks only for consequential missing
product decisions, rather than permission at each ordinary stage.

## What happens

1. Inspect the app, current branch/diff and existing work; recover an existing
   matching run if this is a continuation.
2. Define observable acceptance and a dependency graph. Record affected modules,
   files, checks, non-goals and actual risks. Shared interfaces precede consumers.
3. Initialize a durable run with finite shared attempts/repairs/time/packet limits.
4. Claim ready tasks and implement with appropriate tests. With managed subagents,
   separate code and test authors get small scoped packets. The current host
   integrates outputs, validates them and owns the journal.
5. Run affected checks on the combined candidate, diagnose failures and make
   bounded repairs. Get one independent final review after checks pass.
6. Finish with actual check/review evidence or a concrete blocked handoff. A saved
   plan, dispatch, scaffolding or CLI success is not app acceptance.

For mechanical/documentation work, use direct validation. Sensitive changes add
review of the affected risks. There is no mandatory cloud runtime, central
service registry, database, paid API executor or new user chat.

If managed subagents are absent, the host can implement and test sequentially,
reporting the missing independent authorship/review. It cannot label its own
review independent or finish a reviewed run by inventing another owner name.

## Limits and recovery

The portable Kotlin/JVM CLI provides `feature init`, `status`, `claim`, `settle`
and `finish`. Its [exact contract and JSON example](../skills/orchestrate-feature/references/ledger.md)
are shipped with the skill. Every invocation reads persisted state; no daemon or
background scheduler is needed.

The state file `.feature-runs/runs/<run-id>.json` stores the immutable plan/limits,
attempts, owners, outcomes, packet hashes, source fingerprints and evidence.
Use a local Git exclude for this directory; keep packets/logs there or outside
source scopes. It is project-local, so do not move a run between worktrees.
Record worker worktree/branch IDs and integrate into the host candidate.

The journal checks acyclic dependencies, exclusive active scopes in one project,
shared attempt/repair caps, admission deadline and packet size. Defaults suggested
by the skill are 12 attempts, 6 repairs, 2 hours and 16 KiB per packet; the host
selects finite values appropriate to the request and respects user-supplied limits.
Retries need changed source or packet plus a concrete reason. Read/settle/blocked
handoff remain possible after exhaustion; new work cannot bypass it in the host.

To resume, ask: `Continue the theme feature from its saved run.` The agent reads
actual state and checks existing worker outputs before replacing a stopped worker.
Unknown dispatch effects remain unresolved until reconciled. It retains prior
attempts and does not reset allowance after interruption. Completed/blocked runs
are terminal; there is no automatic allowance grant or terminal reset command.
Keep the matching skill/JAR version for unfinished runs, since version mismatch
fails explicitly. See [recovery](../skills/orchestrate-feature/references/recovery.md).

## Evidence and practical boundaries

The candidate hash includes declared source files/directories (including dirty,
new and deleted files) plus project.yaml, wrappers and bootstrap.json. Declare all
affected source/config/test inputs. Tester and reviewer evidence must match the
final candidate; a review cannot modify its candidate. Final host diff inspection
must catch changes outside declared scope and missing check coverage.

The CLI verifies state transitions, references and hashes. Evidence is a host
assertion: it cannot authenticate authors, prove log contents, enforce file
permissions or determine semantic correctness. It does not execute models/checks,
cap in-progress inference, measure billing or aggregate token telemetry. Unknown
usage stays null; real tool-reported counters can be retained in evidence.
Project-local scope checks do not coordinate separate worktrees or repositories.
The host must enforce that wider ownership and honor normal sandbox permissions.

Actual builds, executed unit/platform tests, UI/device checks and live integrations
are reported separately. A missing Android SDK, full Xcode, simulator or network
binding is a named acceptance gap. See the skill's
[platform guide](../skills/orchestrate-feature/references/verification.md).
PR creation, merge, deployment and store publication require the applicable
existing authority; completing a feature does not grant those actions.

## Design provenance

Adapted ideas from RoadTower's remote main, verified on 2026-10-29 at
[910aa869e298acaf37ec8c3a2468bf5015e4b04f](https://github.com/RoadTowerPlatformOrg/roadtower-workspace/commit/910aa869e298acaf37ec8c3a2468bf5015e4b04f).
The local RoadTower checkout was older; these six owning files were fetched at the
pinned remote commit and matched local Git blob hashes:

| Source path in RoadTower | Git blob |
| --- | --- |
| `.agents/skills/orchestrate-task/SKILL.md` | `83a7a84ac1a31aa663fb1c7c9d1bc3a4a8f0155e` |
| `docs/runbooks/ORCHESTRATION-PROCESS.md` | `475afad5dab425690fa72f2dde407bd15f97cffe` |
| `docs/runbooks/BOUNDED-ORCHESTRATION.md` | `563fd076e1621898c12537a22f9f7ca3019dea79` |
| `scripts/orchestration_session.py` | `e63bbd7d53c45a0a5660bd94645156ed00b144d3` |
| `scripts/orchestration_process.py` | `f9d926c0ef2e4ac32369e986c9f9533a26f8c956` |
| `scripts/orchestration_host.py` | `6963c40b66f2a2b9efb22961919f4f3fc58f433b` |

Transferred: current-session ownership, separate authors, compact task packets,
dependency order, bounded changed-input repairs, durable recovery and honest
candidate-bound checks/review. This is an independent Kotlin implementation of
those workflow ideas. RoadTower's Python executor, service bridges, private
tracking/policy bundle, model profiles, quota assumptions and cloud delivery are
not dependencies. Remote source links are provenance, not runtime requirements.

## Verification of this implementation

Local verification on 2026-10-29:

- `./kotlin do bundle`: passed; both skill JARs are byte-identical.
- `./kotlin test`: 77 tests passed, none skipped, including 13 feature-journal
  regressions, independent-copy skill discovery/reference checks and isolated
  execution of the copied orchestrator JAR.
- Standard skill-creator validation: both skills passed.
- `claude plugin validate` for plugin.json and marketplace.json: passed.
- Local documentation links, JSON metadata and `git diff --check`: passed.

A subsequent live acceptance run used a temporary shared-CMP app targeting only
Android, iOS and Web, real no-history managed code/test authors, a guided tester
and an independent final reviewer. All three platform builds and test runs passed
(8 tests per platform); Android emulator, iOS Simulator and Chrome UI interactions
were exercised separately. Native Codex skills/list discovered both independently
copied skills; plugin/read resolved both prefixed skill names at local version
3.2.0. These are local discovery receipts, not a fresh remote installation.
See [live verification evidence](verification/feature-orchestrator-live-20261029/README.md)
for exact stages, outcomes and limitations. No application templates or toolchain
pins changed, and no release was published by this verification.
