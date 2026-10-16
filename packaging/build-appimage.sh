#!/usr/bin/env bash
# Compose Desktop's packageAppImage task on this toolchain writes a jpackage
# app image (directory + bundled JRE), not a .AppImage file. This script turns
# that image into MeloX-Linux-x86_64.AppImage.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

./gradlew :desktop:packageAppImage --no-daemon

APP="$ROOT/desktop/build/compose/binaries/main/app/MeloX"
if [[ ! -x "$APP/bin/MeloX" ]]; then
  echo "jpackage image missing: $APP" >&2
  exit 1
fi

# The jpackage native launcher aborts on KDE Wayland. Ship a java launcher
# and start the JVM from AppRun instead.
RT="$(dirname "$(dirname "$(readlink -f "$(command -v java)")")")"
mkdir -p "$APP/lib/runtime/bin"
cp -f "$RT/bin/java" "$APP/lib/runtime/bin/java"
chmod +x "$APP/lib/runtime/bin/java"

APPDIR="$ROOT/packaging/AppDir"
rm -rf "$APPDIR"
mkdir -p "$APPDIR/usr"
cp -a "$APP" "$APPDIR/usr/MeloX"
cp "$ROOT/packaging/AppRun" "$APPDIR/AppRun"
cp "$ROOT/packaging/melox.desktop" "$APPDIR/melox.desktop"
ICON="${MELOX_ICON:-/home/lladlam/项目/MeloX-Android/android/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png}"
if [[ -f "$ICON" ]]; then
  cp "$ICON" "$APPDIR/melox.png"
fi
chmod +x "$APPDIR/AppRun"

TOOL="${APPIMAGETOOL:-}"
if [[ -z "$TOOL" ]] && command -v appimagetool >/dev/null 2>&1; then
  TOOL="$(command -v appimagetool)"
fi
if [[ -z "$TOOL" ]]; then
  TOOL_DIR="$ROOT/build/appimagetool"
  mkdir -p "$TOOL_DIR"
  TOOL="$TOOL_DIR/appimagetool-x86_64.AppImage"
  if [[ ! -x "$TOOL" ]]; then
    curl -fsSL \
      https://github.com/AppImage/appimagetool/releases/download/continuous/appimagetool-x86_64.AppImage \
      -o "$TOOL"
    chmod +x "$TOOL"
  fi
fi

# Extract appimagetool to avoid requiring FUSE just to run the packaging tool.
if [[ "$TOOL" == *.AppImage ]]; then
  TOOL_DIR="$ROOT/build/appimagetool-extracted"
  mkdir -p "$TOOL_DIR"
  (cd "$TOOL_DIR" && "$TOOL" --appimage-extract >/dev/null)
  TOOL="$TOOL_DIR/squashfs-root/AppRun"
fi

DEST="$ROOT/MeloX-Linux-x86_64.AppImage"
rm -f "$DEST"
ARCH=x86_64 "$TOOL" "$APPDIR" "$DEST"
chmod +x "$DEST"
ls -lh "$DEST"
