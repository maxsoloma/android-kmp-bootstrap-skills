# Platforms

Shared Compose supports any nonempty subset of Android, iOS, desktop JVM and
browser Wasm. Native Android supports Android only. SwiftUI requires iOS and
optionally Android; its shared Kotlin module remains free of Compose UI.

iOS supports ARM64 device and simulator targets. NavigationStack requires iOS
16.0+. Desktop uses the local OS Compose artifact. Wasm needs a WasmGC-capable
browser; Toolchain tests provision Chromium with Playwright. Generation is offline;
initial builds need network access. Inspect catalog/plan for exact choices/pins.
