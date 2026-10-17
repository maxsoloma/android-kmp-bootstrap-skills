# Release 3.0.0

The release contains one portable `bootstrap-project` skill, its standalone JAR,
and Codex/Claude plugin manifests. The release JAR is built by `./kotlin do bundle`;
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

## Submission packages

The GitHub release provides the CLI JAR and a ZIP containing the skill, icons,
manifests, README, license, third-party notices and privacy information. Codex
requires local execution, explicitly declared by `requires_local_executor: true`.
There is no MCP server to register and no reviewer login to this project's service.

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
