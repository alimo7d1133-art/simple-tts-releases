# Simple TTS 2.0.0 — Kilohertz experimental tone engine

Version 2.0.0 adds one optional engine, **Kilohertz**, and WAV export to the standalone Simple TTS app. Kilohertz is a small in-app sine-wave generator, not speech synthesis: it does not read, pronounce, encode or make input text intelligible.

WAV files can be saved as mono PCM with 8-, 16-, 24- or 32-bit containers. Speech engines generate PCM16; 24- and 32-bit WAV options store those same samples in wider containers and do not add source detail. This setting affects WAV only; MP3 export remains unchanged.

Enter 1–128 frequency values between 40 and 5000 Hz, separated by spaces, commas or semicolons. Bare numbers mean Hz; an explicit `kHz` suffix is converted to Hz. For example, `440, 660, 880, 1.2kHz` generates four sequential tones. Arabic-Indic digits and Arabic list separators are accepted. The app applies the existing speed control to tone duration and the pitch control to frequency. The output uses 48 kHz PCM and follows the existing listen, MP3, WAV and stop paths. A short fade at each tone edge reduces clicks; the notice advises keeping device volume low.

The engine is labelled as experimental tones in all six interface languages and the accessible input hint describes the frequency syntax. The existing speech engines and their data, licensing notices, AI-related exceptions, and deliberately excluded engines are otherwise unchanged. Simple TTS remains a standalone app, not an Android system TTS service; no network permission or new external dependency was added.

The upgrade identity remains `org.oldtts.android`; versionCode is 14 and versionName is `2.0.0`. The Android namespace and signing configuration were not changed. The project source does not include the private key corresponding to the previously published signing certificate, so this environment can build only the configured unsigned release APK. Do not sign it with a different key if it must install as an update over an existing signed copy.

The instrumentation suite now checks the version/package identity, all six localized engine labels and notices, frequency parsing (including Arabic digits), rejection of non-frequency input, tone generation, and the existing engine/voice totals. Building the instrumentation package does not itself constitute an on-device playback or accessibility test.
