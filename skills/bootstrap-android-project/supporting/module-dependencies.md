# Optional Module Dependency Graph

Each `core-optional/<x>/_manifest.yml` declares direct `requires:` on OTHER optional
modules. The skill computes the transitive closure of the user's selection and copies
every module in it.

## Closure algorithm

```
selected = user picks
closure  = empty set
worklist = selected
while worklist not empty:
    m = worklist.pop()
    if m in closure: continue
    closure.add(m)
    for dep in manifest(m).requires:
        if dep not in closure: worklist.push(dep)
generate every module in closure
```

Cycles are impossible by construction (the table below is a DAG); if a future module
introduces one, the closure loop's `if m in closure` guard still terminates.

## Direct dependencies (mirror of the _manifest.yml files)

| Module | requires |
|--------|----------|
| config | — |
| analytics | config |
| remote-config | config, analytics |
| database | — |
| sync | — |
| auth | config, sync, analytics |
| billing | remote-config, analytics |
| image | config |
| permissions | — (consumes core-required `design`, always present) |
| dma | config, analytics |

## Worked example (acceptance criterion)

User selects only `billing`:
- `billing` → requires `remote-config`, `analytics`
- `remote-config` → requires `config`, `analytics`
- `analytics` → requires `config`
- closure = { billing, remote-config, analytics, config }

So `remote-config` and `analytics` (and `config`) are present even though the user picked
only `billing`.

Note: `requires:` lists ONLY optional-on-optional edges. Dependencies on core-required
modules (`di`, `domain`, `network`, `design`, `utils`) are always satisfied because those
modules are copied unconditionally.
