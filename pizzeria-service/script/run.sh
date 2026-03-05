#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/.."  # Go to service root where pom.xml is located

echo "=== Starting PostgreSQL Database ==="
if [ -f docker-compose.yml ]; then
    docker-compose up -d
    echo "Waiting for database to be ready..."
    # Wait for database health check to pass
    for i in {1..30}; do
        if docker-compose exec -T postgres pg_isready -U pizzeria -d pizzeria > /dev/null 2>&1; then
            echo "✓ Database is ready"
            break
        fi
        if [ $i -eq 30 ]; then
            echo "✗ Database failed to become ready after 30 seconds"
            exit 1
        fi
        sleep 1
    done
else
    echo "Warning: docker-compose.yml not found, assuming database is already running"
fi

echo ""
echo "=== Starting Backend (Spring Boot) ==="
echo "Logs will be written to: backend.log"
echo "Tail logs with: ./script/tail-log.sh"

# Load environment variables from .env file (BEFORE setting defaults)
# Safe parsing to prevent command injection
if [ -f .env ]; then
    while IFS='=' read -r key value; do
        # Remove leading/trailing whitespace and quotes from value
        value=$(echo "$value" | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//' -e 's/^["'\'']//' -e 's/["'\'']$//')
        # Export allowed environment variables (whitelist)
        if [[ "$key" =~ ^(SERVER_PORT|HOST|SPRING_R2DBC_URL|SPRING_R2DBC_USERNAME|SPRING_R2DBC_PASSWORD|SPRING_LIQUIBASE_URL|SPRING_LIQUIBASE_USERNAME|SPRING_LIQUIBASE_PASSWORD)$ ]]; then
            export "$key"="$value"
        fi
    done < <(grep -E '^(SERVER_PORT|HOST|SPRING_)' .env | grep -v '^[[:space:]]*#')
fi

# Apply defaults if not set in .env
PORT=${SERVER_PORT:-9900}
R2DBC_URL=${SPRING_R2DBC_URL:-r2dbc:postgresql://localhost:5432/pizzeria}
R2DBC_USER=${SPRING_R2DBC_USERNAME:-pizzeria}
R2DBC_PASS=${SPRING_R2DBC_PASSWORD:-pizzeria}
LIQUIBASE_URL=${SPRING_LIQUIBASE_URL:-jdbc:postgresql://localhost:5432/pizzeria}
LIQUIBASE_USER=${SPRING_LIQUIBASE_USERNAME:-pizzeria}
LIQUIBASE_PASS=${SPRING_LIQUIBASE_PASSWORD:-pizzeria}

echo "Server will start on port ${PORT}"
echo "Database: ${R2DBC_URL}"
echo "Starting in background (detached)..."

# Start Spring Boot in background, detached from terminal
# Pass database credentials as system properties
nohup mvn spring-boot:run \
  -Dspring-boot.run.arguments="--server.port=${PORT} --spring.r2dbc.url=${R2DBC_URL} --spring.r2dbc.username=${R2DBC_USER} --spring.r2dbc.password=${R2DBC_PASS} --spring.liquibase.url=${LIQUIBASE_URL} --spring.liquibase.user=${LIQUIBASE_USER} --spring.liquibase.password=${LIQUIBASE_PASS}" \
  > backend.log 2>&1 &

echo "Backend started with PID: $!"
echo "URL: http://localhost:${PORT}"
