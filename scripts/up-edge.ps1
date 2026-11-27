$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
docker compose -f docker-compose.edge.yml up --build -d
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host 'Edge Kiosk: http://localhost:8090'
