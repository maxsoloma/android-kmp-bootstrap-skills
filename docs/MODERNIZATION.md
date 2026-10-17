# Toolchain architecture

Version 3 uses Kotlin Toolchain for both the standalone Kotlin/JVM bootstrap CLI
and generated apps. The wizard and agent adapters share strict configuration,
compatibility validation, offline rendering, no-overwrite publication and locked
replay. Build/test evidence is separate from generation.

The local Kotlin build plugin prepares indexed resources and bundles the CLI with
its runtime dependencies. Only `bootstrap-project` is shipped; Claude and Codex
manifests use the same version. No Python runtime is required.

Kotlin Toolchain wrappers are pinned to the published release, including upstream
checksums. Root and generated builds pin Kotlin and library versions explicitly.
Application modules use declarative YAML and platform-qualified source folders.
The native Xcode host uses the Toolchain integration script and KotlinModules.

The user explicitly requested removal of Gradle and hardened profiles. Their old
configuration now fails explicitly, before writes. Prior OpenSpec records retain the migration history. Retired source templates,
golden hashes and obsolete local Gradle caches have been deleted.
