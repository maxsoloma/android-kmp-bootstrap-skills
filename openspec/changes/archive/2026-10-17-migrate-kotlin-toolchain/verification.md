# Kotlin Toolchain migration verification

Acceptance passed on 2026-10-17. Kotlin Toolchain 0.13.0 is the sole/default
backend for modern recipes and for building this repository. Claude and Codex
plugin/CLI version is 3.0.0; normalized ProjectSpec version is 2.

## Actual generated application matrix

| Recipe | Projects | Build/test commands | Successful test executions |
|---|---:|---:|---:|
| Native Android Compose | 16 | 32 | 71 |
| Shared Compose: Android, iOS, desktop JVM, browser Wasm | 16 | 128 | 308 |
| SwiftUI + Android Compose + UI-free shared JVM logic | 16 | 96 | 216 |
| Platform-only source-layout checks | 5 | 12 | 45 |
| Total | 53 | 268 | 640 |

Each 16-project group covers manual/Koin × none/applicable navigation × none/Ktor
× none/Settings. Navigation is Navigation 3 for Compose and platform-native for
SwiftUI. Every command returned zero; there are no skipped acceptance checks.
The 268 commands comprise 134 builds and 134 test runs. Test executions repeat
cases across platforms and recipes; they are not 640 distinct test definitions.

The five subset projects are desktop-only, web-only, iOS-only Compose, iOS-only
SwiftUI and Android-only KMP. They also exercise min SDK 23, target SDK 35 and iOS
15.0 where applicable. A separate sixth subset, iOS-only SwiftUI NavigationStack
with Koin/Ktor/Settings at iOS 16.0, passed shared JVM and iOS build/test checks.

Android checks compile APKs and run unit tests. JVM checks compile desktop hosts
and run shared tests. Wasm checks build production bundles and execute tests in
Chromium, including actual localStorage reopening. Every iOS matrix build includes
the Xcode host; native simulator tests include real NSUserDefaults reopening.
Data tests retain mock HTTP success/error/timeout, owner cancellation, failed
storage visibility, independent DI owners and actual temporary-file reopening.
Route tests serialize keys explicitly without JVM reflection.

Raw per-command results, executed test names, command timings and log SHA-256
values are preserved in [matrix-results.json](matrix-results.json). Full local
sources/configs/locks and logs are under `/tmp/bootstrap-ktc-migration.Gdazz6/`,
in `matrix-android`, `matrix-shared`, `matrix-native` and `subsets`. Passed build
outputs were partly cleaned with `kotlin clean` after archiving available test
reports; source files, verification JSON and `.bootstrap` logs remain intact.

The final executable regenerated all 53 configurations. All application sources,
module/project YAML, wrappers, binary assets, Xcode projects, metadata and normalized
specs are byte-identical to the tested projects. Only README guidance and resource
recipe-lock hashes are excluded from this comparison: documentation and removal
of unused legacy resources changed during acceptance. This parity evidence is
included in `matrix-results.json`; no application build input was excluded.

## Additional native and engine checks

- Xcode ARM64 iPhone SDK unsigned builds passed for both all-components Compose
  and SwiftUI representative apps (`** BUILD SUCCEEDED **`). Logs:
  `compose-device-build-canonical.log` and `native-device-build-canonical.log`.
- A temporary integration test opened/closed the actual selected CIO, Js and
  Darwin Ktor engines, without sending a request, on Android/JVM/Wasm/iOS.
  Logs: `engine-smoke.log`, `engine-smoke-ios.log` and
  `compose-v3/.bootstrap/ios-test.log`. This supplements generated MockEngine tests.
- The extra iOS-only NavigationStack result is in
  `native-ios-navigation/.bootstrap/verification.json`.

## Generator, flow and packaging

`./kotlin do bundle` and `./kotlin test` passed: 55 regression tests, zero failures
or skips. They include rendering all 15 nonempty shared-platform subsets across
DI/navigation/network/storage (240 combinations), compatibility/type validation,
retired-option rejection, plan/replay, symlink/no-overwrite safeguards, timeout
and continue-after-failure behavior, standalone installation and plugin discovery.

A real terminal completed SwiftUI/Koin/navigation/Ktor/Settings init and `create`.
A separate cancellation left no files. Non-TTY init returned `interactive_required`.
The final JAR, copied alone to a directory with spaces outside this repository,
passed catalog → plan → generate → locked replay; generated trees were identical.
An incompatible older recipe lock and overwrite attempt were rejected.
See local `final-standalone.json`, `non-tty.log`, `tests-final2.log` and
`bundle-final2.log`. The skill-creator validator returned `Skill is valid!`.

