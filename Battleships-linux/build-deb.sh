#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════════════════════
# AB BATTLESHIPS — Linux .deb Package Builder
# ═══════════════════════════════════════════════════════════════════════════════
# This script:
#   1. Installs system & Node.js dependencies
#   2. Builds the React web client
#   3. Copies the build into the Electron renderer directory
#   4. Generates PNG icons from SVG
#   5. Packages everything into a .deb using electron-builder
#
# Usage:
#   chmod +x build-deb.sh
#   ./build-deb.sh
#
# Prerequisites:
#   - Node.js >= 18 & npm
#   - librsvg2-bin (for icon generation) OR imagemagick OR inkscape
#   - dpkg, fakeroot (usually pre-installed on Debian/Ubuntu)
#
# Output:
#   dist/abbattleships_1.0.0_amd64.deb
#
# Install the .deb:
#   sudo apt install ./dist/abbattleships_1.0.0_amd64.deb
# ═══════════════════════════════════════════════════════════════════════════════
set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
BOLD='\033[1m'
NC='\033[0m' # No Color

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CLIENT_DIR="$SCRIPT_DIR/../Battleships-web/client"
RENDERER_DIR="$SCRIPT_DIR/renderer"

echo -e "${BLUE}${BOLD}"
echo "╔═══════════════════════════════════════════════════════╗"
echo "║         AB BATTLESHIPS — .deb Package Builder        ║"
echo "╚═══════════════════════════════════════════════════════╝"
echo -e "${NC}"

# ── Step 0: Check prerequisites ──────────────────────
echo -e "${YELLOW}[0/5]${NC} Checking prerequisites..."

if ! command -v node &>/dev/null; then
  echo -e "${RED}❌ Node.js not found. Install Node.js >= 18 first.${NC}"
  echo "   https://nodejs.org or: sudo apt install nodejs npm"
  exit 1
fi

NODE_VERSION=$(node -v | sed 's/v//' | cut -d. -f1)
if [ "$NODE_VERSION" -lt 18 ]; then
  echo -e "${RED}❌ Node.js version >= 18 required (found: $(node -v))${NC}"
  exit 1
fi

if ! command -v npm &>/dev/null; then
  echo -e "${RED}❌ npm not found.${NC}"
  exit 1
fi

if ! command -v dpkg &>/dev/null; then
  echo -e "${RED}❌ dpkg not found. This script requires a Debian-based system.${NC}"
  exit 1
fi

# Check for icon converter
HAS_ICON_TOOL=false
for tool in rsvg-convert convert inkscape; do
  if command -v "$tool" &>/dev/null; then
    HAS_ICON_TOOL=true
    break
  fi
done
if [ "$HAS_ICON_TOOL" = false ]; then
  echo -e "${YELLOW}⚠ No SVG converter found. Installing librsvg2-bin...${NC}"
  sudo apt-get install -y librsvg2-bin 2>/dev/null || {
    echo -e "${RED}❌ Could not install librsvg2-bin. Please install manually:${NC}"
    echo "   sudo apt install librsvg2-bin"
    exit 1
  }
fi

echo -e "${GREEN}✅ All prerequisites met${NC}"
echo ""

# ── Step 1: Generate icons ───────────────────────────
echo -e "${YELLOW}[1/5]${NC} Generating PNG icons from SVG..."
cd "$SCRIPT_DIR"
chmod +x assets/generate-icons.sh
bash assets/generate-icons.sh
echo ""

# ── Step 2: Build React client ───────────────────────
echo -e "${YELLOW}[2/5]${NC} Building React web client..."
cd "$CLIENT_DIR"

if [ ! -d "node_modules" ]; then
  echo "   Installing client dependencies..."
  npm install --legacy-peer-deps
fi

echo "   Running production build..."
# Set PUBLIC_URL for Electron file:// loading (relative paths)
PUBLIC_URL="./" npm run build

echo -e "${GREEN}   ✅ React client built successfully${NC}"
echo ""

# ── Step 3: Copy client build to Electron renderer ───
echo -e "${YELLOW}[3/5]${NC} Copying client build to Electron renderer..."
cd "$SCRIPT_DIR"
node scripts/copy-client.js
echo ""

# ── Step 4: Install Electron dependencies ────────────
echo -e "${YELLOW}[4/5]${NC} Installing Electron dependencies..."
cd "$SCRIPT_DIR"

if [ ! -d "node_modules" ]; then
  npm install
fi

echo -e "${GREEN}   ✅ Dependencies installed${NC}"
echo ""

# ── Step 5: Build .deb package ───────────────────────
echo -e "${YELLOW}[5/5]${NC} Building .deb package with electron-builder..."
cd "$SCRIPT_DIR"

npx electron-builder --linux deb

echo ""
echo -e "${GREEN}${BOLD}"
echo "╔═══════════════════════════════════════════════════════╗"
echo "║                 BUILD COMPLETE! 🎉                   ║"
echo "╚═══════════════════════════════════════════════════════╝"
echo -e "${NC}"

# Find the .deb file
DEB_FILE=$(find "$SCRIPT_DIR/dist" -name "*.deb" -type f | head -n1)
if [ -n "$DEB_FILE" ]; then
  DEB_NAME=$(basename "$DEB_FILE")
  DEB_SIZE=$(du -h "$DEB_FILE" | cut -f1)
  echo -e "${GREEN}📦 Package:  ${BOLD}dist/$DEB_NAME${NC}"
  echo -e "${GREEN}📏 Size:     $DEB_SIZE${NC}"
  echo ""
  echo -e "${BLUE}To install:${NC}"
  echo -e "  ${BOLD}sudo apt install ./dist/$DEB_NAME${NC}"
  echo ""
  echo -e "${BLUE}To uninstall:${NC}"
  echo -e "  ${BOLD}sudo apt remove abbattleships${NC}"
  echo ""
  echo -e "${BLUE}To run after install:${NC}"
  echo -e "  ${BOLD}abbattleships${NC}"
  echo "  Or find 'AB Battleships' in your application menu."
else
  echo -e "${YELLOW}⚠ Could not locate .deb file in dist/${NC}"
  echo "  Check the dist/ directory manually."
fi
