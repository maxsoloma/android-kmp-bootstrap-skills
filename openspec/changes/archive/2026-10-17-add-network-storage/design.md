# Design

## Context

See proposal.md for motivation. The current Kotlin CLI supports strict config,
six presets, UI/DI/navigation compatibility, additive modern rendering and 42
tests. Existing hardened modules and toolchain pins must remain byte-compatible.
No main specs have been synced; prior completed changes remain active for review.

## Goals / Non-Goals

Goals: independently selectable real HTTP and key-value persistence on every
current modern target; testable owners, explicit failures and shared terminal /
agent configuration. Generation stays offline.

Non-goals: modern SQL databases, DataStore, Retrofit, offline synchronization,
credentials/auth flows, shared Swift coroutine/ViewModel framework or migrations.
Settings is key-value storage, not a database or secure credential store.

## Decisions

- Add optional `network` (none/ktor), `storage` (none/settings), `network_url` to
  ProjectSpec v1 and flat flags. Omission preserves all previous options and
  outputs. Hardened defaults use a reserved `profile` choice for its existing
  data stack; overrides require choosing modern explicitly.
- Validate HTTPS URI with host, no userinfo or fragment. An endpoint with network
  none/profile is an error. Default demo endpoint is https://httpbin.org/uuid;
  no request occurs at generation, startup or during tests. A button performs GET
  and displays a bounded text preview. CORS, connectivity and service uptime are
  runtime limitations, not claims derived from URL validation.
- Ktor 3.6.0 uses CIO on Android/desktop/JVM logic tests, Darwin on iOS, Js on
  wasmJs; the published Js artifact includes Wasm variants. Use a 10-second request
  timeout, expectSuccess and owned HttpClient/coroutine scope. Callbacks resume on
  Main; Android and desktop get coroutines Android/Swing dispatchers. Inject mock
  engines/dispatchers for tests. Closing cancels scope/client; cancellation is not
  converted into an error callback. Swift uses plain callbacks with weak captures.
- Settings 1.3.0 has confirmed Wasm publication. Use explicit adapters, not its
  no-argument initializer: SharedPreferences (application context, committed
  writes), NSUserDefaults (app-specific suite), PropertiesSettings with file
  persistence on JVM, StorageSettings (localStorage) on Wasm. Android context is
  initialized by the host before shared UI. The file adapter accepts an explicit
  path, supports atomic replacement and is tested in a temporary directory.
- Preserve the old rendering path exactly when both new options are omitted.
  Data rendering layers over the existing stack recipe, replaces its counter
  owner with an injected store-aware graph, and adds network UI only when selected.
  Manual/Koin graphs own their clients; tests use fresh in-memory test stores and
  mock engines. Storage errors are surfaced and never treated as saved state.
- Keep component pins separate from existing toolchain metadata. Plan lists the
  exact selected adapters, added dependencies and verification limits. Browser
  and device persistence require runtime checks, independently of JVM file tests.

## Risks / Trade-offs

- Settings APIs have platform-specific opt-ins → inspect versioned source and
  compile the actual selected source sets; keep opt-ins narrowly scoped.
- Synchronous small key-value writes can block briefly → constrain example to
  a small counter; document that this is not a bulk storage/transaction recipe.
- File/localStorage failures can occur → preserve failure state, do not reset or
  silently fall back to memory. Test failed writes and reopening explicitly.
- Initial local checks used Command Line Tools. Follow-up with installed Xcode
  27.1 must compile/link native frameworks, build the Swift app and run native
  tests on an available simulator, recording environmental blockers separately.

## Xcode verification follow-up

The user requested actual Xcode builds/tests after accepting its license.
Xcode's generic simulator destination requests x86_64 as well as ARM64 by default,
but the documented generated targets are ARM64 only. Correct the modern Xcode
template to match those targets, preserve unrelated output and binary assets,
and update only affected golden entries for reviewed integration fixes. Follow
current direct-integration guidance for IDE build guards and script execution.
Check Kotlin Toolchain CLI separately: its Alpha build system and YAML layout
are not prerequisites for the existing Gradle/Xcode framework integration.

## Migration Plan

Rebuild all three standalone JARs and update skills/docs. New fields default to
none for modern and profile for hardened. Existing locks remain tied to their
original bundle as before. No existing application files are modified.

## Sources

Context7 and versioned primary artifacts reviewed 2026-10-17:
- https://github.com/ktorio/ktor-documentation (client engines, timeout, MockEngine)
- https://github.com/russhwolf/multiplatform-settings/tree/v1.3.0
- Maven metadata/modules: Ktor 3.6.0, Settings 1.3.0, coroutines 1.11.0, including
  Settings wasmJs and Ktor Js wasmJs variants.
