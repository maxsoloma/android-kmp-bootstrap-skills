# Android & KMP Bootstrap

Create Android, iOS, desktop JVM and browser Wasm apps, then implement features
in existing Kotlin Toolchain apps from a conversation in Codex or Claude Code.
For a new app, choose shared Compose UI or native SwiftUI, add components, review
the file plan and generate a runnable starter.

The plugin ships two portable skills: `bootstrap-project` for new apps and
`orchestrate-feature` for implementation in existing apps. Both include the
Kotlin generator CLI as sources with a launcher. Generated apps use pinned Kotlin Toolchain wrappers.

Selecting Koin DI installs the four [official Koin agent skills](https://insert-koin.io/docs/tooling/ai-agents/)
and Kotzilla MCP configuration in the generated project for the selected agents
(Codex, Claude Code, Gemini, Copilot or Cursor). Skills and their references are
bundled, so generation needs no Node.js, downloads or global agent changes.
Manual DI and `agents: ["none"]` do not install this tooling. Trust/reload the
project for discovery. Kotzilla requires OAuth login in the agent; runtime
measurements also require an explicitly configured SDK and sessions. Installing
skills does not change the Kotlin Toolchain backend or enable a compiler plugin.

## Install once

You need your agent and Git. **No Java installation is required**: the plugin
includes the generator sources and a pinned Kotlin Toolchain wrapper that
provisions what is missing on first use (about a minute once per machine) and
reuses an installed Java 25+. See [first run and Java reuse](docs/INSTALL.md#first-run-and-java-reuse).

**Codex — paste into a terminal:**

```sh
codex plugin marketplace add maxsoloma/android-kmp-bootstrap-skills
codex plugin add bootstrap-projects@android-kmp-bootstrap-skills
```

**Claude Code — paste into its chat:**

```text
/plugin marketplace add maxsoloma/android-kmp-bootstrap-skills
/plugin install bootstrap-projects@android-kmp-bootstrap-skills
```

## Start the step-by-step wizard

Open a **new conversation** in the workspace where you want to create the app.
Select **Android & KMP Bootstrap** in Codex or send just the skill invocation:

| Agent | Send in chat |
|---|---|
| Codex | `$bootstrap-projects:bootstrap-project` |
| Claude Code | `/bootstrap-projects:bootstrap-project` |

**No project description or arguments are required.** The skill starts a guided
conversation. You type only the project name and package; platforms, UI and
components are selected from the offered options. Optional Firebase connection
also selects a cloud project/account; creating a project can require its ID.

The agent uses native question forms when they are available in its current
mode. Otherwise, it shows the same choices as numbered chat menus: reply with
option numbers, including several numbers for platforms. The wizard stays in
chat; it does not require opening a terminal. Questions follow your language.

See [installation details and troubleshooting](docs/INSTALL.md) for terminal
installation in Claude Code and manual portable installation.

## Implement a feature in an existing app

Open the app workspace and describe the outcome with the new skill:

| Agent | Send in chat |
|---|---|
| Codex | `$bootstrap-projects:orchestrate-feature Add a persistent theme selector to this app.` |
| Claude Code | `/bootstrap-projects:orchestrate-feature Add a persistent theme selector to this app.` |

The agent inspects the existing architecture, defines acceptance and dependency
tasks, implements the change, runs affected platform checks and obtains independent
review when managed subagents are available. Code and tests use separate authors;
testers receive a bundled role guide and map requirements to behavioral cases.
Without subagents it can implement and check sequentially, while reporting the
missing independent review. The same workflow handles bug fixes and continuation:
`Use orchestrate-feature to continue the theme feature from its saved run.`

Each run has a local `.feature-runs/runs/<id>.json` journal with shared attempt, repair,
time and packet limits, ownership and source-bound evidence. Interrupted work
resumes the same run. Changed code invalidates test/review evidence; repeated
unchanged retries and exhausted starts are rejected. Keep journals and logs locally
excluded from Git. The agent manages the CLI; users do not need to prepare JSON.

This workflow targets existing Kotlin Toolchain apps. It preserves the selected
stack and wrapper, and does not regenerate or migrate an existing application.
The journal is cooperative: it does not launch models, authenticate authors,
verify submitted logs or enforce token/billing quotas. Completion, PR, merge and
release remain separate outcomes. See the [feature orchestration guide](docs/FEATURE-ORCHESTRATION.md)
for examples, recovery, limits and implementation details.

## What the wizard asks

| Step | Your input |
|---|---|
| 1. Name and package | Type e.g. `Aurora` and `com.example.aurora`. |
| 2. Platforms | Select one or more: Android, iOS, Desktop JVM, Browser Wasm. |
| 3. UI | Select a compatible option: native Android Compose, shared Compose, or SwiftUI on iOS with shared Kotlin logic. |
| 4. DI | Select Manual wiring or Koin. |
| 5. Navigation | Navigation 3 is recommended for Compose; None remains available. SwiftUI offers native navigation or None. |
| 6. Networking | Select None or Ktor. |
| 7. Local storage | Select None or Settings key-value storage. |
| 8. Firebase | None, Analytics + Messaging, or custom Analytics/Messaging/Remote Config. Connect a project now or prepare SDKs only. |
| 9. Review | Select Create project, Change choices or Cancel. |

Only compatible UI/navigation options are offered. When shared Compose is the
only UI compatible with your platforms, the wizard explains the selection and
continues. Single-select question tools collect platforms through Include/Exclude
choices rather than forcing a preset combination.

The destination defaults to `./<ProjectName>` in the calling workspace; the
application ID defaults to the package. SDK versions and additional agent
instructions use the generator defaults. Native SwiftUI navigation raises an
unspecified iOS minimum to 16.0 and shows it in review. With Ktor, the sample
HTTPS endpoint is supplied automatically. None of these fields requires typing
in the normal wizard. You can request advanced settings explicitly.

Each choice waits for your answer. A recommendation is not selected on your
behalf. Before generation, review shows the destination, selected stack, SDKs,
modules and file plan. Change choices revisits a step while retaining other
answers. Cancel creates no project. Existing project files are never overwritten;
an occupied default destination prompts a choice of an unused sibling directory.

## Example: create an Android and iOS app

Invoke the skill without arguments, then enter/select:

| Step | Example answer |
|---|---|
| Name and package | Type `AuroraMobile` and `com.example.auroramobile`. |
| Platforms | Android + iOS |
| UI | Shared Compose UI |
| DI | Koin |
| Navigation | Navigation 3 |
| Networking | Ktor |
| Local storage | Settings |
| Firebase | Analytics + Messaging, or None |
| Review | Create project |

The result is `./AuroraMobile` with shared Compose UI, a sample HTTPS action and
a persistent counter. These are starter integrations to adapt to your product.

Other examples use the same menus:

| App | Platforms | UI | DI / navigation / network / storage |
|---|---|---|---|
| Minimal mobile starter | Android + iOS | Shared Compose | Manual / None / None / None |
| Android starter | Android | Native Android Compose | Koin / Navigation 3 / None / None |
| Native mobile UI | Android + iOS | SwiftUI + Android Compose | Koin / Native navigation / None / None |
| iOS-only app | iOS | SwiftUI | Manual / None / None / None |
| Desktop and browser | Desktop JVM + Browser Wasm | Shared Compose | Manual / None / None / Settings |
| All four platforms | Android + iOS + Desktop JVM + Browser Wasm | Shared Compose | Koin / Navigation 3 / Ktor / Settings |

You can also supply choices in your initial request. The wizard keeps those
answers and asks only the remaining steps. Explicitly asking for defaults skips
the corresponding menus; JSON/config replay remains available for automation.

The skill generates project scaffolding and sample functionality, not a complete
product or its backend. Build/test checks run when requested, with actual
results reported separately from file generation. For example, after creating
a desktop+browser app named Workbench:

```text
Build and test the desktop and browser targets of Workbench.
Report which checks passed, failed or were skipped, with their logs.
```

The agent handles CLI paths and JSON. Direct terminal and CI usage is documented
in [installation](docs/INSTALL.md#terminal-wizard-and-ci) and
[configuration](docs/CLI-CONFIG.md).

## Supported choices and defaults

These are the generator defaults. The chat wizard asks you to confirm component
choices instead of silently applying their defaults.

| Choice | Supported options | Default |
|---|---|---|
| Platforms | Any nonempty subset of Android, iOS, desktop, web for shared Compose | Android + iOS |
| UI | Shared Compose; native Android Compose; SwiftUI with shared Kotlin logic | Shared Compose for KMP; Android Compose for native Android |
| DI | Manual wiring or Koin | Manual |
| Navigation | None; Navigation 3 for Compose; platform-native for SwiftUI | None |
| Network | None or Ktor with platform-specific engines | None |
| Storage | None or Settings-backed key-value data | None |
| Android SDK | Configurable minimum and target; compile SDK 37 | min 26, target 37, compile 37 |
| iOS minimum | 15.0+; SwiftUI NavigationStack requires 16.0+ | 15.0 |
| Agent instructions | Codex, Claude, Gemini, Copilot, Cursor or none | Codex + Claude |

Settings is intended for small non-secret values. SQL/Room, DataStore, Retrofit
and Hilt recipes are not implemented. Navigation 3 scaffolding does not include
browser history integration. Ktor uses a demo HTTPS endpoint unless you supply
your own; browser requests also depend on the server's CORS policy.
See [stack compatibility](docs/STACKS.md) and [data integrations](docs/DATA.md).

## What gets generated

A KMP project with all four targets has this structure; only selected app
modules are included:

```text
Aurora/
├── project.yaml
├── kotlin / kotlin.bat
├── shared/                 # Shared Kotlin logic and, for shared Compose, UI
│   ├── module.yaml
│   ├── src/                # Common code
│   ├── src@android/        # Platform adapters when needed
│   ├── src@ios/
│   ├── src@jvm/
│   ├── src@wasmJs/
│   └── test/               # Tests; platform tests use test@<platform>
├── androidApp/
├── iosApp/                 # Includes module.xcodeproj, scheme app
├── desktopApp/
├── webApp/
├── bootstrap.json          # Selected stack and verification commands
├── bootstrap.spec.json     # Portable generation recipe
├── bootstrap.lock.json     # Recipe and generated-content hashes
├── AGENTS.md
├── CLAUDE.md               # Included when Claude instructions are selected
└── README.md               # Commands for this specific project
```

Native Android uses one `androidApp` module. SwiftUI keeps shared Kotlin logic
UI-free and puts SwiftUI code in `iosApp/src`. The generator does not initialize
Git. Specs and locks reproduce a recipe with the matching generator version;
they do not migrate an existing application.

## Build, test and run your app

Run commands from the **generated project directory**, for example `cd Aurora`.
The first build downloads Kotlin Toolchain, dependencies and platform tools.
Use the checked-in wrapper; its version is pinned. On Windows PowerShell,
replace `./kotlin` with `.\kotlin.bat`.

Build and test all selected targets on a host with the required tools:

```sh
./kotlin build
./kotlin test
```

To check only one platform, use `./kotlin build --platform PLATFORM` and
`./kotlin test --platform PLATFORM`:

| Target | `PLATFORM` | Run command |
|---|---|---|
| Android | `android` | `./kotlin run -m androidApp` |
| iOS simulator | `iosSimulatorArm64` | `./kotlin run -m iosApp --platform iosSimulatorArm64` |
| Desktop JVM | `jvm` | `./kotlin run -m desktopApp` |
| Browser Wasm | `wasmJs` | `./kotlin run -m webApp` |

Use commands only for modules selected in your recipe. Android runs need a
connected device or running emulator. Use `--device-id ID` to select an Android
device or iOS simulator. The web command starts a local server and opens a
browser; `--port 8081 --no-open-browser` changes this behavior.

For iOS, use macOS with full Xcode, its first-launch setup and an installed
simulator runtime. Generated iOS targets are ARM64 device and simulator targets.
Device signing requires your own development team. Build checks, unit/browser
tests and device UI interaction are separate verification steps. See
[platform requirements](docs/INSTALL.md#build-requirements) and your generated
README for Xcode-specific commands.

## More documentation

| Guide | Contents |
|---|---|
| [Install and start](docs/INSTALL.md) | Agent installation, discovery troubleshooting, portable copy, first run and Java reuse, direct launcher usage |
| [Chat and terminal wizards](docs/BOOTSTRAP-WIZARD.md) | Menu flow, review, editing choices, saving and cancellation |
| [Feature orchestration](docs/FEATURE-ORCHESTRATION.md) | Implement/resume features, dependencies, bounded attempts and review |
| [Configuration and CLI](docs/CLI-CONFIG.md) | JSON fields, defaults, plan/generate commands and locked replay |
| [Application stacks](docs/STACKS.md) | UI, platforms, DI, navigation and compatibility |
| [Networking and storage](docs/DATA.md) | Ktor engines, Settings adapters and runtime behavior |
| [Build this repository](docs/BUILD.md) | Bundle the CLI, run tests and the integration matrix |
| [Architecture](docs/MODERNIZATION.md) | Generator, resources, wrappers and versioning |
| [Release](docs/RELEASE.md) | Packaging and release verification |

Version 3 uses Kotlin Toolchain exclusively. Gradle generation and hardened
profiles were retired; use the matching old release for existing old recipes.

## Verification evidence

The recorded 3.0 acceptance runs cover:

- [48-stack matrix and platform-only projects](openspec/changes/archive/2026-10-17-migrate-kotlin-toolchain/verification.md): 134 builds, 134 test runs and 640 successful test executions.
- [Runtime UI checks](openspec/changes/archive/2026-10-17-verify-runtime-ui/verification.md): navigation, live HTTPS and restart persistence on emulator, simulator, desktop and browser, with exact coverage limits.
- [Installed Codex plugin flow](openspec/changes/archive/2026-10-17-verify-codex-plugin-flow/verification.md): installation, discovery, wizard, agent generation and app launch.

See the linked reports for exact versions, commands and environment limitations.

[MIT license](LICENSE) · [Third-party notices](THIRD_PARTY_NOTICES.md) · [Privacy](PRIVACY.md)
