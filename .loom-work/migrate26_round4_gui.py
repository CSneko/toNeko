#!/usr/bin/env python3
"""Round 4: GuiGraphics -> GuiGraphicsExtractor migration for 26.x GUI 管线.

映射规则（旧 -> 新）：
- 类型: net.minecraft.client.gui.GuiGraphics -> net.minecraft.client.gui.GuiGraphicsExtractor
- Screen 渲染挂钩: render(GuiGraphics g, ...) -> extractRenderState(GuiGraphicsExtractor g, ...)
- drawString(Font, ..., int y, int color)            -> text(Font, ...)
- drawCenteredString                                  -> centeredText
- hLine/vLine                                         -> horizontalLine/verticalLine（参数顺序不同，需人工核对）
- renderOutline                                       -> outline
- renderItem / renderItemDecorations                  -> item / itemDecorations
- blit(Identifier,...)                                -> blit(同名重载)
"""
import re, pathlib

FILES = [l.strip() for l in open("/tmp/ggfiles.txt") if l.strip()]

REPL = [
    # type & imports
    ("net.minecraft.client.gui.GuiGraphics;", "net.minecraft.client.gui.GuiGraphicsExtractor;"),
    (re.compile(r"\bGuiGraphics\b"), "GuiGraphicsExtractor"),
]

def transform(text):
    orig = text
    # import first (avoid double apply)
    if "import net.minecraft.client.gui.GuiGraphics;" in text:
        text = text.replace("import net.minecraft.client.gui.GuiGraphics;",
                            "import net.minecraft.client.gui.GuiGraphicsExtractor;")
    elif re.search(r"(?<![\w.])GuiGraphics(?![\w])", text):
        # referenced via wildcard/qualified name: add explicit import
        m = re.search(r"^package ([\w.]+);", text, re.M)
        pkg = m.group(1)
        if "net.minecraft.client.gui" not in text or "screens" in pkg:
            pass

    # raw identifier swaps
    text = re.sub(r"(?<![\w.])GuiGraphics(?![\w])", "GuiGraphicsExtractor", text)

    # method renames (conservative, keep args intact; ambiguous ones fixed by hand later)
    text = re.sub(r"\.drawString\(", ".text(", text)
    text = re.sub(r"\.drawCenteredString\(", ".centeredText(", text)
    text = re.sub(r"\.hLine\(", ".horizontalLine(", text)
    text = re.sub(r"\.vLine\(", ".verticalLine(", text)
    text = re.sub(r"\.renderOutline\(", ".outline(", text)
    text = re.sub(r"\.renderItemDecorations\(", ".itemDecorations(", text)

    return text, orig

changed = []
for f in FILES:
    p = pathlib.Path(f)
    t = p.read_text(encoding="utf-8")
    nt, _ = transform(t)
    if nt != t:
        p.write_text(nt, encoding="utf-8")
        changed.append(f)
print("updated:", len(changed))
for c in changed:
    print("  ", c)
