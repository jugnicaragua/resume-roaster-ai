---
name: safe-check
description: Scan staged changes for secrets, sensitive data, bad practices, and code issues before committing.
---

# Safe Check

Inspect staged changes and report any findings. Do not modify files — only report.

## Steps

1. Check for likely-sensitive files in the unstaged changes (e.g. `.env`, `*.pem`, `*credentials*`, `*secret*`). Warn the user by name and do not stage them — stage everything else using `git add` on individual paths. If no sensitive files are found, run `git add -A` to stage all changes.
2. Run `git diff --cached` to get the staged diff.
3. If nothing is staged, tell the user and stop.
4. Scan the diff for the following, reporting each finding with the file name and line:

   **Secrets & credentials**
   - Hardcoded passwords, tokens, API keys, private keys, connection strings
   - Patterns like `password =`, `secret =`, `api_key =`, `token =`, `Bearer `, `-----BEGIN`)
   - `.env` files or files named `*secret*`, `*credential*`, `*private*`

   **Sensitive data**
   - Personal information: emails, phone numbers, SSNs, credit card numbers
   - Internal URLs, IPs, hostnames that look non-public

   **Bad practices**
   - Commented-out code blocks (more than 2 consecutive commented lines)
   - `print()` / `console.log()` debug statements left in non-notebook, non-test code
   - `TODO` / `FIXME` / `HACK` comments introduced in this diff
   - Hardcoded file paths (e.g. `/Users/`, `C:\Users\`)
   - Disabled security checks (`--no-verify`, `verify=False`, `ssl=False`, `checksum=False`)

   **Code issues**
   - Syntax that looks obviously broken (unclosed brackets, malformed YAML/JSON in config files)
   - Test files staged alongside production code with failing assertions or skipped tests (`@skip`, `xit(`, `xdescribe(`)

5. Present the output in this order:
   a. **Summary** — one line at the very top: overall verdict (clean / warnings / blockers) and a count of findings per category.
   b. Findings grouped by category. For each finding include: file, line number (if visible in the diff), and a one-line explanation of the risk.
6. Do not commit, modify, or suggest fixes — findings only.
