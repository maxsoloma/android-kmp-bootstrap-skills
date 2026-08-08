# Post-generation Verification

Run these in `outputPath`, in order. On any failure: surface stderr verbatim and STOP — the
skill never auto-fixes generated code.

| Step | Command | Pass criterion |
|------|---------|----------------|
| 1 | `./gradlew detekt :core:architecture-test:test --rerun-tasks :app:assembleDebug test` | 0 detekt errors; no `detekt-baseline.xml`; all 8 Konsist rules pass; debug APK builds; all unit tests pass |

The `--rerun-tasks` is required because Konsist uses `scopeFromProduction()` which
Gradle's UP-TO-DATE check may otherwise skip, causing false negatives.

## Fault table

| Symptom | Likely cause | Hint to surface |
|---------|--------------|-----------------|
| `Plugin [id: 'app....'] not found` | build-logic not on classpath, or the `app.*` plugins not registered there | confirm `includeBuild("build-logic")` in settings and that build-logic registers the `app.` convention plugins |
| `Unresolved reference: BuildConfig` in `config` | `buildFeatures.buildConfig` missing | ensure `config` module kept its `buildConfig = true` |
| `architecture-test` rule #1 fails | a domain file imports a forbidden layer | the failing test names the file; fix the import, do not suppress |
| rule #8 fails | a `@Suppress` lacks `// SUPPRESS-RATIONALE:` | add the rationale comment on the preceding line |
| detekt fails on `detekt-baseline.xml present` | someone generated a baseline | delete the baseline; fix the underlying violations |
| KSP / Room error in `database` | `app.room` plugin not applied | confirm `app.room` in `core/database/build.gradle.kts` |
| tests skipped / UP-TO-DATE on arch-test | Gradle cached the test task | use `--rerun-tasks` as documented above |

## Acceptance checks (spec §5)

- Fresh empty dir → buildable project passing detekt + 8 arch tests + assembleDebug + test.
- Select `billing` alone → `remote-config`, `analytics`, `config` all present.
- Inject a Room `*Entity` import into a domain file → rule #1 fails naming the file.
- Add a `@Suppress` with no rationale → rule #8 fails.
- Add `detekt-baseline.xml` → detekt verification task fails.
- `outputPath` already has `settings.gradle.kts` → skill refuses (it never modifies an existing project).
