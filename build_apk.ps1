# PowerShell script to download Gradle and compile the Android APK
$ErrorActionPreference = "Stop"

Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "  Building Android APK for P2P Drop                     " -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan

# Ensure TLS 1.2 is enabled
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$gradleVersion = "8.7"
$gradleDir = Join-Path $PSScriptRoot ".gradle_bin"
$gradleHome = Join-Path $gradleDir "gradle-$gradleVersion"
$gradleBat = Join-Path $gradleHome "bin\gradle.bat"

if (-not (Test-Path $gradleBat)) {
    if (-not (Test-Path $gradleDir)) {
        New-Item -ItemType Directory -Path $gradleDir -Force | Out-Null
    }

    $zipUrl = "https://services.gradle.org/distributions/gradle-$gradleVersion-bin.zip"
    $zipFile = Join-Path $gradleDir "gradle-$gradleVersion-bin.zip"

    Write-Host "[*] Downloading Gradle $gradleVersion distribution..." -ForegroundColor Yellow
    Invoke-WebRequest -Uri $zipUrl -OutFile $zipFile -UseBasicParsing
    Write-Host "[+] Download complete. Extracting..." -ForegroundColor Green

    Expand-Archive -Path $zipFile -DestinationPath $gradleDir -Force
    if (Test-Path $zipFile) {
        Remove-Item -Path $zipFile -Force
    }
}

if (-not (Test-Path $gradleBat)) {
    Write-Host "[-] Could not find gradle.bat at $gradleBat" -ForegroundColor Red
    exit 1
}

Write-Host "[*] Starting Gradle assembleDebug..." -ForegroundColor Cyan
& $gradleBat assembleDebug

$apkPath = Join-Path $PSScriptRoot "app\build\outputs\apk\debug\app-debug.apk"
if (Test-Path $apkPath) {
    Write-Host "`n========================================================" -ForegroundColor Green
    Write-Host "  ✅ APK Generated Successfully!                        " -ForegroundColor Green
    Write-Host "  APK Location: $apkPath" -ForegroundColor Green
    Write-Host "========================================================`n" -ForegroundColor Green
} else {
    Write-Host "`n[*] Build completed. Checking output folder..." -ForegroundColor Yellow
}
