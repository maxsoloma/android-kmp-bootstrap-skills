# Resume without duplicating effects

Read the matching run JSON, task plan, latest attempts, owner/worker identities,
branch/worktree references and evidence before doing more work. Inspect current
Git status and sources: saved prose is not current source evidence. `status`
reports ready dependency tasks and a fresh source fingerprint; it does not dispatch.

A running attempt remains reserved after a crash. Query its recorded worker or
process and inspect its outputs first. If the dispatch response was lost, preserve
that uncertainty; do not spawn a duplicate. Once observed inactive, the host may
settle it blocked/failed using its recorded owner and explicit recovery evidence.
That is attribution to the original attempt, not permission to impersonate an
active author. Never steal a claim or delete a lock/state file to unblock work.

Write a compact handoff containing run ID/path, project/branch/worktree, acceptance
remaining, source fingerprint, worker IDs, attempt/repair totals and deadline,
findings, artifact/check paths, unknown usage and the concrete next action.
Continue independently actionable ready requirements before ending for a blocker.
Settle stopped attempts, then finish blocked when no authorized progress remains.

Terminal records are immutable through the CLI. After budget exhaustion stop and
report remaining work. Additional allowance requires an explicit user decision;
this version has no grant/successor command. Do not create a replacement run to
reset spend. A later separately authorized stage may have a new plan, referencing
the previous run and its full attempt history in the handoff; do not present it as
an automatic budget extension or resume. New CLI versions reject old journals;
retain the original installed skill/JAR to inspect/recover with its matching version.

The journal limits cooperative attempts/repairs/time/packet size only. It neither
intercepts arbitrary tools nor hard-caps model inference, subscription quota or
money. Usage is null when unavailable and is not zero. Preserve actual tool-reported
usage in evidence when provided; this version does not aggregate token telemetry.
No token allowance, ledger record or task status grants external-action authority.
