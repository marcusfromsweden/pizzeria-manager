#!/bin/bash

# Kill any existing backend (Spring Boot) processes for this project

echo "Killing backend processes..."

# Kill only this project's Spring Boot processes (more specific pattern)
pkill -f "spring-boot:run.*pizzeria-service" || true

# Also kill by Maven pattern for this project
pkill -f "mvn.*spring-boot:run.*pizzeria-service" || true

# Alternative: kill java processes running Spring Boot from this directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

pgrep -f "spring-boot:run" | while read pid; do
    if [ -n "$pid" ]; then
        # Check if process is running in our project directory
        if readlink /proc/$pid/cwd 2>/dev/null | grep -q "$PROJECT_DIR"; then
            kill $pid 2>/dev/null || true
        fi
    fi
done

echo "Backend processes killed."
