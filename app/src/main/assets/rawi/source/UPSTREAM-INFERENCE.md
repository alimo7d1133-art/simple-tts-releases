# Rawi v3 inference provenance

Ported from the Apache-2.0 `TigreGotico/text2tashkeel` implementation at
commit `31eb5743d50047ec4ecaf8749f9ef2249ce33846` (`text2tashkeel/_models.py`).
The upstream model card is <https://huggingface.co/TigreGotico/rawi-v3>.

The Android adapter reproduces the relevant path: Unicode NFD; drop Unicode
`So` symbols and `Mn` combining marks for model input; map unknown characters
to `<UNK>`; run the `input` tensor; apply a value class only when sigmoid(`presence`) is greater than 0.5
(equivalently, the validated ONNX presence logit is greater than 0); restore only Arabic letters; NFC output.
Text is chunked at sensible boundaries around 300 characters. Latin, digits,
and punctuation are never given model marks. A word containing any existing
Arabic vowel/orthographic mark is returned byte-for-byte unchanged, which
preserves both fully and partially vowelled user input.
