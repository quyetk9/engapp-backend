#!/bin/sh
set -e

GRADLE_VERSION="8.9"
GRADLE_ZIP_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
CACHE_DIR="${HOME}/.cache/gradle-wrapper"
DIST_DIR="${CACHE_DIR}/gradle-${GRADLE_VERSION}"

if [ ! -x "${DIST_DIR}/bin/gradle" ]; then
  mkdir -p "${CACHE_DIR}"
  TMP_ZIP="${CACHE_DIR}/gradle.zip"
  echo "Downloading Gradle ${GRADLE_VERSION}..."
  curl -fsSL "$GRADLE_ZIP_URL" -o "$TMP_ZIP"
  unzip -oq "$TMP_ZIP" -d "$CACHE_DIR"
  chmod +x "${DIST_DIR}/bin/gradle"
fi

exec "${DIST_DIR}/bin/gradle" "$@"
