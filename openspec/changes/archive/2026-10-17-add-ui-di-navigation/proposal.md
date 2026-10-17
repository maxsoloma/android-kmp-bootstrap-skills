## Why

The current wizard only generates Compose UI with a fixed modern stack. Teams
need native SwiftUI on iOS and explicit DI/navigation choices that are validated
against all selected platforms before creating a project.

## What Changes

- Add a modern native-mobile recipe: shared Kotlin logic, native SwiftUI on iOS,
  and Jetpack Compose on Android. Support iOS alone or Android+iOS.
- Add modern DI choices manual and Koin, with actual bindings and graph tests.
- Add navigation choices none, Navigation 3 for Compose, and platform-native
  navigation (NavigationStack on iOS and Navigation 3 on Android).
- Expose the same compatibility matrix and pinned component dependencies through
  catalog, config, plan, terminal menus and agent skills. Explain unavailable Hilt
  modern integration and mixed SwiftUI/desktop/web combinations.
- Preserve existing default outputs, flags, standalone JARs, hardened profiles,
  publication guards, verification commands and regression fixtures.

## Capabilities

### New Capabilities

- `native-mobile-ui`: SwiftUI host using shared Kotlin logic with Android Compose.
- `stack-selection`: DI/navigation selection, compatibility, discovery and real
  generated integrations across terminal and config workflows.

### Modified Capabilities

None in the main spec store. These capabilities extend the completed but active
`add-bootstrap-init` change. Its existing-only availability statements describe
iteration one; this change adds supported recipes without changing old defaults.

## Impact

Kotlin CLI options/resolver/catalog, modern renderer/resources, terminal wizard,
skill instructions, documentation, regression and generated-project checks. No new
CLI runtime, SDK installation, network/storage recipe or hardened stack migration.
