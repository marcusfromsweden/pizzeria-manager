#!/bin/bash
# Restart frontend dev server (kill + run)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "Restarting frontend dev server..."

# Kill existing server
"$SCRIPT_DIR/kill.sh"

# Wait for process to actually terminate (max 5 seconds)
echo "Waiting for process cleanup..."
for i in {1..10}; do
    if ! pgrep -f "vite.*pizzeria-front-end" > /dev/null 2>&1; then
        echo "Process terminated successfully"
        break
    fi
    sleep 0.5
done

# Verify process is dead
if pgrep -f "vite.*pizzeria-front-end" > /dev/null 2>&1; then
    echo "ERROR: Failed to kill old process"
    exit 1
fi

# Start server in background and detach
"$SCRIPT_DIR/run.sh" > /dev/null 2>&1 &
disown

echo "Frontend restart initiated"
