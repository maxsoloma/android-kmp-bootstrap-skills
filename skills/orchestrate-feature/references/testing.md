# Independent Kotlin feature testing

Read this file when authoring or repairing behavioral tests, including a delegated
tester task. It is bundled with this skill; another plugin or a global test skill
is not required. Read the local AGENTS.md and the requirement/public interface
packet before implementation details. Keep production repair with its author.

## Turn requirements into observable cases

Create a compact requirement-to-case map before editing tests. Expected outcomes
come from the contract and user-visible behavior, not a copy of the implementation.
Choose meaningful boundaries, invalid input, negative outcomes, state transitions,
no-ops, cancellation/lifecycle and affected platform consumers. Include only cases
relevant to the feature. For state/history features, check both the result and
whether the original state/history remains correct after rejected operations.

Own only the assigned test files. Do not change production code, loosen assertions,
alter dependency/toolchain pins or redefine acceptance to obtain green checks.
Record an ambiguous contract as a question; continue independent cases. A failing
check needs diagnosis before a new attempt. Fix incorrect tests in the test scope;
route implementation defects to the code author with input/expected/actual evidence.

## Select and report real checks

Use existing kotlin.test/test conventions and the checked-in wrapper. Consult
current official docs through Context7 when library/CLI decisions require it.
Read [platform verification](verification.md) for Android, iOS and browser commands.
Do not substitute JVM results for iOS/Wasm or a build for executed tests.

When adding behavior, capture a meaningful RED if practical: the test fails for
the missing behavior, not an SDK/configuration failure. A missing newly agreed
symbol is a valid compilation RED; it does not establish a runtime assertion
failure. Host-owned platform execution can run after test authorship; clearly say
which commands YOU ran and which receipts came from the host. Avoid concurrent
Toolchain/Xcode processes that share outputs or a simulator.

Run the relevant checks on the integrated candidate after code and tests settle.
Record exact argv, observed exit code, log path, executed test counts and skips.
Attach the source fingerprint used by the host. Source changes require affected
rechecks, even when tests themselves did not change. Missing SDK/simulator/network
is blocked coverage and must not become passed evidence. Keep unit/platform,
UI interaction and real integration acceptance separate.

## Bounded tester packet

The host passes these fields as a small packet, with paths rather than copied logs:

```text
Role: independent tester
Skill: <this-skill>/SKILL.md
Role guide: <this-skill>/references/testing.md
Project / branch / run / task / owner: <actual identities>
Goal and acceptance: <observable requirements>
Public interfaces: <agreed signatures and semantics>
Owned test files: <disjoint write scope>
Relevant platforms and checks: <actual selected consumers>
Execution ownership: <worker commands versus host-owned serial platform checks>
References: <requirements and only needed local instructions>
Return: applied skill paths, requirement-to-case map, changed files,
        actual commands/results/logs, findings and unavailable coverage.
```

Select other installed specialist skills only for a concrete need (for example,
accessibility or API contracts), after reading their actual instructions. Pass
exact available paths to the worker and require its result to name the applied
skills. Do not assume the parent's installed catalog is available to a no-history
worker. If an external skill is absent, this bundled testing guide still works;
report any missing genuinely required capability. Loading a guide does not prove
it was followed: the host reviews cases and actual evidence before settlement.

## UI runner observations

Wait for the rendered state after an action, not just the return of the input
command. A canvas accessibility bridge may expose role/name text without HTML
disabled attributes; do not use a DOM isEnabled result alone as proof of Compose
button state. Check actual no-op behavior and the rendered/accessible state.
For simulator setup, inspect the installed runtime's supportedDeviceTypes rather
than assuming every listed device type is compatible. Select the actual active
display of multi-display/foldable devices before judging a black screenshot.
