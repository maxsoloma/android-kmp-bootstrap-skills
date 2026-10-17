# Terminal wizard

For a conversation in Codex or Claude Code, invoke the installed skill and
describe your app; the agent uses the configuration flow. The terminal wizard
is an alternative for choosing the same settings yourself.

## Start

Use the JAR inside your installed skill, as described in [installation](INSTALL.md):

```sh
bootstrap_skill_dir="/path/to/installed/skills/bootstrap-project"
java -jar "$bootstrap_skill_dir/bin/bootstrap.jar" init ./Aurora
```

Run this in a real terminal. `./Aurora` must be absent or empty. Omitting the
output argument starts with the current directory as the proposed destination;
you can change it at the first step.

## Choose a recipe

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

## Review and edit

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

## Save for later

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

## Without a terminal

`init` returns a structured `interactive_required` error outside a TTY. Agents
and CI use `catalog --json`, a JSON config, `plan` and `generate`. The same
resolver validates both flows. Review and generation do not install tools or
run application builds; build/test results must be checked separately.