The final standalone JAR and skill JAR have SHA-256:
`b1627e41df09847208b1eddb87e1deeca5f282f3089b770e07fd806daa87c434`.
A forced repeat of the bundle task produced exactly the same bytes.
The JAR contains no retired templates, golden fixtures or Gradle infrastructure.
The two hardened skills, Gradle renderer/build/wrappers and hardened module
machinery are removed from the active distribution. Prior OpenSpec records retain the history. At the user's subsequent cleanup
request, original golden fixtures, retired templates and obsolete local Gradle
caches were deleted before runtime/UI acceptance.

## Environment and exact tool versions

- macOS 27.0.1, ARM64; generator JVM OpenJDK 21.0.12.1.
- Kotlin Toolchain 0.13.0, commit `abc7a5f`, release date 2026-10-01.
- Xcode 27.1 / 27A9275; iPhoneOS and iOS Simulator SDK 27.1, installed ARM64 runtime.
- `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer`; global Xcode selection
  was not changed. Android SDK: `ANDROID_HOME=/tmp/bootstrap-android-sdk`, API 37.0.
- Playwright-provisioned Chromium 149.0.7827.55, browser build v1228.
- Kotlin 2.4.21, Compose Multiplatform 1.12.2, Material 3 1.9.0, Activity 1.13.0.
- Koin 4.2.2, Navigation 3 1.1.2, serialization/coroutines 1.11.0, Ktor 3.6.0,
  Settings 1.3.0. Default Android min/target/compile SDK 26/37/37; iOS minimum 15.0.

Published Maven release wrappers were used, not the dev wrapper committed in the
upstream v0.13.0 source tag. Their original bytes/checksums are preserved:

| Artifact | SHA-256 |
|---|---|
| `kotlin` | `8e5972c57ee6cb277ea3ec48a7b5ace1f19983e5f4373f3f29752b31082d340b` |
| `kotlin.bat` | `a83763d91ca43d9906e92e90aa6b93caeacc32b202343b61594e94de2d506dd8` |
| Toolchain distribution | `fa82b9872dcfa04d26c8f800f33d597c99fbffd747a0e9bbce3a9e4424b3a588` |

## Failures resolved and remaining scope

1. Removed a duplicated serializer registration from a transitional data-test
   overlay; retained the explicit polymorphic route round-trip test.
2. Added explicit Combine imports for SwiftUI ObservableObject/@Published with
   Xcode's modern member-import visibility enabled.
3. Toolchain 0.13.0's iOS simulator mutex coordinates only one JVM. Concurrent
   processes passed their assertions but could fail during simulator shutdown.
   `verify` now uses an OS file lock with bounded waiting across processes; its
   regression tests cover exclusivity, timeout and reuse. Remaining matrices ran
   with this fix, and the affected iOS-only test was rerun successfully. Original
   failure logs are retained in `subsets/ios-compose/.bootstrap-concurrent-attempt`.
4. Direct Xcode builds through `/tmp` encountered upstream module-path comparison
   errors (`/tmp` versus `/private/tmp`), even with an absolute project argument.
   Passing the physical `PROJECT_DIR` fixes this; generated Xcode commands and the
   Toolchain reference include it. Both iPhone SDK builds then passed. The normal
   Toolchain build already supplies its prebuilt result and passes independently.

Toolchain emits non-fatal Ktor Wasm KLIB-loader and Android AAR snapshotting
warnings. Compilation, browser tests and actual platform-engine smoke checks all
passed; these upstream warnings are retained in logs rather than hidden.

The repository/generated projects have no Gradle backend or wrapper. Toolchain
0.13.0 internally provisions Gradle 9.5 for Android; removing that upstream detail
would require changing Toolchain itself. No global Gradle, Python runtime or
repository sibling is required by the bootstrap distribution.

Physical-device execution/signing, Android instrumented/UI tests, full UI journeys,
live endpoint/CORS behavior, production persistence durability and app-store
publication were not tested. Linux/Windows generator CI is configured but was not
executed on this macOS host. These limits do not replace or skip any matrix check.

Reproduce with the commands in [docs/BUILD.md](../../../../docs/BUILD.md). CI's manual
workflow runs all three full matrices and platform-only checks on macOS, and
normal CI builds/tests the generator on Linux, macOS and Windows.
