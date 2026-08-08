---
name: bootstrap-android-project
description: >
  Scaffold a new Android project with Clean Architecture: multi-module
  gradle setup with convention plugins, MVI Delegate pattern, Compose Material 3, Hilt DI,
  Room/DataStore, type-safe navigation, network api/impl split, and Konsist architecture
  tests that enforce layer boundaries. Use when starting a fresh Android app and wanting a
  hardened baseline rather than handcrafted boilerplate. Skip for: adding modules to an
  existing project (this skill only creates new projects) or non-Android
  Kotlin projects.
license: MIT
compatibility: >
  Requires zsh (the generator uses associative arrays and the :l case modifier),
  python3, and JDK 17+. The verification build additionally needs the Android SDK
  and network access for Gradle dependency resolution.
metadata:
  version: "1.0.0"
  repository: https://github.com/maxsoloma/android-kmp-bootstrap-skills
---

# bootstrap-android-project

Generate a hardened, multi-module Android project from inline templates. No template
engine — files are copied and seven placeholders are substituted with plain string
replacement.

## Placeholders (exactly seven, user-supplied)

| Token | Meaning | Example |
|-------|---------|---------|
| `{{PROJECT_NAME}}` | PascalCase root project name | `Acme` |
| `{{PACKAGE_BASE}}` | dotted package base | `com.acme.app` |
| `{{PACKAGE_PATH}}` | slash-form of the package base | `com/acme/app` |
| `{{APP_ID}}` | Android `applicationId` in `:app` | `com.acme.app` |
| `{{MIN_SDK}}` | `android.minSdk` in `gradle.properties` | `26` |
| `{{TARGET_SDK}}` | `android.targetSdk` in `gradle.properties` | `35` |
| `{{FEATURE_NAME}}` | PascalCase name of the starter feature | `Main` |

There is no eighth token. Convention-plugin ids use a fixed literal prefix `app.`
(e.g. `app.library.noresources`, `app.hilt`) that is identical in every generated
project, so it never needs substitution.

## Phase 1 — Prompts

Ask the four prompts verbatim from `supporting/prompts.md`:
basics, optional modules, first feature, plan approval. Do not write to disk before the
user approves the plan tree (prompt 4).

## Phase 2 — Pre-flight (fail fast with an actionable hint)

- **Abort guard:** if `outputPath` already contains `settings.gradle.kts`, STOP and tell the
  user the target is an existing project. The skill never modifies an existing project.
- `outputPath` must not exist or must be empty.
- `java -version` reports JDK 17+.
- Gradle wrapper is reachable.

## Phase 3 — Generation

**Run the committed generator script** (do not hand-follow the prose below when a script exists):

```bash
<skill-dir>/supporting/generate.sh \
  <outputPath> \
  <PROJECT_NAME> <PACKAGE_BASE> <APP_ID> \
  <MIN_SDK> <TARGET_SDK> \
  <FEATURE_NAME> \
  [optionalModule...]
```

Argument order matches exactly: outputDir PROJECT_NAME PACKAGE_BASE APP_ID MIN_SDK TARGET_SDK FEATURE_NAME [optional...].
Example: `generate.sh .scratch/demo Sample io.sample.app io.sample.app 26 35 Main database billing`

The script is located at `supporting/generate.sh` relative to the skill root.
It is self-contained and deterministic: all steps below are its specification.

### What the script does (specification — authoritative source of truth)

1. Copy the required templates into their destinations (all paths relative to `outputPath`):

   | Template dir | Destination in `outputPath` |
   |---|---|
   | `templates/_root/.` | `.` (project root — copy **contents**, not the dir itself) |
   | `templates/_build-logic` | `build-logic/` |
   | `templates/_app` | `app/` |
   | `templates/core-required/.` | `core/` (copy **contents**, not the dir itself; each sub-dir becomes `core/<sub>/`) |
   | `templates/feature-main` | `feature/<featureName.lowercase()>/` (e.g. `feature/main/`) |

   This mapping is why `includeBuild("build-logic")` in `settings.gradle.kts` resolves
   correctly, the Gradle wrapper scripts are at the project root, and step 4's `include()`
   scan under `app/`, `feature/`, and `core/` finds all modules.

