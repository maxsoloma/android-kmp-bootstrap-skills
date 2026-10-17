# Install

The Claude Code and Codex plugin manifests expose the portable
`skills/bootstrap-project` skill. No sibling skill or repository runtime is
required after installation.

## Codex plugin

From a local checkout, register its marketplace and install the plugin:

```sh
cd /path/to/android-kmp-bootstrap-skills
codex plugin marketplace add .
codex plugin add bootstrap-projects@android-kmp-bootstrap-skills --json
codex plugin list --marketplace android-kmp-bootstrap-skills --json
```

These commands were exercised with `codex-cli 0.163.0`. The install response's
`installedPath` identifies the cached plugin directory. The list response should
show `installed: true`, `enabled: true` and version `3.0.0`.
Marketplace layout and cached local installs are described in the
[official plugin packaging documentation](https://developers.openai.com/plugins/build/plugins).

Start a fresh Codex conversation in your destination workspace. Invoke the skill
as `$bootstrap-projects:bootstrap-project`, or select **Android & KMP Bootstrap**
from the skills picker. For example:

> Use $bootstrap-projects:bootstrap-project to create Aurora in a new Aurora
> directory: Android and iOS, shared Compose, Koin, Navigation 3, Ktor and Settings.

Codex collects missing choices in chat and runs catalog → config → plan → generate
using the installed skill's JAR. The current conversation's skill inventory may
predate installation; verify discovery in a fresh conversation. The plugin does
not add a separate native Codex form or a slash command named `/init`: its terminal
wizard and its agent conversation use the same Kotlin CLI.

## Terminal wizard from the installed plugin

Use the `skills/bootstrap-project` folder under the returned `installedPath`.
For the tested installation:

```sh
bootstrap_skill_dir="$HOME/.codex/plugins/cache/android-kmp-bootstrap-skills/bootstrap-projects/3.0.0/skills/bootstrap-project"
java -jar "$bootstrap_skill_dir/bin/bootstrap.jar" init /path/to/NewApp
```

Run init in a real terminal. Review supports `details`, `edit <choice>`, `create`,
`save` and `cancel`. `save` writes the config outside the destination and exits;
it does not create the project. Replay that config when ready:

```sh
java -jar "$bootstrap_skill_dir/bin/bootstrap.jar" plan --config /path/to/recipe.json --output /path/to/NewApp
java -jar "$bootstrap_skill_dir/bin/bootstrap.jar" generate --config /path/to/recipe.json --output /path/to/NewApp
java -jar "$bootstrap_skill_dir/bin/bootstrap.jar" verify /path/to/NewApp
```

Generation refuses a populated destination. Agents/CI without a terminal use the
config flow; `init` returns structured `interactive_required` feedback there.

## Portable skill

Copy `skills/bootstrap-project` into your agent's skills directory when installing
the skill independently. Keep all of its bundled files intact.

The skill includes `bin/bootstrap.jar`; run it with Java 17+. Keep the JAR intact.
Claude, Codex, Gemini, Copilot and Cursor can follow the same catalog/config/plan
flow; additional generated instruction files are selected independently.

```sh
java -jar /path/to/bootstrap-project/bin/bootstrap.jar catalog --json
java -jar /path/to/bootstrap-project/bin/bootstrap.jar init /path/to/NewApp
```

A global Kotlin CLI is not needed: generated apps include pinned `kotlin` and
`kotlin.bat` wrappers. First app builds provision Toolchain/dependencies/SDK tools.
iOS needs macOS, full Xcode, accepted license and first-launch components; native
tests need an installed simulator runtime. Configure a signing team for devices.
When Command Line Tools is active, set `DEVELOPER_DIR` to your Xcode developer
folder for the build command instead of assuming Xcode is absent.

```sh
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer \
  java -jar "$bootstrap_skill_dir/bin/bootstrap.jar" verify /path/to/NewApp
```

Version 3 includes only the modern Kotlin Toolchain skill. The two hardened skills
and Gradle backend were removed. Existing generated applications stay unchanged;
use their corresponding old release to replay old locks or build instructions.
