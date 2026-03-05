#!/bin/bash
# Check frontend dev server status

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVICE_ROOT="$SCRIPT_DIR/.."

echo "=== Frontend Dev Server Status ==="

# Check if vite process is running for this project
PID=$(pgrep -f "vite.*pizzeria-front-end")

if [ -n "$PID" ]; then
    echo "✓ Frontend is RUNNING"
    echo "  PID: $PID"

    # Check port from .env or use default (safe parsing)
    if [ -f "$SERVICE_ROOT/.env" ]; then
        PORT=$(grep -E "^(VITE_PORT|PORT)=" "$SERVICE_ROOT/.env" | head -1 | cut -d '=' -f2 | tr -d '[:space:]' | tr -d '"' | tr -d "'")
        # Validate PORT is numeric
        if ! [[ "$PORT" =~ ^[0-9]+$ ]]; then
            PORT=""
        fi
    fi
    PORT=${PORT:-5173}

    echo "  Port: $PORT"
    echo "  URL: http://localhost:$PORT"

    # Check if port is listening
    if lsof -Pi :$PORT -sTCP:LISTEN -t >/dev/null 2>&1; then
        echo "  Status: Listening on port $PORT"
    else
        echo "  Warning: Process running but not listening on port $PORT (may still be starting)"
    fi
else
    echo "✗ Frontend is NOT running"
fi
