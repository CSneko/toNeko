/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2020-2021 FabricMC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.fabricmc.loom.util.srg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.apache.commons.io.IOUtils;
import org.cadixdev.lorenz.MappingSet;
import org.cadixdev.lorenz.io.srg.tsrg.TSrgReader;
import org.cadixdev.lorenz.model.ClassMapping;
import org.cadixdev.lorenz.model.FieldMapping;
import org.cadixdev.lorenz.model.InnerClassMapping;
import org.cadixdev.lorenz.model.MethodMapping;
import org.cadixdev.lorenz.model.TopLevelClassMapping;
import org.jetbrains.annotations.Nullable;

import net.fabricmc.loom.util.FileSystemUtil;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.stitch.commands.tinyv2.TinyClass;
import net.fabricmc.stitch.commands.tinyv2.TinyField;
import net.fabricmc.stitch.commands.tinyv2.TinyFile;
import net.fabricmc.stitch.commands.tinyv2.TinyMethod;
import net.fabricmc.stitch.commands.tinyv2.TinyMethodParameter;
import net.fabricmc.stitch.commands.tinyv2.TinyV2Reader;

public class MCPReader {
	private final Path intermediaryTinyPath;
	private final Path srgTsrgPath;

	public MCPReader(Path intermediaryTinyPath, Path srgTsrgPath) {
		this.intermediaryTinyPath = intermediaryTinyPath;
		this.srgTsrgPath = srgTsrgPath;
	}

	public TinyFile read(Path mcpJar) throws IOException {
		Map<MemberToken, String> srgTokens = readSrg();
		TinyFile intermediaryTiny = TinyV2Reader.read(intermediaryTinyPath);
		Map<String, String> intermediaryToMCPMap = createIntermediaryToMCPMap(intermediaryTiny, srgTokens);
		Map<String, String[]> intermediaryToDocsMap = new HashMap<>();
		Map<String, Map<Integer, String>> intermediaryToParamsMap = new HashMap<>();

		try {
			injectMcp(mcpJar, intermediaryToMCPMap, intermediaryToDocsMap, intermediaryToParamsMap);
		} catch (CsvValidationException e) {
			throw new RuntimeException(e);
		}

		mergeTokensIntoIntermediary(intermediaryTiny, intermediaryToMCPMap, intermediaryToDocsMap, intermediaryToParamsMap);
		return intermediaryTiny;
	}

	private Map<String, String> createIntermediaryToMCPMap(TinyFile tiny, Map<MemberToken, String> officialToMCP) {
		Map<String, String> map = new HashMap<>();

		for (TinyClass tinyClass : tiny.getClassEntries()) {
			String classObf = tinyClass.getMapping().get(0);
			String classIntermediary = tinyClass.getMapping().get(1);
			MemberToken classTokenObf = MemberToken.ofClass(classObf);

			if (officialToMCP.containsKey(classTokenObf)) {
				map.put(classIntermediary, officialToMCP.get(classTokenObf));
			}

			for (TinyField tinyField : tinyClass.getFields()) {
				String fieldObf = tinyField.getMapping().get(0);
				String fieldIntermediary = tinyField.getMapping().get(1);
				MemberToken fieldTokenObf = MemberToken.ofField(classTokenObf, fieldObf);

				if (officialToMCP.containsKey(fieldTokenObf)) {
					map.put(fieldIntermediary, officialToMCP.get(fieldTokenObf));
				}
			}

			for (TinyMethod tinyMethod : tinyClass.getMethods()) {
				String methodObf = tinyMethod.getMapping().get(0);
				String methodIntermediary = tinyMethod.getMapping().get(1);
				MemberToken methodTokenObf = MemberToken.ofMethod(classTokenObf, methodObf, tinyMethod.getMethodDescriptorInFirstNamespace());

				if (officialToMCP.containsKey(methodTokenObf)) {
					map.put(methodIntermediary, officialToMCP.get(methodTokenObf));
				}
			}
		}

		return map;
	}

