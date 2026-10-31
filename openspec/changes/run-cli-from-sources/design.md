# Design

## Context

- See proposal.md for motivation. Requirements are in the `toolchain-projects` and
  `codex-plugin-integration` deltas.
- The CLI is a single `jvm/app` module (`module.yaml`, `mainClass: bootstrap.MainKt`,
  `release: 17`, `jdk.version: 21`) plus the `build-tools` plugin. Its `resources`
  task copies `skills/bootstrap-project/assets`, `supporting/toolchain.json` and
  `src/main/resources` into generated resources and writes `resources.index`. That
  task runs on every compile, not only in `bundle`.
- Wrapper 0.13.0 (`kotlin`): it uses `KOTLIN_CLI_JAVA_HOME` if set, otherwise it
  downloads Zulu JRE 25 for the CLI process. It never inspects `JAVA_HOME` or `PATH`
  for that runtime. Measured: `KOTLIN_CLI_JAVA_HOME=<JDK 21>` fails with
  `Unrecognized VM option 'UseCompactObjectHeaders'`, so the CLI runtime minimum is 25.
- JDK for compiling/running the module follows `settings.jvm.jdk`. The default
  `selectionMode: auto` uses `JAVA_HOME` when it matches `version`, else provisions it
  (Kotlin Toolchain docs, JDK provisioning).
- Prototype: `kotlin --log-level=error run --project-dir=<repo> --working-dir=<cwd> --
  catalog --json` printed parseable JSON only, and `generate` produced a tree that
  `diff -r` reported identical to the shipped JAR's output. Warm run ≈1 s.
- `kotlin run` has no standalone-script mode usable here: it requires a project.

## Goals / Non-Goals

**Goals:**
- One portable launcher contract for both skills on macOS, Linux and Windows.
- Minimal downloads: reuse existing Java; download once per machine otherwise.

**Non-Goals:**
- Running without any JVM (the Toolchain CLI and Kotlin compiler are JVM programs).
- Offline first run on an empty cache.
- Changing generated applications, recipes, locks or the generated wrapper.
- Reintroducing Gradle. Gradle needs a pre-installed JVM for its launcher and a
  committed `gradle-wrapper.jar`, and the backend was retired in 3.0.

## Decisions

### D1. Each skill carries a full CLI project copy under `cli/`
`skills/<skill>/cli/` contains `project.yaml`, `module.yaml` (without test settings),
`src/main/kotlin`, `src/main/resources`, `assets/`, `toolchain.json`, `build-tools/`
and byte-identical `kotlin`/`kotlin.bat`. Its plugin config points at these local
paths. The repository root remains the single source of truth; `./kotlin do bundle`
regenerates both copies deterministically, and CI fails on drift.
- Alternative: one shared CLI copy referenced by both skills. Rejected because each
  skill must work when copied independently.
- Alternative: keep the JAR as a fallback. Rejected because it keeps the binary and
  the Java 17 prerequisite. The duplicated 68 KB of templates in `bootstrap-project`
  is acceptable.

### D2. Launcher scripts `bin/bootstrap` and `bin/bootstrap.cmd`
The launcher resolves its own directory and runs
`<skill>/cli/kotlin --log-level=error run --project-dir=<skill>/cli
--build-dir=<cache> --working-dir=<caller cwd> -- "$@"` with
`KOTLIN_CLI_NO_WELCOME_BANNER=1`, and returns the CLI's exit code.
- `<cache>` is version-keyed and per-user: `~/Library/Caches/bootstrap-projects/<ver>`
  on macOS, `${XDG_CACHE_HOME:-~/.cache}/bootstrap-projects/<ver>` on Linux, and
  `%LOCALAPPDATA%\bootstrap-projects\<ver>` on Windows. An installed plugin
  directory may be read-only and must stay unchanged.
- Any Toolchain stdout lines that are not CLI output are a defect. Task 1.2 checks
  this on an empty cache.

### D3. Java reuse probe
If `KOTLIN_CLI_JAVA_HOME` is set, the launcher leaves it unchanged. Otherwise it probes
`$JAVA_HOME/bin/java`, then `java` on `PATH`, by running
`-XshowSettings:properties -version` and reading `java.specification.version`.
- The first candidate that exits 0 with major version ≥ 25 becomes
  `KOTLIN_CLI_JAVA_HOME` (its `java.home`).
- A failing run (for example, the macOS `/usr/bin/java` stub) or a lower version is
  ignored, and the wrapper provisions its pinned JRE.
- The minimum is one launcher constant, tied to the pinned wrapper version.

### D4. Compilation JDK version 25 with `selectionMode: auto`
Bytecode stays `release: 17`. Changing `jdk.version` from 21 to 25 means a single
installed JDK 25 satisfies both the CLI runtime and compilation, so a user who already
has it gets zero Java downloads. With 21, every user would need two Java versions to
avoid downloads. A user with only JDK 21 now gets JDK 25 provisioned once; before, the
same user downloaded the JRE 25 anyway.
- Alternative: keep 21. It favors today's common install but never achieves zero
  downloads.
- Alternative: `selectionMode: javaHome`. Rejected because it fails on machines
  without a matching JDK.

### D5. Tests exercise the copied skill, not a JAR
Isolated-installation tests copy `skills/<skill>/` to a temp directory, make it
read-only and run `bin/bootstrap` with a temp build cache. They assert JSON
cleanliness, exit codes, generation equivalence against the in-repo build and Java
probe outcomes. The probe tests use fake `java` scripts that print a chosen
`java.specification.version` or fail, so they do not depend on installed JDKs.

## Risks / Trade-offs

- [First run needs network and takes noticeably longer] → The skill's first step
  runs `bin/bootstrap --version` and tells the user that one-time provisioning may
  download components. A cache warmed by any generated-project build makes it near
  instant.
- [Concurrent launches compile into the same build dir] → Version-keyed cache. Task
  2.3 tests two parallel invocations; if the Toolchain does not serialize them, the
  launcher adds a directory lock with a timeout.
- [Corporate proxies or mirrors] → The wrapper's `KOTLIN_CLI_DOWNLOAD_ROOT` and
  `KOTLIN_CLI_JRE_DOWNLOAD_ROOT` pass through and are documented in INSTALL.
- [Toolchain stdout noise in a future wrapper bump] → Task 1.2 becomes a regression
  test run on every bundle.
- [Larger skill payload in files, smaller in bytes] → Sources are ≈1 MB versus the
  2.9 MB JAR per skill.

## Migration Plan

Release as a minor version (3.4.0) with a CHANGELOG entry: "Java is no longer
required; first run provisions the Toolchain". Existing plugin caches update through
the normal plugin update. To roll back, reinstall the previous plugin version, which
still contains the JAR. No user projects are touched.

## Open Questions

- Whether `kotlin --version`-style warm-up should be a separate CLI subcommand
  (`bootstrap prepare`). This does not change specs or tasks; `--version` suffices
  initially.
