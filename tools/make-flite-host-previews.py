#!/usr/bin/env python3
"""Generate explicitly labelled host previews using the installed Flite library.
These are not evidence of Android APK execution. Uses unmodified bundled models.
"""
import argparse
import ctypes as c
from ctypes.util import find_library
from pathlib import Path
import wave

p=argparse.ArgumentParser(description=__doc__)
p.add_argument('output',type=Path)
a=p.parse_args();a.output.mkdir(parents=True,exist_ok=True)
root=Path(__file__).resolve().parents[1]
def library(name):
 path=find_library(name)
 if not path:raise RuntimeError('Install host Flite library: '+name)
 return c.CDLL(path,mode=c.RTLD_GLOBAL)
flite=library('flite');english=library('flite_usenglish');lexicon=library('flite_cmulex')
flite.flite_init.restype=c.c_int
flite.flite_init()
flite.flite_add_lang.argtypes=[c.c_char_p,c.c_void_p,c.c_void_p]
flite.flite_add_lang(b'eng',c.cast(english.usenglish_init,c.c_void_p),c.cast(lexicon.cmu_lex_init,c.c_void_p))
flite.flite_voice_load.argtypes=[c.c_char_p];flite.flite_voice_load.restype=c.c_void_p
flite.flite_text_to_wave.argtypes=[c.c_char_p,c.c_void_p];flite.flite_text_to_wave.restype=c.c_void_p
flite.cst_wave_save_riff.argtypes=[c.c_void_p,c.c_char_p];flite.cst_wave_save_riff.restype=c.c_int
flite.delete_wave.argtypes=[c.c_void_p]
for voice in ['aew','ahw','aup']:
 model=root/'app/src/main/assets/flite-extra'/('cmu_us_'+voice+'.flitevox')
 loaded=flite.flite_voice_load(str(model).encode());assert loaded,'Cannot load '+voice
 pcm=flite.flite_text_to_wave(b'Hello. This is a new classic voice in Simple TTS. One, two, three.',loaded);assert pcm,'No PCM '+voice
 target=a.output/(voice.upper()+'-Flite-host-preview.wav')
 status=flite.cst_wave_save_riff(pcm,str(target).encode());flite.delete_wave(pcm)
 assert status==0 and target.is_file(),'Cannot save '+voice
 with wave.open(str(target)) as w:
  assert w.getnframes()>0
  print(target,'Hz='+str(w.getframerate()),'seconds='+str(round(w.getnframes()/w.getframerate(),3)),flush=True)
