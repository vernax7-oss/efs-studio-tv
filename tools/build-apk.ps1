# © EFS Studio
# Builds an APK without Gradle: aapt2 (resources) -> javac -> d8 -> zipalign -> apksigner.
#
# Usage (normally through apps/<app>/build.ps1):
#   .\tools\build-apk.ps1 -App apps\launcher
#
# Requirements: Android SDK (build-tools + platform android-29 or newer), JDK 17 on PATH (javac, jar, keytool).
# The SDK is looked up in: -Sdk, $env:ANDROID_HOME, $env:ANDROID_SDK_ROOT, %LOCALAPPDATA%\Android\Sdk.
#
# Signing:
#   - if keystore.properties exists in the repo root (it is git-ignored), your own key is used:
#       storeFile=keystore/my.jks
#       storePassword=...
#       keyAlias=...
#       keyPassword=...
#   - otherwise the standard Android debug key (%USERPROFILE%\.android\debug.keystore),
#     created automatically if it does not exist.
#   Note: updating an app on the TV requires the SAME key as the previous install.
#
# -RenamePackage <name>: builds a copy under another package name, so it can be installed next to
#   an existing install (e.g. one signed with a different key) for testing. Start it with
#   adb shell am start -n <name>/studio.efs.braviahome.MainActivity and uninstall it afterwards.
param(
  [Parameter(Mandatory = $true)][string]$App,
  [string]$Sdk = "",
  [int]$MinSdk = 23,
  [int]$TargetSdk = 28,
  [string]$RenamePackage = ""
)
$ErrorActionPreference = "Stop"
$repo = Split-Path -Parent $PSScriptRoot
$root = (Resolve-Path (Join-Path $repo $App)).Path
$out  = Join-Path $root "build"

# --- SDK and tools ---
$candidates = @($Sdk, $env:ANDROID_HOME, $env:ANDROID_SDK_ROOT, (Join-Path $env:LOCALAPPDATA "Android\Sdk")) |
  Where-Object { $_ -and (Test-Path (Join-Path $_ "build-tools")) }
if (-not $candidates) { throw "Android SDK not found. Set ANDROID_HOME or pass -Sdk." }
$sdkDir = @($candidates)[0]
$bt = Get-ChildItem (Join-Path $sdkDir "build-tools") -Directory |
  Sort-Object { [version]($_.Name -replace '[^0-9.].*$', '') } | Select-Object -Last 1
$platform = Get-ChildItem (Join-Path $sdkDir "platforms") -Directory |
  Where-Object { $_.Name -match '^android-(\d+)$' -and [int]$Matches[1] -ge 29 } |
  Sort-Object { [int]($_.Name -replace 'android-', '') } | Select-Object -Last 1
if (-not $bt -or -not $platform) { throw "Missing build-tools or platform android-29+ in $sdkDir (install them with sdkmanager)." }
$bt  = $bt.FullName
$jar = Join-Path $platform.FullName "android.jar"
Write-Host "SDK: $sdkDir | build-tools: $(Split-Path -Leaf $bt) | platform: $($platform.Name)"

# --- signing key ---
$props = Join-Path $repo "keystore.properties"
if (Test-Path $props) {
  $p = @{}
  Get-Content $props | Where-Object { $_ -match '^\s*([^#=]+?)\s*=\s*(.*)$' } | ForEach-Object { $p[$Matches[1]] = $Matches[2] }
  $ks = $p["storeFile"]; if (-not [System.IO.Path]::IsPathRooted($ks)) { $ks = Join-Path $repo $ks }
  $ksPass = $p["storePassword"]; $alias = $p["keyAlias"]; $keyPass = $p["keyPassword"]
  Write-Host "Signing: your own key ($ks)"
} else {
  $ks = Join-Path $env:USERPROFILE ".android\debug.keystore"
  $ksPass = "android"; $alias = "androiddebugkey"; $keyPass = "android"
  if (-not (Test-Path $ks)) {
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $ks) | Out-Null
    & keytool -genkeypair -keystore $ks -storepass android -keypass android -alias androiddebugkey `
      -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US" | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "keytool could not create the debug key" }
  }
  Write-Host "Signing: Android debug key ($ks)"
}

Write-Host "== clean =="
Remove-Item $out -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path "$out\classes", "$out\dex", "$out\gen" | Out-Null

Write-Host "== aapt2 compile + link =="
& "$bt\aapt2.exe" compile --dir "$root\res" -o "$out\res.zip"
if ($LASTEXITCODE -ne 0) { throw "aapt2 compile failed" }
$assets = @(); if (Test-Path "$root\assets") { $assets = @("-A", "$root\assets") }
if ($RenamePackage) { $assets += @("--rename-manifest-package", $RenamePackage) }
& "$bt\aapt2.exe" link -o "$out\app-unsigned.apk" -I $jar `
    --manifest "$root\AndroidManifest.xml" `
    --min-sdk-version $MinSdk --target-sdk-version $TargetSdk @assets `
    --java "$out\gen" --auto-add-overlay "$out\res.zip"
if ($LASTEXITCODE -ne 0) { throw "aapt2 link failed" }

Write-Host "== javac =="
$sources = @()
$sources += (Get-ChildItem "$root\src" -Recurse -Filter *.java | ForEach-Object { $_.FullName })
$sources += (Get-ChildItem "$out\gen" -Recurse -Filter *.java | ForEach-Object { $_.FullName })
& javac -encoding UTF-8 -source 8 -target 8 -bootclasspath $jar -d "$out\classes" $sources
if ($LASTEXITCODE -ne 0) { throw "javac failed" }

Write-Host "== d8 =="
Push-Location "$out\classes"; & jar cf "$out\classes.jar" .; Pop-Location
& "$bt\d8.bat" --lib $jar --release --output "$out\dex" "$out\classes.jar"
if ($LASTEXITCODE -ne 0) { throw "d8 failed" }
Push-Location "$out\dex"; & "$bt\aapt.exe" add "$out\app-unsigned.apk" classes.dex | Out-Null; Pop-Location

Write-Host "== zipalign + sign =="
& "$bt\zipalign.exe" -f 4 "$out\app-unsigned.apk" "$out\app-aligned.apk"
if ($LASTEXITCODE -ne 0) { throw "zipalign failed" }
& "$bt\apksigner.bat" sign --ks $ks --ks-key-alias $alias `
    --ks-pass "pass:$ksPass" --key-pass "pass:$keyPass" `
    --out "$out\app.apk" "$out\app-aligned.apk"
if ($LASTEXITCODE -ne 0) { throw "apksigner failed" }

Write-Host ""
Write-Host ("DONE: " + (Join-Path $out "app.apk") + "  (" + [math]::Round((Get-Item "$out\app.apk").Length / 1kb, 1) + " KB)")
