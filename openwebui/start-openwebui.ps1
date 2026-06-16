$ErrorActionPreference = "Stop"

$dockerDesktop = "C:\Program Files\Docker\Docker\Docker Desktop.exe"
$machinePath = [System.Environment]::GetEnvironmentVariable("Path", "Machine")
$userPath = [System.Environment]::GetEnvironmentVariable("Path", "User")
$env:Path = "$machinePath;$userPath"

function Wait-DockerEngine {
  param([int] $TimeoutSeconds = 180)

  $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
  do {
    try {
      docker info --format "{{.ServerVersion}}" | Out-Host
      return
    } catch {
      Start-Sleep -Seconds 5
    }
  } while ((Get-Date) -lt $deadline)

  throw "Docker engine did not become ready within $TimeoutSeconds seconds."
}

Write-Host "Checking WSL..."
try {
  wsl --status
} catch {
  Write-Host "WSL is not ready yet. Try running this in an elevated PowerShell:"
  Write-Host "  winget install --id Microsoft.WSL --source winget --accept-package-agreements --accept-source-agreements --disable-interactivity --scope machine"
}

Write-Host "Starting Docker Desktop..."
if (Test-Path $dockerDesktop) {
  Start-Process -WindowStyle Hidden -FilePath $dockerDesktop
}

Wait-DockerEngine -TimeoutSeconds 240

Set-Location "D:\personal-site\openwebui"
Write-Host "Starting Open WebUI..."
docker compose up -d
docker compose ps

Write-Host ""
Write-Host "Open WebUI: http://127.0.0.1:3000"
Write-Host "Main site:   http://127.0.0.1:5174/#ai"
