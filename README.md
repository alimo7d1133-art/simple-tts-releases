# Simple TTS

Simple TTS is a standalone, fully offline Android app that turns text into speech, plays it, and saves it as MP3. It offers **12 engine entries and 63 built-in voice options** (plus whatever text-to-speech engines are installed on the phone) (English, Arabic, Japanese, Cantonese and more) and needs no internet connection, no account and no cloud service. It is **not** an Android system TTS engine: it cannot be selected as the default engine or used by TalkBack.

- Package id: `org.oldtts.android` (kept stable so updates install over older versions)
- Version: `1.9.0` (versionCode `12`), minSdk 26 (Android 8), targetSdk 36
- ABIs: `arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86`
- UI languages: Arabic (RTL), English, German, Spanish, French, Italian
- Arabic documentation: [README_AR.md](README_AR.md) (full feature description, in Arabic)

**Voice packs (new in 1.9.0):** the app itself is much smaller because the large voice data (English Flite voices, Indian-language voices, MBROLA English, Open JTalk Japanese and Ekho Cantonese) is split into 14 separate packs published as ZIP files on the release page. The app has **no INTERNET permission** and never downloads anything. To add a pack: download its ZIP, unzip it, move the folder into `Simple TTS data/` in the phone's internal storage (the app asks for file access only for this), and open the app: it verifies every file with SHA-256 against a manifest whose hash is compiled into the app, copies the files to private storage and activates the voices. The *Voice packs* screen lists every pack with its size and status and can delete a pack again (the folder you copied is never touched). Voices of packs that are not installed are hidden and the app says which pack to add. Arabic basics (MBROLA AR1/AR2, Flite ASC, ClArTTS, IQC, CVM, CVF, Festival Ziad), Pico, rsynth, Klatt, Gnuspeech, FreeTTS, HF92/HM6097 and the SLT HTS voice stay inside the app. Updating from 1.8.0 keeps the voices that were already extracted. A new engine entry, *Android installed voices*, lists the voices of other TTS engines installed on the phone; those engines are not part of this project and may be neural. See [ADDITIONS-1.9.0.md](ADDITIONS-1.9.0.md).

**Per-ABI builds (smaller downloads):** release 1.9.0 also has one APK per architecture (`arm64-v8a`, `armeabi-v7a`) with the same package, versionCode and signing certificate. To build one: `./gradlew :app:assembleRelease -PsimpleTtsAbis=arm64-v8a`. Without the property all four ABIs are built into one universal APK.
**Download the built app:** [Releases](../../releases) (APK and a source archive, with SHA-256 sums in the release notes).

## What is in this repository

This repository is the complete corresponding source of the 1.9.0 release: the Android app, all engine sources and runtime data, voice models, build scripts, tests, test evidence, and the license notices of every component. Nothing needed to build is left out. Signing keys and credentials were never part of the source and are not here.

| Path | Contents |
|---|---|
| `app/` | Android app: Java UI and engine plugins (`app/src/main/java`), JNI/CMake native code (`app/src/main/cpp`), bundled voices, models and license files (`app/src/main/assets`), instrumentation tests (`app/src/androidTest`) |
| `packs-src/` | The exact files of the 14 voice packs (voice data and manifests); `tools/make-packs.py` zips them |
| `flite/` | CMU Flite source (revision `6c9f20dc915b17f5619340069889db0aa007fcdc`) |
| `pico/` | SVOX Pico from AOSP `external/svox`, with the six official language packs |
| `gnuspeech/` | GnuspeechSA source and English data with Android build patches |
| `freetts/` | FreeTTS 1.2.2 source, jars and KAL data (Android port rebuilt by `tools/rebuild-freetts-android-port.sh`) |
| `festival-runtime/`, `festival-data/`, `festival-lexicon-source/`, `festival-voice-source/`, `asc-voice-source/` | Festival, Edinburgh Speech Tools, HTS runtime and the voice and lexicon data (Ziad Arabic, Kurt English, ASC) |
| `training/voices-1.8/` | Data-preparation, Clustergen training and Flite conversion scripts for the 1.8.0 trained voices (no corpus audio is included) |
| `training/arabic-clartts/` | Scripts, text list and MCD report used to train the ClArTTS Arabic voice (no corpus audio is included) |
| `tools/`, `tests/` | Build helpers (Festival cross-build, source packaging, FreeTTS port) and host tests |
| `docs/`, `test-evidence-*/` | Test results and raw logs for 1.5.0 to 1.9.0 |
| `ADDITIONS-*.md`, `ENGINE-SOURCES.md`, `MODULES.md`, `*-ASSESSMENT.md` | Engineering notes: what was added in each release, where each engine came from, what was evaluated and rejected and why (some are written in Arabic) |

