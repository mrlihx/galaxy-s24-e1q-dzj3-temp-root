import os
﻿from elftools.elf.elffile import ELFFile
from capstone import Cs, CS_ARCH_ARM64, CS_MODE_ARM
import struct
BASE = 0xffffffc008000000
p = os.environ.get("VMLINUX", "vmlinux.elf")
img = open(os.environ.get("KERNEL", "kernel"), "rb").read()
syms={}
with open(p,"rb") as f:
    e=ELFFile(f); st=e.get_section_by_name(".symtab")
    for s in st.iter_symbols():
        if s.name: syms.setdefault(s.name,[]).append(s["st_value"])
name2addr={}
for n,vs in syms.items():
    for v in vs: name2addr.setdefault(v,n)
def off(n):
    v=syms.get(n)
    return (v[0]-BASE) if v else None
keys=["init_cred","kdp_enable","init_task","init_user_ns","root_user","init_groups","init_ucounts",
      "selinux_state","posix_timers_cache","cred_jar","system_unbound_wq","call_usermodehelper_exec_work",
      "anon_pipe_buf_ops","creds_fast_hash","panic_on_warn"]
print("=== key static symbols: EZIF(6.1.162) vs DZJ3(6.1.145) ===")
ezif={"init_cred":0x17c4018,"kdp_enable":0x17c5f18,"init_task":0x226f6c0,"init_user_ns":0x2280830,
      "root_user":0x2280ac0,"init_groups":0x2281de0,"init_ucounts":0x2282298,"selinux_state":0x2542618,
      "posix_timers_cache":0x24934a0,"cred_jar":0x246bc20}
for k in keys:
    o=off(k)
    print(f"{k:34s} DZJ3={('0x%08x'%o) if o is not None else 'NOT FOUND':>12s}  EZIF={('0x%08x'%ezif[k]) if k in ezif else '-'}")

def find_call(func, callees):
    fo = off(func)
    if fo is None: return None
    md=Cs(CS_ARCH_ARM64,CS_MODE_ARM)
    for ins in md.disasm(img[fo:fo+0x400], BASE+fo):
        if ins.mnemonic=="bl":
            raw=img[ins.address-BASE:ins.address-BASE+4]; w=struct.unpack("<I",raw)[0]
            imm=w&0x03FFFFFF
            if imm&0x02000000: imm-=0x04000000
            tgt=ins.address+imm*4
            n=name2addr.get(tgt)
            if n in callees:
                return (ins.address-BASE+4, n)
    return None
print("\n=== call sites ===")
for func,callees in [("do_timer_create",{"kmem_cache_alloc","kmem_cache_alloc_trace","kmem_cache_alloc_node"}),
                     ("prepare_creds",{"kmem_cache_alloc","kmem_cache_alloc_trace","kmem_cache_alloc_node"}),
                     ("dup_task_struct",{"kmem_cache_alloc","kmem_cache_alloc_trace","kmem_cache_alloc_node"}),
                     ("copy_process",{"kmem_cache_alloc","kmem_cache_alloc_trace","kmem_cache_alloc_node"})]:
    print(func, "->", find_call(func,callees))
