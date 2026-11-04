param(
  [string[]]$HostNames = @("www.nstl.gov.cn", "nstl.gov.cn"),
  [string]$IpAddress = "127.0.0.1",
  [string]$LoginHostName = "login.nstl.gov.cn",
  [string]$LoginIpAddress = "127.0.0.1",
  [string]$PfxPassword = $env:NSTL_PFX_PASSWORD
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$certDir = Join-Path $repoRoot "certs"
$primaryHostName = $HostNames[0]
$pfxPath = Join-Path $certDir "$primaryHostName.pfx"
$hostsPath = Join-Path $env:SystemRoot "System32\drivers\etc\hosts"
$internetSettingsPath = "HKCU:\Software\Microsoft\Windows\CurrentVersion\Internet Settings"

if (-not (Test-Path $certDir)) {
  New-Item -ItemType Directory -Path $certDir | Out-Null
}

$existingCert = Get-ChildItem Cert:\CurrentUser\My |
  Where-Object { $_.Subject -eq "CN=$primaryHostName" } |
  Sort-Object NotAfter -Descending |
  Select-Object -First 1

if (-not $existingCert) {
  $existingCert = New-SelfSignedCertificate `
    -DnsName $HostNames `
    -CertStoreLocation "Cert:\CurrentUser\My" `
    -FriendlyName "Interface Platform local NSTL certificate" `
    -NotAfter (Get-Date).AddYears(2)
}

$trustedCert = Get-ChildItem Cert:\CurrentUser\Root |
  Where-Object { $_.Thumbprint -eq $existingCert.Thumbprint } |
  Select-Object -First 1

if (-not $trustedCert) {
  $tempCerPath = Join-Path $certDir "$primaryHostName.cer"
  Export-Certificate -Cert $existingCert -FilePath $tempCerPath | Out-Null
  Import-Certificate -FilePath $tempCerPath -CertStoreLocation "Cert:\CurrentUser\Root" | Out-Null
}

$securePassword = ConvertTo-SecureString $PfxPassword -AsPlainText -Force
Export-PfxCertificate -Cert $existingCert -FilePath $pfxPath -Password $securePassword | Out-Null

$hostsContent = Get-Content -Path $hostsPath -ErrorAction SilentlyContinue
$filteredHosts = @($hostsContent)
foreach ($hostName in $HostNames) {
  $hostsPattern = "^\s*\S+\s+$([Regex]::Escape($hostName))(\s|$)"
  $filteredHosts = @($filteredHosts | Where-Object { $_ -notmatch $hostsPattern })
}
$loginHostsPattern = "^\s*\S+\s+$([Regex]::Escape($LoginHostName))(\s|$)"
$filteredHosts = @($filteredHosts | Where-Object { $_ -notmatch $loginHostsPattern })
$nextHosts = @($filteredHosts)
foreach ($hostName in $HostNames) {
  $nextHosts += "$IpAddress $hostName"
}
$nextHosts += "$LoginIpAddress $LoginHostName"

try {
  Set-Content -Path $hostsPath -Value $nextHosts
} catch {
  $lines = (($HostNames | ForEach-Object { "$IpAddress $_" }) + "$LoginIpAddress $LoginHostName") -join '; '
  Write-Warning "Could not update hosts automatically. Run as Administrator or update SwitchHosts so these mappings override other entries: $lines"
}

$internetSettings = Get-ItemProperty -Path $internetSettingsPath -ErrorAction SilentlyContinue
$proxyOverride = if ($internetSettings.ProxyOverride) { [string]$internetSettings.ProxyOverride } else { "" }
$proxyItems = @()
if ($proxyOverride.Trim()) {
  $proxyItems = $proxyOverride -split ';' | Where-Object { $_.Trim() }
}

foreach ($hostName in $HostNames) {
  if ($proxyItems -notcontains $hostName) {
    $proxyItems += $hostName
  }
}
if ($proxyItems -notcontains '<local>') {
  $proxyItems += '<local>'
}
$nextProxyOverride = ($proxyItems | Select-Object -Unique) -join ';'
Set-ItemProperty -Path $internetSettingsPath -Name ProxyOverride -Value $nextProxyOverride

try {
  ipconfig /flushdns | Out-Null
} catch {
  Write-Warning "Could not flush DNS automatically. Please run: ipconfig /flushdns"
}

Write-Host "NSTL local HTTPS host is ready:"
foreach ($hostName in $HostNames) {
  Write-Host "  https://$hostName`:18300/contents"
  Write-Host "  https://$hostName`:18301/login"
}
Write-Host "Use the HTTPS NSTL-domain URLs above for real SSO testing. Do not use localhost for that flow."
Write-Host "Login host:"
Write-Host "  $LoginIpAddress $LoginHostName"
Write-Host "Certificate:"
Write-Host "  $pfxPath"
