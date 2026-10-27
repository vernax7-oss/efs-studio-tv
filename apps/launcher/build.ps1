# © EFS Studio
# Builds the EFS Studio TV launcher -> apps\launcher\build\app.apk
& (Join-Path $PSScriptRoot "..\..\tools\build-apk.ps1") -App "apps\launcher" @args
