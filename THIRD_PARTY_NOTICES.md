# Third-party notices

This project is MIT licensed. Kotlin and kotlinx.serialization (JetBrains and
contributors), JetBrains annotations, and the bundled Kotlin Toolchain wrapper
scripts use Apache License 2.0. Original script notices/checksums are retained.
The CLI's executable includes runtime dependencies and their upstream notices.
See `src/main/resources/META-INF/licenses/` for bundled license texts.

Kotlin Toolchain upstream: https://github.com/JetBrains/kotlin-toolchain
Kotlin: https://github.com/JetBrains/kotlin
Serialization: https://github.com/Kotlin/kotlinx.serialization

Official Koin agent skills (Kotzilla/InsertKoinIO), Apache License 2.0:
https://github.com/InsertKoinIO/koin-skills
Vendored unchanged at commit a9fb11f8446c0683ffdd76b49b504d14134fa9d2 (plugin 2.0.0)
in `src/main/resources/koin-agent/skills/`, with LICENSE, PRIVACY.md and SOURCE.md
alongside. Included in both standalone skill JARs and copied to generated projects
only for Koin DI with coding agents selected.
