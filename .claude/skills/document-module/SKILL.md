---
name: document-module
description: Audit and update code-level documentation (JavaDoc, Python docstrings, etc.) for a specific module or package. Leaves adequate docs untouched; only writes or rewrites what is missing or misleading.
disable-model-invocation: true
argument-hint: <module-path>
arguments: [module]
allowed-tools: Read Bash Glob Grep Edit
---

# Document Module

Audit and improve inline code documentation for the module at `$module`. Never touch markdown files. Only write or rewrite docs that are absent, incomplete, or misleading — leave everything else as is.

## Parameters

- `module` — relative path to the package or directory to document (e.g. `src/main/java/ni/jug/resumeroaster/service`). Required.

## Steps

1. Validate the path: confirm `$module` exists. If not, stop and tell the user.

2. Detect the language by inspecting file extensions inside `$module`:
   - `.java` → JavaDoc (`/** ... */` on classes, public methods, public fields)
   - `.py` → Python docstrings (Google style: `Args:`, `Returns:`, `Raises:`)
   - `.kt` → KDoc (`/** ... */`, same rules as JavaDoc)
   - `.ts` / `.js` → JSDoc (`/** ... */`)
   - For mixed modules, handle each language by its own rules.

3. List all source files in `$module` (non-recursive into sub-packages — stay within the given path depth):
   ```
   find $module -maxdepth 1 -type f \( -name "*.java" -o -name "*.py" -o -name "*.kt" -o -name "*.ts" \)
   ```

4. For each file, read it fully and evaluate every documentable element against the criteria below. Build an internal triage list: **OK** (leave alone), **Missing** (no doc at all), or **Inadequate** (doc exists but is wrong, trivially useless, or incomplete).

   **JavaDoc / KDoc adequacy criteria** — a doc block is OK only if it:
   - Exists on every `public` and `protected` class, interface, enum, record, and their non-trivial members.
   - Has a meaningful first sentence (not just the method name restated).
   - Documents every `@param` and `@return` with a non-trivial description (not "the foo", "a string").
   - Has `@throws` for every checked exception declared in the signature.
   - Private and package-private members: document only if the logic is non-obvious.

   **Python docstring adequacy criteria** — a docstring is OK only if it:
   - Exists on every public module, class, and function/method.
   - Has a one-line summary that describes purpose, not implementation.
   - Has an `Args:` block with every parameter described (skip `self`/`cls`).
   - Has a `Returns:` block unless the return type is `None`.
   - Has a `Raises:` block for every exception the function can raise intentionally.
   - Trivial properties (`@property` returning a stored field) may be skipped.

5. Process only **Missing** and **Inadequate** items. For each:
   - Write the new doc block immediately above the element, following the language's standard format.
   - Use the element's name, signature, and body to infer correct descriptions — do not guess at behavior not visible in the code.
   - Keep descriptions factual and concise; one sentence per param/return is usually enough.
   - Apply edits with the Edit tool (one call per file — batch all changes to the same file into a single operation where possible).

6. Do not:
   - Add docs to test classes or test methods.
   - Change any code logic, formatting, or import order.
   - Add `@author`, `@version`, or `@since` tags unless they already appear in the file.
   - Generate placeholder text like "TODO: document this".

7. After all edits, print a table:

   | File | OK (unchanged) | Added | Rewritten |
   |------|---------------|-------|-----------|

   Then one sentence on anything that was skipped and why (e.g. "Skipped `FooTest.java` — test file").
