# Verification

Use `<skill>/bin/bootstrap doctor --targets TARGETS` for environment
inspection. Toolchain can provision build tools on first use; iOS requires full
Xcode and a runtime for simulator tests. Optional Android CLI is independent.

Run `<skill>/bin/bootstrap verify PROJECT` or the plan's commands.
Each selected platform has separate Toolchain build and test commands. Logs and
structured results are written under `.bootstrap/`. Failures do not prevent the
other checks from running. Return codes: 0 passed; 1 failed; 2 includes skips.
SwiftUI also gets shared JVM logic checks without Xcode.

`verify` serializes iOS simulator test processes on the same host and bounds the
lock wait by the command timeout. Toolchain 0.13.0's own simulator lock is local
to one JVM; concurrent direct `./kotlin test --platform iosSimulatorArm64` calls
can race during shutdown. Use `verify` for parallel agent checks, or run direct
native test commands sequentially.

A successful build is not a UI test. Browser unit tests exercise mock transport
and actual origin-local localStorage, native storage tests reopen NSUserDefaults,
and JVM tests reopen temporary files. These do not prove physical-device signing,
app-store publication, CORS/service availability or all privacy/quota policies.
Never claim missing SDK/network/Xcode/browser checks passed.
