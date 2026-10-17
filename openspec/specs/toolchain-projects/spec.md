# toolchain-projects Specification

## Purpose

Create portable, reproducible Kotlin Toolchain applications through terminal,
configuration and coding-agent workflows with working cross-platform stacks.

## Requirements

### Requirement: Toolchain project generation
The generator SHALL produce self-contained Kotlin Toolchain projects for every supported modern platform and UI choice, with pinned wrappers and dependencies. Generation MUST work offline and MUST NOT install a global CLI, initialize Git or overwrite existing project files.

#### Scenario: Independent installation
- **WHEN** a copied executable generates an app outside the repository
- **THEN** all selected modules, wrappers, sources and configuration are present without sibling resources

### Requirement: Preserved application capabilities
Projects SHALL retain manual/Koin DI, applicable Compose/SwiftUI navigation, optional Ktor networking and optional native/browser/JVM key-value storage, including lifecycle ownership, cancellation, failure states and meaningful generated tests.

#### Scenario: Selected stack
- **WHEN** a supported UI, DI, navigation, network and storage combination is generated
- **THEN** its application and tests use the selected real integrations on each selected platform

### Requirement: Shared configuration and guided flow
Catalog, plan, terminal wizard and agent instructions SHALL use the same strict compatibility rules and expose Toolchain build/test commands. Invalid choices and malformed input MUST fail before publication. Config and lock replay MUST reproduce the same generated files with the matching executable.

#### Scenario: Locked replay
- **WHEN** a valid config and matching lock are replayed at another empty destination
- **THEN** generated files are byte-identical

### Requirement: Retired backends are removed
The distribution SHALL exclude Gradle generation/build infrastructure and hardened profiles, templates and dedicated skills. Requests for retired profiles or their optional modules MUST fail explicitly before writes. Existing generated applications MUST NOT be modified automatically.

#### Scenario: Retired profile requested
- **WHEN** a config or command selects hardened or retired module options
- **THEN** the CLI explains removal and creates no destination

### Requirement: Verified default
Kotlin Toolchain SHALL become the default only after actual generated Android, iOS, desktop and browser builds and tests pass the documented stack matrix. Verification MUST distinguish compilation, native/browser tests and application runtime checks, retain logs and report failures or unavailable checks without claiming full success.

#### Scenario: Integration matrix passes
- **WHEN** the documented matrix passes actual builds and tests
- **THEN** the shipped catalog, wizard, skill and CLI default to the verified Toolchain recipes

### Requirement: Portable CLI delivery
The generator SHALL remain a standalone Kotlin/JVM executable with no Python runtime. Its own build, tests and packaging SHALL use Kotlin Toolchain. Claude and Codex plugin metadata MUST discover the surviving skill consistently.

#### Scenario: Standalone executable
- **WHEN** the bundled executable is copied without the source repository
- **THEN** catalog, planning, generation, doctor and verification remain available
