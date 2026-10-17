# data-integrations Specification

## Purpose

Generate working network and local key-value integrations chosen through the same
platform-aware contract by terminal users, coding agents and automation.

## Requirements

### Requirement: Portable data choices
Modern configs SHALL accept network none/Ktor, storage none/Settings and an optional HTTPS endpoint for Ktor. Defaults MUST preserve current Toolchain recipe choices. Invalid types, unsupported choices, endpoint credentials/fragments, non-HTTPS URLs and requests for retired hardened data recipes MUST fail with structured issues before writes.

#### Scenario: Independent choices
- **WHEN** a modern config selects network only, storage only or both
- **THEN** generation includes only the selected integrations and required supporting dependencies

#### Scenario: Incompatible data request
- **WHEN** a config selects an unsupported provider or supplies an endpoint without Ktor
- **THEN** plan explains the conflicting field and creates no files

### Requirement: Real network behavior
Selecting Ktor SHALL generate a user-triggered request with loading, response and error states, request timeout and an owned client. Closing the UI owner MUST cancel in-flight work and prevent later callbacks. Generated tests MUST use a mock transport instead of public-service availability.

#### Scenario: Fetch and fail
- **WHEN** a generated request succeeds or returns an HTTP/transport error
- **THEN** the UI displays the response or error and leaves loading state

#### Scenario: Owner closes
- **WHEN** the UI owner closes during an in-flight request
- **THEN** the request is cancelled and no completion callback updates the disposed UI

### Requirement: Real local persistence
Selecting Settings SHALL persist the counter through a real adapter for each target. Reopening the store MUST restore the value. Storage failures MUST be exposed without silently substituting successful in-memory persistence. Browser origin/quota restrictions and platform-specific verification limits MUST be documented.

#### Scenario: Reopen
- **WHEN** a counter is incremented, its owner closes and another owner opens the same store
- **THEN** the saved count is restored

#### Scenario: Write failure
- **WHEN** persistence throws during an increment
- **THEN** the application shows the error and does not claim the failed value was saved

### Requirement: Platform adapters and integration
The data recipe SHALL select matching network engines and storage adapters for Android, iOS, desktop and Wasm. It MUST work with manual/Koin DI, Compose/SwiftUI and applicable navigation choices. Android permission/context setup and platform lifecycle handling MUST be generated where required.

#### Scenario: Universal project
- **WHEN** all four modern targets select both data integrations
- **THEN** shared APIs use platform-specific engines and persisted storage without fake native or browser implementations

### Requirement: Guided discovery and reproducibility
Catalog, plan, wizard and skills SHALL expose network/storage choices, endpoint validation, adapters, dependency pins and limitations. Targeted editing MUST retain other answers and expose conflicts. New choices MUST replay through spec/lock. Current Toolchain defaults, standalone delivery and verification checks MUST remain intact; retired profiles MUST fail before writes.

#### Scenario: Edit data options
- **WHEN** the user changes a provider or profile after entering an endpoint
- **THEN** the endpoint remains visible as incompatible until explicitly removed or corrected

#### Scenario: Replay
- **WHEN** the generated spec and lock are replayed at a new destination with the matching CLI
- **THEN** generated files are byte-identical
