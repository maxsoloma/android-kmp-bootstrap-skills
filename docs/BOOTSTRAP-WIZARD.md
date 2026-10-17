# Terminal and agent wizard

Run `java -jar skills/bootstrap-project/bin/bootstrap.jar init OUTPUT` in a TTY.
The six steps are basics, recipe, platforms, SDKs, components and agent adapters.
Recipes: native Android, mobile shared Compose, universal Compose, native mobile
UI (SwiftUI with optional Android). All use Kotlin Toolchain.

Enter accepts shown values; menus accept IDs or comma-separated numbers. `back`
returns to the previous step; `cancel`, `quit` and EOF exit without writes.
Components expose compatibility feedback for UI, DI, navigation, network and local
storage. Invalid choices remain visible until corrected. SwiftUI NavigationStack
requires an explicit iOS minimum of 16.0+.

Review shows modules, pins, data adapters, verification commands and a full file
plan. `details` prints structured config/lock/plan. `edit basics|recipe|targets|sdk|
components|ui|di|navigation|network|storage|endpoint|agents` changes selected answers
without resetting the rest. Changing recipe preserves explicit choices, so UI or
platform conflicts must be corrected. `clear` in endpoint editing removes its
explicit value. A disabled network with a retained endpoint remains a conflict.

Only `create` publishes a project. The destination is revalidated at publication
so files added during review remain safe. `save` writes only JSON outside the
project, using an existing parent and never replacing a file or symlink.

Without a TTY `init` returns `interactive_required`. Agents use catalog → collect
JSON → plan → generate. The same resolver checks both flows, and the same lock
replays the output. Review and generation do not install tools or run builds.
