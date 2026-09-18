#!/bin/bash
# Deploy Battleships to iPhone 14 Pro from Ubuntu using xtool
# Usage: ./deploy.sh

export PATH="/usr/share/swift/usr/bin:$PATH"

UDID_14="00008120-001214310198201E"  # iPhone 14 Pro
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
APP_BUNDLE="$SCRIPT_DIR/xtool/Battleships.app"

cd "$SCRIPT_DIR"

echo "==> Building and installing app + widget extension..."
xtool dev run --udid "$UDID_14" 2>&1
if [ $? -ne 0 ]; then echo "Build failed!"; exit 1; fi

echo "==> Patching main app Info.plist..."
python3 - "$APP_BUNDLE" << 'PYEOF'
import plistlib, sys
path = sys.argv[1] + '/Info.plist'
with open(path, 'rb') as f:
    p = plistlib.load(f)
p['CFBundleIcons'] = {'CFBundlePrimaryIcon': {'CFBundleIconFiles': ['AppIcon120', 'AppIcon180'], 'CFBundleIconName': 'AppIcon'}}
p['CFBundleIcons~ipad'] = {'CFBundlePrimaryIcon': {'CFBundleIconFiles': ['AppIcon120', 'AppIcon180'], 'CFBundleIconName': 'AppIcon'}}
p['NSSupportsLiveActivities'] = True
with open(path, 'wb') as f:
    plistlib.dump(p, f)
print("Icon config patched.")
PYEOF

echo "==> Reinstalling with patched plist..."
xtool install --udid "$UDID_14" "$APP_BUNDLE" 2>&1

echo "==> Done!"
