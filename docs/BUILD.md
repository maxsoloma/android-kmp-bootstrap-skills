# Build the Kotlin/JVM CLI

Use the pinned Kotlin Toolchain wrapper. No global Kotlin CLI or Python is needed.

```sh
./kotlin do bundle
./kotlin test
skills/bootstrap-project/bin/bootstrap catalog --json
```

The repository root (`src/main`, `build-tools/`, `skills/bootstrap-project/assets`
and `supporting/toolchain.json`) is the single source of truth. `bundle` copies it
deterministically into `skills/bootstrap-project/cli/` and
`skills/orchestrate-feature/cli/`, together with byte-identical `kotlin`/`kotlin.bat`
wrappers, and writes each skill's `bin/bootstrap` and `bin/bootstrap.cmd` launchers
from `build-tools/launchers/`. Commit the synced copies; CI fails when they drift.
No executable archive is committed.

A launcher runs `kotlin build` (output on stderr) and then `kotlin run` for its
`cli/` project, with build output in a per-user cache. In a skill copy, resources
live in `cli/resources/` and are copied by the plugin's `prepareSkillResources`
task, because the Toolchain copies resource directory permissions and would fail
in read-only installations. The build plugin preserves embedded binary bytes.
Wrappers are release 0.13.0 with upstream distribution checksums. First builds
download Toolchain, compiler and Maven dependencies.

`./kotlin test` copies both skills read-only to a temporary directory and runs their
launchers from foreign working directories: output cleanliness, exit codes,
generation equivalence with the in-repository build, doctor/verify, feature
journals and Java reuse (with fake `java` executables).

The repository and generated projects have no Gradle build files or wrapper.
Toolchain 0.13.0 itself delegates Android work to an internally provisioned Gradle
backend. That is an upstream implementation detail, not a second project backend
or an additional installation required from the user.

Run the real integration matrix on macOS with full Xcode, an iOS simulator runtime
and access to Maven/tool downloads. Choose a fresh absolute output directory:

```sh
scripts/verify-matrix.sh "$PWD/skills/bootstrap-project/bin/bootstrap" /tmp/bootstrap-android android
scripts/verify-matrix.sh "$PWD/skills/bootstrap-project/bin/bootstrap" /tmp/bootstrap-shared shared
scripts/verify-matrix.sh "$PWD/skills/bootstrap-project/bin/bootstrap" /tmp/bootstrap-native native
scripts/verify-platform-subsets.sh "$PWD/skills/bootstrap-project/bin/bootstrap" /tmp/bootstrap-subsets
```

Set `DEVELOPER_DIR` if the selected developer directory is Command Line Tools.
The scripts retain per-recipe status, source/config/lock, structured verification
and separate build/test logs. Each matrix group checks all sixteen combinations
of DI, applicable navigation, network and storage. Generation unit tests also
render every compatible platform subset. Device interaction and signing are
outside these build/unit/browser checks.

The verifier uses a host lock around iOS tests because Toolchain 0.13.0's simulator
lock does not coordinate separate CLI processes. The wait has a timeout. Run
direct Toolchain native test commands sequentially on the same simulator.
