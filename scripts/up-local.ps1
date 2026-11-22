#!/usr/bin/env pwsh
$ErrorActionPreference = "Stop"
Set-Location (Split-Path $PSScriptRoot -Parent)

Write-Host "Building and starting stack from current workspace..."
docker compose build
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
docker compose up -d
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host ""
Write-Host "Stack is up (local images)."
Write-Host "  Frontend: http://localhost:5173"
Write-Host "  Backend:  http://localhost:8080"
Write-Host "  Swagger:  http://localhost:8080/swagger-ui.html"
Write-Host "  Mocks:    http://localhost:4000"
