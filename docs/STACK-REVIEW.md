# Stack review — 2026-10-28

The 3.1.0 update preserves Kotlin Toolchain as the sole backend. Published stable
artifacts, official documentation and generated applications were checked.

| Component | Pin / decision |
|---|---|
| Kotlin Toolchain | 0.13.0, existing release wrappers and checksum unchanged |
| Kotlin / Compose Multiplatform | 2.4.21 / 1.12.2 |
| Material 3 / Activity Compose | 1.9.0 / 1.13.0 |
| Navigation 3 | Updated 1.1.2 → 1.2.0; recommended in Compose menus |
| Serialization | Updated 1.11.0 → 1.12.0, including the generator |
| Koin / Ktor | 4.2.2 / 3.6.0 |
| Coroutines / Settings | 1.11.0 / 1.3.0 |
| Android SDK min / target / compile | 26 / 37 / 37 |
| Firebase Android | Analytics 24.0.0, Messaging 26.0.1, Config 24.0.1; BoM 35.1.0 alignment |
| Firebase Apple | 13.1.1, exact SPM products |

The Navigation 3 stable artifact is newer than the release candidate still listed
in the Compose 1.12 documentation table. Its published POM supports the pinned
Compose/Kotlin baseline. Maven Central confirms the other stable component pins;
Google Maven confirms Activity Compose.

The current AGP 9 migration separates Android app entry points from KMP shared
libraries and uses `com.android.kotlin.multiplatform.library` in Gradle projects.
This generator already separates `androidApp` from the `shared` library. Toolchain
owns its Android tooling; generated projects keep `project.yaml`, `module.yaml`,
`src` and `src@platform`. No Gradle backend or legacy-API escape hatch was added.

## Firebase and agent behavior

The generator implements native mobile Analytics, Messaging and Remote Config.
Android uses official resource names instead of a Google Services Gradle plugin;
Apple uses SPM and a bundled plist. Config validation binds project ID, numeric
project/sender IDs and application/bundle ID. Public SDK config is embedded in
the normalized spec for path-independent replay. Missing files remain explicit
CONFIG_MISSING, SDK identity validation is CONFIGURED, live checks start NOT_RUN.

Messaging uses current FID registration APIs on both platforms, notification
permission actions, Android foreground notifications and explicit SwiftUI
delegate/APNs mapping. Firebase delegate swizzling is disabled for SwiftUI hosts,
with manual notification Analytics forwarding. Backend registration, Apple
signing/APNs credentials, Analytics account linking and live receipts remain
application/account work.

Crashlytics is rejected because its build ID/mapping and dSYM upload tooling is
not implemented. Auth/Firestore/Storage and desktop/Wasm Firebase are not recipes.

The portable skill and generated AGENTS.md describe fresh-context workers,
bounded briefs, file ownership, concise evidence and serial device ownership.
Two independent fresh-context reviews checked wizard behavior and Firebase code;
the dependency name, sender consistency and SwiftUI delegate findings were fixed.

## Verification

- `./kotlin do bundle` and `./kotlin test`: 62 tests passed, no skips. Includes
  strict Firebase inputs, per-service/target selection, source placement, native
  delegate setup, public-config replay, lock mismatches and terminal edits.
- Skill Creator validator passed; the shell verification script parses and
  `git diff --check` is clean. Python/PyYAML is maintenance-only, never a skill
  runtime requirement.
- `scripts/verify-firebase.sh` checks fresh Android, universal shared Compose and
  native SwiftUI recipes with Koin, Ktor, Settings and all three Firebase services.
  It is included in the manually dispatched CI matrix.

| Fresh recipe | Actual checks | Result |
|---|---|---|
| Native Android | Android build + 8 tests | PASS |
| Shared Compose, Android/iOS/JVM/Wasm | All four builds, Xcode host, all four platform test runners | PASS |
| SwiftUI + Android Compose | Shared JVM build/tests, Android build/tests, iOS/Xcode build/tests | PASS |

All 16 final build/test checks passed. The final matrix has no failed or skipped
checks. SDK-only preparation was used for the live-cloud-independent matrix.

Local platform checks use Xcode 27.1 via process-local `DEVELOPER_DIR`; the
system xcode-select setting was not changed.

Local evidence is in `/private/tmp/bootstrap-31-verification` and
`/private/tmp/bootstrap-31-current-matrix`; logs are not committed. The frozen
standalone JAR used for the final matrix has SHA-256
`ee3aeca78af2d309c472e3701038aaa2f45d145c58b7535a61904fd8e73540a3`.

The current configured Android fixture also builds successfully. Earlier real
builds verified Android config resource packaging and Apple plist
packaging using explicit fictional IDs, without launching a connected app. An
earlier overlapping iOS run passed its tests but failed during simulator shutdown;
the final run uses the verifier's simulator lock and sequential checks.
The Wasm build retains an upstream Ktor 3.6.0 KLIB-loader warning; compilation
and browser tests pass. No SDK, browser or Xcode check is silently counted as passed.

No cloud project/app was created, no credentials were used, and no live Firebase
receipt, signed device push, production release or marketplace publication is
claimed. The full historical 48-stack matrix was not rerun for this change.

## Sources

- [Kotlin releases](https://kotlinlang.org/docs/releases.html)
- [Toolchain 0.13.0](https://github.com/JetBrains/kotlin-toolchain/releases/tag/v0.13.0)
- [Compose 1.12](https://kotlinlang.org/docs/multiplatform/whats-new-compose-112.html)
- [Navigation 3 setup](https://kotlinlang.org/docs/multiplatform/compose-navigation-3.html)
- [Navigation 3 published versions](https://repo1.maven.org/maven2/org/jetbrains/androidx/navigation3/navigation3-ui/maven-metadata.xml)
- [Serialization published versions](https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-serialization-json/maven-metadata.xml)
- [KMP AGP 9 migration](https://kotlinlang.org/docs/multiplatform/multiplatform-project-agp-9-migration.html)
- [Firebase Android releases](https://firebase.google.com/support/release-notes/android)
- [Firebase Apple releases](https://firebase.google.com/support/release-notes/ios)
- [Android FCM setup](https://firebase.google.com/docs/cloud-messaging/android/get-started)
- [Apple FCM setup](https://firebase.google.com/docs/cloud-messaging/ios/get-started)
- [Firebase SwiftUI integration](https://firebase.google.com/docs/ios/learn-more#swiftui)
