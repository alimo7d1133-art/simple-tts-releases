#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
BUILD=$(mktemp -d)
trap 'rm -rf "$BUILD"' EXIT
mkdir -p "$BUILD/org/retro/flitetts"
cat > "$BUILD/org/retro/flitetts/StandaloneAudio.java" <<'JAVA'
package org.retro.flitetts;
public final class StandaloneAudio {
  public interface Cancellation { boolean cancelled(); }
}
JAVA
cp "$ROOT/app/src/main/java/org/retro/flitetts/AudioResampler.java" "$BUILD/org/retro/flitetts/"
cp "$ROOT/tests/resampler/HostResamplerTest.java" "$BUILD/org/retro/flitetts/"
javac -encoding UTF-8 -d "$BUILD/classes" \
  "$BUILD/org/retro/flitetts/StandaloneAudio.java" \
  "$BUILD/org/retro/flitetts/AudioResampler.java" \
  "$BUILD/org/retro/flitetts/HostResamplerTest.java"
java -cp "$BUILD/classes" org.retro.flitetts.HostResamplerTest
