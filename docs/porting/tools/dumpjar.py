import zipfile,struct,json,sys
def parse(b):
    p=10; n=struct.unpack('>H',b[8:10])[0]; cp=[None]*n; i=1
    while i<n:
        t=b[p]
        if t==1:
            l=struct.unpack('>H',b[p+1:p+3])[0]; cp[i]=b[p+3:p+3+l].decode('utf-8','replace'); p+=3+l
        elif t in (3,4): p+=5
        elif t in (5,6): p+=9; i+=1
        elif t in (7,8,16,19,20): cp[i]=('ref',struct.unpack('>H',b[p+1:p+3])[0]); p+=3
        elif t in (9,10,11,12,17,18): p+=5
        elif t==15: p+=4
        else: raise Exception('tag %d'%t)
        i+=1
    def cls(ix): return cp[cp[ix][1]] if ix else None
    acc,this,sup=struct.unpack('>HHH',b[p:p+6]); p+=6
    ic=struct.unpack('>H',b[p:p+2])[0]; p+=2
    itf=[cls(struct.unpack('>H',b[p+2*k:p+2*k+2])[0]) for k in range(ic)]; p+=2*ic
    def members():
        nonlocal p
        c=struct.unpack('>H',b[p:p+2])[0]; p+=2; out=[]
        for _ in range(c):
            a,ni,di,ac=struct.unpack('>HHHH',b[p:p+8]); p+=8
            for _ in range(ac):
                l=struct.unpack('>I',b[p+2:p+6])[0]; p+=6+l
            out.append((cp[ni],cp[di],a))
        return out
    f=members(); m=members()
    return cls(this),{'acc':acc,'super':cls(sup),'itf':itf,'fields':{x[0]:[x[1],x[2]] for x in f},'methods':{x[0]+x[1]:x[2] for x in m}}
out={}
with zipfile.ZipFile(sys.argv[1]) as z:
    for e in z.namelist():
        if e.endswith('.class') and (e.startswith('net/minecraft/') or e.startswith('com/mojang/')):
            n,d=parse(z.read(e)); out[n]=d
json.dump(out,open(sys.argv[2],'w'))
print(sys.argv[2],len(out))
