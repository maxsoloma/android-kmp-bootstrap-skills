# Verification

Verified on macOS ARM64, JDK 21, 2026-10-17. Projects and test logs are in temporary
directories outside the repository. Android builds used the temporary API 37 SDK.

## CLI and preservation

- `./gradlew test bundleSkills`: PASS, 55 tests, no failures/skips. Existing 42
  tests remain, with 12 additional data configuration/rendering/wizard tests and
  one Xcode integration regression test.
- Hardened golden fixtures and generated binary assets are unchanged. Modern
  default and optional-stack fixtures preserve unrelated output; the Xcode
  follow-up intentionally updates only the generated `project.pbxproj` hashes
  for ARM64 architecture, the IDE build guard and script execution settings.
- Resolver tests cover all 15 nonempty target subsets, both DI choices, independent
  network/storage selections, strict types, HTTPS constraints, hardened conflicts
  and normalized config replay.
- Wizard tests cover new choices, editing, explicit endpoint removal, transitions
  to hardened, missing TTY, EOF/cancel and existing publication/save protections.
- Actual terminal: native-mobile + Koin + platform-native navigation + Ktor +
  Settings reached review with no destination written. Explicit create published
  the project. Another actual terminal cancelled at review without output.
- Actual non-TTY init returned `interactive_required`, exit 1, without output.
- A copied standalone JAR completed catalog → plan → generate → automatic sibling
  lock replay from an isolated directory; recursive comparison found identical
  files. No repository/sibling runtime was used.
- All three skill metadata validators passed. Bundled JARs are identical:
  SHA-256 `4f1ad362cfeb7bb1ce2563ce62940899005d025b6badb814e046bb878a0fc24d`.

## Generated build and test matrix

| Recipe | Data / DI / navigation | Actual result |
|---|---|---|
| Universal shared Compose | Ktor + Settings, Koin, Navigation 3 | Android APK/host tests/lint, desktop tests/classes and Wasm production distribution PASS |
| Native Android+iOS SwiftUI | Ktor + Settings, manual, platform-native | Shared JVM tests (including real file reopen), Android APK/tests/lint and shared Android host tests PASS |
| Android-only, min SDK 23 | Ktor only, Koin, no navigation | APK, mock/lifecycle/timeout tests and lint PASS |
| Shared Compose Android/desktop/web | Settings only, manual, Navigation 3 | Android APK/tests/lint, desktop file/graph tests and Wasm production distribution PASS |
| iOS-only SwiftUI | Ktor + Settings, Koin, no navigation | Shared JVM graph/network/file tests PASS without Android SDK or Xcode |
| Shared Compose desktop/web | Ktor + Settings, Koin, Navigation 3 | Eight Wasm tests PASS in real headless Chrome 155, including localStorage reopen, network success/error/timeout/cancellation and explicit route serialization |

Browser networking tests use MockEngine; no public HTTP endpoint is called.
Browser persistence tests use real origin-local localStorage and restore their
namespaced key. JVM persistence tests reopen real temporary files rather than
merely observing a retained in-memory value.

Integration checks caught and resolved named-source-set/inline dependency DSL
errors, deferred iOS hierarchy creation and missing shared Wasm executable setup
needed by Compose/Skiko's test bundler. Browser execution also required an explicit
PolymorphicSerializer for NavKey in the new data recipe's route test; prior
non-data output fixtures were deliberately preserved. Passing browser checks here
apply to the new data recipe, not retroactively to every previous generated test.

## Xcode follow-up: actual builds and native tests

On 2026-10-17 the user accepted the license and completed Xcode first-launch
setup. All checks below used Xcode 27.1 (27A9275) via per-process
`DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer`; the global developer
directory was not changed. The initial Command Line Tools/license/CoreSimulator
blockers are resolved. iOS 27.1 (24A94425) ARM64 runtime was installed. Xcode's
download command also attempted iOS 27.0 and reported insufficient space for
that additional runtime; 27.1 installed successfully and executed the tests.

Builds found a real template error: a generic simulator destination requested
`ios_x64` although only ARM64 Kotlin targets exist. Both Xcode configurations now
set `ARCHS = arm64`. The build script also follows current JetBrains direct
integration guidance: always run on ordinary builds and skip the duplicate
Gradle invocation when the IDE sets `OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED=YES`.
Script sandboxing remains disabled and Kotlin builds precede Swift compilation.
Only reviewed Xcode project hashes were changed in prior-output fixtures.

