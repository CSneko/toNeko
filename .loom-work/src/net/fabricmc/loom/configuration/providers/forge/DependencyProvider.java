/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016-2022 FabricMC
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

import java.io.File;
import java.nio.file.Path;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

import net.fabricmc.loom.LoomGradleExtension;
import net.fabricmc.loom.configuration.DependencyInfo;
import net.fabricmc.loom.configuration.providers.minecraft.MinecraftProvider;
import net.fabricmc.loom.extension.LoomFiles;

public abstract class DependencyProvider {
	private final Project project;
	private final LoomGradleExtension extension;

	public DependencyProvider(Project project) {
		this.project = project;
		this.extension = LoomGradleExtension.get(project);
	}

	public abstract void provide(DependencyInfo dependency) throws Exception;

	public abstract String getTargetConfig();

	public Dependency addDependency(Object object, String target) {
		return addDependency(project, object, target);
	}

	static Dependency addDependency(Project project, Object object, String target) {
		if (object instanceof File || object instanceof Path) {
			object = project.files(object);
		}

		return project.getDependencies().add(target, object);
	}

	public Project getProject() {
		return project;
	}

	public LoomGradleExtension getExtension() {
		return extension;
	}

	public LoomFiles getDirectories() {
		return getExtension().getFiles();
	}

	public MinecraftProvider getMinecraftProvider() {
		return getExtension().getMinecraftProvider();
	}

	public boolean refreshDeps() {
		return getExtension().refreshDeps();
	}
}
