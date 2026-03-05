# CLAUDE.md

This file provides guidance to Claude Code when working with code in this repository.

## Project Overview

React + TypeScript single-page application for a multi-tenant pizzeria platform. The frontend communicates with a Spring Boot backend API.

## Technology Stack

- **Framework**: React 18 with TypeScript
- **Build Tool**: Vite
- **Styling**: Tailwind CSS v3
- **State Management**: React Query v5 (server state), React Context (auth)
- **Routing**: React Router DOM v6
- **HTTP Client**: Axios
- **i18n**: react-i18next (English and Swedish)
- **Testing**: Vitest with React Testing Library

## Build and Development Commands

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

## Project Structure

```
src/
├── api/                    # API client modules (auth, menu, pizzas, preferences, feedback, scores)
├── components/
│   ├── ui/                 # Reusable UI (Button, Input, Card, Alert, Badge, Select, Spinner)
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
├── tests/                  # Test utilities and setup
├── types/                  # TypeScript interfaces matching API contract
└── styles/                 # Tailwind CSS entry point
```

## Multi-Tenancy Routing

All routes are prefixed with `/:pizzeriaCode`:

| Route | Page | Auth Required |
|-------|------|---------------|
| `/:pizzeriaCode` | Home | No |
| `/:pizzeriaCode/menu` | Full menu | No |
| `/:pizzeriaCode/pizzas` | Pizza list | No |
| `/:pizzeriaCode/pizzas/:id` | Pizza detail | No |
| `/:pizzeriaCode/login` | Login | No |
| `/:pizzeriaCode/register` | Register | No |
| `/:pizzeriaCode/verify-email` | Email verification | No |
| `/:pizzeriaCode/forgot-password` | Forgot password | No |
| `/:pizzeriaCode/reset-password` | Reset password | No |
| `/:pizzeriaCode/profile` | User profile | Yes |
| `/:pizzeriaCode/preferences` | Diet & ingredients | Yes |
| `/:pizzeriaCode/scores` | Pizza ratings | Yes |
| `/:pizzeriaCode/feedback` | Service feedback | Yes |

The root path `/` redirects to `/default` (or configured default pizzeria).

## API Integration

- **Base URL**: `/api/v1` (proxied to backend)
- **Dev Proxy**: Vite proxies `/api` requests to `http://localhost:9900`
- **Public endpoints**: Include `pizzeriaCode` in URL path
- **Authenticated endpoints**: Use Bearer token only
- **Token storage**: Per-pizzeria in localStorage: `pizzeria-{code}-auth-token`

### Configuration Files

- `src/api/client.ts` - Axios instance with interceptors
- `vite.config.ts` - Dev server proxy configuration

## Testing

Test suite: **320 tests** across **18 test files**

```bash
# Run all tests
npm run test

# Run tests once (CI mode)
npm run test -- --run

# Run specific test file
npm run test -- --run src/features/auth/auth-pages.test.tsx
```

### Test Files

| File | Tests | Coverage |
|------|-------|----------|
| `src/api/client.test.ts` | 13 | API client, interceptors |
| `src/api/api-modules.test.ts` | 19 | All API modules |
| `src/features/auth/AuthProvider.test.tsx` | 10 | Auth context |
| `src/hooks/hooks.test.tsx` | 10 | Custom hooks |
| `src/routes/routes.test.tsx` | 5 | Route guards |
| `src/components/ui/ui-components.test.tsx` | 52 | UI components |
| `src/components/layout/layout.test.tsx` | 10 | Layout components |
| `src/features/auth/auth-pages.test.tsx` | 35 | Auth pages (login, register, verify, forgot/reset password) |
| `src/features/data-pages.test.tsx` | 18 | Data pages |
| `src/features/user-pages.test.tsx` | 22 | User pages |
| `src/features/info-pages.test.tsx` | 14 | Info pages |
| `src/i18n/i18n.test.ts` | 33 | i18n config |

### Test Utilities

- `src/tests/setup.ts` - Vitest setup with jsdom
- `src/tests/test-utils.tsx` - Render helpers, mock contexts, test i18n instance

## i18n (Internationalization)

Supported languages: **English (en)**, **Swedish (sv)**

```
src/i18n/
├── config.ts              # i18next configuration
└── locales/
    ├── en/
    │   ├── common.json    # Navigation, actions, status
    │   ├── auth.json      # Login, register, verify email, forgot/reset password, profile
    │   └── menu.json      # Menu, pizzas, dietary info
    └── sv/
        └── ...            # Swedish translations
```

The `useTranslateKey` hook handles API translation keys (e.g., `translation.key.disc.pizza.margarita`).

## Running with Backend

```bash
# Terminal 1: Start backend (from pizzeria-service/)
docker-compose up -d    # PostgreSQL
mvn spring-boot:run     # Backend on port 9900

# Terminal 2: Start frontend (from pizzeria-front-end/)
npm run dev             # Frontend on port 5173
```

Visit: `http://localhost:5173/default` (or your pizzeria code)

## Code Style

- TypeScript strict mode
- ESLint for linting
- Tailwind CSS for styling (no CSS modules)
- Feature-based folder structure
- Co-located test files (`.test.tsx` next to source)

## Development Guidelines

**Testing Requirement**: When implementing new features or fixing bugs, always add relevant tests. This includes:
- Unit tests for new components, hooks, and utility functions
- Integration tests for new pages or features
- Test updates when modifying existing functionality

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
