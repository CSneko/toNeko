import pathlib
p = pathlib.Path(".loom-work/src/net/fabricmc/loom/configuration/mods/AccessWidenerUtils.java")
t = p.read_text()
# Fix Header construction: use reflection or bypass reading header for CT – return null Data and skip later?
# Instead, make readAccessWidenerData return null for classTweaker so caller skips it entirely (since CT is not AW, it's processed elsewhere as classTweaker)
old = """\t\tfinal String accessWidenerPath = classTweakers.get(0);
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
new = """\t\tfinal String accessWidenerPath = classTweakers.get(0);
\t\tfinal byte[] accessWidener = fabricModJson.getSource().read(accessWidenerPath);
\t\t// 26.2: some fabric mods (e.g. fabric-content-registries) declare classTweaker files (header "classTweaker v1 official")
\t\t// as their "accessWidener" entry. Old AW reader (2.1.0) throws on that header. Skip CT entries here – they are
\t\t// class tweakers, not access wideners, and for unobfuscated 26.2 they need no remapping anyway.
\t\tString headerPreviewCT = new String(accessWidener, StandardCharsets.UTF_8);
\t\tString firstLineCT = headerPreviewCT.contains("\\n") ? headerPreviewCT.substring(0, headerPreviewCT.indexOf("\\n")) : headerPreviewCT;
\t\tif (firstLineCT.startsWith("classTweaker")) {
\t\t\treturn null;
\t\t}
\t\tfinal AccessWidenerReader.Header header = AccessWidenerReader.readHeader(accessWidener);

\t\treturn new AccessWidenerData(accessWidenerPath, header, accessWidener);"""
if old in t:
    t = t.replace(old, new)
    p.write_text(t)
    print("fixed utils to skip CT")
else:
    print("not found")
    print(t[2000:4000])
