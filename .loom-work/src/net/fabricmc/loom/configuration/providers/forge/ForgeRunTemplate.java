/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2022-2023 FabricMC
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

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.gradle.api.Named;

import net.fabricmc.loom.configuration.ide.RunConfigSettings;
import net.fabricmc.loom.util.Constants;
import net.fabricmc.loom.util.function.CollectionUtil;

public record ForgeRunTemplate(
		String name,
		String main,
		List<ConfigValue> args,
		List<ConfigValue> jvmArgs,
		Map<String, ConfigValue> env,
		Map<String, ConfigValue> props
) implements Named {
	public static final Codec<ForgeRunTemplate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.optionalFieldOf("name", "") // note: empty is used since DFU crashes with null
					.forGetter(ForgeRunTemplate::name),
			Codec.STRING.fieldOf("main")
					.forGetter(ForgeRunTemplate::main),
			ConfigValue.CODEC.listOf().optionalFieldOf("args", List.of())
					.forGetter(ForgeRunTemplate::args),
			ConfigValue.CODEC.listOf().optionalFieldOf("jvmArgs", List.of())
					.forGetter(ForgeRunTemplate::jvmArgs),
			Codec.unboundedMap(Codec.STRING, ConfigValue.CODEC).optionalFieldOf("env", Map.of())
					.forGetter(ForgeRunTemplate::env),
			Codec.unboundedMap(Codec.STRING, ConfigValue.CODEC).optionalFieldOf("props", Map.of())
					.forGetter(ForgeRunTemplate::props)
	).apply(instance, ForgeRunTemplate::new));

	public static final Codec<Map<String, ForgeRunTemplate>> MAP_CODEC = Codec.unboundedMap(Codec.STRING, CODEC)
			.xmap(
					map -> {
						final Map<String, ForgeRunTemplate> newMap = new HashMap<>();

						// TODO: Remove this hack once we patch DLI to support clientData as env
						for (Map.Entry<String, ForgeRunTemplate> entry : map.entrySet()) {
							String name = entry.getKey().replaceAll("clientData", "dataClient").replaceAll("serverData", "dataServer");
							newMap.put(name, entry.getValue());
						}

						// Iterate through all templates and fill in empty names.
						// The NeoForge format doesn't include the name property, so we'll use the map keys
						// as a replacement.
						for (Map.Entry<String, ForgeRunTemplate> entry : newMap.entrySet()) {
							final ForgeRunTemplate template = entry.getValue();

							if (template.name.isEmpty()) {
								final ForgeRunTemplate completed = new ForgeRunTemplate(
										entry.getKey(),
										template.main,
										template.args,
										template.jvmArgs,
										template.env,
										template.props
								);

								entry.setValue(completed);
							}
						}

						return newMap;
					},
					Function.identity()
			);

	@Override
	public String getName() {
		return name;
	}

	public void applyTo(RunConfigSettings settings, ConfigValue.Resolver configValueResolver) {
		if (settings.getDefaultMainClass().equals(Constants.Forge.UNDETERMINED_MAIN_CLASS)) {
			settings.defaultMainClass(main);
		}

		settings.vmArgs(CollectionUtil.map(jvmArgs, value -> value.resolve(configValueResolver)));

		env.forEach((key, value) -> {
			String resolved = value.resolve(configValueResolver);
			settings.getEnvironmentVariables().putIfAbsent(key, resolved);
		});

		// Add MOD_CLASSES, this is something that ForgeGradle does
		settings.getEnvironmentVariables().computeIfAbsent("MOD_CLASSES", $ -> ConfigValue.of("{source_roots}").resolve(configValueResolver));
	}

	public Resolved resolve(ConfigValue.Resolver configValueResolver) {
		final Function<ConfigValue, String> resolve = value -> value.resolve(configValueResolver);
		final Collector<Map.Entry<String, ConfigValue>, ?, Map<String, String>> resolveMap =
				Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().resolve(configValueResolver));

		final List<String> args = this.args.stream().map(resolve).toList();
		final List<String> jvmArgs = this.jvmArgs.stream().map(resolve).toList();
		final Map<String, String> env = this.env.entrySet().stream().collect(resolveMap);
		final Map<String, String> props = this.props.entrySet().stream().collect(resolveMap);

		return new Resolved(
				name,
				main,
				args,
				jvmArgs,
				env,
				props
		);
	}

	public record Resolved(
			String name,
			String main,
			List<String> args,
			List<String> jvmArgs,
			Map<String, String> env,
			Map<String, String> props
	) implements Serializable { }
}
