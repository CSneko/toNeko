#!/usr/bin/env python3
"""Mechanical renames for toNeko -> Minecraft 26.x migration."""
import re, pathlib, sys

ROOT = pathlib.Path("/media/crystalneko/enkoo/java/toNeko")
SRCS = ["common/src", "fabric/src", "neoforge/src"]

# full-string import/package path replacements
IMPORT_MAP = [
    ("software.bernie.geckolib.cache.object.BakedGeoModel", "com.geckolib.cache.model.BakedGeoModel"),
    ("software.bernie.geckolib.cache.object.GeoBone",       "com.geckolib.cache.model.GeoBone"),
    ("software.bernie.geckolib.animation.AnimatableManager","com.geckolib.animatable.manager.AnimatableManager"),
    ("software.bernie.geckolib.animation.PlayState",        "com.geckolib.animation.object.PlayState"),
    ("software.bernie.geckolib.renderer.GeoRenderer",       "com.geckolib.renderer.base.GeoRenderer"),
    ("software.bernie.geckolib.renderer.layer.ItemArmorGeoLayer", "com.geckolib.renderer.layer.builtin.ItemArmorGeoLayer"),
    ("software.bernie.geckolib.animatable.instance",        "com.geckolib.animatable.instance"),
    ("software.bernie.geckolib.util.GeckoLibUtil",          "com.geckolib.util.GeckoLibUtil"),
    ("software.bernie.geckolib.util.Color",                 "com.geckolib.util.__COLOR__"),  # marker, fixed below
    ("software.bernie.geckolib.animation.RawAnimation",     "com.geckolib.animation.RawAnimation"),
    ("software.bernie.geckolib.animation.AnimationController","com.geckolib.animation.AnimationController"),
    ("net.minecraft.world.entity.vehicle.AbstractMinecart", "net.minecraft.world.entity.vehicle.minecart.AbstractMinecart"),
    ("net.minecraft.world.entity.vehicle.Minecart",         "net.minecraft.world.entity.vehicle.minecart.Minecart"),
    ("net.minecraft.world.level.GameRules",                 "net.minecraft.world.level.gamerules.GameRules"),
]

TOKEN_RE = [
    # remaining generic package rename
    (re.compile(r"\bsoftware\.bernie\.geckolib\b"), "com.geckolib"),
    # ResourceLocation -> Identifier but NOT our own ResourceLocationUtil
    (re.compile(r"\bResourceLocation\b(?!Util)"), "Identifier"),
    (re.compile(r"\bMobSpawnType\b"), "EntitySpawnReason"),
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
