# Ekho integration status

Implemented a real offline Cantonese plugin using the official Ekho frontend/core and the official CantoneseWong `jyutping-wong-16000-v6` 16-kHz voice recordings. No placeholder waveform and no eSpeak/RHVoice/IBM/DECtalk dependency is used.

## Licensing blocker / compatibility
The Ekho core is **GPL-2.0-or-later**. The CantoneseWong data archive is **GPL-2.0-only**. GPL-2.0-only is compatible with an AGPLv3 application only if the combined distribution is licensed under GPLv2-compatible terms; it cannot be relicensed as AGPLv3-only. Therefore parent must obtain an explicit GPLv3-or-later or AGPL-compatible additional permission from Cameron Wong, or distribute this Ekho-enabled variant under GPLv2-or-later (with complete corresponding source). Do not claim the current AGPLv3 app is legally cleared without that decision.

## Parent integration
1. Include `app/src/main/cpp/EkhoCMake.cmake` after the native target is created, then call `ekho_attach_to_target(<target>)`.
2. Add `EkhoPlugin` to the registry and include its instrumentation runner in the test manifest; do not change the plugin's sample rate (native output is 16,000 Hz). Existing HQ path may resample to 48 kHz.
3. Package `app/src/main/assets/ekho/**` unchanged and preserve both license notices.
4. Add `EkhoInstrumentation` to the test manifest. It verifies PCM, speed/pitch, 16/48-kHz WAV and MP3 exports.

The JNI bridge serializes the non-reentrant Ekho instance, sets `EKHO_DATA_PATH` to app-private extracted assets, and calls `Ekho::synth3` for actual PCM.
