# Tasks

## 1. Source-delivered skill layout

- [x] 1.1 Replace the `bundle` task in `build-tools` with a deterministic sync of the CLI project into `skills/bootstrap-project/cli/` and `skills/orchestrate-feature/cli/` (sources, resources, assets, `toolchain.json`, `build-tools`, generated `project.yaml`/`module.yaml` with `jdk.version: 25`, byte-identical `kotlin`/`kotlin.bat`); verify by running `./kotlin do bundle` twice with no diff and `cmp` of the wrapper bytes against the root copies
- [x] 1.2 Add `bin/bootstrap` and `bin/bootstrap.cmd` per design D2; verify on an empty cache by setting `KOTLIN_CLI_BOOTSTRAP_CACHE_DIR`, `--shared-cache-dir` and the build cache to temp dirs, and check that `catalog --json` stdout parses as JSON, provisioning messages appear only on stderr, and a failing command's exit code is propagated
- [x] 1.3 Delete `skills/*/bin/bootstrap.jar` and update root `module.yaml` test settings; verify `git ls-files '*.jar'` is empty and `./kotlin test` passes

## 2. Java reuse and installation behavior

- [x] 2.1 Implement the Java probe from design D3 in both launchers; verify with tests using fake `java` scripts: an explicit `KOTLIN_CLI_JAVA_HOME` passes through, a version-25 `JAVA_HOME` is selected, version 21 and a failing stub are ignored
- [x] 2.2 Port the isolated-installation cases in `BootstrapTest`, `FeatureTest` and `SkillDiscoveryTest` to a read-only copied skill invoked from a foreign working directory; verify catalog, plan, generate, doctor, verify and `feature init` pass, no file under the copied skill changes, and the generated tree is byte-identical to the in-repo build
- [x] 2.3 Run two launcher invocations in parallel on a cold build cache; verify both succeed, or add a launcher directory lock with timeout and verify again
- [x] 2.4 Verify offline behavior: after one warm run, repeat catalog and generate with network disabled (for example, an unreachable `KOTLIN_CLI_DOWNLOAD_ROOT` and proxy) and record the result

## 3. Skills, docs and CI

- [x] 3.1 Update both `SKILL.md` files and references (`chat-wizard`, `verification`, `android-tools`, `ledger`, `toolchain`) from `java -jar …/bootstrap.jar` to `<skill>/bin/bootstrap`, including the first-run provisioning notice; verify `grep -rn "bootstrap.jar\|Java 17" skills` returns nothing
- [x] 3.2 Update README, INSTALL (proxy/mirror variables, Java reuse rules), BUILD, RELEASE, CLI-CONFIG, BOOTSTRAP-WIZARD, FEATURE-ORCHESTRATION and `AGENTS.md`; verify each documented launcher command runs as written
- [ ] 3.3 Switch `scripts/verify-*.sh` to accept a launcher path and update `.github/workflows/validate.yml`: drop the JAR diff check, add a check that the skill copies have no diff after bundle, and run the launcher test without `setup-java` on at least one OS; verify the workflow on the branch
- [x] 3.4 Bump plugin manifests and `CLI_VERSION` to 3.4.0, add the CHANGELOG entry, and verify `SkillDiscoveryTest` and manifest version alignment pass

## 4. Integration

- [ ] 4.1 Run `scripts/verify-matrix.sh` (android, shared, native) and `verify-platform-subsets.sh` with the launcher on macOS with Xcode, and record exact pass/fail/skip results
- [ ] 4.2 Install the plugin from the branch in Claude Code and Codex in a fresh workspace with no `JAVA_HOME`, run the wizard to generation, and record the first-run download behavior and timing

## Workflow follow-up

- Archive the change after review and a merged release.
