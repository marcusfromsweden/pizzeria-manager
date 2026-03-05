#!/bin/bash

# Kill any existing frontend (Vite dev server) processes for this project

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "Killing frontend processes..."

# Kill vite processes running in this specific project directory
# More specific pattern to avoid killing vite processes from other projects
pkill -f "vite.*pizzeria-front-end" || true

# Also try to kill by searching for processes with cwd in this directory
pgrep -f "vite" | while read pid; do
    if [ -n "$pid" ]; then
        # Check if process is running in our project directory
        if readlink /proc/$pid/cwd 2>/dev/null | grep -q "$PROJECT_DIR"; then
            kill $pid 2>/dev/null || true
        fi
    fi
done

echo "Frontend processes killed."
