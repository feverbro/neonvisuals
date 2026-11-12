@echo off
setlocal EnableExtensions
cd /d "%~dp0"
title NeonVisuals - Build

cls
echo ============================================================
echo                 NEONVISUALS BUILD
echo ============================================================
echo.
echo Project folder: %CD%
echo Build log: build-log.txt
echo.

if not exist "%~dp0gradlew.bat" (
  echo ERROR: gradlew.bat was not found.
  echo Make sure you extracted the entire ZIP archive first.
  goto failed
)

where java >nul 2>&1
if errorlevel 1 (
  echo ERROR: Java was not found on this computer.
  echo Install Java JDK 21, then run build.bat again.
  echo Download: https://adoptium.net/temurin/releases/?version=21
  goto failed
)

echo Java found:
java -version 2>&1
echo.
echo Starting Gradle. The first build downloads Gradle and Fabric files.
echo This can take a while depending on your internet connection.
echo.

call "%~dp0gradlew.bat" --no-daemon clean build > "%~dp0build-log.txt" 2>&1
set "BUILD_RESULT=%ERRORLEVEL%"

echo.
echo ================= BUILD OUTPUT =================
type "%~dp0build-log.txt"
echo ==================================================
echo.

if not "%BUILD_RESULT%"=="0" goto failed

if exist "%~dp0build\libs\neonvisuals-1.0.0.jar" (
  echo SUCCESS! Your mod JAR is here:
  echo %~dp0build\libs\neonvisuals-1.0.0.jar
  start "" explorer "%~dp0build\libs"
) else (
  echo Gradle finished, but the expected JAR was not found.
  goto failed
)

echo.
echo Press any key to close this window.
pause >nul
exit /b 0

:failed
echo.
echo BUILD DID NOT FINISH.
echo Send the file build-log.txt to ChatGPT so the error can be fixed.
echo.
echo Press any key to close this window.
pause >nul
exit /b 1
