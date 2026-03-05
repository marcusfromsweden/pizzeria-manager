#!/bin/bash
# Kill both backend and frontend services

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "========================================="
echo "   Killing All Services"
echo "========================================="
echo ""

# Kill backend
echo "Killing backend..."
(cd "$PROJECT_ROOT/pizzeria-service" && script/kill.sh)
echo ""

# Kill frontend
echo "Killing frontend..."
(cd "$PROJECT_ROOT/pizzeria-front-end" && script/kill.sh)
echo ""

echo "========================================="
echo "   All Services Stopped"
echo "========================================="
