# Installing

These skills follow the [Agent Skills](https://agentskills.io) open standard, so they work
unmodified in any agent that implements it. What differs between agents is only *where* the
files go.

## The short version

Most agents — Codex, GitHub Copilot, Gemini CLI, Cursor, Firebender — read a shared
`~/.agents/skills/` directory. One copy covers all of them:

```bash
git clone https://github.com/maxsoloma/android-kmp-bootstrap-skills.git
mkdir -p ~/.agents/skills
cp -R android-kmp-bootstrap-skills/skills/* ~/.agents/skills/
```

**Claude Code is the exception**: it does not read `~/.agents/skills/`. Use the plugin
install below, or copy into `~/.claude/skills/`.

After copying, restart the agent if the new skills don't show up — most detect changes live,
but not all do.

## Claude Code

Installing as a plugin is preferred: you get a pinned version and `/plugin update`.

```
/plugin marketplace add maxsoloma/android-kmp-bootstrap-skills
/plugin install bootstrap-projects@android-kmp-bootstrap-skills
```

If the install summary says `Run /reload-plugins to activate.`, run that.

Plugin skills are namespaced, so both of these work:

```
/bootstrap-projects:bootstrap-android-project
/bootstrap-android-project
```

The bare name resolves unless another installed skill already claims it.

To update later: `/plugin marketplace update` then `/plugin update bootstrap-projects`.
To remove: `/plugin uninstall bootstrap-projects`.

<details>
<summary>Without the plugin system</summary>

```bash
mkdir -p ~/.claude/skills
cp -R android-kmp-bootstrap-skills/skills/* ~/.claude/skills/
```

Or project-scoped, committed to the repo so your whole team gets them:

```bash
mkdir -p .claude/skills
cp -R /path/to/android-kmp-bootstrap-skills/skills/* .claude/skills/
```

</details>

Docs: [code.claude.com/docs/en/skills](https://code.claude.com/docs/en/skills)

## Codex

```bash
mkdir -p ~/.agents/skills
cp -R android-kmp-bootstrap-skills/skills/* ~/.agents/skills/
```

Codex also reads `.agents/skills/` from the current directory and from the repository root,
so committing the skills into a repo makes them available to everyone working in it.

Docs: [learn.chatgpt.com/docs/build-skills](https://learn.chatgpt.com/docs/build-skills)

## GitHub Copilot

Personal — either location works:

```bash
mkdir -p ~/.copilot/skills          # or ~/.agents/skills
cp -R android-kmp-bootstrap-skills/skills/* ~/.copilot/skills/
```

Repository-scoped — Copilot reads `.github/skills/`, `.claude/skills/`, and `.agents/skills/`:

```bash
mkdir -p .github/skills
cp -R /path/to/android-kmp-bootstrap-skills/skills/* .github/skills/
```

This covers the Copilot CLI, the Copilot app, agent mode in VS Code and JetBrains IDEs, the
cloud agent, and Copilot code review.

Docs: [docs.github.com — About agent skills](https://docs.github.com/en/copilot/concepts/agents/about-agent-skills)

## Gemini CLI

Gemini CLI can install straight from a git URL:

```bash
gemini skills install https://github.com/maxsoloma/android-kmp-bootstrap-skills.git \
  --path skills/bootstrap-android-project --consent

gemini skills install https://github.com/maxsoloma/android-kmp-bootstrap-skills.git \
  --path skills/bootstrap-kmp-project --consent
```

`--path` selects a skill directory inside the repository; `--consent` skips the interactive
security confirmation; add `--scope workspace` to install into the project instead of your
user profile. If your Gemini CLI version handles these flags differently, fall back to
copying into `~/.gemini/skills/` or `~/.agents/skills/`.

Docs: [geminicli.com/docs/cli/skills](https://geminicli.com/docs/cli/skills/)

## Cursor

```bash
mkdir -p ~/.agents/skills          # or ~/.cursor/skills
cp -R android-kmp-bootstrap-skills/skills/* ~/.agents/skills/
```

Project-scoped: `.agents/skills/` or `.cursor/skills/`. Cursor also reads `.claude/skills/`
and `.codex/skills/` for compatibility, so an existing install elsewhere is likely already
visible to it.

Docs: [cursor.com/docs/context/skills](https://cursor.com/docs/context/skills)

## Firebender

Firebender is Android-native, which makes it a natural fit for these skills.

```bash
mkdir -p ~/.firebender/skills      # or ~/.agents/skills
cp -R android-kmp-bootstrap-skills/skills/* ~/.firebender/skills/
```

Team-scoped: `.firebender/skills/`. Firebender additionally reads `~/.claude/skills/`,
`~/.codex/skills/`, `~/.cursor/skills/`, `~/.goose/skills/`, and `~/.agents/skills/`.

Docs: [docs.firebender.com/multi-agent/skills](https://docs.firebender.com/multi-agent/skills)

## Other agents

Around 45 products implement the standard, including Junie, OpenCode, Amp, Goose, Factory,
Kiro, Roo Code, VS Code, Letta, and Trae. The current list with each vendor's own setup
instructions is at [agentskills.io/clients](https://agentskills.io/clients).

The pattern is the same everywhere: find the agent's skills directory and copy the two
directories from `skills/` into it.

## Verifying the install

Ask the agent to list its available skills, or type `/` and look for
`bootstrap-android-project` and `bootstrap-kmp-project` in the completion menu.

If they don't appear:

1. Confirm the layout is `<skills-dir>/bootstrap-android-project/SKILL.md` — one directory
   per skill, `SKILL.md` directly inside it. A common mistake is copying the `skills/`
   directory itself, which nests everything one level too deep.
2. Restart the agent.
3. Check that `supporting/` came along with `SKILL.md`. Without it the skill loads but
   cannot generate anything.

## Uninstalling

Claude Code plugin: `/plugin uninstall bootstrap-projects`.

Copied install: delete the two directories from wherever you put them.

```bash
rm -rf ~/.agents/skills/bootstrap-android-project ~/.agents/skills/bootstrap-kmp-project
```
