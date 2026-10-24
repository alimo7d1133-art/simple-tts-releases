#!/usr/bin/env bash
# Rebuild the documented Android ICU-regex portability change in the vendored jar.
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT"
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
CP=$(printf '%s:' app/libs/*.jar)
javac --release 8 -cp "$CP" -d "$TMP" \
 freetts/source/com/sun/speech/freetts/en/us/USEnglish.java \
 freetts/source/com/sun/speech/freetts/en/us/TokenToWords.java
jar uf app/libs/en_us.jar -C "$TMP" .
cp app/libs/en_us.jar freetts/lib/en_us.jar
printf 'Rebuilt Android-safe USEnglish/TokenToWords in app/libs/en_us.jar\n'
