---
name: gradle-version-catalog-migration
description: Migrate Spring Boot Gradle projects to Gradle Version Catalogs using OpenRewrite.
---

# Gradle Version Catalog Migration

Use this skill when migrating a Spring Boot Gradle project from inline dependency/plugin declarations to `gradle/libs.versions.toml`.

## Steps

1. Inspect `build.gradle`, `build.gradle.kts`, `settings.gradle`, or `settings.gradle.kts`.
2. Add the OpenRewrite Gradle plugin temporarily if not already present.
3. Configure the recipe:

   `org.openrewrite.gradle.MigrateDependenciesToVersionCatalog`

4. Run:

   `./gradlew rewriteRun`

5. Inspect generated or updated `gradle/libs.versions.toml`.
6. Update build files to use:
    - `libs.*` for libraries
    - `alias(libs.plugins.*)` for external plugins
7. Keep Gradle core plugins like `java` as direct plugins.
8. Avoid adding explicit versions to Spring Boot starters when Spring Boot dependency management controls them.
9. Run:

   `./gradlew clean build`

10. Remove temporary OpenRewrite setup unless it should remain.
11. Summarize the diff and any manual fixes.
