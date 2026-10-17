# Networking and local storage

ProjectSpec v2 and flat generate flags accept independent data choices:

| Field | Choices | Default |
|---|---|---|
| `network` / `--network` | `none`, `ktor` | `none` |
| `storage` / `--storage` | `none`, `settings` | `none` |
| `network_url` / `--network-url` | Absolute HTTPS URL, no userinfo or fragment; Ktor only | Demo `https://httpbin.org/uuid` when Ktor is selected |

All current modern platform subsets and manual/Koin, Compose/SwiftUI and applicable
navigation choices are supported. The shared modern Xcode template uses the Kotlin Toolchain integration described in the toolchain reference.

| Target | Ktor engine | Settings adapter |
|---|---|---|
| Android | CIO | SharedPreferences, initialized with application context |
| iOS | Darwin | NSUserDefaults in an app-specific suite |
| Desktop JVM | CIO | PropertiesSettings with an app-specific persisted file |
| Browser Wasm | Js / fetch | StorageSettings / origin-local localStorage |

Pins: Ktor 3.6.0, Settings 1.3.0, coroutines 1.11.0. Confirmed published Wasm
variants are used; the generator does not infer support just from a library name.

Settings stores a small non-secret counter, not SQL data or credentials. Modern
Room, DataStore and Retrofit recipes are not implemented. CORS, server availability, localStorage quota/origin
and denied filesystem/platform access are runtime constraints. URL validation
does not establish endpoint availability or browser CORS permission.

The HTTP action is explicit; generation, startup and tests do not call a public
service. Replace the demo URL for your product. Tests use a mock transport, test
stores and temporary files; they do not require public API uptime.

```json
{"name":"Aurora","package":"com.example.aurora","targets":["android","ios","desktop","web"],"di":"koin","navigation":"navigation3","network":"ktor","storage":"settings"}
```

Save outside the destination, run `plan --config recipe.json --output /tmp/Aurora`,
then `generate --config recipe.json --output /tmp/Aurora`. Review selected adapters
and pins; spec/lock replay captures the same data choices. In terminal init use
`edit network`, `edit storage` or `edit endpoint`; `clear` at the endpoint prompt
removes an explicit URL. Existing answers are retained until corrected.

Networking uses a ten-second request timeout and an owned coroutine/client
lifetime. Storage errors are displayed and failed writes do not increment the
reported counter. JVM files use atomic replacement; unsupported atomic moves
are reported as failures. iOS defaults are persisted according to the platform's
normal write lifecycle; no crash-durability or multi-process transaction guarantee
is implied. This sample has one state owner above navigation.

Run browser tests with `./kotlin test --platform wasmJs`; Kotlin Toolchain provisions
Playwright and Chromium. The tests use MockEngine and, when
Settings is selected, a real localStorage adapter with a namespaced key restored
after the test. This verifies storage in that test origin, not all browser privacy
or quota configurations. `verify` builds the production web bundle and executes
the browser tests, retaining separate build and test logs.

For iOS run `./kotlin test --platform iosSimulatorArm64` with full Xcode and an
installed iOS simulator runtime. Settings recipes include a real NSUserDefaults
reopen test which restores the app-specific test key afterwards. Native tests
also exercise mock networking and owner cancellation; they do not call the demo
endpoint. `./kotlin build --platform iosSimulatorArm64` also builds the Xcode host.
See [toolchain integration](../skills/bootstrap-project/references/toolchain.md)
for Xcode setup and wrapper details.

Sources reviewed 2026-10-17: [Ktor client docs](https://ktor.io/docs/client-engines.html),
[Settings 1.3.0](https://github.com/russhwolf/multiplatform-settings/tree/v1.3.0).
