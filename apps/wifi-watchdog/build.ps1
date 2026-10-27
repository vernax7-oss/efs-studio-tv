# © EFS Studio
# Builds EFS Wi-Fi Watchdog -> apps\wifi-watchdog\build\app.apk
# targetSdk 28 is intentional: setWifiEnabled() only works for apps targeting API 28 or lower.
& (Join-Path $PSScriptRoot "..\..\tools\build-apk.ps1") -App "apps\wifi-watchdog" -TargetSdk 28 @args
