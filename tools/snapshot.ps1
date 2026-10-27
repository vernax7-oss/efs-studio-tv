# © EFS Studio
# Snapshot of the TV state BEFORE any change: packages, disabled packages, settings, home screen,
# Wi-Fi, power. Written to local\state-before-<date> (git-ignored — it contains network data).
#
# Usage:
#   .\tools\snapshot.ps1 -Dev 192.168.1.50:5555
param(
  [Parameter(Mandatory = $true)][string]$Dev,
  [string]$Adb = ""
)
$ErrorActionPreference = "Continue"
. "$PSScriptRoot\common.ps1"
$Adb = Find-Adb $Adb
Assert-Device $Adb $Dev
$dir = Join-Path $LocalDir "state-before-$(Get-Date -Format 'yyyyMMdd-HHmm')"
New-Item -ItemType Directory -Force -Path $dir | Out-Null

function Save-Sh {
  param([string]$Name, [string]$Cmd)
  $path = Join-Path $dir $Name
  $out = & $Adb -s $Dev shell $Cmd 2>&1
  $out | Out-File -FilePath $path -Encoding utf8
  Write-Host ("{0,-30} {1,6} lines" -f $Name, ($out | Measure-Object).Count)
}

Save-Sh "01-device.txt"            "getprop"
Save-Sh "02-packages.txt"          "pm list packages -f"
Save-Sh "03-packages-disabled.txt" "pm list packages -d"
Save-Sh "04-settings-global.txt"   "settings list global"
Save-Sh "05-settings-secure.txt"   "settings list secure"
Save-Sh "06-settings-system.txt"   "settings list system"
Save-Sh "07-home-current.txt"      "cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME"
Save-Sh "08-home-all.txt"          "cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME"
Save-Sh "09-animations.txt"        "settings get global window_animation_scale; settings get global transition_animation_scale; settings get global animator_duration_scale"
Save-Sh "10-memory.txt"            "dumpsys meminfo | head -60; echo ---; df -h /data /cache"
Save-Sh "11-wifi.txt"              "cmd wifi status; echo ---; settings list global | grep -i wifi"
Save-Sh "12-power.txt"             "dumpsys power | head -80"
Save-Sh "13-display.txt"           "wm size; wm density"
Save-Sh "14-background-ops.txt"    "for p in `$(pm list packages -3 | cut -d: -f2); do echo `$p `$(cmd appops get `$p RUN_ANY_IN_BACKGROUND); done"

Write-Host ""
Write-Host "Saved to: $dir"