	private void mergeTokensIntoIntermediary(TinyFile tiny, Map<String, String> intermediaryToMCPMap, Map<String, String[]> intermediaryToDocsMap, Map<String, Map<Integer, String>> intermediaryToParamsMap) {
		stripTinyWithParametersAndLocal(tiny);

		// We will be adding the "named" namespace with MCP
		tiny.getHeader().getNamespaces().add("named");

		for (TinyClass tinyClass : tiny.getClassEntries()) {
			String classIntermediary = tinyClass.getMapping().get(1);
			tinyClass.getMapping().add(intermediaryToMCPMap.getOrDefault(classIntermediary, classIntermediary));

			for (TinyField tinyField : tinyClass.getFields()) {
				String fieldIntermediary = tinyField.getMapping().get(1);
				String[] docs = intermediaryToDocsMap.get(fieldIntermediary);
				tinyField.getMapping().add(intermediaryToMCPMap.getOrDefault(fieldIntermediary, fieldIntermediary));

				if (docs != null) {
					tinyField.getComments().clear();
					tinyField.getComments().addAll(Arrays.asList(docs));
				}
			}

			for (TinyMethod tinyMethod : tinyClass.getMethods()) {
				String methodIntermediary = tinyMethod.getMapping().get(1);
				String[] docs = intermediaryToDocsMap.get(methodIntermediary);
				tinyMethod.getMapping().add(intermediaryToMCPMap.getOrDefault(methodIntermediary, methodIntermediary));

				if (docs != null) {
					tinyMethod.getComments().clear();
					tinyMethod.getComments().addAll(Arrays.asList(docs));
				}

				Map<Integer, String> params = intermediaryToParamsMap.get(methodIntermediary);

				if (params != null) {
					for (Map.Entry<Integer, String> entry : params.entrySet()) {
						int lvIndex = entry.getKey();
						String paramName = entry.getValue();

						ArrayList<String> mappings = new ArrayList<>();
						mappings.add("");
						mappings.add("");
						mappings.add(paramName);
						tinyMethod.getParameters().add(new TinyMethodParameter(lvIndex, mappings, new ArrayList<>()));
					}
				}
			}
		}
	}

	private void stripTinyWithParametersAndLocal(TinyFile tiny) {
		for (TinyClass tinyClass : tiny.getClassEntries()) {
			for (TinyMethod tinyMethod : tinyClass.getMethods()) {
				tinyMethod.getParameters().clear();
				tinyMethod.getLocalVariables().clear();
			}
		}
	}

	private Map<MemberToken, String> readSrg() throws IOException {
		Map<MemberToken, String> tokens = new HashMap<>();

		try (BufferedReader reader = Files.newBufferedReader(srgTsrgPath, StandardCharsets.UTF_8)) {
			String content = IOUtils.toString(reader);

			if (content.startsWith("tsrg2")) {
				readTsrg2(tokens, content);
			} else {
				MappingSet mappingSet = new TSrgReader(new StringReader(content)).read();

				for (TopLevelClassMapping classMapping : mappingSet.getTopLevelClassMappings()) {
					appendClass(tokens, classMapping);
				}
			}
		}

		return tokens;
	}

	private void readTsrg2(Map<MemberToken, String> tokens, String content) throws IOException {
		MemoryMappingTree tree = new MemoryMappingTree();
		MappingReader.read(new StringReader(content), tree);
		int obfIndex = tree.getNamespaceId("obf");
		int srgIndex = tree.getNamespaceId("srg");

		for (MappingTree.ClassMapping classDef : tree.getClasses()) {
			MemberToken ofClass = MemberToken.ofClass(classDef.getName(obfIndex));
			tokens.put(ofClass, classDef.getName(srgIndex));

			for (MappingTree.FieldMapping fieldDef : classDef.getFields()) {
				tokens.put(MemberToken.ofField(ofClass, fieldDef.getName(obfIndex)), fieldDef.getName(srgIndex));
			}

			for (MappingTree.MethodMapping methodDef : classDef.getMethods()) {
				tokens.put(MemberToken.ofMethod(ofClass, methodDef.getName(obfIndex), methodDef.getDesc(obfIndex)),
						methodDef.getName(srgIndex));
			}
		}
	}

