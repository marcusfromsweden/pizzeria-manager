#!/bin/bash

# Tail backend service logs

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVICE_ROOT="$SCRIPT_DIR/.."

LOG_FILE="$SERVICE_ROOT/backend.log"

echo "=== Tailing backend logs ==="
echo ""

# Check if log file exists
if [ ! -f "$LOG_FILE" ]; then
    echo "⚠️  Log file not found: $LOG_FILE"
    echo ""
    echo "Backend might not be running, or run.sh hasn't created the log yet."
    echo "Start backend with: ./script/run.sh"
    echo ""

    # Check if backend is running
    BACKEND_PID=$(pgrep -f "spring-boot:run.*pizzeria-service" | head -1)
    if [ -n "$BACKEND_PID" ]; then
        echo "Note: Backend IS running (PID: $BACKEND_PID) but using old startup method."
        echo "Restart backend to enable logging:"
        echo "  ./script/restart.sh"
    fi
    exit 1
fi

# Check if backend is running
BACKEND_PID=$(pgrep -f "spring-boot:run.*pizzeria-service" | head -1)
if [ -n "$BACKEND_PID" ]; then
    echo "✓ Backend is running (PID: $BACKEND_PID)"
else
    echo "⚠️  Backend might not be running"
fi

echo "✓ Tailing: $LOG_FILE"
echo "Press Ctrl+C to stop"
echo ""

# Tail the log file
tail -f "$LOG_FILE"
