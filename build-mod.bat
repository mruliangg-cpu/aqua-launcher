@echo off
chcp 65001 >nul
title Aqua Client - build
cd /d "%~dp0aqua-client"
echo === Сборка Aqua Client (Fabric 1.21.1) ===
echo Первая сборка скачивает Minecraft и зависимости - это 3-10 минут.
call gradlew.bat build --no-daemon
if errorlevel 1 (
  echo.
  echo [!] Ошибка сборки. Нужна установленная Java 21 JDK: https://adoptium.net/temurin/releases/?version=21
  pause
  exit /b 1
)
echo.
echo Готово! aqua-client-1.21.1.jar лежит в launcher\resources
pause
