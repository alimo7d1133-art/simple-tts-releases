#!/usr/bin/env python3
"""Validate real WAV/MP3 samples and package with provenance and notices."""
import argparse,array,hashlib,json,math,subprocess,sys,wave,zipfile
from pathlib import Path
p=argparse.ArgumentParser(description=__doc__);p.add_argument('delivery',type=Path);args=p.parse_args()
r=Path(__file__).resolve().parents[1];public=args.delivery/'public';samples=public/'samples';records=[]
for path in sorted(samples.rglob('*')):
 if not path.is_file() or path.suffix.lower() not in ['.wav','.mp3']:continue
 record={'file':path.relative_to(samples).as_posix(),'bytes':path.stat().st_size,'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'origin':'Android APK' if 'android' in path.parts else 'host preview'}
 if path.suffix=='.wav':
  with wave.open(str(path)) as f:
   assert f.getsampwidth()==2 and f.getnchannels()==1,path
   data=array.array('h',f.readframes(f.getnframes()))
   if sys.byteorder!='little':data.byteswap()
   assert data and max(abs(n) for n in data)>0,path
   record.update({'sample_rate':f.getframerate(),'seconds':len(data)/f.getframerate(),'rms':math.sqrt(sum(n*n for n in data)/len(data)),'peak':max(abs(n) for n in data),'clipped_samples':sum(abs(n)>=32767 for n in data)})
 else:
  info=json.loads(subprocess.check_output(['ffprobe','-v','error','-select_streams','a:0','-show_entries','stream=codec_name,sample_rate,channels:format=duration','-of','json',str(path)],text=True))
  stream=info['streams'][0];assert stream['codec_name']=='mp3' and int(stream['channels'])==1,path
  record.update({'codec':'mp3','sample_rate':int(stream['sample_rate']),'seconds':float(info['format']['duration'])})
 records.append(record)
assert len(records)>=35,'Missing new-voice clips'
(samples/'Audio-Checks.json').write_text(json.dumps(records,indent=2)+'\n')
readme='''# عينات الأصوات الجديدة في Simple TTS 1.6.0

خمسة أصوات جديدة فقط: AEW وAHW وAUP داخل Flite، وKurt داخل Festival، وCantoneseWong داخل محرك Ekho الجديد. عدد الملفات ليس عدد الأصوات.

مجلد android يحتوي تسجيلات وMP3 وWAV مولّدة فعليًا على محاكي Android 9 x86_64 من APK. normal وcantonese-16000 هما سرعة وطبقة طبيعيّتان 1×. الملفات المسماة rate أو pitch تعرض التغيير المذكور في اسمها. 48k/48000 تعني إعادة أخذ العينات إلى 48 كيلوهرتز، لا صوتًا جديدًا ولا استعادة تفاصيل مفقودة.

ملفات host-preview خارج android معاينات أُنتجت على Linux من النماذج نفسها، وليست دليل تشغيل أندرويد. لا توجد هنا تسجيلات مستوردة لأصوات أخطاء نظام؛ لم تُختلق أصوات أخطاء وتُنسب إلى نظام حقيقي.

نص Flite: Hello. This is a new classic voice in Simple TTS. One, two, three.
نص Kurt على Android: Hello. This is Kurt, a classic American English voice in Simple TTS.
نص الكانتونية: 你好，多謝你使用粵語離線語音合成。

Audio-Checks.json يسجل المعدل والمدة والبصمة وخصائص PCM، ويتحقق من أن MP3 ملف MP3 فعلي. هذه فحوص تقنية، وليست تقييمًا بشريًا لوضوح الكلام أو سلامة اللكنة.

إشعارات أصحاب المحركات والأصوات مرفقة داخل licenses. تحتفظ تسجيلات Wong برخصة GPL-2.0، وقاموسها CC BY-SA 3.0. مصدر المحركات وبيانات الأصوات الكاملة وتراخيصها ضمن Simple-TTS-1.6.0-source.zip المرفق بالإصدار. لا يُفهم من أسماء المؤلفين تأييدهم للتطبيق.
'''
(samples/'README-NEW-VOICES.md').write_text(readme)
assets=r/'app/src/main/assets';notices=[assets/'flite-extra/EXTRA-VOICES-NOTICE.txt',assets/'festival/KD-COPYRIGHT.txt',assets/'festival/CMU-LEXICON-COPYRIGHT.txt',assets/'festival/POSLEX-COPYRIGHT.txt']
notices.extend(sorted((assets/'ekho/licenses').glob('*')))
target=public/'Simple-TTS-1.6.0-New-Voice-Samples.zip'
with zipfile.ZipFile(target,'w',zipfile.ZIP_DEFLATED,compresslevel=6) as z:
 for row in records:z.write(samples/row['file'],'samples/'+row['file'])
 for name in ['Audio-Checks.json','README-NEW-VOICES.md']:z.write(samples/name,name)
 for notice in notices:
  assert notice.is_file(),notice
  z.write(notice,'licenses/'+notice.relative_to(assets).as_posix())
with zipfile.ZipFile(target) as z:assert z.testzip() is None
print('PASS',len(records),'real audio files validated;',target,target.stat().st_size,'bytes',flush=True)
