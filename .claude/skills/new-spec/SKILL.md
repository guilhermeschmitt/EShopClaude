---
name: new-spec
description: Create or complete one microservice spec under _agenticdev/microservices for the spec-driven workflow in _agenticdev/WOW.md. Drafts from PROJECT.md and PLAN.md, asks the open design questions, and writes Markdown only — no branch, no commit, no code.
argument-hint: <service> <NN> [slug] ["title"]
disable-model-invocation: true
allowed-tools: Read, Write, Edit, Glob
---

# /new-spec

Arguments: `$ARGUMENTS` → `<service> <NN> [slug] ["title"]`. `<service>` is `catalog`, `basket` or `ordering`; `<NN>` is two digits.

**Hard limits:** only create or edit Markdown under `_agenticdev/`. Never create a branch, commit, or touch anything in `src/`. Never overwrite an existing spec file. Leave **Status** at `Not started` — a status that runs ahead of reality is worse than none.

**Section order** (the template's sections plus the four extra ones that finished specs use, as in `catalog/01-product-schema.md`):
Goal · Context · Depends on · Scope · Explicit non-goals · Data model · Tasks · Files touched · Definition of Done · References.

## Steps

1. **Resolve.** Glob `_agenticdev/microservices/<service>/<NN>-*.md`. Read `_agenticdev/WOW.md`, `_agenticdev/PROJECT.md`, `_agenticdev/PLAN.md` and `CLAUDE.md` for context.
2. **If no file exists:** `slug` is required (stop and ask if missing). Create `<NN>-<slug>.md` from `_agenticdev/templates/spec-template.md`, heading `# <service> NN — <title or slug>`, `**Status:** Not started`. Draft every section from the matching PROJECT.md (§) and PLAN.md (step) parts; mark anything you cannot source as `TODO(decide): …`.
3. **If the file exists:** read it and report, section by section, `complete` / `stub` / `too vague to build from` (with one line why). Ignore `slug`/`title` arguments. Continue to fill the gaps — edit, never recreate.
4. **Status table.** Read `_agenticdev/microservices/README.md`. Fill **Depends on** from it (format `<service>/NN`, or "Nothing."). If the spec has no row, add one in service/number order: link, branch `feature/<service>-<NN>-<slug>`, depends on, `Not started`.
5. **Ask before writing.** For every design decision the documents do not settle, ask me — one question per decision, with the options you see and your recommendation. Always cover, when relevant: field names and their types; how the database schema is created (Flyway migration, seed, `ddl-auto`); how the service gets its connection string / env vars from the AppHost. Wait for answers.
6. **Write the spec.** Fill **Context** (current state of the code and AppHost this builds on), **Data model** (tables/keys/entities/DTOs, or "None."), **Tasks** (numbered, by file/layer), **Files touched** (exact paths, created vs modified — one service plus its AppHost registration only). Sharpen every **Definition of Done** line into a runnable check: exact command or HTTP request and its expected result (status code, body shape, row count, test name). Include the CLAUDE.md pre-commit checks (`npm run build` in `src/`, `mvn -f src/java-back/<service>/pom.xml test`, `aspire run` all healthy).
7. **Report.** Print the spec path, whether the README row was added, and a bullet list of everything still to be decided by hand (remaining `TODO(decide)` items and unanswered questions).
