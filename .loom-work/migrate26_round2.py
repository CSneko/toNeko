#!/usr/bin/env python3
"""Round 2 mechanical renames for toNeko -> Minecraft 26.x migration."""
import re, pathlib

ROOT = pathlib.Path("/media/crystalneko/enkoo/java/toNeko")
SRCS = ["common/src", "fabric/src", "neoforge/src"]

IMPORT_MAP = [
    ("import net.minecraft.world.entity.monster.Zombie;",   "import net.minecraft.world.entity.monster.zombie.Zombie;"),
    ("import net.minecraft.world.entity.monster.Spider;",   "import net.minecraft.world.entity.monster.spider.Spider;"),
    ("import net.minecraft.world.entity.monster.CaveSpider;","import net.minecraft.world.entity.monster.spider.CaveSpider;"),
    ("import net.minecraft.world.entity.animal.Cat;",       "import net.minecraft.world.entity.animal.feline.Cat;"),
    ("import net.minecraft.world.entity.animal.Ocelot;",    "import net.minecraft.world.entity.animal.feline.Ocelot;"),
    ("import net.minecraft.world.entity.animal.IronGolem;", "import net.minecraft.world.entity.animal.golem.IronGolem;"),
    ("import net.minecraft.world.entity.npc.Villager;",     "import net.minecraft.world.entity.npc.villager.Villager;"),
    ("import net.minecraft.advancements.CriteriaTriggers;", "import net.minecraft.advancements.triggers.CriteriaTriggers;"),
    # ItemNameBlockItem removed: everything is BlockItem now
    ("net.minecraft.world.item.ItemNameBlockItem",          "net.minecraft.world.item.BlockItem"),
]

TOKEN_RE = [
    (re.compile(r"\bItemNameBlockItem\b"), "BlockItem"),
]

changed = []
for base in SRCS:
    for p in (ROOT / base).rglob("*.java"):
        text = p.read_text(encoding="utf-8")
        orig = text
        for a, b in IMPORT_MAP:
            text = text.replace(a, b)
        for rx, rep in TOKEN_RE:
            text = rx.sub(rep, text)
        if text != orig:
            p.write_text(text, encoding="utf-8")
            changed.append(str(p))

print(f"updated {len(changed)} files")
for c in changed[:20]:
    print("  ", c)
