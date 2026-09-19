#!/bin/bash
# Deploy Battleships to iPhone 14 Pro from Ubuntu using xtool
# Usage: ./deploy.sh

set -euo pipefail

export PATH="/usr/share/swift/usr/bin:$PATH"

UDID_14="00008120-001214310198201E"  # iPhone 14 Pro
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
APP_BUNDLE="$SCRIPT_DIR/xtool/Battleships.app"

cd "$SCRIPT_DIR"

echo "==> Verifying Swift toolchain..."
command -v swift >/dev/null 2>&1 || {
  echo "Swift not found in PATH. Expected /usr/share/swift/usr/bin" >&2
  exit 1
}

echo "==> Listing connected devices..."
xtool ds devices list || true

echo "==> Building and installing app + widget extension..."
xtool dev run --udid "$UDID_14" --all

if [ -f "$APP_BUNDLE/Info.plist" ]; then
  echo "==> Patching main app Info.plist..."
  python3 - "$APP_BUNDLE" "$SCRIPT_DIR" <<'PYEOF'
import os, plistlib, shutil, sys
bundle_path = sys.argv[1]
script_dir = sys.argv[2]
info_path = bundle_path + '/Info.plist'
source_google = os.path.join(script_dir, 'Battleships', 'GoogleService-Info.plist')

def ensure_google_configs():
    if os.path.exists(source_google):
        dest_google = os.path.join(bundle_path, 'GoogleService-Info.plist')
        shutil.copy2(source_google, dest_google)
        print('GoogleService-Info.plist copied into app bundle.')
    else:
        print('GoogleService-Info.plist source missing; skipping bundle copy.')

with open(info_path, 'rb') as f:
    p = plistlib.load(f)

p['CFBundleIcons'] = {'CFBundlePrimaryIcon': {'CFBundleIconFiles': ['AppIcon120', 'AppIcon180'], 'CFBundleIconName': 'AppIcon'}}
p['CFBundleIcons~ipad'] = {'CFBundlePrimaryIcon': {'CFBundleIconFiles': ['AppIcon120', 'AppIcon180'], 'CFBundleIconName': 'AppIcon'}}
p['NSSupportsLiveActivities'] = True
p['CFBundleURLTypes'] = [{
    'CFBundleTypeRole': 'Editor',
    'CFBundleURLSchemes': ['com.googleusercontent.apps.498836539028-ppqdtbvc6eos5urr7kgd5hctrvv82dr3']
}]
with open(info_path, 'wb') as f:
    plistlib.dump(p, f)
print('Icon and URL config patched.')
ensure_google_configs()
PYEOF

  echo "==> Reinstalling with patched plist..."
  xtool install --udid "$UDID_14" "$APP_BUNDLE"
fi

echo "==> Done!"
