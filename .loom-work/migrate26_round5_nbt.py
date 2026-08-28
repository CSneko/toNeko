#!/usr/bin/env python3
"""Round 5: NBT API 适配（26.x）。

规则：
- compound.contains(key, TagType)        -> compound.contains(key)
- compound.getList(key, type)            -> compound.getListOrEmpty(key)
- compound.getCompound(key)              -> compound.getCompoundOrEmpty(key)
- compound.getDouble/Float/Int(key)      -> get{X}Or(key, 0/0f/0)
"""
import re, pathlib

ROOT = pathlib.Path("/media/crystalneko/enkoo/java/toNeko")
SRCS = ["common/src", "fabric/src", "neoforge/src"]

patt_contains = re.compile(r"\.contains\(([^,()]+),\s*(?:CompoundTag|ListTag|Tag)\.TAG_[A-Z_]+\)")
patt_list     = re.compile(r"\.getList\(([^,()]+),\s*(?:CompoundTag|ListTag|Tag)\.TAG_[A-Z_]+\)")

changed=[]
for base in SRCS:
    for p in list(ROOT.glob(base+"/**/*.java")):
        t=p.read_text(encoding="utf-8"); o=t
        t = patt_list.sub(r".getListOrEmpty(\1)", t)
        t = patt_contains.sub(r".contains(\1)", t)
        if t!=o:
            p.write_text(t,encoding="utf-8"); changed.append(str(p))
print(len(changed))
