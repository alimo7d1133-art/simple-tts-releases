# MBROLA integration

This module bundles the **AGPL-3.0 MBROLA engine** and the separately licensed
**EN1 diphone database**. EN1 is distributed only for use with MBROLA and its
complete terms are in `licenses/EN1-README.txt`; the engine license is in
`licenses/MBROLA-AGPL-3.0.txt`. Do not treat the engine license as a voice
license.

MBROLA has no text-to-phoneme frontend. `MbrolaPlugin` therefore runs the
bundled CMU Flite US English frontend (`flite_synth_text`), extracts its
Segment phonemes and durations, and feeds MBROLA's documented `write_MBR()`
phoneme-command API. This is **MBROLA + Flite frontend**, not native MBROLA
raw-text input and not eSpeak. Unknown phones are configured to become
silence (`setNoError_MBR(1)`), rather than pretending an unsupported mapping
is valid.

The upstream one-channel API explicitly provides `init_MBR`, `write_MBR`,
`flush_MBR`, and `read_MBR`; it is a synchronous string-in/sample-out API, not
an input callback API. The isolated CMake file builds the upstream source and
JNI adapter as `mbrolaretro`.
