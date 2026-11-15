import os
﻿a=open(os.environ.get("KERNEL_A", "kernel_a"), "rb").read()
b=open(os.environ.get("KERNEL_B", "kernel_b"), "rb").read()
print("sizes",len(a),len(b))
n=0; first=[]; last=[]
for i,(x,y) in enumerate(zip(a,b)):
    if x!=y:
        n+=1
        if len(first)<40: first.append(i)
        last.append(i)
print("diff bytes",n)
print("first",[hex(x) for x in first[:20]])
print("last",[hex(x) for x in last[-10:]])
# diff ranges
ranges=[]; start=None; prev=None
for i in last:
    if start is None: start=i
    elif i>prev+1:
        ranges.append((start,prev)); start=i
    prev=i
if start is not None: ranges.append((start,prev))
print("ranges",len(ranges))
for r in ranges[:10]:
    print(hex(r[0]),hex(r[1]),b[r[0]:r[1]+1][:80])
