# Spec Delta

## MODIFIED Requirements

### Requirement: Installed skill discovery
Codex SHALL discover the bootstrap skill from an installed plugin, with its
self-contained generator available through a skill-relative launcher.

#### Scenario: Install and discover outside the source checkout
- **WHEN** the user installs the marketplace plugin and opens a fresh workspace
- **THEN** native Codex discovery lists the skill and its cached launcher runs there without a user-installed Java
