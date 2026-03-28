@echo off
REM ─────────────────────────────────────────────────────────
REM  AB Battleships — Icon Generator (Windows)
REM  Generates PNG + .ico from icon.svg using Inkscape or
REM  ImageMagick if installed, otherwise guides the user.
REM ─────────────────────────────────────────────────────────

setlocal enabledelayedexpansion
set "SCRIPT_DIR=%~dp0"
set "SVG=%SCRIPT_DIR%icon.svg"
set "ICONS_DIR=%SCRIPT_DIR%icons"
set "SIZES=16 24 32 48 64 128 256 512"

if not exist "%SVG%" (
    echo ERROR: icon.svg not found at %SVG%
    exit /b 1
)

if not exist "%ICONS_DIR%" mkdir "%ICONS_DIR%"

REM ── Try Inkscape first ────────────────────────────────────
where inkscape >nul 2>&1
if %errorlevel% == 0 (
    echo Using Inkscape to generate icons...
    for %%S in (%SIZES%) do (
        inkscape -w %%S -h %%S "%SVG%" -o "%ICONS_DIR%\%%Sx%%S.png" 2>nul
        echo    OK: %%Sx%%S.png
    )
    goto :make_ico
)

REM ── Try ImageMagick ──────────────────────────────────────
where magick >nul 2>&1
if %errorlevel% == 0 (
    echo Using ImageMagick to generate icons...
    for %%S in (%SIZES%) do (
        magick -background none -density 300 "%SVG%" -resize %%Sx%%S "%ICONS_DIR%\%%Sx%%S.png"
        echo    OK: %%Sx%%S.png
    )
    goto :make_ico
)

echo.
echo ERROR: No icon generator found.
echo Please install one of:
echo   1. Inkscape:      https://inkscape.org/release/
echo   2. ImageMagick:   https://imagemagick.org/script/download.php#windows
echo.
echo Alternatively, convert icon.svg manually at these sizes:
echo   %SIZES%
echo and place them into: %ICONS_DIR%\NxN.png
echo Then re-run this script to build icon.ico.
exit /b 1

:make_ico
REM Copy 512px as main icon.png
copy /Y "%ICONS_DIR%\512x512.png" "%SCRIPT_DIR%icon.png" >nul
echo Copied icon.png

REM Build .ico using ImageMagick (magick command)
where magick >nul 2>&1
if %errorlevel% == 0 (
    echo Building icon.ico...
    magick "%ICONS_DIR%\16x16.png" "%ICONS_DIR%\24x24.png" "%ICONS_DIR%\32x32.png" ^
           "%ICONS_DIR%\48x48.png" "%ICONS_DIR%\64x64.png" "%ICONS_DIR%\128x128.png" ^
           "%ICONS_DIR%\256x256.png" "%SCRIPT_DIR%icon.ico"
    echo    OK: icon.ico
) else (
    echo WARNING: ImageMagick not found, cannot build .ico automatically.
    echo          You can convert icon.png to icon.ico using an online converter:
    echo          https://convertio.co/png-ico/
    echo          Save as: %SCRIPT_DIR%icon.ico
)

echo.
echo Done! Icons are in %ICONS_DIR%\
