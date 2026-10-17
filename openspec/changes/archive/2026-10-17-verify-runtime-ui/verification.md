# Real runtime acceptance — 2026-10-17

All seven tested application/platform combinations passed navigation, a real
HTTPS request, and counter persistence after process restart (web: reload and
close/reopen). These are direct UI observations, separate from build/test results.

## Cleanup before testing

Removed `src/test/resources/golden/`, this migration's older
`../migrate-kotlin-toolchain/retired-fixtures/`, and the root `.gradle/` cache.
The first deletion attempt was rejected by automatic approval review because the
general request to remove old material did not identify these directories. The
user then explicitly approved all three; deletion succeeded before runtime tests.
Updated current references. Historical OpenSpec decisions remain available.

## Fixtures and environment

Generated from the standalone Kotlin/JVM CLI into `/tmp/bootstrap-ui-acceptance`:

| Fixture | Configuration |
|---|---|
| `shared`, then `shared-fixed` | Android/iOS/JVM/Wasm; shared Compose, Koin, Navigation 3, Ktor, Settings; `dev.bootstrap.ui.shared` |
| `native`, then `native-fixed` | Android/iOS; SwiftUI + Android Compose over UI-free shared logic, Koin, platform-native navigation, Ktor, Settings; `dev.bootstrap.ui.nativeapp`; iOS 16 minimum |
| `android`, then `android-fixed` | Android-only Compose, Koin, Navigation 3, Ktor, Settings; `dev.bootstrap.ui.androidapp` |

`*-fixed` directories were newly generated after the fix; no fixture project was
overwritten. `diff -qr` confirmed unchanged application inputs for shared logic,
desktop, web, and native iOS between initial and final fixtures. Both final iOS
applications were also built, installed and run again. The Android fix changes
the shared-UI Android host configuration; that scenario was repeated in full.

- macOS ARM64, Xcode 27.1 (27A9275), OpenJDK 21.0.12.1.
- Kotlin Toolchain 0.13.0, Kotlin 2.4.21, Compose Multiplatform 1.12.2.
- Dedicated API 36 Google Play ARM64 AVD `BootstrapUI`, serial `emulator-5554`;
  AVD data under the temporary acceptance directory. Compile/target SDK 37.
- Dedicated iOS 27.1 iPhone Duo simulator `BootstrapUI`, UDID
  `97B37601-0107-4CD0-81FC-9E689864408C`; Device Hub supplied the UI controls.
- Android CLI 1.0.16625370 supplied layout, selector taps and screen captures.
  It is acceptance tooling, not a new generator/runtime dependency. Layout
  instrumentation was installed only on the test emulator.
- Chrome, separate test tabs at `http://127.0.0.1:8437`; browser networking used
  the generated Wasm Ktor JS engine, including real CORS enforcement.
- Real endpoint: `https://httpbin.org/uuid`, triggered only by the Fetch button.
  No HTTP mocks, injected state, private data, credentials or server-side writes.

Build commands used `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer`
and `ANDROID_HOME=/tmp/bootstrap-android-sdk`. Global `xcode-select` was not
changed. The temporary SDK links existing vendor emulator/system-image assets;
the test AVD has its own data directory.

## Observed UI results

Each mobile/web sequence began at 0, incremented twice on Home, opened Details
showing 2, incremented there, returned to Home showing 3, fetched, and restarted
without clearing data. Desktop began at 1 from an earlier UI probe and finished
at 4 through the same sequence. Native SwiftUI's explicit Back button and Compose
Navigation 3's Back button were exercised.

| Application/platform | Navigation and counter | Real HTTP result | Restart persistence |
|---|---|---|---|
| Shared Compose / Android | Details 2 → Home 3 | UUID `86765fdc-6a37-4a76-a275-470c95e1d53e` | `force-stop` / launch → 3 |
| Android-only Compose | Details 2 → Home 3 | UUID `c98c4659-85cc-46e0-990a-38f7d62767f8` | `force-stop` / launch → 3 |
| Native recipe / Android Compose | Details 2 → Home 3 | First request: visible `Network error: Software caused connection abort`; retry succeeded with UUID `e7bd6570-0a23-4a01-b70d-1eb1b763e0ee` | `force-stop` / launch → 3; retry worked |
| Shared Compose / iOS | Details 2 → Home 3 | UUID `22afd7e3-deee-47ab-bbdd-d6b83ba508de`; final fixture UUID `b67d56f3-0cb4-4df3-b1d8-3452a753b6e5` | `simctl terminate` / launch → 3; final installation also restored 3 |
| SwiftUI / iOS | Details 2 → Home 3 | UUID `563a0bdd-ac51-4f38-89f8-72ce0921edcc`; final fixture UUID `54198317-df64-4c4b-9be5-f2ff17a0a8f2` | `simctl terminate` / launch → 3; final installation also restored 3 |
| Shared Compose / desktop JVM | Details 3 → Home 4 | UUID `57f96149-8454-419f-be5f-a09de860a934`; repeat `3f08b855-2052-48ec-9b18-a4a3e1cb21e5` | Terminated packaged app process; reopened → 4 |
| Shared Compose / browser Wasm | Details 2 → Home 3 | UUID `d2788996-c02b-45c3-8bf6-cc71bf686204` | Reload → 3; closed tab and reopened same origin → 3 |

