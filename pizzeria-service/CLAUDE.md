# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this
repository.

## Repository Overview

This repository contains a full-stack pizzeria platform with:

- **Backend**: `./pizzeria-service/` - Spring Boot WebFlux reactive microservice
- **Frontend**: `./pizzeria-front-end/` - React + TypeScript single-page application

The backend provides REST APIs for user management, menu browsing, pizza customization, dietary
suitability checks, ratings, and feedback.

**Multi-Tenancy**: The service supports multiple pizzerias. Public endpoints include
`{pizzeriaCode}` in the URL path for tenant resolution. Authenticated endpoints resolve the pizzeria
from the Bearer token.

**Configured Pizzeria**: The active pizzeria is "Ramona" with code `ramonamalmo` (ID:
`00000000-0000-0000-0000-000000000002`). The default pizzeria is deactivated.

## Build and Development Commands

```bash
# Full build with tests and JaCoCo coverage report
mvn clean verify

# Run the service locally (requires PostgreSQL via docker-compose)
# Service runs on port 9900
docker-compose up -d
mvn spring-boot:run

# Auto-format Java sources (required before commits)
mvn spotless:apply

# Run OWASP dependency security audit
mvn dependency-check:check

# Build container image (requires registry credentials)
mvn -Pprod jib:build
```

All commands should be run from the `pizzeria-service/` directory.

## Architecture

### Technology Stack

- **Framework**: Spring Boot 3.x with WebFlux (reactive, non-blocking)
- **Database**: PostgreSQL 16 with R2DBC (reactive driver)
- **Schema Management**: Liquibase (YAML changelogs)
- **Security**: Spring Security with custom token-based authentication
- **API Docs**: SpringDoc OpenAPI (Swagger UI at `/swagger-ui.html`)

### Layered Structure

```
src/main/java/com/pizzeriaservice/
├── api/dto/                    # Request/Response DTOs (API contracts)
└── service/
    ├── config/                 # Security, OpenAPI, WebClient, MessageSource
    ├── controller/             # REST endpoint handlers
    ├── service/                # Business logic
    ├── domain/                 # Domain models (records) and enums
    ├── repository/             # Abstract repository interfaces
    ├── repository/r2dbc/       # R2DBC adapter implementations
    ├── menu/                   # Menu entities and Spring Data R2DBC repos
    ├── user/                   # User entities and repos
    ├── pizzascore/             # PizzaScore entities and repos
    ├── feedback/               # Feedback entities and repos
    └── support/                # Cross-cutting: auth, time, validation, exceptions
```

### Key Patterns

**Repository Adapter Pattern**: Abstract interfaces (`UserRepository`, `FeedbackRepository`,
`PizzaScoreRepository`) in `service/repository/` with R2DBC implementations in
`service/repository/r2dbc/`. In-memory stubs exist in `src/test/java` for unit testing.

**Domain Models**: Immutable Java records with `@Builder(toBuilder=true)` for safe modification.

**Reactive Streams**: All I/O uses `Mono<T>` (single value) or `Flux<T>` (multiple values). Use
`StepVerifier` for testing.

**Time Abstraction**: `TimeProvider` interface enables deterministic testing via `TestTimeProvider`.

### Database Schema

Menu data is loaded from CSV files via Liquibase changelogs in `db/changelog/data/`. Key tables:

- `pizzerias` - Multi-tenant root (id, code, name, active)
- `menu_sections`, `menu_items`, `menu_item_ingredients` - Menu structure
- `menu_ingredient_facts` - Dietary type, allergens, spice level
- `pizza_customisations` - Extra toppings/modifications
- `users`, `user_preferred_ingredients` - User accounts and preferences
- `feedback`, `pizza_scores` - User feedback and ratings

### API Structure

REST endpoints follow noun-based routing with `/api/v1/` prefix:

**Public endpoints** (include `{pizzeriaCode}` in path):

- `GET /api/v1/pizzerias/{pizzeriaCode}` - Pizzeria info (name, code, currency, timezone)
- `POST /api/v1/pizzerias/{pizzeriaCode}/users/register` - Register new user
- `POST /api/v1/pizzerias/{pizzeriaCode}/users/verify-email` - Verify email with token
- `POST /api/v1/pizzerias/{pizzeriaCode}/users/login` - Login, returns Bearer token
- `POST /api/v1/pizzerias/{pizzeriaCode}/users/forgot-password` - Request password reset token
- `POST /api/v1/pizzerias/{pizzeriaCode}/users/reset-password` - Reset password with token
- `GET /api/v1/pizzerias/{pizzeriaCode}/pizzas` - List pizzas
- `GET /api/v1/pizzerias/{pizzeriaCode}/pizzas/{pizzaId}` - Pizza details
- `GET /api/v1/pizzerias/{pizzeriaCode}/menu` - Full menu with sections and items

