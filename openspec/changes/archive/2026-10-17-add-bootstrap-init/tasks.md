# Tasks

## 1. Configuration and compatibility

- [x] 1.1 Implement strict ProjectSpec resolution, structured issues and the current-profile/module catalog; add resolver/catalog tests and document the JSON contract.
- [x] 1.2 Implement read-only plan, config generation and spec/lock replay; verify no-write failures, lock mismatches, all-profile output equivalence and documented CLI examples.

## 2. Terminal wizard

- [x] 2.1 Implement init with numbered menus, editing/backtracking, invalid-input recovery, review/create/save/cancel and non-TTY guidance; cover state transitions, EOF and publication guards with tests and document terminal usage.

## 3. Agent interfaces

- [x] 3.1 Update all skill flows and README to use catalog/config/plan without hidden terminal interaction; preserve legacy commands and hardened verification, and verify examples with an isolated JAR.

## 4. Integration

- [x] 4.1 Run the full existing/new Kotlin tests, rebuild all skill JARs, exercise a real PTY init and a generated modern build, validate OpenSpec and record host-specific skips and actual results.
