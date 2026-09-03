@echo off
title Building Android APK - P2P Drop
cd /d "%~dp0"
echo ========================================================
echo   Starting Android APK Build (Zero PowerShell)
echo ========================================================
node build_apk.js
echo.
echo Press any key to exit...
pause >nul
