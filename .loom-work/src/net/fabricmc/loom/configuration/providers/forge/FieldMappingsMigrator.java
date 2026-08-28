/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021-2023 FabricMC
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
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Stopwatch;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.gradle.api.Project;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

import net.fabricmc.loom.LoomGradleExtension;
import net.fabricmc.loom.api.mappings.layered.MappingsNamespace;
import net.fabricmc.loom.configuration.providers.minecraft.MinecraftProvider;
import net.fabricmc.loom.util.FileSystemUtil;
import net.fabricmc.loom.util.ThreadingUtils;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.format.tiny.Tiny2FileWriter;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MappingTreeView;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

public final class FieldMappingsMigrator implements MappingsMigrator {
	private List<Map.Entry<FieldMember, String>> migratedFields = new ArrayList<>();
	public Path migratedFieldsCache;

	@Override
	public long setup(Project project, MinecraftProvider minecraftProvider, Path cache, Path rawMappings, boolean hasSrg, boolean hasMojang) throws IOException {
		migratedFieldsCache = cache.resolve("migrated-fields.json");
		migratedFields.clear();

		if (!minecraftProvider.refreshDeps() && Files.exists(migratedFieldsCache)) {
			try (BufferedReader reader = Files.newBufferedReader(migratedFieldsCache)) {
				Map<String, String> map = new Gson().fromJson(reader, new TypeToken<Map<String, String>>() {
				}.getType());
				migratedFields = new ArrayList<>();
				map.forEach((key, newDescriptor) -> {
					String[] split = key.split("#");
					migratedFields.add(new AbstractMap.SimpleEntry<>(new FieldMember(split[0], split[1]), newDescriptor));
				});
			}
		} else {
			Files.deleteIfExists(migratedFieldsCache);
			migratedFields.clear();

			if (hasSrg) {
				migratedFields.addAll(generateNewFieldMigration(project, MinecraftPatchedProvider.get(project).getMinecraftPatchedIntermediateJar(), MappingsNamespace.SRG.toString(), rawMappings).entrySet());
			} else if (hasMojang) {
				migratedFields.addAll(generateNewFieldMigration(project, MinecraftPatchedProvider.get(project).getMinecraftPatchedIntermediateJar(), MappingsNamespace.MOJANG.toString(), rawMappings).entrySet());
			}

			Map<String, String> map = new HashMap<>();
			migratedFields.forEach(entry -> {
				map.put(entry.getKey().owner + "#" + entry.getKey().field, entry.getValue());
			});
			Files.writeString(migratedFieldsCache, new Gson().toJson(map));
		}

		this.migratedFields.sort(Comparator.comparing(entry -> entry.getKey().owner + "#" + entry.getKey().field));
		return migratedFields.hashCode();
	}

