from pathlib import Path
import re,sys
root=Path(sys.argv[1]);st=root/'speech_tools';fe=root/'festival'
p=st/'config/config';s=p.read_text()
s=re.sub(r'(?m)^\s*INCLUDE_MODULES\s*\+=\s*(?:NATIVE_AUDIO|EDITLINE|SIOD_PYTHON)\s*$', '# Disabled in Android local PCM runtime',s)
for key in ('OMP_OPTS','OMP_DEFS','TERMCAPLIB','CONFIG_WRAPPER_LANGUAGES','CONFIG_PYTHON_INCLUDES'):
 s=re.sub(r'(?m)^\s*'+key+r'\s*=.*$',key+'=',s)
p.write_text(s)
p=st/'Makefile';s=p.read_text().replace('LIB_BUILD_DIRS = utils','LIB_BUILD_DIRS = audio utils')
s=s.replace('BUILD_DIRS = $(LIB_BUILD_DIRS) lib main scripts testsuite bin','BUILD_DIRS = $(LIB_BUILD_DIRS) lib');p.write_text(s)
p=fe/'src/modules/hts_engine/HTS_misc.c';s=p.read_text()
if '      fpos_t pos;' in s:
 a=s.index('      fpos_t pos;',s.index('size_t HTS_ftell'));b=s.index('\n   }',a)
 s=s[:a]+'''      /* Android Bionic fpos_t is opaque/platform-dependent; use the
         standard C file-position API (voice files are below 2 GiB). */
      long pos = ftell((FILE *) fp->pointer);
      return pos < 0 ? 0 : (size_t) pos;'''+s[b:]
p.write_text(s)
p=fe/'src/modules/hts_engine/fest2hts_engine.cc';s=p.read_text()
if 'HTS_Engine_add_half_tone(engine,' not in s:
 anchor='  HTS_Engine_set_speed(engine, speech_speed);'
 assert anchor in s,'HTS speed anchor absent'
 s=s.replace(anchor,anchor+'''\n  /* -fm is an absolute semitone offset, set anew for each utterance. */
  HTS_Engine_add_half_tone(engine, (double)get_param_float("-fm", hts_engine_params, 0.0));''')
p.write_text(s)
print('Applied portable ftell, disabled host audio/editline/python/OpenMP, enabled absolute -fm HTS pitch.')
