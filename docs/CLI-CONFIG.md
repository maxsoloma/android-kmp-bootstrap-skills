# Configuration and CLI

Agent users normally invoke the skill and choose platforms, UI and components
from its chat menus; the skill writes the configuration and runs these commands. This guide covers direct terminal use, CI and replay.
Use the standalone JAR bundled in the installed skill with Java 17+.

## Locate the CLI

Set the path to your installed `skills/bootstrap-project` directory. In Codex,
`codex plugin list --marketplace android-kmp-bootstrap-skills --json` reports the
plugin's `installedPath`; append `skills/bootstrap-project` to that path.
For a repository checkout, the skill is at `skills/bootstrap-project`.

```sh
bootstrap_skill_dir="/path/to/installed/skills/bootstrap-project"
java -jar "$bootstrap_skill_dir/bin/bootstrap.jar" --help
java -jar "$bootstrap_skill_dir/bin/bootstrap.jar" catalog --json
```

The examples below use a POSIX shell. In PowerShell, set
`$bootstrap_skill_dir = 'C:\path\to\skills\bootstrap-project'` and use the same
`java -jar "$bootstrap_skill_dir/bin/bootstrap.jar" ...` commands on single lines.

## Minimal recipe

Save this as `recipe.json` **outside** the destination project directory:

```json
{
  "name": "Aurora",
  "package": "com.example.aurora",
  "targets": ["android", "ios"]
}
```

Only `name` and `package` are required. This example uses shared Compose,
manual DI and no optional navigation, networking or storage.

Preview the files, then generate into a new or empty `./Aurora` directory:

```sh
java -jar "$bootstrap_skill_dir/bin/bootstrap.jar" plan --config recipe.json --output ./Aurora
java -jar "$bootstrap_skill_dir/bin/bootstrap.jar" generate --config recipe.json --output ./Aurora
```

`plan` validates and renders without creating the destination. Its JSON includes
`valid`, `issues`, resolved `spec`, `stack`, `modules`, `files`, `requirements`
and `verification`. Invalid input returns issues and exit code 1 before writes.

## Full recipe

```json
{
  "schema_version": 2,
  "build_system": "kotlin-toolchain",
  "name": "Everywhere",
  "package": "com.example.everywhere",
  "app_id": "com.example.everywhere",
  "kind": "kmp",
  "targets": ["android", "ios", "desktop", "web"],
  "ui": "shared-compose",
  "di": "koin",
  "navigation": "navigation3",
  "network": "ktor",
  "storage": "settings",
  "agents": ["codex", "claude"],
  "sdk": {"min": 26, "target": 37, "compile": 37},
  "ios_deployment_target": "15.0"
}
```

Ktor uses the bundled demo HTTPS endpoint when `network_url` is omitted. Set
`network_url` to your product endpoint if needed; remove the field when changing
`network` to `none`.

## Fields and defaults

| Field | Values / constraint | Default |
|---|---|---|
| `name` | PascalCase identifier, e.g. `Aurora` | Required |
| `package` | At least two dotted lowercase non-keyword segments | Required |
| `app_id` | Android application / iOS bundle ID; no underscores for iOS | `package` |
| `schema_version` | `2`; modern schema `1` inputs are normalized to `2` | `2` |
| `build_system` | `kotlin-toolchain` | `kotlin-toolchain` |
| `kind` | `kmp` or `android` | `kmp` |
| `targets` | Nonempty array of `android`, `ios`, `desktop`, `web` | `android,ios`; `android` for kind `android` |
| `ui` | `shared-compose`, `android-compose`, `swiftui` | `shared-compose`; `android-compose` for kind `android` |
| `di` | `manual`, `koin` | `manual` |
| `navigation` | `none`, `navigation3`, `platform-native` | `none` |
| `network` | `none`, `ktor` | `none` |
| `storage` | `none`, `settings` | `none` |
| `network_url` | Ktor-only absolute HTTPS URL with host, valid port, no credentials or fragment | `https://httpbin.org/uuid` with Ktor |
| `agents` | Nonempty array of `codex`, `claude`, `gemini`, `copilot`, `cursor`, or `none` alone | `codex,claude` |
| `sdk` | Object with integer `min`, `target`, `compile`; `23 <= min <= target <= compile <= 37`; Android requires compile 37 | `26/37/37` |
| `ios_deployment_target` | Version string, at least `15.0`; NavigationStack requires `16.0`+ | `15.0` |

Kind `android` supports only `android-compose` on Android. Shared Compose
supports any nonempty platform subset. SwiftUI requires iOS, optionally Android,
and supports `none` or `platform-native` navigation. See [stack compatibility](STACKS.md).

`AGENTS.md` is always generated. The `agents` field controls additional adapter
files; it does not select the agent running the generator.
Unknown fields, wrong types, incompatible components and retired Gradle/hardened
options fail before project publication.

## Other commands

```sh
# Interactive wizard: use a real terminal
java -jar "$bootstrap_skill_dir/bin/bootstrap.jar" init ./NewApp

# Inspect local tools without installing them
java -jar "$bootstrap_skill_dir/bin/bootstrap.jar" doctor --targets android,ios

# Build and test only the selected generated targets
java -jar "$bootstrap_skill_dir/bin/bootstrap.jar" verify ./Aurora --targets android --timeout 1200
```

`verify` reads the generated `bootstrap.json`, uses the project's pinned wrapper
and saves `.bootstrap/verification.json` plus per-target build/test logs.
`--timeout` is a positive number of seconds per command. SwiftUI verification
also builds and tests shared Kotlin logic on JVM.

| Exit code | Meaning |
|---|---|
| `0` | Command succeeded; for `verify`, all executed checks passed with none skipped |
| `1` | Invalid input or command/check failure |
| `2` | `verify` completed with skipped checks and no failed checks |

`doctor` is a read-only environment probe, not a build guarantee. An unavailable
optional tool can be reported as skipped without failing the doctor command.
Non-interactive `init` returns `interactive_required`; CI should use JSON recipes.
Generation does not install tools, initialize Git, call a public API or build apps.

## Reproduce a generated project

Generated apps include `bootstrap.spec.json` and `bootstrap.lock.json`. Point the
same generator version at the original spec and a **different empty destination**:

```sh
java -jar "$bootstrap_skill_dir/bin/bootstrap.jar" plan --config ./Aurora/bootstrap.spec.json --output ./AuroraCopy
java -jar "$bootstrap_skill_dir/bin/bootstrap.jar" generate --config ./Aurora/bootstrap.spec.json --output ./AuroraCopy
```

The spec's sibling `bootstrap.lock.json` is loaded automatically. The lock
captures the resolved config, bundled recipes and generated file hashes, not
output paths or later edits to your app. You can supply a lock explicitly with
`--lock /path/to/bootstrap.lock.json`.

A changed config, generator or bundle fails with `lock_mismatch`. Use the matching
generator/spec to reproduce the recipe. To deliberately create a different
recipe, save a new config separately from the old lock and generate to a new
destination. Replay does not update an existing application.
