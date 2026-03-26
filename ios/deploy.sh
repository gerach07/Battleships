#!/bin/bash
# Deploy Battleships to iPhones from Ubuntu using xtool
# Usage: ./deploy.sh
# Deploys to both iPhone 13 Pro Max and iPhone 14 Pro

export PATH="/usr/share/swift/usr/bin:$PATH"

UDID_13="00008110-001E659C3492401E"  # iPhone 13 Pro Max
UDID_14="00008120-001214310198201E"  # iPhone 14 Pro
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
APP_BUNDLE="$SCRIPT_DIR/xtool/Battleships.app"

cd "$SCRIPT_DIR"

echo "==> Building..."
xtool dev run --udid "$UDID_13" 2>&1
if [ $? -ne 0 ]; then echo "Build failed!"; exit 1; fi

echo "==> Patching Info.plist with icon config..."
python3 - "$APP_BUNDLE" << 'PYEOF'
import plistlib, sys
path = sys.argv[1] + '/Info.plist'
with open(path, 'rb') as f:
    p = plistlib.load(f)
p['CFBundleIcons'] = {'CFBundlePrimaryIcon': {'CFBundleIconFiles': ['AppIcon120', 'AppIcon180'], 'CFBundleIconName': 'AppIcon'}}
p['CFBundleIcons~ipad'] = {'CFBundlePrimaryIcon': {'CFBundleIconFiles': ['AppIcon120', 'AppIcon180'], 'CFBundleIconName': 'AppIcon'}}
with open(path, 'wb') as f:
    plistlib.dump(p, f)
print("Icon config patched.")
PYEOF

echo "==> Installing on iPhone 13 Pro Max..."
xtool install --udid "$UDID_13" "$APP_BUNDLE" 2>&1

echo "==> Installing on iPhone 14 Pro..."
xtool install --udid "$UDID_14" "$APP_BUNDLE" 2>&1

echo "==> Done!"
