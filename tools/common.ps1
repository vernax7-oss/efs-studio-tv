# © EFS Studio
# Shared helpers for the scripts (included with: . "$PSScriptRoot\common.ps1").

# Folder for data about one particular TV (state before changes, undo files).
# It is git-ignored: it contains e.g. Wi-Fi network names and addresses — never commit it.
$LocalDir = Join-Path (Split-Path -Parent $PSScriptRoot) "local"
New-Item -ItemType Directory -Force -Path $LocalDir | Out-Null

# Finds adb: parameter, PATH, ANDROID_HOME, ANDROID_SDK_ROOT, %LOCALAPPDATA%\Android\Sdk.
function Find-Adb([string]$Given) {
  if ($Given -and (Test-Path $Given)) { return $Given }
  $cmd = Get-Command adb -ErrorAction SilentlyContinue
  if ($cmd) { return $cmd.Source }
  foreach ($sdk in @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT, (Join-Path $env:LOCALAPPDATA "Android\Sdk"))) {
    if (-not $sdk) { continue }
    $p = Join-Path $sdk "platform-tools\adb.exe"
    if (Test-Path $p) { return $p }
  }
  throw "adb not found. Install Android SDK Platform-Tools or pass -Adb."
}

# Checks that the TV answers over ADB.
function Assert-Device([string]$Adb, [string]$Dev) {
  $state = (& $Adb -s $Dev get-state 2>&1) -join ' '
  if ($state -notmatch '^device') {
    throw "TV $Dev is not reachable over ADB ($state). Run first: adb connect $Dev"
  }
}

# Whether a package is installed.
function Test-Package([string]$Adb, [string]$Dev, [string]$Pkg) {
  $r = & $Adb -s $Dev shell "pm list packages $Pkg" 2>&1
  return [bool](($r -split "`n") | Where-Object { $_.Trim() -eq "package:$Pkg" })
}
