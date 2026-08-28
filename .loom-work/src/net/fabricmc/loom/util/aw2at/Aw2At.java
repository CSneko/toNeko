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

package net.fabricmc.loom.util.aw2at;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import dev.architectury.at.AccessChange;
import dev.architectury.at.AccessTransform;
import dev.architectury.at.AccessTransformSet;
import dev.architectury.at.ModifierChange;
import org.cadixdev.bombe.type.signature.MethodSignature;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.jetbrains.annotations.VisibleForTesting;

import net.fabricmc.accesswidener.AccessWidenerReader;
import net.fabricmc.accesswidener.AccessWidenerVisitor;
import net.fabricmc.loom.LoomGradleExtension;
import net.fabricmc.loom.task.RemapJarTask;

/**
 * Converts AW files to AT files.
 *
 * @author Juuz
 */
public final class Aw2At {
	public static void setup(Project project, RemapJarTask remapJar) {
		LoomGradleExtension extension = LoomGradleExtension.get(project);

		if (extension.getAccessWidenerPath().isPresent()) {
			// Find the relative AW file name
			String awName = null;
			Path awPath = extension.getAccessWidenerPath().get().getAsFile().toPath();
			SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
			SourceSet main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
			boolean found = false;

			for (File srcDir : main.getResources().getSrcDirs()) {
				Path srcDirPath = srcDir.toPath().toAbsolutePath();

				if (awPath.startsWith(srcDirPath)) {
					awName = srcDirPath.relativize(awPath).toString().replace(File.separator, "/");
					found = true;
					break;
				}
			}

			if (!found) {
				awName = awPath.getFileName().toString();
			}

			remapJar.getAtAccessWideners().add(awName);
		}

		remapJar.getAtAccessWideners().addAll(extension.getForge().getExtraAccessWideners());
	}

	/**
	 * Converts an access widener file to an access transform set.
	 *
	 * @param reader the reader that is used to read the AW
	 * @return the access transform set
	 */
	public static AccessTransformSet toAccessTransformSet(BufferedReader reader) throws IOException {
		AccessTransformSet atSet = AccessTransformSet.create();

		new AccessWidenerReader(new AccessWidenerVisitor() {
			@Override
			public void visitClass(String name, AccessWidenerReader.AccessType access, boolean transitive) {
				atSet.getOrCreateClass(name).merge(toAt(access));
			}

			@Override
			public void visitMethod(String owner, String name, String descriptor, AccessWidenerReader.AccessType access, boolean transitive) {
				atSet.getOrCreateClass(owner).mergeMethod(MethodSignature.of(name, descriptor), toAt(access));
			}

			@Override
			public void visitField(String owner, String name, String descriptor, AccessWidenerReader.AccessType access, boolean transitive) {
				atSet.getOrCreateClass(owner).mergeField(name, toAt(access));
			}
		}).read(reader);

		return atSet;
	}

	@VisibleForTesting
	public static AccessTransform toAt(AccessWidenerReader.AccessType access) {
		return switch (access) {
		// This behaviour doesn't match what the actual AW does for methods.
		//   - accessible makes the method final if it was private
		//   - extendable makes the method protected if it was (package-)private
		// It also ignores that mutable doesn't change the visibility (but AT disallows bare "-f" modifier).
		// None of these can be achieved with Forge ATs without using bytecode analysis.
		// However, this is good enough for us. (The "unwanted" effects to visibility only apply in prod.)
		case ACCESSIBLE -> AccessTransform.of(AccessChange.PUBLIC);
		case EXTENDABLE, MUTABLE -> AccessTransform.of(AccessChange.PUBLIC, ModifierChange.REMOVE);
		};
	}
}
