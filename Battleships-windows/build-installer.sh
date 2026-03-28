#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════════════════════
# AB BATTLESHIPS — Windows Installer Builder (cross-compile from Linux)
# ═══════════════════════════════════════════════════════════════════════════════
# Builds a Windows NSIS installer .exe from a Linux machine.
# electron-builder handles cross-compilation natively — no Wine needed.
#
# Usage:
#   chmod +x build-installer.sh
#   ./build-installer.sh
#
# Prerequisites (on Linux):
#   - Node.js >= 18 & npm
#   - imagemagick or librsvg2-bin  (for icon generation + .ico)
#   - icoutils (optional, for icotool)     sudo apt install icoutils
#
# Output:
#   dist/ABBattleships-Setup-1.0.0.exe
#
# The resulting .exe is a full NSIS installer for Windows x64.
# Transfer it to a Windows machine and run it there to install.
# ═══════════════════════════════════════════════════════════════════════════════
set -euo pipefail

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[0;33m'; BLUE='\033[0;34m'
BOLD='\033[1m'; NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CLIENT_DIR="$SCRIPT_DIR/../Battleships-web/client"

echo -e "${BLUE}${BOLD}"
echo "╔═══════════════════════════════════════════════════════╗"
echo "║   AB BATTLESHIPS — Windows Installer (from Linux)    ║"
echo "╚═══════════════════════════════════════════════════════╝"
echo -e "${NC}"

# ── Step 0: Check prerequisites ─────────────────────────
echo -e "${YELLOW}[0/5]${NC} Checking prerequisites..."

if ! command -v node &>/dev/null; then
  echo -e "${RED}❌ Node.js not found. Install Node.js >= 18.${NC}"
  exit 1
fi
NODE_MAJOR=$(node -v | sed 's/v//' | cut -d. -f1)
if [ "$NODE_MAJOR" -lt 18 ]; then
  echo -e "${RED}❌ Node.js >= 18 required (found: $(node -v))${NC}"
  exit 1
fi
if ! command -v npm &>/dev/null; then
  echo -e "${RED}❌ npm not found.${NC}"; exit 1
fi

# Check for ICO generation capability
HAS_MAGICK=false
command -v convert &>/dev/null && HAS_MAGICK=true
if [ "$HAS_MAGICK" = false ]; then
  echo -e "${YELLOW}⚠ ImageMagick not found. Trying to install...${NC}"
  sudo apt-get install -y imagemagick 2>/dev/null || {
    echo -e "${YELLOW}⚠ Could not auto-install. Run: sudo apt install imagemagick${NC}"
  }
fi

echo -e "${GREEN}✅ Prerequisites OK${NC}"
echo ""

# ── Step 1: Generate icons ───────────────────────────────
echo -e "${YELLOW}[1/6]${NC} Generating icons..."
cd "$SCRIPT_DIR"
chmod +x assets/generate-icons.sh
bash assets/generate-icons.sh

if [ ! -f "assets/icon.ico" ]; then
  echo -e "${RED}❌ icon.ico was not generated. electron-builder requires it for Windows builds.${NC}"
  echo "   Install ImageMagick: sudo apt install imagemagick"
  exit 1
fi
echo ""

# ── Step 2: Build React client ───────────────────────────
echo -e "${YELLOW}[2/6]${NC} Building React web client..."
cd "$CLIENT_DIR"
[ ! -d "node_modules" ] && npm install --legacy-peer-deps
PUBLIC_URL="./" npm run build
echo -e "${GREEN}   ✅ React client built${NC}"
echo ""

# ── Step 3: Copy to renderer ─────────────────────────────
echo -e "${YELLOW}[3/6]${NC} Copying client build to Electron renderer..."
cd "$SCRIPT_DIR"
node scripts/copy-client.js
echo ""

# ── Step 4: Package Electron (win-unpacked) via electron-builder ──────────
echo -e "${YELLOW}[4/6]${NC} Installing Electron dependencies and packaging..."
cd "$SCRIPT_DIR"
[ ! -d "node_modules" ] && npm install

rm -rf dist/win-unpacked

# Use --win dir to create win-unpacked only — no NSIS step, no Wine needed here
export CSC_IDENTITY_AUTO_DISCOVERY=false
trap '' INT
node node_modules/.bin/electron-builder --win dir --x64
trap - INT
echo -e "${GREEN}   ✅ win-unpacked ready ($(du -sh dist/win-unpacked | cut -f1))${NC}"
echo ""

# ── Step 5: Check makensis ───────────────────────────────
if ! command -v makensis &>/dev/null; then
  echo -e "${YELLOW}⚠ makensis not found. Installing nsis...${NC}"
  sudo apt-get install -y nsis || { echo -e "${RED}❌ Could not install nsis. Run: sudo apt install nsis${NC}"; exit 1; }
fi

# ── Step 6: Compile NSIS installer ──────────────────────
echo -e "${YELLOW}[6/6]${NC} Compiling NSIS installer with makensis (no Wine needed)..."
cd "$SCRIPT_DIR"
mkdir -p dist
makensis installer.nsi

echo ""
echo -e "${GREEN}${BOLD}"
echo "╔═══════════════════════════════════════════════════════╗"
echo "║                 BUILD COMPLETE! 🎉                   ║"
echo "╚═══════════════════════════════════════════════════════╝"
echo -e "${NC}"

EXE_FILE=$(find "$SCRIPT_DIR/dist" -name "*.exe" -type f | head -n1)
if [ -n "$EXE_FILE" ]; then
  EXE_NAME=$(basename "$EXE_FILE")
  EXE_SIZE=$(du -h "$EXE_FILE" | cut -f1)
  echo -e "${GREEN}📦 Installer:  ${BOLD}dist/$EXE_NAME${NC}"
  echo -e "${GREEN}📏 Size:       $EXE_SIZE${NC}"
  echo ""
  echo -e "${BLUE}Transfer to Windows and double-click to install.${NC}"
  echo -e "${BLUE}Silent install: ${BOLD}$EXE_NAME /S${NC}"
  echo ""
  echo -e "${BLUE}To uninstall on Windows:${NC}"
  echo -e "  Settings → Apps → AB Battleships → Uninstall"
fi
