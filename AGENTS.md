# Working on Android & KMP Bootstrap

This repository ships portable Agent Skills in `skills/bootstrap-project/` and
`skills/orchestrate-feature/`, plus separate Claude Code and Codex plugin manifests. Keep versions/discovery aligned.
Kotlin Toolchain is the only active project/build backend. Gradle and hardened
profiles were explicitly retired by the user in the 3.0 migration. Reject retired
configurations before writes; do not migrate or regenerate over existing applications.

Use Context7 resolve-library-id then query-docs for current library/framework/SDK/
API/CLI docs. Prefer versioned official sources and published artifacts. Source
refactoring and generator logic do not need documentation lookups. Do not assume
all agents have the same MCP tools.

Each skill must work when copied independently. Generator/doctor/verifier and
orchestration logic lives in the Kotlin/JVM CLI, delivered as each skill's
`bin/bootstrap.jar`. Generation never overwrites existing apps; the feature
workflow makes explicitly requested scoped edits without regeneration/migration.
No Python or sibling repository runtime is required. Preserve upstream wrappers
and embedded binary assets byte-for-byte. Never regenerate over existing projects.

Run `./kotlin do bundle` then `./kotlin test` after generator/packaging changes.
For toolchain/template changes generate temporary projects and run actual Kotlin
Toolchain builds, platform tests and Xcode checks. Record exact failures/skips;
generation tests do not establish that an application builds. Historical OpenSpec
artifacts retain migration decisions; retired templates and golden fixtures were deleted.
