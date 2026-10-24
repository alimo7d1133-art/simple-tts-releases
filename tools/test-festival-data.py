#!/usr/bin/env python3
"""Offline integrity checks for the packaged Festival voices (no Android/Gradle needed)."""
from pathlib import Path
import zipfile
root=Path(__file__).resolve().parents[1]
z=root/'app/src/main/assets/festival/data.zip'
with zipfile.ZipFile(z) as f:
    names=set(f.namelist())
    required={
      'lib/voices/arabic/ara_norm_ziad_hts/festvox/ara_norm_ziad_hts.scm',
      'lib/voices/english/ked_diphone/festvox/ked_diphone.scm',
      'lib/voices/english/ked_diphone/group/kedlpc16k.group',
    }
    missing=required-names
    assert not missing, f'missing packaged Festival files: {sorted(missing)}'
    # The runtime may contain Festival's generic mbrola Scheme module; no MBROLA voice data is packaged.
    assert not any('/voices/mbrola' in n.lower() or 'sherpa' in n.lower() or 'espeak' in n.lower() for n in names)
for n in ('VOICE-NOTICE.txt','KD-COPYRIGHT.txt','FESTIVAL-COPYING.txt','SPEECH-TOOLS-COPYING.txt','HTS-COPYING.txt'):
    assert (root/'app/src/main/assets/festival'/n).is_file(), n
java=(root/'app/src/main/java/org/retro/flitetts/FestivalPlugin.java').read_text()
assert 'festival-160' in java and 'ked_diphone' in java and 'NativeFestival.render' in java
cpp=(root/'app/src/main/cpp/festival/FestivalPlugin.cpp').read_text()
assert 'voice_ked_diphone' in cpp and 'Duration_Stretch' in cpp and 'ara_norm_ziad_hts::hts_engine_params' in cpp
print(f'PASS Festival package integrity: {z.stat().st_size} bytes, KD LPC + Ziad HTS, separate notices')
