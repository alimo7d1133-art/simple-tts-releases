# Old TTS 1.5.0 — sources and distribution notes

The registry contains nine real implementations and 30 selectable voice/model options. This is not a claim of 30 distinct recorded speakers. The source archive includes runtime data and original notices; individual component terms are not replaced by the integration's AGPL-3.0-or-later license.

## Included engines

- **Flite:** [upstream](https://github.com/festvox/flite), revision `6c9f20dc915b17f5619340069889db0aa007fcdc`. CMU/BSD-style notices retained. KAL, KAL16, AWB, RMS, SLT, BDL, CLB, JMK, KSP and the Arabic ASC model are included. ASC comes from [asrajeh/arabic-tts](https://github.com/asrajeh/arabic-tts), with its MIT notice and attribution to Arabic Speech Corpus (CC BY 4.0).
- **SVOX Pico:** [AOSP source](https://android.googlesource.com/platform/external/svox/), including six official language model pairs and Apache-2.0/SVOX notices.
- **rsynth:** [2005-12-16 release](https://sourceforge.net/projects/rsynth/files/rsynth/2005-12-16/). Original GNU Library GPL 2-or-later core notices and GPL/LGPL texts retained. The Android `opsynth.c` portability patch uses `!isfinite(x)` rather than assuming ordered glibc FP classification constants. No Beep dictionary was imported.
- **MBROLA:** [engine](https://github.com/numediart/MBROLA), AGPL-3.0-or-later; [voice databases](https://github.com/numediart/MBROLA-voices) EN1, AR1, AR2, US1, US2 and US3. Each database retains its own README/license. These database terms permit gratis redistribution with notices and use with MBROLA; sale or incorporation in a sold product requires permission. They are **not** relicensed as AGPL. Flite supplies the English frontend; Arabic uses the app's rules-based frontend. MBROLA synthesizes the actual PCM.
- **GnuSpeechSA:** [GNU project](https://savannah.gnu.org/projects/gnuspeech/) and [source tree](https://git.savannah.gnu.org/cgit/gnuspeech/gnuspeechsa.git/tree/), GPL-3.0-or-later. Source, English data and Android build patches are included. Upstream `-march=native` is disabled on Android.
- **Standalone Klatt:** [independent core](https://github.com/espeak-ng/klatt), GPL-3.0-or-later. This is the separate Klatt cascade/parallel implementation, not the excluded eSpeak engine and not commercial DECtalk. The app adds a Flite-phone formant mapper. Two excitation modes are exposed, not two recorded speakers.
- **FreeTTS 1.2.2:** [project](https://freetts.sourceforge.io/) and [license](https://freetts.sourceforge.io/license.terms), Sun/CMU BSD-style terms. Full source, KAL/KAL16 data and lexicons are included. It performs its own Java synthesis. The Android ICU numeric-regex patch is marked in `USEnglish`; `tools/rebuild-freetts-android-port.sh` rebuilds changed `en_us.jar` classes from the included source.
- **Festival HTS:** [Festival](https://github.com/festvox/festival), Edinburgh Speech Tools, Scheme runtime and HTS are included with their original permissive notices and complete source. The Android cross-build and JNI integration are real. [Ziad Arabic model](https://github.com/linuxscout/festival-tts-arabic-voices) retains GPL-3.0 and Arabic Speech Corpus CC BY 4.0 attribution. See `ARABIC-VOICE-SOURCES.md`. ASC and Ziad use the same speech corpus, not independent recorded speakers.
- **Open JTalk — added in 1.5.0:** [official project](https://open-jtalk.sourceforge.net/), Open JTalk 1.11, hts_engine API 1.10, bundled MeCab and Open JTalk UTF-8 dictionary 1.11. Source and BSD-style notices are included under `app/src/main/cpp/openjtalk/upstream`; dictionary-specific terms are retained beside the dictionary. [Nitech ATR503 male voice](https://open-jtalk.sourceforge.net/readme_hts_voice_nitech_jp_atr503_m001.php), CC BY 3.0, is Japanese-only and operates natively at 48000 Hz. Original Nitech/Tokyo Institute of Technology attribution and full CC BY terms are included. Its Debian copyright notice records that the training toolkit HTK is non-free; HTK itself is not included, linked or required at runtime. The distributed voice data have their own explicit CC BY 3.0 grant.

## Open JTalk Android changes

A separate `libopenjtalk.so` target contains the original front end, HTS engine and MeCab code. The app's JNI adapter extracts bundled data locally, serializes engine use, applies speed and pitch, bounds text/output size and returns real PCM16. The source port retains EOF in an `int` in three HTS file-token readers, so ARM's unsigned plain `char` cannot discard the EOF sentinel. No network service, external TTS app or substitute voice is used. See `TEST-RESULTS.txt` for actual runtime coverage; building all four ABIs is not the same as testing all four on devices.

## Other included components

LAME is a local MP3 encoder, not a synthesis engine; its LGPL source is included. Rawi Arabic diacritization and ONNX Runtime retain Apache-2.0 and MIT notices respectively. Upsampling to 48 kHz does not recover frequency information absent from the source model.

## Not included

No eSpeak, RHVoice, IBM TTS or DECtalk runtime is shipped. No proprietary Saeeda/Acapela archive or ClaritySynth neural/formant voice was used. SAPI/SAM Windows binaries and BestSpeech/Keynote wrappers do not grant redistribution rights to their external proprietary engines. Festvox training tools and paper/demo audio alone are not additional runtime voices. See `ARABIC-ADDITIONS-1.5.0.md` for why no additional distinct Arabic model was added and `EKHO-INTEGRATION-ASSESSMENT.md` for the separate Ekho assessment.

This is an engineering distribution assessment, not legal advice. Preserve all original licenses and required author attributions when sharing; author names do not imply endorsement.