**Authenticated endpoints** (Bearer token in `Authorization` header):

- `POST /api/v1/users/logout` - Logout (invalidate token)
- `GET/PATCH/DELETE /api/v1/users/me` - User profile management
- `GET/PUT /api/v1/users/me/diet` - Dietary preferences (enum: `VEGAN`, `VEGETARIAN`, `CARNIVORE`,
  `NONE`)
- `GET/POST/DELETE /api/v1/users/me/preferences/ingredients/preferred` - Preferred ingredients
- `POST /api/v1/pizzas/suitability` - Check pizza suitability for user diet
- `POST/GET /api/v1/pizza-scores` - Pizza ratings (score 1-5)
- `POST /api/v1/feedback/service` - Service feedback (rating 1-5)

### Authentication Flow

1. **Register** → User created with `emailVerified=false`, returns verification token
2. **Verify Email** → Validates token, sets `emailVerified=true`
3. **Login** → Email + password → Bearer token generated and stored in-memory
4. **Use Token** → Include `Authorization: Bearer <token>` header
5. **Logout** → Token removed from active tokens map
6. **Forgot Password** → Email → password reset token generated (returned in response for dev mode)
7. **Reset Password** → Token + new password → password updated, token consumed (single-use)

**Note**: Auth tokens and password reset tokens are stored in-memory (`ConcurrentHashMap`). For
multi-instance deployments, migrate to Redis or stateless JWT.

## Testing

```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=UserServiceTest

# Run a specific test method
mvn test -Dtest=UserServiceTest#shouldRegisterUserSuccessfully
```

Unit tests use in-memory repository implementations and `TestTimeProvider`. Integration tests (in
`src/integrationTest`) use Testcontainers with PostgreSQL.

Test naming: `<TypeUnderTest>Test` with methods `should<Expectation>()`.

## Code Style

- Format with Spotless (`mvn spotless:apply`) before commits
- 4-space indentation, ~120 char line width
- Class naming: `*Controller`, `*Service`, `*Repository`, `*Request`, `*Response`
- Conventional Commits: `feat:`, `fix:`, `chore:` prefixes

## Development Guidelines

**Testing Requirement**: When implementing new features or fixing bugs, always add relevant tests.
This includes:

- Unit tests for new service methods and controllers
- Integration tests for new endpoints when appropriate
- Test updates when modifying existing functionality
- Both backend (Java/JUnit) and frontend (Vitest/React Testing Library) tests as applicable

## API Validation Rules

Key DTO constraints enforced by Jakarta validation:

- `name`: max 100 characters
- `password`: min 8 characters
- `score`/`rating`: integer 1-5
- `diet`: enum `VEGAN`, `VEGETARIAN`, `CARNIVORE`, `NONE`

## Configuration

Environment variables (with defaults in `application.yaml`):

- `SPRING_R2DBC_URL` - R2DBC connection string
- `SPRING_R2DBC_USERNAME/PASSWORD` - Database credentials
- `SPRING_LIQUIBASE_URL/USERNAME/PASSWORD` - Liquibase JDBC connection
- `SPRING_LIQUIBASE_ENABLED` - Toggle migrations

i18n messages in `messages.properties` (English) and `messages_sv.properties` (Swedish).

---

## Frontend Application

The frontend is located in `pizzeria-front-end/` and is a React + TypeScript single-page
application.

### IDE Recommendation

**Use VS Code** for the frontend. The project includes `.vscode/settings.json` configured for
Tailwind CSS. Install the **Tailwind CSS IntelliSense** extension for:

- Autocomplete for Tailwind classes
- Hover previews showing the CSS
- Linting for class conflicts

IntelliJ/WebStorm users will see `@tailwind` at-rule warnings. To fix: **Settings → Editor →
Inspections → CSS → Invalid CSS → Uncheck "Unknown at-rule"**.

### Technology Stack

- **Framework**: React 18 with TypeScript
- **Build Tool**: Vite
- **Styling**: Tailwind CSS v3
- **State Management**: React Query v5 (server state), React Context (auth)
- **Routing**: React Router DOM v6
- **HTTP Client**: Axios
- **i18n**: react-i18next (English and Swedish)

