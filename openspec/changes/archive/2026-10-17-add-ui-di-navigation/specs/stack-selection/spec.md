## Purpose

Let terminals and coding agents select real DI and navigation integrations using
one platform-aware compatibility contract before generating any project files.

## ADDED Requirements

### Requirement: Explicit compatible stack choices
Modern configurations SHALL accept manual or Koin DI, and no navigation, Compose Navigation 3 or platform-native navigation. Compose navigation MUST require Compose UI; platform-native navigation MUST require SwiftUI. SwiftUI navigation MUST require iOS 16+. Hardened profiles MUST retain their fixed DI/navigation and reject overrides.

#### Scenario: Compose targets
- **WHEN** modern shared Compose selects Koin and Navigation 3 on any supported platform subset
- **THEN** the configuration resolves to matching dependencies and integrations for those targets

#### Scenario: Native navigation
- **WHEN** SwiftUI selects platform-native navigation
- **THEN** iOS uses native navigation and Android, if selected, uses its Compose navigation integration

#### Scenario: Conflict before writes
- **WHEN** an unsupported DI, UI, navigation, profile or iOS-minimum combination is submitted
- **THEN** all detected conflicts are returned as structured issues and no project is written

### Requirement: Usable generated integrations
SwiftUI and nondefault component recipes SHALL generate a working graph and a test of dependency creation. Selecting navigation SHALL generate two connected screens with a back action and retained shared counter state. Non-JVM navigation keys MUST support serialization without reflection. The original manual/no-navigation starter MUST remain byte-compatible.

#### Scenario: Navigate and return
- **WHEN** the user increments the counter, opens details and returns
- **THEN** the same counter value is displayed and back does not remove the root screen

### Requirement: Shared discovery and editing
Catalog, plan, terminal wizard and agent instructions SHALL expose the same UI/DI/navigation choices, defaults, scopes, dependency pins and compatibility reasons. Editing targets, profile or UI MUST retain previously selected dependent answers and explain conflicts until the user corrects them explicitly.

#### Scenario: Change UI
- **WHEN** a user changes shared Compose to SwiftUI after selecting Compose navigation
- **THEN** the old navigation answer remains visible as incompatible and creation is blocked until corrected

### Requirement: Backward-compatible defaults and replay
Missing new fields SHALL preserve previous generation defaults. Existing flag outputs, golden fixtures, hardened recipes, binaries and checks MUST retain their bytes and behavior. New selections MUST be persisted in normalized specs and locks and replay deterministically; mismatching locks MUST fail before writes.

#### Scenario: Existing input
- **WHEN** the previous regression inputs are generated with existing flags
- **THEN** all previous golden hashes still match

#### Scenario: Replay new stack
- **WHEN** a generated SwiftUI or component config and matching lock are replayed at a new destination with the same CLI
- **THEN** all generated files are identical
