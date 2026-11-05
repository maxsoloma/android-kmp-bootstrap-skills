# Kotlin Toolchain policy

The only backend is published Kotlin Toolchain 0.13.0. Use bundled wrappers,
never a moving global installation or the dev wrapper checked into upstream's
source tag. The wrapper embeds the release distribution SHA-256. Other pins are
in `supporting/toolchain.json` and selected components are exposed by catalog.

Kotlin 2.4.21, Compose Multiplatform 1.12.2, Material 3 1.9.0, SDK 26/37/37.
Koin 4.2.2, Navigation 3 1.2.0, serialization 1.12.0, coroutines 1.11.0, Ktor 3.6.0,
Settings 1.3.0 and Lifecycle 2.11.0 are included only when selected. Pin dependency changes explicitly.

Generated apps use `project.yaml`, `module.yaml`, `src` and `src@platform`.
`./kotlin build` compiles apps/tests; `./kotlin test` executes tests. The wrapper
provisions its tool dependencies, JDK/compiler and platform build tooling. iOS
still requires Xcode and simulator setup; physical devices need signing settings.

The Xcode project is `iosApp/module.xcodeproj`, scheme `app`. It imports
KotlinModules and invokes `"${KOTLIN_CLI_WRAPPER_PATH}" tool xcode-integration`.
ARM64 device/simulator scope and the chosen deployment target are explicit.
SwiftUI imports Combine explicitly for ObservableObject/@Published.

For direct `xcodebuild`, use the physical project path as shown in the generated
README (`$(pwd -P)/iosApp/module.xcodeproj`) and pass
`PROJECT_DIR="$(pwd -P)/iosApp"`. Toolchain 0.13.0 compares Xcode's
module path with its canonical project path; aliases such as macOS `/tmp` versus
`/private/tmp` otherwise cause a module-resolution error. The regular Toolchain
build command already uses the canonical path.

Toolchain is Alpha; verify versioned sources and real apps. Older Wasm documentation
says tests are unavailable, but 0.13.0 implements Chromium/Playwright execution.
Do not infer unsupported functionality from stale documentation or successful
file generation. Keep actual matrix results and environmental failures visible.

Reviewed 2026-10-28: Kotlin 2.4.21, Compose 1.12.2 and Toolchain 0.13.0 remain
the current stable release pins. Published Maven Central metadata confirms
Navigation 3 1.2.0 and serialization 1.12.0; both pins are updated. The Compose
1.12 documentation table still lists a Navigation 3 release candidate, so published
stable artifacts take precedence. Android app hosts are already separate from
the shared library, as
required by the AGP 9 KMP migration. Toolchain owns Android build tooling; do not
add Gradle plugin DSL or retired Gradle files to this recipe. Use [verification](verification.md) to record concrete build evidence for each run.

Reviewed 2026-11-05: Lifecycle 2.11.0 publishes `lifecycle-viewmodel-compose` and
`lifecycle-runtime-compose` for Android, desktop JVM, iOS ARM64/simulator and Wasm,
so Compose recipes with a controller depend on `androidx.lifecycle` directly. The
Android KMP ViewModel guide restricts `org.jetbrains.androidx.lifecycle` ViewModel
artifacts to Lifecycle below 2.11.0; the Compose Multiplatform page still shows the
2.10.0 fork. Compose 1.12.2 pulls the fork's `lifecycle-runtime-compose` 2.9.6, whose
non-Android variants depend on `androidx.lifecycle:lifecycle-runtime-compose`, so
2.11.0 wins resolution. `koin-compose-viewmodel` 4.2.2 still depends on the fork's
`lifecycle-viewmodel-compose` 2.9.6 and is not used.

Sources:
- https://github.com/JetBrains/kotlin-toolchain/releases/tag/v0.13.0
- https://kotlinlang.org/docs/multiplatform/kotlin-toolchain.html
- https://kotlin-toolchain.org/0.13/cli/provisioning/
- https://kotlin-toolchain.org/0.13/user-guide/product-types/ios-app/
- https://kotlinlang.org/docs/releases.html
- https://kotlinlang.org/docs/multiplatform/whats-new-compose-112.html
- https://kotlinlang.org/docs/multiplatform/multiplatform-project-agp-9-migration.html
- https://kotlinlang.org/docs/multiplatform/compose-navigation-3.html
- https://repo1.maven.org/maven2/org/jetbrains/androidx/navigation3/navigation3-ui/maven-metadata.xml
- https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-serialization-json/maven-metadata.xml
- https://developer.android.com/kotlin/multiplatform/viewmodel
- https://developer.android.com/jetpack/androidx/releases/lifecycle
- https://kotlinlang.org/docs/multiplatform/compose-viewmodel.html
- https://dl.google.com/android/maven2/androidx/lifecycle/group-index.xml
