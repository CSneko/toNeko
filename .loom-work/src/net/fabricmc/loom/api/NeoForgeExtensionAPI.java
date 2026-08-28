/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2023 FabricMC
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

import org.gradle.api.file.ConfigurableFileCollection;

/**
 * This is the NeoForge extension API available to build scripts.
 */
public interface NeoForgeExtensionAPI {
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
}
