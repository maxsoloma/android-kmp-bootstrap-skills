# Official Koin agent skills

- Upstream: https://github.com/InsertKoinIO/koin-skills
- Commit: a9fb11f8446c0683ffdd76b49b504d14134fa9d2
- Plugin version: 2.0.0
- Retrieved: 2026-10-29
- License: Apache-2.0, see LICENSE
- Documentation: https://insert-koin.io/docs/tooling/ai-agents/

The complete upstream skills/ tree is copied without modifications. Local
generation copies it into the selected agents' project skill directories.
No upstream scripts are run. Kotzilla configuration uses the upstream plugin's
HTTP endpoint, https://mcp.kotzilla.io/mcp; OAuth remains a user login step.

These skills include Gradle examples. Generated AGENTS.md preserves the owning
project's Kotlin Toolchain backend and existing DSL; installing these skills
does not install the Koin Compiler Plugin, Gradle, KSP or the Kotzilla SDK.
