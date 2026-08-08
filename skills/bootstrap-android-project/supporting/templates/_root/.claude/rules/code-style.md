---
paths:
  - "**/*.kt"
---

# Code Style — Comments & Documentation

## Default Rule: No Comments

**Do not write comments unless explicitly required below.** If code needs a comment to be understood — rename or extract first. The less comments, the better.

## KDoc — interface contracts only

KDoc is allowed on **`interface` and `internal interface` methods** when behavior is non-obvious.
The rule is based on **contract vs implementation**, not on visibility:

- `interface` / `internal interface` method — it is a contract boundary, KDoc is allowed
- `internal class ...Impl` method — implementation detail, the interface KDoc is sufficient
- `@Composable` functions — use descriptive naming instead
- Obvious members where the name is self-explanatory (`fun getUser(): User`)

```kotlin
// — internal interface is still a contract
internal interface GenerationAnalyticsTracker {
    /** errorType is null on success. generationTimeSeconds is wall-clock time. */
    fun trackGenerationResult(outfitCount: Int, isSuccess: Boolean, generationTimeSeconds: Long, errorType: String?)
}

// — public interface contract
interface GetSavedOutfitsUseCase {
    /** Returns empty list (not error) when the user has no saved outfits. */
    suspend operator fun invoke(userId: String): Result<List<SavedOutfit>>
}

// — implementation, interface is already documented
internal class GetSavedOutfitsUseCaseImpl : GetSavedOutfitsUseCase {
    // NO KDoc here — redundant
    override suspend fun invoke(userId: String) = ...
}
```

## Inline Comments — WHY only, never WHAT

Only when the reason cannot be expressed in code (API quirk, workaround, non-obvious business rule):

```kotlin
// Never
updateState { copy(isLoading = true) } // Set loading to true

// Only when unavoidable
// Outfit API returns 404 for new users — treat as empty state, not error
val outfits = result.getOrElse { emptyList() }
```

Forbidden: section dividers (`// --- Setup ---`), `//region` blocks, comments explaining what the code does.

## TODOs

Require a ticket: `// TODO(TICKET-123): description` — No bare `TODO` in production code.

## Never

- Commented-out code — delete it (git history preserves it)
- `//region` / `//endregion`
- Comments that restate what the code does

## Self-documenting Code

Prefer extraction and naming over comments. Use `require()` / `check()` for preconditions:

```kotlin
// Bad
val d = Date() // current date
// Good
val currentDate = Date()

// Bad
// Validate and map
val name = input.trim().takeIf { it.isNotEmpty() } ?: throw IllegalArgumentException("...")
// Good
val name = validateAndMapName(input)
```
