---
name: bootstrap-project
description: Create Android and Kotlin Multiplatform projects for iOS, desktop and browser Wasm with Kotlin Toolchain, an interactive wizard or reproducible agent config flow.
---

# Bootstrap Kotlin Toolchain projects

Use this skill's standalone `bin/bootstrap.jar` with Java 17+. Resolve paths
relative to this skill directory; do not require repository sources or sibling
skills. No Python or global Kotlin CLI is needed. Claude and Codex use the same
CLI contract. Discover supported choices instead of inventing integrations.

## Agent flow

1. Run `java -jar <skill>/bin/bootstrap.jar catalog --json`.
2. Collect project name, dotted package/application ID, empty destination,
   platforms, UI, DI, navigation, network/storage and agent preferences. Ask only
   for missing decisions; infer defaults when appropriate. Show compatibility
   reasons returned by plan. MCP tools such as Context7 are optional per agent;
   use official vendor docs if unavailable.
3. Save a strict JSON config outside the destination. Run
   `java -jar <skill>/bin/bootstrap.jar plan --config CONFIG --output OUTPUT`.
4. Resolve reported issues without silently replacing explicit choices. Present
   concrete scope/file plan before generation. Generate when authorized with
   `java -jar <skill>/bin/bootstrap.jar generate --config CONFIG --output OUTPUT`.
5. Run requested/available build checks with the generated `kotlin` wrapper,
   or `java -jar <skill>/bin/bootstrap.jar verify OUTPUT`. Report actual results,
   logs and limitations separately from generation. Never claim skipped checks pass.

Humans can run `java -jar <skill>/bin/bootstrap.jar init OUTPUT` in a real terminal.
Non-TTY agents use catalog → config → plan → generate. Wizard review supports
`edit ui`, `edit di`, `edit navigation`, `edit network`, `edit storage` and
`edit endpoint`. Disabling Ktor requires explicitly clearing a retained endpoint.
Cancellation/EOF and review never create a project.

## Implemented choices

- Kotlin Toolchain is the sole backend. Wrappers/pins are bundled; generation is
  offline. Gradle/hardened profiles and optional hardened modules were retired.
- Shared Compose supports Android/iOS/desktop/Wasm. Native Android is Android only.
  SwiftUI shares UI-free Kotlin logic and supports iOS, optionally Android.
- Manual/Koin DI; none/Navigation 3 for Compose; none/platform-native for SwiftUI.
  SwiftUI NavigationStack needs an explicit iOS minimum of 16.0+.
- Optional Ktor HTTPS request with platform engines, loading/result/error,
  timeout/cancellation and owned client. No live request at generation/startup/tests.
- Optional Settings-backed counter: Android application-context SharedPreferences,
  app-suite NSUserDefaults, real JVM file and origin-local browser localStorage.
  Storage failures are visible. Settings is not SQL or credential storage.
- All generated code/config/version fields must come from the CLI. Do not create
  unsupported SQL/DataStore/Retrofit/Hilt recipes by merely adding dependencies.

## Safety and verification

Never overwrite or merge into an existing project. Keep wrapper and embedded
binary bytes intact. Review the config/lock before replay; mismatched old locks
fail explicitly. Preserve the project's owned controller/lifecycle above navigation.

`./kotlin build --platform PLATFORM` compiles selected code/app hosts and tests;
`./kotlin test --platform PLATFORM` executes platform tests. iOS also requires
full Xcode and simulator setup; app-store signing is separate. Browser tests use
Toolchain's Chromium/Playwright runner. SDK/network/browser/Xcode limitations must
be reported. Use the generated plan's commands and logs as evidence.

References: [configuration](references/stacks.md), [data](references/data.md),
[toolchain](references/toolchain.md), [verification](references/verification.md).
