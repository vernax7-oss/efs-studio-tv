# © EFS Studio
# Disables telemetry on an Android TV (package list verified on Sony BRAVIA)
# and gives the Play Store and Cast full background access.
# Kept: only what the Play Store needs — Google Play services (gms) and Google Services Framework (gsf).
# Everything is reversible: undo commands are written to local\undo-telemetry-*.txt.
#
# Usage:
#   .\tools\telemetry.ps1 -Dev 192.168.1.50:5555
#
# Packages that are not on the given TV are skipped ("n/a").
param(
  [Parameter(Mandatory = $true)][string]$Dev,
  [string]$Adb = ""
)
. "$PSScriptRoot\common.ps1"
$Adb = Find-Adb $Adb
Assert-Device $Adb $Dev

$log = Join-Path $LocalDir "undo-telemetry-$(Get-Date -Format 'yyyyMMdd-HHmm').txt"
"# Undo commands (run them one by one in PowerShell; set `$adb = path to adb.exe first):" | Out-File $log -Encoding utf8

function Disable($pkg) {
  if (-not (Test-Package $Adb $Dev $pkg)) { Write-Host ("  n/a  {0}" -f $pkg); return }
  $r = & $Adb -s $Dev shell "pm disable-user --user 0 $pkg" 2>&1
  if ($r -match "disabled-user") {
    Write-Host ("  OFF  {0}" -f $pkg)
    "& `$adb -s $Dev shell `"pm enable $pkg`"" | Out-File $log -Append -Encoding utf8
  } else {
    Write-Host ("  --   {0}  ({1})" -f $pkg, ($r -join ' ').Trim())
  }
}

function AllowBackground($pkg) {
  if (-not (Test-Package $Adb $Dev $pkg)) { Write-Host ("  n/a  {0}" -f $pkg); return }
  & $Adb -s $Dev shell "appops set $pkg RUN_IN_BACKGROUND allow" 2>&1 | Out-Null
  & $Adb -s $Dev shell "appops set $pkg RUN_ANY_IN_BACKGROUND allow" 2>&1 | Out-Null
  & $Adb -s $Dev shell "am set-standby-bucket $pkg active" 2>&1 | Out-Null
  Write-Host ("  BG+  {0}" -f $pkg)
}

# Telemetry: crash/bug reports, usage and viewing data collection.
# Sony's native IDD daemons (iddd, idd-logreader) cannot be stopped without root —
# we disable the agent that collects and uploads their data.
$telemetry = @(
  "com.google.android.feedback",           # Google: sends app crash reports
  "com.google.android.tv.bugreportsender", # Google: sends system bug reports
  "com.sony.dtv.sonybugreportsys",         # Sony: system bug reports
  "com.sonyericsson.idd.agent",            # Sony IDD: TV usage data
  "com.sony.dtv.usercontext"               # Sony: viewing context (what and when you watch)
)

# Not touched: Google Play services (gms), Google Services Framework (gsf) — required by the Play Store;
# com.sony.dtv.privacypolicy (consents), da.service / Qterics (system updates), MediaTek packages.

Write-Host "== telemetry =="
foreach ($p in $telemetry) { Disable $p }

Write-Host "== Play Store and Cast: full background access =="
AllowBackground "com.android.vending"
AllowBackground "com.google.android.apps.mediashell"

Write-Host ""
Write-Host "Do this manually in the TV menu (ADB cannot change it):"
Write-Host "  - Google: Usage & diagnostics -> off"
Write-Host "  - Sony: Privacy policy / data collection consents -> withdraw"
Write-Host ("Undo file: " + $log)
