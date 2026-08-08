# Architecture Tests (Konsist)

Run via `./gradlew :core:architecture-test:test --rerun-tasks`. Each rule is one test in its
own file so a failure fingerprints itself in CI logs. Config lives in
`core/architecture-test/config.yml`; the suppress allow-list lives in
`core/architecture-test/suppress-allowlist.yml`.

| # | File | Rule | Default |
|---|------|------|---------|
| 1 | `Rule01DomainIsolationTest.kt` | `domain/repository/` & `domain/usecase/` import nothing from `*.data.*`, `*.presentation.*`, `*.ui.*`, `*.database.entity.*`, `*.network.api.*` | on |
| 2 | `Rule02RepositorySignatureTest.kt` | public `*Repository` methods never accept/return types whose simple name ends with `Entity`, `Dto`, `Ui`, `StateUi`, `ActionUi`, `EventUi` | on |
| 3 | `Rule03DomainModelPurityTest.kt` | classes in `domain/model/` carry none of `@Serializable`, `@SerialName`, `@Entity`, `@PrimaryKey`, `@ColumnInfo`, `@Embedded`, `@TypeConverter`, `@Json*` | on |
| 4 | `Rule04RepositoryPairingTest.kt` | each `domain/repository/` interface has exactly one impl named `*RepositoryImpl` in `data/repository/`; no orphans | on |
| 5 | `Rule05ViewModelSizeTest.kt` | `*ViewModel` primary-ctor param count ≤ `viewModelMaxParams` | 8 |
| 6 | `Rule06MapperConformanceTest.kt` | classes ending `Mapper` implement `{{PACKAGE_BASE}}.core.domain.mappers.Mapper<FROM, TO>` or `BidirectionalMapper<A, B>` | on |
| 7 | `Rule07FeatureAbiTest.kt` | `feature/X/` imports nothing from another feature's `data`/`presentation`/`domain.repository` packages | on |
| 8 | `Rule08SuppressRationaleTest.kt` | every `@Suppress`/`@file:Suppress` in `src/main/` has a `// SUPPRESS-RATIONALE:` comment on the immediately-preceding line, unless the rule name is in `suppress-allowlist.yml` | on (separable) |

## Interaction with optional modules

- The `database` module's `*Entity` classes carry `@Entity`/`@PrimaryKey`/`@ColumnInfo`, but
  they live under `core.database.entity`, NOT under any `domain/model/` package — so rule #3
  (domain model purity) does not fire on them. Persistence types are allowed their
  annotations; only domain models must stay clean.
- Optional repositories (e.g. `billing.PremiumStateRepository`) follow rule #2: their public
  methods expose only domain/primitive types (`Flow<Boolean>`), never `*Entity`/`*Dto`/`*Ui`.
- `auth`'s OkHttp `AuthInterceptor` lives in `core.auth.interceptor`, outside any `domain/`
  package, so its framework imports do not trip rule #1.

## Tightening

Raise rigor by lowering `viewModelMaxParams` in `config.yml` or by removing entries from
`suppress-allowlist.yml`. The allow-list keys on detekt RULE NAMES, never file paths, so no
module can silence a category project-wide via an obscure file.
