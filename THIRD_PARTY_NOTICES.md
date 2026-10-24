# Third-party notices and license map

Simple TTS is a collection. The original project code is `AGPL-3.0-or-later` (see [LICENSE](LICENSE)); every other component, voice and data set below keeps its **own** license, which is stored beside it in this repository and in the APK (`app/src/main/assets/**`). This file is a map, not a replacement: when you redistribute, read the exact license files and keep them with the files they cover. It is an engineering summary, not legal advice. Author and institution names are attribution only and imply no endorsement.

Full AGPL-3.0 text: [LICENSE](LICENSE) (identical copy in `COPYING` and `app/src/main/assets/APP-LICENSE-AGPL-3.0.txt`). Other full texts: `FLITE-COPYING.txt`, `APACHE-2.0.txt`, and the per-component files listed below.

## 1. Project code

| Part | License | Where |
|---|---|---|
| Java app, engine plugins, JNI adapters, CMake integration, Arabic frontend for MBROLA, resampler, scripts, ClArTTS training scripts | AGPL-3.0-or-later | `app/src/main/java`, `app/src/main/cpp/*` (adapter files), `tools/`, `tests/`, `training/` |
| Marked modifications to upstream code (Android portability fixes: rsynth `opsynth.c`, FreeTTS `en_us.jar` regex fix, Festival/HTS Android build, Open JTalk EOF fix, Flite+hts_engine shared-voice fix, GnuspeechSA signed-char fix, Ekho Cantonese-only build) | Same license as the file they modify; changes are marked in the files and notices | see `ENGINE-SOURCES.md`, `NOTICE.txt` |

## 2. Engines and code components

