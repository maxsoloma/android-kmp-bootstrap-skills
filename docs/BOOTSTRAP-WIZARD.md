# Chat and terminal wizards

## Chat wizard: the default skill entry point

Invoke `$bootstrap-projects:bootstrap-project` in Codex or
`/bootstrap-projects:bootstrap-project` in Claude Code without arguments. The
skill starts a step-by-step conversation. Only the project name and package are
free-text inputs; it proposes `./<ProjectName>` as the destination.

Choose platforms, compatible UI, DI, navigation, networking and local storage
from the menus, then review and choose Create project, Change choices or Cancel.
Platforms support any nonempty subset of Android, iOS, desktop and web. The UI
menu offers only compatible choices. Each step waits for an answer; default
recommendations do not skip the component choices.

Native question tools display forms when available in the current agent/mode.
Multi-select forms collect platforms together; single-select forms offer an
Include/Exclude choice per platform. Otherwise, the agent displays numbered chat
menus, accepting multiple numbers for platforms and one for other choices.
The agent follows your language and handles the CLI/config behind the menus.

Review displays the derived SDK settings, package/application ID, destination,
selected components and modules/file plan. Native SwiftUI navigation sets an
unspecified iOS minimum to 16.0. Ktor uses the demo HTTPS endpoint unless supplied
explicitly. Advanced settings can be requested, but are not mandatory inputs.

Change choices lets you revisit a step without resetting unrelated answers.
Conflicting dependent choices are explained and corrected through menus. Cancel
creates no project. Existing output is never overwritten; an occupied proposed
directory offers a new destination choice.

Choices already supplied in the invocation are preserved, and the wizard skips
those answered steps. Explicit default/config/replay requests can bypass menus.
See [the README example](../README.md#example-create-an-android-and-ios-app).

## Terminal wizard: an optional alternative

The standalone terminal wizard uses the same generator and compatibility rules.
Its terminal prompts include advanced settings; its six-step flow below is
separate from the default in-chat menu flow.

### Start

Use the JAR inside your installed skill, as described in [installation](INSTALL.md):

```sh
bootstrap_skill_dir="/path/to/installed/skills/bootstrap-project"
java -jar "$bootstrap_skill_dir/bin/bootstrap.jar" init ./Aurora
```

Run this in a real terminal. `./Aurora` must be absent or empty. Omitting the
output argument starts with the current directory as the proposed destination;
you can change it at the first step.

### Choose a recipe

The wizard has six steps:

1. **Basics:** destination, PascalCase project name, package and application ID.
2. **Recipe:** choose the starting UI and platform setup.
3. **Targets:** select platforms by IDs or comma-separated menu numbers.
4. **SDK:** Android minimum/target/compile levels and iOS minimum version.
5. **Components:** UI, DI, navigation, networking, endpoint and local storage.
6. **Agents:** additional instruction files; `AGENTS.md` is always generated.

| Recipe | Starting targets | UI |
|---|---|---|
| Native Android | Android | Android Compose |
| Mobile / Shared Compose UI | Android + iOS | Shared Compose |
| Universal Compose | Android + iOS + desktop + web | Shared Compose |
| Native Mobile UI | Android + iOS | SwiftUI on iOS; Compose on Android |

Press Enter to accept a shown value. `back` returns to the preceding step;
`cancel`, `quit` or EOF exits without creating a project or saving a config.
The component menus explain incompatible options, including integrations that
are not implemented. Resolve those conflicts before creating or saving.

### Review and edit

Review summarizes modules, dependency pins, platform adapters, requirements and
verification commands. Build checks are still `NOT_RUN` at this stage.

| Review action | Effect |
|---|---|
| `details` | Print the full JSON plan, file list, resolved config and lock. |
| `edit basics` | Change destination, name, package or application ID. |
| `edit recipe`, `edit targets`, `edit sdk`, `edit agents` | Revisit the corresponding step. |
| `edit components` | Revisit all UI and component choices. |
| `edit ui`, `edit di`, `edit navigation`, `edit network`, `edit storage`, `edit endpoint` | Change one component choice. |
| `create` | Revalidate the destination and publish the project. |
| `save` | Save the resolved JSON config outside the output directory, then exit. |
| `cancel` | Exit without creating the project or saving a config. |

Edits preserve other answers. Changing a recipe may leave a previously chosen
UI or platform combination incompatible; review reports what needs correction.
For SwiftUI with `platform-native` navigation, use `edit sdk` to set the iOS
minimum to at least `16.0`.

When disabling Ktor after entering an endpoint, also use `edit endpoint` and
enter `clear` to remove the retained URL. With Ktor enabled, clearing the explicit
URL restores its default demo endpoint. Network requests run only on the sample
app's Fetch action, and browser endpoints must permit CORS.

### Save for later

At review, enter `save` and a config path outside the destination. Its parent
must already exist and the file must not exist. The wizard saves only JSON;
it does not create the project or save a separate lock file.

For example, save `./aurora-recipe.json` and later run:

```sh
java -jar "$bootstrap_skill_dir/bin/bootstrap.jar" plan --config ./aurora-recipe.json --output ./Aurora
java -jar "$bootstrap_skill_dir/bin/bootstrap.jar" generate --config ./aurora-recipe.json --output ./Aurora
```

Generation includes the resolved spec and lock in the new project. See
[configuration and replay](CLI-CONFIG.md) for preserving and reusing them.

### Without a terminal

`init` returns a structured `interactive_required` error outside a TTY. Agents
and CI use `catalog --json`, a JSON config, `plan` and `generate`. The same
resolver validates both flows. Review and generation do not install tools or
run application builds; build/test results must be checked separately.
