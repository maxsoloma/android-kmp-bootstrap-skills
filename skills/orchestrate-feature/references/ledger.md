# Standalone feature journal CLI

Resolve `JAR` to this skill's `bin/bootstrap.jar`. All operations require Java 17+
and an existing Kotlin Toolchain project (project.yaml and kotlin or kotlin.bat).
The CLI never generates app files, launches agents, executes commands from plans,
creates Git branches or contacts a service. The host performs those operations.

## Plan and initialize

Write plan.json outside the app or inside its locally excluded .feature-runs area.
Example behavioral plan (replace paths and commands with the actual app):

```json
{
  "goal": "Persist the selected theme and restore it on launch",
  "kind": "behavioral",
  "limits": {"attempts": 12, "repairs": 6, "seconds": 7200, "packet_bytes": 16384},
  "tasks": [
    {
      "id": "theme-code", "role": "developer", "depends_on": [],
      "files": ["shared/src"],
      "acceptance": ["Restore selection on launch; expose storage errors"],
      "checks": ["Compile affected shared code"]
    },
    {
      "id": "theme-tests", "role": "tester", "depends_on": ["theme-code"],
      "files": ["shared/test"],
      "acceptance": ["Exercise restart and storage failure from requirements"],
      "checks": ["./kotlin test --platform jvm"]
    },
    {
      "id": "final-review", "role": "reviewer", "depends_on": ["theme-tests"],
      "files": ["shared/src", "shared/test"],
      "acceptance": ["Review combined change against requirements and check evidence"],
      "checks": ["Inspect current diff and test logs"]
    }
  ]
}
```

`kind` is behavioral or mechanical; roles are developer, tester, mechanical,
reviewer. Behavioral plans need developer and tester tasks; every plan needs
exactly one final reviewer. Mechanical plans can use mechanical + reviewer.
Dependencies must exist and be acyclic. All fields shown are required, unknown
plan/task/limit fields fail. IDs use lowercase letters, digits and hyphens, max
64 characters. Limits are positive integers; they apply to the entire run.

`files` declares source ownership AND fingerprint coverage. Include all affected
shared/platform modules, tests, project config, dependencies and relevant source
documents, including existing files read by checks. Use repository-relative files
or directories. Existing and future files under declared directories are hashed.
Absolute, root-relative and drive-qualified paths, dot/parent components,
symlinks and reserved cache/state scopes are rejected. Plan files are not an OS sandbox. All scopes contribute to one
candidate hash; project.yaml, kotlin, kotlin.bat and bootstrap.json are included
automatically. Anything outside that coverage requires host diff review and a
properly scoped plan before implementation; do not silently expand work.

```sh
java -jar "$JAR" feature init /path/to/App --run theme --plan /path/to/plan.json
java -jar "$JAR" feature status /path/to/App --run theme
```

State is `.feature-runs/runs/theme.json`; writes are atomic under a project-wide kernel
lock. The plan and limits are stored once. Reinitializing the same ID fails. The
project directory/version must match on every read. Keep a local ignore rule for
.feature-runs, and put packets/check logs there so they cannot alter candidate
source hashes. Never include that directory in source scopes.

## Admit and settle

Write a bounded worker packet with requirements, interfaces, owned files, commands
and reference paths. Record branch/worktree, role and actual worker ID after
launch in a separate artifact referenced by settlement evidence. The host claims
before work/dispatch, then binds the returned worker by recording it immediately.
A lost response leaves a running claim to reconcile, not permission to retry.

```sh
java -jar "$JAR" feature claim /path/to/App --run theme --task theme-code \
  --owner developer-1 --packet /path/to/code-packet.md
```

Dependencies must pass. Active overlapping scopes across runs in the same project
block admission. Worktrees have separate journals; the host must also ensure
ownership across worktrees/repositories. A reviewer starts only after all other
tasks pass and must have a different owner from all recorded authors. Names are
host assertions, not authenticated identities; verify actual worker separation.

After doing the work and actual checks, write evidence JSON. A passed attempt
requires a nonempty summary and at least one passed check with command and log
references. Keep actual exit codes, worker identities, artifacts and risks as
additional evidence fields. Failed/blocked attempts require a summary; include
failed/skipped check records and findings. For review, add `"verdict":"approved"`
only after genuine independent review; reviewer source must remain unchanged.

```json
{
  "summary": "Theme restore and storage failure checks passed",
  "worker_id": "actual-managed-worker-id",
  "checks": [
    {"command": "./kotlin test --platform jvm", "status": "passed",
     "exit_code": 0, "log": "/path/to/App/.feature-runs/theme-tests.log"}
  ]
}
```

```sh
java -jar "$JAR" feature settle /path/to/App --run theme --task theme-code \
  --owner developer-1 --outcome passed --evidence /path/to/evidence.json
```

Allowed settlement outcomes: passed, failed, blocked. Only a running attempt's
recorded owner can settle it. The CLI stores the evidence object and its file
hash/path plus candidate source hash; it does not execute or authenticate evidence
commands/logs. The host must read logs and verify reported outcomes. Test/check
and review evidence must cover the exact integrated candidate, not worker-only
branches. Unknown usage stays null; extra measured usage may be kept in evidence.

## Repair, complete, stop

Reclaim a settled task with a nonempty `--reason` and changed packet or source.
Every reclaim consumes the same global attempts and repairs, even if it was
previously passed. An identical task/source/packet retry is rejected. Fix the
cause before requesting another review. Retain prior attempts and findings.

```sh
java -jar "$JAR" feature claim /path/to/App --run theme --task theme-code \
  --owner developer-2 --packet /path/to/fix-packet.md --reason 'Fix reported restart regression'
```

The host propagates source changes to affected downstream tasks and reruns checks.
Completion requires every task's latest attempt passed, no active attempts, and
current tester/reviewer source fingerprints. Reclaim stale test/review tasks with
a reason and remaining allowance. The journal cannot determine which unrecorded
check is required or detect omitted source scope; final host diff review remains
necessary. No command grants approval or raises limits.

```sh
java -jar "$JAR" feature finish /path/to/App --run theme \
  --outcome completed --evidence /path/to/final-handoff.md
```

To stop after a genuine blocker/exhaustion, reconcile and settle active workers,
write remaining acceptance and next actions in the handoff, and finish with
`--outcome blocked`. `status` and settlement remain possible after the admission
deadline; terminal records cannot mutate. Exit 0 means the requested journal
operation succeeded, not that app tests passed. Invalid operations exit 1.
