# android-kmp-bootstrap-skills

Two [Agent Skills](https://agentskills.io) that scaffold a hardened Kotlin project — native
Android or Kotlin Multiplatform (Android + iOS) — with Clean Architecture, a multi-module
Gradle setup, and architecture tests that **fail the build** when a layer boundary is crossed.

These are not file templates. They are procedures an agent follows: it interviews you about
the project, expands the module dependency closure from your choices, generates the tree,
runs the verification build, and reports per-rule status.

| Skill | Target | Stack |
|---|---|---|
| `bootstrap-android-project` | Native Android | Hilt · Compose Material 3 · Room/DataStore · Retrofit · MVI Delegate |
| `bootstrap-kmp-project` | Android + iOS | Koin · Compose Multiplatform · Room KMP · Ktor · SKIE iOS interop |

## Install

**Claude Code** — as a plugin, so you get versioning and updates:

```
/plugin marketplace add maxsoloma/android-kmp-bootstrap-skills
/plugin install bootstrap-projects@android-kmp-bootstrap-skills
```

**Any other agent** — Codex, GitHub Copilot, Gemini CLI, Cursor, Firebender and others read
the shared `~/.agents/skills/` directory:

```bash
git clone https://github.com/maxsoloma/android-kmp-bootstrap-skills.git
mkdir -p ~/.agents/skills
cp -R android-kmp-bootstrap-skills/skills/* ~/.agents/skills/
```

Per-agent paths, project-scoped installs, and the agents that need a different directory are
in **[docs/INSTALL.md](docs/INSTALL.md)**.

## Use

Ask for a new project in plain language, or invoke the skill directly:

```
/bootstrap-android-project
/bootstrap-kmp-project
```

Installed as a plugin, the namespaced form `/bootstrap-projects:bootstrap-android-project`
always works; the bare name works too unless it collides with another skill.

The agent asks four questions — project basics, optional modules, first feature name, and
plan approval — and writes nothing to disk until you approve the plan tree.

## What you get

**Required modules** (always generated): `domain`, `design`, `di`, `navigation`, `network`
(api/impl split), `resources`, `utils`, plus the architecture-test module and one starter
feature.

**Optional modules** (you pick; dependencies resolve transitively): `analytics`, `auth`,
`billing`, `config`, `database`, `datastore` (KMP), `dma`, `image`, `permissions`,
`remote-config` (Android), `sync`. Picking `billing` also pulls in `remote-config`,
`analytics`, and `config` — you don't track that yourself.

**Architecture tests** — the part that makes this different from a template. Konsist rules
run as ordinary unit tests, so a violation is a red build, not a review comment:

| # | Rule |
|---|---|
| 1 | `domain/repository` and `domain/usecase` import nothing from data, presentation, or UI packages |
| 2 | Public `*Repository` methods never accept or return `*Entity`, `*Dto`, or `*Ui` types |
| 3 | `domain/model` classes carry no `@Serializable`, `@Entity`, `@PrimaryKey`, or converter annotations |
| 4 | Every `domain/repository` interface has exactly one `*RepositoryImpl` — no orphans on either side |
| 5 | `*ViewModel` constructor parameters stay within a configurable limit (default 8) |
| 6 | `*Mapper` classes implement the shared `Mapper<FROM, TO>` contract |
| 7 | A feature imports nothing from another feature's internals |
| 8 | Every `@Suppress` carries a `// SUPPRESS-RATIONALE:` comment or is explicitly allow-listed |

The KMP skill adds three source-set rules that catch the mistakes specific to multiplatform:

| # | Rule |
|---|---|
| 9 | No `actual` declarations in `commonMain` |
| 10 | `commonMain` imports no `android.*`, `platform.*`, `java.*`, `javax.*`, or `kotlinx.cinterop` |
| 11 | Types from `androidMain`/`iosMain` never appear in public `commonMain` signatures |

Rules 5, 8, and 10 read their thresholds and allow-lists from `config.yml` and
`suppress-allowlist.yml`, so you can tune them without editing test code.

**Zero-baseline detekt.** No `detekt-baseline.xml` ships, and a `checkNoDetektBaseline` task
fails the build if one appears. Generating a baseline to silence violations is the one escape
hatch these skills deliberately remove.

## Requirements

- **zsh** — the generators use associative arrays and the `:l` case modifier, so bash 3.2
  (still the default `/bin/bash` on macOS) will not run them. zsh is the macOS default shell
  and is packaged for every major Linux distribution.
- **python3** — used for path-segment rewriting that plain shell handles badly.
- **JDK 17+** and the **Android SDK** for the verification build.
- **macOS with Xcode** — only for the KMP skill's iOS link check. Without it that step is
  reported as SKIPPED rather than silently passed.

## Scope

These skills only create new projects. Both abort if the target directory already contains a
`settings.gradle.kts` — they never modify an existing project.

## Contributing

Issues and pull requests are welcome. When changing a template, please describe which
verification steps you ran; `supporting/verification.md` in each skill lists the full gate.

## License

[MIT](LICENSE)
