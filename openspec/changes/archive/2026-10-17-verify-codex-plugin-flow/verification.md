# Installed Codex plugin acceptance — 2026-10-17

Passed the local installed-plugin journey with **codex-cli 0.163.0** and plugin
**3.0.0**: marketplace registration → installation → native skill discovery →
terminal init / Codex agent generation → actual builds/tests → application launch.

## Installation and native discovery

Executed from the current local checkout:

```sh
codex plugin marketplace add /Users/maxsolonkevich/Work/Progects/Git/android-kmp-bootstrap-skills
codex plugin add bootstrap-projects@android-kmp-bootstrap-skills --json
codex plugin list --marketplace android-kmp-bootstrap-skills --json
```

The plugin was initially unregistered/uninstalled. Installation succeeded and the
list reports `installed: true`, `enabled: true`, version `3.0.0`. Repeating add
also succeeded; final discovery was repeated after that refresh. The requested
plugin remains installed and enabled for the user. Other plugins were not changed.

Installed root:
`/Users/maxsolonkevich/.codex/plugins/cache/android-kmp-bootstrap-skills/bootstrap-projects/3.0.0`.

A fresh real `codex app-server` was initialized and queried with `skills/list`,
`cwds: ["/tmp/bootstrap-codex-flow/workspace"]`, `forceReload: true`. That workspace
is outside the author's repository. Native discovery returned exactly one enabled
bootstrap skill, named `bootstrap-projects:bootstrap-project`, with a cached
`skills/bootstrap-project/SKILL.md` path and no discovery errors.

The cached JAR is byte-identical to the repository's delivered skill JAR:
SHA-256 `10825753c9cf334bb556e45364b8531d0a028f471972fe5483655163a5449ffd`.
All bootstrap commands below used that cached JAR, not the source checkout.

Evidence: [install](evidence/plugin-install.json), [list](evidence/plugin-list.json),
[native discovery](evidence/discovery.json).

## Terminal wizard

Used a real PTY, recorded with macOS `script`, from a temporary workspace:

```sh
java -jar /Users/maxsolonkevich/.codex/plugins/cache/android-kmp-bootstrap-skills/bootstrap-projects/3.0.0/skills/bootstrap-project/bin/bootstrap.jar init '/tmp/bootstrap-codex-flow/Wizard App'
```

Selected `CodexWizard`, `dev.bootstrap.codex.wizard`, universal Compose on all four
platforms, iOS 16.0, Koin, Navigation 3, Ktor/default HTTPS endpoint, Settings and
Codex instructions. Observed compatibility descriptions, concrete review and the
full details/file plan. Deliberately changed UI to SwiftUI while keeping desktop,
web and Navigation 3; create was blocked with both compatibility explanations.
Correcting UI to shared Compose retained all other answers.

`save` wrote [wizard.json](evidence/wizard.json) outside the destination and exited
without creating a project. A second PTY run used `create`, emitted 43 files and
reported build/verification `NOT_RUN`. Its normalized spec matched the saved
config exactly. A separate locked config replay produced 43 byte-identical files.

Cancellation left no output. A non-TTY init exited 1 immediately with structured
`interactive_required` guidance and no output. Repeated generation into both
created projects exited 1, preserving every source/config file (43 and 23 files).

Evidence: [review/edit/save transcript](evidence/init-main.log),
[create transcript](evidence/init-create.log), [cancel](evidence/init-cancel.log),
[guards and locked replay](evidence/guards.json).

## Real Codex agent journey

Started a fresh, ephemeral `codex exec` invocation in a second temporary workspace,
using the skill's default `$bootstrap-project` invocation with a fully specified,
authorized acceptance request. It discovered/read the installed skill and its
reference, invoked the cached JAR's catalog, wrote the config outside the output,
ran plan, presented the file/stack scope, and generated the app. It did not write
application source independently, run builds or call the demo endpoint.

Output: `/tmp/bootstrap-codex-flow/Agent App`, `CodexAgent`,
`dev.bootstrap.codex.agent`, web-only KMP/shared Compose with Koin, Navigation 3,
Ktor, Settings and Codex instructions. Plan valid, zero issues, 23 files generated,
build status truthfully `NOT_RUN`. All test invocation commands exited 0.

