#!/bin/bash
set -e

echo "=== Pizzeria Frontend Setup ==="
echo ""

# Check if Node.js is installed
if ! command -v node &> /dev/null; then
    echo "ERROR: Node.js not found"
    echo ""
    echo "Install from: https://nodejs.org/"
    exit 1
fi

echo "[*] Node.js version: $(node --version)"
echo "[*] npm version: $(npm --version)"
echo ""

# Check if node_modules exists
if [ ! -d "node_modules" ]; then
    echo "[*] Installing npm dependencies..."
    npm install
    echo "[OK] Dependencies installed"
else
    echo "[OK] node_modules already exists"
fi

echo ""
echo "=== Starting Frontend Development Server ==="
echo ""

echo "[INFO] Frontend:     http://localhost:5173"
echo "[INFO] Backend API:  http://localhost:9900 (must be running)"
echo "[INFO] Pizzeria:     Ramona (ramonamalmo)"
echo ""

echo "Press Ctrl+C to stop the server"
echo ""

# Run the dev server
npm run dev
