# Application stacks

| Kind/UI | Targets | DI | Navigation |
|---|---|---|---|
| Android / android-compose | Android only | manual, koin | none, navigation3 |
| KMP / shared-compose | Any nonempty Android/iOS/desktop/web subset | manual, koin | none, navigation3 |
| KMP / swiftui | iOS, optionally Android | manual, koin | none, platform-native |

All use pinned Kotlin Toolchain wrappers and module YAML. Shared Compose puts UI
and logic in `shared/src`, with platform code in `src@android`, `src@ios`,
`src@jvm`, `src@wasmJs`. Each selected app is a separate module. Native Android
uses one app module. SwiftUI puts shared logic in a UI-free module with a JVM test
target, Android Compose in the Android host and SwiftUI in `iosApp/src`.

Manual/Koin graphs are owned per counter/controller. Closing the owner disposes
the graph and optional network client/scope. Navigation keeps the owner above the
back stack. Navigation 3 keys have explicit polymorphic serializers; browser
history integration is not generated. Native navigation uses SwiftUI NavigationStack
(iOS minimum 16.0+) and Android Navigation 3 when Android is selected.

Networking and key-value storage are independent choices; see [data](DATA.md).
Unsupported Hilt/Retrofit/SQL/DataStore choices fail with compatibility feedback.

Use `./kotlin build --platform PLATFORM` and `./kotlin test --platform PLATFORM`.
Platforms are `android`, `iosSimulatorArm64`, `jvm`, `wasmJs`; iOS also has the
ARM64 device target. The Xcode host uses `iosApp/module.xcodeproj`, scheme `app`,
`KotlinModules` and the supported `tool xcode-integration` wrapper phase.
Signing a physical device or App Store build requires your own team/settings.
