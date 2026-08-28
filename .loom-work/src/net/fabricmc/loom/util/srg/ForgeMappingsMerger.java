/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2020-2023 FabricMC
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.jetbrains.annotations.Nullable;

import net.fabricmc.loom.api.mappings.layered.MappingContext;
import net.fabricmc.loom.api.mappings.layered.MappingsNamespace;
import net.fabricmc.loom.configuration.providers.forge.SrgProvider;
import net.fabricmc.loom.util.MappingException;
import net.fabricmc.loom.util.function.CollectionUtil;
import net.fabricmc.mappingio.FlatMappingVisitor;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.adapter.ForwardingMappingVisitor;
import net.fabricmc.mappingio.adapter.MappingNsRenamer;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.adapter.RegularAsFlatMappingVisitor;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.format.srg.TsrgFileReader;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MappingTreeView;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

/**
 * Merges a Tiny file with a new namespace.
 *
 * @author Juuz
 */
public final class ForgeMappingsMerger {
	private static final List<String> INPUT_NAMESPACES = List.of("official", "intermediary", "named");
	private static final List<String> INPUT_NAMESPACES_WITH_MOJANG = List.of("official", "mojang", "intermediary", "named");
	private final MemoryMappingTree newNs;
	private final MemoryMappingTree src;
	private final MemoryMappingTree output;
	private final FlatMappingVisitor flatOutput;
	private final boolean lenient;
	private final @Nullable MemoryMappingTree extra;
	private final ListMultimap<MethodKey, MethodData> methodsByNewNs;

	private ForgeMappingsMerger(MemoryMappingTree newNs, MemoryMappingTree src, @Nullable ExtraMappings extraMappings, boolean lenient) throws IOException {
		this.newNs = newNs;
		Preconditions.checkArgument(this.newNs.getDstNamespaces().size() == 1, "New namespace must have exactly one destination namespace");
		this.src = src;
		this.output = new MemoryMappingTree();
		this.flatOutput = new RegularAsFlatMappingVisitor(output);
		this.lenient = lenient;
		this.methodsByNewNs = ArrayListMultimap.create();

		if (extraMappings != null) {
			this.extra = new MemoryMappingTree();
			MappingVisitor visitor = new MappingSourceNsSwitch(this.extra, MappingsNamespace.OFFICIAL.toString());

			if (!extraMappings.hasCorrectNamespaces()) {
				Map<String, String> namespaces = Map.of(
						extraMappings.obfuscatedNamespace(), MappingsNamespace.OFFICIAL.toString(),
						extraMappings.deobfuscatedNamespace(), MappingsNamespace.NAMED.toString()
				);
				visitor = new MappingNsRenamer(visitor, namespaces);
			}

			MappingReader.read(extraMappings.path(), extraMappings.format(), visitor);
		} else {
			this.extra = null;
		}

		var newDstNamespaces = new ArrayList<String>();
		newDstNamespaces.add(this.newNs.getDstNamespaces().get(0));
		newDstNamespaces.addAll(this.src.getDstNamespaces());
		this.output.visitNamespaces(this.src.getSrcNamespace(), newDstNamespaces);
	}

	private static MemoryMappingTree readInput(Path tiny) throws IOException {
		MemoryMappingTree src = new MemoryMappingTree();
		MappingReader.read(tiny, src);
		List<String> inputNamespaces = new ArrayList<>(src.getDstNamespaces());
		inputNamespaces.add(0, src.getSrcNamespace());

		if (!inputNamespaces.equals(INPUT_NAMESPACES) && !inputNamespaces.equals(INPUT_NAMESPACES_WITH_MOJANG)) {
			throw new MappingException("Mapping file " + tiny + " does not have 'official(, mojang), intermediary, named' as its namespaces! Found: " + inputNamespaces);
		}

		return src;
	}

	/**
	 * Creates a destination name array where the first element
	 * will be the new namespace name of the mapping element.
	 */
	private String[] createDstNameArray(MappingTree.ElementMappingView newNs) {
		String[] dstNames = new String[output.getDstNamespaces().size()];
		dstNames[0] = newNs.getDstName(0);
		return dstNames;
	}

	/**
	 * Copies all the original destination names from an element.
	 */
	private void copyDstNames(String[] dstNames, MappingTreeView.ElementMappingView from) {
		for (int i = 1; i < dstNames.length; i++) {
			dstNames[i] = from.getDstName(i - 1);
		}
	}

