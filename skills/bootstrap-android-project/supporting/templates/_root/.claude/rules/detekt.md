---
paths:
  - "**/*.kt"
---

# Detekt Code Quality Rules

> This project uses Detekt for Kotlin code quality analysis. Config: `config/detekt.yml`

## How to Run

```bash
# Ensure JAVA_HOME is set (JDK 17 required)
./gradlew detekt                        # Entire project
./gradlew :feature:profile:detekt       # Specific module
./gradlew detektBaseline                # Generate baseline for legacy code
```

**Workflow:** Edit `.kt` -> Run detekt -> Fix issues -> Re-run -> Only when 0 errors -> Continue

## Rule Thresholds (config/detekt.yml)

### Complexity
| Rule | Threshold | Fix |
|------|-----------|-----|
| CyclomaticComplexMethod | 10 | Break into smaller functions, early returns |
| LongMethod | 50 lines | Extract logic into separate functions |
| LongParameterList | 8 (functions) / 9 (constructors) | Group into data classes |
| TooManyFunctions | 20 (files) / 15 (classes) | Split responsibilities. Ignores `@Preview` |

### Coroutines
- `GlobalScope` -> `viewModelScope`, `lifecycleScope`
- `suspend fun`: Flow<T>` -> `fun`: Flow<T>` (Flow is already async)
- Redundant suspend -> remove if function doesn't actually suspend

### Exceptions
- Catching `Exception`, `Throwable`, `RuntimeException` -> Specific exceptions (`IOException`, `HttpException`)
- Prefer `runCatching {}` with `Result<T>`

### Naming
- Functions: `camelCase()` — `@Composable`: `PascalCase()`
- Packages: all lowercase `com.example.app.feature.profile`
- Constants: `UPPER_SNAKE_CASE`

### Style
| Rule | Guideline |
|------|-----------|
| ClassOrdering | properties -> init -> public fns -> private fns -> companion |
| MagicNumber | Extract to constants. Ignores: -1, 0, 1, 2, hashCode, properties, annotations, enums, companion |
| MaxLineLength | 140 chars (excludes comments) |
| UnusedPrivateMember | Remove unused. Allows `.*Preview.*` |
| UseCheckOrError | `check(condition) { msg }` instead of `if + throw IllegalStateException` |
| UseRequire | `require(condition) { msg }` for parameter validation |

### Compose-Specific
| Rule | Guideline |
|------|-----------|
| ComposableParamOrder | Modifier as **last** optional parameter |
| Material2 | No Material 2 -> Material 3 only |
| ModifierMissing | Public @Composable must have `Modifier` param |
| ModifierWithoutDefault | Must have `= Modifier` default |
| ModifierNotUsedAtRoot | Apply modifier to root element |
| MutableParams | No mutable parameters |
| RememberMissing | Use `remember` for mutable state |
| UnstableCollections | `MutableList` -> `ImmutableList`, `persistentListOf()` |
| ViewModelInjection | `hiltViewModel()`, don't pass ViewModel as param |

### Compose CompositionLocal Allowlist
`LocalColors`, `LocalShapes`, `LocalTypography`

## Fixing Issues

1. Read error -> understand rule -> fix using patterns above
2. Re-run `./gradlew detekt` -> verify 0 errors
3. False positive -> `@Suppress("RuleName")` with justification comment

## Resources
- **Config:** `config/detekt.yml`
- **Plugin:** `build-logic/convention/src/main/kotlin/DetektConventionPlugin.kt`
- **Reports:** `build/reports/detekt/detekt.html`
