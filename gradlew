#!/usr/bin/env sh
# Lightweight Gradle bootstrapper. Downloads Gradle 8.8 once, then reuses it.
set -eu
VERSION="8.8"
BASE="${GRADLE_USER_HOME:-$HOME/.gradle}/neonvisuals-wrapper"
HOME_DIR="$BASE/gradle-$VERSION"
if [ ! -x "$HOME_DIR/bin/gradle" ]; then
  mkdir -p "$BASE"
  ZIP="$BASE/gradle-$VERSION-bin.zip"
  echo "Downloading Gradle $VERSION..."
  if command -v curl >/dev/null 2>&1; then
    curl -fL "https://services.gradle.org/distributions/gradle-$VERSION-bin.zip" -o "$ZIP"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$ZIP" "https://services.gradle.org/distributions/gradle-$VERSION-bin.zip"
  else
    echo "curl or wget is required to download Gradle." >&2
    exit 1
  fi
  if command -v unzip >/dev/null 2>&1; then
    unzip -q -o "$ZIP" -d "$BASE"
  else
    echo "unzip is required to extract Gradle." >&2
    exit 1
  fi
fi
exec "$HOME_DIR/bin/gradle" "$@"
