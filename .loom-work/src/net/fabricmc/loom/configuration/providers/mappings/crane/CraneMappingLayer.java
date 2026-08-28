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

package net.fabricmc.loom.configuration.providers.mappings.crane;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import net.fabricmc.loom.api.mappings.layered.MappingLayer;
import net.fabricmc.loom.util.FileSystemUtil;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.format.tiny.Tiny2FileReader;

public record CraneMappingLayer(Path craneJar) implements MappingLayer {
	private static final String TINY_FILE_NAME = "crane.tiny";

	@Override
	public void visit(MappingVisitor visitor) throws IOException {
		try (FileSystemUtil.Delegate fs = FileSystemUtil.getJarFileSystem(craneJar(), false)) {
			try (BufferedReader reader = Files.newBufferedReader(fs.get().getPath(TINY_FILE_NAME), StandardCharsets.UTF_8)) {
				Tiny2FileReader.read(reader, visitor);
			}
		}
	}
}
