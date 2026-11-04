param(
  [Parameter(Mandatory = $true)]
  [int]$Port,
  [Parameter(Mandatory = $true)]
  [string]$MatchText,
  [Parameter(Mandatory = $true)]
  [string]$RootPath
)

$ErrorActionPreference = 'Stop'

try {
  $conn = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue | Select-Object -First 1
  if (-not $conn) {
    Write-Host "    Port $Port is available."
    exit 0
  }

  $proc = Get-CimInstance Win32_Process -Filter ("ProcessId = " + $conn.OwningProcess) -ErrorAction SilentlyContinue
  $name = if ($proc) { $proc.Name } else { 'unknown' }
  $cmd = if ($proc) { [string]$proc.CommandLine } else { '' }
  $text = @($name, $cmd) -join ' '
  $owned = $text.IndexOf($MatchText, [System.StringComparison]::OrdinalIgnoreCase) -ge 0 `
    -or $text.IndexOf($RootPath, [System.StringComparison]::OrdinalIgnoreCase) -ge 0

  if (-not $owned) {
    Write-Host "[ERROR] Port $Port is in use by PID $($conn.OwningProcess) ($name). It is not owned by this project, so it was not stopped."
    exit 1
  }

  Stop-Process -Id $conn.OwningProcess -Force -ErrorAction Stop
  Start-Sleep -Seconds 1

  $stillUsed = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue | Select-Object -First 1
  if ($stillUsed) {
    Write-Host "[ERROR] Port $Port is still in use after stopping PID $($conn.OwningProcess) ($name)."
    exit 1
  }

  Write-Host "    Reclaimed port $Port from PID $($conn.OwningProcess) ($name)."
  exit 0
} catch {
  Write-Host "[ERROR] Port $Port could not be inspected: $($_.Exception.Message)"
  exit 1
}
