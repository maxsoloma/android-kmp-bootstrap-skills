# Stack discovery

Run the standalone CLI's `catalog --json`, then validate a config with `plan`.
UI: Android Compose, shared Compose or SwiftUI. DI: manual/Koin. Navigation:
none/Navigation 3 for Compose; none/platform-native for SwiftUI (iOS 16+).
Network and storage: independent none/Ktor and none/Settings choices.

All recipes use Kotlin Toolchain. Unsupported combinations return structured
issues before writes. Do not invent Hilt, Retrofit, SQL or DataStore integrations
from dependency names. Generated owners close graphs/clients/scopes; retain one
owner above navigation. See data.md and verification.md for runtime constraints.
