# Konsist architecture rules (`:core:konsistTest`)

Standalone JVM module (`org.jetbrains.kotlin.jvm`) — Konsist 0.17.3 parses
source via PSI, so a JVM test sees `commonMain`/`iosMain`/`androidMain` text.
Run: `./gradlew :core:konsistTest:test --rerun-tasks`. One rule = one file
`Rule01…Test.kt` … `Rule11…Test.kt` so a failure fingerprints itself.

Scope is built once in `ProjectScope.kt` via
`Konsist.scopeFromProject(sourceSetName = "commonMain")` (rule 11 adds
`androidMain`/`iosMain` scopes).

| # | Rule | Scope | Origin |
|---|------|-------|--------|
| 1 | Domain isolation — `domain/repository`,`domain/usecase` import no `*.data.*`/`*.presentation.*`/`*.ui.*`/`*.database.entity.*`/`*.network.api.*` | commonMain | ported |
| 2 | Repository signature purity — public `*Repository` methods avoid `*Entity`/`*Dto`/`*Ui`/`*StateUi`/`*ActionUi`/`*EventUi` types | commonMain | ported |
| 3 | Domain model purity — `domain/model` carries none of `@Serializable`,`@SerialName`,`@Entity`,`@PrimaryKey`,`@ColumnInfo`,`@Embedded`,`@TypeConverter` | commonMain | ported |
| 4 | Repository pairing — each `domain/repository` interface has exactly one impl in `data/repository` | commonMain | ported |
| 5 | ViewModel size — `*ViewModel` ctor params ≤ `viewModelMaxParams` (default 8, reads from `config.yml`) | project | ported |
| 6 | Mapper conformance — `*Mapper` classes implement `core.domain.mappers.Mapper<FROM,TO>` | commonMain | ported |
| 7 | Feature ABI — `feature/X` imports only `feature/Y/api/*` | commonMain | ported |
| 8 | Suppress rationale — every `@Suppress` preceded by `// SUPPRESS-RATIONALE:` (allow-list in `config/suppress-allowlist.yml`) | project | ported |
| 9 | expect/actual PLACEMENT — no `actual` in commonMain | commonMain | KMP |
| 10 | commonMain platform isolation — commonMain imports no `android.*`,`platform.*`,`java.*`,`javax.*`,`kotlinx.cinterop` (allow-list in `config.yml`) | commonMain | KMP |
| 11 | Cross-target leak — types from `androidMain`/`iosMain` not in public commonMain function signatures | cross | KMP |

## Rule 9 resolution (Open Question §7)
Konsist 0.17.3 exposes `hasActualModifier` but has NO cross-source-set
linking. Rule 9 is therefore **placement-only**: assert no commonMain
declaration has the `actual` modifier. Bidirectional completeness (every
`expect` has a matching `actual`) is ALREADY a compile error, so we do not
duplicate it. This matches spec §3.1/§7.

## Rule 10 — the load-bearing KMP guard (acceptance criterion)
A deliberate `import android.content.Context` in any commonMain file MUST
fail rule 10. The androidx-lifecycle / compose / datastore commonMain
artifacts are allow-listed by exact prefix in `config.yml`.

## Rule 11 — known limitation
Konsist cannot resolve cross-source-set types, so rule 11 matches platform
type SIMPLE NAMES against public commonMain function signatures (return +
parameter types). It catches the common leak (a platform-only type named in
a common signature) without a full type resolver.
