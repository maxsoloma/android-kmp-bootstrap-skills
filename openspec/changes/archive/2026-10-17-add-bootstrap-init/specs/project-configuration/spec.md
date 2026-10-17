## Purpose

Provide portable project choices and consistent compatibility feedback for terminal, agent and automated generation workflows.

## ADDED Requirements

### Requirement: Strict portable configuration
The CLI SHALL accept a versioned JSON configuration covering all current profile, target, identifier, SDK, agent and optional-module choices. It MUST reject unsupported versions, unknown fields, invalid types and incompatible choices, and report missing required inputs without prompting in config mode.

#### Scenario: Missing agent answers
- **WHEN** plan receives a config without name and package
- **THEN** it returns machine-readable missing fields and exits unsuccessfully without writing any project files

#### Scenario: Invalid selection
- **WHEN** config requests hardened web, a new UI recipe, or unknown optional modules
- **THEN** validation explains the incompatibility and generation writes nothing

### Requirement: Discoverable current recipes
The CLI SHALL expose a JSON catalog of existing profiles, platform and UI constraints, defaults, agent adapters and hardened modules with dependencies. Future native SwiftUI support MUST be marked unavailable rather than silently mapped to shared Compose.

#### Scenario: Agent discovers choices
- **WHEN** an agent requests catalog JSON
- **THEN** it receives enough field and profile information to collect a config without assuming a specific client's UI tools

### Requirement: Read-only generation plan
The CLI SHALL resolve a valid config into a deterministic plan containing normalized choices, files, modules, resolved optional dependencies, pinned toolchain information, environment requirements and verification scope. Planning MUST NOT create directories or claim that build checks passed.

#### Scenario: Transitive module selection
- **WHEN** a hardened config selects billing
- **THEN** the plan shows its required modules separately from the user's direct selection

### Requirement: Reproducible config generation
Config-based generation SHALL reuse the existing renderers and publication guards and additionally emit portable spec and lock files. The lock MUST bind the normalized spec to the bundled recipe content and generator version; a supplied mismatching lock MUST fail before writes.

#### Scenario: Repeat a recipe
- **WHEN** the emitted spec and matching lock are used with the same generator resources at a new destination
- **THEN** generated files have identical bytes and contain no destination-specific path in the spec or lock

#### Scenario: Stale lock
- **WHEN** the spec, recipe resources or lock fields do not match the supplied lock
- **THEN** generation fails with an explanation and leaves the destination unchanged

### Requirement: Preserve established CLI behavior
Existing generate flags, standalone skill installation, hardened shell adapters, doctor and verify SHALL remain available. Existing flag-based generation MUST retain its output inventory and bytes, including wrapper binaries and verification metadata.

#### Scenario: Regression fixtures
- **WHEN** previous modern and hardened inputs are generated through existing flags
- **THEN** the existing golden tests still pass without updating their hashes
