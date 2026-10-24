import sys,json,re,subprocess,os,numpy as np,pyarrow.parquet as pq
from scipy.io import wavfile
from rf import RangeFile
shard,tag,want=sys.argv[1],sys.argv[2],int(sys.argv[3])
fs=dict((p.split('-')[1],(p,s)) for p,s in json.load(open('shards.json')))
p,sz=fs[shard]
pf=pq.ParquetFile(RangeFile('https://huggingface.co/datasets/MikhailT/hifi-tts/resolve/main/'+p,sz))
out='/workspace/hifi/'+tag; os.makedirs(out+'/wav',exist_ok=True)
lines=[];dur=0.
order=list(range(0,pf.num_row_groups,1))
import random; random.Random(7).shuffle(order)
for g in order:
    t=pf.read_row_group(g,columns=['audio','duration','text_normalized']).to_pydict()
    for a,d,tx in zip(t['audio'],t['duration'],t['text_normalized']):
        if not (2.5<=d<=9.5): continue
        tx=re.sub(r"[^a-z' ,.;:!?-]",'',tx.lower().replace('"','')); tx=re.sub(r'\s+',' ',tx).strip()
        if len(tx.split())<3: continue
        pr=subprocess.run(['ffmpeg','-v','quiet','-i','pipe:0','-f','s16le','-ac','1','-ar','16000','pipe:1'],input=a['bytes'],capture_output=True)
        y=np.frombuffer(pr.stdout,dtype=np.int16)
        if len(y)<16000*2: continue
        base='%s_%04d'%(tag,len(lines))
        wavfile.write('%s/wav/%s.wav'%(out,base),16000,y)
        lines.append('( %s "%s" )\n'%(base,tx)); dur+=len(y)/16000
        if len(lines)>=want: break
    print(g,len(lines),'%.2fh'%(dur/3600),flush=True)
    if len(lines)>=want: break
open(out+'/txt.done.data','w').writelines(lines)
print('DONE',len(lines),dur/3600)
