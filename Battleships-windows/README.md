# AB Battleships — Windows Desktop App

A native Windows desktop application for the AB Battleships multiplayer game,
packaged as a proper NSIS installer `.exe`.

---

## Quick Start — Build on Windows

```bat
REM From the Battleships-windows directory:
build-installer.bat
```

Then double-click `dist\ABBattleships-Setup-1.0.0.exe` to install.

---

## Quick Start — Cross-compile from Linux

**No Wine required.** Uses `electron-builder --win dir` (packages the app) then `makensis` (compiles the installer).

```bash
# Install prerequisites
sudo apt install nodejs npm imagemagick nsis

chmod +x build-installer.sh
./build-installer.sh
```

Transfer `dist/ABBattleships-Setup-1.0.0.exe` to a Windows machine and run it.

---

## Prerequisites

### Building on Windows
- **Node.js** >= 18 — https://nodejs.org
- **ImageMagick** or **Inkscape** (for icon generation)
  - ImageMagick: https://imagemagick.org/script/download.php#windows
  - Inkscape: https://inkscape.org/release/

### Cross-compiling from Linux
```bash
sudo apt install nodejs npm imagemagick nsis
# Note: Wine is NOT required
```

---

## Build Scripts

| Script | Platform | Description |
|--------|----------|-------------|
| `build-installer.bat` | Windows | Full build → produces `.exe` |
| `build-installer.sh`  | Linux   | Full cross-compile → produces `.exe` (no Wine) |
| `package-nsis.sh`     | Linux   | Same as above, alternative entry point |
| `installer.nsi`       | Any     | Raw NSIS script — use `makensis installer.nsi` |

---

## Installer Features

The NSIS installer:
- Shows a **license agreement** screen
- Lets the user choose the **installation directory** (default: `C:\Program Files\AB Battleships`)
- Creates a **Start Menu** shortcut (under Games)
- Creates an optional **Desktop** shortcut
- Registers the app in **Apps & Features** (proper uninstall)
- Installs an **uninstaller** (`Uninstall AB Battleships.exe`)
- Registers the `abbattleships://` URL protocol

**Silent install** (no UI):
```bat
ABBattleships-Setup-1.0.0.exe /S
```

**Silent install to custom directory:**
```bat
ABBattleships-Setup-1.0.0.exe /S /D=C:\Games\ABBattleships
```

---

## Install / Uninstall on Windows

**Install:**
> Double-click `ABBattleships-Setup-1.0.0.exe`

**Uninstall:**
> Settings → Apps → AB Battleships → Uninstall
>
> or: `"C:\Program Files\AB Battleships\Uninstall AB Battleships.exe"`

---

## Project Structure

```
Battleships-windows/
├── main.js                      # Electron main process
├── preload.js                   # Secure preload script
├── package.json                 # Electron + electron-builder (NSIS) config
├── installer.nsi                # NSIS installer script (compiled with makensis)
├── build-installer.bat          # Full build script (run on Windows)
├── build-installer.sh           # Full cross-compile script (run on Linux, no Wine)
├── package-nsis.sh              # Alternative Linux build (NSIS packaging only)
├── assets/
│   ├── icon.svg                 # Source icon (vector)
│   ├── generate-icons.sh        # Generates PNGs + .ico (Linux)
│   ├── generate-icons.bat       # Generates PNGs + .ico (Windows)
│   └── icons/                   # Generated PNGs (16–512px)
├── resources/
│   ├── installer.nsh            # Custom NSIS macros (registry, protocol)
│   └── LICENSE.rtf              # License shown during install
├── scripts/
│   └── copy-client.js           # Copies React build → renderer/
├── renderer/                    # (generated) React client build
└── dist/
    ├── win-unpacked/            # (generated) Unzipped Electron app
    └── ABBattleships-Setup-*.exe  # (generated) NSIS installer
```

---

## Development

Run the app in dev mode without packaging:
```bash
npm install
npm run build:client
npm run copy:client
npm start                 # opens Electron window
```
