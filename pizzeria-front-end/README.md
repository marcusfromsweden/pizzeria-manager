# Pizzeria Front-End

React + Vite client for the Pizzeria Service API.

## Getting started

```bash
yarn install # or npm install
npm run dev
```

Configure `VITE_API_BASE_URL` to point at the backend. During local dev the Vite dev server proxies `/api` to `http://localhost:8080` by default (configured in `vite.config.ts`).

## Available scripts

- `npm run dev` – start the Vite dev server.
- `npm run build` – type-check and create a production bundle.
- `npm run test` – execute Vitest unit tests.
- `npm run lint` – lint TypeScript source files.

## Structure overview

The app follows a feature-first layout inspired by the reference projects in `front-end-repos-for-reference/web`:

```
src/
  api/         # REST clients aligned with backend controllers
  features/    # Feature pages and UI flows (pizzas, auth, profile, etc.)
  components/  # Shared layout components
  routes/      # Route guards and navigation helpers
  tests/       # Test utilities
  types/       # Shared DTO interfaces mirrored from the backend
```
