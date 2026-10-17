# guided-bootstrap Specification

## Purpose

Guide users through existing bootstrap recipes in a terminal or an agent conversation while preserving safe, reviewable project creation.

## Requirements

### Requirement: Terminal wizard
Init SHALL provide numbered Toolchain recipes, editable project basics, targets, UI, DI, navigation, network/storage, SDK values and agent adapters. It MUST use the same compatibility validation as config mode and allow correction of invalid input without terminating the session.

#### Scenario: Current profiles
- **WHEN** a user selects a supported Kotlin Toolchain recipe
- **THEN** the wizard offers the choices supported by that profile and retains its generation semantics

### Requirement: Backtracking and dependent choices
The wizard SHALL support going back and editing individual steps from review. Changing the recipe or targets MUST identify incompatible dependent answers and require their correction rather than silently altering them.

#### Scenario: Switch to hardened
- **WHEN** a user requests a retired hardened configuration
- **THEN** the retirement is explained and generation remains unavailable until a supported recipe is selected

### Requirement: Review before writing
Init SHALL show a generation plan and explicit create action before publishing. Users MUST be able to save a valid configuration separately without creating the project. Cancellation, EOF or interruption before create/save MUST leave the output untouched.

#### Scenario: Cancel at review
- **WHEN** a user cancels instead of creating
- **THEN** no project or draft file is written

#### Scenario: Save configuration
- **WHEN** a user explicitly saves the reviewed config to an unused path outside the output directory
- **THEN** only that file is created and it can be passed to plan or generate

### Requirement: Noninteractive safety
Init without a terminal MUST return an actionable structured error rather than waiting for input. Config, catalog and plan commands MUST never prompt. Nonempty output paths and symlinks MUST remain protected in every entry point.

#### Scenario: Agent invokes init without TTY
- **WHEN** init is called with no interactive console
- **THEN** it exits promptly and directs the caller to catalog and config-based planning

### Requirement: Agent workflow
Each skill SHALL describe a portable dialog that uses catalog and plan to collect only missing material choices, exposes compatibility explanations and invokes config generation when project creation is authorized. It MUST NOT depend on a particular MCP UI or start a hidden terminal wizard.

#### Scenario: Different agent clients
- **WHEN** Codex or Claude runs the skill without specialized form tools
- **THEN** ordinary conversation and the same CLI config protocol are sufficient to generate the project

### Requirement: Verification remains explicit
Wizard creation SHALL report generation separately from builds and provide the applicable verification commands. Existing Toolchain verifier checks MUST remain intact; missing SDKs MUST NOT be presented as passing checks.

#### Scenario: No Xcode
- **WHEN** a user generates a profile containing iOS on a host without Xcode
- **THEN** generation may succeed but no iOS build success is claimed
