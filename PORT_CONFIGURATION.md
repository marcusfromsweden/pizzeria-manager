# Port Configuration Guide

This document explains how ports are configured for the backend and frontend services in the pizzeria platform.

---

## Quick Reference

| Service | Default Port | Actual Port | Configured In |
|---------|--------------|-------------|---------------|
| Backend (Spring Boot) | 9900 | 8802 | `pizzeria-service/.env` |
| Frontend (React + Vite) | 5173 | 5202 | `pizzeria-front-end/.env` |
| Database (PostgreSQL) | 5432 | 5432 | `docker-compose.yml` |

---

## Backend (Spring Boot) Port Configuration

### Configuration Hierarchy (Priority: highest to lowest)

1. **Command-line arguments** (highest priority)
   - Example: `--server.port=8802`

2. **Environment variables**
   - Example: `SERVER_PORT=8802`

3. **application.yaml** (lowest priority - default fallback)
   - Located at: `pizzeria-service/src/main/resources/application.yaml`
   - Contains: `server.port: 9900`

### How It Works

**Step 1: application.yaml (default)**
```yaml
# pizzeria-service/src/main/resources/application.yaml
server:
  port: 9900  # Default fallback if nothing else is set
```

**Step 2: .env file (local override)**
```bash
# pizzeria-service/.env
SERVER_PORT=8802  # Override the default
```

**Step 3: script/run.sh (applies the override)**
```bash
# Reads .env file using safe parsing
if [ -f .env ]; then
    while IFS='=' read -r key value; do
        if [[ "$key" =~ ^(SERVER_PORT|...)$ ]]; then
            export "$key"="$value"  # Exports SERVER_PORT=8802
        fi
    done < .env
fi

PORT=${SERVER_PORT:-9900}  # Use SERVER_PORT from .env, or default to 9900

# Pass as command-line argument (highest priority)
nohup mvn spring-boot:run \
  -Dspring-boot.run.arguments="--server.port=${PORT}" \
  > backend.log 2>&1 &
```

**Result:** Backend runs on **8802** (from .env) instead of 9900 (from application.yaml)

### Backend URLs

With `SERVER_PORT=8802`:
- API Base: `http://localhost:8802/api/v1`
- Swagger UI: `http://localhost:8802/swagger-ui.html`
- OpenAPI Docs: `http://localhost:8802/v3/api-docs`
- Health Check: `http://localhost:8802/actuator/health`

---

## Frontend (React + Vite) Port Configuration

### Configuration Hierarchy

1. **Environment variable `VITE_PORT`**
   - Read by vite.config.ts at runtime

2. **vite.config.ts default**
   - Hardcoded fallback: `5202`

### How It Works

**Step 1: vite.config.ts (with default)**
```typescript
// pizzeria-front-end/vite.config.ts
export default defineConfig({
  server: {
    port: Number(process.env.VITE_PORT) || 5202,  // Reads VITE_PORT or defaults to 5202
    proxy: {
      '/api': {
        target: process.env.VITE_API_PROXY_TARGET ?? 'http://localhost:8802',
        // Proxies /api requests to backend
      }
    }
  }
});
```

**Step 2: .env file (sets the port)**
```bash
# pizzeria-front-end/.env
VITE_PORT=5202
VITE_API_PROXY_TARGET=http://localhost:8802  # Backend URL for API proxy
```

**Step 3: script/run.sh (exports environment variables)**
```bash
# Reads .env file using safe parsing
if [ -f .env ]; then
    while IFS='=' read -r key value; do
        if [[ "$key" =~ ^(VITE_PORT|PORT)$ ]]; then
            export "$key"="$value"  # Exports VITE_PORT=5202
        fi
    done < .env
fi

PORT=${PORT:-5173}  # Fallback (not used because VITE_PORT is set)

# Start Vite (reads process.env.VITE_PORT)
nohup npm run dev > frontend.log 2>&1 &
```

**Result:** Frontend runs on **5202** (from .env via vite.config.ts)

### Frontend URLs

With `VITE_PORT=5202`:
- Frontend: `http://localhost:5202`
- Default Pizzeria: `http://localhost:5202/ramonamalmo`

---

## API Proxy Configuration

The frontend needs to communicate with the backend API. Since they run on different ports, Vite's dev server proxies API requests.

### How API Proxy Works

1. **Frontend makes request:** `fetch('/api/v1/pizzerias/ramonamalmo')`
2. **Vite intercepts:** Sees request starts with `/api`
3. **Vite proxies:** Forwards to `http://localhost:8802/api/v1/pizzerias/ramonamalmo`
4. **Backend responds:** Spring Boot handles request on port 8802
5. **Vite returns response:** Frontend receives data

### Configuration

```bash
# pizzeria-front-end/.env
VITE_API_PROXY_TARGET=http://localhost:8802  # Must match backend port!
```

