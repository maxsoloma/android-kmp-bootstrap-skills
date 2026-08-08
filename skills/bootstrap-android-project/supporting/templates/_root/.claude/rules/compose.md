---
paths:
  - "**/ui/**"
  - "**/presentation/**"
---

# Compose UI Rules

## Material 3 Only
- No Material 2 imports (`androidx.compose.material.*`)
- Only `androidx.compose.material3.*`

## Modifier Rules
- Public @Composable **must** have `Modifier` parameter
- Modifier must have default `= Modifier`
- Apply modifier to **root** element only
- Never reuse modifier on multiple elements

## State & Remember
- Use `remember` for mutable state in composables
- No mutable collections as params — use `ImmutableList`, `persistentListOf()`

## CompositionLocal
Allowed: `LocalColors`, `LocalShapes`, `LocalTypography`

## Preview
- `@Preview` functions: PascalCase, private
- Exempt from `UnusedPrivateMember` if name contains "Preview"
