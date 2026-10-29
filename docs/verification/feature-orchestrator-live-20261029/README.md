# Live feature-orchestrator acceptance — 2026-10-29

Temporary app: `/private/tmp/kmp-orchestrator-live-20261029/OrchestratorProbe`.
Run: `counter-live`, branch `codex/live-feature-probe`. Runtime: the independently
copied `portable/orchestrate-feature/bin/bootstrap.jar`, version 3.2.0.
The raw journal and logs remain under that temporary root for inspection.

## Scenario and authors

Generated an Android + iOS + Web Kotlin Toolchain project using shared Compose
on all three targets, manual wiring, no networking/storage/navigation/Firebase.
Implemented a bounded immutable counter: steps 1/5, range 0..99, reset, one-use
undo, no-op history preservation and enabled-state rules in the shared UI.

Real managed agents were admitted before dispatch, with no inherited conversation:

| Role | Actual agent | Owned output |
| --- | --- | --- |
| Skill coverage audit | `/root/skill_coverage_audit` | `probe/skill-audit.md` |
| Independent initial tester | `/root/counter_test_author` | `shared/test/.../CounterStateTest.kt` |
| Developer | `/root/counter_code_author` | `App.kt`, `CounterState.kt` |
| Guided tester reassessment | `/root/counter_guided_test_author` | Requirement-to-case review, no further source edits |
| Final reviewer | Recorded in final review receipt | Read-only combined candidate and evidence review |

The tester received requirements/interfaces before implementation, did not read
production implementation and wrote seven behavioral/negative/invariant tests.
The developer did not read tests. The initial Wasm test command failed at compile
time because the new CounterState symbol was absent: a missing-symbol RED,
not a runtime assertion failure. The repair/reassessment packet added the newly
bundled testing role guide; the new tester mapped requirements to cases and found
no missing state-contract scenario. Host-owned platform checks supplied GREEN.

## Observed checks

| Check | Result |
| --- | --- |
| `./kotlin build --platform android` | Passed, debug APK built |
| `./kotlin test --platform android` | 8 passed, 0 skipped |
| `./kotlin build --platform iosSimulatorArm64` | Passed, Xcode host `Build Succeeded` |
| `./kotlin test --platform iosSimulatorArm64` | 8 passed, 0 skipped |
| `./kotlin build --platform wasmJs` | Passed |
| `./kotlin test --platform wasmJs` | 8 passed, 0 skipped in browser runner |
| Android emulator UI | 12 checked states passed; step/boundary/reset/undo/enabled rules |
| iOS Simulator UI | 33 interactions passed; step/boundary/reset/undo/disabled-state assertions |
| Chrome Web UI | 35 interactions passed; rendered state, boundary and no-op assertions |
| Repository `./kotlin do bundle` + `./kotlin test` | Passed; 77 tests, 0 skipped |
| Native Codex `skills/list` | Both standalone copies discovered and enabled |
| Native Codex `plugin/read` | Local 3.2.0 metadata and both prefixed skills resolved |
| Live journal rejection guards | Reinitialization, unchanged retry, self-review and premature completion rejected without state mutation |

The platform commands ran through the portable CLI's `verify` operation with
`DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer`. Exact logs are
`.bootstrap/{android,ios,web}-{build,test}.log` in the temporary app. The iOS
build includes the actual Xcode app-host build, not only shared compilation.
The original greeting test is included in each platform's eight executions.

The host added regression tests for discovery names/interface metadata, portable
reference closure, missing tester guidance, invalid behavioral role plans and
sequential-author fallback without self-approved review. These are filesystem
regressions; the separate native receipts establish actual local host discovery.
No model/thread/turn was started by the app-server discovery probe.

## Screens

All three show the verified final state Count 98, Step 5; Increment and Undo are
disabled. These images are actual platform screenshots.

[Android](android.png) · [iOS Simulator](ios.png) · [Web](web.png)

## Findings and limitations

- Added a focused portable tester playbook and a bounded tester packet contract.
  Workers report actual applied skill paths and requirement-to-case coverage.
- The Web Compose semantic bridge exposes role/name divs without disabled HTML
  attributes. DOM isEnabled alone was misleading. Verification used actual
  input/no-op behavior and rendered screenshots; this is not a complete Web
  accessibility audit.
- Immediate DOM snapshots can precede Compose rendering after input; state-based
  observations avoid interpreting this delay as failed behavior.
- The installed Xcode provides DeviceHub, and its iOS runtime supports iPhone Duo.
  Globally listed iPhone 17 was incompatible. A newly created Duo initially
  reported Data Migration Failed via bootstatus despite exit 0. Installation,
  launch, active-display screenshots and all subsequent UI interactions succeeded;
  the diagnostic was retained rather than treated as a green boot receipt.
- A closed/inactive foldable display initially produced a black screenshot.
  Selecting the actual temporary device and open posture exposed the app screen.
- Current host configuration selected Command Line Tools by default; the explicit
  DEVELOPER_DIR enabled full Xcode without changing the global selection.
- Agent usage metrics were not exposed; journal usage remains null, not zero.
- The live portable entrypoint predates the final link to the tester guide; the
  guided tester received that bundled guide explicitly. Final independent-copy
  tests and native discovery cover the linked entrypoint; this is not a fresh
  installation of every final documentation byte.
- Native local discovery/plugin read is not clean remote installation or proof of
  automatic intent routing in a newly installed user conversation. Store release,
  physical-device signing, production backend and publication were outside scope.

Independent final review approved the exact candidate without findings requiring
repair. The journal completed with five attempts and one repair; a subsequent
terminal reopen was rejected without changing its bytes. See the
[review](final-review.md) and [final receipt](final-receipt.json) for the actual
source fingerprint, limits, counts and retained limitations.
Temporary emulator/simulator processes are stopped after evidence capture; test
sources, journals, logs and recovery artifacts are retained.
