#!/usr/bin/env python3
"""Package the complete Simple TTS source and licensed runtime data, never signing keys."""
import os
import sys
import zipfile
from pathlib import Path

root = Path(__file__).resolve().parents[1]
out = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else root.parent / 'Simple-TTS-1.6.0-source.zip'
excluded_dirs = {'.git', '.svn', '.gradle', '.cxx', '.idea', '__pycache__', 'build', 'classes', 'klatt-classes', 'mbrola-classes', 'freetts-classes'}
excluded_suffixes = {'.o', '.a', '.so', '.class', '.pyc', '.jks', '.keystore', '.idsig', '.apk', '.log'}
excluded_names = {'local.properties', 'password', 'credentials', '.DS_Store'}
count = size = 0
with zipfile.ZipFile(out, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=6, allowZip64=True) as archive:
    for directory, dirs, files in os.walk(root, followlinks=False):
        dirs[:] = sorted(d for d in dirs if d not in excluded_dirs and not Path(directory, d).is_symlink())
        for name in sorted(files):
            path = Path(directory, name)
            required_festival_archive = path.suffix.lower() == '.a' and path.relative_to(root).parts[:2] == ('festival-runtime', 'prebuilt')
            if name in excluded_names or (path.suffix.lower() in excluded_suffixes and not required_festival_archive) or path == out:
                continue
            if not path.resolve().is_relative_to(root):
                raise RuntimeError('Refusing path outside project: ' + str(path))
            archive.write(path, 'Simple-TTS/' + path.relative_to(root).as_posix())
            count += 1
            size += path.stat().st_size
with zipfile.ZipFile(out) as archive:
    bad = archive.testzip()
    if bad:
        raise RuntimeError('Corrupt ZIP member: ' + bad)
    required = ['app/build.gradle', 'app/src/main/AndroidManifest.xml', 'app/src/main/java/org/retro/flitetts/ArabicFrontend.java',
                'app/src/main/cpp/klatt/upstream/parwave.c', 'freetts/source/license.terms', 'freetts/source/com/sun/speech/freetts/en/us/USEnglish.java',
                'app/libs/en_us.jar', 'app/src/main/assets/mbrola/ar1', 'app/src/main/assets/mbrola/ar2',
                'packs-src/mbrola-english/us1', 'packs-src/mbrola-english/us2', 'packs-src/mbrola-english/us3', 'README_AR.md',
                'packs-src/flite-en-us-1/cmu_us_bdl.flitevox', 'packs-src/flite-en-us-1/cmu_us_clb.flitevox',
                'packs-src/flite-en-us-1/cmu_us_jmk.flitevox', 'packs-src/flite-en-in/cmu_us_ksp.flitevox',
                'app/src/main/assets/flite-extra/EXTRA-VOICES-NOTICE.txt',
                'app/src/androidTest/java/org/retro/flitetts/StandaloneInstrumentation.java', 'gnuspeech/android/CMakeLists.txt', 'app/src/main/java/org/retro/flitetts/UiLocale.java',
                'app/src/androidTest/java/org/retro/flitetts/LocalizationInstrumentation.java',
                'app/src/main/res/values/strings.xml', 'app/src/main/res/values-ar/strings.xml',
                'app/src/main/res/values-de/strings.xml', 'app/src/main/res/values-es/strings.xml',
                'app/src/main/res/values-fr/strings.xml', 'app/src/main/res/values-it/strings.xml',
                'app/src/main/java/org/retro/flitetts/StandaloneAudio.java', 'app/src/main/java/org/retro/flitetts/Mp3Encoder.java',
                'app/src/main/cpp/mp3/CMakeLists.txt', 'app/src/main/cpp/mp3/lame/COPYING', 'app/src/main/cpp/mp3/lame/libmp3lame/lame.c',
                'app/src/main/java/org/retro/flitetts/RawiDiacritizer.java', 'app/src/main/java/org/retro/flitetts/Mp3Folder.java', 'app/src/main/assets/rawi/rawi_v3.int8.onnx',
                'app/src/main/assets/rawi/rawi_v3.vocab.json', 'app/src/main/assets/rawi/MODEL-NOTICE.txt',
                'app/src/main/assets/rawi/source/LICENSE-APACHE-2.0.txt', 'app/src/main/assets/LAME-COPYING.txt',
                'app/src/main/assets/ONNX-RUNTIME-LICENSE.txt', 'app/src/androidTest/java/org/retro/flitetts/RawiInstrumentation.java']
    required += ['app/src/main/java/org/retro/flitetts/EkhoPlugin.java', 'app/src/main/cpp/ekho/CMakeLists.txt', 'app/src/main/cpp/ekho/ekho_jni.cpp', 'app/src/main/cpp/ekho/disabled_features.cpp', 'app/src/main/cpp/ekho/sonic/sonic.c', 'app/src/main/cpp/ekho/libsndfile-src/sndfile.c', 'app/src/main/cpp/ekho/sr-convert/dsp.cpp', 'packs-src/ekho-cantonese/jyutping-wong-16000-v6/aa1.wav', 'packs-src/ekho-cantonese/zhy_list', 'packs-src/ekho-cantonese/zhy.dict', 'app/src/main/assets/ekho/licenses/COPYING-jyutping-wong', 'app/src/main/assets/ekho/licenses/CC-BY-SA-3.0.txt', 'packs-src/flite-en-us-1/cmu_us_aew.flitevox', 'packs-src/flite-en-us-2/cmu_us_ahw.flitevox', 'packs-src/flite-en-in/cmu_us_aup.flitevox', 'festival-voice-source/voices/english/ked_diphone/group/kedlpc16k.group', 'festival-lexicon-source/dicts/cmu/cmulex.scm', 'festival-lexicon-source/dicts/wsj.wp39.poslexR', 'app/src/main/assets/festival/CMU-LEXICON-COPYRIGHT.txt', 'app/src/main/assets/festival/KD-COPYRIGHT.txt', 'app/src/main/assets/festival/data.zip', 'app/src/main/cpp/openjtalk/CMakeLists.txt', 'app/src/main/java/org/retro/flitetts/OpenJtalkPlugin.java', 'ADDITIONS-1.6.0.md']
    for member in required:
        if 'Simple-TTS/' + member not in archive.namelist():
            raise RuntimeError('Missing source/data member: ' + member)
print(f'PASS source archive: {out}; {count} files; {size} uncompressed bytes; {out.stat().st_size} ZIP bytes; CRC verified; no signing keys.')
