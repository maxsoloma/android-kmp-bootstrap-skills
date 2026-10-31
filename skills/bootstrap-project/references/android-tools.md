# Android tooling for agents

The current **Android CLI** (`android`) is distinct from the retired SDK `android`
tool. Inspect `android --help` for `skills`, `studio`, `describe`, `info` before
assuming the executable is the modern agent CLI. `bin/bootstrap doctor` performs
this read-only detection. SDK command-line tools (`sdkmanager`, `avdmanager`,
`adb`, `emulator`) remain useful fallbacks.

## Use installed capabilities

1. `android info` reports the environment. Use the project's wrapper to build.
2. `android describe` locates project build metadata and artifacts. Inspect help
   for the installed CLI before supplying version-sensitive flags.
3. `android run --help` / `android install --help` guide deployment of the actual
   built APK. Select the intended device when several are connected.
4. `android layout`, `android screen --help` and `android input --help` support
   actual UI inspection. Preserve device changes within the requested test scope.
5. `android studio --help` exposes supported IDE capabilities: code analysis,
   declaration/usages lookup, Compose previews and library version lookup.
6. `android docs --help` searches/fetches the official Android Knowledge Base.
   Context7 and an already connected Google Developer Knowledge MCP are other
   documentation sources. Do not invent a dependency on a particular MCP name.

For initial CLI setup requested by the user, follow the official download page
for their OS, inspect the installer, and run `android init` to install its CLI
skill. `android skills --help` exposes additional official skills. This plugin
does not bundle, replace, or auto-install Google's Android skills. Avoid two
generators targeting one directory: use `android create` only for a separately
chosen Android CLI template, never on top of an already generated project.

Without Android CLI, use Android Studio SDK Manager or `sdkmanager` to provision
the exact SDK requested, then `./kotlin build --platform android`,
`adb devices -l`, and the appropriate install/run workflow. Do not auto-accept
licenses or create billable remote-device reservations during diagnostics.

Sources (reviewed 2026-10-17):
- [Android CLI overview](https://developer.android.com/tools/agents/android-cli)
- [Android CLI command reference](https://developer.android.com/tools/agents/android-cli/commands)
- [Official Android skills](https://github.com/android/skills)
- [Android CLI downloads](https://developer.android.com/tools/agents/android-cli/download)