	/**
	 * Fills in an array of destination names with the element's source name.
	 */
	private void fillMappings(String[] names, MappingTree.ElementMappingView newNs) {
		for (int i = 1; i < names.length; i++) {
			names[i] = newNs.getSrcName();
		}
	}

	public MemoryMappingTree merge() throws IOException {
		for (MappingTree.ClassMapping newNsClass : newNs.getClasses()) {
			String[] dstNames = createDstNameArray(newNsClass);
			MappingTree.ClassMapping tinyClass = src.getClass(newNsClass.getSrcName());
			String comment = null;

			if (tinyClass != null) {
				copyDstNames(dstNames, tinyClass);
				comment = tinyClass.getComment();
			} else if (lenient) {
				// Tiny class not found, we'll just use new namespace names
				fillMappings(dstNames, newNsClass);
			} else {
				throw new MappingException("Could not find class " + newNsClass.getSrcName() + "|" + newNsClass.getDstName(0));
			}

			flatOutput.visitClass(newNsClass.getSrcName(), dstNames);
			if (comment != null) flatOutput.visitClassComment(newNsClass.getSrcName(), comment);

			for (MappingTree.FieldMapping field : newNsClass.getFields()) {
				mergeField(newNsClass, field, tinyClass);
			}

			for (MappingTree.MethodMapping method : newNsClass.getMethods()) {
				mergeMethod(newNsClass, method, tinyClass);
			}
		}

		resolveConflicts();

		return output;
	}

	private void mergeField(MappingTree.ClassMapping newNsClass, MappingTree.FieldMapping newNsField, @Nullable MappingTree.ClassMapping tinyClass) throws IOException {
		String[] dstNames = createDstNameArray(newNsField);
		MappingTree.FieldMapping tinyField = null;
		String srcDesc = newNsField.getSrcDesc();
		String comment = null;

		if (tinyClass != null) {
			if (srcDesc != null) {
				tinyField = tinyClass.getField(newNsField.getSrcName(), newNsField.getSrcDesc());
			} else {
				tinyField = CollectionUtil.find(tinyClass.getFields(), field -> field.getSrcName().equals(newNsField.getSrcName())).orElse(null);
			}
		} else if (!lenient) {
			throw new MappingException("Could not find field " + newNsClass.getDstName(0) + '.' + newNsField.getDstName(0) + ' ' + newNsField.getDstDesc(0));
		}

		if (tinyField != null) {
			copyDstNames(dstNames, tinyField);
			srcDesc = tinyField.getSrcDesc();
			comment = tinyField.getComment();
		} else {
			fillMappings(dstNames, newNsField);
		}

		if (srcDesc != null) {
			flatOutput.visitField(newNsClass.getSrcName(), newNsField.getSrcName(), srcDesc, dstNames);
			if (comment != null) flatOutput.visitFieldComment(newNsClass.getSrcName(), newNsField.getSrcName(), srcDesc, comment);
		} else if (!lenient) {
			throw new MappingException("Could not find descriptor for field " + newNsClass.getDstName(0) + '.' + newNsField.getDstName(0));
		}
	}

