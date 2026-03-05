#!/bin/bash
# Restart both backend and frontend services

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "========================================="
echo "   Restarting All Services"
echo "========================================="
echo ""

# Restart backend first with error checking
echo "=== Restarting Backend ==="
if ! (cd "$PROJECT_ROOT/pizzeria-service" && script/restart.sh); then
    echo "ERROR: Backend restart failed"
    exit 1
fi
echo ""

# Wait between services to prevent resource contention
sleep 1

# Restart frontend with error checking
echo "=== Restarting Frontend ==="
if ! (cd "$PROJECT_ROOT/pizzeria-front-end" && script/restart.sh); then
    echo "ERROR: Frontend restart failed"
    exit 1
fi
echo ""

# Wait for both services to be fully listening
echo "Waiting for services to start..."
sleep 3

echo "========================================="
echo "   Restart Complete"
echo "========================================="
echo ""

# Show final status
"$SCRIPT_DIR/status.sh"

echo ""
echo "Monitor backend logs with:  ./pizzeria-service/script/tail-log.sh"
echo "Monitor frontend logs with: ./pizzeria-front-end/script/tail-log.sh"
