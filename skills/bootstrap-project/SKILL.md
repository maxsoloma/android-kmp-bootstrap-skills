---
name: bootstrap-project
description: Create Android and Kotlin Multiplatform projects through chat menus for platforms, Compose or SwiftUI, Navigation 3 and optional mobile Firebase setup, using a portable Kotlin Toolchain generator.
---

# Bootstrap Kotlin Toolchain projects

Use this skill's standalone `bin/bootstrap.jar` with Java 17+. Resolve paths
relative to this skill directory; do not require repository sources or sibling
skills. No Python or global Kotlin CLI is needed. Claude and Codex use the same
CLI contract. Discover supported choices instead of inventing integrations.

## Start the chat wizard on invocation

A bare skill invocation starts the guided menus immediately; do not ask the user
to write a project specification or run the terminal wizard. Read
[the chat wizard](references/chat-wizard.md) and follow it for interactive creation.
For local generation only project name and package require free-text input.
Optional Firebase connection also needs the chosen cloud project/account. Default the destination
to `<calling workspace>/<project name>` and the application ID to the package.
Use the agent's available native question/choice tool in its current mode;
otherwise present one numbered menu at a time in chat. Wait for each answer.

Offer explicit choices for platforms, UI, DI, navigation, networking, storage and Firebase;
recommendations are not submitted answers. Do not silently default these steps.
Honor choices already supplied in the request, and skip only answered steps or
choices the user explicitly delegated to defaults. Config/replay/CI requests can
use the direct CLI flow without an interactive wizard.

## Agent flow

1. Run `java -jar <skill>/bin/bootstrap.jar catalog --json`.
2. Collect the chat wizard's answers, derive the destination and compatible
   config, and show compatibility reasons returned by plan. MCP tools such as
   Context7 are optional per agent; use official vendor docs if unavailable.
3. Save a strict JSON config outside the destination. Run
   `java -jar <skill>/bin/bootstrap.jar plan --config CONFIG --output OUTPUT`.
4. Resolve reported issues without silently replacing explicit choices. Present
   concrete scope/file plan before generation. Generate when authorized with
   `java -jar <skill>/bin/bootstrap.jar generate --config CONFIG --output OUTPUT`.
5. Run requested/available build checks with the generated `kotlin` wrapper,
   or `java -jar <skill>/bin/bootstrap.jar verify OUTPUT`. Report actual results,
   logs and limitations separately from generation. Never claim skipped checks pass.

Keep simple creation in one agent. For independent research or verification that
benefits from workers, read [delegation](references/agents.md): start fresh workers
with a bounded brief, file ownership and compact results. Do not forward the
conversation or load every reference into each worker.

Humans can run `java -jar <skill>/bin/bootstrap.jar init OUTPUT` in a real terminal.
Non-TTY agents use catalog → config → plan → generate. Terminal review supports
`edit ui`, `edit di`, `edit navigation`, `edit network`, `edit storage` and
`edit endpoint`. Disabling Ktor requires explicitly clearing a retained endpoint.
Cancellation/EOF and review never create a project.

## Implemented choices

- Kotlin Toolchain is the sole backend. Wrappers/pins are bundled; generation is
  offline. Gradle/hardened profiles and optional hardened modules were retired.
- Shared Compose supports Android/iOS/desktop/Wasm. Native Android is Android only.
  SwiftUI shares UI-free Kotlin logic and supports iOS, optionally Android.
- Manual/Koin DI; none/Navigation 3 for Compose; none/platform-native for SwiftUI.
  Koin with coding agents selected also installs the four official Koin skills
  with their references and project-scoped Kotzilla MCP settings for those agents.
  Generation is offline; OAuth login is a separate user step. Read the generated
  AGENTS.md: Kotlin Toolchain and the existing DSL override upstream Gradle recipes.
  Recommend Navigation 3 in Compose menus. Keep None available; direct CLI/config
  defaults remain None for replay compatibility. Navigation keys use explicit
  non-JVM serializers and per-entry saveable state.
  SwiftUI NavigationStack needs iOS 16.0+; the chat wizard derives this minimum
  when the user has not explicitly set one.
- Optional Ktor HTTPS request with platform engines, loading/result/error,
  timeout/cancellation and owned client. No live request at generation/startup/tests.
- Optional Settings-backed counter: Android application-context SharedPreferences,
  app-suite NSUserDefaults, real JVM file and origin-local browser localStorage.
  Storage failures are visible. Settings is not SQL or credential storage.
- All generated code/config/version fields must come from the CLI. Do not create
  unsupported SQL/DataStore/Retrofit/Hilt recipes by merely adding dependencies.
- Optional native Firebase Analytics, Cloud Messaging and Remote Config in Android
  and iOS app hosts. Read [Firebase](references/firebase.md) only when selected;
  it covers project/app registration, config retrieval and platform checks.
  Generation stays offline. Missing configs are CONFIG_MISSING; live service
  checks start NOT_RUN. Crashlytics and desktop/web Firebase recipes are unsupported.

## Safety and verification

Never overwrite or merge into an existing project. Keep wrapper and embedded
binary bytes intact. Review the config/lock before replay; mismatched old locks
fail explicitly. Preserve the project's owned controller/lifecycle above navigation.

`./kotlin build --platform PLATFORM` compiles selected code/app hosts and tests;
`./kotlin test --platform PLATFORM` executes platform tests. iOS also requires
full Xcode and simulator setup; app-store signing is separate. Browser tests use
Toolchain's Chromium/Playwright runner. SDK/network/browser/Xcode limitations must
be reported. Use the generated plan's commands and logs as evidence.

References: [chat wizard](references/chat-wizard.md),
[configuration](references/stacks.md), [data](references/data.md),
[toolchain](references/toolchain.md), [verification](references/verification.md).

## Continue with feature implementation

After generation, requests to implement features in the existing app belong to
`orchestrate-feature`, available separately in this plugin. Use it if installed;
never regenerate into the app as a feature implementation strategy. A portable
copy of this creation skill does not require that sibling to generate projects.
