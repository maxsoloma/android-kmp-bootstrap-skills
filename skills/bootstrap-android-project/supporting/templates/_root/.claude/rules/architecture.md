# Architecture Rules

## Layer Boundaries
- `Presentation → Domain ← Data`. Domain has ZERO data/presentation imports.
- Feature modules may not import from another feature's data/presentation/domain packages.
- All `@Suppress` annotations require a `// SUPPRESS-RATIONALE:` comment on the preceding line.

## Module Guidelines
- New modules must apply the appropriate convention plugin (`app.library`, `app.feature`, etc.)
- Register every new module in `settings.gradle.kts`.
- Run `./gradlew detekt :core:architecture-test:test --rerun-tasks` after changes.
