# runtime-acceptance Specification

## Purpose

Verify actual generated application startup, user interaction, networking and
persistence separately from compilation and unit test acceptance.

## Requirements

### Requirement: Real application startup and UI behavior
Supported generated UI recipes SHALL start on Android emulator, iOS simulator,
desktop JVM and browser Wasm and retain working navigation, networking and storage.

#### Scenario: Navigate and relaunch
- **WHEN** a user increments on Home and Details, returns, and restarts the app
- **THEN** navigation retains one state owner and Settings restores the saved count

#### Scenario: User-triggered real networking
- **WHEN** a user presses Fetch
- **THEN** the app displays the real HTTP result or a visible error without crashing

### Requirement: Runtime evidence and regression preservation
Acceptance SHALL record observed UI outcomes separately from builds/tests. Fixes
MUST preserve existing profiles of modern UI, DI, navigation, network/storage and
safe generation. Deprecated fixtures MUST be removed as authorized by the user.

#### Scenario: A runtime defect is found
- **WHEN** a generated application fails startup or a required UI scenario
- **THEN** the generator/template is corrected, appropriate regressions pass and
  the affected scenario is repeated on a newly generated application
