---
name: suggest-commit
description: Analyze unstaged/staged changes and recent commit history to suggest commit message names for the next commit.
---

# Suggest Commit Message

Use this skill when the user wants commit message suggestions based on their current changes and recent commit history.

## Parameters

- `n` — number of recent commits to inspect for style reference (default: 10)

## Steps

1. Run `git add -A` to stage all changes (tracked modifications, deletions, and new files).
2. Run `git diff HEAD` to see all current staged and unstaged changes.
3. Run `git log --oneline -<n>` (use the provided `n`, or 10 if not specified) to understand the project's commit message style and conventions.
4. Run `git status` to identify which files are affected.
5. Analyze the diff to understand what changed and why — focus on intent, not just file names.
6. Generate exactly 2 commit message candidates that:
   - Match the tone and style of recent commits in this repo
   - Start with an imperative verb (Add, Fix, Refactor, Remove, Extract, etc.)
   - Are concise (under 72 characters)
   - Describe the *why* or *what* — not implementation details
   - Are ordered from most to least specific/recommended
7. Present the suggestions as a numbered list with a one-line rationale for each.
8. Do not create the commit — only suggest names.
