@echo off
rem Companyware
rem Copyright (c) 2026 Companyware contributors
rem
rem SPDX-License-Identifier: MIT
rem Licensed under the MIT License; see LICENSE in the project root.
rem This file is part of Companyware.
setlocal
cd /d "%~dp0"

if not exist "%JAVA_HOME%\bin\javac.exe" (
  echo ERROR: JAVA_HOME must point to a JDK with bin\javac.exe.
  echo Example: set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-8...
  if /I not "%~1"=="--no-pause" pause
  exit /b 1
)

echo Building and verifying Companyware Desktop from:
echo %CD%
powershell.exe -NoProfile -File ".\tools\verify-framework-release.ps1"
if errorlevel 1 (
  echo.
  echo RELEASE BUILD FAILED. No artifact is approved for publication.
  if /I not "%~1"=="--no-pause" pause
  exit /b 1
)

echo.
echo RELEASE BUILD PASSED.
echo Output: %CD%\target\release
echo JAR and EXE are inside the versioned folder and ZIP file.
if /I not "%~1"=="--no-pause" pause
exit /b 0
