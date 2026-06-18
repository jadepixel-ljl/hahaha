param(
  [ValidateSet('local', 'mysql')]
  [string]$BackendProfile = 'local',

  [int]$BackendPort = 8080,

  [int]$FrontendPort = 5173,

  [switch]$SkipInstall,

  [switch]$NoBrowser,

  [switch]$DryRun
)

$ErrorActionPreference = 'Stop'

$RootDir = $PSScriptRoot
if (-not $RootDir) {
  $RootDir = (Get-Location).Path
}

$BackendDir = Join-Path $RootDir 'backend'
$FrontendDir = Join-Path $RootDir 'frontend'
$LogsDir = Join-Path $RootDir 'logs'
$LocalMaven = Join-Path $RootDir '.tools\apache-maven-3.9.9\bin\mvn.cmd'

function Write-Step {
  param([string]$Message)
  Write-Host "[personal-site] $Message" -ForegroundColor Cyan
}

function Write-Warn {
  param([string]$Message)
  Write-Host "[personal-site] $Message" -ForegroundColor Yellow
}

function Quote-PS {
  param([string]$Value)
  return "'" + ($Value -replace "'", "''") + "'"
}

function Get-CommandPath {
  param([string[]]$Names)

  foreach ($name in $Names) {
    $command = Get-Command $name -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($command) {
      return $command.Source
    }
  }

  return $null
}

function Get-JavaHomeFromExecutable {
  param([string]$JavaExe)

  if (-not $JavaExe -or -not (Test-Path $JavaExe)) {
    return $null
  }

  $binDir = Split-Path -Parent $JavaExe
  $javaHome = Split-Path -Parent $binDir
  if (Test-Path (Join-Path $javaHome 'bin\java.exe')) {
    return $javaHome
  }

  return $null
}

function Find-JavaHome {
  $candidates = New-Object System.Collections.Generic.List[string]

  if ($env:JAVA_HOME) {
    $candidates.Add($env:JAVA_HOME)
  }

  $pathJava = Get-CommandPath @('java.exe', 'java')
  $pathJavaHome = Get-JavaHomeFromExecutable $pathJava
  if ($pathJavaHome) {
    $candidates.Add($pathJavaHome)
  }

  $staticCandidates = @(
    "$env:USERPROFILE\Desktop\PyCharm 2025.2.3\jbr"
  )

  foreach ($candidate in $staticCandidates) {
    if ($candidate) {
      $candidates.Add($candidate)
    }
  }

  $searchRoots = @(
    "$env:USERPROFILE\Desktop",
    'C:\Program Files\JetBrains',
    'C:\Program Files\Java',
    'C:\Program Files\Eclipse Adoptium',
    'C:\Program Files\Microsoft'
  )

  foreach ($root in $searchRoots) {
    if (-not $root -or -not (Test-Path $root)) {
      continue
    }

    Get-ChildItem -Path $root -Directory -ErrorAction SilentlyContinue |
      Where-Object { $_.Name -match 'PyCharm|IntelliJ|jdk|jbr|temurin|java' } |
      ForEach-Object {
        $candidates.Add($_.FullName)
        $jbr = Join-Path $_.FullName 'jbr'
        if (Test-Path $jbr) {
          $candidates.Add($jbr)
        }
      }
  }

  foreach ($candidate in $candidates) {
    if ($candidate -and (Test-Path (Join-Path $candidate 'bin\java.exe'))) {
      return (Resolve-Path $candidate).Path
    }
  }

  return $null
}

function Find-Maven {
  if (Test-Path $LocalMaven) {
    return (Resolve-Path $LocalMaven).Path
  }

  return Get-CommandPath @('mvn.cmd', 'mvn')
}

function Test-PortListening {
  param([int]$Port)

  $connection = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
    Select-Object -First 1

  return $null -ne $connection
}

function New-EncodedCommand {
  param([string]$Command)

  $bytes = [System.Text.Encoding]::Unicode.GetBytes($Command)
  return [Convert]::ToBase64String($bytes)
}

function Start-PowerShellWindow {
  param(
    [string]$Title,
    [string]$WorkingDirectory,
    [string]$Command
  )

  $encoded = New-EncodedCommand $Command
  $arguments = @(
    '-NoExit',
    '-ExecutionPolicy', 'Bypass',
    '-EncodedCommand', $encoded
  )

  if ($DryRun) {
    Write-Step "Dry run: would start $Title in $WorkingDirectory"
    Write-Host $Command
    return
  }

  Start-Process -FilePath 'powershell.exe' -WorkingDirectory $WorkingDirectory -ArgumentList $arguments
}

