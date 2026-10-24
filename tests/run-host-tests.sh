#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${JAVA_HOME:?Set JAVA_HOME to a JDK with JNI headers}"
cmake -S app/src/main/cpp -B build-host -DCMAKE_BUILD_TYPE=Release \
  "-DCMAKE_C_FLAGS=-I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux"
cmake --build build-host -j4
mkdir -p tests/classes tests/voice-previews
"${JAVA_HOME}/bin/javac" -d tests/classes app/src/main/java/org/retro/flitetts/NativeBridge.java tests/NativeTest.java
"${JAVA_HOME}/bin/java" -Djava.library.path=build-host -cp tests/classes org.retro.flitetts.NativeTest tests/voice-previews
