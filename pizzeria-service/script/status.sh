#!/bin/bash
# Check backend server status

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVICE_ROOT="$SCRIPT_DIR/.."

# Check database status
echo "=== PostgreSQL Database Status ==="
cd "$SERVICE_ROOT"
if [ -f docker-compose.yml ]; then
    DB_STATUS=$(docker-compose ps -q postgres 2>/dev/null)
    if [ -n "$DB_STATUS" ]; then
        DB_RUNNING=$(docker inspect -f '{{.State.Running}}' $(docker-compose ps -q postgres) 2>/dev/null)
        if [ "$DB_RUNNING" = "true" ]; then
            echo "✓ Database is RUNNING"
            echo "  Container: postgres"
            echo "  Port: 5432"
        else
            echo "✗ Database container exists but is NOT running"
        fi
    else
        echo "✗ Database is NOT running"
    fi
else
    echo "⚠ docker-compose.yml not found"
fi

echo ""
echo "=== Backend Server Status ==="

# Check if Spring Boot process is running
PID=$(pgrep -f "spring-boot:run.*pizzeria-service")

if [ -n "$PID" ]; then
    echo "✓ Backend is RUNNING"
    echo "  PID: $PID"

    # Check port from .env or use default (safe parsing)
    if [ -f "$SERVICE_ROOT/.env" ]; then
        PORT=$(grep "^SERVER_PORT=" "$SERVICE_ROOT/.env" | cut -d '=' -f2 | tr -d '[:space:]' | tr -d '"' | tr -d "'")
        # Validate PORT is numeric
        if ! [[ "$PORT" =~ ^[0-9]+$ ]]; then
            PORT=""
        fi
    fi
    PORT=${PORT:-9900}

    echo "  Port: $PORT"
    echo "  URL: http://localhost:$PORT"
    echo "  Swagger UI: http://localhost:$PORT/swagger-ui.html"

    # Check if port is listening
    if lsof -Pi :$PORT -sTCP:LISTEN -t >/dev/null 2>&1; then
        echo "  Status: Listening on port $PORT"
    else
        echo "  Warning: Process running but not listening on port $PORT (may still be starting)"
    fi
else
    echo "✗ Backend is NOT running"
fi