Screenshots, Android layout trees, native accessibility snapshots and logs are in
[ui-evidence/](ui-evidence/). The native recipe's transient Android transport
failure was retained as error evidence alongside the successful retry. No new
Android crash followed the corrected startup; the retained crash log contains
only the original 14:17:41 `NoSuchMethodError`.

The standard desktop `run` started Compose Hot Reload. The unbundled JVM app
could not be selected by the native computer-use API, so its generated executable
JAR was placed in a standard JDK `jpackage --type app-image` for native UI control.
No application source or storage backend was changed. Desktop screenshots were
only macOS window thumbnails; readable native accessibility snapshots document
the actual counter and HTTP output instead. iOS screenshots use the simulator's
native frame capture to avoid the same host window thumbnail behavior.

After recording evidence, the test browser tab was closed, the local web server
and desktop app were stopped, and the dedicated AVD and simulator were shut down.
Temporary generated projects/device data remain available for reproduction.

## Defects fixed

1. **Shared Compose Android app crashed on startup.** The generated Android host
   called `setContent` but did not enable the Compose compiler when `shared`
   owned the UI. Compilation succeeded while producing an incompatible callback
   signature (`Function0` instead of the transformed composable callback), so
   startup threw `NoSuchMethodError` in `ComponentActivityKt.setContent$default`.
   `configureToolchain` now enables Compose for every Android host. A regression
   checks baseline/data-enabled KMP and Android-only hosts, plus native Android,
   while ensuring SwiftUI's shared Kotlin module remains free of Compose.
   A fresh fixture then passed startup and all required Android UI scenarios.
2. **Generated iOS launch instructions were ambiguous.** `run -m iosApp` exposes
   device and simulator targets and asks for a platform even with a device ID.
   Generated README commands now select `--platform iosSimulatorArm64`, describe
   `--device-id`, and list only selected platform applications. The regression
   checks the simulator command; both final iOS applications ran with it.

The transient Android connection abort was not an application crash: the error
was visible, the Fetch button became usable again, and a real retry succeeded.

## Repeated builds and tests

After generator changes:

```sh
./kotlin do bundle
./kotlin test
```

All **56 CLI regression tests passed**. Root and bundled JARs are identical:
SHA-256 `10825753c9cf334bb556e45364b8531d0a028f471972fe5483655163a5449ffd`.
Root/skill POSIX and Windows Toolchain wrappers remain byte-for-byte identical.

Fresh shared fixture:

```sh
./kotlin build --platform android --platform iosSimulatorArm64 --platform jvm --platform wasmJs
./kotlin test --platform android --platform jvm --platform wasmJs
./kotlin test --platform iosSimulatorArm64
```

All four application builds passed, including Xcode. **31 test executions passed**
(Android 7, JVM 8, Wasm browser 8, iOS simulator 8).

Fresh native fixture:

```sh
./kotlin build --platform android --platform iosSimulatorArm64
./kotlin test --platform android --platform jvm
./kotlin test --platform iosSimulatorArm64
```

Both application builds passed, including Xcode. **21 test executions passed**
(Android 7, JVM 7, iOS simulator 7).

Fresh Android-only fixture: `./kotlin build` and `./kotlin test` passed, with
**7 test executions**. Total final generated-app test executions: **59**.
iOS test commands ran sequentially to avoid Toolchain simulator shutdown races.

Runtime commands used:

```sh
./kotlin run -m androidApp --device-id emulator-5554
./kotlin run -m iosApp --platform iosSimulatorArm64 --device-id 97B37601-0107-4CD0-81FC-9E689864408C
./kotlin run -m desktopApp
./kotlin run -m webApp --port 8437 --no-open-browser
./kotlin package -m desktopApp --format executable-jar
```

Strict OpenSpec validation and `git diff --check` passed. Nonfatal upstream output
includes module-list sorting advice and a cached Ktor Wasm KLIB loader warning;
the Wasm application build, browser tests and real HTTP interaction all passed.

## Scope and limits

This is real UI acceptance for the seven combinations above using Koin,
Navigation 3 / SwiftUI navigation, Ktor and Settings. It does not claim UI coverage
for every manual-DI/no-navigation/no-network/no-storage permutation. The earlier
[48-stack build/test matrix](../2026-10-17-migrate-kotlin-toolchain/verification.md) remains
separate evidence; it was not rerun or relabeled as device UI coverage here.

No physical-device signing, App Store/Play publication, x86 desktop/Windows/Linux,
Safari/Firefox, process restoration to the previous navigation destination,
browser-history integration, rotations or fold/unfold transitions were tested.
Persisted counter restoration intentionally returns to Home. The demo endpoint
is an external service: current success does not guarantee future availability.
