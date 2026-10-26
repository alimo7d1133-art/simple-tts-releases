# Simple TTS 1.9.0 — voice packs and system voices

## What changed
* The base app keeps all small voices (Flite built-in set, Arabic Flite voices including ASC, ClArTTS, IQCorp, Common Voice male/female, the two Hi-Fi TTS American voices, Festival, MBROLA Arabic AR1/AR2, Pico, rsynth, Klatt, Gnuspeech, Flite+HTS SLT) and no longer contains the large voice data.
* Large voices are separate **voice packs** (ZIP files on the GitHub release page). Packs are never downloaded by the app: the app has no internet permission. The user downloads a ZIP, unzips it and moves the folder into `Simple TTS data/` in internal storage; the app verifies every file with SHA-256 against a manifest whose own SHA-256 is compiled into the app, copies the files to private storage and activates the voices. Packs can be deleted again from the **Voice packs** screen (the user's folder is never touched).
* Voices of a pack that is not installed are hidden from the voice list; a hint tells how many are hidden. Choosing an engine that has no available voice, or rendering a voice whose pack was removed, shows the exact pack, ZIP name and folder to use.
* New engine entry **Android installed voices**: uses the text-to-speech engines installed on the phone (Google, Samsung, ...) through `TextToSpeech.synthesizeToFile`. These engines are external and can be neural; the notice says so. Simple TTS itself is still not a system TTS engine and has no INTERNET permission.
* Upgrade from 1.8.0: packs that 1.8.0 already extracted are recognised, so nothing has to be downloaded again.

## Packs
| Pack | Content | ZIP | Installed |
|---|---|---|---|
| `flite-en-us-1` | American/Canadian English CMU ARCTIC, part 1 (bdl, clb, jmk, aew) | 34.3 MB | 43.7 MB |
| `flite-en-us-2` | American English CMU ARCTIC, part 2 (ahw, eey, fem, ljm, lnh, rxr) | 32.9 MB | 42.4 MB |
| `flite-en-in` | Indian-accent English CMU ARCTIC (ksp, aup, axb, gka, slp) | 28.8 MB | 37.1 MB |
| `flite-indic-hi` | Hindi (CMU Indic) | 23.6 MB | 29.5 MB |
| `flite-indic-bn` | Bengali (CMU Indic) | 6.7 MB | 8.5 MB |
| `flite-indic-gu` | Gujarati (CMU Indic, 3 voices) | 20.8 MB | 26.2 MB |
| `flite-indic-kn` | Kannada (CMU Indic) | 6.8 MB | 8.5 MB |
| `flite-indic-mr` | Marathi (CMU Indic, 2 voices) | 18.7 MB | 24.1 MB |
| `flite-indic-pa` | Punjabi (CMU Indic) | 6.0 MB | 7.7 MB |
| `flite-indic-ta` | Tamil (CMU Indic) | 8.4 MB | 10.8 MB |
| `flite-indic-te` | Telugu (CMU Indic, 3 voices) | 38.0 MB | 47.5 MB |
| `mbrola-english` | MBROLA English (EN1, US1, US2, US3) | 23.8 MB | 27.1 MB |
| `openjtalk-ja` | Open JTalk Japanese (dictionary and Nitech voice) | 23.3 MB | 103.4 MB |
| `ekho-cantonese` | Ekho Cantonese (CantoneseWong syllables) | 23.0 MB | 24.8 MB |

SHA-256 of every ZIP: `PACKS-SHA256SUMS.txt` on the release page.

## Not included in 1.9.0
Training of the American LJSpeech voice and the two Russian voices (RF9014, RM8169) did not finish with an intelligible, verified result in the time available, so they are not shipped. Nothing is announced that is not in the app.
