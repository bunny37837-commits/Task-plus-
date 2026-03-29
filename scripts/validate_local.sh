#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
cd "$ROOT_DIR"

echo "[validate] project: $ROOT_DIR"

if ! command -v java >/dev/null 2>&1; then
  echo "[validate] ERROR: java not found. Install JDK 17+ and set JAVA_HOME."
  exit 1
fi

if [ -z "${JAVA_HOME:-}" ]; then
  echo "[validate] WARN: JAVA_HOME is not set. Gradle may fail depending on shell setup."
fi

echo "[validate] Running :app:assembleDebug"
./gradlew :app:assembleDebug --console=plain

echo "[validate] Running :app:lintDebug"
./gradlew :app:lintDebug --console=plain

echo "[validate] Running unit tests (:app:testDebugUnitTest)"
./gradlew :app:testDebugUnitTest --console=plain

echo "[validate] Done."
