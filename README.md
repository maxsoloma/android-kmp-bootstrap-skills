# Android & KMP Bootstrap

A Kotlin/JVM bootstrap CLI with a terminal wizard and portable agent workflow.
Creates Kotlin Toolchain projects for Android, iOS, desktop JVM and browser Wasm.
Claude Code and Codex plugins discover the same `bootstrap-project` skill.

## Start

Install from GitHub without cloning this repository:

```sh
codex plugin marketplace add maxsoloma/android-kmp-bootstrap-skills
codex plugin add bootstrap-projects@android-kmp-bootstrap-skills
```

Claude Code:

```text
/plugin install bootstrap-projects --marketplace maxsoloma/android-kmp-bootstrap-skills
```

Start a fresh agent conversation and select **Android & KMP Bootstrap**. For a
terminal wizard, use the installed skill's JAR as described in [installation](docs/INSTALL.md).

```sh
java -jar skills/bootstrap-project/bin/bootstrap.jar init /path/to/Aurora
```

The wizard selects platforms, UI, DI, navigation, networking, local storage and
agent instructions. Review the file plan before `create`; `save` writes only the
config. Existing files are never overwritten. Agents and CI use the same contract:

```sh
java -jar skills/bootstrap-project/bin/bootstrap.jar catalog --json
java -jar skills/bootstrap-project/bin/bootstrap.jar plan --config recipe.json --output /path/to/Aurora
java -jar skills/bootstrap-project/bin/bootstrap.jar generate --config recipe.json --output /path/to/Aurora
```

```json
{"name":"Aurora","package":"com.example.aurora","targets":["android","ios","desktop","web"],"di":"koin","navigation":"navigation3","network":"ktor","storage":"settings"}
```

## Stack choices

| Choice | Supported options |
|---|---|
| UI | Shared Compose; native Android Compose; shared Kotlin logic with SwiftUI on iOS and Compose on Android |
| DI | Manual composition or Koin |
| Navigation | None; Navigation 3 for Compose; SwiftUI NavigationStack with Android Navigation 3 |
| Network | None or Ktor, with per-platform engines, timeout and cancellation |
| Storage | None or Settings: SharedPreferences, NSUserDefaults, JVM file, browser localStorage |

SwiftUI supports iOS alone or Android+iOS. NavigationStack requires iOS minimum
16.0+. Shared Compose supports every nonempty subset of the four platforms.
Networking is user-triggered; generated tests use mock transports. Settings is
for small non-secret key-value data. Modern SQL/DataStore/Retrofit/Hilt recipes
are not implemented. Catalog/plan explains compatibility before writing.

## Build generated apps

```sh
./kotlin build
./kotlin test
./kotlin run -m desktopApp
```

Use `--platform android|iosSimulatorArm64|jvm|wasmJs` for host-specific checks.
iOS requires macOS, full Xcode and an installed simulator runtime for tests.
Toolchain provisions other build tools/dependencies on first use. A global Kotlin
CLI is not required. Generated specs/locks replay only with a matching generator.
`bootstrap verify PROJECT` records separate build/test results in `.bootstrap/`.

## Develop this repository

See [build instructions](docs/BUILD.md), [configuration](docs/CLI-CONFIG.md),
[stack choices](docs/STACKS.md), [data integrations](docs/DATA.md),
[terminal wizard](docs/BOOTSTRAP-WIZARD.md) and [installation](docs/INSTALL.md).

Version 3 removes the Gradle backend and hardened profiles/skills. Old generated
projects are left untouched; use their matching old release to reproduce them.
Prior OpenSpec changes retain the migration history; retired templates and golden fixtures have been removed.

The Toolchain default passed the real 48-stack matrix plus five platform-only
projects: 134 builds and 134 test runs with 640 successful test executions.
See [acceptance evidence](openspec/changes/archive/2026-10-17-migrate-kotlin-toolchain/verification.md)
for exact versions, commands, fixes and runtime limits.

Real UI acceptance also passed navigation, live HTTPS and restart persistence on
Android emulator, iOS simulator (Compose and SwiftUI), desktop and browser Wasm.
It found and fixed a missing Compose compiler on the KMP Android host. See the
[runtime report and screenshots](openspec/changes/archive/2026-10-17-verify-runtime-ui/verification.md)
for the seven tested combinations and remaining coverage limits.

The installed Codex plugin also passed marketplace installation, native skill
discovery, terminal init, agent generation and generated-app launch. See the
[Codex flow report](openspec/changes/archive/2026-10-17-verify-codex-plugin-flow/verification.md)
and [installation commands](docs/INSTALL.md).