	private void injectMcp(Path mcpJar, Map<String, String> intermediaryToSrgMap, Map<String, String[]> intermediaryToDocsMap, Map<String, Map<Integer, String>> intermediaryToParamsMap)
			throws IOException, CsvValidationException {
		Map<String, List<String>> srgToIntermediary = inverseMap(intermediaryToSrgMap);
		Map<String, List<String>> simpleSrgToIntermediary = new HashMap<>();
		Pattern methodPattern = Pattern.compile("(func_\\d*)_.*");

		for (Map.Entry<String, List<String>> entry : srgToIntermediary.entrySet()) {
			Matcher matcher = methodPattern.matcher(entry.getKey());

			if (matcher.matches()) {
				simpleSrgToIntermediary.put(matcher.group(1), entry.getValue());
			}
		}

		try (FileSystemUtil.Delegate fs = FileSystemUtil.getJarFileSystem(mcpJar, false)) {
			Path fields = fs.getPath("fields.csv");
			Path methods = fs.getPath("methods.csv");
			Path params = fs.getPath("params.csv");
			Pattern paramsPattern = Pattern.compile("p_[^\\d]*(\\d+)_(\\d)+_?");

			try (CSVReader reader = new CSVReader(Files.newBufferedReader(fields, StandardCharsets.UTF_8))) {
				reader.readNext();
				String[] line;

				while ((line = reader.readNext()) != null) {
					List<String> intermediaryField = srgToIntermediary.get(line[0]);
					String[] docs = line[3].split("\n");

					if (intermediaryField != null) {
						for (String s : intermediaryField) {
							intermediaryToSrgMap.put(s, line[1]);

							if (!line[3].trim().isEmpty() && docs.length > 0) {
								intermediaryToDocsMap.put(s, docs);
							}
						}
					}
				}
			}

			try (CSVReader reader = new CSVReader(Files.newBufferedReader(methods, StandardCharsets.UTF_8))) {
				reader.readNext();
				String[] line;

				while ((line = reader.readNext()) != null) {
					List<String> intermediaryMethod = srgToIntermediary.get(line[0]);
					String[] docs = line[3].split("\n");

					if (intermediaryMethod != null) {
						for (String s : intermediaryMethod) {
							intermediaryToSrgMap.put(s, line[1]);

							if (!line[3].trim().isEmpty() && docs.length > 0) {
								intermediaryToDocsMap.put(s, docs);
							}
						}
					}
				}
			}

			if (Files.exists(params)) {
				try (CSVReader reader = new CSVReader(Files.newBufferedReader(params, StandardCharsets.UTF_8))) {
					reader.readNext();
					String[] line;

					while ((line = reader.readNext()) != null) {
						Matcher param = paramsPattern.matcher(line[0]);

						if (param.matches()) {
							String named = line[1];
							String srgMethodStartWith = "func_" + param.group(1);
							int lvIndex = Integer.parseInt(param.group(2));
							List<String> intermediaryMethod = simpleSrgToIntermediary.get(srgMethodStartWith);

							if (intermediaryMethod != null) {
								for (String s : intermediaryMethod) {
									intermediaryToParamsMap.computeIfAbsent(s, s1 -> new HashMap<>()).put(lvIndex, named);
								}
							}
						}
					}
				}
			}
		}
	}

	private Map<String, List<String>> inverseMap(Map<String, String> intermediaryToMCPMap) {
		Map<String, List<String>> map = new HashMap<>();

		for (Map.Entry<String, String> token : intermediaryToMCPMap.entrySet()) {
			map.computeIfAbsent(token.getValue(), s -> new ArrayList<>()).add(token.getKey());
		}

		return map;
	}

	private void appendClass(Map<MemberToken, String> tokens, ClassMapping<?, ?> classMapping) {
		MemberToken ofClass = MemberToken.ofClass(classMapping.getFullObfuscatedName());
		tokens.put(ofClass, classMapping.getFullDeobfuscatedName());

		for (FieldMapping fieldMapping : classMapping.getFieldMappings()) {
			tokens.put(MemberToken.ofField(ofClass, fieldMapping.getObfuscatedName()), fieldMapping.getDeobfuscatedName());
		}

		for (MethodMapping methodMapping : classMapping.getMethodMappings()) {
			tokens.put(MemberToken.ofMethod(ofClass, methodMapping.getObfuscatedName(), methodMapping.getObfuscatedDescriptor()), methodMapping.getDeobfuscatedName());
		}

		for (InnerClassMapping mapping : classMapping.getInnerClassMappings()) {
			appendClass(tokens, mapping);
		}
	}

	private record MemberToken(
			TokenType type,
			@Nullable MCPReader.MemberToken owner,
			String name,
			@Nullable String descriptor
	) {
		static MemberToken ofClass(String name) {
			return new MemberToken(TokenType.CLASS, null, name, null);
		}

		static MemberToken ofField(MemberToken owner, String name) {
			return new MemberToken(TokenType.FIELD, owner, name, null);
		}

		static MemberToken ofMethod(MemberToken owner, String name, String descriptor) {
			return new MemberToken(TokenType.METHOD, owner, name, descriptor);
		}
	}

	private enum TokenType {
		CLASS,
		METHOD,
		FIELD
	}
}
