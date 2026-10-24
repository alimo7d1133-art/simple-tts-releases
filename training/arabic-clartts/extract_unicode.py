import pyarrow.parquet as pq, glob, os, re, unicodedata, numpy as np
from scipy.io import wavfile
root=os.environ.get('TRAIN_ROOT','arabic-train-clartts'); os.makedirs(root+'/wav',exist_ok=True)
# Same phone inventory as ArabicFrontend.java; explicit diacritized ClArTTS text is used.
vals=dict(zip('بتثجحخدذرزسشصضطظعغفقكلمنهويء', ['b','t','T','j','H','x','d','D','r','z','s','S','s.','d.','t.','z.','E','G','f','q','k','l','m','n','h','w','j','?']))
vals.update({'أ':'?','إ':'?','ؤ':'?','ئ':'?','ک':'k','ی':'j','ة':'h','ا':'a','ى':'aa','آ':'? aa'})
marks=set(chr(x) for x in range(0x64b,0x660))|{'ٰ'}
def vowel(m):
 if any(x in m for x in 'ًَ'): return 'a'
 if any(x in m for x in 'ٍِ'): return 'i'
 if any(x in m for x in 'ٌُ'): return 'u'
 return None
def phones(word):
 letters=[]
 for c in word:
  if c in marks:
   if letters: letters[-1][1]+=c
  elif c in vals: letters.append([c,''])
 out=[]; article=len(letters)>2 and letters[0][0]=='ا' and letters[1][0]=='ل'; sun=article and letters[2][0] in 'تثدذرزسشصضطظلن'
 for i,(c,m) in enumerate(letters):
  if sun and i==1: continue
  if c in 'اىآ':
   if c=='آ': out += ['?','aa']
   elif i==0: out += (['?'] if not article else []) + [vowel(m) or 'a']
   else: out += ['aa']
   continue
  ph=vals.get(c)
  if not ph: continue
  if c=='ة': ph='t' if m else 'h'
  emph=ph.endswith('.')
  sh='ّ' in m or (sun and i==2)
  out.append(ph); v=vowel(m); tan=any(x in m for x in 'ًٌٍ')
  if v and not tan and i+1<len(letters):
   nc,nm=letters[i+1]
   if (v=='a' and nc in 'اى') or (v=='i' and nc=='ي' and not nm) or (v=='u' and nc=='و' and not nm):
    out.append(v+v); i+=1; continue
  if v: out.append((v+'.' if emph else v))
  if tan: out.append('n')
 return out
arabic=re.compile(r'[\u0600-\u06ff]')
kept=0; totaldur=0.; seen=set()
for fn in sorted(glob.glob(root+'/data-train-*.parquet')):
 t=pq.read_table(fn,columns=['text','file','audio','sampling_rate','duration'])
 for row in t.to_pylist():
  text=row['text'] or ''; dur=float(row['duration'] or 0)
  if not (2<=dur<=10): continue
  if any(('0'<=c<='9') or ('A'<=c<='Z') or ('a'<=c<='z') for c in text): continue
  if not any(arabic.match(c) for c in text): continue
  toks=[]
  for w in re.findall(r'[\u0600-\u06ff]+',text): toks += phones(w)
  if len(toks)<3: continue
  base='clartts_%04d'%kept
  with open(root+'/etc_txt_unicode.tmp','a',encoding='utf8') as f: f.write('( %s "%s" )\n'%(base,' '.join(re.sub('[^\u0621-\u063a\u0641-\u0652\u0670]','',w) for w in re.findall(r'[\u0600-\u06ff]+',text) if re.sub('[^\u0621-\u063a\u0641-\u0652\u0670]','',w))))
  kept+=1; totaldur+=dur
  if kept>=2100: break
 if kept>=2100: break
print('selected',kept,'hours',totaldur/3600)
