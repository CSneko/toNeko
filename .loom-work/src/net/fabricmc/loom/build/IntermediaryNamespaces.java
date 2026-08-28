/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2022-2024 FabricMC
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

package net.fabricmc.loom.build;

import org.gradle.api.Project;

import net.fabricmc.loom.LoomGradleExtension;
import net.fabricmc.loom.api.mappings.layered.MappingsNamespace;
import net.fabricmc.loom.util.ModPlatform;

public final class IntermediaryNamespaces {
	/**
	 * Returns the intermediary namespace of the project.
	 */
	public static String intermediary(Project project) {
		return intermediaryNamespace(project).toString();
	}

	/**
	 * Returns the runtime intermediary namespace of the project.
	 * This is the namespace used in the compiled jar.
	 */
	public static String runtimeIntermediary(Project project) {
		return runtimeIntermediaryNamespace(project).toString();
	}

	/**
	 * Returns the intermediary namespace of the project.
	 */
	public static MappingsNamespace intermediaryNamespace(Project project) {
		return intermediaryNamespace(LoomGradleExtension.get(project).getPlatform().get());
	}

	/**
	 * Returns the intermediary namespace of the platform.
	 */
	public static MappingsNamespace intermediaryNamespace(ModPlatform platform) {
		return switch (platform) {
		case FABRIC, QUILT -> MappingsNamespace.INTERMEDIARY;
		case FORGE -> MappingsNamespace.SRG;
		case NEOFORGE -> MappingsNamespace.MOJANG;
		};
	}

	/**
	 * Returns the intermediary namespace of the project.
	 */
	public static MappingsNamespace runtimeIntermediaryNamespace(Project project) {
		LoomGradleExtension extension = LoomGradleExtension.get(project);
		if (extension.isForge() && extension.getForgeProvider().usesMojangAtRuntime()) return MappingsNamespace.MOJANG;
		return intermediaryNamespace(project);
	}

	/**
	 * Potentially replaces the remapping target namespace for mixin refmaps.
	 *
	 * <p>All {@linkplain #runtimeIntermediary(Project) intermediary-like namespaces} are replaced
	 * by {@code intermediary} since fabric-mixin-compile-extensions only supports intermediary.
	 * We transform the namespaces in the input mappings, e.g. {@code intermediary} -> {@code yraidemretni} and
	 * {@code srg} -> {@code intermediary}.
	 *
	 * @param project   the project
	 * @param namespace the original namespace
	 * @return the correct namespace to use
	 */
	public static String replaceMixinIntermediaryNamespace(Project project, String namespace) {
		return namespace.equals(runtimeIntermediary(project)) ? MappingsNamespace.INTERMEDIARY.toString() : namespace;
	}
}
