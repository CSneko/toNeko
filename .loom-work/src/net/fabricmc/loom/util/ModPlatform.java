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

import java.util.Locale;
import java.util.function.Supplier;

import org.gradle.api.GradleException;
import org.gradle.api.Project;

import net.fabricmc.loom.LoomGradleExtension;
import net.fabricmc.loom.api.LoomGradleExtensionAPI;

public enum ModPlatform {
	FABRIC("Fabric", false),
	FORGE("Forge", false),
	QUILT("Quilt", false),
	NEOFORGE("NeoForge", false);

	private final String displayName;
	private final boolean experimental;

	ModPlatform(String displayName, boolean experimental) {
		this.displayName = displayName;
		this.experimental = experimental;
	}

	/**
	 * Returns the lowercase ID of this mod platform.
	 */
	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}

	public String displayName() {
		return displayName;
	}

	public boolean isExperimental() {
		return experimental;
	}

	public boolean isForgeLike() {
		return this == FORGE || this == NEOFORGE;
	}

	public static void assertPlatform(Project project, ModPlatform platform) {
		assertPlatform(LoomGradleExtension.get(project), platform);
	}

	public static void assertPlatform(LoomGradleExtensionAPI extension, ModPlatform platform) {
		assertPlatform(extension, platform, () -> {
			String msg = "Loom is not running on %s.%nYou can switch to it by adding 'loom.platform = %s' to your gradle.properties";
			return msg.formatted(platform.displayName(), platform.id());
		});
	}

	public static void assertPlatform(LoomGradleExtensionAPI extension, ModPlatform platform, Supplier<String> message) {
		if (extension.getPlatform().get() != platform) {
			throw new GradleException(message.get());
		}
	}

	public static void assertForgeLike(LoomGradleExtensionAPI extension) {
		assertForgeLike(extension, () -> "Loom is not running on a Forge-like platform (Forge or NeoForge).");
	}

	public static void assertForgeLike(LoomGradleExtensionAPI extension, Supplier<String> message) {
		if (!extension.getPlatform().get().isForgeLike()) {
			throw new GradleException(message.get());
		}
	}
}
