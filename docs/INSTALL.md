# Install and start

Install through Codex or Claude Code's built-in plugin manager. You need Git
and a Java 17+ JDK to run the generator. The plugin contains two portable skills, each with its own compiled
`bin/bootstrap.jar`; bootstrap-project also includes generation assets.

## Install the plugin

### Codex

Run in a terminal, from any directory:

```sh
codex plugin marketplace add maxsoloma/android-kmp-bootstrap-skills
codex plugin add bootstrap-projects@android-kmp-bootstrap-skills
```

Start a new conversation and select **Android & KMP Bootstrap**, or send:

```text
$bootstrap-projects:bootstrap-project
```

These native commands were exercised with `codex-cli 0.163.0`. To inspect the
installation and its `installedPath`:

```sh
codex plugin list --marketplace android-kmp-bootstrap-skills --json
```

This checkout targets plugin version `3.3.0`; verify the installed release version
before expecting its new skill. If the skill is
missing, open a new conversation so discovery reloads the installed plugin. For
local plugin development, replace the GitHub source with the checkout path in
the marketplace command. See [official plugin packaging documentation](https://developers.openai.com/plugins/build/plugins).

### Claude Code

Run in a terminal:

```sh
claude plugin marketplace add maxsoloma/android-kmp-bootstrap-skills
claude plugin install bootstrap-projects@android-kmp-bootstrap-skills
```

Or run the equivalent commands inside Claude Code:

```text
/plugin marketplace add maxsoloma/android-kmp-bootstrap-skills
/plugin install bootstrap-projects@android-kmp-bootstrap-skills
```

Start a new conversation and send:

```text
/bootstrap-projects:bootstrap-project
```

The [Claude Code skills documentation](https://code.claude.com/docs/en/skills)
explains the plugin prefix and automatic invocation.

## Upgrade an existing installation

Refresh only this marketplace, then install/update the target plugin:

```sh
# Codex
codex plugin marketplace upgrade android-kmp-bootstrap-skills
codex plugin add bootstrap-projects@android-kmp-bootstrap-skills
codex plugin list --marketplace android-kmp-bootstrap-skills --json

# Claude Code
claude plugin marketplace update android-kmp-bootstrap-skills
claude plugin update bootstrap-projects@android-kmp-bootstrap-skills
claude plugin list --json
```

Verify version `3.3.0` and the installed source/path. Start a new conversation so
the installed skill catalog reloads; restart the client if it retains the old
catalog. Both `bootstrap-project` and `orchestrate-feature` should be available.
For manual copies, replace the complete skill folder with the matching release;
keep the previous copy for any unfinished run that requires its CLI version.

## Feature implementation in an existing app

Open the existing app workspace. In Codex invoke
`$bootstrap-projects:orchestrate-feature` with the desired feature or bug fix;
in Claude Code use `/bootstrap-projects:orchestrate-feature`. Describe the
observable outcome; the agent handles planning, the journal and checks.
See [feature orchestration](FEATURE-ORCHESTRATION.md).

## Start in chat

Invoke the skill without arguments using the command above, or select
**Android & KMP Bootstrap** in Codex. The wizard begins in the conversation:

1. Type the project name and package.
2. Select platforms, then a compatible UI.
3. Select DI, navigation, networking and local storage from the offered options.
4. Review the file plan and choose Create project, Change choices or Cancel.

The agent uses its available native question forms, or numbered chat menus when
forms are unavailable. It waits for each answer; you do not need to write a
specification or find a JAR. Output defaults to `./<ProjectName>` in the workspace,
and the package is also used as the application ID. The SDKs and other advanced
fields are derived automatically and shown in review.

See [the README walkthrough](../README.md#example-create-an-android-and-ios-app)
and [wizard behavior](BOOTSTRAP-WIZARD.md) for the menu choices and editing flow.

## Troubleshooting installation and startup

| Symptom | What to check |
|---|---|
| Skill is missing after installation | Start a new conversation in your destination workspace. Check that `bootstrap-projects` is installed and enabled. |
| Questions appear as text instead of buttons | This agent/mode has no callable native question form. Choose the numbered chat options; the same wizard remains available. |
| Short skill name does not work | Plugin installations use `$bootstrap-projects:bootstrap-project` in Codex and `/bootstrap-projects:bootstrap-project` in Claude Code. Manual copies use the short name. |
| Codex does not recognize `plugin` | Use a Codex version with plugin support, or install the portable skill manually below. The recorded plugin flow used codex-cli 0.163.0. |
| `java` is missing or the JAR reports an unsupported class version | Run `java -version`; expose a Java 17+ JDK on your terminal/agent's PATH. An IDE-bundled JDK can be used if its `bin` directory is on PATH. |
| Destination is not empty | Choose a new directory or an existing empty one. The skill does not add its scaffold to an existing app. |
| `init` reports `interactive_required` | Run the wizard in a real terminal, or use the chat/JSON configuration flow. |

Inspect the installed plugins from a terminal:

```sh
# Codex
codex plugin list --marketplace android-kmp-bootstrap-skills --json

# Claude Code
claude plugin list --json
```

## Terminal wizard and CI

Agent users can skip this section. To run the terminal wizard yourself, use
`skills/bootstrap-project` under the installed plugin directory. In Codex,
`installedPath` from the plugin list identifies that directory. Set the skill
path once, then run:

```sh
bootstrap_skill_dir="/path/to/installed/skills/bootstrap-project"
java -jar "$bootstrap_skill_dir/bin/bootstrap.jar" init /path/to/NewApp
```

`init` needs a real terminal. Review supports `details`, `edit <choice>`, `create`,
`save` and `cancel`. `save` writes only a config outside the destination.
Agents and CI without a TTY use the reproducible config flow:

```sh
java -jar "$bootstrap_skill_dir/bin/bootstrap.jar" catalog --json
java -jar "$bootstrap_skill_dir/bin/bootstrap.jar" plan --config /path/to/recipe.json --output /path/to/NewApp
java -jar "$bootstrap_skill_dir/bin/bootstrap.jar" generate --config /path/to/recipe.json --output /path/to/NewApp
java -jar "$bootstrap_skill_dir/bin/bootstrap.jar" verify /path/to/NewApp
```

See [configuration](CLI-CONFIG.md) for JSON examples and
[the wizard guide](BOOTSTRAP-WIZARD.md) for editing choices. Generation refuses a
populated destination; existing projects are never overwritten. Non-TTY `init`
returns structured `interactive_required` feedback.

## Manual portable installation

From a downloaded repository, copy the complete `skills/bootstrap-project`
directory into your agent's skills directory: `~/.agents/skills/` for Codex or
`~/.claude/skills/` for Claude Code. With this method, invoke `$bootstrap-project`
in Codex or `/bootstrap-project` in Claude Code. Use either the plugin or the
manual copy to avoid duplicate skill entries. Keep all bundled files intact.
For feature implementation, independently copy the complete
`skills/orchestrate-feature` folder and invoke `$orchestrate-feature` or
`/orchestrate-feature`. It includes its own JAR and references; the bootstrap skill
is not a prerequisite.

Each skill is self-contained: Java 17+ runs the JAR without repository sources or
sibling skills. Generated instruction files for other agents are selected
independently of which agent runs the skill.

## Build requirements

The generator can create files offline after plugin installation. The first
application build needs network access for Toolchain, dependencies and SDK tools.
Generated apps include pinned `kotlin` and `kotlin.bat` wrappers.

| Target | Additional requirements |
|---|---|
| Android | SDK tooling is provisioned by Toolchain; running the app needs a connected device or running emulator. |
| iOS | macOS, full Xcode, first-launch setup and an installed simulator runtime; generated simulator/device targets are ARM64. |
| Desktop JVM | Toolchain provisions its build JDK; running the Compose app needs a graphical desktop session. |
| Browser Wasm | A browser with WasmGC support; Toolchain provisions its browser test runner. |

Complete Xcode license acceptance and first-launch setup before iOS builds,
and configure a signing team for physical devices. When Command Line Tools is
active, set `DEVELOPER_DIR` to your Xcode developer folder for the build command:

```sh
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer \
  java -jar "$bootstrap_skill_dir/bin/bootstrap.jar" verify /path/to/NewApp
```

Version 3 uses Kotlin Toolchain for generation and feature implementation. Hardened skills and
the Gradle backend were removed. Use an existing application's corresponding
old release to replay old locks or build instructions.
