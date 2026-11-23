---
name: start-spec
description: Steps 2–4 of the WOW.md cycle for one spec — preflight checks, create the local feature branch from main, commit the spec as the branch's first commit, then plan the implementation in plan mode and stop for approval. Never implements, merges or pushes.
argument-hint: <service> <NN>
disable-model-invocation: true
allowed-tools: Read, Glob, Bash(git status:*), Bash(git branch:*), Bash(git checkout:*), Bash(git add:*), Bash(git commit:*)
---

# /start-spec

Repository state when this skill was invoked:
- Current branch: !`git branch --show-current`
- Working tree:
```
!`git status --porcelain`
```

Arguments: `$ARGUMENTS` → `<service> <NN>` (`catalog`, `basket` or `ordering`; two digits).

**Hard limits:** no push (feature branches stay local), no merge, no `rm`, no edits to any file. Only the git commands in `allowed-tools`.

## Steps

1. **Resolve.** Glob `_agenticdev/microservices/<service>/<NN>-*.md`. Exactly one match, or stop. Read it, `_agenticdev/WOW.md` and `_agenticdev/microservices/README.md`.
2. **Preflight.** If any check fails, change nothing: name the failed check, show the evidence, and say what to do next.
   - Current branch is not `main` → "switch to `main`" (and finish or park the other branch).
   - Working tree has any change other than the spec file itself (untracked `??` or modified) → "commit, stash or discard those first".
   - A "Depends on" entry is not `Done` in the README status table → name it and say "finish that spec first".
   - Any section is missing, empty, an italic template placeholder (`_…_`), or contains `TODO(decide)`. Required sections: Goal, Context, Depends on, Scope, Explicit non-goals, Data model, Tasks, Files touched, Definition of Done, References → "run `/new-spec <service> <NN>` first".
   - Spec **Status** is `Done` (in the file or the README) → "nothing to start".
   - The derived branch (step 3) already exists (`git branch --list <name>`) → "resume or delete it by hand".
3. **Branch name.** Apply the WOW.md rule `feature/<service>-<NN>-<short-name>`, where `<short-name>` is the spec filename without `NN-` and `.md`. It must equal the README's Branch column; if not, stop and report the mismatch. Never invent a name or ask for one.
4. **Branch.** `git checkout -b <branch> main`.
5. **Spec commit.** `git add <spec path>` only, then `git commit -m "Add spec <service>/<NN>-<short-name> before implementation"`. If the spec is already committed and unchanged, make it the first commit anyway with `git commit --allow-empty` using the same message. Show `git status --porcelain` afterwards (must be empty for the spec).
6. **Plan.** Enter plan mode. Read the spec and only what it references (PROJECT.md §, PLAN.md step, the files in Files touched). Write a plan that follows the spec's Tasks in order, touches only its Files touched, respects every Explicit non-goal, and ends with how each Definition of Done check will be run.
7. **Stop.** Present the plan for approval. Do not implement anything, even after approval — implementation is step 5 of the cycle and starts on its own.