### Build and Development Commands

```bash
# Install dependencies
npm install

# Start dev server (http://localhost:5173)
npm run dev

# Type check
npm run typecheck

# Production build
npm run build

# Run tests
npm run test

# Lint
npm run lint
```

All commands should be run from the `pizzeria-front-end/` directory.

### Project Structure

```
src/
├── api/                    # API client modules (auth, menu, pizzas, etc.)
├── components/
│   ├── ui/                 # Reusable UI (Button, Input, Card, Alert, etc.)
│   └── layout/             # Layout components (Header, Layout, Container)
├── features/               # Feature-based pages
│   ├── auth/               # Login, Register, VerifyEmail, ForgotPassword, ResetPassword, AuthProvider
│   ├── menu/               # MenuPage
│   ├── pizzas/             # PizzaListPage, PizzaDetailPage
│   ├── profile/            # ProfilePage
│   ├── preferences/        # PreferencesPage (diet, ingredients)
│   ├── scores/             # ScoresPage (pizza ratings)
│   ├── feedback/           # FeedbackPage
│   ├── home/               # HomePage
│   └── error/              # NotFoundPage
├── hooks/                  # Custom hooks (useAuth, usePizzeriaCode, useTranslateKey)
├── i18n/                   # i18next config and locale files (en, sv)
├── routes/                 # Routing (AppRoutes, ProtectedRoute, PizzeriaProvider)
├── types/                  # TypeScript interfaces matching API contract
├── styles/                 # Tailwind CSS entry point
├── App.tsx                 # Root component with providers
└── main.tsx                # Entry point
```

### Multi-Tenancy Routing

All routes are prefixed with `/:pizzeriaCode`:

| Route                            | Page               | Auth Required |
|----------------------------------|--------------------|---------------|
| `/:pizzeriaCode`                 | Home               | No            |
| `/:pizzeriaCode/menu`            | Full menu          | No            |
| `/:pizzeriaCode/pizzas`          | Pizza list         | No            |
| `/:pizzeriaCode/pizzas/:id`      | Pizza detail       | No            |
| `/:pizzeriaCode/login`           | Login              | No            |
| `/:pizzeriaCode/register`        | Register           | No            |
| `/:pizzeriaCode/verify-email`    | Email verification | No            |
| `/:pizzeriaCode/forgot-password` | Forgot password    | No            |
| `/:pizzeriaCode/reset-password`  | Reset password     | No            |
| `/:pizzeriaCode/profile`         | User profile       | Yes           |
| `/:pizzeriaCode/preferences`     | Diet & ingredients | Yes           |
| `/:pizzeriaCode/scores`          | Pizza ratings      | Yes           |
| `/:pizzeriaCode/feedback`        | Service feedback   | Yes           |

The root path `/` redirects to `/ramonamalmo` by default.

### API Integration

- **Public endpoints**: Include `pizzeriaCode` in URL path (e.g., `/api/v1/pizzerias/{code}/menu`)
- **Authenticated endpoints**: Use Bearer token only, no `pizzeriaCode` needed
- Auth tokens are stored per-pizzeria in localStorage: `pizzeria-{code}-auth-token`
- **PizzeriaProvider context**: Use `usePizzeriaContext()` to access `pizzeriaCode`, `pizzeriaName`,
  and `isLoading`. The provider fetches pizzeria info on mount.

### Configuration

Environment variables (optional):

- `VITE_API_BASE_URL` - API base URL (default: `/api/v1`)
- `VITE_API_PROXY_TARGET` - Dev server proxy target (default: `http://localhost:9900`)

### Running Frontend and Backend Together

```bash
# Terminal 1: Start backend (from pizzeria-service/)
docker-compose up -d
mvn spring-boot:run

# Terminal 2: Start frontend (from pizzeria-front-end/)
npm run dev
```

Frontend runs on `http://localhost:5173`, proxies `/api` requests to backend on port 9900.

### Translation Keys

The API returns translation keys (e.g., `translation.key.disc.pizza.margarita`). These are mapped
in:

- `src/i18n/locales/en/menu.json`
- `src/i18n/locales/sv/menu.json`

The `useTranslateKey` hook handles key translation with fallback to formatted key names.

### Known Limitations / TODOs

- No ingredients list page (API endpoint exists but page not implemented)
- Email verification flow shows token on screen (for development/testing)
- Password reset flow shows token on screen (for development/testing, like email verification)
- No real-time updates (no WebSocket/SSE)

