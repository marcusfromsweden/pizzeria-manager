# Troubleshooting Guide

Quick solutions for common problems encountered during development of the pizzeria platform.

**Last Updated:** 2026-02-11

---

## Table of Contents

1. [Quick Checklist](#quick-checklist)
2. [Backend Issues](#backend-issues)
3. [Frontend Issues](#frontend-issues)
4. [Database Issues](#database-issues)
5. [API Issues](#api-issues)
6. [Authentication Issues](#authentication-issues)
7. [Build & Tooling Issues](#build--tooling-issues)
8. [Debug Commands](#debug-commands)

---

## Quick Checklist

Run through this before deeper investigation:

- [ ] Backend running? (`cd pizzeria-service && mvn spring-boot:run`)
- [ ] Frontend running? (`cd pizzeria-front-end && npm run dev`)
- [ ] PostgreSQL running? (`docker-compose ps` in pizzeria-service/)
- [ ] Browser hard refreshed? (Ctrl+Shift+R or Cmd+Shift+R)
- [ ] Correct pizzeria code in URL? (should be `ramonamalmo`)
- [ ] Auth token valid? (try logging in again)
- [ ] Browser console errors? (F12 → Console tab)
- [ ] Backend terminal showing errors? (check terminal output)

---

## Backend Issues

### Problem: "Port 9900 already in use"

**Symptom:** Backend fails to start with address-already-in-use error.

**Solution:**
```bash
# Find process using port 9900
lsof -i :9900

# Kill it
kill -9 <PID>

# Or use:
lsof -ti :9900 | xargs kill -9
```

---

### Problem: Liquibase migration fails on startup

**Symptom:** Backend crashes during startup with Liquibase changelog error.

**Common causes:**

1. **Database schema drift** — schema was modified outside Liquibase
   ```bash
   # Nuclear option: reset database
   cd pizzeria-service
   docker-compose down -v
   docker-compose up -d
   mvn spring-boot:run
   ```

2. **Changelog checksum mismatch** — changelog file was modified after running
   - Do not modify already-applied changelogs
   - Create new changelog files for new changes

3. **PostgreSQL not ready** — container started but DB not accepting connections
   ```bash
   # Check container health
   docker-compose ps
   # Wait for "healthy" status, then retry
   ```

---

### Problem: R2DBC connection refused

**Symptom:** `ConnectionRefusedException` or `Connection to localhost:5432 refused`.

**Solutions:**

1. Start PostgreSQL: `cd pizzeria-service && docker-compose up -d`
2. Wait for container health check: `docker-compose ps` (should show "healthy")
3. Verify port: `lsof -i :5432`

---

### Problem: Spotless formatting fails

**Symptom:** `mvn verify` fails with Spotless violations.

**Solution:**
```bash
cd pizzeria-service
mvn spotless:apply  # Auto-format
mvn verify          # Retry
```

---

### Problem: Tests fail with H2 compatibility issues

**Symptom:** Tests fail with SQL syntax errors (H2 doesn't match PostgreSQL behavior).

**Notes:**
- Unit tests use H2 in-memory with `MODE=PostgreSQL`
- Some PostgreSQL-specific features may not work in H2
- Integration tests use Testcontainers with real PostgreSQL
- If H2 can't handle a query, consider writing an integration test instead

---

## Frontend Issues

### Problem: Page doesn't update after code changes

**Symptom:** Modified a React component but browser shows old version.

**Solutions (in order):**

1. **Wait for Vite HMR** — check terminal for "✓ compiled" message
2. **Manual refresh** — F5 or Ctrl+R
3. **Hard refresh** — Ctrl+Shift+R (clears cache)
4. **Restart frontend:**
   ```bash
   # Ctrl+C in frontend terminal, then:
   cd pizzeria-front-end && npm run dev
   ```

---

### Problem: Proxy errors (CORS / 502 / connection refused)

**Symptom:** API calls fail with network errors, CORS errors, or 502 Bad Gateway.

**Cause:** Frontend dev server can't reach backend.

**Solutions:**

1. Verify backend is running on port 9900
2. Check Vite proxy config in `vite.config.ts` — should proxy `/api` to `http://localhost:9900`
3. Check environment variable: `VITE_API_PROXY_TARGET`

---

### Problem: TypeScript type errors after API contract changes

**Symptom:** Red squiggles in IDE after backend DTO changes.

**Solution:** Update TypeScript interfaces in `src/types/api.ts` to match new backend contract.
Check Swagger UI at `http://localhost:9900/swagger-ui.html` for current schema.

---

### Problem: Translation keys showing raw (e.g., "translation.key.disc.pizza.margarita")

**Symptom:** Menu items show translation keys instead of readable names.

**Causes:**
1. Missing key in locale file (`src/i18n/locales/{lang}/menu.json`)
2. Wrong namespace in `useTranslation()` call
3. i18n not initialized — check `src/i18n/config.ts`

**Debug:**
```typescript
// In browser console
i18next.t('menu:translation.key.disc.pizza.margarita')
// If returns the key itself, it's missing from locale file
```

---

### Problem: React Query cache shows stale data

**Symptom:** Data appears outdated after a mutation.

**Solution:** Invalidate the relevant query after mutation:
```typescript
const queryClient = useQueryClient();
// After mutation succeeds:
queryClient.invalidateQueries({ queryKey: ['entity-name'] });
```

Default `staleTime` is 5 minutes. Pizzeria info has `staleTime: Infinity`.

---

## Database Issues

### Problem: PostgreSQL container won't start

**Symptom:** `docker-compose up -d` fails or container exits immediately.

**Solutions:**

1. **Port conflict:**
   ```bash
   lsof -i :5432
   # If another PostgreSQL is running, stop it or change the port
   ```

2. **Corrupt data volume:**
   ```bash
   docker-compose down -v  # WARNING: deletes all data
   docker-compose up -d
   ```

3. **Docker not running:**
   ```bash
   # Check Docker daemon
   docker info
   # Start Docker if needed (WSL/Linux)
   sudo service docker start
   ```

---

### Problem: Data seeding looks wrong or incomplete

**Symptom:** Menu items missing, wrong prices, or missing sections.

**Debug:**
```bash
# Connect to database
docker exec -it $(docker-compose ps -q postgres) psql -U pizzeria -d pizzeria

# Check seeded data
SELECT count(*) FROM menu_items WHERE pizzeria_id = '00000000-0000-0000-0000-000000000002';
SELECT count(*) FROM menu_sections WHERE pizzeria_id = '00000000-0000-0000-0000-000000000002';

# Check pizzeria status
SELECT id, code, name, active FROM pizzerias;
```

**Fix:** Reset database (see Liquibase section above).

---

## API Issues

### Problem: 401 Unauthorized on authenticated endpoints

**Symptom:** API returns 401 even though user is logged in.

**Causes:**

1. **Token expired** (24-hour default) — log in again
2. **Backend restarted** — in-memory tokens lost, log in again
3. **Wrong header format** — must be `Authorization: Bearer <token>`
4. **Token for wrong pizzeria** — tokens are scoped per pizzeria

**Debug:**
```bash
# Test token manually
curl -H "Authorization: Bearer <token>" http://localhost:9900/api/v1/users/me
```

---

### Problem: 404 Not Found on public endpoints

**Symptom:** Menu, pizzas, or pizzeria info returns 404.

**Common causes:**

1. **Wrong pizzeria code** — use `ramonamalmo`, not `default`
2. **Pizzeria inactive** — Default pizzeria is deactivated; only Ramona is active
3. **Wrong URL** — all public endpoints require `{pizzeriaCode}` in path

**Verify:**
```bash
curl http://localhost:9900/api/v1/pizzerias/ramonamalmo
# Should return pizzeria info
```

---

### Problem: 422 Unprocessable Entity

**Symptom:** API returns 422 with validation error.

**Cause:** Request body fails Jakarta validation.

**Common constraints:**
- `name`: max 100 characters
- `password`: min 8 characters
- `score`/`rating`: integer 1-5
- `diet`: must be VEGAN, VEGETARIAN, CARNIVORE, or NONE
- `email`: must be valid email format

**Debug:** Check the `detail` field in the ProblemDetail response for specific field errors.

---

### Problem: Swagger UI shows outdated schema

**Symptom:** Swagger UI at `/swagger-ui.html` doesn't reflect recent changes.

**Solution:** Restart backend — OpenAPI schema is generated on startup.

---

## Authentication Issues

### Problem: Email verification token lost

**Symptom:** User registered but can't verify email because token is gone.

**Causes:**
1. Backend restarted (tokens are in-memory)
2. Token already consumed

**Workaround:** Register again with a different email, or restart backend + re-register.

---

### Problem: Login fails with "email not verified"

**Symptom:** Login returns error even though correct credentials.

**Solution:** Complete email verification first:
```bash
curl -X POST http://localhost:9900/api/v1/pizzerias/ramonamalmo/users/verify-email \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "token": "<verification-token>"}'
```

---

## Build & Tooling Issues

### Problem: `mvn clean verify` fails

**Common causes and fixes:**

1. **Spotless formatting** — run `mvn spotless:apply` first
2. **Test failures** — check test output, fix failing tests
3. **OWASP check** — dependency vulnerability found (CVSS >= 7.0)
   - Check `mvn dependency-check:check` output
   - Update vulnerable dependency or add suppression

---

### Problem: `npm run build` fails with type errors

**Solution:**
```bash
cd pizzeria-front-end
npm run typecheck  # See specific errors
# Fix TypeScript errors, then retry:
npm run build
```

---

### Problem: ESLint errors blocking commit

**Solution:**
```bash
cd pizzeria-front-end
npm run lint  # See all errors
# Fix errors, or for auto-fixable:
npx eslint --fix src/
```

---

## Debug Commands

### Backend

```bash
# Check backend health
curl http://localhost:9900/actuator/health

# View Swagger UI (interactive API docs)
open http://localhost:9900/swagger-ui.html

# Test pizzeria info
curl http://localhost:9900/api/v1/pizzerias/ramonamalmo

# Test menu
curl http://localhost:9900/api/v1/pizzerias/ramonamalmo/menu

# Test login
curl -X POST http://localhost:9900/api/v1/pizzerias/ramonamalmo/users/login \
  -H "Content-Type: application/json" \
  -d '{"email": "test@test.com", "password": "password123"}'

# Test authenticated endpoint
curl -H "Authorization: Bearer <token>" http://localhost:9900/api/v1/users/me
```

### Frontend

```bash
# Check dev server
curl http://localhost:5173

# Type check without building
cd pizzeria-front-end && npm run typecheck

# Run tests
cd pizzeria-front-end && npm test

# Check for lint issues
cd pizzeria-front-end && npm run lint
```

### Database

```bash
# Check PostgreSQL container
cd pizzeria-service && docker-compose ps

# Connect to database
docker exec -it $(docker-compose ps -q postgres) psql -U pizzeria -d pizzeria

# Common SQL queries
# List pizzerias:
SELECT id, code, name, active FROM pizzerias;

# Count menu items per pizzeria:
SELECT p.code, count(mi.id)
FROM pizzerias p
LEFT JOIN menu_sections ms ON ms.pizzeria_id = p.id
LEFT JOIN menu_items mi ON mi.section_id = ms.id
GROUP BY p.code;

# List users:
SELECT id, name, email, email_verified, pizzeria_admin FROM users;
```

### Docker

```bash
# View running containers
docker-compose ps

# View container logs
docker-compose logs postgres

# Restart container
docker-compose restart postgres

# Full reset (data loss!)
docker-compose down -v && docker-compose up -d
```
