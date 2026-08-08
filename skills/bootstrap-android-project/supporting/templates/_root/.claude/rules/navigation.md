---
paths:
  - "**/navigation/**"
---

# Navigation Rules

## DI Contribution Pattern

```
feature/main/      — MainScreen with NavHost (no feature deps)
core/navigation/   — FeatureNavGraphBuilder interface
feature/<name>/    — Registers its graph via Hilt @IntoSet
```

## Creating Navigation

**1. Define destination (Kotlin Serialization):**
```kotlin
@Serializable
data object Profile : TopLevelDestination
```

**2. Create NavGraphBuilder:**
```kotlin
internal class ProfileNavGraphBuilder @Inject constructor() : FeatureNavGraphBuilder {
    override fun registerGraph(navGraphBuilder: NavGraphBuilder, navController: NavHostController) {
        navGraphBuilder.composable<Profile> { ProfileDestination(navController) }
    }
}
```

**3. Register in Hilt:**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
internal abstract class ProfileNavigationModule {
    @Binds @IntoSet
    abstract fun bind(impl: ProfileNavGraphBuilder): FeatureNavGraphBuilder
}
```

## Rules
- Type-safe navigation with Kotlin Serialization
- Each feature registers its own routes via `@IntoSet`
- MainScreen has no direct feature dependencies
- No hardcoded route strings
