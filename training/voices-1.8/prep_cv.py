import pyarrow.parquet as pq, subprocess, numpy as np, re, os, sys
from scipy.io import wavfile
sys.path.insert(0,'/workspace/cv-ar'); import rawi
import pyarrow as pa
t=pa.concat_tables([pq.read_table('/workspace/cv-ar/train%d.parquet'%k,columns=['client_id','audio','sentence','up_votes','down_votes']) for k in (0,1)])
ids=t.column('client_id').to_pylist(); aud=t.column('audio').to_pylist(); sen=t.column('sentence').to_pylist()
up=t.column('up_votes').to_pylist(); dn=t.column('down_votes').to_pylist()
def dec(b):
    p=subprocess.run(['ffmpeg','-v','quiet','-i','pipe:0','-f','s16le','-ac','1','-ar','16000','pipe:1'],input=b,capture_output=True)
    return np.frombuffer(p.stdout,dtype=np.int16).astype(np.float32)/32768
for tag,prefix in (('cvf','1ebda36c'),('cvm','3b941765')):
    out='/workspace/cv-ar/%s'%tag; os.makedirs(out+'/wav',exist_ok=True)
    lines=[];tot=0.
    for i,c in enumerate(ids):
        if not c.startswith(prefix) or up[i]<2 or up[i]<=dn[i]: continue
        s=sen[i]
        if re.search(r'[0-9A-Za-z\u0660-\u0669]',s): continue
        if re.search(r'[\u200b-\u200f\u202a-\u202e\ufeff]',s): continue
        s=re.sub('[\u0640]','',s)
        words=re.findall(r'[\u0621-\u064a\u0671]+',s)
        if len(words)<2 or sum(len(w)==1 for w in words)>1 or max(len(w) for w in words)>14: continue
        y=dec(aud[i]['bytes'])
        # trim leading/trailing quiet
        e=np.abs(y); thr=0.02*e.max() if e.max()>0 else 0
        nz=np.where(e>thr)[0]
        if len(nz)==0: continue
        y=y[max(0,nz[0]-1600):nz[-1]+3200]
        d=len(y)/16000
        if not (1.0<=d<=10) or e.max()<0.05: continue
        txt=rawi.diacritize(' '.join(words))
        txt=' '.join(re.sub('[^\u0621-\u063a\u0641-\u0652\u0670]','',w) for w in txt.split())
        base='%s_%04d'%(tag,len(lines))
        wavfile.write('%s/wav/%s.wav'%(out,base),16000,(np.clip(y,-1,1)*32767).astype(np.int16))
        lines.append('( %s "%s" )\n'%(base,txt)); tot+=d
        if len(lines)>=1500: break
    open(out+'/txt.done.unicode','w',encoding='utf8').writelines(lines)
    print(tag,len(lines),'hours %.2f'%(tot/3600),flush=True)
