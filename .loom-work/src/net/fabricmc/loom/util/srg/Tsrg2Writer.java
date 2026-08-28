/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021 FabricMC
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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MappingTreeView;

public class Tsrg2Writer {
	public Tsrg2Writer() {
	}

	public static String serialize(MappingTree tree) {
		List<String> namespaces = Stream.concat(Stream.of(tree.getSrcNamespace()), tree.getDstNamespaces().stream()).collect(Collectors.toList());
		StringBuilder builder = new StringBuilder();
		writeHeader(namespaces, builder);

		for (MappingTree.ClassMapping classMapping : tree.getClasses()) {
			writeClass(namespaces, classMapping, builder);
		}

		return builder.toString();
	}

	private static void writeClass(List<String> namespaces, MappingTree.ClassMapping def, StringBuilder builder) {
		writeMapped(null, namespaces, def, builder);

		for (MappingTree.MethodMapping method : def.getMethods()) {
			writeMethod(namespaces, method, builder);
		}

		for (MappingTree.FieldMapping field : def.getFields()) {
			writeMapped('\t', namespaces, field, builder);
		}
	}

	private static void writeMethod(List<String> namespaces, MappingTree.MethodMapping def, StringBuilder builder) {
		writeMapped('\t', namespaces, def, builder);

		for (MappingTree.MethodArgMapping arg : def.getArgs()) {
			builder.append("\t\t").append(arg.getLvIndex());
			writeMapped(' ', namespaces, arg, builder);
		}
	}

	private static void writeField(List<String> namespaces, MappingTree.FieldMapping def, StringBuilder builder) {
		writeMapped('\t', namespaces, def, builder);
	}

	private static void writeMapped(Character first, List<String> namespaces, MappingTreeView.ElementMappingView mapped, StringBuilder builder) {
		String[] names = namespaces.stream().map(mapped::getName).toArray(String[]::new);

		for (int i = 0; i < names.length; ++i) {
			String name = names[i];

			if (i == 0) {
				if (first != null) {
					builder.append(first);
				}
			} else {
				builder.append(' ');
			}

			builder.append(name);

			if (i == 0 && mapped instanceof MappingTreeView.MemberMappingView) {
				String descriptor = ((MappingTreeView.MemberMappingView) mapped).getSrcDesc();

				if (descriptor != null && !descriptor.isEmpty()) {
					builder.append(' ');
					builder.append(descriptor);
				}
			}
		}

		builder.append('\n');
	}

	private static void writeHeader(List<String> namespaces, StringBuilder builder) {
		builder.append("tsrg2");

		for (String namespace : namespaces) {
			builder.append(' ');
			builder.append(namespace);
		}

		builder.append('\n');
	}
}