	private void mergeMethod(MappingTree.ClassMapping newNsClass, MappingTree.MethodMapping newNsMethod, @Nullable MappingTree.ClassMapping tinyClass) throws IOException {
		String[] dstNames = createDstNameArray(newNsMethod);
		MappingTree.MethodMapping tinyMethod = null;
		String intermediaryName, namedName;
		String comment = null;

		if (tinyClass != null) {
			tinyMethod = tinyClass.getMethod(newNsMethod.getSrcName(), newNsMethod.getSrcDesc());
		} else if (!lenient) {
			throw new MappingException("Could not find method " + newNsClass.getDstName(0) + '.' + newNsMethod.getDstName(0) + ' ' + newNsMethod.getDstDesc(0));
		}

		if (tinyMethod != null) {
			copyDstNames(dstNames, tinyMethod);
			intermediaryName = tinyMethod.getName("intermediary");
			namedName = tinyMethod.getName("named");
			comment = tinyMethod.getComment();
		} else {
			if (newNsMethod.getSrcName().equals(newNsMethod.getDstName(0))) {
				// These are only methods like <init> or toString which have the same name in every NS.
				// We can safely ignore those.
				return;
			}

			@Nullable MappingTree.MethodMapping fillMethod = null;

			if (extra != null) {
				MappingTree.MethodMapping extraMethod = extra.getMethod(newNsClass.getSrcName(), newNsMethod.getSrcName(), newNsMethod.getSrcDesc());

				if (extraMethod != null && extraMethod.getSrcName().equals(extraMethod.getDstName(0))) {
					fillMethod = extraMethod;
				}
			}

			if (fillMethod != null) {
				fillMappings(dstNames, fillMethod);
				intermediaryName = namedName = fillMethod.getSrcName();
			} else {
				// Do not allow missing methods as these are typically subclass methods and cause issues where
				// class B extends A, and overrides a method from the superclass. Then the subclass method
				// DOES get a new namespace name but not a yarn/intermediary name.
				return;
			}
		}

		if (!newNsMethod.getSrcName().equals(dstNames[0])) { // ignore <init> and the likes
			methodsByNewNs.put(
					new MethodKey(dstNames[0], newNsMethod.getSrcDesc()),
					new MethodData(newNsClass.getSrcName(), newNsMethod.getSrcName(), newNsMethod.getSrcDesc(), tinyMethod != null, intermediaryName, namedName)
			);
		}

		flatOutput.visitMethod(newNsClass.getSrcName(), newNsMethod.getSrcName(), newNsMethod.getSrcDesc(), dstNames);
		if (comment != null) flatOutput.visitMethodComment(newNsClass.getSrcName(), newNsMethod.getSrcName(), newNsMethod.getSrcDesc(), comment);

		if (tinyMethod != null) {
			for (MappingTree.MethodArgMapping arg : tinyMethod.getArgs()) {
				String[] argDstNames = new String[output.getDstNamespaces().size()];
				copyDstNames(argDstNames, arg);
				flatOutput.visitMethodArg(
						newNsClass.getSrcName(), newNsMethod.getSrcName(), newNsMethod.getSrcDesc(),
						arg.getArgPosition(), arg.getLvIndex(), arg.getSrcName(), argDstNames
				);

				if (arg.getComment() != null) {
					flatOutput.visitMethodArgComment(
							newNsClass.getSrcName(), newNsMethod.getSrcName(), newNsMethod.getSrcDesc(),
							arg.getArgPosition(), arg.getLvIndex(), arg.getSrcName(),
							arg.getComment()
					);
				}
			}
		}
	}

	/**
	 * Resolves conflicts where multiple methods map to a method in the new namespace.
	 * We will prefer the ones with the Tiny mappings.
	 */
	private void resolveConflicts() {
		List<String> conflicts = new ArrayList<>();

		for (MethodKey methodKey : methodsByNewNs.keySet()) {
			List<MethodData> methods = methodsByNewNs.get(methodKey);
			if (methods.size() == 1) continue;

			// Determine whether the names conflict
			Set<String> foundNamedNames = new HashSet<>();

			for (MethodData method : methods) {
				foundNamedNames.add(method.namedName());
			}

			if (foundNamedNames.size() == 1) {
				// No conflict, go on
				continue;
			}

			// Find preferred method
			@Nullable MethodData preferred = findPreferredMethod(methods, conflicts::add);
			if (preferred == null) continue;

			// Remove non-preferred methods
			for (MethodData method : methods) {
				if (method != preferred) {
					MappingTree.ClassMapping clazz = output.getClass(method.obfOwner());
					clazz.getMethods().removeIf(m -> m.getSrcName().equals(method.obfName()) && m.getSrcDesc().equals(method.obfDesc()));
				}
			}
		}

		if (!conflicts.isEmpty()) {
			throw new MappingException("Unfixable conflicts:\n" + String.join("\n", conflicts));
		}
	}

	private @Nullable MethodData findPreferredMethod(List<MethodData> methods, Consumer<String> conflictReporter) {
		List<MethodData> hasTiny = CollectionUtil.filter(methods, MethodData::hasTiny);

		// Record conflicts if needed
		if (hasTiny.size() > 1) { // Multiple methods map to this new name
			// Sometimes unrelated methods share a SRG name. Probably overrides from a past version?
			Set<String> intermediaryNames = new HashSet<>();

			for (MethodData method : methods) {
				intermediaryNames.add(method.intermediaryName());
			}

			// Only record a conflict if we map one intermediary name with multiple named names
			if (intermediaryNames.size() == 1) {
				StringBuilder message = new StringBuilder();
				message.append("- multiple preferred methods for ").append(newNs).append(':');

				for (MethodData preferred : hasTiny) {
					message.append("\n\t> ").append(preferred);
				}

				conflictReporter.accept(message.toString());
			}

			return null;
		} else if (hasTiny.isEmpty()) { // No methods map to this new name
			conflictReporter.accept("- no preferred methods found for " + newNs + ", available: " + methods);
			return null;
		}

		return hasTiny.get(0);
	}

