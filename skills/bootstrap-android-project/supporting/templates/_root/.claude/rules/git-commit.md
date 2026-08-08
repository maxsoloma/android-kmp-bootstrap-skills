# Git Commit Rules

## Type = Branch Prefix (MANDATORY)

| Branch Prefix | Type |
|---------------|------|
| `feature/*`, `feat/*` | `feat` |
| `fix/*`, `bugfix/*` | `fix` |
| `refactor/*` | `refactor` |
| `chore/*` | `chore` |
| `docs/*` | `docs` |
| `test/*` | `test` |
| `perf/*` | `perf` |

**NEVER override type based on file content.**

## Format

```
[JIRA-TICKET] <type>: <description>
```

- Extract JIRA ticket (`[A-Z]{2,}-\d+`) from branch name if present
- Imperative mood, lowercase, no period, under 50 chars
- No vague messages ("update code", "fix bug", "changes")

## Commit Granularity

- Prefer **one logical change per commit** — easier to review, revert, and cherry-pick
- For multi-file features: group by logical unit (e.g., "add Repository interface + impl" as one commit)
- Never mix unrelated changes in a single commit (e.g., a bug fix + a new feature)