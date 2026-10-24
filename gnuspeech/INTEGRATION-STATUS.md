# GnuSpeechSA in Old TTS

This is a real registered English articulatory synthesizer, not a placeholder.
Official source is in `source/` and GPL-3.0-or-later notices/data are retained.

`android/CMakeLists.txt` is included by the app's actual parent CMake build.
Upstream CMake omits `-march=native` and host-only CLI/install assumptions on
Android. The host build retains the original command-line tools.

`app/src/main/cpp/GnuspeechBridge.cpp` invokes the real English text parser,
phonetic parser, model and Controller and returns PCM16 at 44100 Hz. The JNI
adapter applies the native tempo and pitch offset, uses a separate temporary
parameter file rather than overwriting trm.config, and releases UTF-8 strings
safely on both normal and exception paths.

`GnuspeechPlugin` extracts the bundled English dictionary/configuration/model
assets to private storage and supplies one English male voice to EngineRegistry.
No proprietary voice or eSpeak frontend is used.

The Controller host harness generated 117538 samples, of which 105612 were
nonzero. The integrated app was built for arm64-v8a, armeabi-v7a, x86_64 and x86.
Actual final Android TTS validation and limitations are recorded separately in
TEST-RESULTS.txt; host/ABI build success alone is not a phone usability test.
