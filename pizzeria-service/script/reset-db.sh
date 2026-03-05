#!/bin/bash
# Reset PostgreSQL database (removes all data and recreates from scratch)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/.."

echo "=== Resetting PostgreSQL Database ==="
echo "⚠️  WARNING: This will delete ALL database data!"
echo ""

if [ -f docker-compose.yml ]; then
    echo "Stopping and removing database container..."
    docker-compose down -v
    echo ""
    echo "✓ Database reset complete"
    echo ""
    echo "Start backend again with: ./script/run.sh"
else
    echo "✗ docker-compose.yml not found"
    exit 1
fi
