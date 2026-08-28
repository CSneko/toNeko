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

package net.fabricmc.loom.extension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;

import net.fabricmc.loom.LoomGradleExtension;
import net.fabricmc.loom.api.ForgeExtensionAPI;
import net.fabricmc.loom.configuration.ide.RunConfigSettings;

public class ForgeExtensionImpl implements ForgeExtensionAPI {
	private final LoomGradleExtension extension;
	private final Property<Boolean> convertAccessWideners;
	private final SetProperty<String> extraAccessWideners;
	private final ConfigurableFileCollection accessTransformers;
	private final SetProperty<String> mixinConfigs;
	private final Property<Boolean> useCustomMixin;
	private final Property<Boolean> useForgeLoggerConfig;
	private final List<String> dataGenMods = new ArrayList<>(); // not a property because it has custom adding logic

	@Inject
	public ForgeExtensionImpl(Project project, LoomGradleExtension extension) {
		this.extension = extension;
		convertAccessWideners = project.getObjects().property(Boolean.class).convention(false);
		extraAccessWideners = project.getObjects().setProperty(String.class).empty();
		accessTransformers = project.getObjects().fileCollection();
		mixinConfigs = project.getObjects().setProperty(String.class).empty();
		useCustomMixin = project.getObjects().property(Boolean.class).convention(true);
		useForgeLoggerConfig = project.getObjects().property(Boolean.class).convention(false);
	}

	@Override
	public Property<Boolean> getConvertAccessWideners() {
		return convertAccessWideners;
	}

	@Override
	public SetProperty<String> getExtraAccessWideners() {
		return extraAccessWideners;
	}

	@Override
	public ConfigurableFileCollection getAccessTransformers() {
		return accessTransformers;
	}

	@Override
	public void accessTransformer(Object file) {
		accessTransformers.from(file);
	}

	@Override
	public SetProperty<String> getMixinConfigs() {
		return mixinConfigs;
	}

	@Override
	public void mixinConfigs(String... mixinConfigs) {
		this.mixinConfigs.addAll(mixinConfigs);
	}

	@Override
	public Property<Boolean> getUseCustomMixin() {
		return useCustomMixin;
	}

	@Override
	public Property<Boolean> getUseForgeLoggerConfig() {
		return useForgeLoggerConfig;
	}

	@Override
	public List<String> getDataGenMods() {
		// unmod list prevents uncontrolled additions (we want to create the run config too)
		return Collections.unmodifiableList(dataGenMods);
	}

	@SuppressWarnings("Convert2Lambda")
	@Override
	public void dataGen(Action<DataGenConsumer> action) {
		action.execute(new DataGenConsumer() {
			@Override
			public void mod(String... modIds) {
				dataGenMods.addAll(Arrays.asList(modIds));

				if (modIds.length > 0 && extension.getRunConfigs().findByName("data") == null) {
					extension.getRunConfigs().create("data", RunConfigSettings::data);
				}
			}
		});
	}
}
