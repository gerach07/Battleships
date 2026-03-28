#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════════════════════
# AB BATTLESHIPS — Windows .exe Installer using system makensis (no Wine)
# ═══════════════════════════════════════════════════════════════════════════════
# This script uses the NSIS script (installer.nsi) directly with the system
# makensis binary — Wine is NOT required.
#
# Usage:
#   chmod +x package-nsis.sh
#   ./package-nsis.sh
#
# Prerequisites:
#   sudo apt install nsis nodejs npm imagemagick
#
# This script compiles the Windows installer from the Linux host.
# ═══════════════════════════════════════════════════════════════════════════════
set -euo pipefail

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[0;33m'; BLUE='\033[0;34m'
BOLD='\033[1m'; NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CLIENT_DIR="$SCRIPT_DIR/../Battleships-web/client"

echo -e "${BLUE}${BOLD}"
echo "╔═══════════════════════════════════════════════════════╗"
echo "║  AB BATTLESHIPS — Windows .exe Builder (no Wine)     ║"
echo "╚═══════════════════════════════════════════════════════╝"
echo -e "${NC}"

# ── Step 0: Prerequisites ────────────────────────────────
echo -e "${YELLOW}[0/6]${NC} Checking prerequisites..."

check() { command -v "$1" &>/dev/null || { echo -e "${RED}❌ $1 not found. Install: $2${NC}"; exit 1; }; }
check node  "sudo apt install nodejs"
check npm   "sudo apt install npm"
check makensis "sudo apt install nsis"

echo -e "${GREEN}✅ Prerequisites OK${NC}"; echo ""

# ── Step 1: Generate icons ────────────────────────────────
echo -e "${YELLOW}[1/6]${NC} Generating icons..."
chmod +x "$SCRIPT_DIR/assets/generate-icons.sh"
bash "$SCRIPT_DIR/assets/generate-icons.sh"

[ ! -f "$SCRIPT_DIR/assets/icon.ico" ] && {
  echo -e "${RED}❌ icon.ico missing. Install imagemagick: sudo apt install imagemagick${NC}"; exit 1
}
echo ""

# ── Step 2: Build React client ────────────────────────────
echo -e "${YELLOW}[2/6]${NC} Building React web client..."
cd "$CLIENT_DIR"
[ ! -d "node_modules" ] && npm install --legacy-peer-deps
PUBLIC_URL="./" npm run build
echo -e "${GREEN}   ✅ Client built${NC}"; echo ""

# ── Step 3: Copy to renderer ─────────────────────────────
echo -e "${YELLOW}[3/6]${NC} Copying client → renderer..."
cd "$SCRIPT_DIR"
node scripts/copy-client.js; echo ""

# ── Step 4: Package Electron (win-unpacked) via electron-builder ───────────
echo -e "${YELLOW}[4/6]${NC} Packaging Electron app (Windows)..."
cd "$SCRIPT_DIR"
[ ! -d "node_modules" ] && npm install

rm -rf dist/win-unpacked

# Use electron-builder to ONLY create win-unpacked (no NSIS step)
CSC_IDENTITY_AUTO_DISCOVERY=false \
  node node_modules/.bin/electron-builder --win dir --x64

echo -e "${GREEN}   ✅ win-unpacked ready ($(du -sh dist/win-unpacked | cut -f1))${NC}"; echo ""

# ── Step 5: Build NSIS installer with system makensis ────────────────────────
echo -e "${YELLOW}[5/6]${NC} Compiling NSIS installer with makensis..."
cd "$SCRIPT_DIR"
mkdir -p dist

makensis \
  -DAPP_VERSION=1.0.0 \
  -NOCD \
  installer.nsi

echo -e "${GREEN}   ✅ NSIS installer compiled${NC}"; echo ""

# ── Step 6: Done ─────────────────────────────────────────
EXE=$(find "$SCRIPT_DIR/dist" -maxdepth 1 -name "*.exe" | head -n1)
if [ -n "$EXE" ]; then
  SIZE=$(du -h "$EXE" | cut -f1)
  echo -e "${GREEN}${BOLD}"
  echo "╔═══════════════════════════════════════════════════════╗"
  echo "║                 BUILD COMPLETE! 🎉                   ║"
  echo "╚═══════════════════════════════════════════════════════╝"
  echo -e "${NC}"
  echo -e "${GREEN}📦 Installer: ${BOLD}dist/$(basename "$EXE")${NC}"
  echo -e "${GREEN}📏 Size:      $SIZE${NC}"
  echo ""
  echo -e "${BLUE}Transfer to Windows and double-click to install.${NC}"
  echo -e "${BLUE}Silent install: ${BOLD}$(basename "$EXE") /S${NC}"
else
  echo -e "${RED}❌ .exe not found in dist/${NC}"
  exit 1
fi
