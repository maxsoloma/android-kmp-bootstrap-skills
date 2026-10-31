# Proposal

## Why

Both skills ship a committed 2.9 MB `bin/bootstrap.jar` and require the user to
install Java 17+ before the first wizard step. The pinned Kotlin Toolchain wrapper
that the skills already bundle provisions its own JRE and JDK, and a prototype
showed `kotlin run` executing the real CLI from sources with clean JSON stdout and
byte-identical generated projects. Replacing the JAR removes the binary from Git and
the user-installed Java prerequisite, while reusing any suitable Java already present.

## What Changes

- **BREAKING (distribution)**: remove `bin/bootstrap.jar` from both skills. Each
  skill instead carries the CLI sources, its Toolchain module/plugin configuration
  and byte-identical upstream `kotlin`/`kotlin.bat` wrappers.
- Add skill-relative launchers `bin/bootstrap` (POSIX sh) and `bin/bootstrap.cmd`
  (Windows) that run the CLI through the bundled wrapper with build output in a
  per-user cache outside the installed skill, and forward arguments, stdin, stdout,
  stderr and exit codes unchanged.
- Launchers reuse existing Java instead of downloading it again:
  - an explicit `KOTLIN_CLI_JAVA_HOME` is always respected;
  - otherwise a `JAVA_HOME` (then `java` on `PATH`) runtime of version 25+ is passed
    as the CLI runtime; older or stub runtimes are ignored and the wrapper's pinned
    JRE is used;
  - CLI compilation keeps JDK `selectionMode: auto`, so a matching `JAVA_HOME` JDK is
    reused and only a missing version is provisioned.
- The first launcher run may download the Toolchain distribution, JRE/JDK when no
  suitable one exists, the Kotlin compiler and Maven dependencies into the shared
  Toolchain cache. Later runs and project generation work offline.
- Skill instructions, README, INSTALL, BUILD, RELEASE, CLI and verification docs,
  scripts and CI switch from `java -jar …/bootstrap.jar` to the launcher. The
  README no longer lists Java 17+ as an install prerequisite.
- `./kotlin do bundle` syncs the CLI sources into both skills instead of building
  JARs; CI checks the committed skill copies are current.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `toolchain-projects`: "Portable CLI delivery" changes from a standalone JVM
  executable to a source-delivered launcher with Java reuse; "Toolchain project
  generation" scopes the offline guarantee to after one-time CLI provisioning.
- `codex-plugin-integration`: "Installed skill discovery" refers to the skill-relative
  launcher instead of the cached JAR.

## Impact

- Skill layout: `skills/bootstrap-project/` and `skills/orchestrate-feature/`
  (`bin/`, new CLI source copy, wrappers), `build-tools/` (bundle task and
  resource-path inputs), root `module.yaml`/test settings.
- Tests: `BootstrapTest`, `FeatureTest`, `SkillDiscoveryTest` isolated-installation
  cases move from copied JARs to copied skills with launchers.
- `.github/workflows/validate.yml`, `scripts/verify-*.sh`, docs, `SKILL.md` files
  and references, plugin manifests/version, `CHANGELOG.md`, `AGENTS.md`.
- Users: no Java prerequisite; first invocation needs network unless the Toolchain
  cache is already warm (for example, from building a generated project).
- Generated applications, recipes, locks and the Kotlin Toolchain backend are
  unchanged.
