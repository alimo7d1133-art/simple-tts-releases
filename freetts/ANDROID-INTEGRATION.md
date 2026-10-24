# FreeTTS Android integration

The five local jars are registered in app/build.gradle and FreeTtsPlugin is registered in EngineRegistry. Complete source and data notices are retained.

Old TTS modifications: Android AudioFormat value shim, custom serialized big-endian PCM sink, rate/pitch controls, and Android ICU regex correction `[---+]` to `[-+]` in USEnglish. The original redundant range works in a desktop JDK but throws PatternSyntaxException on Android. Only en_us.jar has rebuilt classes; all other original jars/data remain unchanged.

Reproduce the portability jar with JDK17: `bash tools/rebuild-freetts-android-port.sh`. Then run Gradle normally. No network speech, Java Sound output or MBROLA delegation is used.

Original license full text in freetts/source/license.terms, freetts/notices and APK assets, from https://freetts.sourceforge.io/license.terms .
