---
name: bootstrap-kmp-project
description: >
  Scaffold a new Kotlin Multiplatform (Android + iOS) project with
  Clean Architecture: feature-modular Gradle setup with class-based convention plugins,
  shared Compose Multiplatform UI, MVI on a commonMain androidx ViewModel, Koin DI,
  Ktor networking (api/impl split), Room KMP + DataStore, type-safe Compose navigation,
  SKIE iOS interop, and Konsist architecture tests that enforce layer boundaries AND
  KMP source-set boundaries (commonMain platform-API isolation, expect/actual
  placement) at build time. Use when starting a fresh KMP app and wanting a hardened
  baseline. Skip for: Android-only apps (use bootstrap-android-project), adding
  modules to an existing project (this skill only creates new projects), or
  non-Android/iOS Kotlin projects.
license: MIT
compatibility: >
  Requires zsh (the generator uses associative arrays and the :l case modifier),
  python3, and JDK 17+. The verification build additionally needs the Android SDK
  and network access for Gradle dependency resolution. iOS link verification is
  skipped unless run on macOS with Xcode installed.
metadata:
  version: "1.0.1"
  repository: https://github.com/maxsoloma/android-kmp-bootstrap-skills
---

# bootstrap-kmp-project

Generates a hardened KMP project tree from `supporting/templates/` by copying
`*.template` files (suffix stripped) and substituting EIGHT placeholders via
plain string.replace: `{{PROJECT_NAME}}`, `{{PACKAGE_BASE}}`, `{{PACKAGE_PATH}}`,
`{{APP_ID}}`, `{{MIN_SDK}}`, `{{TARGET_SDK}}`, `{{IOS_DEPLOYMENT_TARGET}}`,
`{{FEATURE_NAME}}`.

## When NOT to run
Abort if `<outputPath>` already contains `settings.gradle.kts` — this skill
never modifies an existing project.

## Flow
1. **Prompts** — `supporting/prompts.md` (4 prompts; plan approval before any write).
2. **Pre-flight** — outputPath empty; `java -version` ≥ 17; Gradle wrapper 9.x;
   probe `xcodebuild -version` → set `iosVerifiable`. Warn (do not fail) if false.
3. **Generation** — Run `supporting/generate.sh <outputDir> <PROJECT_NAME> <PACKAGE_BASE> <APP_ID> <FEATURE_NAME> [optionalModule...]`
   (from the skill root, e.g. `.claude/skills/bootstrap-kmp-project/`).
   The script implements the following generation logic:
   - Copy `_root/`, `_build-logic/`, `_android-app/`, `_ios-app/`, all
     `core-required/*`, `feature-main/`.
   - Expand optional selection via `supporting/module-dependencies.md` closure;
     copy each `core-optional/<m>/` in the closure.
   - **Single pass substitution** — strip `.template` suffix, then substitute all
     8 tokens in: **(a) file CONTENT**, **(b) directory names**, **(c) file names**.
     All three must be substituted, not just content. Examples of token-bearing names:
     - `{{PROJECT_NAME}}Application.kt.template` → `AcmeApplication.kt`
     - `{{FEATURE_NAME}}ViewModel.kt.template` → `MainViewModel.kt`
     - `Feature{{FEATURE_NAME}}Module.kt.template` → `FeatureMainModule.kt`
     - `feature/{{FEATURE_NAME}}/` dir → `feature/main/`
   - Build-logic class-based plugin files live under
     `build-logic/convention/src/main/kotlin/{{PACKAGE_PATH}}/convention/` —
     substitute `{{PACKAGE_PATH}}` in the directory path.
   - Emit `settings.gradle.kts` with `includeBuild("build-logic")` +
     `include(":core:<m>")` for every copied module + `include(":androidApp")` +
     `include(":feature:<featureName>")` where `<featureName>` is `{{FEATURE_NAME}}`
     lowercased.
   - `libs.versions.toml` ships baked pins (see Tech Stack in the plan header).
4. **Verification** — `supporting/verification.md` (cross-platform subset +
   macOS-gated iOS link). NEVER claim a step that did not run.