Fresh projects: `/tmp/bootstrap-ios-followup.OFtNiE`.

| Recipe | Xcode Debug simulator app | Native simulator tests |
|---|---|---|
| SwiftUI, Koin, no navigation, Ktor + Settings | PASS (`swift-koin`) | 7 PASS (`native-tests`) |
| SwiftUI, manual DI, NavigationStack, Ktor + Settings, iOS minimum 16 | PASS (`swift-manual-nav`) | 7 PASS (`native-manual-tests`) |
| Shared Compose, Koin, Navigation 3, Ktor + Settings | PASS (`compose-koin-nav`) | 8 PASS (`compose-tests`) |
| Original shared Compose default, no optional components | PASS (`compose-default`) | 1 PASS |

Commands: the generated plan's `xcodebuild ... -configuration Debug -sdk
iphonesimulator -destination 'generic/platform=iOS Simulator'
CODE_SIGNING_ALLOWED=NO build`, with a separate derived-data path per recipe;
and `./gradlew :shared:iosSimulatorArm64Test
:shared:linkDebugFrameworkIosSimulatorArm64 --console=plain`. Koin SwiftUI also
passed `:shared:linkDebugFrameworkIosArm64` for the device architecture.
JUnit XML confirms 23 executed native tests, zero failures/errors/skips.

The new generated `IosPersistenceTest` reopens the real NSUserDefaults-backed
platform store and restores its prior namespaced key in `finally`. Native tests
also exercise graph ownership, failed writes, mock HTTP success/error/timeout,
cancellation and (Compose navigation) route serialization. No live HTTP is used.

Xcode build logs: `/tmp/bootstrap-ios-{swift-koin,swift-manual-nav,compose-koin-nav,compose-default}.log`.
Native logs: `/tmp/bootstrap-ios-{native-tests,native-manual-tests,compose-tests,baseline-tests}.log`.
SwiftUI and Compose data apps were installed and launched on a dedicated
`BootstrapVerification` iPhone Duo simulator; their processes remained alive and
the first screens rendered. Screenshots: `/tmp/bootstrap-ios-swiftui-primary-final.png`
and `/tmp/bootstrap-ios-compose-primary-final.png`. The temporary simulator was
shut down and deleted afterwards; the existing simulator was retained.

## Kotlin Toolchain applicability

Context7 and official JetBrains documentation confirm Kotlin Toolchain CLI is
an Alpha build system with `project.yaml`/`module.yaml`, not a prerequisite for
Gradle-based KMP. This repository has no such generated backend and no global
`kotlin`/`kotlinc` command was present in the checked PATH. The required Kotlin
Gradle plugins, Kotlin/Native provisioning and Xcode direct framework integration
are already present and now verified. No alternate build system was installed
or silently introduced. See the skill's `references/toolchain.md` for sources
and the distinction from Gradle JVM toolchains and IDE plugins.

## Remaining runtime scope

- Native unit tests run through Gradle using Xcode's SDK and simulator tooling;
  no XCTest/UI test target is generated, and `xcodebuild test` was not claimed.
- Android device persistence/UI, iOS button/navigation interaction, persistence
  across full app restarts, physical-device signing/runtime and desktop GUI
  interaction were not exercised. NSUserDefaults reopening and first-screen
  startup are verified separately. Browser tests establish the test origin's storage behavior;
  they do not establish every privacy/quota policy, live endpoint availability,
  live CORS behavior or end-to-end Compose UI interaction.
- Windows/Linux terminal behavior was not run locally. Existing three-OS
  generator CI remains configured. Hardened builds were not rerun; their templates
  and verification procedures remain unchanged and regression-tested.

CI now contains data-stack Android/desktop/web builds, headless browser tests,
native data owners with both DI choices and new iOS data matrix entries. YAML
parsing passed; remote workflow-dispatch jobs were not triggered or claimed as
passing during this session.

## Specification and source hygiene

- `openspec validate add-network-storage --strict`: PASS.
- `git diff --check`: PASS.
- The change remains active for review; no sync/archive, commit or publication was
  performed automatically.
