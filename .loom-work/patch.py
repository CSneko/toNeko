import pathlib
loom_src = pathlib.Path(".loom-work/src/net/fabricmc/loom/configuration/mods/AccessWidenerUtils.java")
text = loom_src.read_text()
old = """\tpublic static byte[] remapAccessWidener(byte[] input, Remapper remapper) {
\t\tint version = AccessWidenerReader.readVersion(input);

\t\tAccessWidenerWriter writer = new AccessWidenerWriter(version);
\t\tAccessWidenerRemapper awRemapper = new AccessWidenerRemapper(
\t\t\t\twriter,
\t\t\t\tremapper,
\t\t\t\tMappingsNamespace.INTERMEDIARY.toString(),
\t\t\t\tMappingsNamespace.NAMED.toString()
\t\t);
\t\tAccessWidenerReader reader = new AccessWidenerReader(awRemapper);
\t\treader.read(input);
\t\treturn writer.write();
\t}"""
new = """\tpublic static byte[] remapAccessWidener(byte[] input, Remapper remapper) {
\t\ttry {
\t\t\tint version = AccessWidenerReader.readVersion(input);
\t\t\tAccessWidenerWriter writer = new AccessWidenerWriter(version);
\t\t\tAccessWidenerRemapper awRemapper = new AccessWidenerRemapper(
\t\t\t\t\twriter,
\t\t\t\t\tremapper,
\t\t\t\t\tMappingsNamespace.INTERMEDIARY.toString(),
\t\t\t\t\tMappingsNamespace.NAMED.toString()
\t\t\t);
\t\t\tAccessWidenerReader reader = new AccessWidenerReader(awRemapper);
\t\t\treader.read(input);
\t\t\treturn writer.write();
\t\t} catch (Exception e) {
\t\t\tString header = new String(input, java.nio.charset.StandardCharsets.UTF_8);
\t\t\tString firstLine = header.contains("\\n") ? header.substring(0, header.indexOf("\\n")) : header;
\t\t\tif (firstLine.startsWith("classTweaker")) {
\t\t\t\treturn input;
\t\t\t}
\t\t\tthrow e;
\t\t}
\t}"""
if old in text:
    text = text.replace(old, new)
    if "java.nio.charset.StandardCharsets" not in text:
        text = text.replace("import org.objectweb.asm.commons.Remapper;", "import java.nio.charset.StandardCharsets;\nimport org.objectweb.asm.commons.Remapper;")
    loom_src.write_text(text)
    print("patched remap")
else:
    print("OLD not found")

text2 = loom_src.read_text()
old2 = """\t\tfinal String accessWidenerPath = classTweakers.get(0);
\t\tfinal byte[] accessWidener = fabricModJson.getSource().read(accessWidenerPath);
\t\tfinal AccessWidenerReader.Header header = AccessWidenerReader.readHeader(accessWidener);

\t\treturn new AccessWidenerData(accessWidenerPath, header, accessWidener);"""
new2 = """\t\tfinal String accessWidenerPath = classTweakers.get(0);
\t\tfinal byte[] accessWidener = fabricModJson.getSource().read(accessWidenerPath);
\t\tfinal AccessWidenerReader.Header header;
\t\ttry {
\t\t\theader = AccessWidenerReader.readHeader(accessWidener);
\t\t} catch (Exception e) {
\t\t\tString headerStr2 = new String(accessWidener, StandardCharsets.UTF_8);
\t\t\tString firstLine2 = headerStr2.contains("\\n") ? headerStr2.substring(0, headerStr2.indexOf("\\n")) : headerStr2;
\t\t\tif (firstLine2.startsWith("classTweaker")) {
\t\t\t\tString[] parts = firstLine2.split("\\\\s+");
\t\t\t\tString ns = parts.length >= 3 ? parts[2] : MappingsNamespace.INTERMEDIARY.toString();
\t\t\t\theader = new AccessWidenerReader.Header(1, ns);
\t\t\t} else {
\t\t\t\tthrow e;
\t\t\t}
\t\t}

\t\treturn new AccessWidenerData(accessWidenerPath, header, accessWidener);"""
if old2 in text2:
    text2 = text2.replace(old2, new2)
    loom_src.write_text(text2)
    print("patched readAccessWidenerData")
else:
    print("OLD2 not found")

prov = pathlib.Path(".loom-work/src/net/fabricmc/loom/configuration/mods/AccessWidenerAnalyzeVisitorProvider.java")
t3 = prov.read_text()
old3 = """\tstatic AccessWidenerAnalyzeVisitorProvider createFromMods(String namespace, List<ModDependency> mods, ModPlatform platform) throws IOException {
\t\tAccessWidener accessWidener = new AccessWidener();
\t\taccessWidener.visitHeader(namespace);

\t\tfor (ModDependency mod : mods) {
\t\t\tfinal var accessWidenerData = AccessWidenerUtils.readAccessWidenerData(mod.getInputFile(), platform);

\t\t\tif (accessWidenerData == null) {
\t\t\t\tcontinue;
\t\t\t}

\t\t\tfinal var reader = new AccessWidenerReader(accessWidener);
\t\t\treader.read(accessWidenerData.content());
\t\t}

\t\treturn new AccessWidenerAnalyzeVisitorProvider(accessWidener);
\t}"""
new3 = """\tstatic AccessWidenerAnalyzeVisitorProvider createFromMods(String namespace, List<ModDependency> mods, ModPlatform platform) throws IOException {
\t\tAccessWidener accessWidener = new AccessWidener();
\t\taccessWidener.visitHeader(namespace);

\t\tfor (ModDependency mod : mods) {
\t\t\tfinal var accessWidenerData = AccessWidenerUtils.readAccessWidenerData(mod.getInputFile(), platform);

\t\t\tif (accessWidenerData == null) {
\t\t\t\tcontinue;
\t\t\t}

\t\t\tString headerPreview = new String(accessWidenerData.content(), java.nio.charset.StandardCharsets.UTF_8);
\t\t\tString first = headerPreview.contains("\\n") ? headerPreview.substring(0, headerPreview.indexOf("\\n")) : headerPreview;
\t\t\tif (first.startsWith("classTweaker")) {
\t\t\t\tcontinue;
\t\t\t}
\t\t\tfinal var reader = new AccessWidenerReader(accessWidener);
\t\t\treader.read(accessWidenerData.content());
\t\t}

\t\treturn new AccessWidenerAnalyzeVisitorProvider(accessWidener);
\t}"""
if old3 in t3:
    t3 = t3.replace(old3, new3)
    if "java.nio.charset.StandardCharsets" not in t3:
        t3 = t3.replace("import net.fabricmc.accesswidener.AccessWidener;", "import java.nio.charset.StandardCharsets;\nimport net.fabricmc.accesswidener.AccessWidener;")
    prov.write_text(t3)
    print("patched AnalyzeVisitor")
else:
    print("OLD3 not found")
