/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2024 FabricMC
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

package net.fabricmc.loom.configuration.providers.forge;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import net.fabricmc.loom.LoomGradleExtension;
import net.fabricmc.loom.api.mappings.layered.MappingsNamespace;
import net.fabricmc.loom.configuration.providers.minecraft.MinecraftProvider;
import net.fabricmc.loom.util.FileSystemUtil;
import net.fabricmc.loom.util.Pair;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.format.tiny.Tiny2FileWriter;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

/**
 * With some forge patches, methods can inherit methods from a class that is not in the mappings.
 * This migrator will try to detect all the methods that are inherited from a class that is not in the mappings,
 * see if there are different names for the same method in the mappings, and remove them.
 */
public final class MethodInheritanceMappingsMigrator implements MappingsMigrator {
	private Set<Pair<String, String>> methodsToRemove;

	@Override
	public long setup(Project project, MinecraftProvider minecraftProvider, Path cache, Path rawMappings, boolean hasSrg, boolean hasMojang) throws IOException {
		Path cacheFile = cache.resolve("method-inheritance-migrator.json");

		if (!minecraftProvider.refreshDeps() && Files.exists(cacheFile)) {
			try (BufferedReader reader = Files.newBufferedReader(cacheFile)) {
				List<Pair<String, String>> list = new Gson().fromJson(reader, new TypeToken<List<Pair<String, String>>>() {
				}.getType());
				methodsToRemove = new HashSet<>(list);
			}
		} else {
			Files.deleteIfExists(cacheFile);
			LoomGradleExtension extension = LoomGradleExtension.get(project);
			Path patchedIntermediateJar = MinecraftPatchedProvider.get(project).getMinecraftPatchedIntermediateJar();
			List<Path> jars = List.of(patchedIntermediateJar, extension.getForgeUniversalProvider().getForge().toPath(), extension.getForgeUserdevProvider().getUserdevJar().toPath());
			methodsToRemove = prepareCache(project.getLogger(), rawMappings, jars, hasSrg, hasMojang);
			Files.writeString(cacheFile, new Gson().toJson(methodsToRemove.stream().sorted(Comparator.comparing(p -> p.left() + "|" + p.right())).toList()), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		}

		return methodsToRemove.hashCode();
	}

	@Override
	public void migrate(Project project, List<MappingsEntry> entries) throws IOException {
		for (MappingsEntry entry : entries) {
			MemoryMappingTree mappings = new MemoryMappingTree();

			try (BufferedReader reader = Files.newBufferedReader(entry.path())) {
				MappingReader.read(reader, mappings);
			}

			for (MappingTree.ClassMapping classMapping : mappings.getClasses()) {
				if (classMapping.getMethods().isEmpty()) continue;
				classMapping.getMethods().removeIf(method -> {
					return methodsToRemove.contains(new Pair<>(method.getName(MappingsNamespace.INTERMEDIARY.toString()), method.getDesc(MappingsNamespace.INTERMEDIARY.toString())));
				});
			}

			try (Writer writer = Files.newBufferedWriter(entry.path(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
				mappings.accept(new Tiny2FileWriter(writer, false));
			}
		}
	}

	private Set<Pair<String, String>> prepareCache(Logger logger, Path rawMappings, List<Path> jars, boolean hasSrg, boolean hasMojang) throws IOException {
		MemoryMappingTree mappings = new MemoryMappingTree();
		String patchedNs = hasSrg ? MappingsNamespace.SRG.toString() : MappingsNamespace.MOJANG.toString();

		try (BufferedReader reader = Files.newBufferedReader(rawMappings)) {
			MappingReader.read(reader, new MappingSourceNsSwitch(mappings, patchedNs));
		}

		Pair<Multimap<String, String>, Set<MethodKey>> collected = collectClassesAndMethods(jars);
		Multimap<String, String> classInheritanceMap = collected.left();
		Set<MethodKey> methods = collected.right();

		Multimap<MethodKey, Pair<String, String>> overriddenIntermediaries = Multimaps.newSetMultimap(new HashMap<>(), LinkedHashSet::new);

		for (MethodKey method : methods) {
			// First check if the method is in the mappings, and as a different intermediary name
			MappingTree.ClassMapping aClass = mappings.getClass(method.className());
			if (aClass == null) continue;
			MappingTree.MethodMapping aMethod = aClass.getMethod(method.name(), method.descriptor());
			if (aMethod == null) continue;
			String intermediaryName = aMethod.getName(MappingsNamespace.INTERMEDIARY.toString());
			if (intermediaryName == null || Objects.equals(intermediaryName, method.name())) continue;

			for (String superClass : classInheritanceMap.get(method.className())) {
				if (methods.contains(new MethodKey(superClass, method.name(), method.descriptor()))) {
					if (mappings.getClass(superClass) == null) {
						// We will collect these methods here, and remove them later
						// if there are more than intermediary name for the same method
						String intermediaryDesc = aMethod.getDesc(MappingsNamespace.INTERMEDIARY.toString());
						overriddenIntermediaries.put(new MethodKey(superClass, method.name(), method.descriptor()), new Pair<>(intermediaryName, intermediaryDesc));
					}
				}
			}
		}

		Set<Pair<String, String>> methodsToRemove = new HashSet<>();

		for (Map.Entry<MethodKey, Collection<Pair<String, String>>> entry : overriddenIntermediaries.asMap().entrySet()) {
			if (entry.getValue().size() >= 2) {
				// We should remove these names from the mappings
				// as the particular method is inherited by multiple different intermediary names
				for (Pair<String, String> pair : entry.getValue()) {
					methodsToRemove.add(pair);
					logger.info("Removing method {}{} from the mappings", pair.left(), pair.right());
				}

				break;
			}
		}

		return methodsToRemove;
	}

	private static Pair<Multimap<String, String>, Set<MethodKey>> collectClassesAndMethods(Iterable<Path> jars) throws IOException {
		Multimap<String, String> classInheritanceMap = Multimaps.newSetMultimap(new HashMap<>(), LinkedHashSet::new);
		Set<MethodKey> methods = new HashSet<>();
		Visitor visitor = new Visitor(Opcodes.ASM9, classInheritanceMap, methods);

		for (Path jar : jars) {
			try (FileSystemUtil.Delegate system = FileSystemUtil.getJarFileSystem(jar, false)) {
				for (Path fsPath : (Iterable<? extends Path>) Files.walk(system.get().getPath("/"))::iterator) {
					if (Files.isRegularFile(fsPath) && fsPath.toString().endsWith(".class")) {
						new ClassReader(Files.readAllBytes(fsPath)).accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
					}
				}
			}
		}

		// Populate class inheritance
		Multimap<String, String> classes = Multimaps.newSetMultimap(new HashMap<>(), LinkedHashSet::new);

		for (Map.Entry<String, Collection<String>> entry : classInheritanceMap.asMap().entrySet()) {
			Set<String> allSuperClasses = new HashSet<>();

			for (String superClass : entry.getValue()) {
				collectSuperClasses(superClass, new HashSet<>(), allSuperClasses, classInheritanceMap);
			}

			classes.putAll(entry.getKey(), allSuperClasses);
		}

		return new Pair<>(classes, methods);
	}

	private static void collectSuperClasses(String className, Set<String> travelled, Set<String> allSuperClasses, Multimap<String, String> classInheritanceMap) {
		if (className != null && !className.isEmpty()) {
			allSuperClasses.add(className);

			for (String superClass : classInheritanceMap.get(className)) {
				if (travelled.add(superClass)) {
					collectSuperClasses(superClass, travelled, allSuperClasses, classInheritanceMap);
				}
			}
		}
	}

	private static class Visitor extends ClassVisitor {
		private final Multimap<String, String> classInheritanceMap;
		private final Set<MethodKey> methods;
		private String lastClass = null;

		Visitor(int api, Multimap<String, String> classInheritanceMap, Set<MethodKey> methods) {
			super(api);
			this.classInheritanceMap = classInheritanceMap;
			this.methods = methods;
		}

		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			this.lastClass = name;
			this.classInheritanceMap.put(name, superName);
			this.classInheritanceMap.putAll(name, Arrays.asList(interfaces));
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
			this.methods.add(new MethodKey(this.lastClass, name, descriptor));
			return super.visitMethod(access, name, descriptor, signature, exceptions);
		}
	}

	private record MethodKey(String className, String name, String descriptor) {
	}
}
