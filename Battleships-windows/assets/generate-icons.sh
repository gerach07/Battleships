#!/usr/bin/env bash
# ─────────────────────────────────────────────────────
# Generate PNG icons + Windows ICO from icon.svg
# For use when cross-compiling from Linux to Windows.
# Requires: rsvg-convert (librsvg2-bin) OR imagemagick
# ─────────────────────────────────────────────────────
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SVG_FILE="$SCRIPT_DIR/icon.svg"
ICONS_DIR="$SCRIPT_DIR/icons"

# Sizes for the ICO file (Windows standard sizes)
ICO_SIZES=(16 24 32 48 64 128 256)
# Extra large PNG
ALL_SIZES=(16 24 32 48 64 128 256 512)

if [ ! -f "$SVG_FILE" ]; then
  echo "❌ icon.svg not found at $SVG_FILE"
  exit 1
fi

mkdir -p "$ICONS_DIR"

# Pick converter
if command -v rsvg-convert &>/dev/null; then
  CONVERTER="rsvg"
elif command -v convert &>/dev/null; then
  CONVERTER="imagemagick"
elif command -v inkscape &>/dev/null; then
  CONVERTER="inkscape"
else
  echo "❌ No SVG converter found."
  echo "   sudo apt install librsvg2-bin    (recommended)"
  echo "   sudo apt install imagemagick"
  exit 1
fi

echo "📐 Generating PNGs using $CONVERTER..."
for size in "${ALL_SIZES[@]}"; do
  outfile="$ICONS_DIR/${size}x${size}.png"
  case "$CONVERTER" in
    rsvg)       rsvg-convert -w "$size" -h "$size" "$SVG_FILE" -o "$outfile" ;;
    imagemagick) convert -background none -density 300 "$SVG_FILE" -resize "${size}x${size}" "$outfile" ;;
    inkscape)   inkscape -w "$size" -h "$size" "$SVG_FILE" -o "$outfile" 2>/dev/null ;;
  esac
  echo "   ✅ ${size}x${size}.png"
done

# Copy main 512px as icon.png
cp "$ICONS_DIR/512x512.png" "$SCRIPT_DIR/icon.png"

# Build Windows .ico from multiple sizes using ImageMagick/rsvg
echo ""
echo "🪟 Building Windows icon.ico..."

ICO_INPUTS=()
for size in "${ICO_SIZES[@]}"; do
  ICO_INPUTS+=("$ICONS_DIR/${size}x${size}.png")
done

if command -v convert &>/dev/null; then
  convert "${ICO_INPUTS[@]}" "$SCRIPT_DIR/icon.ico"
  echo "   ✅ icon.ico created via ImageMagick"
elif command -v icotool &>/dev/null; then
  icotool -c -o "$SCRIPT_DIR/icon.ico" "${ICO_INPUTS[@]}"
  echo "   ✅ icon.ico created via icotool"
else
  echo "   ⚠ Could not create .ico — install imagemagick or icoutils:"
  echo "     sudo apt install imagemagick"
  echo "     sudo apt install icoutils"
  echo "   PNG icons are available in $ICONS_DIR/"
fi

echo ""
echo "✅ All icons generated in $ICONS_DIR/"
echo "✅ icon.png  → $SCRIPT_DIR/icon.png"
[ -f "$SCRIPT_DIR/icon.ico" ] && echo "✅ icon.ico  → $SCRIPT_DIR/icon.ico"
