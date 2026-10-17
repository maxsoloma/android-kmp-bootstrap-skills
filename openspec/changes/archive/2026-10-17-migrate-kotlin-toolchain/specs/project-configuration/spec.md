## MODIFIED Requirements

### Requirement: Strict portable configuration
The CLI SHALL accept a versioned JSON configuration covering current target, UI, DI, navigation, network/storage, identifier, SDK and agent choices. It MUST reject unsupported versions, unknown fields, invalid types and incompatible choices, and report missing required inputs without prompting in config mode.

#### Scenario: Missing agent answers
- **WHEN** plan receives a config without name and package
- **THEN** it returns machine-readable missing fields and exits unsuccessfully without writing any project files

#### Scenario: Invalid selection
- **WHEN** config requests a retired hardened profile, an unsupported UI recipe, or unknown optional modules
- **THEN** validation explains the incompatibility and generation writes nothing

### Requirement: Discoverable current recipes
The CLI SHALL expose a JSON catalog of supported Toolchain recipes, platform/UI constraints, defaults, component choices, agent adapters and dependencies. SwiftUI MUST be represented as a native mobile recipe with its actual platform restrictions.

#### Scenario: Agent discovers choices
- **WHEN** an agent requests catalog JSON
- **THEN** it receives enough field and profile information to collect a config without assuming a specific client's UI tools

### Requirement: Read-only generation plan
The CLI SHALL resolve a valid config into a deterministic plan containing normalized choices, files, modules, resolved integration dependencies, pinned toolchain information, environment requirements and verification scope. Planning MUST NOT create directories or claim that build checks passed.

#### Scenario: Transitive module selection
- **WHEN** a supported config selects Ktor and Settings
- **THEN** the plan shows matching platform adapters and dependency pins alongside the user's direct choices

### Requirement: Reproducible config generation
Config-based generation SHALL reuse the existing renderers and publication guards and additionally emit portable spec and lock files. The lock MUST bind the normalized spec to the bundled recipe content and generator version; a supplied mismatching lock MUST fail before writes.

#### Scenario: Repeat a recipe
- **WHEN** the emitted spec and matching lock are used with the same generator resources at a new destination
- **THEN** generated files have identical bytes and contain no destination-specific path in the spec or lock

#### Scenario: Stale lock
- **WHEN** the spec, recipe resources or lock fields do not match the supplied lock
- **THEN** generation fails with an explanation and leaves the destination unchanged

### Requirement: Preserve established CLI behavior
Supported Toolchain generate flags, standalone skill installation, doctor and verify SHALL remain available. Retired Gradle/hardened flags MUST fail explicitly. Supported generation MUST retain pinned wrapper bytes and truthful verification metadata.

#### Scenario: Regression fixtures
- **WHEN** supported Toolchain inputs and retired-profile requests are submitted through CLI flags
- **THEN** generation regressions pass, retired requests fail before writes and embedded released binaries retain their bytes
