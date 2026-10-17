# Data choices

Discover current `stack_choices.network` and `stack_choices.storage` via the
standalone JAR's catalog. Modern supports none/Ktor and none/Settings independently
on every existing target, with manual/Koin and applicable UI/navigation recipes.
Defaults are none. All recipes use Kotlin Toolchain.

```json
{"name":"Aurora","package":"com.example.aurora","targets":["android","ios"],"network":"ktor","storage":"settings","network_url":"https://httpbin.org/uuid"}
```

The HTTPS endpoint must have a host and no userinfo/fragment. Omission uses a demo
URL; replace it for the product. Disabling Ktor requires explicitly removing any
entered URL. Config/plan never calls the endpoint. Tests use MockEngine, not public
API availability. Browser CORS is runtime-dependent and cannot be inferred from a
valid URL. No credentials or authentication are configured by this recipe.

Ktor uses CIO on Android/JVM, Darwin on iOS and Js on Wasm. Generated UI has a
Fetch action, loading/result/error state, timeout and cancellation on disposal.
Swift callbacks capture owners weakly. Keep the owner above the back stack.

Settings persists small non-secret values using SharedPreferences on Android,
NSUserDefaults on iOS, an app-specific file on desktop and localStorage on web.
Android host initializes the application context before creating the shared UI.
Do not replace denied storage with successful in-memory stubs. JVM file tests
reopen real temporary files; common tests use explicit test settings. These do
not establish browser quota/origin behavior or mobile-device durability.
With Settings and iOS selected, `./kotlin test --platform iosSimulatorArm64` also tests a real
NSUserDefaults adapter reopening the counter, restoring its namespaced key in
`finally`. This requires full Xcode plus an installed iOS simulator runtime.
The Toolchain iOS build also builds the Swift application; native unit tests do
not interact with its UI.

The plan includes selected pins/adapters and verification commands. Settings is
not SQL or encrypted credential storage; modern Room/DataStore/Retrofit recipes
are not implemented.
