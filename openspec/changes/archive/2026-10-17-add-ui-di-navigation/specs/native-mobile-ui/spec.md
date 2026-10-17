## Purpose

Generate mobile projects that share Kotlin business logic while keeping the iOS
interface native SwiftUI and the Android interface Jetpack Compose.

## ADDED Requirements

### Requirement: Native mobile UI recipe
The modern profile SHALL offer a SwiftUI recipe for iOS alone or Android+iOS. It MUST generate a native SwiftUI host consuming shared Kotlin logic, with Compose confined to the Android host. Desktop/web, Android-only kind and hardened use of this recipe MUST be rejected with actionable compatibility issues.

#### Scenario: Native mobile project
- **WHEN** a user selects SwiftUI with Android+iOS
- **THEN** shared Kotlin logic and tests, an Android Compose host and a native SwiftUI host are generated without a Compose UIViewController bridge

#### Scenario: Unsupported targets
- **WHEN** SwiftUI is selected with desktop, web, no iOS or a hardened profile
- **THEN** plan reports the conflicting field and creates no files

### Requirement: Shared observable example
Both native interfaces SHALL display and update the same shared counter logic through platform state holders. The generated shared logic MUST have tests for its behavior and dependency creation. The Swift API MUST avoid requiring a coroutine or shared ViewModel bridge.

#### Scenario: Increment action
- **WHEN** the increment action is invoked on either platform
- **THEN** it calls the shared counter and refreshes that platform's displayed count

### Requirement: Honest native verification
The plan SHALL include shared tests, framework linking and an explicit Xcode application build command for SwiftUI. Unavailable Xcode and unexecuted Swift/UI checks MUST be reported separately from generation and passing Android or shared tests.

#### Scenario: Missing full Xcode
- **WHEN** SwiftUI is generated on a machine with only Command Line Tools
- **THEN** generation succeeds but iOS application verification is reported as unexecuted or skipped
