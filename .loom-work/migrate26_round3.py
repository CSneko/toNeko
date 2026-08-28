#!/usr/bin/env python3
"""Round 3: InteractionResultHolder -> InteractionResult migration."""
import re, pathlib

ROOT = pathlib.Path("/media/crystalneko/enkoo/java/toNeko")
SRCS = ["common/src", "fabric/src", "neoforge/src"]

balanced = r"(?:[^()]|\([^()]*\))*"

RULES = [
    # method signatures
    (re.compile(r"InteractionResultHolder<ItemStack>\s+use\("), "InteractionResult use("),
    (re.compile(r"InteractionResultHolder<Item>\s+use\("), "InteractionResult use("),
    (re.compile(r"InteractionResultHolder<Unit>\s+use\("), "InteractionResult use("),
    # return expressions
    (re.compile(r"InteractionResultHolder\.success\(" + balanced + r"\)"), "InteractionResult.SUCCESS"),
    (re.compile(r"InteractionResultHolder\.fail\(" + balanced + r"\)"), "InteractionResult.FAIL"),
    (re.compile(r"InteractionResultHolder\.pass\(" + balanced + r"\)"), "InteractionResult.PASS"),
    (re.compile(r"InteractionResultHolder\.consume\((" + balanced + r")\)"), r"InteractionResult.CONSUME /* stack consumed manually below */"),
    (re.compile(r"InteractionResultHolder\.sidedSuccess\((?:[^()]|\([^()]*\))*level\.isClientSide\)"), "(level.isClientSide ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER)"),
    # remove stale import
    (re.compile(r"^import net\.minecraft\.world\.InteractionResultHolder;\n", re.M), ""),
]

changed = []
for base in SRCS:
    for p in (ROOT / base).rglob("*.java"):
        text = p.read_text(encoding="utf-8")
        orig = text
        for rx, rep in RULES:
            text = rx.sub(rep, text)
        if text != orig:
            p.write_text(text, encoding="utf-8")
            changed.append(str(p))

print(f"updated {len(changed)} files")
for c in changed:
    print("  ", c)
