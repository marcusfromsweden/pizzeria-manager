# Repository Guidelines

## Project Structure & Module Organization
- `pizzeria-service/` contains the reactive Spring Boot API. Domain logic lives under `src/main/java/com/pizzeriaservice/service`, with controller adapters in `controller/` and DTOs in `api/dto/`. Unit tests reside in `src/test/java`, and the scaffolded integration test suite is prepared under `src/integrationTest/` for Testcontainers scenarios.
- `pizzeria-front-end/` is a Vite + React client aligned with the API contracts. Feature folders inside `src/features/` mirror backend capabilities (pizzas, users, preferences, feedback). Shared infrastructure sits in `src/api/`, `src/components/`, and `src/routes/`.
- `front-end-repos-for-reference/` holds historical UI patterns; treat this directory as read-only inspiration rather than an active project.

## Build, Test, and Development Commands
- Backend: `mvn clean verify` for a full build, `mvn spring-boot:run` for local development, and `mvn spotless:apply` before committing format changes.
- Front-end: `npm install` (or `yarn install`) inside `pizzeria-front-end/`, then `npm run dev` for Vite, `npm run build` for production bundles, `npm run lint`, and `npm run test` for Vitest.
- Docker or Kubernetes manifests from prior work can be adapted, but are not wired into automation yet.

## Coding Style & Naming Conventions
- Java follows the defaults enforced by Spotless and the parent Sandvik Spring Boot starter. Use clear package names (`com.pizzeriaservice.service.<layer>`) and PascalCase for records and enums.
- TypeScript is strictly typed (`strict` mode) and linted via ESLint + Prettier. Co-locate feature components, keep filenames in PascalCase for components (`PizzaListPage.tsx`) and camelCase for utilities.
- Prefer descriptive DTO names mirroring backend records so the API surface stays symmetrical across layers.

## Testing Guidelines
- Write JUnit 5 tests under `src/test/java/**`. Integration tests should live in `src/integrationTest/java/**` and rely on the existing Testcontainers dependencies; use the `@Testcontainers` lifecycle to provision PostgreSQL.
- Front-end components should include Vitest + Testing Library coverage under `src/features/__tests__/`. Name tests `*.test.tsx` and render via `MemoryRouter` + providers. Stub network calls when asserting component behaviour.
- Maintain meaningful assertions—avoid snapshot-only coverage for complex flows.

## Commit & Pull Request Guidelines
- No historical commit pattern exists; adopt Conventional Commits (`feat:`, `fix:`, `chore:`) to ease changelog generation.
- Each pull request should describe scope, validation steps (`mvn clean verify`, `npm run test`), and reference tracking tickets. Include screenshots or terminal output when UI or API behaviour changes.
- Keep PRs focused: backend, front-end, and infra edits should be split unless they form an inseparable feature slice.

## Security & Configuration Tips
- Never commit secrets. Use environment variables for database, OAuth, and `VITE_API_BASE_URL` configuration. Local `.env` files belong in `.gitignore`.
- Enable TLS proxies when exposing the API externally and keep dependency scanning (`mvn dependency-check:aggregate`) in regular rotation.