5. **Report** — absolute project path; module count (required/optional split);
   per-rule status of the 11 Konsist rules; iOS status RAN/SKIPPED; next steps
   ("Open in Android Studio", "open iosApp.xcodeproj in Xcode", "set iOS team",
   "git init").

## Token reference

| Token | Golden literal | Used in |
|-------|---------------|---------|
| `{{PROJECT_NAME}}` | Acme | Root project name, Application class, theme, manifest label |
| `{{PACKAGE_BASE}}` | com.acme.outfit | All package declarations, plugin IDs |
| `{{PACKAGE_PATH}}` | com/acme/outfit | Directory paths for Kotlin source sets |
| `{{APP_ID}}` | com.acme.outfit | `applicationId` in androidApp + iOS `PRODUCT_BUNDLE_IDENTIFIER` |
| `{{MIN_SDK}}` | 26 | `minSdk` in libs.versions.toml |
| `{{TARGET_SDK}}` | 36 | `targetSdk` in libs.versions.toml |
| `{{IOS_DEPLOYMENT_TARGET}}` | 15.0 | `IPHONEOS_DEPLOYMENT_TARGET` in project.pbxproj |
| `{{FEATURE_NAME}}` | Main | Feature identifiers (class name prefix) + `main` in dir/package paths |

Note: `compileSdk` is also 36 in the golden but has NO token — it stays literal `36`.

### FEATURE_NAME dual-case rule (important)
`{{FEATURE_NAME}}` is prompted and stored as **PascalCase** (e.g. `Main`). The generator
applies it in two forms:
- **PascalCase verbatim** — class names, file names, identifiers:
  `{{FEATURE_NAME}}ViewModel`, `{{FEATURE_NAME}}Screen`, `Feature{{FEATURE_NAME}}Module.kt`
- **lowercased** — package segments, directory names, Gradle path:
  `feature/{{FEATURE_NAME}}/` becomes `feature/main/`, `:feature:{{FEATURE_NAME}}`
  becomes `:feature:main`, package `...feature.{{FEATURE_NAME}}.` becomes `...feature.main.`

`detekt`'s `PackageNaming` rule requires lowercase-starting package segments — PascalCase
in a package declaration is a detekt violation. The generator MUST lowercase `{{FEATURE_NAME}}`
before inserting it into package declarations and directory paths.

## Hardening (Level C)
- 11 Konsist rules — `supporting/konsist-rules.md` (rules 9–11 are KMP-specific;
  rule 9 is placement-only because the compiler already enforces completeness).
- Zero-baseline detekt with commonMain metadata analysis (`./gradlew detekt`
  transitively runs `detektMetadata*`); a baseline file = build failure.
- Suppress-rationale (rule 8): every `@Suppress` needs `// SUPPRESS-RATIONALE:`.

## KMP deltas (see spec)
- iosApp = opaque Xcode templates — `supporting/ios-app-templating.md`.
- Resources via `compose.resources` (`Res` accessor), not strings.xml.
- Build-time config via BuildKonfig, not BuildConfig + flavors.
- Convention plugins are CLASS-BASED (`*ConventionPlugin.kt`) under
  `build-logic/convention/src/main/kotlin/{{PACKAGE_PATH}}/convention/`.

## Known limitations
- **iOS framework link is macOS + Xcode.app-gated.** `assembleSharedXCFramework` and
  `xcodebuild` require a full **Xcode.app** (Command Line Tools alone are not enough).
  iOS Kotlin source compiles (`compileKotlinIosSimulatorArm64`); the framework link +
  Xcode build are verifiable only on a machine with Xcode.app installed.
- **Adding a second feature (Prompt 3) needs one manual rename.** `core/navigation`
  `TopLevelDestination` carries a literal `MAIN` enum constant. The generator substitutes
  `{{FEATURE_NAME}}` into class / package / directory names, but not into that constant's
  UPPER form — so renaming the starter feature works, while adding a *second* feature
  requires a manual enum-constant rename. (A `{{FEATURE_NAME_UPPER}}` token would close this.)
