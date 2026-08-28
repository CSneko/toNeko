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

package net.fabricmc.loom.util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gradle.api.Named;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.file.FileCollection;

/**
 * Simplified but powerful dependency downloading.
 *
 * @author Juuz
 */
public final class DependencyDownloader {
	private final Project project;
	private final List<DependencyEntry> dependencies = new ArrayList<>();
	private final Map<Attribute<?>, Object> attributes = new HashMap<>();

	public DependencyDownloader(Project project) {
		this.project = project;
	}

	/**
	 * Adds a dependency to download.
	 *
	 * @param dependencyNotation the dependency notation
	 * @return this downloader
	 */
	public DependencyDownloader add(String dependencyNotation) {
		dependencies.add(new DependencyEntry.Notation(dependencyNotation));
		return this;
	}

	/**
	 * Adds a platform dependency.
	 *
	 * @param dependencyNotation the dependency notation
	 * @return this downloader
	 */
	public DependencyDownloader platform(String dependencyNotation) {
		dependencies.add(new DependencyEntry.Platform(dependencyNotation));
		return this;
	}

	/**
	 * Adds all dependencies from a configuration to download.
	 *
	 * @param configuration the dependency configuration
	 * @return this downloader
	 */
	public DependencyDownloader addAll(Configuration configuration) {
		configuration.getAllDependencies().stream()
				.map(DependencyEntry.Direct::new)
				.forEach(dependencies::add);
		return this;
	}

	/**
	 * Adds an attribute to control the downloaded artifacts.
	 *
	 * @param attribute the attribute
	 * @param value     the attribute's value
	 * @param <T> the value type
	 * @return this downloader
	 */
	public <T> DependencyDownloader attribute(Attribute<T> attribute, T value) {
		attributes.put(attribute, value);
		return this;
	}

	/**
	 * Adds a named attribute to control the downloaded artifacts.
	 *
	 * @param attribute the attribute
	 * @param name      the attribute's name
	 * @param <T> the value type
	 * @return this downloader
	 */
	public <T extends Named> DependencyDownloader attribute(Attribute<T> attribute, String name) {
		T value = project.getObjects().named(attribute.getType(), name);
		return attribute(attribute, value);
	}

	/**
	 * Resolves the dependencies as well as their transitive dependencies into a {@link FileCollection}.
	 *
	 * @return the resolved files
	 */
	public FileCollection download() {
		return download(true, false);
	}

	/**
	 * Resolves a dependency as well as its transitive dependencies into a {@link FileCollection}.
	 *
	 * @param transitive whether to include transitive dependencies
	 * @param resolve    whether to eagerly resolve the file collection
	 * @return the resolved files
	 */
	@SuppressWarnings("unchecked")
	public FileCollection download(boolean transitive, boolean resolve) {
		Dependency[] dependencies = this.dependencies.stream()
				.map(entry -> entry.getDependency(project.getDependencies(), transitive))
				.toArray(Dependency[]::new);

		Configuration config = project.getConfigurations().detachedConfiguration(dependencies);
		config.setTransitive(transitive);
		config.attributes(attributes -> {
			this.attributes.forEach((attribute, value) -> {
				attributes.attribute((Attribute<Object>) attribute, value);
			});
		});

		return resolve ? project.files(config.resolve()) : config;
	}

	/**
	 * Resolves a dependency as well as its transitive dependencies into a {@link FileCollection}.
	 *
	 * @param project            the project needing these files
	 * @param dependencyNotation the dependency notation
	 * @return the resolved files
	 */
	public static FileCollection download(Project project, String dependencyNotation) {
		return new DependencyDownloader(project).add(dependencyNotation).download();
	}

	public static FileCollection download(Project project, String dependencyNotation, boolean transitive, boolean resolve) {
		return new DependencyDownloader(project).add(dependencyNotation).download(transitive, resolve);
	}

	/**
	 * Resolves a configuration and its superconfigurations.
	 *
	 * <p>Note that unlike resolving a {@linkplain Configuration#copyRecursive() recursive copy} of the configuration,
	 * this method overrides the transitivity of all superconfigurations as well.
	 *
	 * @param configuration the configuration to resolve
	 * @param transitive    true if transitive dependencies should be included, false otherwise
	 * @return a mutable set containing the resolved files of the configuration
	 */
	public static Set<File> resolveFiles(Project project, Configuration configuration, boolean transitive) {
		return new DependencyDownloader(project)
				.addAll(configuration)
				.download(true, false)
				.getFiles();
	}

	private sealed interface DependencyEntry {
		Dependency getDependency(DependencyHandler dependencies, boolean transitive);

		record Notation(String notation) implements DependencyEntry {
			@Override
			public Dependency getDependency(DependencyHandler dependencies, boolean transitive) {
				Dependency dependency = dependencies.create(notation);

				if (dependency instanceof ModuleDependency md) {
					md.setTransitive(transitive);
				}

				return dependency;
			}
		}

		record Platform(String notation) implements DependencyEntry {
			@Override
			public Dependency getDependency(DependencyHandler dependencies, boolean transitive) {
				return dependencies.platform(notation);
			}
		}

		record Direct(Dependency dependency) implements DependencyEntry {
			@Override
			public Dependency getDependency(DependencyHandler dependencies, boolean transitive) {
				return dependency;
			}
		}
	}
}
