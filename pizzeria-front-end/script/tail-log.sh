#!/bin/bash

# Tail frontend dev server logs

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVICE_ROOT="$SCRIPT_DIR/.."

LOG_FILE="$SERVICE_ROOT/frontend.log"

echo "=== Tailing frontend logs ==="
echo ""

# Check if log file exists
if [ ! -f "$LOG_FILE" ]; then
    echo "⚠️  Log file not found: $LOG_FILE"
    echo ""
    echo "Frontend might not be running, or run.sh hasn't created the log yet."
    echo "Start frontend with: ./script/run.sh"
    echo ""

    # Check if frontend is running
    FRONTEND_PID=$(pgrep -f "vite.*pizzeria-front-end" | head -1)
    if [ -n "$FRONTEND_PID" ]; then
        echo "Note: Frontend IS running (PID: $FRONTEND_PID) but using old startup method."
        echo "Restart frontend to enable logging:"
        echo "  ./script/restart.sh"
    fi
    exit 1
fi

# Check if frontend is running
FRONTEND_PID=$(pgrep -f "vite.*pizzeria-front-end" | head -1)
if [ -n "$FRONTEND_PID" ]; then
    echo "✓ Frontend is running (PID: $FRONTEND_PID)"
else
    echo "⚠️  Frontend might not be running"
fi

echo "✓ Tailing: $LOG_FILE"
echo "Press Ctrl+C to stop"
echo ""

# Tail the log file
tail -f "$LOG_FILE"
