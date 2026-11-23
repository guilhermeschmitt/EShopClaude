---
name: auto-spec
description: Run one spec end to end by following /new-spec, /start-spec and /finish-spec in place, with a human gate after the spec and after the plan. Does none of their steps itself.
argument-hint: <service> <NN>
disable-model-invocation: true
allowed-tools: Read, Write, Edit, Glob, Bash(git:*), Bash(npm --prefix src run build), Bash(mvn:*)
---

# /auto-spec

Arguments: `$ARGUMENTS` → `<service> <NN>`.

This skill only sequences. The three skills stay manual-only, so instead of invoking them, read each one's `SKILL.md` and follow it in place. That file is the single source of truth for its procedure; never repeat, shortcut or reimplement its steps here.

**Following a skill in place:**
- First run every `` !`…` `` command at the top of its `SKILL.md`, right then (not reused from an earlier skill — the state changes between them), and use the output as its repository state.
- Use `<service> <NN>` as its `$ARGUMENTS`.
- Respect its Hard limits and its own `allowed-tools` as rules, even though this skill's tools are broader.

## Steps

1. Follow `.claude/skills/new-spec/SKILL.md`.
2. **Gate one.** Show me the completed spec and ask: "Is this spec buildable?" Stop on anything but yes.
3. Follow `.claude/skills/start-spec/SKILL.md`.
4. **Gate two.** Let me approve the implementation plan. Stop on anything but approval.
5. Follow `.claude/skills/finish-spec/SKILL.md`.
6. **Refusals.** If any of the three refuses or stops, stop immediately. Report which skill refused and its reason, verbatim. Never work around a refusal (no manual git, no edits to make a check pass, no skipping a gate).
7. Print the close-out summary from `/finish-spec` as it gave it.

## When not to use this
Run the three skills by hand instead when:
- the spec introduces a pattern this repository has not built before;
- the spec touches more than one service;
- the spec's Definition of Done contains a check nobody can actually run.
