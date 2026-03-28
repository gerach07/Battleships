#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════════════════════
# AB BATTLESHIPS — Manual .deb Builder (dpkg-deb)
# ═══════════════════════════════════════════════════════════════════════════════
# Alternative to electron-builder. Builds the .deb using dpkg-deb directly.
# Useful if electron-builder has issues or you want full packaging control.
#
# This script:
#   1. Builds the React client
#   2. Uses a pre-built Electron binary (downloaded via npm)
#   3. Assembles the Debian package structure manually
#   4. Builds .deb with dpkg-deb
#
# Usage:
#   chmod +x build-deb-manual.sh
#   ./build-deb-manual.sh
#
# Output:
#   dist/abbattleships_1.0.0_amd64.deb
# ═══════════════════════════════════════════════════════════════════════════════
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CLIENT_DIR="$SCRIPT_DIR/../Battleships-web/client"
RENDERER_DIR="$SCRIPT_DIR/renderer"

VERSION="1.0.0"
ARCH="amd64"
PKG_NAME="abbattleships"
DEB_DIR="$SCRIPT_DIR/dist/deb-staging"
INSTALL_PREFIX="/opt/${PKG_NAME}"
DEB_OUTPUT="$SCRIPT_DIR/dist/${PKG_NAME}_${VERSION}_${ARCH}.deb"

echo "╔═══════════════════════════════════════════════════════╗"
echo "║    AB BATTLESHIPS — Manual .deb Builder (dpkg-deb)   ║"
echo "╚═══════════════════════════════════════════════════════╝"
echo ""

# ── Check prerequisites ──────────────────────────────
for cmd in node npm dpkg-deb fakeroot; do
  if ! command -v "$cmd" &>/dev/null; then
    echo "❌ Missing: $cmd"
    echo "   Install with: sudo apt install $([ "$cmd" = "node" ] && echo "nodejs" || echo "$cmd")"
    exit 1
  fi
done

# ── Generate icons ───────────────────────────────────
echo "[1/6] Generating icons..."
chmod +x "$SCRIPT_DIR/assets/generate-icons.sh"
bash "$SCRIPT_DIR/assets/generate-icons.sh"
echo ""

# ── Build React client ───────────────────────────────
echo "[2/6] Building React client..."
cd "$CLIENT_DIR"
[ ! -d "node_modules" ] && npm install --legacy-peer-deps
PUBLIC_URL="./" npm run build
echo ""

# ── Copy to renderer ────────────────────────────────
echo "[3/6] Copying build to renderer..."
cd "$SCRIPT_DIR"
node scripts/copy-client.js
echo ""

# ── Install Electron ─────────────────────────────────
echo "[4/6] Ensuring Electron is available..."
cd "$SCRIPT_DIR"
[ ! -d "node_modules" ] && npm install
ELECTRON_BIN="$SCRIPT_DIR/node_modules/.bin/electron"
ELECTRON_DIST="$SCRIPT_DIR/node_modules/electron/dist"

if [ ! -d "$ELECTRON_DIST" ]; then
  echo "❌ Electron dist not found. Run: npm install"
  exit 1
fi
echo "   Electron dist: $ELECTRON_DIST"
echo ""

# ── Assemble Debian package structure ────────────────
echo "[5/6] Assembling Debian package structure..."

rm -rf "$DEB_DIR"
mkdir -p "$DEB_DIR/DEBIAN"
mkdir -p "$DEB_DIR${INSTALL_PREFIX}"
mkdir -p "$DEB_DIR/usr/bin"
mkdir -p "$DEB_DIR/usr/share/applications"
mkdir -p "$DEB_DIR/usr/share/doc/${PKG_NAME}"

# Copy Electron runtime
cp -r "$ELECTRON_DIST/"* "$DEB_DIR${INSTALL_PREFIX}/"

# Rename electron binary
if [ -f "$DEB_DIR${INSTALL_PREFIX}/electron" ]; then
  mv "$DEB_DIR${INSTALL_PREFIX}/electron" "$DEB_DIR${INSTALL_PREFIX}/${PKG_NAME}"
fi

# Copy app files into resources/app
mkdir -p "$DEB_DIR${INSTALL_PREFIX}/resources/app"
cp "$SCRIPT_DIR/main.js"     "$DEB_DIR${INSTALL_PREFIX}/resources/app/"
cp "$SCRIPT_DIR/preload.js"  "$DEB_DIR${INSTALL_PREFIX}/resources/app/"
cp "$SCRIPT_DIR/package.json" "$DEB_DIR${INSTALL_PREFIX}/resources/app/"
cp -r "$SCRIPT_DIR/renderer"  "$DEB_DIR${INSTALL_PREFIX}/resources/app/"
cp -r "$SCRIPT_DIR/assets"    "$DEB_DIR${INSTALL_PREFIX}/resources/app/"

# Create symlink in /usr/bin
ln -sf "${INSTALL_PREFIX}/${PKG_NAME}" "$DEB_DIR/usr/bin/${PKG_NAME}"

