@echo off
chcp 65001 >nul
title Aqua Launcher
cd /d "%~dp0launcher"
where npm >nul 2>nul || (echo Установите Node.js LTS: https://nodejs.org & pause & exit /b 1)
if not exist node_modules (
  echo Установка зависимостей - один раз...
  call npm install
)
call npm start
