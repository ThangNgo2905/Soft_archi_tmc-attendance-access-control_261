#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

echo "Building and starting stack from current workspace..."
docker compose build
docker compose up -d

echo ""
echo "Stack is up (local images)."
echo "  Frontend: http://localhost:5173"
echo "  Backend:  http://localhost:8080"
echo "  Swagger:  http://localhost:8080/swagger-ui.html"
echo "  Mocks:    http://localhost:4000"
