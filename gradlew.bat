@echo off
setlocal EnableExtensions
set "GRADLE_VERSION=8.8"
set "BASE=%USERPROFILE%\.gradle\neonvisuals-wrapper"
set "GRADLE_HOME=%BASE%\gradle-%GRADLE_VERSION%"

if exist "%GRADLE_HOME%\bin\gradle.bat" goto run_gradle

if not exist "%BASE%" mkdir "%BASE%"
echo Downloading Gradle %GRADLE_VERSION%...
where powershell >nul 2>&1
if errorlevel 1 (
  echo ERROR: PowerShell was not found. It is required to download Gradle.
  exit /b 1
)

powershell -NoLogo -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; [Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; $base=Join-Path $env:USERPROFILE '.gradle\neonvisuals-wrapper'; $zip=Join-Path $base 'gradle-8.8-bin.zip'; Invoke-WebRequest -UseBasicParsing -Uri 'https://services.gradle.org/distributions/gradle-8.8-bin.zip' -OutFile $zip; Expand-Archive -LiteralPath $zip -DestinationPath $base -Force"
if errorlevel 1 (
  echo ERROR: Could not download or unpack Gradle 8.8.
  echo Check your internet connection and try again.
  exit /b 1
)

:run_gradle
call "%GRADLE_HOME%\bin\gradle.bat" %*
exit /b %ERRORLEVEL%
