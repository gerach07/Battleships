@echo off
REM ═══════════════════════════════════════════════════════════════════════════
REM  AB BATTLESHIPS — Windows Installer Builder (.exe via NSIS)
REM ═══════════════════════════════════════════════════════════════════════════
REM  This script:
REM    1. Checks prerequisites (Node.js, npm)
REM    2. Generates PNG icons + .ico from icon.svg
REM    3. Builds the React web client
REM    4. Copies the build into the Electron renderer/ directory
REM    5. Installs Electron dependencies
REM    6. Packages everything into a Windows installer .exe via NSIS
REM
REM  Usage (run from the Battleships-windows directory):
REM    build-installer.bat
REM
REM  Prerequisites:
REM    - Node.js >= 18  (https://nodejs.org)
REM    - ImageMagick or Inkscape (for icon generation)
REM
REM  Output:
REM    dist\ABBattleships-Setup-1.0.0.exe
REM
REM  Install:
REM    Double-click dist\ABBattleships-Setup-1.0.0.exe
REM    or run silently: ABBattleships-Setup-1.0.0.exe /S
REM ═══════════════════════════════════════════════════════════════════════════

setlocal enabledelayedexpansion
set "SCRIPT_DIR=%~dp0"
set "CLIENT_DIR=%SCRIPT_DIR%..\Battleships-web\client"

echo.
echo +=======================================================+
echo ^|       AB BATTLESHIPS — Windows Installer Builder     ^|
echo +=======================================================+
echo.

REM ── Step 0: Check Node.js ────────────────────────────────
echo [0/5] Checking prerequisites...
where node >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Node.js not found!
    echo        Download from https://nodejs.org ^(v18 or newer^)
    exit /b 1
)

for /f "tokens=1 delims=v." %%V in ('node -v') do set "NODE_MAJOR=%%V"
for /f "tokens=2 delims=v." %%V in ('node -v') do set "NODE_MAJOR=%%V"
where npm >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: npm not found!
    exit /b 1
)
echo    OK — Node.js and npm found.
echo.

REM ── Step 1: Generate icons ───────────────────────────────
echo [1/5] Generating icons...
call "%SCRIPT_DIR%assets\generate-icons.bat"
if %errorlevel% neq 0 (
    echo ERROR: Icon generation failed. See above for details.
    exit /b 1
)
echo.

REM ── Step 2: Build React client ───────────────────────────
echo [2/5] Building React web client...
pushd "%CLIENT_DIR%"
if not exist "node_modules\" (
    echo    Installing client dependencies...
    call npm install --legacy-peer-deps
)
echo    Running production build...
set "PUBLIC_URL=./"
call npm run build
if %errorlevel% neq 0 (
    echo ERROR: React build failed!
    popd
    exit /b 1
)
popd
echo    OK — React client built.
echo.

REM ── Step 3: Copy client to renderer ─────────────────────
echo [3/5] Copying client build to Electron renderer...
pushd "%SCRIPT_DIR%"
node scripts\copy-client.js
if %errorlevel% neq 0 (
    echo ERROR: Failed to copy client build!
    popd
    exit /b 1
)
popd
echo.

REM ── Step 4: Install Electron dependencies ────────────────
echo [4/5] Installing Electron dependencies...
pushd "%SCRIPT_DIR%"
if not exist "node_modules\" (
    call npm install
    if %errorlevel% neq 0 (
        echo ERROR: npm install failed!
        popd
        exit /b 1
    )
)
popd
echo    OK — Dependencies installed.
echo.

REM ── Step 5: Build Windows installer ─────────────────────
echo [5/5] Building Windows installer with electron-builder...
pushd "%SCRIPT_DIR%"
REM Disable code signing — no certificate needed for personal/local use
set CSC_IDENTITY_AUTO_DISCOVERY=false
call node node_modules\.bin\electron-builder --win nsis
if %errorlevel% neq 0 (
    echo ERROR: electron-builder failed!
    popd
    exit /b 1
)
popd

echo.
echo +=======================================================+
echo ^|              BUILD COMPLETE!                         ^|
echo +=======================================================+
echo.

set "DIST_DIR=%SCRIPT_DIR%dist\"
for %%F in ("%DIST_DIR%*.exe") do (
    echo  Installer: dist\%%~nxF
    echo.
    echo  To install:
    echo    Double-click:   dist\%%~nxF
    echo    Silent install: dist\%%~nxF /S
    echo.
    echo  To uninstall:
    echo    Control Panel ^> Programs ^> AB Battleships ^> Uninstall
    echo    or: "%%ProgramFiles%%\AB Battleships\Uninstall AB Battleships.exe"
)

endlocal
