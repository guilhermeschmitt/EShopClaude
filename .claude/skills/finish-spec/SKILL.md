---
name: finish-spec
description: Steps 5–9 of the WOW.md cycle for one spec (Java service or storefront) — implement it on its feature branch following the approved plan, verify the build, tests and every Definition of Done line, mark it Done, then (after explicit confirmation) merge --no-ff into main, delete the branch and push main.
argument-hint: <service> <NN>
disable-model-invocation: true
allowed-tools: Read, Write, Edit, Glob, Bash(git:*), Bash(npm --prefix src run build), Bash(mvn:*), Bash(npm --prefix src/vue-front/storefront ci), Bash(npm --prefix src/vue-front/storefront run build), Bash(docker build src/java-back/*), Bash(docker ps:*), Bash(docker exec -i postgres-* sh -c *psql*), Bash(aspire run:*), Bash(aspire wait:*), Bash(aspire describe:*), Bash(aspire logs:*), Bash(curl -s * http://localhost:*), TaskStop, Agent(spec-compliance-reviewer), mcp__playwright__browser_navigate, mcp__playwright__browser_snapshot, mcp__playwright__browser_click, mcp__playwright__browser_wait_for, mcp__playwright__browser_take_screenshot, mcp__playwright__browser_evaluate, mcp__playwright__browser_console_messages, mcp__playwright__browser_network_requests, mcp__playwright__browser_close
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

Arguments: `$ARGUMENTS` → `<service> <NN>`, where `<service>` is `catalog`, `basket` or `ordering` (Java services) or `storefront` (the Vue app). Spec: `_agenticdev/microservices/<service>/<NN>-*.md`. Its branch is `feature/<service>-<NN>-<short-name>` (WOW.md rule; must match the Branch column in `_agenticdev/microservices/README.md`).

**Hard limits:** never push a feature branch — only `main`, after the merge. Never `git add -A` / `git add .`; stage explicit paths. Never resolve a merge conflict yourself. Any failed check or any "no" from me stops the skill before anything is merged.

## Steps

1. **Preflight.** Resolve the spec and its branch name. Stop and explain if:
   - the current branch is `main` → never run here; if `git branch --list <branch>` is empty, "run `/start-spec <service> <NN>` first", else "`git checkout <branch>`";
   - the current branch is any other branch than this spec's → name both and stop;
   - there is no approved plan for this spec in this session → "run `/start-spec` (or re-approve the plan) first".
2. **Develop.** Implement the spec following the approved plan: its Tasks in order, only its Files touched, every Explicit non-goal honoured, the conventions in `CLAUDE.md`. Commit on the branch as you go, staging files by explicit path, with messages naming the spec (`<service>/<NN>: …`).
3. **Verify.** Stop at the first failure and report it.
   - `npm --prefix src run build` (the AppHost build in CLAUDE.md) must pass.
   - The service's own build and tests:
     - **Java service:** `mvn -f src/java-back/<service>/pom.xml test` must pass, and `docker build src/java-back/<service>` must exit 0.
     - **Storefront:** `npm --prefix src/vue-front/storefront ci` and then `npm --prefix src/vue-front/storefront run build` must both exit 0. There's no `mvn test` or `docker build`, because no Java service is touched and the storefront runs through `addViteApp`, not a Dockerfile.
   - **Start the solution.** From `src/`, run `aspire run` in the background. Wait with `aspire wait <resource> --timeout 240 --non-interactive` for each resource the spec lists, every one must exit 0.
     - `aspire wait` exits `7` immediately while the AppHost is still starting. Retry until it returns something other than `7`, then judge that result.
     - pgAdmin's resource name is `pgadmin`. `aspire describe` doesn't list it as its own row, so wait on it by name.
     - Then `aspire describe --non-interactive` must show each as Running / Healthy. Read each service's URL from it, and the storefront's too, because the host port changes every run.
   - **Walk the spec's Definition of Done line by line** and run each check yourself, recording the exact command and its output:
     - HTTP requests: `curl -s` against `http://localhost:<port>` only.
     - Database: `docker exec -i <postgres-container> sh -c 'PGPASSWORD="$POSTGRES_PASSWORD" psql -U postgres -d <db>'` with the SQL on stdin. The container is the `postgres-*` one in `docker ps`, never another Postgres on the machine.
     - pgAdmin: Playwright on the pgAdmin port from `docker ps` (no login under Aspire): expand Servers → Databases → `<db>` → Schemas → public → Tables, select the table, **All Rows**, and read a screenshot saved under `.playwright-mcp/` (git-ignored).
     - Storefront in the browser: the Playwright MCP, on the storefront URL from `aspire describe`.
       - `browser_snapshot` for what renders: headings, links, cards.
       - `browser_console_messages` (level `debug`, `all: true`) for errors and warnings. A browser-generated 404 such as `/favicon.ico` still counts as a console error.
       - `browser_network_requests` (with `filter`) for which `/api/` calls were made.
       - `browser_evaluate` for `localStorage`, `location` and to check whether a click reloaded the page (set a `window` marker before clicking and read it afterwards).
       - Never commit Playwright test files: frontend tests are out of scope (PROJECT.md §9).
     - Resource console logs (for example, the storefront's `npm install`): `aspire logs <resource> --non-interactive`.
     - Dashboard-only checks (environment variables, traces): ask me — one line per check, with the exact thing to look at and the expected result — and wait.
   - If any code changes after a check ran (a fix found while verifying), commit it with a `<service>/<NN>: …` message, update the spec if it described the old behaviour, and re-run every check from the top against a fresh `aspire run`.
   - **Tear down.** Close the browser and stop the `aspire run` task (TaskStop) before marking Done, also when a check failed.
   - Any failed check or any "no" from me stops here.
4. **Compliance review.** Only after every Definition of Done line has passed. `git status --porcelain` must be empty (everything committed). Then launch the `spec-compliance-reviewer` agent with the service, the spec number `<NN>` and the branch name, and wait for its answer.
   - Show me its full output verbatim.
   - `FAIL` (or a refusal, such as a dirty working tree) stops the skill here, before Mark Done. Do not fix the findings yourself; report them and stop.
   - `PASS` continues to the next step.
5. **Mark Done.** Take `git rev-parse --short HEAD`. In the spec set `**Status:** Done (<sha>)`.
6. **Status table.** Set the same `Done (<sha>)` in this spec's row of `_agenticdev/microservices/README.md`.
7. **Docs commit.** `git add <spec path> _agenticdev/microservices/README.md`, then `git commit -m "Mark <service>/<NN>-<short-name> as Done"`.
8. **Confirm, then merge.** Show the branch, the target `main`, and `git log --oneline main..HEAD`, and ask me to confirm. Only after a "yes":
   - `git checkout main`
   - `git merge --no-ff <branch> -m "Merge <branch>"` — on any conflict, stop and report; do not resolve or abort on my behalf.
   - `git branch -d <branch>`
   - `git push origin main`
9. **Report.** The merge commit SHA, `git diff --stat <merge>^1 <merge>` (files changed), a table of every Definition of Done line with how it was verified (command run and its result / confirmed by me), and the compliance reviewer's verdict (`PASS`) with its Observations.
