# Shell Scripts for Pizzeria Service

This document describes the shell scripts for managing the pizzeria platform (backend + frontend).

## Quick Reference

### Start All Services
```bash
./script/restart.sh
```

### Check Status
```bash
./script/status.sh
```

### Stop All Services
```bash
./script/kill.sh
```

### Monitor Logs
```bash
# Backend logs
./pizzeria-service/script/tail-log.sh

# Frontend logs
./pizzeria-front-end/script/tail-log.sh
```

---

## Script Organization

```
pizzeria-service/
├── script/                      # Root-level orchestration
│   ├── restart.sh              # Restart both services
│   ├── status.sh               # Check status of all services
│   └── kill.sh                 # Stop all services
├── pizzeria-service/           # Backend (Spring Boot)
│   └── script/
│       ├── run.sh              # Start backend
│       ├── kill.sh             # Stop backend
│       ├── restart.sh          # Restart backend
│       ├── status.sh           # Check backend status
│       └── tail-log.sh         # View backend logs
└── pizzeria-front-end/         # Frontend (React + Vite)
    └── script/
        ├── run.sh              # Start frontend
        ├── kill.sh             # Stop frontend
        ├── restart.sh          # Restart frontend
        ├── status.sh           # Check frontend status
        └── tail-log.sh         # View frontend logs
```

---

## Backend Scripts (Spring Boot)

### Start Backend
```bash
cd pizzeria-service
./script/run.sh
```

- Starts Spring Boot with `mvn spring-boot:run`
- Runs in background (detached from terminal)
- Logs to `backend.log`
- Default port: **9900** (configurable via `.env`)

### Stop Backend
```bash
cd pizzeria-service
./script/kill.sh
```

- Kills all Spring Boot processes for this project
- Uses project-specific pattern to avoid killing other projects

### Restart Backend
```bash
cd pizzeria-service
./script/restart.sh
```

- Stops backend, waits for process cleanup, then starts it again
- Includes race condition prevention (verifies old process terminated)

### Check Backend Status
```bash
cd pizzeria-service
./script/status.sh
```

- Shows if backend is running
- Displays PID, port, and URLs
- Checks if port is actually listening

### View Backend Logs
```bash
cd pizzeria-service
./script/tail-log.sh
```

- Tails `backend.log` in real-time
- Press Ctrl+C to stop

---

## Frontend Scripts (React + Vite)

### Start Frontend
```bash
cd pizzeria-front-end
./script/run.sh
```

- Installs dependencies if needed
- Starts Vite dev server with `npm run dev`
- Runs in background (detached from terminal)
- Logs to `frontend.log`
- Default port: **5173** (configurable via `.env`)

### Stop Frontend
```bash
cd pizzeria-front-end
./script/kill.sh
```

- Kills all Vite processes for this project
- Uses both pattern matching and working directory checking

### Restart Frontend
```bash
cd pizzeria-front-end
./script/restart.sh
```

- Stops frontend, waits for process cleanup, then starts it again

### Check Frontend Status
```bash
cd pizzeria-front-end
./script/status.sh
```

- Shows if frontend is running
- Displays PID, port, and URL

### View Frontend Logs
```bash
cd pizzeria-front-end
./script/tail-log.sh
```

- Tails `frontend.log` in real-time
- Press Ctrl+C to stop

---

## Root-Level Scripts

### Restart All Services
```bash
./script/restart.sh
```

- Restarts backend first, then frontend
- Sequential restart (not parallel) to prevent resource contention
- Error checking: exits immediately if any service fails
- Shows final status

### Check All Services
```bash
./script/status.sh
```

- Shows status of both backend and frontend
- Displays URLs for quick access
- Summary of which services are running

### Stop All Services
```bash
./script/kill.sh
```

- Stops both backend and frontend
- Useful for complete shutdown

---

## Configuration

### Backend Port (.env in pizzeria-service/)
```bash
SERVER_PORT=9900
```

### Frontend Port (.env in pizzeria-front-end/)
```bash
VITE_PORT=5173
# or
PORT=5173
```

---

## Key Features

### Security
- **Command Injection Prevention**: Safe `.env` parsing with whitelist approach
- **Port Validation**: Validates numeric values from `.env`
- **Project Isolation**: Process patterns include project name to avoid killing other projects

### Robustness
- **Race Condition Prevention**: Waits for process termination before restarting
- **Fail Fast**: Exits with error if old process won't die
- **Terminal Detachment**: Processes survive terminal close (`nohup ... &`)

### Usability
- **Visual Indicators**: ✓/✗ for quick status scanning
- **Error Messages**: Clear feedback when things go wrong
- **Log Access**: Easy log tailing with dedicated scripts

---

## Typical Workflow

### Initial Startup
```bash
# From project root
./script/restart.sh
./script/status.sh
```

### During Development
```bash
# Restart just the backend after code changes
cd pizzeria-service
./script/restart.sh

# Or restart just the frontend
cd pizzeria-front-end
./script/restart.sh
```

### Monitoring
```bash
# Watch backend logs
./pizzeria-service/script/tail-log.sh

# In another terminal, watch frontend logs
./pizzeria-front-end/script/tail-log.sh
```

### Shutdown
```bash
./script/kill.sh
```

---

## Troubleshooting

### "Address already in use" error
- Old process not fully terminated
- Run `./script/kill.sh` then `./script/restart.sh`
- Check status with `./script/status.sh`

### Backend not starting
- Check if PostgreSQL is running: `docker-compose ps`
- Start database: `cd pizzeria-service && docker-compose up -d`
- View logs: `./pizzeria-service/script/tail-log.sh`

### Frontend not starting
- Check if dependencies are installed: `cd pizzeria-front-end && npm install`
- View logs: `./pizzeria-front-end/script/tail-log.sh`

### Process won't die
- Find PID: `./script/status.sh`
- Force kill: `kill -9 <PID>`
- Or use system tools: `pkill -9 -f "spring-boot:run.*pizzeria-service"`

---

## Log Files

Both services write logs to their respective directories:

- **Backend**: `pizzeria-service/backend.log`
- **Frontend**: `pizzeria-front-end/frontend.log`

These files are automatically added to `.gitignore` and should not be committed.

---

## Integration with Existing Workflows

These scripts complement the existing tools:

### Development
- Use `mvn spring-boot:run` directly → Use `./script/run.sh` for background execution
- Use `npm run dev` directly → Use `./script/run.sh` for background execution

### Testing
- Run tests with `mvn test` (no script needed)
- Run frontend tests with `npm run test` (no script needed)

### Production
- Use `mvn -Pprod jib:build` for container builds (no script needed)

---

## Pattern Source

This script organization follows the pattern documented in:
`/home/marcus/marcusfromsweden/code/evolutionary-iimoijs/SHELL_SCRIPT_ORGANIZATION.md`

Key patterns applied:
- Safe `.env` parsing to prevent command injection
- Project-specific process patterns
- Race condition prevention in restarts
- Terminal-detached background processes
- Proper working directory management
