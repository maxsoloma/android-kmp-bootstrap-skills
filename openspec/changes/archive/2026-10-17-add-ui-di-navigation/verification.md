# Verification

Verified on macOS ARM64, JDK 21, 2026-10-17. Temporary generated projects live
outside the repository. Android builds used a temporary SDK with API 37.

## CLI, wizard and packaging

- `./gradlew test bundleSkills`: PASS, 42 tests, no failures/skips. This includes
  the original 12 regression tests and all previous init/config/plan checks.
  Existing golden fixtures and wrapper hashes were not changed.
- Compatibility tests exercise all 15 nonempty Compose target subsets with both
  DI and navigation choices, native iOS/mobile combinations, fixed hardened
  stacks, strict field types, invalid UI/platform/minimum combinations and replay.
- Six presets, default flow, UI/DI/navigation edits, conflict retention/correction,
  profile transitions, EOF/cancel, save and publication guards pass scripted tests.
- Real PTY: selected native mobile, SwiftUI, Koin, platform-native navigation and
  iOS 16.0; destination did not exist at review; explicit create published files.
  Another real PTY cancelled in the navigation menu without creating a destination.
  Non-TTY init returned `interactive_required`, exit 1, without writing output.
- Latest standalone copied JAR: catalog → plan → config generation → replay using
  the emitted spec and automatically discovered lock; recursive comparison found
  identical output bytes. No sibling skill/repository resources were available.
- Three skills passed the metadata validator. All bundled JARs match SHA-256
  `7d85da7159a033cd0487a4d1f489d45597b1e52e41f4529fac2b5f0b82eee355`.

## Generated build matrix

| Recipe | DI | Navigation | Results |
|---|---|---|---|
| Universal shared Compose | Koin | Navigation 3 | Android APK/tests/lint + shared Android tests, desktop tests/classes, Wasm production distribution PASS |
| Universal shared Compose | Manual | Navigation 3 | Android APK/tests/lint + shared Android tests, desktop tests/classes, Wasm production distribution PASS |
| Universal shared Compose | Koin | None | Android APK/tests/lint + shared Android tests, desktop tests/classes, Wasm production distribution PASS |
| Native mobile, Android+iOS | Koin | Platform-native | Shared JVM tests + Android APK/tests/lint + shared Android tests PASS |
| Native mobile, Android+iOS | Manual | Platform-native | Shared JVM tests + Android APK/tests/lint + shared Android tests PASS |
| Native Android, min SDK 23 | Koin | Navigation 3 | APK, graph/route unit tests and lint PASS |
| Native Android | Manual | Navigation 3 | APK, graph/route unit tests and lint PASS |
| Native iOS-only | Koin | None | Shared JVM graph/counter tests PASS without Android SDK or Xcode |

The first native mobile build exposed a missing Android JUnit dependency for the
new route tests. The generator was corrected, regression coverage added, and both
native-mobile DI variants regenerated and verified successfully.

New component pins are Koin 4.2.2, Navigation 3 UI 1.1.2 and serialization 1.11.0.
Existing default and hardened pins/output remain covered by golden regressions.
Hardened Gradle builds were not rerun because their templates were unchanged.

## iOS and runtime limits

- `plutil -lint` for generated Xcode project and Info.plist: PASS.
- `swiftc -frontend -parse` for native Swift source: PASS (syntax only).
- iOS framework linking, native tests and Swift application typechecking/build:
  SKIPPED locally because the active developer directory was Command Line Tools.
  Follow-up on 2026-10-17 confirmed Xcode 27.1 is installed, but explicit selection
  through `DEVELOPER_DIR` is blocked by unaccepted Xcode/Apple SDK license
  agreements. A subsequent 2026-10-17 follow-up resolved setup, fixed the shared
  Xcode template's ARM64 settings and passed application builds for SwiftUI with
  both DI choices and shared Compose with Navigation 3. See
  `../add-network-storage/verification.md` for the tested recipes and native tests.
  Verifier reports separate `ios` and `ios-app` skips and exits 2, not all-pass.
- Device/emulator UI, desktop GUI and browser runtime were not exercised. Wasm
  production compilation is not evidence of browser interactions or history.
- Windows/Linux terminal behavior was not exercised locally; generator tests are
  configured for the existing three-OS CI matrix.

CI now includes selectable Compose/native Android builds and an iOS matrix for
both DI choices, both UI modes and applicable navigation modes, including native
tests and Xcode application builds. Workflow YAML parses locally. These optional
workflow-dispatch jobs were not dispatched or claimed as passing in this session.

## Source and specification checks

- `openspec validate add-ui-di-navigation --strict`: PASS.
- `git diff --check`: PASS.
- The previous OpenSpec change and this change remain active for review. Neither
  was synced or archived automatically.
