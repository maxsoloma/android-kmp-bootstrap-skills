# Chat wizard

Start this workflow when the skill is invoked for interactive project creation.
The same workflow works in Codex, Claude Code and a portable skill installation.
Run `java -jar <skill>/bin/bootstrap.jar catalog --json` first; use its supported
choices and defaults as the authority. Handle CLI paths and JSON for the user.

## Present questions

- Use a native user-question tool if it is available and allowed in the current
  mode: for example Claude's `AskUserQuestion`, or Codex's `request_user_input`
  / `request_user_input_async`. Follow that tool's actual schema and limits.
  Do not use onboarding-only tools or switch modes just to display the wizard.
- Ask only the current step, in the user's language, with short option labels
  and a sentence explaining each option. Put the recommended option first.
  Paginate long edit menus when needed to stay within the tool's option limits.
- For platforms, use multi-select when the tool supports it. With single-select
  tools, ask Include/Exclude for each platform instead. Respect per-call limits;
  retain the entire nonempty selection rather than forcing a platform preset.
- Without a callable question tool, show a numbered menu and wait for the reply.
  Platform menus accept multiple numbers, such as `1,2,4`; other menus accept one.
  Keep the same choices and step order. Do not pretend plain text is a native form.
- An asynchronous question is still pending until the user answers. Do not
  proceed with a preselected option or launch a second step while awaiting it.
  A skipped/unanswered question is not consent to use its recommendation.
- Reuse answers supplied in the invocation. Skip a menu only if its choice was
  supplied or the user explicitly asked for defaults. A bare invocation must
  show the component menus, including None options.

## Steps and config mapping

1. **Name and package.** Ask for these two free-text values together if missing:
   a PascalCase name such as `Aurora`, and a dotted lowercase package such as
   `com.example.aurora`. Reuse supplied values. Validate with the CLI and re-ask
   only invalid fields. Use free-text form fields when supported, or ask in chat;
   do not invent name/package choices for a multiple-choice-only tool.
   Use the package as `app_id`.
   Default output to `./<name>` relative to the calling workspace, never the
   installed skill directory. Honor an explicitly supplied path. If the default
   is populated or a symlink, offer a suggested unused sibling directory or
   Cancel; do not require the user to type a path or overwrite existing files.
2. **Platforms.** Offer Android, iOS, Desktop JVM and Browser Wasm, mapping to
   `targets`: `android`, `ios`, `desktop`, `web`. Require at least one selection.
   Recommend Android+iOS without treating it as an answer. Do not hide iOS based
   on the host: explain that iOS builds require macOS/Xcode.
3. **UI.** Offer only compatible options:

   | Selected platforms | Menu option | Config |
   |---|---|---|
   | Android alone | Native Android Compose | `kind: android`, `ui: android-compose` |
   | Any nonempty platform subset | Shared Compose UI | `kind: kmp`, `ui: shared-compose` |
   | iOS alone or Android+iOS | SwiftUI on iOS; Compose on Android when selected | `kind: kmp`, `ui: swiftui` |

   Recommend native Compose for Android alone and shared Compose otherwise.
   If shared Compose is the sole valid option (desktop/web selections), show why
   it applies and continue only when there is no earlier conflicting UI answer.
   After a platform edit, ask the user to replace an incompatible explicit UI
   choice or revisit platforms; do not silently switch it to the sole option.
4. **Dependency injection.** Manual wiring (`di: manual`, recommended) or Koin
   (`di: koin`). Explain that manual wiring needs no DI framework.
5. **Navigation.** None (`navigation: none`, recommended), or Navigation 3
   (`navigation3`) for either Compose UI. For SwiftUI, offer None or Native
   navigation (`platform-native`: SwiftUI NavigationStack plus Android
   Navigation 3 when Android is selected). For native navigation set the iOS
   minimum to at least the catalog's `ios_minimum` (currently `16.0`) and show
   this derived setting in review. If the user explicitly requested a lower
   minimum, offer Raise minimum / No navigation / Back instead of overriding it.
6. **Networking.** None (`network: none`, recommended) or Ktor (`network: ktor`).
   With Ktor, omit `network_url` in the draft config unless the user supplied it;
   read the generator's resolved endpoint from `plan.spec.network_url` and show
   it in review without requiring URL input. Disabling Ktor also removes a
   default/generated `network_url`. For an explicitly supplied URL,
   explain the conflict and offer Keep Ktor / Remove URL rather than silently
   dropping the user's endpoint.
7. **Local storage.** None (`storage: none`, recommended) or Settings key-value
   storage (`storage: settings`). Explain that Settings stores small non-secret
   values and is not a SQL database.
8. **Review.** Resolve the complete config with `plan` and show selected platforms,
   UI, components, output, package/application ID, derived SDKs/iOS minimum,
   endpoint when selected, modules/file plan and requirements relevant to selected
   platforms (Xcode is an iOS requirement). Offer **Create project / Change choices
   / Cancel**. Change choices opens a menu of
   previous steps; preserve other answers and revalidate dependent choices.
   Create project runs `generate` with the reviewed config and destination.
   Cancel ends the wizard without creating a project; discard temporary config
   files created by this run. Never run generation with an unanswered choice.

For fields outside these menus, use catalog defaults (SDK versions and additional
agent instructions) unless supplied explicitly. Do not ask for application ID,
SDK numbers, instruction adapters or endpoint text during the normal wizard.
Advanced requests can override these fields, subject to the same CLI validation.

After an earlier choice changes, explain any affected choices and offer compatible
replacements. Do not silently convert an explicitly chosen UI or remove selected
platforms. If the user cancels or says Back, retain or discard answers as requested;
never interpret either as permission to create the app.

## Example numbered fallback

After collecting the name and package, a platform step can look like:

```text
Step 2/8 — Platforms (choose one or more)
1. Android
2. iOS
3. Desktop JVM
4. Browser Wasm

Recommended: Android + iOS. Reply with option numbers, for example 1,2.
```

Continue with the compatible UI menu after the answer. Do not send every menu as
one long questionnaire. Use native forms instead of this fallback when available.
