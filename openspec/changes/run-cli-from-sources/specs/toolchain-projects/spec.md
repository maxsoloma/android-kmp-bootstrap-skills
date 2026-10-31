# Spec Delta

## MODIFIED Requirements

### Requirement: Toolchain project generation
The generator SHALL produce self-contained Kotlin Toolchain projects for every supported modern platform and UI choice, with pinned wrappers and dependencies. After the CLI's one-time provisioning, generation MUST work offline and MUST NOT install a global CLI, initialize Git or overwrite existing project files.

#### Scenario: Independent installation
- **WHEN** a copied skill generates an app outside the repository
- **THEN** all selected modules, wrappers, sources and configuration are present without sibling resources

#### Scenario: Offline after provisioning
- **WHEN** the launcher has completed one successful run and network access is then unavailable
- **THEN** catalog, plan and generate succeed without downloads

### Requirement: Portable CLI delivery
Each skill SHALL deliver the generator as Kotlin sources with a skill-relative launcher that runs them through the bundled pinned Kotlin Toolchain wrapper, with no committed executable archive and no Python runtime or user-installed Java prerequisite. Its own build, tests and packaging SHALL use Kotlin Toolchain. Claude and Codex plugin metadata MUST discover the surviving skill consistently.

#### Scenario: Standalone executable
- **WHEN** a skill directory is copied without the source repository or sibling skill and its launcher is invoked from another working directory
- **THEN** catalog, planning, generation, doctor, verification and feature commands remain available with relative paths resolved against the caller's working directory

#### Scenario: Machine-readable output
- **WHEN** the launcher runs a command that prints JSON, including the first run on an empty Toolchain cache
- **THEN** stdout contains only the command's output, provisioning messages go to stderr, and the CLI exit code is returned unchanged

#### Scenario: Equivalent output
- **WHEN** the same config is generated through the launcher and through the repository build of the same version
- **THEN** the generated trees are byte-identical

#### Scenario: Installed skill is not modified
- **WHEN** the launcher runs from an installed skill
- **THEN** compilation output is written to a per-user cache and no files inside the skill directory are created or changed

## ADDED Requirements

### Requirement: Existing Java reuse
The launcher SHALL reuse a suitable Java already present before any runtime is downloaded. An explicit `KOTLIN_CLI_JAVA_HOME` MUST be respected; otherwise a `JAVA_HOME` or `PATH` runtime that meets the Toolchain CLI's minimum version MUST be used. Missing, older or non-functional runtimes MUST fall back to the wrapper's pinned runtime. CLI compilation MUST reuse a `JAVA_HOME` JDK that matches its required version.

#### Scenario: Suitable Java installed
- **WHEN** `JAVA_HOME` points to a JDK that meets the CLI runtime minimum and the compilation version
- **THEN** the first run downloads no JRE or JDK

#### Scenario: Older Java installed
- **WHEN** only a Java runtime below the CLI runtime minimum is available
- **THEN** the launcher uses the wrapper's pinned runtime, reuses that Java for compilation only if it matches the compilation version, and the command succeeds

#### Scenario: Explicit runtime override
- **WHEN** `KOTLIN_CLI_JAVA_HOME` is set
- **THEN** the launcher passes it through unchanged and does not probe other runtimes

#### Scenario: No Java installed
- **WHEN** no functional Java is available, including a platform stub `java` command
- **THEN** the launcher provisions the pinned runtime and JDK through the wrapper and the command succeeds when network access is available
