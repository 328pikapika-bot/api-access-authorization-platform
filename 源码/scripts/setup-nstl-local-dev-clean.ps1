param(
  [string]$HostName = "www.nstl.gov.cn",
  [string]$IpAddress = "127.0.0.1",
  [string]$PfxPassword = $env:NSTL_PFX_PASSWORD
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$certDir = Join-Path $repoRoot "certs"
$pfxPath = Join-Path $certDir "$HostName.pfx"
$hostsPath = Join-Path $env:SystemRoot "System32\drivers\etc\hosts"
$hostsLine = "$IpAddress $HostName"
$internetSettingsPath = "HKCU:\Software\Microsoft\Windows\CurrentVersion\Internet Settings"

if (-not (Test-Path $certDir)) {
  New-Item -ItemType Directory -Path $certDir | Out-Null
}

$existingCert = Get-ChildItem Cert:\CurrentUser\My |
  Where-Object { $_.Subject -eq "CN=$HostName" } |
  Sort-Object NotAfter -Descending |
  Select-Object -First 1

if (-not $existingCert) {
  $existingCert = New-SelfSignedCertificate `
    -DnsName $HostName `
    -CertStoreLocation "Cert:\CurrentUser\My" `
    -FriendlyName "Interface Platform local NSTL certificate" `
    -NotAfter (Get-Date).AddYears(2)
}

$trustedCert = Get-ChildItem Cert:\CurrentUser\Root |
  Where-Object { $_.Thumbprint -eq $existingCert.Thumbprint } |
  Select-Object -First 1

if (-not $trustedCert) {
  $tempCerPath = Join-Path $certDir "$HostName.cer"
  Export-Certificate -Cert $existingCert -FilePath $tempCerPath | Out-Null
  Import-Certificate -FilePath $tempCerPath -CertStoreLocation "Cert:\CurrentUser\Root" | Out-Null
}

$securePassword = ConvertTo-SecureString $PfxPassword -AsPlainText -Force
Export-PfxCertificate -Cert $existingCert -FilePath $pfxPath -Password $securePassword | Out-Null

$hostsContent = Get-Content -Path $hostsPath -ErrorAction SilentlyContinue
$hostsPattern = "^\s*\S+\s+$([Regex]::Escape($HostName))(\s|$)"

if (-not ($hostsContent | Where-Object { $_ -match $hostsPattern })) {
  try {
    Add-Content -Path $hostsPath -Value $hostsLine
  } catch {
    Write-Warning "无法自动写入 hosts，请管理员手动添加：$hostsLine"
  }
}

$internetSettings = Get-ItemProperty -Path $internetSettingsPath -ErrorAction SilentlyContinue
$proxyOverride = if ($internetSettings.ProxyOverride) { [string]$internetSettings.ProxyOverride } else { "" }
$proxyItems = @()
if ($proxyOverride.Trim()) {
  $proxyItems = $proxyOverride -split ';' | Where-Object { $_.Trim() }
}

if ($proxyItems -notcontains $HostName) {
  $proxyItems += $HostName
  if ($proxyItems -notcontains '<local>') {
    $proxyItems += '<local>'
  }
  $nextProxyOverride = ($proxyItems | Select-Object -Unique) -join ';'
  Set-ItemProperty -Path $internetSettingsPath -Name ProxyOverride -Value $nextProxyOverride
}

try {
  ipconfig /flushdns | Out-Null
} catch {
  Write-Warning "无法自动刷新 DNS，请手动执行：ipconfig /flushdns"
}

Write-Host "NSTL 本地域名环境已准备完成："
Write-Host "  http://$HostName`:18300/contents"
Write-Host "  http://$HostName`:18301/login"
Write-Host "证书："
Write-Host "  $pfxPath"
