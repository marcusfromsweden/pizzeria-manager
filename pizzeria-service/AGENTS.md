# Repository Guidelines

## Project Structure & Module Organization
Source lives under `src/main/java/com/pizzeriaservice`, split into `api` (DTOs), `service` (business logic), `repository` (adapters, including in-memory stubs), and `support` (cross-cutting helpers). HTTP entry points sit in `service/controller`, while application configuration (security, OpenAPI, messaging) is in `service/config`. Runtime settings (`application.yaml`, `messages.properties`) live in `src/main/resources`. Unit tests mirror the production package layout in `src/test/java`, and `src/integrationTest` is kept for future Testcontainers suites so unit tests stay fast.

## Build, Test, and Development Commands
- `mvn clean verify` — full build with unit tests, Spotless check, and a JaCoCo report in `target/site/jacoco`.
- `mvn spring-boot:run` — launch the WebFlux service locally against the default profile.
- `mvn spotless:apply` — auto-format Java sources using the shared PlatformX style.
- `mvn dependency-check:check` — run the OWASP dependency audit before release branches.
- `mvn -Pprod jib:build` — publish a container image when registry credentials are configured.

## Coding Style & Naming Conventions
Java is formatted via Spotless; keep four-space indentation and wrap near 120 chars. Classes follow Spring layering: `*Controller`, `*Service`, `*Repository`, with DTOs named `*Request`/`*Response`. Enums live beside their domain or API context. Package names stay lowercase and singular (`com.pizzeriaservice.service.controller`). Keep REST endpoints noun-based and route-versioned.

## Testing Guidelines
Write JUnit 5 tests with AssertJ and Reactor Test utilities for reactive flows. Name test classes `<TypeUnderTest>Test` and methods `should<Expectation>()`. Unit tests belong in `src/test/java`, while slower database or WebClient scenarios should target `src/integrationTest/java` with Testcontainers. Keep JaCoCo coverage at or above the current baseline; confirm via `target/site/jacoco/index.html`.

## Commit & Pull Request Guidelines
Adopt Conventional Commit prefixes (`feat:`, `fix:`, `chore:`) followed by an imperative summary, e.g., `feat: add pizza suitability endpoint`. Group related changes per commit and keep diffs focused. Pull requests must outline intent, list validation steps (`mvn clean verify`), reference Jira or Azure Boards IDs, and include payload examples when API contracts change. Flag configuration or security-sensitive updates so reviewers can prioritize them.

## Security & Configuration Tips
Never commit secrets; load external credentials through environment variables consumed by `application.yaml`. Regenerate tokens if they appear in logs or PRs. When adding outbound clients, wire them through `WebClientConfig` so common timeouts and tracing remain consistent, and document any new ports or scopes in the PR description.
