# Post-generation verification (Phase 4)

Run inside `<outputPath>`. Cross-platform subset (ANY OS):

```bash
./gradlew detekt                      # 0 errors; analyses commonMain via detektMetadata*; fails if any detekt-baseline.xml exists
./gradlew :core:konsistTest:test --rerun-tasks      # all 11 rules pass
./gradlew :androidApp:assembleDebug   # android build succeeds
./gradlew test                        # common/jvm/android unit tests
```

iOS link — macOS + Xcode ONLY (gated on Phase-2 `iosVerifiable`):

```bash
./gradlew :feature:<featureName>:assembleSharedXCFramework    # iOS framework link
```

## detekt note
The detekt convention makes the `detekt` task depend on the
`detektMetadata*` tasks, so plain `./gradlew detekt` analyses commonMain.
`./gradlew detektAll` is the equivalent explicit aggregate.

## Reporting rule (Delta 4 — never overclaim)
- If `iosVerifiable == false`: report iOS verification **SKIPPED (host not macOS)**.
  NEVER print a pass for a step that did not run.
- On any failure: surface stderr verbatim, do NOT auto-fix.

## Baseline guard
The root `failIfDetektBaselineExists` task FAILS the build if any
`detekt-baseline.xml` is found (wired into `check`). Generated CLAUDE.md
instructs: do not create a baseline to silence violations.
