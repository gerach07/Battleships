# AB Battleships — Linux Desktop App

A native Linux desktop application for the AB Battleships multiplayer game,
packaged as a `.deb` for installation via `apt`.

## Quick Start

```bash
# Build the .deb package (does everything automatically)
chmod +x build-deb.sh
./build-deb.sh

# Install it
sudo apt install ./dist/abbattleships_1.0.0_amd64.deb

# Run it
abbattleships
```

## Prerequisites

- **Node.js** >= 18 with npm
- **librsvg2-bin** (for icon generation) — installed automatically if missing
- **dpkg** and **fakeroot** (pre-installed on Debian/Ubuntu)

Install prerequisites:
```bash
sudo apt install nodejs npm librsvg2-bin fakeroot
```

## Build Scripts

| Script | Description |
|--------|-------------|
| `build-deb.sh` | **Recommended.** Uses electron-builder to create a polished `.deb` |
| `build-deb-manual.sh` | Alternative. Uses `dpkg-deb` directly for manual control |

## What Gets Built

The build process:
1. Generates PNG icons from `assets/icon.svg` at all standard sizes
2. Builds the React web client (production optimized)
3. Copies the build into the Electron renderer directory
4. Packages everything with Electron into a `.deb`

## Output

```
dist/abbattleships_1.0.0_amd64.deb
```

## Install / Uninstall

```bash
# Install
sudo apt install ./dist/abbattleships_1.0.0_amd64.deb

# Uninstall
sudo apt remove abbattleships
```

## After Installation

- **Binary** is at `/usr/bin/abbattleships` (or `/opt/abbattleships/`)
- **Desktop entry** appears in your app menu under Games → AB Battleships
- **Icon** is installed in the system icon theme

## Project Structure

```
Battleships-linux/
├── main.js                    # Electron main process
├── preload.js                 # Secure preload script
├── package.json               # Electron + electron-builder config
├── build-deb.sh               # Main build script (electron-builder)
├── build-deb-manual.sh        # Alternative build (dpkg-deb)
├── assets/
│   ├── icon.svg               # Source icon (vector)
│   ├── generate-icons.sh      # Icon size generator
│   └── icons/                 # Generated PNGs (16-512px)
├── resources/
│   └── abbattleships.desktop  # Linux desktop entry
├── scripts/
│   └── copy-client.js         # Copies React build to renderer/
├── renderer/                  # (generated) React client build
└── dist/                      # (generated) .deb output
```

## Development

Run the app without packaging:
```bash
npm install
npm run build:client
npm run copy:client
npm start
```

## How It Works

The desktop app is an **Electron** wrapper around the existing React web client.
It connects to the production game server (`battleships-production.up.railway.app`)
for multiplayer functionality — no local server needed.
