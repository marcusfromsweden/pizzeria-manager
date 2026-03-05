#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/.."  # Go to service root where pom.xml is located

echo "Starting backend (Spring Boot)..."
echo "Logs will be written to: backend.log"
echo "Tail logs with: ./script/tail-log.sh"

# Load PORT from .env file if it exists (BEFORE setting defaults)
# Safe parsing to prevent command injection
if [ -f .env ]; then
    while IFS='=' read -r key value; do
        # Remove leading/trailing whitespace and quotes from value
        value=$(echo "$value" | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//' -e 's/^["'\'']//' -e 's/["'\'']$//')
        if [[ "$key" =~ ^(SERVER_PORT)$ ]]; then
            export "$key"="$value"
        fi
    done < <(grep -E '^SERVER_PORT=' .env | grep -v '^[[:space:]]*#')
fi

# Apply defaults if not set in .env
PORT=${SERVER_PORT:-9900}

echo "Server will start on port ${PORT}"
echo "Starting in background (detached)..."

# Start Spring Boot in background, detached from terminal
nohup mvn spring-boot:run > backend.log 2>&1 &

echo "Backend started with PID: $!"
echo "URL: http://localhost:${PORT}"