| Component | License | Copyright / source | Where in repo |
|---|---|---|---|
| CMU Flite (rev `6c9f20dc`) | CMU BSD-style | Carnegie Mellon University, Alan W Black, Kevin A. Lenzo; <https://github.com/festvox/flite> | `flite/`, `FLITE-COPYING.txt`, `ACKNOWLEDGEMENTS` |
| Flite+hts_engine 1.07 | Modified BSD | Nagoya Institute of Technology | `app/src/main/cpp/flitehts`, `app/src/main/assets/flitehts/licenses/` |
| hts_engine API 1.10 | Modified BSD | Nagoya Institute of Technology, Tokyo Institute of Technology | `app/src/main/cpp/openjtalk/upstream/hts_engine_API-1.10` |
| SVOX Pico (AOSP `external/svox`) and six language packs | Apache-2.0 | SVOX AG, Android Open Source Project | `pico/`, `app/src/main/assets/PICO-NOTICE.txt` |
| rsynth 2005-12-16 | GPL-2.0-or-later and LGPL-2.0-or-later (core library files carry the Library GPL notice); formant tables only, no external dictionary | Nick Ing-Simmons and contributors; <https://sourceforge.net/projects/rsynth/> | `app/src/main/cpp/rsynth` (`COPYING`, `COPYING.LIB`, `COPYRIGHT-STATUS.txt`) |
| MBROLA engine | AGPL-3.0-or-later | Numediart / UMONS; <https://github.com/numediart/MBROLA> | `app/src/main/cpp/mbrola` |
| GnuspeechSA (code and English runtime data) | GPL-3.0-or-later | David R. Hill, Leonard Manzara, Craig Schock and contributors; <https://savannah.gnu.org/projects/gnuspeech/> | `gnuspeech/`, `app/src/main/assets/gnuspeech`, `GNUSPEECH-COPYING.txt` |
| Klatt cascade/parallel synthesizer | GPL-3.0-or-later | Dennis Klatt's algorithm; Jon Iles, Nick Ing-Simmons, Reece H. Dunn; <https://github.com/espeak-ng/klatt> (this is not the eSpeak engine) | `app/src/main/cpp/klatt`, `KLATT-COPYING.txt` |
| FreeTTS 1.2.2 | BSD-style (Sun / CMU) | Sun Microsystems, Carnegie Mellon University; <https://freetts.sourceforge.io/license.terms> | `freetts/`, `app/libs/*.jar`, `app/src/main/assets/freetts/` |
| Festival and Edinburgh Speech Tools | Permissive Edinburgh/CMU terms (MIT-like) | University of Edinburgh, Carnegie Mellon University | `festival-runtime/`, `app/src/main/assets/festival/*-COPYING.txt` |
| HTS engine as embedded in Festival | BSD-style | Nagoya Institute of Technology | `app/src/main/assets/festival/HTS-COPYING.txt` |
| Open JTalk 1.11 | Modified BSD | Nagoya Institute of Technology; <https://open-jtalk.sourceforge.net/> | `app/src/main/cpp/openjtalk/upstream/open_jtalk-1.11`, `app/src/main/assets/openjtalk/licenses/` |
| MeCab (bundled with Open JTalk) | BSD (as in the bundled `COPYING`) | Taku Kudo, NTT | `.../open_jtalk-1.11/mecab/COPYING` |
| Ekho core | GPL-2.0-or-later, used under GPLv3 | Cameron Wong; <https://github.com/hgneng/ekho> | `app/src/main/cpp/ekho` |
| sr-convert (in Ekho) | GPL-2.0-or-later, used under GPLv3 | Edward Kiser | `app/src/main/cpp/ekho/sr-convert` |
| libsndfile (in Ekho) | LGPL-2.1-or-later | Erik de Castro Lopo and contributors | `app/src/main/cpp/ekho/libsndfile-src` |
| Sonic (in Ekho) | LGPL-2.1 (see its `COPYING`) | Bill Cox | `app/src/main/cpp/ekho/sonic` |
| GSM codec code (in libsndfile/Ekho) | Permissive TU Berlin license (use permitted with notice kept) | Jutta Degener, Carsten Bormann | `app/src/main/assets/ekho/licenses/GSM-COPYRIGHT.txt` |
| UTF8-CPP (in Ekho) | Boost-style permissive | Nemanja Trifunovic | `app/src/main/assets/ekho/licenses/UTFCPP-NOTICE.txt` |
| LAME 3.100 (MP3 encoder, shared library) | LGPL-2.0 | The LAME project; <https://lame.sourceforge.io/> | `app/src/main/cpp/mp3/lame`, `LAME-COPYING.txt` |
| ONNX Runtime Android 1.31.0 (Maven dependency, runs the Rawi model) | MIT, with bundled third-party notices | Microsoft and contributors | `app/src/main/assets/ONNX-RUNTIME-LICENSE.txt`, `ONNX-RUNTIME-THIRD-PARTY-NOTICES.txt` |
| rapidxml (in GnuspeechSA) | Boost Software License 1.0 or MIT (user's choice) | Marcin Kalicinski | `gnuspeech/source/src/rapidxml/license.txt` |
| Android NDK / LLVM / C++ runtime | Apache-2.0 with LLVM exception and other notices | Google, LLVM project | `app/src/main/assets/NDK-NOTICE.txt` |
| Gradle wrapper | Apache-2.0 | Gradle Inc. | `gradle/wrapper` |

## 3. Voices, models and data

| Item | License | Source / attribution | Where |
|---|---|---|---|
| Flite voices KAL, KAL16, AWB, RMS, SLT | CMU BSD-style (Flite terms) | CMU / Kevin Lenzo, Alan W Black | `flite/` |
| Flite voices BDL, CLB, JMK, KSP, AEW, AHW, AUP, AXB, EEY, FEM, GKA, LJM, LNH, RXR, SLP (official `.flitevox`, unmodified) | CMU ARCTIC BSD-style terms (retain notice, mark modifications, keep author names) | John Kominek, Alan W Black, CMU-LTI-03-177 | `app/src/main/assets/flite-extra/`, `EXTRA-VOICES-NOTICE.txt` |
| HTS voice `cmu_us_arctic_slt` 1.06 | CC BY 3.0 plus CMU ARCTIC terms | HTS Working Group; CMU | `app/src/main/assets/flitehts/` |
| ASC Arabic Flite voice | MIT (voice code, asrajeh/arabic-tts); trained on Arabic Speech Corpus, CC BY 4.0 | Abdulrahman Alrajeh; Nawar Halabi, University of Southampton | `asc-voice-source/`, `app/src/main/assets/asc/` |
| ClArTTS Arabic Flite voice (statistical model trained for this project) | CMU Flite terms for the voice format; training data ClArTTS corpus CC BY 4.0 (corpus audio not included) | Kulkarni, Kulkarni, Shatnawi, Aldarmaki (MBZUAI), Interspeech 2023, doi:10.21437/Interspeech.2023-2224; LibriVox recording | `app/src/main/assets/clartts/CLARTTS-NOTICE.txt`, `training/arabic-clartts/` |
| Ziad Arabic HTS voice `ara_norm_ziad_hts` (model unchanged) | GPL-3.0; trained on Arabic Speech Corpus (CC BY 4.0) | Taha Zerrouki, Oussama Heminna, Karim Hemina; <https://github.com/linuxscout/festival-tts-arabic-voices> | `festival-voice-source/`, `app/src/main/assets/festival/VOICE-NOTICE.txt`, `ARABIC-VOICE-SOURCES.md` |
| Kurt `ked_diphone` English LPC/diphone voice | University of Edinburgh permissive terms (free use and distribution, notices retained) | CSTR, University of Edinburgh; Debian `festvox-kdlpc16k` | `festival-data/lib/voices/english/ked_diphone/LICENSES/` |
| CMU lexicon and POSLEX dictionaries | Permissive CMU / Edinburgh terms | CMU; University of Edinburgh | `festival-lexicon-source/`, `app/src/main/assets/festival/*-COPYRIGHT.txt` |
| Rawi v3 INT8 Arabic diacritizer (`rawi_v3.int8.onnx`) | Apache-2.0 | TigreGotico; GPU training by Mike Hansen | `app/src/main/assets/rawi/` |
| Open JTalk UTF-8 dictionary 1.11 (derived from NAIST-jdic) | NAIST BSD-style | Nara Institute of Science and Technology | `app/src/main/assets/openjtalk/open_jtalk_dic_utf_8-1.11/COPYING` |
| Nitech ATR503 male Japanese HTS voice 1.02 | CC BY 3.0 (see caveat in section 4) | Nagoya Institute of Technology, Tokyo Institute of Technology | `app/src/main/assets/openjtalk/voice/`, `.../hts_voice_nitech_jp_atr503_m001-1.02/` |
| FreeTTS KAL / lexicon data | BSD-style (FreeTTS / CMU) | Sun, CMU | `freetts/`, `app/libs/` |

## 4. Items that are not plain open source, or whose status is not fully settled

These are the places where the "everything open source" goal is not literally true. They are kept because the app needs them to build and run, they are not relicensed, and each is clearly documented.

1. **MBROLA voice databases EN1, US1, US2, US3, AR1, AR2 are free-of-charge, not open source.** Owners: EN1, Faculté Polytechnique de Mons from the Edinburgh CSTR ROGER database; AR1, FPMs / Nawfal Tounsi; AR2, SEDCO (via FPMs); US1 and US2, Babel Technology; US3, Oregon Graduate Institute. Terms (see `app/src/main/assets/mbrola/licenses/`): use *only* with the MBROLA program; distribution under the same terms as MBROLA with the original license and README files; the database may **not be sold or incorporated into any product that is sold** without the owner's prior permission. The AGPL does not apply to them, and nobody may relicense them. If you want a strictly free-software build, remove the MBROLA engine plugin and the six database directories `app/src/main/assets/mbrola/{en1,us1,us2,us3,ar1,ar2}`; the rest of the app does not depend on them.
2. **Ekho CantoneseWong recordings (`jyutping-wong-16000-v6`) are GPL-2.0-only**, and the Cantonese dictionary (`zhy_list`, `zhy.dict`) is CC BY-SA 3.0 (JyutDict, CC-CEDICT contributions). They are separate data files read at run time and are shipped with their own licenses, but GPL-2.0-only cannot be re-licensed as AGPL-3.0, and CC BY-SA 3.0 is not formally one-way compatible with GPLv3. Whether this aggregate is acceptable without an explicit additional permission from the author (Cameron Wong) was left open by the original integration notes: see [EKHO-INTEGRATION-ASSESSMENT.md](EKHO-INTEGRATION-ASSESSMENT.md). Treat this as **unresolved**; the safe option is to remove the Ekho plugin and `app/src/main/assets/ekho` or obtain permission.
3. **Nitech ATR503 Japanese voice: CC BY 3.0 with an HTK caveat.** The voice data carry an explicit CC BY 3.0 grant, but the Debian copyright file (kept at `app/src/main/assets/openjtalk/voice/COPYRIGHT`) records that the voice was created with the HTK toolkit, whose own EULA is non-free. HTK is not included, linked or needed at run time. Whether that matters for downstream use is not settled by the files here.
4. **Pre-built binaries in git:** `festival-runtime/prebuilt/<abi>/*.a` (Festival and Speech Tools static libraries, rebuildable with `tools/build-festival-android.sh`) and the FreeTTS jars in `app/libs/` and `freetts/lib/` (rebuildable for the changed classes with `tools/rebuild-freetts-android-port.sh`). The Open JTalk dictionary `sys.dic` is a compiled MeCab dictionary derived from the NAIST-jdic CSV that is also in the repository.
5. **Generated build files from third-party trees** (for example `config.status` and `Makefile` files in the LAME and Festival trees) were produced by those projects' `configure` steps and are kept as found; they may contain build-machine paths and are not used by the Android build.

## 5. Deliberately excluded

eSpeak and eSpeak NG engines and data, RHVoice, IBM TTS, DECtalk, SAM (Software Automatic Mouth), ClaritySynth voices, Saeeda/Acapela voices, Epos voice packs (GPL-2.0-only with a "no combination" clause), the Oxford Advanced Learner's Dictionary lexicon used by Festival RAB (non-commercial), HTK, the ClArTTS and ArVoice corpus audio, and any neural TTS engine. Reasons are in `ADDITIONS-*.md` and `ENGINE-SOURCES.md`.

## 6. No secrets or personal data

The repository contains no signing keys, keystores, passwords, tokens, or personal data about the people who built or published it. The public certificate fingerprint of the officially published APK appears in `test-evidence-*/signature.txt`; it identifies a certificate, not a person.
