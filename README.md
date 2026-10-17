# Android & KMP Bootstrap

Create Android, iOS, desktop JVM and browser Wasm apps from a conversation in
Codex or Claude Code. Choose shared Compose UI or native SwiftUI, add the
components you need, review the file plan and generate a runnable starter.

The plugin ships one portable `bootstrap-project` skill and a ready-to-run
Kotlin/JVM generator. Generated apps use pinned Kotlin Toolchain wrappers.

## Install once

You need your agent, Git and a **Java 17+ JDK** available as `java`.
The plugin includes the compiled generator and its assets.

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

Open a **new conversation** in the workspace where you want to create the app.
You can select **Android & KMP Bootstrap** in Codex or invoke the skill directly:

| Agent | Send in chat |
|---|---|
| Codex | `$bootstrap-projects:bootstrap-project` |
| Claude Code | `/bootstrap-projects:bootstrap-project` |

The skill asks for project basics. You can also include your complete request
after the invocation, or use the examples below in ordinary chat.
See [installation details and troubleshooting](docs/INSTALL.md) for terminal
installation in Claude Code and manual portable installation.

## Examples to paste into chat

Requests can be written in your usual language. Include the project name,
package, platforms and destination to avoid follow-up questions about basics.
All destinations below are new directories relative to the agent's workspace.

### Start with the defaults

```text
Use bootstrap-project to create Aurora in ./Aurora for Android and iOS.
Package: com.example.aurora. Use the default shared Compose setup.
```

This creates a minimal shared UI starter with manual dependency wiring and no
optional navigation, networking or storage. If you are still choosing a stack:

```text
Use bootstrap-project to help me choose a setup for a new mobile app.
Ask me about the project basics and explain shared Compose versus SwiftUI.
```

### Android only

```text
Use bootstrap-project to create Notes in ./Notes, package com.example.notes.
Use the native Android recipe with Jetpack Compose, Koin and Navigation 3.
No networking or local storage.
```

### Android and iOS with shared UI and data

```text
Use bootstrap-project to create AuroraMobile in ./AuroraMobile,
package com.example.auroramobile. Target Android and iOS with shared Compose UI,
Koin, Navigation 3, Ktor networking and Settings key-value storage.
```

Ktor includes a sample HTTPS action; Settings includes a persistent counter.
These are starter integrations to adapt to your product.

### SwiftUI on iOS, Compose on Android

```text
Use bootstrap-project to create NativeMobile in ./NativeMobile,
package com.example.nativemobile. Share Kotlin logic between Android and iOS.
Use SwiftUI on iOS and native Compose on Android, Koin and platform-native
navigation. Set the iOS minimum to 16.0. No networking or storage.
```

This selects SwiftUI NavigationStack on iOS and Navigation 3 on Android.
You can also request iOS alone. SwiftUI recipes support iOS and optional Android;
desktop and web use the shared Compose recipe.

### Desktop and browser

```text
Use bootstrap-project to create Workbench in ./Workbench,
package com.example.workbench. Target desktop JVM and browser Wasm with shared
Compose UI and Settings storage. Keep manual DI and no navigation or networking.
```

### All four platforms

```text
Use bootstrap-project to create Everywhere in ./Everywhere,
package com.example.everywhere. Target Android, iOS, desktop and web with shared
Compose UI, Koin, Navigation 3, Ktor and Settings.
```

The skill generates project scaffolding and sample functionality. A project name
such as Notes does not generate a complete notes product or its backend.

## What happens after the request

1. The agent resolves the project basics and the components you requested.
   Unspecified optional components use the generator's defaults.
2. It validates compatibility and shows the selected stack, modules, file plan
   and build requirements. You can adjust choices before generation.
3. It generates the app in an absent or empty destination. Existing project
   files are never overwritten or merged.
4. It runs build/test checks when requested and reports their actual results.
   File generation alone does not establish that the app builds or runs.

For example, follow up with:

```text
Build and test the desktop and browser targets of Workbench.
Report which checks passed, failed or were skipped, with their logs.
```

The agent handles CLI paths and configuration files. Direct terminal and CI
usage is documented in [installation](docs/INSTALL.md#terminal-wizard-and-ci)
and [configuration](docs/CLI-CONFIG.md).

## Supported choices and defaults

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
| [Install and start](docs/INSTALL.md) | Agent installation, discovery troubleshooting, portable copy, direct JAR usage |
| [Terminal wizard](docs/BOOTSTRAP-WIZARD.md) | Recipes, review, editing choices, saving and cancellation |
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
