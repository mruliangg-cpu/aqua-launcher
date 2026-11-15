@echo off
chcp 65001 >nul
title Aqua Launcher - build exe
cd /d "%~dp0launcher"
where npm >nul 2>nul || (echo Установите Node.js LTS: https://nodejs.org & pause & exit /b 1)
if not exist resources\aqua-client-1.21.1.jar echo [!] Aqua Client ещё не собран - сначала запустите build-mod.bat
call npm install
call npm run dist
echo.
echo Готово! Установщик и portable-версия лежат в launcher\dist
pause
