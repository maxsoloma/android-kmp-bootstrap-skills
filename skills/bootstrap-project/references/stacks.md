# Stack discovery

Run the standalone CLI's `catalog --json`, then validate a config with `plan`.
UI: Android Compose, shared Compose or SwiftUI. DI: manual/Koin. Navigation:
none/Navigation 3 for Compose; none/platform-native for SwiftUI (iOS 16+).
Network and storage: independent none/Ktor and none/Settings choices.

All recipes use Kotlin Toolchain. Unsupported combinations return structured
issues before writes. Do not invent Hilt, Retrofit, SQL or DataStore integrations
from dependency names. Generated owners close graphs/clients/scopes; retain one
owner above navigation. Compose recipes with a shared controller obtain a
`CounterViewModel` (androidx.lifecycle 2.11.0, not the JetBrains fork) through
`viewModel(factory = counterViewModelFactory)` above the back stack; clearing its
ViewModelStore closes the controller. SwiftUI keeps its ObservableObject owner.
The baseline manual/no-navigation/no-data recipe stays a stateless counter.
See data.md and verification.md for runtime constraints.
