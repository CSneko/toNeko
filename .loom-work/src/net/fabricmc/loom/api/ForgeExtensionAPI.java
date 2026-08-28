/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021-2024 FabricMC
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

package net.fabricmc.loom.api;

import java.util.List;

import org.gradle.api.Action;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.jetbrains.annotations.ApiStatus;

/**
 * This is the Forge extension API available to build scripts.
 */
@ApiStatus.NonExtendable
public interface ForgeExtensionAPI {
	/**
	 * If true, {@linkplain LoomGradleExtensionAPI#getAccessWidenerPath() the project access widener file}
	 * will be remapped to an access transformer file if set.
	 *
	 * @return the property
	 */
	Property<Boolean> getConvertAccessWideners();

	/**
	 * A set of additional access widener files that will be converted to access transformers
	 * {@linkplain #getConvertAccessWideners() if enabled}. The files are specified as paths in jar files
	 * (e.g. {@code path/to/my_aw.accesswidener}).
	 *
	 * @return the property
	 */
	SetProperty<String> getExtraAccessWideners();

	/**
	 * A collection of all project access transformers.
	 * The collection should only contain AT files, and not directories or other files.
	 *
	 * <p>If this collection is empty, Loom tries to resolve the AT from the default path
	 * ({@code META-INF/accesstransformer.cfg} in the {@code main} source set).
	 *
	 * @return the collection of AT files
	 */
	ConfigurableFileCollection getAccessTransformers();

	/**
	 * Adds a {@linkplain #getAccessTransformers() project access transformer}.
	 *
	 * @param file the file, evaluated as per {@link org.gradle.api.Project#file(Object)}
	 */
	void accessTransformer(Object file);

	/**
	 * A set of all mixin configs related to source set resource roots.
	 * All mixin configs must be added to this property so that they apply in a dev environment.
	 *
	 * @return the property
	 */
	SetProperty<String> getMixinConfigs();

	/**
	 * Adds mixin config files to {@link #getMixinConfigs() mixinConfigs}.
	 *
	 * @param mixinConfigs the mixin config file paths relative to resource roots
	 */
	void mixinConfigs(String... mixinConfigs);

	/**
	 * Adds mixin config files to {@link #getMixinConfigs() mixinConfigs}.
	 *
	 * @param mixinConfigs the mixin config file paths relative to resource roots
	 */
	default void mixinConfig(String... mixinConfigs) {
		mixinConfigs(mixinConfigs);
	}

	/**
	 * If true, upstream Mixin from Sponge will be replaced with Fabric's or Architectury's fork.
	 * This is enabled by default.
	 *
	 * @return the property
	 */
	Property<Boolean> getUseCustomMixin();

	/**
	 * If true, Loom will use Forge's Log4J config file instead of its own.
	 * This is disabled by default.
	 *
	 * @return the property
	 * @deprecated This API is not needed on newer Minecraft versions where Forge forces its own logger config.
	 */
	@ApiStatus.ScheduledForRemoval(inVersion = "2.0")
	@Deprecated(forRemoval = true)
	Property<Boolean> getUseForgeLoggerConfig();

	/**
	 * A list of mod IDs for mods applied for data generation.
	 * The returned list is unmodifiable but not immutable - it will reflect changes done with
	 * {@link #dataGen(Action)}.
	 *
	 * @return the list
	 * @deprecated Removed in favor of configuring the data generator directly.
	 */
	@ApiStatus.ScheduledForRemoval(inVersion = "2.0")
	@Deprecated(forRemoval = true)
	List<String> getDataGenMods();

	/**
	 * Applies data generation settings.
	 *
	 * @param action the action to configure data generation
	 * @deprecated Removed in favor of configuring the data generator directly.
	 */
	@ApiStatus.ScheduledForRemoval(inVersion = "2.0")
	@Deprecated(forRemoval = true)
	void dataGen(Action<DataGenConsumer> action);

	/**
	 * Data generation config.
	 * @deprecated Removed in favor of configuring the data generator directly.
	 */
	@ApiStatus.NonExtendable
	@ApiStatus.ScheduledForRemoval(inVersion = "2.0")
	@Deprecated(forRemoval = true)
	interface DataGenConsumer {
		/**
		 * Adds mod IDs applied for data generation.
		 *
		 * @param modIds the mod IDs
		 */
		void mod(String... modIds);
	}
}
