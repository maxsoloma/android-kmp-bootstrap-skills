# Optional module dependency graph

The skill reads each `core-optional/<x>/_manifest.yml` and computes the
transitive closure of the user's selection. A user CANNOT end up with a
module whose prerequisites are absent.

| Module       | Requires (transitive)      | Notes |
|--------------|----------------------------|-------|
| database     | —                          | Room KMP + BundledSQLiteDriver; default-selected |
| datastore    | —                          | DataStore Preferences (`androidx.datastore:datastore-preferences`, NOT `-core`) |
| analytics    | —                          | `AnalyticsTracker` commonMain + platform sinks |
| auth         | —                          | expect/actual secure storage |
| config       | —                          | BuildKonfig (`com.codingfeline.buildkonfig`) — typed commonMain constants |
| image        | —                          | Coil 3 CMP (`io.coil-kt.coil3`) |
| permissions  | —                          | expect/actual permission flow |
| dma          | —                          | Data Manipulation Adapter |
| sync         | database                   | PendingChangesManager persists pending rows |
| billing      | analytics, config          | facade emits analytics + reads BASE flags |

## Closure algorithm (skill applies this)

1. Start with the user's checked set S.
2. For each m in S, union in `requires(m)`; repeat to a fixed point.
3. Copy `core-optional/<m>/` for every m in the closure.
4. Emit `include(":core:<m>")` per module into `settings.gradle.kts`.

## Acceptance check
Selecting `billing` alone yields `{billing, analytics, config}` on disk.
Selecting `sync` alone yields `{sync, database}`.
