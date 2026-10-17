# Configuration and replay

Use the bundled standalone JAR with Java 17+. `catalog --json` describes choices;
`plan --config SPEC --output OUTPUT` validates without creating output; `generate`
with the same arguments publishes only to an absent or empty destination.

```json
{"schema_version":2,"build_system":"kotlin-toolchain","name":"Aurora","package":"com.example.aurora","kind":"kmp","targets":["android","ios","desktop","web"],"ui":"shared-compose","di":"koin","navigation":"navigation3","network":"ktor","storage":"settings","agents":["codex","claude"],"sdk":{"min":26,"target":37,"compile":37},"ios_deployment_target":"15.0"}
```

Required: PascalCase `name` and dotted lowercase non-keyword `package`.
`app_id` defaults to package. Kind `android` supports only Android; kind `kmp`
supports any nonempty subset of Android/iOS/desktop/web. SwiftUI needs iOS and
allows Android, with no desktop/web. NavigationStack needs iOS 16.0+.

Network/storage are independent none/ktor and none/settings. A Ktor endpoint must
be absolute HTTPS with a host, no credentials/fragment and a valid port. Omission
uses the demo endpoint; disabling networking requires removing an explicit URL.
Defaults: shared Compose, manual DI, no navigation/network/storage, Android+iOS,
SDK 26/37/37 and iOS 15.0. Native Android defaults to Android Compose.

Unknown keys, wrong JSON types and incompatible choices return structured issues
before writes. Modern schema v1 choices are accepted and normalized to v2.
Hardened profiles, feature/modules and a Gradle backend are explicitly rejected.

Generated projects include portable `bootstrap.spec.json` and `bootstrap.lock.json`.
Replay the spec with the same CLI; its sibling lock is loaded automatically.
The lock captures configuration, bundled recipes and generated file hashes, not
output paths. A changed config/bundle/old lock fails instead of silently upgrading.
To deliberately use a different recipe, start from a config without the old lock.
Generation never installs tools, initializes Git, calls a public API or builds apps.