Evidence: [actual request](evidence/agent-request.txt),
[completed Codex commands/messages](evidence/agent-transcript.json),
[result](evidence/agent-result.md), [config](evidence/agent.json).

The local user's unrelated Idealista MCP configuration reported HTTP 403 during
Codex startup. Bootstrap has no MCP dependency and completed its entire route;
no unrelated connection settings were changed.

## Actual builds, tests and launch

Used the installed JAR's `verify` on both outputs. The first universal check used
the host's default Command Line Tools selection: Android/JVM/Wasm passed and two
iOS checks were explicitly skipped, exit 2. Retained that
[initial report](evidence/wizard-verify-initial.json). Repeated the full verifier
with `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer` and
`ANDROID_HOME=/tmp/bootstrap-android-sdk`; global `xcode-select` was not changed.

| Output | Final build/test results | Passed generated test executions |
|---|---|---|
| Wizard App | Android, iOS Simulator ARM64/Xcode, JVM, Wasm: all 8 checks PASS; exit 0 | 31: Android 7, iOS 8, JVM 8, Wasm 8 |
| Agent App | Wasm build and browser tests: both checks PASS; exit 0 | 8 |

The output paths intentionally contain spaces. This includes a real successful
Xcode application build from `Wizard App`, not merely shared Kotlin compilation.
Also reran the repository's existing **56 CLI regressions**, all passed. No
generator/manifest/template changes were required; delivered binary bytes and
versions are unchanged. Final totals: **5 build checks, 5 test checks, 39 generated
test executions, 56 CLI regressions**.

Started both actual applications using their generated wrappers:

```sh
# In Agent App
./kotlin run -m webApp --port 8439 --no-open-browser
# In Wizard App
./kotlin run -m webApp --port 8440 --no-open-browser
```

Both applications opened in real Chrome tabs. `CodexWizard: 0` was visible after
terminal init/create. In the agent-created app, Home 0 → Increment twice →
Details 2 → Increment → Back → Home 3 succeeded. Fetch returned real HTTPS JSON
with UUID `e66af881-8373-4361-a24d-88888a808dab`. Reload restored 3; closing the tab
and reopening the same origin also restored 3. No mocked transport or injected
application state was used. Some Compose accessibility snapshots lagged one frame;
screenshots confirmed each transition and the eventual accessibility state.

Evidence: [wizard verifier](evidence/wizard-checks/verification.json),
[agent verifier](evidence/agent-checks/verification.json),
[CLI regressions](evidence/cli-tests.log),
[wizard launched](evidence/wizard-web-started.png),
[agent HTTP](evidence/agent-web-network.png),
[reload](evidence/agent-web-reloaded.png),
[reopened](evidence/agent-web-reopened.png). Per-command logs accompany the reports.

## Result and scope

No functional generator, installed discovery or skill-path defect was found.
Fixed the incomplete [installation guide](../../../../docs/INSTALL.md): concrete
marketplace/install/list commands, cached JAR path, namespaced invocation, fresh
conversation discovery, terminal review/save/create semantics, non-TTY config
flow and a concrete Xcode environment example. The terminal menu and chat workflow
remain thin wrappers over the same Kotlin generator; no additional runtime was
introduced. The setup matches the official
[local plugin packaging model](https://developers.openai.com/plugins/build/plugins).

This verifies the current **local checkout** in Codex CLI/native app-server and
a real fresh Codex model invocation. Public directory publication, remote Git
installation of unreleased changes, a desktop app restart/GUI install dialog,
and other agent products were not tested in this change. Current-chat discovery
is not retroactive; use a fresh Codex conversation for the newly installed skill.
Missing-choice chat elicitation was not tested; the agent request specified all
choices. Actual UI launch here was browser Wasm for both flows; other-platform UI
coverage remains in the earlier [runtime acceptance](../2026-10-17-verify-runtime-ui/verification.md).

Strict OpenSpec validation and `git diff --check` passed. Test tabs and local web
servers were stopped after evidence capture; generated fixtures remain under
`/tmp/bootstrap-codex-flow` for reproduction.
