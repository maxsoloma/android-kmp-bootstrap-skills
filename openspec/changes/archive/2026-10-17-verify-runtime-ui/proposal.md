# Runtime and UI acceptance

## Why

Build/unit/browser-test acceptance is green, but actual app startup, navigation,
user-triggered networking and persistence after process restart need direct UI
validation. The user also explicitly authorized deletion of the three remaining
obsolete fixture/cache directories before runtime checks.

## What Changes

Delete retired golden/Gradle fixtures and obsolete local cache, update references,
then generate fresh app fixtures from the standalone JAR. Exercise shared Compose
on Android emulator, iOS simulator, desktop and browser, plus native SwiftUI and
native Android recipes. Fix demonstrated failures in generator templates or CLI,
preserve existing stack choices and checks, and record observed UI evidence.

## Impact

Potential template/build configuration fixes, regression tests for actual defects,
updated bundled JAR and a runtime verification record. No production deployments,
physical-device signing or modification of unrelated user applications.
