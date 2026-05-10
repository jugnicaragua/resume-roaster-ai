# Resume Roaster AI — Agent Guide

Demo project for a JUG Nicaragua talk: **"Java en la era de la IA: Creando aplicaciones de GenAI con LangChain4J"**.  
A Spring Boot REST API that accepts a CV/resume and returns AI-generated roast feedback with configurable personalities.

## Stack

- **Java 21 / Spring Boot 4** — REST API (no UI; Java backend only)
- **LangChain4J** — LLM integration and prompt chaining
- **MLflow Prompt Registry** — versioned prompt management; personalities are defined via system prompts pulled from MLflow
- **PII Redaction pipeline** — applied before sending resumes to the LLM:
  - Stanford CoreNLP — statistical NER (names, orgs, locations)
  - DJL + ONNX — deep-learning NER inference
  - RegEx — rule-based patterns (emails, phones, IDs)

## Project layout

```
src/main/java/ni/jug/resumeroaster/
├── controller/   REST endpoints
├── service/      business logic (roasting, PII redaction)
├── model/        request/response DTOs
└── config/       LangChain4J / MLflow beans
```

## Key conventions

- Lombok for boilerplate; use `@Value` for immutable DTOs, `@Data` sparingly.
- Configuration via `application.yaml`; secrets in env vars, never hardcoded.
- LLM calls go through LangChain4J `AiServices`; do not call the LLM client directly.
- PII redaction must run before any LLM call — enforce this at the service layer.

## Build & run

```bash
./gradlew bootRun          # start API on :8080
./gradlew test             # run tests
```

Requires MLflow tracking server and a configured LLM API key in the environment.
