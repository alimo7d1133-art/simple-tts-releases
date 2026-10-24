# Verified upstream sources for Old TTS 1.2

## Arabic and American MBROLA
Official collection: https://github.com/numediart/MBROLA-voices
Engine: https://github.com/numediart/MBROLA (AGPL-3.0-or-later).
Voice collection terms: https://raw.githubusercontent.com/numediart/MBROLA-voices/master/LICENSE.md

Exact per-voice license and inventory were fetched/read at:
- https://raw.githubusercontent.com/numediart/MBROLA-voices/master/data/ar1/license.txt
- https://raw.githubusercontent.com/numediart/MBROLA-voices/master/data/ar1/README.txt
- https://raw.githubusercontent.com/numediart/MBROLA-voices/master/data/ar2/license.txt
- https://raw.githubusercontent.com/numediart/MBROLA-voices/master/data/ar2/README.txt
- https://raw.githubusercontent.com/numediart/MBROLA-voices/master/data/us1/license.txt
- https://raw.githubusercontent.com/numediart/MBROLA-voices/master/data/us1/README.txt
- https://raw.githubusercontent.com/numediart/MBROLA-voices/master/data/us2/license.txt
- https://raw.githubusercontent.com/numediart/MBROLA-voices/master/data/us2/README.txt
- https://raw.githubusercontent.com/numediart/MBROLA-voices/master/data/us3/license.txt
- https://raw.githubusercontent.com/numediart/MBROLA-voices/master/data/us3/README.txt

All five allow no-charge redistribution with original notices, only for use with MBROLA. Sale/incorporation in a sold product requires prior permission from the voice database owner. AR1: FPMs/Nawfal Tounsi; AR2: SEDCO; US1/US2: Babel Technology; US3: Oregon Graduate Institute. Voice data are not relicensed under AGPL. AR1/AR2 originated in 1998. American phones differ from EN1 (A/O/i/u/r=/EI/AI/OI/E versus British A:/O:/i:/u:/3:/eI/aI/OI/e). Official Arabic example https://raw.githubusercontent.com/numediart/MBROLA-voices/master/data/ar1/TEST/assalam.pho confirms distinct aa/ii/uu phones. Colored a./i./u. follow emphatics. Actual database initialization reports AR1 16000 Hz, AR2 22050 Hz, US1/US2/US3 16000 Hz.

## Standalone Klatt
https://github.com/rhdunn/klatt redirects to https://github.com/espeak-ng/klatt . This is the standalone GPL-3.0-or-later Klatt cascade/parallel synthesizer, not the excluded eSpeak engine. Full upstream parwave implementation and COPYING retained. Original Fortran by Dennis Klatt; C port Jon Iles/Nick Ing-Simmons; maintenance Reece H. Dunn. Accepts parameter frames, so Old TTS provides original approximate phone-to-formant schedule and Flite linguistic analysis. NATURAL is an algorithmic glottal source and does not require proprietary sample data.

## Other candidates not cleared or not ready
- https://www.text2speech.com/ fetched directly: advertises Windows DLL English/Spanish SVTTS licensing, royalties/subscription/fee. No Android redistribution grant. No contact was made.
- https://github.com/phyce/OpenSAPI4 fetched directly: Go reimplementation with 10 L&H TruVoice voices and Microsoft voices; voice packs converted from original Windows data are separate. GitHub API /repos/phyce/OpenSAPI4 license=null; /license returns404. No explicit code/data redistribution license established, so do not include either code or voice packs. No contact was made.
- https://github.com/dectalk/dectalk / develop/LICENCE contains proprietary FONIX terms, not a general redistribution grant.
- https://github.com/s-macke/SAM and https://github.com/vidarh/SAM describe reverse-engineered SoftVoice software without a clear redistribution license.
- https://github.com/nvaccess/NVSpeechPlayer README and source headers specify GPL version2 only, not version2-or-later. Do not link into this AGPL3 application without resolving compatibility. It also requires a separate linguistic frontend.
- https://github.com/rsprouse/klsyn contains C restriction against resale/commercial products despite BSD Python wrapper.
- Festival https://www.cstr.ed.ac.uk/projects/festival/ permits X11-style redistribution, but Android cross-build was not completed; do not advertise or register an unbuilt engine. Official KAL voice and CMU lexicon need separate preserved notices. Partial research is outside the shipped project.

## Open JTalk / HTS — tried, NOT SHIPPED
Official sources https://open-jtalk.sourceforge.net/ and https://hts-engine.sourceforge.net/ . Attempted versions Open JTalk1.11 and HTS engine API1.10, plus MeCab and UTF-8 NAIST-derived dictionary, original source and notices retained. Nitech nitech_jp_atr503_m001 voice explicitly CC-BY3.0 in original COPYING. Debian hts-voice-nitech-jp-atr503-m001 copyright additionally notes training used HTK non-free EULA; this package uses the published voice only, no HTK code or training toolkit is included. Japanese only, not Arabic. Android initialization exceeded the 120 second instrumentation timeout, before render.wav was produced. All engine code/data/registry entries have been removed from the shipped project; trial files remain outside it for future investigation.

## FreeTTS1.2.2
Official project https://freetts.sourceforge.io/ and full license https://freetts.sourceforge.io/license.terms . Sun/CMU BSD-style terms. Full source and original voice/dictionary notices retained. Android ICU rejects upstream [---+] in USEnglish; marked [-+] fix and rebuild script supplied. Runtime jar en_us.jar has only the documented rebuilt USEnglish/TokenToWords classes. Other runtime jars/data unchanged. KAL and KAL16 are bandwidth variants of one original speaker.

## Excluded proprietary engines
IBM TTS is outside this release scope. DECtalk's upstream https://raw.githubusercontent.com/dectalk/dectalk/develop/LICENCE requires a valid written FONIX license; no Android distribution grant is available for this release. No proprietary binaries/data or false registry entries are included. Standalone Klatt is explicitly labelled Klatt, not DECtalk.

## Additional Flite voices
BDL, CLB, JMK and KSP are the unmodified official 2017 Flite 2.3 models from http://festvox.org/flite/packed/flite-2.3/voices/. SHA256, speaker-page links and complete underlying CMU ARCTIC licensing terms are in app/src/main/assets/flite-extra/EXTRA-VOICES-NOTICE.txt. Permission is preserved from Appendix A of http://festvox.org/cmu_arctic/cmu_arctic_report.pdf; it states all database releases are BSD-style free software with commercial/noncommercial use. Base Flite collection COPYING is also preserved. No missing copyright metadata in a model is treated as a substitute for that original permission. Models remain unchanged; only the Android extraction/selection adapter is new.

## NeoSpeech, not GnuSpeechSA
Official source: https://www.readspeaker.com/blog/neospeech/. The former NeoSpeech brand/engine was consolidated into ReadSpeaker; ReadSpeaker handles licensing. No Android redistribution grant was supplied or found. No NeoSpeech binaries, language data or false registry entry are included. The company was not contacted. GnuSpeechSA is a different open articulatory engine already included.

## Android callback correctness
The native syntheses passed but the original pre-start rejection path left FileSynthesisCallback waiting: error() records status, while done() dispatches the error. Fixed fail() to call done() when start() has not happened; after start(), TextToSpeechService performs its normal completion. Verified against Android's API contract at https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android-9.0.0_r1/core/java/android/speech/tts/SynthesisCallback.java and FileSynthesisCallback.java. Rejection tests run first and must deliver an error callback within 15 seconds.
