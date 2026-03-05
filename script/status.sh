#!/bin/bash
# Check status of both backend and frontend services

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "========================================="
echo "   Pizzeria Service Status"
echo "========================================="
echo ""

# Check backend status
(cd "$PROJECT_ROOT/pizzeria-service" && script/status.sh)
echo ""

# Check frontend status
(cd "$PROJECT_ROOT/pizzeria-front-end" && script/status.sh)
echo ""

echo "========================================="
echo "   Summary"
echo "========================================="
BACKEND_PID=$(pgrep -f "spring-boot:run.*pizzeria-service")
FRONTEND_PID=$(pgrep -f "vite.*pizzeria-front-end")

if [ -n "$BACKEND_PID" ] && [ -n "$FRONTEND_PID" ]; then
    echo "✓ All services running"
    echo ""
    echo "Backend:  http://localhost:9900"
    echo "Frontend: http://localhost:5173"
    echo "Swagger:  http://localhost:9900/swagger-ui.html"
elif [ -n "$BACKEND_PID" ]; then
    echo "⚠ Only backend running"
elif [ -n "$FRONTEND_PID" ]; then
    echo "⚠ Only frontend running"
else
    echo "✗ No services running"
    echo ""
    echo "Start all services with: ./script/restart.sh"
fi