	/**
	 * Merges SRG mappings with a tiny mappings tree through the obf names.
	 * This overload returns a {@link MemoryMappingTree} of the merged mappings.
	 *
	 * <p>The namespaces in the tiny file should be {@code official, intermediary, named}.
	 * The SRG names will add a new namespace called {@code srg} so that the final namespaces
	 * are {@code official, srg, intermediary, named}.
	 *
	 * <p>This method does not include local variables in the output.
	 * It can, however, be used for remapping the game jar since it has all
	 * classes, methods, fields, parameters and javadoc comments.
	 *
	 * <p>If {@code lenient} is true, the merger will not error when encountering names not present
	 * in the tiny mappings. Instead, the names will be filled from the {@code official} namespace.
	 *
	 * @param srg           the SRG file in .tsrg format
	 * @param tiny          the tiny file
	 * @param extraMappings an extra mappings file that will be used to determine
	 *                      whether an unobfuscated name is needed in the result file
	 * @param lenient       whether lenient mode is enabled
	 * @return the merged mapping tree
	 * @throws IOException      if an IO error occurs while reading the mappings
	 * @throws MappingException if the input tiny mappings' namespaces are incorrect
	 *                          or if an element mentioned in the SRG file does not have tiny mappings in non-lenient mode
	 */
	public static MemoryMappingTree mergeSrg(Path srg, Path tiny, @Nullable ExtraMappings extraMappings, boolean lenient)
			throws IOException, MappingException {
		return new ForgeMappingsMerger(readSrg(srg), readInput(tiny), extraMappings, lenient).merge();
	}

	public static MemoryMappingTree mergeMojang(MappingContext context, Path tiny, @Nullable ExtraMappings extraMappings, boolean lenient)
			throws IOException, MappingException {
		MemoryMappingTree mojang = new MemoryMappingTree();
		SrgProvider.visitMojangMappings(new MappingNsRenamer(mojang, Map.of(MappingsNamespace.NAMED.toString(), MappingsNamespace.MOJANG.toString())), context);
		return new ForgeMappingsMerger(mojang, readInput(tiny), extraMappings, lenient).merge();
	}

	private static MemoryMappingTree readSrg(Path srg) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(srg)) {
			MemoryMappingTree output = new MemoryMappingTree();
			TsrgFileReader.read(reader, new ForwardingMappingVisitor(output) {
				// Override the namespaces to be official -> srg
				@Override
				public void visitNamespaces(String srcNamespace, List<String> dstNamespaces) throws IOException {
					List<String> newDstNamespaces = new ArrayList<>(dstNamespaces);
					newDstNamespaces.set(0, MappingsNamespace.SRG.toString());
					super.visitNamespaces(MappingsNamespace.OFFICIAL.toString(), newDstNamespaces);
				}
			});
			return output;
		}
	}

	public record ExtraMappings(Path path, MappingFormat format, String obfuscatedNamespace, String deobfuscatedNamespace) {
		boolean hasCorrectNamespaces() {
			return obfuscatedNamespace.equals(MappingsNamespace.OFFICIAL.toString()) && deobfuscatedNamespace.equals(MappingsNamespace.NAMED.toString());
		}

		public static ExtraMappings ofMojmapTsrg(Path path) {
			return new ExtraMappings(path, MappingFormat.TSRG_2_FILE, MappingsNamespace.OFFICIAL.toString(), MappingsNamespace.NAMED.toString());
		}
	}

	private record MethodKey(String name, String desc) {
		@Override
		public String toString() {
			return name + desc;
		}
	}

	private record MethodData(String obfOwner, String obfName, String obfDesc, boolean hasTiny, String intermediaryName, String namedName) {
		@Override
		public String toString() {
			return "%s.%s%s => %s/%s (%s)".formatted(obfOwner, obfName, obfDesc, intermediaryName, namedName, hasTiny ? "tiny" : "filled");
		}
	}
}