```typescript
// pizzeria-front-end/vite.config.ts
proxy: {
  '/api': {
    target: process.env.VITE_API_PROXY_TARGET ?? 'http://localhost:8802',
    changeOrigin: true,
    secure: false
  }
}
```

**Important:** `VITE_API_PROXY_TARGET` must match the backend's actual port (`SERVER_PORT`).

---

## Why This Configuration Pattern?

### 1. Separation of Defaults and Overrides

- **Defaults in code** (application.yaml, vite.config.ts):
  - Good for new developers
  - Committed to git
  - Provide sensible fallbacks

- **Local overrides in .env**:
  - Per-developer customization
  - NOT committed to git (.gitignored)
  - Allow port customization without code changes

### 2. Avoid Port Conflicts

- **Original defaults:** 9900 (backend), 5173 (frontend)
- **Current overrides:** 8802 (backend), 5202 (frontend)
- Allows running multiple projects on same machine without conflicts

### 3. Shell Script Integration

- Scripts read `.env` files for consistent configuration
- Pass values as command-line args (backend) or environment variables (frontend)
- Ensures services start with correct ports

### 4. Environment Consistency

- Development, testing, and production can use different ports
- `.env` files kept out of git prevent accidental port conflicts in team environments
- Easy to change without touching source code

---

## How to Change Ports

### Backend Port

**Edit `pizzeria-service/.env`:**
```bash
SERVER_PORT=8900  # Change to your desired port
```

**Restart backend:**
```bash
cd pizzeria-service
./script/restart.sh
```

### Frontend Port

**Edit `pizzeria-front-end/.env`:**
```bash
VITE_PORT=5300  # Change to your desired port
VITE_API_PROXY_TARGET=http://localhost:8900  # Must match backend port!
```

**Restart frontend:**
```bash
cd pizzeria-front-end
./script/restart.sh
```

### Important Notes

1. **Keep ports in sync:** Frontend's `VITE_API_PROXY_TARGET` must match backend's `SERVER_PORT`
2. **Avoid privileged ports:** Don't use ports 1-1023 (require root)
3. **Check for conflicts:** Use `lsof -i :<port>` to see if port is in use
4. **Restart required:** Port changes only apply after restarting services

---

## Troubleshooting

### Problem: Port already in use

```bash
# Find what's using the port
lsof -i :8802

# Kill the process
lsof -ti :8802 | xargs kill -9

# Or use the kill scripts
./pizzeria-service/script/kill.sh
./pizzeria-front-end/script/kill.sh
```

### Problem: Frontend can't reach backend

**Symptoms:**
- Network errors in browser console
- 404 or connection refused errors

**Solution:**
1. Check backend is running: `./pizzeria-service/script/status.sh`
2. Verify backend port: `cat pizzeria-service/.env | grep SERVER_PORT`
3. Verify frontend proxy: `cat pizzeria-front-end/.env | grep VITE_API_PROXY_TARGET`
4. Ensure they match!

### Problem: "Port 5432 already in use" (Database)

```bash
# Check if another PostgreSQL is running
lsof -i :5432

# If it's from another project, stop this project's database:
cd pizzeria-service
docker-compose down

# Or change the database port in docker-compose.yml (advanced)
```

---

## Configuration Summary for New Developers

**The ports are configured using a hierarchy:**

### Backend (Spring Boot)
- **Default:** `application.yaml` has `server.port: 9900`
- **Override:** `.env` file sets `SERVER_PORT=8802`
- **Mechanism:** Shell script reads `.env` and passes `--server.port=8802` as command-line argument
- **Actual running port: 8802**

### Frontend (Vite)
- **Default:** `vite.config.ts` has fallback `5202`
- **Override:** `.env` file sets `VITE_PORT=5202`
- **Mechanism:** Vite reads `process.env.VITE_PORT` at runtime
- **Actual running port: 5202**

### API Proxy
- **Configuration:** Frontend `.env` has `VITE_API_PROXY_TARGET=http://localhost:8802`
- **Requirement:** Must match the backend's actual port
- **Purpose:** Allows frontend to make API requests during development

**To change ports:**
1. Edit the `.env` files (NOT the application.yaml or vite.config.ts)
2. Backend: Change `SERVER_PORT` in `pizzeria-service/.env`
3. Frontend: Change `VITE_PORT` and `VITE_API_PROXY_TARGET` in `pizzeria-front-end/.env`
4. Restart services: `./script/restart.sh`

---

## See Also

- [SHELL_SCRIPTS.md](SHELL_SCRIPTS.md) - Shell script documentation
- [CLAUDE.md](CLAUDE.md) - Project overview and development guide
- [TROUBLESHOOTING.md](TROUBLESHOOTING.md) - Common issues and solutions
