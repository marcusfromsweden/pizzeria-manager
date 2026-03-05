#!/bin/bash
set -e

echo "=========================================="
echo "  Pizzeria Service - Backend Server"
echo "=========================================="
echo ""

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "ERROR: Maven not found"
    echo ""
    echo "Install Maven from: https://maven.apache.org/download.cgi"
    exit 1
fi

echo "[*] Maven version: $(mvn --version | head -n 1)"
echo ""

# Check if Docker is running (needed for PostgreSQL)
if ! docker ps &> /dev/null; then
    echo "[WARN] Docker is not running"
    echo "       PostgreSQL via docker-compose may not start"
    echo ""
fi

# Check if docker-compose is available
if command -v docker-compose &> /dev/null; then
    echo "[*] Checking PostgreSQL container..."
    if ! docker ps --filter "name=pizzeria-postgres" --format "{{.Names}}" | grep -q "pizzeria-postgres"; then
        echo "[*] Starting PostgreSQL via docker-compose..."
        docker-compose up -d
        echo "[OK] PostgreSQL started"
        echo "[*] Waiting for database to be ready..."
        sleep 5
    else
        echo "[OK] PostgreSQL already running"
    fi
    echo ""
fi

# Show project structure
echo "[*] Project Structure:"
echo "    src/main/java/com/pizzeriaservice/"
echo "    ├── api/dto/                  - Request/Response DTOs"
echo "    ├── service/"
echo "    │   ├── config/               - Security, OpenAPI config"
echo "    │   ├── controller/           - REST endpoints"
echo "    │   ├── service/              - Business logic"
echo "    │   ├── domain/               - Domain models"
echo "    │   ├── repository/           - Repository interfaces"
echo "    │   └── support/              - Auth, validation, etc"
echo "    ├── menu/                     - Menu entities"
echo "    ├── user/                     - User entities"
echo "    ├── feedback/                 - Feedback entities"
echo "    └── pizzascore/               - PizzaScore entities"
echo ""

echo "=========================================="
echo "  Building & Starting Backend Server"
echo "=========================================="
echo ""

echo "[*] Building project with Maven..."
echo "[INFO] This may take a moment on first run..."
echo ""
mvn clean install -DskipTests -q

echo "[OK] Build complete"
echo ""

echo "=========================================="
echo "  Starting Spring Boot Application"
echo "=========================================="
echo ""

echo "[INFO] Application: http://localhost:9900"
echo "[INFO] API Docs:    http://localhost:9900/swagger-ui.html"
echo "[INFO] OpenAPI:     http://localhost:9900/v3/api-docs"
echo ""

echo "Press Ctrl+C to stop the server"
echo ""

# Run the Spring Boot application
mvn spring-boot:run
