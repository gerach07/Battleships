#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APK_PATH="$SCRIPT_DIR/app/build/outputs/apk/release/app-release.apk"
APPLICATION_ID="com.anasio.battleships"
DEVICE_SERIAL="${1:-${ANDROID_SERIAL:-}}"

cd "$SCRIPT_DIR"

command -v adb >/dev/null 2>&1 || {
  echo "adb not found. Install Android Platform Tools and ensure adb is on PATH." >&2
  exit 1
}

adb start-server >/dev/null

if [[ -z "$DEVICE_SERIAL" ]]; then
  mapfile -t CONNECTED_DEVICES < <(adb devices | awk 'NR > 1 && $2 == "device" { print $1 }')

  if [[ ${#CONNECTED_DEVICES[@]} -eq 0 ]]; then
    echo "No authorized Android device found. Connect and unlock your phone, enable USB debugging, then accept its RSA prompt." >&2
    adb devices -l
    exit 1
  fi

  if [[ ${#CONNECTED_DEVICES[@]} -gt 1 ]]; then
    echo "More than one Android device is connected. Choose one with:" >&2
    echo "  ./deploy.sh <device-serial>" >&2
    adb devices -l
    exit 1
  fi

  DEVICE_SERIAL="${CONNECTED_DEVICES[0]}"
fi

DEVICE_STATE="$(adb -s "$DEVICE_SERIAL" get-state 2>/dev/null || true)"
if [[ "$DEVICE_STATE" != "device" ]]; then
  echo "Android device '$DEVICE_SERIAL' is not authorized or available (state: ${DEVICE_STATE:-not found})." >&2
  adb devices -l
  exit 1
fi

if [[ ! -f "$SCRIPT_DIR/app/release.keystore" ]]; then
  echo "Release signing key not found at android/app/release.keystore." >&2
  echo "Cannot build the signed release APK without the project's release key." >&2
  exit 1
fi

echo "==> Building signed release APK..."
./gradlew :app:assembleRelease

if [[ ! -f "$APK_PATH" ]]; then
  echo "Release APK was not produced at: $APK_PATH" >&2
  exit 1
fi

echo "==> Installing on Android device: $DEVICE_SERIAL"
adb -s "$DEVICE_SERIAL" install -r "$APK_PATH"

echo "==> Launching Battleships..."
adb -s "$DEVICE_SERIAL" shell monkey -p "$APPLICATION_ID" -c android.intent.category.LAUNCHER 1

echo "==> Done!"
