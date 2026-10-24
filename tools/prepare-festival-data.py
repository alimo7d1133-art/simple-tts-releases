from pathlib import Path
import shutil, zipfile
ROOT=Path(__file__).resolve().parents[1]
UP=ROOT/'festival-voice-source'
DATA=ROOT/'festival-data'
LIB=DATA/'lib'
LIB.mkdir(parents=True,exist_ok=True)
# Some upstream Scheme files are read-only; make existing generated copies replaceable.
for generated in DATA.rglob('*'):
    if generated.is_file(): generated.chmod(generated.stat().st_mode | 0o200)
for p in list((ROOT/'festival-runtime/festival/lib').glob('*.scm')) + list((ROOT/'festival-runtime/festival/lib').glob('*.ngrambin')):
    shutil.copy2(p,LIB/p.name)
voice=LIB/'voices/arabic/ara_norm_ziad_hts'
shutil.copytree(UP/'voices/ara_norm_ziad_hts',voice,dirs_exist_ok=True)
# English lexicons are separate, unmodified, permissively licensed distributions.
shutil.copytree(ROOT/'festival-lexicon-source/dicts',LIB/'dicts',dirs_exist_ok=True)
# Kurt is the distinct classic LPC/diphone voice; its complete 16 kHz group database is redistributed with its own notice.
kd=LIB/'voices/english/ked_diphone'
shutil.copytree(UP/'voices/english/ked_diphone',kd,dirs_exist_ok=True)
lex=voice/'festvox/ara_norm_ziad_lexicon.scm'
s=lex.read_text().replace('/usr/share/festival/voices/arabic/ara_norm_ziad_hts/festvox/','festvox/')
lex.write_text(';;; Modified for Old TTS Android: resolve voice resources relative to the installed voice directory.\n'+s)
(LIB/'sitevars.scm').write_text(';;; Old TTS Android local runtime settings. No user configuration or external services.\n(set! home-directory "/nonexistent-oldtts-home")\n(set! voice_default \'voice_ara_norm_ziad_hts)\n(set! default-voice-priority-list \'(ara_norm_ziad_hts))\n')
# The upstream source and notices are retained independently of the runtime ZIP.
notices=ROOT/'app/src/main/assets/festival'
notices.mkdir(parents=True,exist_ok=True)
for generated in notices.iterdir():
    if generated.is_file(): generated.chmod(generated.stat().st_mode | 0o200)
for src,dst in [(UP/'LICENSE','VOICE-GPL-3.0.txt'),(UP/'AUTHORS.md','VOICE-AUTHORS.md'),((ROOT/'festival-runtime/festival/COPYING'),'FESTIVAL-COPYING.txt'),((ROOT/'festival-runtime/speech_tools/COPYING'),'SPEECH-TOOLS-COPYING.txt'),((ROOT/'festival-runtime/festival/src/modules/hts_engine/COPYING'),'HTS-COPYING.txt')]:
    shutil.copy2(src,notices/dst)
for name in ['CMU-LEXICON-COPYRIGHT.txt','POSLEX-COPYRIGHT.txt']:
    shutil.copy2(ROOT/'festival-lexicon-source'/name,notices/name)
(notices/'KD-COPYRIGHT.txt').write_text((UP/'voices/english/ked_diphone/LICENSES/COPYRIGHT').read_text())
(notices/'VOICE-NOTICE.txt').write_text('''Ziad Arabic HTS voice: ara_norm_ziad_hts
Kurt American English voice: ked_diphone (classic LPC/diphone, 16 kHz). It is not a neural voice and is not a duplicate of the CMU/HTS speakers.\nSource: https://github.com/linuxscout/festival-tts-arabic-voices\nAuthors: Taha Zerrouki, Oussama Heminna, Karim Hemina; preserve the complete bundled upstream AUTHORS and code notices.\nRepository license: GPL-3.0; see VOICE-GPL-3.0.txt. The model and voice source files are included in the accompanying source archive.\nTrained on Arabic Speech Corpus by Nawar Halabi, University of Southampton.\nCorpus source: https://en.arabicspeechcorpus.com/\nCorpus license: Creative Commons Attribution 4.0 International (CC BY 4.0).\nLicense: https://creativecommons.org/licenses/by/4.0/\nThe corpus website explicitly links and acknowledges this Festival voice project.\nAndroid modifications: private-data installation, relative voice-resource paths, JNI PCM rendering, runtime speed and pitch control, and shadda-before-vowel input normalization required by upstream LTS rules. The trained voice model is unchanged.\nNo original authors endorse this application. The software and voice are provided without warranty.\nFestival and Edinburgh Speech Tools retain their bundled permissive licenses; HTS engine notices remain in the included runtime sources.\n''')
with zipfile.ZipFile(notices/'data.zip','w',zipfile.ZIP_DEFLATED) as z:
    for p in DATA.rglob('*'):
        if p.is_file():z.write(p,p.relative_to(DATA).as_posix())
print('Festival runtime ZIP:',(notices/'data.zip').stat().st_size)
