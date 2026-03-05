#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/.."  # Go to service root where package.json, node_modules, etc. are located

if [ ! -d "node_modules" ]; then
    echo "Installing dependencies..."
    npm install
fi

echo "Starting dev server..."
echo "Logs will be written to: frontend.log"
echo "Tail logs with: ./script/tail-log.sh"

# Load port from .env (Vite uses VITE_PORT or PORT)
PORT=""
if [ -f .env ]; then
    while IFS='=' read -r key value; do
        value=$(echo "$value" | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//' -e 's/^["'\'']//' -e 's/["'\'']$//')
        if [[ "$key" =~ ^(VITE_PORT|PORT)$ ]]; then
            export "$key"="$value"
            if [[ "$key" == "VITE_PORT" ]] || [[ "$key" == "PORT" ]]; then
                PORT="$value"
            fi
        fi
    done < <(grep -E '^(VITE_PORT|PORT)=' .env | grep -v '^[[:space:]]*#')
fi
PORT=${PORT:-5173}

echo "Frontend will start on port ${PORT}"
echo "Starting in background (detached)..."

# Start vite dev server in background, detached from terminal
nohup npm run dev > frontend.log 2>&1 &

echo "Frontend started with PID: $!"
echo "URL: http://localhost:${PORT}"
