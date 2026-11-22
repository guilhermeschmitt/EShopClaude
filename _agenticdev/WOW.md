# EShopClaude — Way of Working

The team and Claude Code both follow this file on this repository.
Behavior lives in [PROJECT.md](PROJECT.md), build order in [PLAN.md](PLAN.md), and commands and conventions in `CLAUDE.md`.

## Principles
- One spec is one sub-feature, one feature branch and one merge to `main`.
- A spec changes exactly one microservice and its own AppHost registration.
- `main` always builds and runs. Every merge leaves a working solution.
- Every spec is self-contained, so a fresh session only needs to read that one file.

## Branches and merges
- One spec is one feature branch from `main`, named `feature/<service>-<NN>-<short-name>`.
  - Example: `feature/catalog-01-read-api`.
- The spec is the first commit on the branch, before any code.
- Merge into `main` with `git merge --no-ff`, then delete the branch.
- Feature branches stay local and are never pushed.
- No direct commits to `main`.

## Folder layout
```
_agenticdev/
├─ PROJECT.md, PLAN.md, WOW.md
├─ templates/
│  └─ spec-template.md
└─ microservices/
   ├─ README.md              status table for all specs
   └─ <service>/
      └─ NN-<short-name>.md  one spec
```
- Specs are numbered per service (`01`, `02`, …) and are never renumbered.
- Every spec starts as a copy of [templates/spec-template.md](templates/spec-template.md) and keeps its sections.
- `microservices/README.md` holds the status table for all specs.

## The cycle, per spec
1. **Pick** the next spec: the lowest-numbered spec in its service whose dependencies are all Done.
2. **Branch:** create `feature/<service>-<NN>-<short-name>` from an up-to-date `main`, and commit the spec file as the first commit.
3. **Fresh session:** start a new Claude Code session and point it at the spec file only.
4. **Plan:** enter plan mode, produce the plan, and a human approves it before any code.
5. **Implement** in auto mode, following the approved plan.
6. **Verify manually:** run the pre-commit checks below and walk the spec's Definition of Done.
7. **Commit and merge:** commit on the branch, then on `main` run `git merge --no-ff <branch>`.
8. **Mark Done** in the spec file and in the status table of `microservices/README.md`.
9. **Delete** the feature branch (`git branch -d <branch>`).

## Ordering rules
- Specs run in number order inside a service.
- A spec does not start until every spec it depends on is merged to `main`.

## Session hygiene
- One spec, one fresh session. Clear the context between specs (`/clear`).
- Plan mode before implementing, and a human approves the plan.
- Before any commit:
  - the pre-commit checks in `CLAUDE.md` pass (AppHost build, `mvn test` for the touched service, `aspire run` with every resource healthy);
  - the spec's Definition of Done passes.