# Desktop file
cp "$SCRIPT_DIR/resources/abbattleships.desktop" "$DEB_DIR/usr/share/applications/"

# Icons (multiple sizes for proper desktop integration)
for size in 16 24 32 48 64 128 256 512; do
  icon_dir="$DEB_DIR/usr/share/icons/hicolor/${size}x${size}/apps"
  mkdir -p "$icon_dir"
  src_icon="$SCRIPT_DIR/assets/icons/${size}x${size}.png"
  if [ -f "$src_icon" ]; then
    cp "$src_icon" "$icon_dir/${PKG_NAME}.png"
  fi
done

# SVG icon
mkdir -p "$DEB_DIR/usr/share/icons/hicolor/scalable/apps"
cp "$SCRIPT_DIR/assets/icon.svg" "$DEB_DIR/usr/share/icons/hicolor/scalable/apps/${PKG_NAME}.svg"

# Copyright / docs
cat > "$DEB_DIR/usr/share/doc/${PKG_NAME}/copyright" << 'COPY_EOF'
Format: https://www.debian.org/doc/packaging-manuals/copyright-format/1.0/
Upstream-Name: AB Battleships
Source: https://abbattleships.web.app

Files: *
Copyright: 2024-2026 AB Battleships
License: MIT
COPY_EOF

# ── DEBIAN control file ─────────────────────────────
INSTALLED_SIZE=$(du -sk "$DEB_DIR${INSTALL_PREFIX}" | cut -f1)

cat > "$DEB_DIR/DEBIAN/control" << CONTROL_EOF
Package: ${PKG_NAME}
Version: ${VERSION}
Section: games
Priority: optional
Architecture: ${ARCH}
Installed-Size: ${INSTALLED_SIZE}
Depends: libgtk-3-0, libnotify4, libnss3, libxss1, libxtst6, xdg-utils, libatspi2.0-0, libuuid1, libsecret-1-0
Maintainer: AB Battleships <abbattleships@users.noreply.github.com>
Homepage: https://abbattleships.web.app
Description: AB Battleships — Multiplayer battleships strategy game
 Play Battleships online with friends in real-time. Create or join a game
 room, place your fleet on the board, take turns firing shots, and sink
 the entire enemy fleet to claim victory!
 .
 Features:
  - Real-time multiplayer via WebSockets
  - Beautiful dark-themed UI with animations
  - Sound effects and background music
  - Chat with your opponent
  - Multiple language support
CONTROL_EOF

# Post-install: update icon cache & desktop database
cat > "$DEB_DIR/DEBIAN/postinst" << 'POSTINST_EOF'
#!/bin/bash
set -e

# Update icon cache
if command -v gtk-update-icon-cache &>/dev/null; then
  gtk-update-icon-cache -f -t /usr/share/icons/hicolor 2>/dev/null || true
fi

# Update desktop database
if command -v update-desktop-database &>/dev/null; then
  update-desktop-database /usr/share/applications 2>/dev/null || true
fi

# Set correct permissions on chrome-sandbox
SANDBOX="/opt/abbattleships/chrome-sandbox"
if [ -f "$SANDBOX" ]; then
  chown root:root "$SANDBOX"
  chmod 4755 "$SANDBOX"
fi

exit 0
POSTINST_EOF
chmod 755 "$DEB_DIR/DEBIAN/postinst"

# Post-remove: clean up icon cache
cat > "$DEB_DIR/DEBIAN/postrm" << 'POSTRM_EOF'
#!/bin/bash
set -e

if [ "$1" = "remove" ] || [ "$1" = "purge" ]; then
  if command -v gtk-update-icon-cache &>/dev/null; then
    gtk-update-icon-cache -f -t /usr/share/icons/hicolor 2>/dev/null || true
  fi
  if command -v update-desktop-database &>/dev/null; then
    update-desktop-database /usr/share/applications 2>/dev/null || true
  fi
fi

exit 0
POSTRM_EOF
chmod 755 "$DEB_DIR/DEBIAN/postrm"

echo "   Package structure assembled."
echo ""

# ── Build .deb ───────────────────────────────────────
echo "[6/6] Building .deb package..."
mkdir -p "$SCRIPT_DIR/dist"
fakeroot dpkg-deb --build "$DEB_DIR" "$DEB_OUTPUT"

# Clean up staging
rm -rf "$DEB_DIR"

echo ""
echo "╔═══════════════════════════════════════════════════════╗"
echo "║                 BUILD COMPLETE! 🎉                   ║"
echo "╚═══════════════════════════════════════════════════════╝"
echo ""

DEB_SIZE=$(du -h "$DEB_OUTPUT" | cut -f1)
echo "📦 Package:  dist/$(basename "$DEB_OUTPUT")"
echo "📏 Size:     $DEB_SIZE"
echo ""
echo "To install:"
echo "  sudo apt install ./dist/$(basename "$DEB_OUTPUT")"
echo ""
echo "To uninstall:"
echo "  sudo apt remove ${PKG_NAME}"
echo ""
echo "To run:"
echo "  ${PKG_NAME}"
echo "  Or find 'AB Battleships' in your application menu."
