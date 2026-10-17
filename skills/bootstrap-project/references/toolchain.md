# Kotlin Toolchain policy

The only backend is published Kotlin Toolchain 0.13.0. Use bundled wrappers,
never a moving global installation or the dev wrapper checked into upstream's
source tag. The wrapper embeds the release distribution SHA-256. Other pins are
in `supporting/toolchain.json` and selected components are exposed by catalog.

Kotlin 2.4.21, Compose Multiplatform 1.12.2, Material 3 1.9.0, SDK 26/37/37.
Koin 4.2.2, Navigation 3 1.1.2, serialization/coroutines 1.11.0, Ktor 3.6.0 and
Settings 1.3.0 are included only when selected. Pin dependency changes explicitly.

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

Sources:
- https://github.com/JetBrains/kotlin-toolchain/releases/tag/v0.13.0
- https://kotlinlang.org/docs/multiplatform/kotlin-toolchain.html
- https://kotlin-toolchain.org/0.13/cli/provisioning/
- https://kotlin-toolchain.org/0.13/user-guide/product-types/ios-app/
