---
name: finish-spec
description: Steps 5–9 of the WOW.md cycle for one spec — implement it on its feature branch following the approved plan, verify the build, tests and every Definition of Done line, mark it Done, then (after explicit confirmation) merge --no-ff into main, delete the branch and push main.
argument-hint: <service> <NN>
disable-model-invocation: true
allowed-tools: Read, Write, Edit, Glob, Bash(git:*), Bash(npm --prefix src run build), Bash(mvn:*)
---

# /finish-spec

Repository state when this skill was invoked:
- Current branch: !`git branch --show-current`
- Commits so far (main..HEAD):
```
!`git log --oneline main..HEAD`
```
- Working tree:
```
!`git status --porcelain`
```

Arguments: `$ARGUMENTS` → `<service> <NN>`. Spec: `_agenticdev/microservices/<service>/<NN>-*.md`. Its branch is `feature/<service>-<NN>-<short-name>` (WOW.md rule; must match the Branch column in `_agenticdev/microservices/README.md`).

**Hard limits:** never push a feature branch — only `main`, after the merge. Never `git add -A` / `git add .`; stage explicit paths. Never resolve a merge conflict yourself. Any failed check or any "no" from me stops the skill before anything is merged.

## Steps

1. **Preflight.** Resolve the spec and its branch name. Stop and explain if:
   - the current branch is `main` → never run here; if `git branch --list <branch>` is empty, "run `/start-spec <service> <NN>` first", else "`git checkout <branch>`";
   - the current branch is any other branch than this spec's → name both and stop;
   - there is no approved plan for this spec in this session → "run `/start-spec` (or re-approve the plan) first".
2. **Develop.** Implement the spec following the approved plan: its Tasks in order, only its Files touched, every Explicit non-goal honoured, the conventions in `CLAUDE.md`. Commit on the branch as you go, staging files by explicit path, with messages naming the spec (`<service>/<NN>: …`).
3. **Verify.** Stop at the first failure and report it.
   - `npm --prefix src run build` (the AppHost build in CLAUDE.md) must pass.
   - `mvn -f src/java-back/<service>/pom.xml test` must pass.
   - Walk the spec's **Definition of Done** line by line. Run every check you can run yourself. For each one you cannot (`aspire run` all healthy, dashboard env vars, a table or row in pgAdmin, a request in a `.http` file), ask me — one line per check, with the exact thing to look at and the expected result — and wait. Any "no" stops here.
4. **Mark Done.** Take `git rev-parse --short HEAD`. In the spec set `**Status:** Done (<sha>)`.
5. **Status table.** Set the same `Done (<sha>)` in this spec's row of `_agenticdev/microservices/README.md`.
6. **Docs commit.** `git add <spec path> _agenticdev/microservices/README.md`, then `git commit -m "Mark <service>/<NN>-<short-name> as Done"`.
7. **Confirm, then merge.** Show the branch, the target `main`, and `git log --oneline main..HEAD`, and ask me to confirm. Only after a "yes":
   - `git checkout main`
   - `git merge --no-ff <branch> -m "Merge <branch>"` — on any conflict, stop and report; do not resolve or abort on my behalf.
   - `git branch -d <branch>`
   - `git push origin main`
8. **Report.** The merge commit SHA, `git diff --stat <merge>^1 <merge>` (files changed), and a table of every Definition of Done line with how it was verified (command run and its result / confirmed by me).