function Wait-HttpOk {
  param(
    [string]$Url,
    [int]$TimeoutSeconds = 60
  )

  $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
  while ((Get-Date) -lt $deadline) {
    try {
      $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 2
      if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 500) {
        return $true
      }
    } catch {
      Start-Sleep -Seconds 2
    }
  }

  return $false
}

if (-not (Test-Path $BackendDir)) {
  throw "Backend directory not found: $BackendDir"
}

if (-not (Test-Path $FrontendDir)) {
  throw "Frontend directory not found: $FrontendDir"
}

New-Item -Path $LogsDir -ItemType Directory -Force | Out-Null

$javaHome = Find-JavaHome
if (-not $javaHome) {
  throw "JDK 17+ was not found. Install JDK 17+, or set JAVA_HOME before running this script."
}

$maven = Find-Maven
if (-not $maven) {
  throw "Maven was not found. Expected $LocalMaven, or mvn.cmd on PATH."
}

$npm = Get-CommandPath @('npm.cmd', 'npm')
if (-not $npm) {
  throw "npm was not found. Install Node.js, then run this script again."
}

Write-Step "Project: $RootDir"
Write-Step "Backend profile: $BackendProfile"
Write-Step "Java: $javaHome"
Write-Step "Maven: $maven"
Write-Step "npm: $npm"

if (-not $SkipInstall -and -not (Test-Path (Join-Path $FrontendDir 'node_modules'))) {
  Write-Step "Installing frontend dependencies..."
  Push-Location $FrontendDir
  try {
    & $npm install
  } finally {
    Pop-Location
  }
}

$backendLog = Join-Path $LogsDir 'backend.out.log'
$frontendLog = Join-Path $LogsDir 'frontend.out.log'
$mavenBin = Split-Path -Parent $maven
$javaBin = Join-Path $javaHome 'bin'
$backendAlreadyRunning = Test-PortListening $BackendPort
$frontendAlreadyRunning = Test-PortListening $FrontendPort

if ($backendAlreadyRunning) {
  Write-Warn "Port $BackendPort is already listening; backend launch skipped."
} else {
  $backendCommand = @"
`$Host.UI.RawUI.WindowTitle = 'personal-site backend'
`$ErrorActionPreference = 'Stop'
`$env:JAVA_HOME = $(Quote-PS $javaHome)
`$env:PATH = $(Quote-PS "$javaBin;$mavenBin") + ';' + `$env:PATH
`$env:SERVER_PORT = $(Quote-PS ([string]$BackendPort))
`$env:SPRING_PROFILES_ACTIVE = $(Quote-PS $BackendProfile)
Write-Host 'Starting personal-site backend...' -ForegroundColor Cyan
Write-Host 'Logs: $backendLog'
& $(Quote-PS $maven) spring-boot:run "-Dspring-boot.run.profiles=$BackendProfile" "-Dspring-boot.run.arguments=--server.port=$BackendPort" 2>&1 | Tee-Object -FilePath $(Quote-PS $backendLog) -Append
"@
  Start-PowerShellWindow -Title 'personal-site backend' -WorkingDirectory $BackendDir -Command $backendCommand
}

if ($frontendAlreadyRunning) {
  Write-Warn "Port $FrontendPort is already listening; frontend launch skipped."
} else {
  $frontendCommand = @"
`$Host.UI.RawUI.WindowTitle = 'personal-site frontend'
`$ErrorActionPreference = 'Stop'
`$env:BROWSER = 'none'
Write-Host 'Starting personal-site frontend...' -ForegroundColor Cyan
Write-Host 'Logs: $frontendLog'
& $(Quote-PS $npm) run dev -- --host 127.0.0.1 --port $FrontendPort --strictPort 2>&1 | Tee-Object -FilePath $(Quote-PS $frontendLog) -Append
"@
  Start-PowerShellWindow -Title 'personal-site frontend' -WorkingDirectory $FrontendDir -Command $frontendCommand
}

$frontendUrl = "http://127.0.0.1:$FrontendPort/"
$backendUrl = "http://127.0.0.1:$BackendPort/api/profile"

Write-Step "Backend API: $backendUrl"
Write-Step "Frontend: $frontendUrl"
Write-Step "Logs: $LogsDir"

if (-not $NoBrowser -and -not $DryRun) {
  Write-Step "Waiting for frontend before opening browser..."
  if (Wait-HttpOk $frontendUrl 90) {
    Start-Process $frontendUrl
  } else {
    Write-Warn "Frontend did not respond within 90 seconds. Check $frontendLog."
  }
}
