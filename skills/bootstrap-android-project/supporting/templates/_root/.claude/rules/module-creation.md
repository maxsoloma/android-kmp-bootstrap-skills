---
paths:
  - "**/build.gradle.kts"
---

# Module Creation Rules

## Feature Module Structure
```bash
mkdir -p feature/<name>/src/main/kotlin/<your/package/base>/feature/<name>/{ui,presentation,navigation,di,domain,data}
```

## build.gradle.kts
```kotlin
plugins {
    alias(libs.plugins.app.feature)
}

android {
    namespace = "<your.package.base>.feature.<name>"
}
```

## Register in settings.gradle.kts
```kotlin
include(":feature:<name>")
```

## Convention Plugins

### `app.feature` — Feature modules
Includes automatically (do NOT re-add these dependencies):
- **Plugins:** `com.android.library`, `kotlin.android`, `kotlin.plugin.compose`, `kotlin.plugin.serialization`, `hilt.android`, `ksp`, `detekt`
- **Compose:** `compose-bom`, `compose-ui`, `compose-material3`, `compose-ui-tooling-preview`
- **Hilt:** `hilt-android`, `hilt-compiler` (via ksp)
- **Navigation:** `hilt-navigation-compose`
- **Lifecycle:** `lifecycle-runtime-compose`, `lifecycle-viewmodel-compose`
- **Core modules:** `:core:design`, `:core:domain`, `:core:navigation`
- **Other:** `kotlinx-coroutines-android`, `kotlinx-serialization-json`, `androidx-core-ktx`, `timber`

### `app.library` — Core library modules
Includes: `com.android.library`, `kotlin.android`, `detekt`, `androidx-core-ktx`, `coroutines-core`, `coroutines-android`, `timber`

### `app.hilt` — Add Hilt to any module
Includes: `ksp`, `hilt.android` plugin, `hilt-android`, `hilt-compiler`

### `app.room` — Room database
Includes: `ksp`, `androidx.room` plugin, `room-runtime`, `room-ktx`, `room-compiler`, schemas in `$projectDir/schemas`

### `app.compose` — Compose only (without library base)
Includes: `kotlin.plugin.compose`, `compose-bom`, `compose-ui`, `compose-material3`, `compose-ui-tooling-preview`

### When to add extra dependencies
Only add to `build.gradle.kts` what is NOT already included by the plugin:
```kotlin
plugins {
    alias(libs.plugins.app.feature)
    // Add only if NOT covered by the feature plugin:
    // alias(libs.plugins.app.room)  <- if the feature needs local DB
}

dependencies {
    // Add ONLY what's missing from app.feature
    implementation(project(":core:network"))  // not included by default
    implementation(project(":core:auth"))      // not included by default
    // DO NOT add: core:design, core:domain, core:navigation, hilt, compose, coroutines
}
```

## Naming
- NO prefixes: `feature/profile` not `feature/feature-profile`
- NO prefixes: `core/domain` not `core/core-domain`

## Core Module build.gradle.kts
```kotlin
plugins {
    alias(libs.plugins.app.library)
}

android {
    namespace = "<your.package.base>.core.<name>"
}
```
