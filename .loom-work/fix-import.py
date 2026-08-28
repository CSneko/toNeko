import pathlib
p = pathlib.Path(".loom-work/src/net/fabricmc/loom/configuration/mods/AccessWidenerUtils.java")
t = p.read_text()
if "import java.nio.charset.StandardCharsets;" not in t:
    t = t.replace("import java.io.IOException;", "import java.io.IOException;\nimport java.nio.charset.StandardCharsets;")
    p.write_text(t)
    print("added import")
else:
    print("already")
