#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
docker compose -f docker-compose.edge.yml up --build -d
echo 'Edge Kiosk: http://localhost:8090'