2. Expand the optional selection: read each selected `core-optional/<x>/_manifest.yml` and
   walk `requires:` transitively (see `supporting/module-dependencies.md`). Copy every
   module in the closure to `core/<moduleName>/` in `outputPath` (e.g. `templates/core-optional/billing`
   → `core/billing/`). A user picking `billing` therefore also gets `remote-config`,
   `analytics`, and `config`.
3. Two-pass substitution over every `*.template` file:
   - **Pass A (context-sensitive `{{FEATURE_NAME}}`):** `{{FEATURE_NAME}}` appears in two
     contexts that must use different cases.
     Substitute FIRST with the **lowercase** form in package/path contexts:
       - `feature.{{FEATURE_NAME}}` → `feature.<featureName.lowercase()>` (package declarations)
       - `projects.feature.{{FEATURE_NAME}}` → `projects.feature.<featureName.lowercase()>` (DSL accessor)
       - `/feature/{{FEATURE_NAME}}` → `/feature/<featureName.lowercase()>` (directory references in strings)
     Then substitute remaining `{{FEATURE_NAME}}` → `<featureName>` (PascalCase, for class/function names).
   - **Pass B (remaining six tokens):** substitute `{{PROJECT_NAME}}`, `{{PACKAGE_BASE}}`,
     `{{PACKAGE_PATH}}`, `{{APP_ID}}`, `{{MIN_SDK}}`, `{{TARGET_SDK}}`.
   - Strip the `.template` suffix.
   - Rename path segment `{{PACKAGE_PATH}}` to the real package path (e.g. `com/demo/app`).
   - Rename path segment `{{FEATURE_NAME}}` to the **lowercase** feature name (e.g. `main`).
   - Rename any file stem carrying `{{FEATURE_NAME}}` (e.g. `{{FEATURE_NAME}}ViewModel.kt` →
     `MainViewModel.kt`) to the **PascalCase** feature name. This applies to all template
     files whose basename begins with `{{FEATURE_NAME}}`:
     `{{FEATURE_NAME}}ViewModel.kt`, `{{FEATURE_NAME}}Screen.kt`,
     `{{FEATURE_NAME}}Destination.kt`, `{{FEATURE_NAME}}NavGraphBuilder.kt`,
     `{{FEATURE_NAME}}NavigationModule.kt`, `{{FEATURE_NAME}}ModelUi.kt`,
     `{{FEATURE_NAME}}ViewModelTest.kt`.
   - Rename any `{{PROJECT_NAME}}Theme.kt` to `<projectName>Theme.kt`.
   - NEVER substitute binary files (the Gradle wrapper jar). The literal `app.` plugin
     prefix is left as-is.
4. Generate `settings.gradle.kts`: emit exactly one `include(...)` line per directory that
   contains a `build.gradle.kts` (after the `.template` suffix is stripped) under `app/`,
   `feature/`, and `core/`. Convert each such directory's path to a Gradle module path by
   replacing `/` with `:` and prefixing `:` — this correctly handles the nested modules
   (`:core:network:api`, `:core:network:impl`, `:core:resources:strings`,
   `:core:resources:graphics`) and any custom-named feature from Prompt 3, while skipping
   container dirs (`core/network/`, `core/resources/`) that have no `build.gradle.kts`.
   Never emit an `include` for `build-logic` (it is wired via `includeBuild`).
5. Ship `gradle/libs.versions.toml`, `gradle.properties`, and `config/detekt.yml` as-is —
   versions and rules are baked into the templates (they ARE `.template` files that carry
   the SDK tokens, so they will be substituted in step 3).

## Phase 4 — Verification (run in outputPath)

Run, in order, surfacing stderr on failure and never auto-fixing
(full detail in `supporting/verification.md`):

```
./gradlew detekt :core:architecture-test:test --rerun-tasks :app:assembleDebug test
```

The `--rerun-tasks` is required because Konsist uses `scopeFromProduction()` which
Gradle's UP-TO-DATE check may otherwise skip, causing false negatives.

## Phase 5 — Report

- absolute path to the generated project
- module count split required vs optional
- per-rule status of the 8 architecture tests (`supporting/konsist-rules.md`)
- next steps: "Open in Android Studio", "git init && first commit"

## Hardening (see supporting/konsist-rules.md)

Eight Konsist rules enforce layer boundaries; rule 8 enforces `// SUPPRESS-RATIONALE:`
on every `@Suppress`. The template ships NO `detekt-baseline.xml`, and the detekt
convention's `checkNoDetektBaseline` task fails the build if one appears. Do not generate
a baseline to silence violations.
