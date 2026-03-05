# Codebase Analysis — Pizzeria Platform

> Generated 2026-02-11. Covers both `pizzeria-service/` (backend) and `pizzeria-front-end/`
> (frontend).

---

## Table of Contents

- [1. High-Level Overview](#1-high-level-overview)
- [2. Backend — pizzeria-service](#2-backend--pizzeria-service)
  - [2.1 Technology Stack](#21-technology-stack)
  - [2.2 Project Structure](#22-project-structure)
  - [2.3 Controllers & Endpoints](#23-controllers--endpoints)
  - [2.4 Services](#24-services)
  - [2.5 Repositories](#25-repositories)
  - [2.6 Domain Models & Enums](#26-domain-models--enums)
  - [2.7 Database Schema & Liquibase](#27-database-schema--liquibase)
  - [2.8 Security & Authentication](#28-security--authentication)
  - [2.9 Error Handling & Validation](#29-error-handling--validation)
  - [2.10 Internationalization (i18n)](#210-internationalization-i18n)
  - [2.11 Configuration](#211-configuration)
  - [2.12 Testing](#212-testing)
  - [2.13 Dependencies (pom.xml)](#213-dependencies-pomxml)
  - [2.14 Build Pipeline & Plugins](#214-build-pipeline--plugins)
  - [2.15 Menu Data Seeding](#215-menu-data-seeding)
- [3. Frontend — pizzeria-front-end](#3-frontend--pizzeria-front-end)
  - [3.1 Technology Stack](#31-technology-stack)
  - [3.2 Project Structure](#32-project-structure)
  - [3.3 Pages & Features](#33-pages--features)
  - [3.4 API Modules](#34-api-modules)
  - [3.5 Components](#35-components)
  - [3.6 Routing](#36-routing)
  - [3.7 State Management](#37-state-management)
  - [3.8 Authentication Flow](#38-authentication-flow)
  - [3.9 TypeScript Types](#39-typescript-types)
  - [3.10 Internationalization](#310-internationalization)
  - [3.11 Styling (Tailwind)](#311-styling-tailwind)
  - [3.12 Configuration](#312-configuration)
  - [3.13 Testing](#313-testing)
  - [3.14 Custom Hooks](#314-custom-hooks)
  - [3.15 Error Handling](#315-error-handling)
  - [3.16 Dependencies (package.json)](#316-dependencies-packagejson)
- [4. API Contract Summary](#4-api-contract-summary)
- [5. Statistics](#5-statistics)
- [6. Areas for Improvement](#6-areas-for-improvement)

---

## 1. High-Level Overview

The repository is a full-stack pizzeria platform:

| Layer | Directory | Tech |
|-------|-----------|------|
| Backend | `pizzeria-service/` | Spring Boot 3.x WebFlux, PostgreSQL 16, R2DBC |
| Frontend | `pizzeria-front-end/` | React 18, TypeScript, Vite, Tailwind CSS |

**Multi-tenancy**: Every public endpoint includes `{pizzeriaCode}` in its URL path. Authenticated
endpoints resolve the tenant from the JWT. The active pizzeria is **Ramona** (`ramonamalmo`, ID
`00000000-0000-0000-0000-000000000002`).

**Key capabilities**: user registration with email verification, JWT auth, menu browsing, dietary
suitability checks, pizza ratings, service feedback with admin replies, ordering (pickup/delivery),
delivery address management, admin price CSV import/export, and admin feedback management.

---

## 2. Backend — pizzeria-service

### 2.1 Technology Stack

- **Framework**: Spring Boot 3.4.10 with WebFlux (reactive, non-blocking)
- **Database**: PostgreSQL 16 with R2DBC (reactive driver)
- **Schema management**: Liquibase (YAML changelogs)
- **Security**: Spring Security + custom JWT (HS256, jjwt 0.12.6)
- **Validation**: Jakarta Bean Validation
- **API docs**: SpringDoc OpenAPI (Swagger UI at `/swagger-ui.html`)
- **Password hashing**: DelegatingPasswordEncoder (bcrypt default)
- **Build**: Maven, Spotless (Google Java Format), JaCoCo, Jib, OWASP Dependency-Check

### 2.2 Project Structure

```
pizzeria-service/src/main/java/com/pizzeriaservice/
├── api/dto/                            # Request / Response DTOs
└── service/
    ├── config/                         # Security, OpenAPI, WebClient, MessageSource
    ├── controller/                     # REST endpoint handlers (12 controllers)
    ├── service/                        # Business logic (10 services)
    ├── domain/                         # Immutable records + enums
    ├── repository/                     # Abstract repository interfaces
    ├── repository/r2dbc/               # R2DBC adapter implementations
    ├── menu/                           # Menu entities & Spring Data repos
    ├── user/                           # User entities & repos
    ├── pizzascore/                     # PizzaScore entities & repos
    ├── feedback/                       # Feedback entities & repos
    └── support/                        # Auth, time, validation, exceptions
```

### 2.3 Controllers & Endpoints

| Controller | Prefix | Auth | Responsibility |
|------------|--------|------|----------------|
| `UserController` | `/api/v1/users` | Yes | Profile CRUD, logout |
| `UserRegistrationController` | `/api/v1/pizzerias/{code}/users` | No | Register, verify email, login |
| `UserDietController` | `/api/v1/users/me/diet` | Yes | Dietary preferences (GET/PUT) |
| `UserPreferredIngredientController` | `/api/v1/users/me/preferences/ingredients` | Yes | Preferred ingredients CRUD |
| `DeliveryAddressController` | `/api/v1/users/me/addresses` | Yes | Delivery address CRUD, set default |
| `PizzeriaController` | `/api/v1/pizzerias/{code}` | No | Pizzeria info |
| `MenuController` | `/api/v1/pizzerias/{code}/menu` | No | Full menu with sections |
| `PizzaController` | `/api/v1/pizzerias/{code}/pizzas` | No | List/detail pizzas, suitability check |
| `PizzaScoreController` | `/api/v1/pizza-scores` | Yes | Rate pizzas, get ratings |
| `FeedbackController` | `/api/v1/feedback` | Yes | Submit feedback, view history, unread count |
| `OrderController` | `/api/v1/orders` | Yes | Create/view/cancel orders |
| `AdminPriceController` | `/api/v1/admin/pizzerias/{code}/prices` | Admin | CSV export/import |
| `AdminFeedbackController` | `/api/v1/admin/pizzerias/{code}/feedback` | Admin | View all feedback, reply |

**Full endpoint table** — see [Section 4](#4-api-contract-summary).

### 2.4 Services

| Service | Key Methods |
|---------|-------------|
| `UserService` | register, login, getProfile, updateProfile, deleteUser |
| `MenuService` | getMenu (sections + items + ingredients + customisations) |
| `PizzaService` | listPizzas, getPizzaById, checkSuitability |
| `PizzaScoreService` | createScore, getUserScores |
| `FeedbackService` | submitFeedback, getUserFeedback, getUnreadCount, markRead, adminReply |
| `OrderService` | createOrder, getOrderHistory, getActiveOrders, getOrder, cancelOrder |
| `DeliveryAddressService` | save, list, delete, setDefault |
| `AdminPriceService` | exportPrices (CSV), importPrices (CSV with dry-run) |
| `AuthTokenService` | generateToken, validateToken (JWT HS256) |
| `VerificationTokenService` | generate (SecureRandom 24 bytes → Base64), consume |

### 2.5 Repositories

**Abstract interfaces** (in `service/repository/`):

| Interface | Methods |
|-----------|---------|
| `UserRepository` | save, findByIdAndPizzeriaId, findByEmailAndPizzeriaId, findAllByPizzeriaId, deleteById |
| `FeedbackRepository` | save, findByUserIdAndPizzeriaId, findByPizzeriaId, countUnreadReplies, markAllRepliesAsRead |
| `PizzaScoreRepository` | save, findByUserIdAndPizzeriaId, findByIdAndUserIdAndPizzeriaId |
| `OrderRepository` | save, saveOrderItem, saveOrderItemCustomisation, findByUserIdAndPizzeriaId, findActiveByUserIdAndPizzeriaId, findMaxOrderNumberForPrefix |
| `DeliveryAddressRepository` | save, findByIdAndUserIdAndPizzeriaId, findAllByUserIdAndPizzeriaId, deleteById, markDefaultByAddressId |

**R2DBC adapters** (in `service/repository/r2dbc/`): `UserRepositoryAdapter`,
`FeedbackRepositoryAdapter`, `PizzaScoreRepositoryAdapter`, `OrderRepositoryAdapter`,
`DeliveryAddressRepositoryAdapter`.

**Spring Data R2DBC repos**: `MenuSectionRepository`, `MenuItemRepository`,
`MenuItemIngredientRepository`, `MenuIngredientFactRepository`, `PizzaCustomisationRepository`,
`UserPreferredIngredientRepository`, `PizzeriaRepository`, `PizzaScoreRepositoryR2dbc`.

**In-memory test stubs**: `InMemoryUserRepository`, `InMemoryFeedbackRepository`,
`InMemoryPizzaScoreRepository`.

### 2.6 Domain Models & Enums

**Domain records** (immutable, `@Builder(toBuilder=true)`):

| Record | Key Fields |
|--------|------------|
| `User` | id, pizzeriaId, name, email, passwordHash, emailVerified, phone, preferredDiet, preferredIngredientIds, status, pizzeriaAdmin, profilePhotoBase64, createdAt, updatedAt |
| `Order` | id, pizzeriaId, userId, orderNumber, status, fulfillmentType, delivery fields, requestedTime, estimatedReadyTime, subtotal, deliveryFee, total, customerNotes, items |
| `OrderItem` | id, orderId, menuItemId, menuItemName, size, quantity, unitPrice, subtotal, customisations |
| `OrderItemCustomisation` | id, orderItemId, customisationId, customisationName, price |
| `Feedback` | id, pizzeriaId, userId, kind, message, rating, category, status, adminReply, adminRepliedAt, adminReplyReadAt |
| `PizzaScore` | id, userId, pizzeriaId, pizzaId, score, createdAt, updatedAt |
| `DeliveryAddress` | id, userId, pizzeriaId, label, street, postalCode, city, phone, instructions, isDefault |
| `Ingredient` | id, ingredientKey, dietaryType, allergenTags, spiceLevel |
| `IngredientPortion` | id, ingredientKey, portion |
| `PizzaTemplate` | id, name, ingredients, basePrice |

**Enums**:

| Enum | Values |
|------|--------|
| `Diet` | VEGAN, VEGETARIAN, CARNIVORE, NONE |
| `DietaryType` | VEGAN, VEGETARIAN, CARNIVORE, NONE |
| `UserStatus` | ACTIVE, INACTIVE, SUSPENDED, DELETED |
| `OrderStatus` | PENDING, CONFIRMED, PREPARING, READY, OUT_FOR_DELIVERY, DELIVERED, PICKED_UP, CANCELLED |
| `FulfillmentType` | PICKUP, DELIVERY |
| `FeedbackKind` | SERVICE, PRODUCT, DELIVERY |
| `FeedbackStatus` | OPEN, CLOSED, IN_PROGRESS |
| `PizzaSize` | REGULAR, FAMILY |
| `PizzaKind` | PIZZA, SALAD, PASTA, GYRO, OTHER |
| `PizzaAvailabilityStatus` | AVAILABLE, UNAVAILABLE, LIMITED |
| `IngredientAvailability` | AVAILABLE, UNAVAILABLE, LIMITED |

**Database entities** (Spring Data `@Table`): `MenuSectionEntity`, `MenuItemEntity`,
`MenuItemIngredientEntity`, `MenuIngredientFactEntity`, `PizzaCustomisationEntity`,
`UserPreferredIngredientEntity`, `PizzeriaEntity`, `PizzaScoreEntity`.

### 2.7 Database Schema & Liquibase

**PostgreSQL 16**, database name `pizzeria`. Changelogs in `db/changelog/`:

| Changelog | Purpose |
|-----------|---------|
| `db.changelog-100-menu-structure.yaml` | Creates menu_sections, menu_items, menu_item_ingredients, pizza_customisations |
| `db.changelog-200-menu-data.yaml` | Loads CSVs (sections, items, ingredients, facts, customisations) |
| `db.changelog-300-multi-tenant.yaml` | Creates pizzerias table, adds pizzeria_id FK to all tables, inserts Default pizzeria |
| `db.changelog-400-pizzeria-config.yaml` | Inserts Ramona pizzeria (ramonamalmo), copies menu data |
| `db.changelog-500-admin-feature.yaml` | Adds pizzeria_admin column to users |
| `db.changelog-600-feedback-category.yaml` | Adds category column to feedback |
| `db.changelog-700-feedback-reply.yaml` | Adds adminReply, adminRepliedAt columns |
| `db.changelog-800-feedback-reply-read.yaml` | Adds adminReplyReadAt column |
| `db.changelog-900-profile-photo.yaml` | Adds profilePhotoBase64 column (TEXT) |
| `db.changelog-1000-ordering.yaml` | Creates delivery_addresses, orders, order_items, order_item_customisations |

**Tables**:

| Table | Purpose | Approximate Seeded Rows |
|-------|---------|------------------------|
| `pizzerias` | Tenant registry | 2 (Default + Ramona) |
| `menu_sections` | Pizza categories | 8 |
| `menu_items` | Dishes | ~85 |
| `menu_item_ingredients` | Item→ingredient mappings | ~400 |
| `menu_ingredient_facts` | Ingredient dietary/allergen data | ~50 |
| `pizza_customisations` | Extra toppings/options | ~8 |
| `users` | User accounts | 0 (grows) |
| `user_preferred_ingredients` | User preferences | grows |
| `pizza_scores` | Ratings (1-5) | grows |
| `feedback` | User feedback | grows |
| `delivery_addresses` | Saved addresses | grows |
| `orders` | Customer orders | grows |
| `order_items` | Order line items | grows |
| `order_item_customisations` | Customisation per order item | grows |

### 2.8 Security & Authentication

**Auth flow**: Register → Verify Email → Login → Use Bearer Token → Logout.

- **JWT**: HS256, 24-hour expiration, claims: userId, pizzeriaId, pizzeriaAdmin, issuedAt,
  expiration. Secret minimum 32 characters.
- **Password**: DelegatingPasswordEncoder (bcrypt default, supports pbkdf2, scrypt, argon2).
- **Verification tokens**: SecureRandom 24 bytes → Base64 URL-encoded, stored in-memory
  (`ConcurrentHashMap`), consumed on use.
- **Multi-tenancy isolation**: Public endpoints use pizzeriaCode in URL; authenticated endpoints
  derive pizzeriaId from JWT.
- **Admin**: `pizzeriaAdmin` field in JWT, checked via `isAdminFor(pizzeriaCode)`.
- **Headers**: CSP `default-src 'self'`, HSTS enabled, CSRF disabled (stateless API).

**Security config** (`SecurityConfig`): Public paths include registration, login, verify-email,
menu, pizzas, ingredients, Swagger. Everything else requires authentication.

### 2.9 Error Handling & Validation

**Exception classes** (in `support/`):

| Exception | HTTP Status |
|-----------|-------------|
| `DomainValidationException` | 422 Unprocessable Entity |
| `ResourceNotFoundException` | 404 Not Found |
| `UnauthorizedException` | 401 Unauthorized |
| `ForbiddenException` | 403 Forbidden |

**Global error handler** (`RestErrorHandler`): All exceptions → ProblemDetail JSON with `type`,
`title`, `status`, `detail`, `errorCode`, `timestamp`, and optional `message` (i18n).

**Validation constraints**: name max 100 chars, email max 255 chars, password 8–100 chars,
score/rating 1–5, diet enum, profilePhotoBase64 data-URL regex.

### 2.10 Internationalization (i18n)

Two message files: `messages.properties` (English), `messages_sv.properties` (Swedish).

Key categories:
- Pizza names: `translation.key.disc.pizza.*` (~31 pizzas)
- Sections: `translation.key.section.*` (8 sections)
- Ingredients: `translation.key.ingredient.*` (~50 ingredients)
- Customisations: `translation.key.pizza.customisation.*` (~8)
- Descriptions: `translation.key.description.*`

The API returns translation keys; the frontend resolves them via i18next.

### 2.11 Configuration

**application.yaml**:

```yaml
server.port: 9900
spring.main.web-application-type: reactive
spring.r2dbc.url: r2dbc:postgresql://localhost:5432/pizzeria
spring.liquibase.change-log: classpath:db/changelog/db.changelog-master.yaml
jwt.secret: dev-secret-key-minimum-32-chars...  # env: JWT_SECRET
jwt.expiration-ms: 86400000                      # 24 hours, env: JWT_EXPIRATION_MS
management.endpoints.web.exposure.include: health,info
management.endpoint.health.probes.enabled: true
```

**application-test.yaml**: H2 in-memory (`r2dbc:h2:mem`), 1-hour JWT expiration, reduced logging.

**WebClientConfig**: Base client with 10s timeout, 16MB buffer, X-Correlation-Id filter.

**OpenApiConfig**: Grouped API v1 matching `/api/v1/**`.

### 2.12 Testing

**Framework**: JUnit 5, Reactor Test (StepVerifier), Testcontainers (PostgreSQL), MockServer.

**29 test files**, structured as:
- Controller unit tests (UserControllerTest, PizzaControllerTest, FeedbackControllerTest, etc.)
- Service unit tests (UserServiceTest, PizzaServiceTest, MenuServiceTest, etc.)
- Config tests (SecurityConfigTest)
- Repository tests (UserRepositoryAdapterTest, FeedbackRepositoryAdapterTest, etc.)
- Integration tests (FeedbackRepositoryAdapterIntegrationTest — uses Testcontainers with real
  PostgreSQL)

**Test naming**: `<TypeUnderTest>Test`, methods `should<Expectation>()`.

**In-memory stubs** (`src/test/java/repository/inmemory/`): ConcurrentHashMap-based fakes for fast
unit testing without a database.

**Coverage**: JaCoCo configured, reports at `target/site/jacoco/`.

### 2.13 Dependencies (pom.xml)

| Category | Key Dependencies |
|----------|------------------|
| Spring Boot 3.4.10 | webflux, actuator, security, oauth2-client, validation, data-r2dbc, jdbc, devtools |
| Database | r2dbc-postgresql, postgresql (JDBC for Liquibase), liquibase-core |
| API Docs | springdoc-openapi-starter-webflux-ui 2.8.13 |
| JWT | jjwt-api/impl/jackson 0.12.6 |
| Utilities | commons-lang3, lombok |
| Testing | spring-boot-starter-test, spring-security-test, reactor-test, testcontainers 1.20.6, h2, r2dbc-h2, mockserver-netty 5.15.0 |

### 2.14 Build Pipeline & Plugins

| Plugin | Purpose |
|--------|---------|
| **Spotless 2.46.1** | Google Java Format, runs on compile + verify |
| **JaCoCo 0.8.12** | Code coverage reports (prepare-agent + report) |
| **Jib 3.4.6** | OCI image build (base: distroless java17-debian12) |
| **OWASP Dependency-Check 12.1.3** | CVE scan, fails on CVSS ≥ 7.0 |

**Docker Compose**: PostgreSQL 16 Alpine on port 5432, database `pizzeria`.

### 2.15 Menu Data Seeding

CSV files in `db/changelog/data/`:
- `menu_sections.csv` — 8 sections (pizzas, gyro_pizzas, gourmet_pizzas, enchilada, gyros, salads,
  pasta, other)
- `menu_items_v2.csv` — ~85 items with dish number, prices, sort order
- `menu_item_ingredients.csv` — ~400 item-to-ingredient mappings
- `menu_ingredient_facts.csv` — ~50 ingredients with dietary_type, allergen_tags, spice_level
- `pizza_customisations.csv` — ~8 customisations (vegetables, fruit, meat, fish, gluten free, kids,
  pizza salad, extra cheese)

Data is initially loaded with NULL pizzeria_id, then migrated to Default pizzeria and copied to
Ramona.

---

## 3. Frontend — pizzeria-front-end

### 3.1 Technology Stack

- **Framework**: React 18.3.1 + TypeScript 5.6.3
- **Build**: Vite 5.4.10 with SWC transpiler
- **Styling**: Tailwind CSS 3.4.19
- **State**: React Query 5.59.20 (server state), React Context (auth, cart, pizzeria)
- **Routing**: React Router DOM 6.28.0
- **HTTP**: Axios 1.7.9
- **i18n**: i18next 25.7.3 + react-i18next 16.5.0
- **Testing**: Vitest 2.1.4 + React Testing Library 16.1.0

### 3.2 Project Structure

```
pizzeria-front-end/src/
├── api/                    # API client modules (9 modules)
├── components/
│   ├── ui/                 # Button, Input, Card, Alert, Badge, Select, Spinner, Avatar, etc.
│   └── layout/             # Header, Layout, Container
├── features/
│   ├── auth/               # LoginPage, RegisterPage, VerifyEmailPage, AuthProvider
│   ├── menu/               # MenuPage
│   ├── pizzas/             # PizzaListPage, PizzaDetailPage, AddToCartModal
│   ├── profile/            # ProfilePage
│   ├── preferences/        # PreferencesPage (diet + ingredients)
│   ├── scores/             # ScoresPage
│   ├── feedback/           # FeedbackPage
│   ├── cart/               # CartPage, CartProvider
│   ├── checkout/           # CheckoutPage
│   ├── orders/             # OrderHistoryPage, OrderDetailPage
│   ├── admin/              # AdminPricesPage, AdminFeedbackPage
│   ├── home/               # HomePage
│   └── error/              # NotFoundPage
├── hooks/                  # useAuth, usePizzeriaCode, useTranslateKey, useUnreadFeedbackCount
├── i18n/                   # Config + locales (en, sv) × (common, auth, menu)
├── routes/                 # AppRoutes, ProtectedRoute, PizzeriaProvider
├── tests/                  # Setup, test-utils, mocks
├── types/                  # api.ts (all interfaces, enums, DTOs)
├── utils/                  # Utility functions
├── styles/                 # Tailwind entry point
├── App.tsx                 # Root: QueryClientProvider + BrowserRouter + AppRoutes
└── main.tsx                # ReactDOM.createRoot entry
```

### 3.3 Pages & Features

#### Public Pages

| Page | Component | Description |
|------|-----------|-------------|
| Home | `HomePage` | Hero, feature cards, CTA for sign-in |
| Menu | `MenuPage` | Expandable sections, items with prices/dietary badges/allergens, customisations |
| Pizza List | `PizzaListPage` | Grid of pizzas with dish number, price, dietary type |
| Pizza Detail | `PizzaDetailPage` | Ingredients, prices, suitability check, add-to-cart modal |
| Login | `LoginPage` | Email + password, redirect after login |
| Register | `RegisterPage` | Name + email + password, shows verification token |
| Verify Email | `VerifyEmailPage` | Token input, optional `?token=` prefill |
| Cart | `CartPage` | Items, quantity adjustment, customisations, subtotal |

#### Protected Pages

| Page | Component | Description |
|------|-----------|-------------|
| Profile | `ProfilePage` | Edit name/phone, avatar upload with crop, delete account |
| Preferences | `PreferencesPage` | Diet selection, add/remove preferred ingredients |
| Scores | `ScoresPage` | Rate pizzas (1-5), view history |
| Feedback | `FeedbackPage` | Submit feedback with rating/category, view admin replies |
| Checkout | `CheckoutPage` | Pickup/delivery, address selection, order summary, delivery fee (49 SEK) |
| Order History | `OrderHistoryPage` | Active + past orders, status, totals |
| Order Detail | `OrderDetailPage` | Full order breakdown, cancel option |

#### Admin Pages

| Page | Component | Description |
|------|-----------|-------------|
| Prices | `AdminPricesPage` | CSV export/import with dry-run preview |
| Feedback | `AdminFeedbackPage` | View all feedback, reply, status management |

### 3.4 API Modules

| Module | Endpoints |
|--------|-----------|
| `client.ts` | Axios instance, auth interceptor, 401 handler, `getApiErrorMessage()` |
| `auth.ts` | register, verifyEmail, login, logout, fetchProfile, updateProfile, deleteProfile |
| `menu.ts` | fetchMenu |
| `pizzas.ts` | fetchPizzas, fetchPizza, checkSuitability |
| `pizzeria.ts` | fetchPizzeriaInfo |
| `preferences.ts` | fetchDiet, updateDiet, fetchPreferredIngredients, addPreferredIngredient, removePreferredIngredient |
| `scores.ts` | createScore, fetchMyScores |
| `feedback.ts` | submitServiceFeedback, fetchMyFeedback, fetchUnreadFeedbackCount, markFeedbackRepliesAsRead |
| `orders.ts` | createOrder, fetchOrderHistory, fetchActiveOrders, fetchOrder, cancelOrder |
| `addresses.ts` | fetchAddresses, saveAddress, deleteAddress, setDefaultAddress |
| `admin.ts` | exportPrices, importPrices, fetchAdminFeedback, replyToFeedback |

### 3.5 Components

**UI components** (`components/ui/`):

| Component | Variants / Features |
|-----------|---------------------|
| `Button` | primary, secondary, danger, ghost; sizes sm/md/lg; loading spinner |
| `Input` | Label, required, autocomplete |
| `Card` | Padding options (none, md, lg) |
| `Alert` | success, error, warning, info; icons, title, dismissible |
| `Badge` | Variants, sizes |
| `Select` | Dropdown with label |
| `Spinner` | Size options |
| `Avatar` | Image or initials fallback |
| `AvatarUpload` | Drag-drop, 500KB limit |
| `AvatarCropModal` | react-easy-crop integration |
| `OpeningHoursDisplay` | Renders pizzeria hours |

**Layout components**: `Header` (sticky nav, mobile menu, cart badge, language selector, unread
feedback badge), `Layout` (header + outlet), `Container` (max-w-7xl).

**Feature components**: `AddToCartModal` (size, customisations, quantity, special instructions,
total calculation).

### 3.6 Routing

Root `/` redirects to `/ramonamalmo`. All routes nested under `/:pizzeriaCode`.

| Route | Component | Auth |
|-------|-----------|------|
| `/:pizzeriaCode` | HomePage | — |
| `/:pizzeriaCode/menu` | MenuPage | — |
| `/:pizzeriaCode/pizzas` | PizzaListPage | — |
| `/:pizzeriaCode/pizzas/:id` | PizzaDetailPage | — |
| `/:pizzeriaCode/login` | LoginPage | — |
| `/:pizzeriaCode/register` | RegisterPage | — |
| `/:pizzeriaCode/verify-email` | VerifyEmailPage | — |
| `/:pizzeriaCode/cart` | CartPage | — |
| `/:pizzeriaCode/checkout` | CheckoutPage | Yes |
| `/:pizzeriaCode/orders` | OrderHistoryPage | Yes |
| `/:pizzeriaCode/orders/:id` | OrderDetailPage | Yes |
| `/:pizzeriaCode/profile` | ProfilePage | Yes |
| `/:pizzeriaCode/preferences` | PreferencesPage | Yes |
| `/:pizzeriaCode/scores` | ScoresPage | Yes |
| `/:pizzeriaCode/feedback` | FeedbackPage | Yes |
| `/:pizzeriaCode/admin/prices` | AdminPricesPage | Admin |
| `/:pizzeriaCode/admin/feedback` | AdminFeedbackPage | Admin |
| `*` | NotFoundPage | — |

`ProtectedRoute` shows a spinner while loading, then redirects to login (with return-to state) if
unauthenticated.

`PizzeriaProvider` wraps all routes and provides pizzeria context (name, timezone, opening hours,
address, phone, isOpenNow). It nests `AuthProvider` and `CartProvider`.

### 3.7 State Management

**React Query** (server state):
- Default `staleTime`: 5 minutes
- `retry`: 1
- Cache invalidation via `useQueryClient().invalidateQueries()`
- Pizzeria info cached with `staleTime: Infinity`

**Context providers**:

| Context | Data | Storage |
|---------|------|---------|
| `AuthContext` | isAuthenticated, isLoading, profile, login/register/logout/refreshProfile/deleteAccount | localStorage per pizzeria: `pizzeria-{code}-auth-token` |
| `PizzeriaContext` | pizzeriaCode, pizzeriaName, timezone, address, openingHours, phoneNumbers, isOpenNow | React Query cache |
| `CartContext` | items, itemCount, subtotal, addItem/updateQuantity/removeItem/clearCart | localStorage: `pizzeria-cart-{code}` |

### 3.8 Authentication Flow

1. **Register** → API returns `verificationToken` → displayed on screen (dev mode)
2. **Verify Email** → token input → API sets emailVerified=true → redirect to login
3. **Login** → email + password → API returns `accessToken` → stored in localStorage + set in axios
4. **Session restore** → on mount, token restored from localStorage, profile fetched
5. **401 handling** → axios interceptor clears token, dispatches unauthorized event
6. **Logout** → optional API call → token cleared from localStorage + axios

### 3.9 TypeScript Types

All types defined in `src/types/api.ts`.

**Enums**: `DietType`, `PizzaType`, `ErrorCode`, `OrderStatus`, `FulfillmentType`, `PizzaSize`.

**Request DTOs**: 19 interfaces covering all API mutations.

**Response DTOs**: 30+ interfaces covering all API responses.

**Cart types**: `CartItem`, `CartItemCustomisation` (local-only, not sent to API directly).

### 3.10 Internationalization

**Setup** (`src/i18n/config.ts`): i18next + react-i18next with browser language detection +
localStorage fallback.

**Locales**: English (`en/`) and Swedish (`sv/`), each with `common.json`, `auth.json`, `menu.json`.

**Key patterns**: Nested dot-notation (`home.features.menu.title`). API translation keys like
`translation.key.disc.pizza.margarita` are mapped in `menu.json`.

**Custom hook**: `useTranslateKey` — translates API keys, falls back to formatted key name
(underscore → space, capitalize).

### 3.11 Styling (Tailwind)

- **Config**: `tailwind.config.js` with custom primary green palette (shades 50-950)
- **Design tokens**: Slate grays, semantic colors (success=green, error=red, warning=yellow,
  info=blue)
- **Responsive**: Mobile-first (md: tablet, lg: desktop)
- **Button styling**: 3D effects (border, shadow) on primary variant
- **Entry point**: `src/styles/index.css` with `@tailwind` directives

### 3.12 Configuration

**vite.config.ts**: Port 5173, auto-open, proxy `/api` → `http://localhost:9900`, source maps.

**tsconfig.json**: ES2020 target, strict mode, React JSX runtime, bundler module resolution.

**vitest.config.ts**: jsdom environment, globals, setup file, CSS disabled.

**Environment variables**: `VITE_API_BASE_URL` (default `/api/v1`), `VITE_API_PROXY_TARGET`
(default `http://localhost:9900`).

### 3.13 Testing

**231 tests across 13 files**:

| File | Tests | Focus |
|------|-------|-------|
| `api/client.test.ts` | 13 | Axios client, interceptors, error handling |
| `api/api-modules.test.ts` | 19 | All API module functions and endpoints |
| `features/auth/AuthProvider.test.tsx` | 10 | Auth context, login/logout, token management |
| `hooks/hooks.test.tsx` | 10 | useAuth, usePizzeriaCode, useTranslateKey |
| `routes/routes.test.tsx` | 5 | Protected routes, redirects |
| `components/ui/ui-components.test.tsx` | 52 | Button, Input, Card, Alert, etc. |
| `components/layout/layout.test.tsx` | 10 | Header, navigation, layout |
| `features/auth/auth-pages.test.tsx` | 22 | Login, Register, VerifyEmail pages |
| `features/data-pages.test.tsx` | 18 | Menu, Pizzas, Preferences, Scores pages |
| `features/user-pages.test.tsx` | 22 | Profile, Feedback, Orders pages |
| `features/info-pages.test.tsx` | 14 | Home page, error pages |
| `i18n/i18n.test.ts` | 33 | i18n config and translations |

**Test utilities**: `src/tests/setup.ts` (jsdom setup), `src/tests/test-utils.tsx` (render helpers
with providers), `src/tests/mocks/api.ts` (mock API responses).

### 3.14 Custom Hooks

| Hook | Returns | Description |
|------|---------|-------------|
| `useAuth` | isAuthenticated, isLoading, profile, login, register, logout, refreshProfile, deleteAccount | Access AuthContext |
| `usePizzeriaCode` | pizzeriaCode | Extract code from PizzeriaContext |
| `usePizzeriaContext` | Full pizzeria info + isOpenNow | Access PizzeriaContext |
| `useTranslateKey` | translateKey(key, fallback?), currentLanguage | Translate API translation keys |
| `useUnreadFeedbackCount` | React Query result with unreadCount | Polls every 30s, refetches on focus |
| `useCart` | items, itemCount, subtotal, addItem, updateQuantity, removeItem, clearCart | Access CartContext |

### 3.15 Error Handling

- **Axios interceptor**: 401 → clears token, dispatches `unauthorized` event
- **`getApiErrorMessage(error)`**: Extracts `detail` from ProblemDetail response
- **Per-page error state**: try-catch around mutations, error displayed in `Alert` component
- **Form validation**: HTML5 validation + custom messages, disabled submit on errors
- **No error boundary**: Missing — unhandled errors could crash the app

### 3.16 Dependencies (package.json)

| Category | Packages |
|----------|----------|
| Core | react 18.3.1, react-dom 18.3.1, react-router-dom 6.28.0 |
| Data | @tanstack/react-query 5.59.20, axios 1.7.9 |
| Styling | tailwindcss 3.4.19, autoprefixer 10.4.23, postcss 8.5.6 |
| i18n | i18next 25.7.3, react-i18next 16.5.0, i18next-browser-languagedetector 8.2.0 |
| Features | react-easy-crop 5.5.6, zod 3.23.8 |
| Dev/Build | typescript 5.6.3, vite 5.4.10, @vitejs/plugin-react-swc, vitest 2.1.4, @testing-library/react 16.1.0, eslint + plugins |

---

## 4. API Contract Summary

All endpoints use prefix `/api/v1`.

### Public Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/pizzerias/{code}` | Pizzeria info (name, currency, timezone, config, branding) |
| POST | `/pizzerias/{code}/users/register` | Register user |
| POST | `/pizzerias/{code}/users/verify-email` | Verify email with token |
| POST | `/pizzerias/{code}/users/login` | Login, returns Bearer token |
| GET | `/pizzerias/{code}/menu` | Full menu (sections + items + ingredients + customisations) |
| GET | `/pizzerias/{code}/pizzas` | List all pizzas |
| GET | `/pizzerias/{code}/pizzas/{pizzaId}` | Pizza details |

### Authenticated Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/users/logout` | Invalidate token |
| GET | `/users/me` | Get user profile |
| PATCH | `/users/me` | Update profile (name, phone, photo) |
| DELETE | `/users/me` | Delete account |
| GET | `/users/me/diet` | Get dietary preference |
| PUT | `/users/me/diet` | Update dietary preference |
| GET | `/users/me/preferences/ingredients/preferred` | List preferred ingredients |
| POST | `/users/me/preferences/ingredients/preferred` | Add preferred ingredient |
| DELETE | `/users/me/preferences/ingredients/preferred/{id}` | Remove preferred ingredient |
| GET | `/users/me/addresses` | List delivery addresses |
| POST | `/users/me/addresses` | Save delivery address |
| DELETE | `/users/me/addresses/{id}` | Delete delivery address |
| POST | `/users/me/addresses/{id}/default` | Set default address |
| POST | `/pizzas/suitability` | Check pizza suitability for user diet |
| POST | `/pizza-scores` | Rate a pizza (1-5) |
| GET | `/pizza-scores/me` | Get user's ratings |
| POST | `/feedback/service` | Submit service feedback |
| GET | `/feedback/me` | Get user's feedback history |
| GET | `/feedback/me/unread-count` | Get unread admin reply count |
| POST | `/feedback/me/mark-read` | Mark admin replies as read |
| POST | `/orders` | Create order |
| GET | `/orders` | Get order history |
| GET | `/orders/active` | Get active orders |
| GET | `/orders/{id}` | Get order details |
| POST | `/orders/{id}/cancel` | Cancel order |

### Admin Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/admin/pizzerias/{code}/prices/export` | Export prices as CSV |
| POST | `/admin/pizzerias/{code}/prices/import` | Import prices from CSV (supports dry-run) |
| GET | `/admin/pizzerias/{code}/feedback` | Get all customer feedback |
| POST | `/admin/pizzerias/{code}/feedback/{id}/reply` | Reply to feedback |

---

## 5. Statistics

| Metric | Backend | Frontend |
|--------|---------|----------|
| Source files | ~147 .java | ~60 .ts/.tsx |
| Test files | 29 | 13 |
| Test cases | — | 231 |
| Controllers / Pages | 12 controllers | 18 pages |
| Services / API modules | 10 services | 11 API modules |
| Domain models / Types | 18 records | 50+ interfaces |
| Database tables | 15 | — |
| API endpoints | 35+ | 35+ consumed |
| Context providers | — | 3 (Auth, Cart, Pizzeria) |
| Custom hooks | — | 6 |
| UI components | — | 11 reusable + 2 layout |
| Supported languages | 2 (en, sv) | 2 (en, sv) |
| Seeded pizzas | ~30 | — |
| Menu sections | 8 | — |
| Unique ingredients | ~50 | — |
| Customisations | ~8 | — |

---

## 6. Areas for Improvement

### Backend

1. **Token storage**: Verification tokens stored in-memory (`ConcurrentHashMap`) with no TTL. Memory
   leak risk over time. Migrate to Redis or database storage with expiration.
2. **Distributed deployment**: JWT tokens work stateless, but verification tokens and any token
   blacklist require shared storage for multi-instance.
3. **Email delivery**: No actual email sent for verification; token displayed on screen
   (development-only). Integrate email service (SendGrid, SES) for production.
4. **Integration test coverage**: Most tests are unit tests with in-memory stubs. Only one
   integration test uses Testcontainers.
5. **No password reset flow**: Neither API nor UI supports password reset.
6. **No rate limiting**: No protection against brute-force login attempts.

### Frontend

1. **No error boundary**: React error boundary missing — unhandled errors crash the entire app.
2. **Hardcoded delivery fee**: 49 SEK is hardcoded in `CheckoutPage`; should come from backend
   configuration.
3. **No skeleton loaders**: Generic spinner used everywhere; skeleton screens would improve perceived
   performance.
4. **Accessibility**: Basic ARIA labels present but not WCAG-audited. Some interactive elements lack
   keyboard support.
5. **No image support for dishes**: Menu items have no photos — a significant UX gap for a food
   ordering platform.
6. **Zod underutilized**: Listed as dependency but not heavily used for runtime validation.

### Cross-cutting

1. **No search**: No search or filtering UI for menu items (backend has the data for it).
2. **No real-time updates**: No WebSocket/SSE for order status updates; polling only for unread
   feedback count.
3. **No analytics**: No tracking (Google Analytics or similar).
4. **No GDPR cookie handling**: Cookie consent not implemented.