	@Override
	public void migrate(Project project, List<MappingsEntry> entries) {
		Stopwatch stopwatch = Stopwatch.createStarted();
		LoomGradleExtension extension = LoomGradleExtension.get(project);

		try {
			updateFieldMigration(project, entries);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		project.getLogger().info(":migrated {} fields in " + stopwatch.stop(), extension.getPlatform().get().id());
	}

	public void updateFieldMigration(Project project, List<MappingsEntry> entries) throws IOException {
		Table<String, String, String> fieldDescriptorMap = HashBasedTable.create();

		for (Map.Entry<FieldMember, String> entry : migratedFields) {
			fieldDescriptorMap.put(entry.getKey().owner, entry.getKey().field, entry.getValue());
		}

		for (MappingsEntry entry : entries) {
			injectMigration(project, fieldDescriptorMap, entry.path());
		}
	}

	private static void injectMigration(Project project, Table<String, String, String> fieldDescriptorMap, Path path) throws IOException {
		MemoryMappingTree mappings = new MemoryMappingTree();

		try (BufferedReader reader = Files.newBufferedReader(path)) {
			MappingReader.read(reader, mappings);
		}

		for (MappingTree.ClassMapping classDef : new ArrayList<>(mappings.getClasses())) {
			Map<String, String> row = fieldDescriptorMap.row(classDef.getName(MappingsNamespace.INTERMEDIARY.toString()));

			if (!row.isEmpty()) {
				for (MappingTree.FieldMapping fieldDef : new ArrayList<>(classDef.getFields())) {
					String newDescriptor = row.get(fieldDef.getName(MappingsNamespace.INTERMEDIARY.toString()));

					if (newDescriptor != null) {
						String prev = fieldDef.getDesc(MappingsNamespace.INTERMEDIARY.toString());
						fieldDef.setSrcDesc(mappings.mapDesc(newDescriptor, mappings.getNamespaceId(MappingsNamespace.INTERMEDIARY.toString()), MappingTreeView.SRC_NAMESPACE_ID));
						project.getLogger().info("Migrated field descriptor of field {}#{} from {} to {}", classDef.getName(MappingsNamespace.INTERMEDIARY.toString()), fieldDef.getName(MappingsNamespace.INTERMEDIARY.toString()), prev, newDescriptor);
					}
				}
			}
		}

		try (Writer writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
			mappings.accept(new Tiny2FileWriter(writer, false));
		}
	}

	private static Map<FieldMember, String> generateNewFieldMigration(Project project, Path patchedJar, String patchedJarNamespace, Path mappingsPath) throws IOException {
		Map<FieldMember, String> fieldDescriptorMap = new ConcurrentHashMap<>();
		LoomGradleExtension extension = LoomGradleExtension.get(project);
		ThreadingUtils.TaskCompleter completer = ThreadingUtils.taskCompleter();

		class Visitor extends ClassVisitor {
			private final ThreadLocal<String> lastClass = new ThreadLocal<>();

			Visitor(int api) {
				super(api);
			}

			@Override
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				lastClass.set(name);
			}

			@Override
			public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
				fieldDescriptorMap.put(new FieldMember(lastClass.get(), name), descriptor);
				return super.visitField(access, name, descriptor, signature, value);
			}
		}

		Visitor visitor = new Visitor(Opcodes.ASM9);
		FileSystemUtil.Delegate system = FileSystemUtil.getJarFileSystem(patchedJar, false);
		completer.onComplete(value -> system.close());

		for (Path fsPath : (Iterable<? extends Path>) Files.walk(system.get().getPath("/"))::iterator) {
			if (Files.isRegularFile(fsPath) && fsPath.toString().endsWith(".class")) {
				completer.add(() -> {
					byte[] bytes = Files.readAllBytes(fsPath);
					new ClassReader(bytes).accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
				});
			}
		}

		completer.complete();
		Map<FieldMember, String> migratedFields = new HashMap<>();

		try (BufferedReader reader = Files.newBufferedReader(mappingsPath)) {
			MemoryMappingTree mappings = new MemoryMappingTree();
			MappingReader.read(reader, mappings);

			for (MappingTree.ClassMapping classDef : mappings.getClasses()) {
				for (MappingTree.FieldMapping fieldDef : classDef.getFields()) {
					String newDescriptor = fieldDescriptorMap.get(new FieldMember(classDef.getName(patchedJarNamespace), fieldDef.getName(patchedJarNamespace)));
					String existingDescriptor = fieldDef.getDesc(patchedJarNamespace);

					if (newDescriptor != null && !newDescriptor.equals(existingDescriptor)) {
						String ownerIntermediary = classDef.getName(MappingsNamespace.INTERMEDIARY.toString());
						String fieldIntermediary = fieldDef.getName(MappingsNamespace.INTERMEDIARY.toString());
						String descriptorIntermediary = fieldDef.getDesc(MappingsNamespace.INTERMEDIARY.toString());
						String newDescriptorIntermediary = mappings.mapDesc(newDescriptor, mappings.getNamespaceId(patchedJarNamespace),
								mappings.getNamespaceId(MappingsNamespace.INTERMEDIARY.toString()));
						migratedFields.put(new FieldMember(ownerIntermediary, fieldIntermediary), newDescriptorIntermediary);
						project.getLogger().info("Found migration of " + ownerIntermediary + "#" + fieldIntermediary + ": " + descriptorIntermediary + " -> " + newDescriptorIntermediary);
					}
				}
			}
		}

		return migratedFields;
	}

	public static class FieldMember {
		public String owner;
		public String field;

		public FieldMember(String owner, String field) {
			this.owner = owner;
			this.field = field;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			FieldMember that = (FieldMember) o;
			return Objects.equals(owner, that.owner) && Objects.equals(field, that.field);
		}

		@Override
		public int hashCode() {
			return Objects.hash(owner, field);
		}
	}
}
