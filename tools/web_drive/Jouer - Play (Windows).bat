@echo off
rem On Board in the browser, from the drive (atelier r222): a small local web server, then the page.
title On Board
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0fichiers - files\serve.ps1"
if errorlevel 1 pause
