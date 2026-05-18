---
name: stage-and-suggest
description: Stage all unstaged changes and suggest a commit message based on the diff and last 10 commits. Does NOT commit.
---

# Stage and Suggest Commit Message

Stage every unstaged change, then recommend a single commit message with a short justification. Never create the commit.

## Steps

1. Run `git status` to show the current working tree state before staging.
2. Check for likely-sensitive files in the unstaged changes (e.g. `.env`, `*.pem`, `*credentials*`, `*secret*`). If any are found, warn the user by name and do not stage them — stage everything else using `git add` on individual paths.
3. If no sensitive files are found, run `git add -A` to stage all changes.
4. Run `git diff --cached` to inspect exactly what was staged.
5. Run `git log --oneline -10` to understand the repo's commit message style and conventions.
6. Analyze the staged diff to understand the intent behind the changes — focus on *why*, not *what*.
7. Suggest exactly one commit message that:
   - Matches the tone and style of recent commits in this repo
   - Starts with an imperative verb (Add, Fix, Refactor, Remove, etc.)
   - Is concise (under 72 characters)
   - Describes the *why* or *what*, not implementation details
8. Follow the suggestion with a 2–3 line justification explaining why this message fits the changes and the repo's conventions.
9. Do not run `git commit` under any circumstances.
