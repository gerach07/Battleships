#!/usr/bin/env bash
# ─────────────────────────────────────────────────────
# Generate PNG icons at all standard sizes from icon.svg
# Requires: rsvg-convert (librsvg2-bin) OR ImageMagick (convert)
# ─────────────────────────────────────────────────────
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ASSETS_DIR="$SCRIPT_DIR"
ICONS_DIR="$ASSETS_DIR/icons"
SVG_FILE="$ASSETS_DIR/icon.svg"

SIZES=(16 24 32 48 64 128 256 512)

if [ ! -f "$SVG_FILE" ]; then
  echo "❌ icon.svg not found at $SVG_FILE"
  exit 1
fi

mkdir -p "$ICONS_DIR"

# Try rsvg-convert first (better quality), fall back to ImageMagick
if command -v rsvg-convert &>/dev/null; then
  echo "📐 Using rsvg-convert to generate icons..."
  for size in "${SIZES[@]}"; do
    outfile="$ICONS_DIR/${size}x${size}.png"
    rsvg-convert -w "$size" -h "$size" "$SVG_FILE" -o "$outfile"
    echo "   ✅ ${size}x${size}.png"
  done
elif command -v convert &>/dev/null; then
  echo "📐 Using ImageMagick to generate icons..."
  for size in "${SIZES[@]}"; do
    outfile="$ICONS_DIR/${size}x${size}.png"
    convert -background none -density 300 "$SVG_FILE" -resize "${size}x${size}" "$outfile"
    echo "   ✅ ${size}x${size}.png"
  done
elif command -v inkscape &>/dev/null; then
  echo "📐 Using Inkscape to generate icons..."
  for size in "${SIZES[@]}"; do
    outfile="$ICONS_DIR/${size}x${size}.png"
    inkscape -w "$size" -h "$size" "$SVG_FILE" -o "$outfile" 2>/dev/null
    echo "   ✅ ${size}x${size}.png"
  done
else
  echo "❌ No SVG converter found. Install one of:"
  echo "   sudo apt install librsvg2-bin    (recommended)"
  echo "   sudo apt install imagemagick"
  echo "   sudo apt install inkscape"
  exit 1
fi

# Copy 512 as the main icon.png
cp "$ICONS_DIR/512x512.png" "$ASSETS_DIR/icon.png"
echo ""
echo "✅ All icons generated in $ICONS_DIR/"
echo "✅ Main icon copied to $ASSETS_DIR/icon.png"
