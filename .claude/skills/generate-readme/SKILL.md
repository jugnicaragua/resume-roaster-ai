---
name: generate-readme
description: Generate or update the README.md for the current project by inspecting project structure, build files, and existing documentation.
disable-model-invocation: true
allowed-tools: Read Bash Glob Grep
---

# Generate / Update README

Produce or refresh the project's `README.md`. If one already exists, update it in-place — preserve sections that are accurate and only rewrite what is stale or missing.

## Steps

1. Detect the project type by checking for these files (read whichever exist):
   - `pom.xml`, `build.gradle`, `build.gradle.kts` → Java/Kotlin (Maven or Gradle)
   - `pyproject.toml`, `setup.py`, `requirements.txt` → Python
   - `package.json` → Node / JavaScript / TypeScript
   - `Cargo.toml` → Rust
   - `go.mod` → Go

2. Read the build file(s) found above to extract: project name, description, version, dependencies, and any declared plugins.

3. Run `find . -maxdepth 4 -name "*.java" -o -name "*.py" -o -name "*.kt" -o -name "*.ts" -o -name "*.go" -o -name "*.rs" | grep -v "test\|Test\|__pycache__\|node_modules\|build\|target\|\.gradle" | sort | head -80` to get a representative picture of the source layout.

4. If a `README.md` already exists, read it fully. Note which sections are accurate and which are stale or absent.

5. Read the following if they exist, for additional context:
   - `.claude/AGENTS.md` or `.claude/CLAUDE.md`
   - `CONTRIBUTING.md`
   - `docker-compose.yml` or `Dockerfile`

6. Draft the README using this structure (omit sections that genuinely don't apply):

   ```
   # <Project Name>

   One-sentence description of what this project does and who it is for.

   ## Features
   Bullet list of the main capabilities.

   ## Tech Stack
   Table or bullet list: language, framework, key libraries, infra.

   ## Project Structure
   Short annotated directory tree of the source layout.

   ## Prerequisites
   What must be installed or configured before running (JDK version, Python version, env vars, external services).

   ## Getting Started
   Minimal steps to clone, configure, and run locally.

   ## API / Usage
   If it is a REST API: example endpoints with curl. If a library: usage code snippet. If a CLI: command examples.

   ## Configuration
   Key environment variables or config files with descriptions.

   ## Running Tests
   Single command to execute the test suite.

   ## Contributing
   Brief contribution guidelines or link to CONTRIBUTING.md.

   ## License
   License line.
   ```

7. Rules for writing:
   - Use plain, direct English — no marketing language.
   - Code blocks must be fenced with the correct language tag.
   - Environment variable names in `code` formatting.
   - Do not invent features, endpoints, or config keys that are not visible in the source.
   - If information is genuinely unknown (e.g. license), leave a `<!-- TODO: fill in -->` placeholder.

8. If `README.md` already exists: apply targeted edits using the Edit tool — do not rewrite sections that are already accurate.
   If it does not exist: create it with the Write tool.

9. Print a one-line summary of what was added, updated, or left unchanged.
