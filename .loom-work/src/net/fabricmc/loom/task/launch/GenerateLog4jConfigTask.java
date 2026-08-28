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

package net.fabricmc.loom.task.launch;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.inject.Inject;

import dev.architectury.loom.util.ForgeLoggerConfig;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.fabricmc.loom.task.AbstractLoomTask;

public abstract class GenerateLog4jConfigTask extends AbstractLoomTask {
	@OutputFile
	public abstract RegularFileProperty getOutputFile();

	@ApiStatus.Internal
	@Input
	protected abstract Property<Boolean> getUseForgeLoggerConfig();

	@ApiStatus.Internal
	@InputFile
	@Optional
	protected abstract RegularFileProperty getForgeLoggerConfigSource();

	@Inject
	public GenerateLog4jConfigTask() {
		getOutputFile().set(getExtension().getFiles().getDefaultLog4jConfigFile());

		if (getExtension().isForge()) {
			getUseForgeLoggerConfig().set(getProject().provider(() -> getExtension().getForge().getUseForgeLoggerConfig().get()));
			getForgeLoggerConfigSource().set(getProject().getLayout().file(
					getProject().provider(() -> ForgeLoggerConfig.getForgeLoggerConfigSource(getProject()))
			));
		} else {
			getUseForgeLoggerConfig().set(false);
		}
	}

	@TaskAction
	public void run() {
		Path outputFile = getOutputFile().get().getAsFile().toPath();

		if (getUseForgeLoggerConfig().get()) {
			final @Nullable RegularFile source = getForgeLoggerConfigSource().getOrNull();
			if (source == null) ForgeLoggerConfig.throwNotFound();
			ForgeLoggerConfig.copyToPath(source.getAsFile().toPath(), outputFile);
			return;
		}

		try (InputStream is = GenerateLog4jConfigTask.class.getClassLoader().getResourceAsStream("log4j2.fabric.xml")) {
			Files.deleteIfExists(outputFile);
			Files.copy(is, outputFile);
		} catch (IOException e) {
			throw new RuntimeException("Failed to generate log4j config", e);
		}
	}
}