---

## Git Workflow

Both repositories are hosted on GitHub:

- **Backend**: https://github.com/marcusfromsweden/pizzeria-service (`./pizzeria-service/`)
- **Frontend**: https://github.com/marcusfromsweden/pizzeria-front-end (`./pizzeria-front-end/`)

### Available Commands

Type any of these commands to trigger automated workflows:

| Command | Description | Aliases |
|---------|-------------|---------|
| `commands` | List all available commands | `show commands` |
| `changes` | Quick summary of pending changes with suggested commit messages | `changes?` |
| `changes detailed` | Detailed analysis with full diff output per repo | `what to commit` |
| `commit backend` | Analyze and commit `./pizzeria-service/` changes | `commit back-end` |
| `commit frontend` | Analyze and commit `./pizzeria-front-end/` changes | `commit front-end` |
| `commit all` | Analyze and commit all changes across both repos | `commit changes` |

### Command Details

#### `changes` / `changes?`

Quick summary of all pending changes with suggested commit messages.

**Process:**
1. Run `git status` and `git diff` in both `./pizzeria-service/` and `./pizzeria-front-end/`
2. Provide summary of what changed (by repo/file)
3. Suggest 3 commit messages per repo (different detail levels):
   - Option 1: Concise/brief
   - Option 2: Medium detail
   - Option 3: Comprehensive/detailed

#### `changes detailed` / `what to commit`

Detailed analysis with full diff output for all pending changes.

