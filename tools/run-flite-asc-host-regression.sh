#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
: "${JAVA_HOME:?Set JAVA_HOME to a JDK with JNI headers}"
MODEL_DIR=${1:-"$ROOT/app/src/main/assets/asc"}
MODEL="$MODEL_DIR/kacst_ar_asc.flitevox"
[[ -s "$MODEL" ]] || { echo "ASC model missing: $MODEL" >&2; exit 2; }
BUILD=${BUILD_DIR:-"$ROOT/build-host"}
cmake -S "$ROOT/app/src/main/cpp" -B "$BUILD" -DCMAKE_BUILD_TYPE=Release \
  "-DCMAKE_C_FLAGS=-I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux"
cmake --build "$BUILD" -j"${JOBS:-4}"
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
mkdir -p "$TMP/src/org/retro/flitetts" "$TMP/classes"
cp "$ROOT/app/src/main/java/org/retro/flitetts/NativeBridge.java" "$TMP/src/org/retro/flitetts/"
cat > "$TMP/src/org/retro/flitetts/AscHostRegression.java" <<'JAVA'
package org.retro.flitetts;
import java.nio.file.*;
public final class AscHostRegression {
  private static long energy(short[] a) { long n=0; for(short x:a)n+=Math.abs((long)x); return n; }
  public static void main(String[] args) throws Exception {
    if (!NativeBridge.loadExtraVoices(args[0])) throw new AssertionError("ASC model not registered");
    int hz=NativeBridge.sampleRate(9);
    if (hz<=0) throw new AssertionError("ASC index 9 has no sample rate");
    String[] texts={
      "مرحبا بكم. هذه تجربة للصوت العربي.",
      "مرحبا بكم. هذه تجربة للصوت العربي.",
      "مَرْحَبًا بِكُمْ. هَذِهِ تَجْرِبَةٌ لِلصَّوْتِ الْعَرَبِيِّ."
    };
    for (int i=0;i<texts.length;i++) {
      short[] pcm=NativeBridge.render(texts[i],9,1f,1f);
      if (pcm==null || pcm.length==0 || energy(pcm)<1000)
        throw new AssertionError("empty/silent ASC PCM for text " + i);
      System.out.println("PASS ASC text="+i+" Hz="+hz+" samples="+pcm.length+" energy="+energy(pcm));
    }
  }
}
JAVA
"$JAVA_HOME/bin/javac" -d "$TMP/classes" "$TMP/src/org/retro/flitetts/NativeBridge.java" "$TMP/src/org/retro/flitetts/AscHostRegression.java"
"$JAVA_HOME/bin/java" -Djava.library.path="$BUILD" -cp "$TMP/classes" org.retro.flitetts.AscHostRegression "$(dirname "$MODEL")"
