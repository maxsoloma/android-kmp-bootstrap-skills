## Purpose

Let Codex users install the bootstrap plugin and create runnable projects through
its bundled skill without relying on the plugin author's repository checkout.

## ADDED Requirements

### Requirement: Installed skill discovery
Codex SHALL discover the bootstrap skill from an installed plugin, with its
self-contained generator available at a skill-relative path.

#### Scenario: Install and discover outside the source checkout
- **WHEN** the user installs the marketplace plugin and opens a fresh workspace
- **THEN** native Codex discovery lists the skill and its cached JAR runs there

### Requirement: Equivalent terminal and agent bootstrap flows
The installed skill SHALL support interactive terminal init and the documented
agent config flow with equivalent validated recipes and reproducible output.

#### Scenario: Review and generate
- **WHEN** a user selects components, reviews or edits the recipe and creates it
- **THEN** the project preserves those choices and can be built and launched

#### Scenario: Agent uses the installed skill
- **WHEN** Codex invokes the installed skill with a fully specified project request
- **THEN** it uses catalog, config, plan and generation from the cached generator

### Requirement: Existing generation guarantees remain
Plugin flows MUST retain compatibility validation, explicit generation/build
status and refusal to overwrite existing projects.

#### Scenario: Invalid or cancelled request
- **WHEN** a recipe is incompatible, cancelled or init has no terminal
- **THEN** no project is published and actionable feedback is reported

#### Scenario: Existing destination
- **WHEN** generation targets an existing project
- **THEN** its files remain unchanged and generation fails explicitly
