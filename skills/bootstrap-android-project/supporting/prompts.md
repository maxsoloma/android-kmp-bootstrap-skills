# Interactive Prompts

## Prompt 1 — Basics

Collect, with the noted defaults:

- `projectName` — PascalCase (becomes `{{PROJECT_NAME}}`).
- `packageBase` — dotted, lowercase (becomes `{{PACKAGE_BASE}}`; slash form is `{{PACKAGE_PATH}}`).
- `appId` — application id (becomes `{{APP_ID}}`; defaults to `packageBase`).
- `minSdk` — default `26` (becomes `{{MIN_SDK}}`).
- `targetSdk` — default `35` (becomes `{{TARGET_SDK}}`).
- `outputPath` — default `./<kebab-case projectName>/`.

## Prompt 2 — Optional modules (multi-select)

Present this checklist. Each line pre-notes transitive deps that will be auto-added:

- [ ] `config` — BuildConfig fields + debug/release flavors. (no deps)
- [ ] `analytics` — provider-agnostic AnalyticsTracker. (adds: config)
- [ ] `remote-config` — Firebase Remote Config typed facade. (adds: config, analytics)
- [ ] `database` — Room + DataStore + KSP. (no deps)
- [ ] `sync` — PendingChangesManager + WorkManager. (no deps)
- [ ] `auth` — token storage + OkHttp auth interceptor. (adds: config, sync, analytics)
- [ ] `billing` — Google Play Billing + PremiumStateRepository. (adds: remote-config, analytics → config)
- [ ] `image` — Coil 3 ImageLoader + RemoteImage helper. (adds: config)
- [ ] `permissions` — runtime permission flow. (adds: design — always present)
- [ ] `dma` — consent / Digital-Markets-Act adapter. (adds: config, analytics)

After selection, show the EXPANDED closure (selected + all transitive deps) so the user
sees exactly which optional modules will be generated.

## Prompt 3 — First feature

Ask for the starter feature name (becomes `{{FEATURE_NAME}}`), default `Main`:
"Name the starter feature module (PascalCase, e.g. 'Home'), or press Enter for 'Main'."
The feature is scaffolded from `templates/feature-main/` with the given name substituted
for all `{{FEATURE_NAME}}` tokens.

## Prompt 4 — Plan approval

Render the full file tree that WILL be written plus the expanded optional-module list, and
ask for explicit approval. Write nothing to disk until the user approves.
