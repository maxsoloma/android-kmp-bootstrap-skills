# Build the Kotlin/JVM CLI

Use the pinned Kotlin Toolchain wrapper. No global Kotlin CLI or Python is needed.

```sh
./kotlin do bundle
./kotlin test
java -jar build/bootstrap.jar catalog --json
```

`bundle` compiles the CLI, prepares its indexed resources and creates the standalone
executable at `build/bootstrap.jar` and identical standalone copies in
`skills/bootstrap-project/bin/bootstrap.jar` and
`skills/orchestrate-feature/bin/bootstrap.jar`. Feature workflow regressions cover
dependencies, ownership, persistent budgets, stale evidence and isolated JAR use.
The build plugin is Kotlin code in `build-tools/`; it reads explicit resource
inputs and preserves embedded binary bytes. JAR entries have deterministic order
and timestamps. Wrappers are release 0.13.0 with upstream distribution checksums.
First builds download Toolchain, compiler and Maven dependencies.

The repository and generated projects have no Gradle build files or wrapper.
Toolchain 0.13.0 itself delegates Android work to an internally provisioned Gradle
backend. That is an upstream implementation detail, not a second project backend
or an additional installation required from the user.

Run the real integration matrix on macOS with full Xcode, an iOS simulator runtime
and access to Maven/tool downloads. Choose a fresh absolute output directory:

```sh
scripts/verify-matrix.sh "$PWD/build/bootstrap.jar" /tmp/bootstrap-android android
scripts/verify-matrix.sh "$PWD/build/bootstrap.jar" /tmp/bootstrap-shared shared
scripts/verify-matrix.sh "$PWD/build/bootstrap.jar" /tmp/bootstrap-native native
scripts/verify-platform-subsets.sh "$PWD/build/bootstrap.jar" /tmp/bootstrap-subsets
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