**Process:**
1. Run `git status` and `git diff` in both repos
2. Provide comprehensive summary organized by repo:
   - **./pizzeria-service/** — Changed Java, SQL, config files
   - **./pizzeria-front-end/** — Changed React/TypeScript files
3. For each repo show files modified with diff summary

#### `commit backend` / `commit back-end`

Analyze and commit changes in `./pizzeria-service/` only.

**Process:**
1. Run `mvn spotless:apply` to format code
2. Analyze changes with `git diff`
3. Suggest 3 commit messages (different detail levels)
4. User selects option (reply with "1", "2", or "3")
5. Stage, commit, and push `./pizzeria-service/` changes

#### `commit frontend` / `commit front-end`

Analyze and commit changes in `./pizzeria-front-end/` only.

**Process:**
1. Analyze changes with `git diff`
2. Suggest 3 commit messages (different detail levels)
3. User selects option (reply with "1", "2", or "3")
4. Stage, commit, and push `./pizzeria-front-end/` changes

#### `commit all` / `commit changes`

Analyze and commit changes across both repos.

**Process:**
1. Run `mvn spotless:apply` in `./pizzeria-service/` if it has changes
2. Analyze all changes with `git diff` in both repos
3. For each repo with changes, suggest 3 commit messages
4. User selects options
5. Stage, commit, and push each repo separately (they have separate git histories)

---

## Feature Roadmap

This section tracks potential features for the pizzeria platform. Features marked with ✅ are
implemented, ⚠️ are partially implemented, and ❌ are not yet started.

### Grundläggande funktioner (Basic Features)

| Feature                                  | Status | Notes                                      |
|------------------------------------------|--------|--------------------------------------------|
| Online-meny (med priser)                 | ✅      | Full menu with sections, items, and prices |
| Bilder på rätter                         | ❌      | No image support yet                       |
| Ingredienslista per rätt                 | ✅      | `menu_item_ingredients` table              |
| Allergener (gluten, laktos, nötter m.m.) | ✅      | `allergen_tags` in `menu_ingredient_facts` |
| Kontaktuppgifter (telefon, e-post)       | ❌      | Not implemented                            |
| Karta (Google Maps / OpenStreetMap)      | ❌      | Not implemented                            |
| Öppettider                               | ❌      | Not implemented                            |
| Mobilanpassad / responsiv design         | ✅      | Tailwind CSS responsive                    |
| Språkstöd (svenska, engelska)            | ✅      | i18next with `en` and `sv` locales         |

### Beställning & betalning (Ordering & Payment)

| Feature                                                   | Status | Notes              |
|-----------------------------------------------------------|--------|--------------------|
| Onlinebeställning via hemsidan                            | ❌      | No ordering system |
| Avhämtning (pickup)                                       | ❌      | -                  |
| Hemleverans                                               | ❌      | -                  |
| Val av leveranstid / upphämtningstid                      | ❌      | -                  |
| Digital betalning (Kort, Swish, Klarna, Apple/Google Pay) | ❌      | -                  |
| Betalning vid leverans                                    | ❌      | -                  |
| Orderbekräftelse (e-post / SMS)                           | ❌      | -                  |
| Orderstatus i realtid                                     | ❌      | -                  |

### Meny & navigering (Menu & Navigation)

| Feature                                         | Status | Notes                                                                      |
|-------------------------------------------------|--------|----------------------------------------------------------------------------|
| Kategorisering av rätter                        | ✅      | `menu_sections` table                                                      |
| Sökfunktion                                     | ❌      | Not implemented                                                            |
| Filter på ingredienser                          | ❌      | Backend has data, no UI filter                                             |
| Filter på allergener                            | ❌      | Backend has data, no UI filter                                             |
| Vegetariskt / veganskt filter                   | ⚠️     | `dietary_type` exists, `POST /pizzas/suitability` API exists, no UI filter |
| Anpassningsbara rätter (extra topping, bortval) | ⚠️     | `pizza_customisations` table exists, no ordering UI                        |
| Favoriter / spara favoritbeställning            | ⚠️     | Preferred ingredients exist, no favorite orders                            |

### Kundupplevelse (Customer Experience)

| Feature                                    | Status | Notes                                                 |
|--------------------------------------------|--------|-------------------------------------------------------|
| Kundkonto / inloggning                     | ✅      | Full auth flow with email verification                |
| Orderhistorik                              | ❌      | No orders yet                                         |
| Snabb återbeställning                      | ❌      | No orders yet                                         |
| Kundrecensioner / testimonials             | ⚠️     | `pizza_scores` for ratings, no public reviews display |
| FAQ-sektion                                | ❌      | Not implemented                                       |
| Tillgänglighetsanpassning (WCAG-grundkrav) | ❌      | Not audited                                           |

### Marknadsföring & försäljning (Marketing & Sales)

| Feature                         | Status | Notes |
|---------------------------------|--------|-------|
| Kampanjer och rabatter          | ❌      | -     |
| Rabattkoder / kuponger          | ❌      | -     |
| Paket- och menydeals            | ❌      | -     |
| Lojalitetsprogram / poängsystem | ❌      | -     |
| Presentkort (digitala)          | ❌      | -     |
| Nyhetsbrev (e-post)             | ❌      | -     |

### Automatisering & smarta funktioner (Automation & Smart Features)

| Feature                                     | Status | Notes |
|---------------------------------------------|--------|-------|
| Realtidsstatus (öppet/stängt/snart stänger) | ❌      | -     |
| Chatbot / AI-orderhjälp                     | ❌      | -     |
| Rekommendationer baserat på tidigare köp    | ❌      | -     |
| Cross-sell / upsell (dryck, tillbehör)      | ❌      | -     |
| Push-notiser (via PWA)                      | ❌      | -     |

### Teknik & integration (Tech & Integration)

| Feature                             | Status | Notes                                                |
|-------------------------------------|--------|------------------------------------------------------|
| Integration med kassasystem (POS)   | ❌      | -                                                    |
| Integration med leveranssystem      | ❌      | -                                                    |
| API-stöd för externa system         | ✅      | REST API with OpenAPI docs                           |
| Google Analytics / tracking         | ❌      | -                                                    |
| Cookie-hantering (GDPR)             | ❌      | -                                                    |
| SSL / HTTPS                         | ⚠️     | Handled at deployment/infrastructure level           |
| CMS för enkel uppdatering av meny   | ⚠️     | Admin price import/export exists, no full CMS        |
| Prestandaoptimering (snabb laddtid) | ⚠️     | Reactive backend, Vite frontend, not fully optimized |

### Extra / nice-to-have

| Feature                                   | Status | Notes |
|-------------------------------------------|--------|-------|
| PDF-meny (nedladdningsbar)                | ❌      | -     |
| Utskriftsvänlig meny                      | ❌      | -     |
| Social media-integration (Instagram-feed) | ❌      | -     |
| Event- eller cateringbokning              | ❌      | -     |
| Blogg / nyhetssektion                     | ❌      | -     |

### Admin Features

| Feature                                    | Status | Notes                                 |
|--------------------------------------------|--------|---------------------------------------|
| Admin price management (CSV export/import) | ✅      | `AdminPriceController`, admin UI page |
| User management                            | ❌      | -                                     |
| Order management                           | ❌      | No orders yet                         |
| Analytics dashboard                        | ❌      | -                                     |
