# Release 3.3.0

The release contains portable `bootstrap-project` and `orchestrate-feature`
skills, identical standalone JARs in each skill and Codex/Claude plugin manifests. The release JAR is built by `./kotlin do bundle`;
run `./kotlin test` and both strict Claude manifest validations before pushing.

## GitHub installation

From a directory outside the repository:

```sh
codex plugin marketplace add maxsoloma/android-kmp-bootstrap-skills
codex plugin add bootstrap-projects@android-kmp-bootstrap-skills
claude plugin marketplace add maxsoloma/android-kmp-bootstrap-skills
claude plugin install bootstrap-projects@android-kmp-bootstrap-skills
```

Native marketplace sources must identify GitHub, not a local checkout. Verify the
cached commit and JAR bytes, start fresh discovery and generate/build a temporary
app using the installed copy. Never interpret local-cache reuse as a clean remote
installation. The existing development marketplace may need to be replaced with
the Git source for this check; leave unrelated marketplaces/plugins untouched.

## Release and update verification

Before publication, confirm the exact remote main/tag and review the complete
release diff, including any earlier local commits not yet pushed. Keep manifest
versions, CLI version, changelog and documentation aligned. Tag the verified
commit as `v3.3.0`; do not move an existing published tag.

The distribution ZIP includes both complete skill folders, their references and
JARs, all user documentation, icons, manifests and license/privacy notices.
Publish `bootstrap.jar`, the ZIP and SHA256SUMS with the GitHub release.

After pushing, follow [upgrade instructions](INSTALL.md#upgrade-an-existing-installation)
for the existing installation. Verify the Git-backed marketplace's actual commit,
installed version, both skills and documentation, and JAR bytes against the tag.
Use the installed copy outside the source repository for a generation/build
smoke and feature-journal initialization; retain the real command results.
A GitHub release and a refreshed repository marketplace do not assert approval
or publication in the universal plugin directory.

## Submission packages

The GitHub release provides the CLI JAR and a ZIP containing both complete skill folders, icons,
manifests, README, license, third-party notices and privacy information. Codex
requires local execution, explicitly declared by `requires_local_executor: true`.
The plugin hosts no MCP service. Koin recipes include optional Kotzilla MCP
client settings and require the user's own OAuth login for live access; the
local generation flow requires no reviewer login.

Publish through the repository's own marketplaces after the remote install passes.
Public store listings use separate review systems:

- [OpenAI plugin portal](https://platform.openai.com/plugins): upload the release
  ZIP, resolve scans, complete verified publisher information and review details,
  submit, then publish the approved version. See the official
  [submission guide](https://developers.openai.com/plugins/deploy/submission) and
  [local-execution requirements](https://developers.openai.com/plugins/deploy/desktop-only).
- [Anthropic developer portal](https://claude.ai/directory/manage): submit the
  GitHub repository at its root, validate the selected commit, complete listing
  and data-handling information, then submit and follow the review/publish state.
  See the official [submission guide](https://claude.com/docs/plugins/submit).

An uploaded draft, successful validation, a submission for review and a published
listing are different outcomes. Record the actual portal status and URL; never
claim a listing is public before the portal confirms publication. Account access,
publisher verification and legally binding acknowledgements may require the owner.
