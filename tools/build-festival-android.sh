#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
ABI=${1:?x86_64|x86|arm64-v8a|armeabi-v7a}
NDK=${2:-${ANDROID_NDK_HOME:-$HOME/android-sdk/ndk/29.0.14206865}}
API=${3:-26}
SRC="$ROOT/festival-runtime"
TMP=${FESTIVAL_BUILD_TMP:-${TMPDIR:-/tmp}/oldtts-festival-$ABI}
case "$ABI" in
 x86_64) TRIPLE=x86_64-linux-android; CT=x86_64-linux-android$API;;
 x86) TRIPLE=i686-linux-android; CT=i686-linux-android$API;;
 arm64-v8a) TRIPLE=aarch64-linux-android; CT=aarch64-linux-android$API;;
 armeabi-v7a) TRIPLE=armv7a-linux-androideabi; CT=armv7a-linux-androideabi$API;;
 *) exit 2;;
esac
LLVM="$NDK/toolchains/llvm/prebuilt/linux-x86_64"
export CC="$LLVM/bin/$CT-clang" CXX="$LLVM/bin/$CT-clang++"
export FESTIVAL_REAL_AR="$LLVM/bin/llvm-ar" FESTIVAL_REAL_RANLIB="$LLVM/bin/llvm-ranlib"
export FESTIVAL_ARCHIVE_LOCK="$TMP/archive.lock"
export AR="$ROOT/tools/festival-locked-ar.sh" RANLIB="$ROOT/tools/festival-locked-ranlib.sh"
export CFLAGS='-fPIC -O2 -g0' CXXFLAGS='-fPIC -O2 -g0 -std=gnu++11'
export PATH="$LLVM/bin:$PATH"
if [ "${FESTIVAL_REUSE_BUILD:-0}" != 1 ]; then
 mkdir -p "$TMP"
 python3 - "$SRC" "$TMP" <<'PY'
from pathlib import Path
import shutil,sys
src,out=map(Path,sys.argv[1:])
for name in ('speech_tools','festival'):
 if (out/name).exists():shutil.rmtree(out/name)
 shutil.copytree(src/name,out/name,ignore=shutil.ignore_patterns('*.o','*.a','*.so','*.lo','.git','config.log','config.cache'))
PY
 (cd "$TMP/speech_tools"; ./configure --host="$TRIPLE" --build=x86_64-pc-linux-gnu --with-pulseaudio=no CC="$CC" CXX="$CXX" AR="$AR" RANLIB="$RANLIB" SHARED=0)
 (cd "$TMP/festival"; ./configure --host="$TRIPLE" --build=x86_64-pc-linux-gnu CC="$CC" CXX="$CXX" AR="$AR" RANLIB="$RANLIB")
fi
python3 "$ROOT/tools/patch-festival-android.py" "$TMP"
# Archive tools use a shared lock because recursive make updates the same .a.
make -C "$TMP/speech_tools" -j"${JOBS:-6}" all AR="$AR" RANLIB="$RANLIB"
make -C "$TMP/festival/src" -j"${JOBS:-6}" arch modules AR="$AR" RANLIB="$RANLIB"
OUT="$SRC/prebuilt/$ABI"
mkdir -p "$OUT"
cp "$TMP/speech_tools/lib/"{libestools.a,libestbase.a,libeststring.a} "$TMP/festival/src/lib/libFestival.a" "$OUT/"
for file in "$OUT/"*.a; do "$LLVM/bin/llvm-ar" s "$file"; done
printf 'ABI=%s\nAPI=%s\nNDK=%s\n' "$ABI" "$API" "$(sed -n 's/Pkg.Revision = //p' "$NDK/source.properties")" > "$OUT/BUILD-METADATA.txt"
echo "Festival Android runtime ready: $ABI"
