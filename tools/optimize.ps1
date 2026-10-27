# © EFS Studio
# Optimization for Sony TVs with Android TV: disables unneeded Sony packages (hotel mode, store demos,
# promotions, on-screen help) and can optionally restrict background work of selected apps.
# Everything is reversible: undo commands are written to local\undo-optimize-*.txt.
#
# Usage:
#   .\tools\optimize.ps1 -Dev 192.168.1.50:5555
#   .\tools\optimize.ps1 -Dev 192.168.1.50:5555 -NoHbbTV -OtherRegions
#   .\tools\optimize.ps1 -Dev 192.168.1.50:5555 -RestrictBackground com.spotify.tv.android
#
# What this script NEVER touches: Play Store, Google Play services, Cast (mediashell), Assistant,
# the TV/HDMI input app (com.sony.dtv.tvx — without it HDMI shows a black screen),
# system updates, TV settings, keyboards.
param(
  [Parameter(Mandatory = $true)][string]$Dev,
  [string]$Adb = "",
  [switch]$NoHbbTV,        # HbbTV and interactive TV (red button) — only if you don't use antenna/HbbTV
  [switch]$OtherRegions,   # YouView (UK) and operator features of other countries
  [switch]$NoRemoteStart,  # Sony "seeds" (turning the TV on over the network) — also disables waking the TV via Cast
  [switch]$NoReminders,    # reminders for broadcast TV programmes
  [string[]]$RestrictBackground = @()  # packages that must not run in the background (they still work when opened)
)
. "$PSScriptRoot\common.ps1"
$Adb = Find-Adb $Adb
Assert-Device $Adb $Dev

$log = Join-Path $LocalDir "undo-optimize-$(Get-Date -Format 'yyyyMMdd-HHmm').txt"
"# Undo commands (run them one by one in PowerShell; set `$adb = path to adb.exe first):" | Out-File $log -Encoding utf8

# These packages are never restricted, even if someone passes them in -RestrictBackground.
$protected = @("com.android.vending", "com.google.android.gms", "com.google.android.gsf",
               "com.google.android.apps.mediashell", "com.google.android.katniss", "com.sony.dtv.tvx")

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

function Restrict($pkg) {
  if ($protected -contains $pkg) { Write-Host ("  SKIP {0} (protected)" -f $pkg); return }
  if (-not (Test-Package $Adb $Dev $pkg)) { Write-Host ("  n/a  {0}" -f $pkg); return }
  & $Adb -s $Dev shell "appops set $pkg RUN_IN_BACKGROUND ignore" 2>&1 | Out-Null
  & $Adb -s $Dev shell "appops set $pkg RUN_ANY_IN_BACKGROUND ignore" 2>&1 | Out-Null
  & $Adb -s $Dev shell "am set-standby-bucket $pkg rare" 2>&1 | Out-Null
  Write-Host ("  BG   {0}" -f $pkg)
  "& `$adb -s $Dev shell `"appops set $pkg RUN_IN_BACKGROUND allow`"" | Out-File $log -Append -Encoding utf8
  "& `$adb -s $Dev shell `"appops set $pkg RUN_ANY_IN_BACKGROUND allow`"" | Out-File $log -Append -Encoding utf8
  "& `$adb -s $Dev shell `"am set-standby-bucket $pkg active`"" | Out-File $log -Append -Encoding utf8
}

# 1. Hotel / B2B mode — not used at home
$b2b = @(
  "com.sony.dtv.b2b.adminpassword", "com.sony.dtv.b2b.deviceadminsettings",
  "com.sony.dtv.b2b.frameworkextension", "com.sony.dtv.b2b.hotelmode",
  "com.sony.dtv.b2b.importexport", "com.sony.dtv.b2b.noderuntime.normal",
  "com.sony.dtv.b2b.noderuntime.privilege", "com.sony.dtv.b2b.noderuntimeextension.node_20_11_1",
  "com.sony.dtv.b2b.prosettings", "com.sony.dtv.b2b.rs232csupport",
  "com.sony.dtv.b2b.softap", "com.sony.dtv.b2b.vendorprotocol",
  "com.sony.dtv.b2b.webappruntimedrm"
)

# 2. Store demos, promotions, Sony recommendations, on-screen help
$bloat = @(
  "com.sony.dtv.demosupport", "com.sony.dtv.demosystemsupport", "com.sony.dtv.multiscreendemo",
  "com.sony.dtv.promos", "com.sony.dtv.sonyselect",
  "com.sony.dtv.helpguidecontents", "com.sony.dtv.smarthelp", "com.sony.dtv.customersupport",
  "com.sony.dtv.customersupport.provider.res", "com.sony.dtv.livingfit",
  "com.sony.dtv.operatorapputil.service", "com.sony.dtv.seconddispsetting"
)

Write-Host "== 1. hotel / B2B mode =="
foreach ($p in $b2b) { Disable $p }

Write-Host "== 2. demos, promotions, help =="
foreach ($p in $bloat) { Disable $p }

if ($NoHbbTV) {
  Write-Host "== HbbTV / interactive TV =="
  foreach ($p in @("com.sony.dtv.hbbtvplayer", "com.sony.dtv.interactivetvutil", "com.sony.dtv.interactivetvutil.output")) { Disable $p }
}
if ($OtherRegions) {
  Write-Host "== other regions =="
  foreach ($p in @("com.sony.dtv.youview")) { Disable $p }
}
if ($NoRemoteStart) {
  Write-Host "== remote start (seeds) =="
  foreach ($p in @("com.sony.dtv.seeds.iot", "com.sony.dtv.seeds.iot.remotestart", "com.sony.dtv.seeds.auth")) { Disable $p }
}
if ($NoReminders) {
  Write-Host "== programme reminders =="
  Disable "com.sony.dtv.reminderservice"
}
if ($RestrictBackground.Count -gt 0) {
  Write-Host "== background restrictions =="
  foreach ($p in $RestrictBackground) { Restrict $p }
}

Write-Host ""
Write-Host ("Undo file: " + $log)