C/C++ sources for MBROLA, rsynth, Klatt, Ekho (with libsndfile, Sonic, sr-convert, UTFCPP), LAME 3.100, Open JTalk 1.11 (with hts_engine API 1.10 and MeCab) and Flite+hts_engine live under `app/src/main/cpp/`.

## Engines and voices

| Engine | Voice options |
|---|---|
| Flite (40) | KAL, KAL16, AWB, RMS, SLT, BDL, CLB, JMK, KSP, AEW, AHW, AUP, AXB, EEY, FEM, GKA, LJM, LNH, RXR, SLP (English variants and accents), **ASC** (Arabic) and **ClArTTS** (Classical Arabic, male); new in 1.8.0: Arabic **IQC** (male, Iraqi corpus MSA), **CVM** and **CVF** (Common Voice, male and female), American English **HF92** (female) and **HM6097** (male) from Hi-Fi TTS, and 13 official CMU Indic voices: Hindi HIN_AB, Bengali BEN_RM, Gujarati GUJ_AD/GUJ_DP/GUJ_KT, Kannada KAN_PLV, Marathi MAR_AUP/MAR_SLP, Punjabi PAN_AMP, Tamil TAM_SDR, Telugu TEL_KPN/TEL_SK/TEL_SS |
| Flite + hts_engine | CMU ARCTIC SLT HTS, American English, female, 48 kHz |
| SVOX Pico | en-US, en-GB, de-DE, es-ES, fr-FR, it-IT |
| rsynth | English formant synthesizer |
| MBROLA | EN1, US1, US2, US3 (English) and AR1, AR2 (Arabic) |
| GnuspeechSA | English articulatory synthesis |
| Klatt | English cascade/parallel formant synthesizer, two excitation modes |
| FreeTTS | KAL and KAL16 (pure Java) |
| Festival | Ziad (Arabic HTS, 48 kHz), Kurt (American English LPC/diphone, 16 kHz) |
| Open JTalk | Nitech ATR503 (Japanese, 48 kHz) |
| Ekho | CantoneseWong (Cantonese, 16 kHz) |

The count is options, not unique speakers: KAL and KAL16 are one speaker at two bandwidths, the two Klatt options are excitation modes, and ASC and Ziad are both trained on the Arabic Speech Corpus. The Arabic voices are Flite ASC, Flite ClArTTS (a different speaker, best with fully diacritized text), Festival Ziad (HTS), and MBROLA AR1 and AR2. An optional on-device diacritizer (Rawi v3, via ONNX Runtime) can add Arabic diacritics. Details and limits are in [ADDITIONS-1.8.0.md](ADDITIONS-1.8.0.md), [ADDITIONS-1.7.0.md](ADDITIONS-1.7.0.md) and [README_AR.md](README_AR.md).

Not included, deliberately: eSpeak, RHVoice, IBM TTS, DECtalk, SAM, ClaritySynth, the Saeeda/Acapela pack, Epos voices, and any neural TTS engine. Each had missing or incompatible redistribution rights; the reasons are written down in the `ADDITIONS-*.md` files.

## Building

Requirements: JDK 17, Android SDK 36, NDK `29.0.14206865`, CMake `3.22.1`, Android Gradle Plugin 8.13.2 and the Gradle wrapper (Gradle 8.14.3); the last two are downloaded automatically. Linux is the tested host. The first build needs internet access to download Gradle and Maven dependencies; the installed app never does.

```sh
export ANDROID_HOME=/path/to/android-sdk            # or create local.properties with sdk.dir=...
export ANDROID_NDK_HOME=$ANDROID_HOME/ndk/29.0.14206865
chmod +x gradlew tools/*.sh
./gradlew :app:assembleRelease
# output: app/build/outputs/apk/release/app-release-unsigned.apk
```

