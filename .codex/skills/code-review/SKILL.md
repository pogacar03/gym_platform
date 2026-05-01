---
name: code-review
description: Use when the user says codereview, code review, review current changes, or asks to review the current branch diff against main. Generate the current workspace diff relative to main, apply the project's Java review rules, and output Critical/Warning/Info findings with file and line references. Do not modify code.
---

# Code Review Skill

This skill performs a read-only review of the current workspace changes relative to `main`.

## When to use

Use this skill when the user says any of the following, or clearly means the same thing:

- `codereview`
- `code review`
- `review current changes`
- `review dev against main`
- `review this diff`

## Required behavior

1. Treat this as a **read-only analysis task**.
2. **Do not edit code**, do not stage files, do not commit, and do not auto-fix anything.
3. Review the current workspace relative to `main`, including local uncommitted changes.
4. Focus on the diff first. Read surrounding source files only when needed to understand impact.
5. Apply the standards in [`/Users/yu/Documents/New project/docs/code-review-rules.md`](/Users/yu/Documents/New%20project/docs/code-review-rules.md).

## Workflow

1. Identify the current branch:
   - `git branch --show-current`
2. Generate the review diff against `main`:
   - `git diff --stat main`
   - `git diff main`
3. If the diff is empty:
   - clearly say there are no current changes to review relative to `main`
   - stop there
4. If the diff is large:
   - prioritize critical path code, security, transaction boundaries, SQL / DB behavior, concurrency, null safety, and authorization
5. Inspect only the additional source context needed to understand changed methods and call chains

## Review scope priorities

1. Functional correctness and side effects
2. Security and permission boundaries
3. Database behavior, transactions, and query efficiency
4. Null safety, exception handling, and Spring / Java best practices
5. Maintainability, readability, and test coverage

## Output format

Always structure the response like this:

### 评审概览

- 变更意图: ...
- 影响范围: ...
- 整体评分: X/5

### 🔴 Critical 问题 (必须修复)

List only real blocking issues. If none, write `未发现`.

### 🟡 Warning 问题 (建议修复)

List medium-severity issues. If none, write `未发现`.

### 🔵 Info 优化建议

List low-severity improvements or testing gaps. If none, write `未发现`.

### 总结

Give a short overall assessment and note any residual risk.

## Finding format

For each finding, include:

- 问题类型: `Critical` / `Warning` / `Info`
- 位置: `绝对路径或明确文件路径:行号`
- 问题描述: concise technical explanation
- 影响: why it matters
- 建议: concrete fix direction without rewriting code

## Guardrails

- Do not review the whole repository if the user asked for `codereview`; review the diff first
- Do not invent line numbers if you have not opened the file context
- Do not give style-only feedback while missing correctness or safety risks
- If there are no findings, explicitly say `未发现阻塞性问题` and mention any remaining testing or validation gaps
