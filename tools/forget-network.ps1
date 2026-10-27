# © EFS Studio
# Removes saved Wi-Fi networks from Android TV 10 without root. Useful for the "no internet after
# waking up" problem: the TV may jump to an old saved network (e.g. one with a static IP from another home).
#
# Why this way:
#   - `cmd wifi forget-network` requires root on Android 10,
#   - an app cannot do it: WifiManager.forgetNetwork(int) does not exist in API 29.
#   - The shell user (uid 2000) has CHANGE_WIFI_STATE, so the binder method can be called directly:
#     IWifiManager.removeNetwork(int, String).
#
# NOTE: transaction number 17 is valid ONLY for Android 10 (IWifiManager.aidl from android-10.0.0_r1).
# On any other Android version this script refuses to run — the number is different there.
#
# Usage:
#   .\tools\forget-network.ps1 -Dev 192.168.1.50:5555 -List       # shows saved networks (ID and SSID)
#   .\tools\forget-network.ps1 -Dev 192.168.1.50:5555 -Ids 3,7    # removes networks with these IDs
param(
  [Parameter(Mandatory = $true)][string]$Dev,
  [int[]]$Ids = @(),
  [switch]$List,
  [string]$Adb = ""
)
. "$PSScriptRoot\common.ps1"
$Adb = Find-Adb $Adb
Assert-Device $Adb $Dev

$sdk = ((& $Adb -s $Dev shell "getprop ro.build.version.sdk") -join '').Trim()
if ($sdk -ne "29") { throw "This script only works on Android 10 (API 29). This TV has API $sdk." }

if ($List -or $Ids.Count -eq 0) {
  Write-Host "Saved networks:"
  & $Adb -s $Dev shell "dumpsys wifi | grep -E '^ID: [0-9]+ SSID'" 2>&1
  if ($Ids.Count -eq 0) { return }
}

foreach ($id in $Ids) {
  $r = & $Adb -s $Dev shell "service call wifi 17 i32 $id s16 com.android.shell" 2>&1
  $ok = ($r -join ' ') -match "00000001"
  if ($ok) { Write-Host ("ID {0,3}: removed" -f $id) }
  else { Write-Host ("ID {0,3}: ERROR -> {1}" -f $id, ($r -join ' ')) }
}