The release build is **unsigned**; sign it with your own key (`apksigner`). An APK you sign yourself will not install as an update over the officially published APK, because Android requires the same signing certificate (the published APK's certificate SHA-256 is `d6dd597bff7d93705d810318f5540c989a9b2ea511fc185449d62805966ab75c`). Uninstall first, or change the package id.

Prebuilt Festival static libraries for the four ABIs are committed in `festival-runtime/prebuilt/<abi>/` because the Festival cross-build is slow. They can be regenerated on Linux:

```sh
for abi in arm64-v8a armeabi-v7a x86_64 x86; do
  ./tools/build-festival-android.sh "$abi" "$ANDROID_NDK_HOME" 26
done
python3 tools/prepare-festival-data.py
```

Useful extras:

- `./gradlew :app:assembleDebugAndroidTest :app:lintRelease` builds the instrumentation tests and runs lint.
- `-PsimpleTtsBuildDir=/absolute/scratch/app` moves Gradle outputs to a scratch directory.
- `bash tests/run-host-tests.sh` (needs `JAVA_HOME`) builds the native engines for the host and runs the JNI tests; `bash tests/resampler/run-host-test.sh` tests the 48 kHz resampler.
- `python3 tools/run-android-tests.py --output android-test-results` runs the instrumentation suites on a connected test device or emulator. These tests change app preferences and write sample files, so do not run them on a device with data you care about.

The large Open JTalk dictionary `app/src/main/assets/openjtalk/open_jtalk_dic_utf_8-1.11/sys.dic` (about 98 MiB) is stored directly in git; it is under GitHub's 100 MiB file limit, so Git LFS is not used. Its source CSV is in the Open JTalk tree under `app/src/main/cpp/openjtalk/upstream/`. A clone is therefore large (several hundred MB).

## What has actually been tested

The 1.9.0 voice-pack tests are in [docs/TEST-RESULTS-1.9.0.txt](docs/TEST-RESULTS-1.9.0.txt) (with raw logs in [test-evidence-1.9.0/](test-evidence-1.9.0/)), including what was not tested. The general claims below come from [docs/TEST-RESULTS-1.7.0.txt](docs/TEST-RESULTS-1.7.0.txt) and the raw logs in [test-evidence-1.7.0/](test-evidence-1.7.0/).

- **Tested:** all 63 voice options produce real PCM and MP3, at normal, fast, low-pitch and 48 kHz settings, on an Android 9 (API 28) x86_64 emulator without hardware acceleration and in airplane mode. Installing over a previous version worked with an identical signing certificate. Localization (six languages, RTL), the engine registry and license file presence passed. `assembleRelease`, `assembleDebugAndroidTest` and `lintRelease` succeeded for all four ABIs with 16 KiB page alignment.
- **Not tested:** any real phone; any ARM device (ARM builds compile but were never run); Android versions newer than 9; human TalkBack testing; human listening evaluation of voice quality or accent; saving MP3 to the shared folder with runtime permissions (the emulator's permission dialogs could not be automated, so this is unverified in 1.6.0 and 1.7.0); the permission-denied path.
- **This repository snapshot** was assembled from the release source archive without changing code, apart from replacing hard-coded sandbox paths in four helper scripts (two in `tools/`, two in `training/arabic-clartts/`) with relative or environment-variable paths. It has not been rebuilt from a fresh clone of this repository.
- Known quality limits: the ClArTTS and ASC Arabic voices are statistical and robotic and need diacritized text; Arabic MBROLA uses a simple rule-based frontend; text is limited to 4000 characters per request; `MANAGE_EXTERNAL_STORAGE` (needed on Android 11+ to create the shared folder) is restricted on Google Play, so the app is intended for direct APK distribution.

## License

The original code of this project (the Java app, the JNI and CMake integration, the Arabic frontend, the resampler, and the scripts written for this project) is licensed under the **GNU Affero General Public License, version 3 or (at your option) any later version** (`AGPL-3.0-or-later`). See [LICENSE](LICENSE). That choice follows the project's earlier releases and fits the components: the bundled MBROLA engine is AGPL-3.0-or-later, and GnuspeechSA, Klatt, the Ziad voice and (through their GPLv3 option) Ekho and sr-convert are GPL-3.0 or GPL-2.0-or-later, which can be combined under AGPL-3.0. Apache-2.0 (Pico, Rawi, Gradle wrapper), MIT (ONNX Runtime, ASC voice code), BSD-style (Flite, Festival, FreeTTS, Open JTalk, hts_engine, CMU ARCTIC) and LGPL (LAME, libsndfile, Sonic, rsynth core) components are compatible with it.

**Not everything in this repository is under the project license, and not everything is open source.** Every upstream component and every voice or data set keeps its own license, stored next to it. [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) lists them all and flags the exceptions. The important ones:

- The six **MBROLA voice databases** (EN1, US1, US2, US3, AR1, AR2) are free of charge but are *not* open source: they may only be used with the MBROLA program, may be redistributed with their notices, and may not be sold or put into a product that is sold without the database owner's permission.
- The **Ekho CantoneseWong recordings** are GPL-2.0-only and the Cantonese dictionary is CC BY-SA 3.0; they are separate data files and are not relicensed. Whether they can sit in one AGPL-3.0 combination without the author's additional permission is an open question documented in [EKHO-INTEGRATION-ASSESSMENT.md](EKHO-INTEGRATION-ASSESSMENT.md).
- The **Nitech Japanese voice** is CC BY 3.0, but Debian notes that it was trained with the non-free HTK toolkit. HTK itself is not included or needed.

This is an engineering summary, not legal advice. Author and institution names in the notices are attribution only and do not imply that anyone endorses this app.

## Contributing

Issues and pull requests are welcome. Please keep every upstream notice intact, do not add engines or voices whose redistribution rights are unclear, and never commit signing keys or credentials.
