# Interactive prompts (Phase 1)

## Prompt 1 — basics
- `projectName` (PascalCase, e.g. `Acme`) → `{{PROJECT_NAME}}`
- `packageBase` (dotted, e.g. `com.acme.outfit`) → `{{PACKAGE_BASE}}`
  (derive `{{PACKAGE_PATH}}` = packageBase with `.`→`/`)
- `appId` (= application/bundle ID, default = packageBase) → `{{APP_ID}}`
- `minSdk` (default 26) → `{{MIN_SDK}}`
- `targetSdk` (default 36) → `{{TARGET_SDK}}`
- `iosDeploymentTarget` (default 15.0) → `{{IOS_DEPLOYMENT_TARGET}}`
- `featureName` (PascalCase, e.g. `Main`) → `{{FEATURE_NAME}}` (lowercase used in dir paths, e.g. `feature/main`)
- `outputPath` (default `./<kebab-projectName>/`)

## Prompt 2 — optional modules (multi-select)
Checklist of: database (PRESELECTED), datastore, analytics, auth, config,
image, permissions, dma, sync, billing. Show transitive deps inline, e.g.
"billing → also adds analytics, config". After selection, expand the closure
(see module-dependencies.md) and re-display the final set for confirmation.

## Prompt 3 — first feature
Optional extra feature name beyond the starter `feature:<featureName>`, or "single feature only".

## Prompt 4 — plan approval
Render the full file tree + expanded module list. NO disk write before
explicit approval. If `outputPath` already contains `settings.gradle.kts`,
ABORT — this skill never modifies an existing project.
