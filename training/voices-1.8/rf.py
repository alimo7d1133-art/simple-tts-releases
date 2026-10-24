import urllib.request,io
class RangeFile(io.RawIOBase):
    def __init__(s,url,size): s.url=url; s.size=size; s.pos=0
    def seekable(s): return True
    def readable(s): return True
    def tell(s): return s.pos
    def seek(s,o,w=0):
        s.pos = o if w==0 else (s.pos+o if w==1 else s.size+o); return s.pos
    def read(s,n=-1):
        if n<0 or s.pos+n>s.size: n=s.size-s.pos
        if n<=0: return b''
        for k in range(5):
            try:
                r=urllib.request.Request(s.url,headers={'Range':'bytes=%d-%d'%(s.pos,s.pos+n-1)})
                b=urllib.request.urlopen(r,timeout=120).read(); break
            except Exception as e: err=e; import time; time.sleep(3)
        else: raise err
        s.pos+=len(b); return b
    def readinto(s,b):
        d=s.read(len(b)); b[:len(d)]=d; return len(d)
