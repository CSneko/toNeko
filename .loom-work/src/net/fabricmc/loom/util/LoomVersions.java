/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2025 FabricMC
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

/**
 * Auto generated class, do not edit.
 */
public record LoomVersions(String group, String module, String version) {
	public static final LoomVersions ACCESS_TRANSFORMERS = new LoomVersions("net.minecraftforge", "accesstransformers", "3.0.1");
	public static final LoomVersions ACCESS_TRANSFORMERS_LOG4J_BOM = new LoomVersions("org.apache.logging.log4j", "log4j-bom", "2.17.1");
	public static final LoomVersions ACCESS_TRANSFORMERS_NEO = new LoomVersions("net.neoforged.accesstransformers", "at-cli", "10.0.2");
	public static final LoomVersions ACCESS_TRANSFORMERS_NEW = new LoomVersions("net.minecraftforge", "accesstransformers", "8.0.5");
	public static final LoomVersions ASM = new LoomVersions("org.ow2.asm", "asm", "9.7");
	public static final LoomVersions CFR = new LoomVersions("net.fabricmc", "cfr", "0.2.2");
	public static final LoomVersions DEV_LAUNCH_INJECTOR = new LoomVersions("net.fabricmc", "dev-launch-injector", "0.2.1+build.8");
	public static final LoomVersions FERNFLOWER = new LoomVersions("net.fabricmc", "fabric-fernflower", "2.0.0");
	public static final LoomVersions JAVAX_ANNOTATIONS = new LoomVersions("com.google.code.findbugs", "jsr305", "3.0.2");
	public static final LoomVersions JETBRAINS_ANNOTATIONS = new LoomVersions("org.jetbrains", "annotations", "25.0.0");
	public static final LoomVersions MCP_ANNOTATIONS = new LoomVersions("dev.architectury", "mcp-annotations", "2.0.9");
	public static final LoomVersions MIXIN_COMPILE_EXTENSIONS = new LoomVersions("net.fabricmc", "fabric-mixin-compile-extensions", "0.6.0");
	public static final LoomVersions MIXIN_REMAPPER_SERVICE = new LoomVersions("dev.architectury", "architectury-mixin-remapper-service", "2.0.9");
	public static final LoomVersions NAMING_SERVICE = new LoomVersions("dev.architectury", "architectury-naming-service", "2.0.9");
	public static final LoomVersions NATIVE_SUPPORT = new LoomVersions("net.fabricmc", "fabric-loom-native-support", "1.0.1");
	public static final LoomVersions TERMINAL_CONSOLE_APPENDER = new LoomVersions("net.minecrell", "terminalconsoleappender", "1.3.0");
	public static final LoomVersions UNPROTECT = new LoomVersions("io.github.juuxel", "unprotect", "1.2.0");
	public static final LoomVersions VINEFLOWER = new LoomVersions("org.vineflower", "vineflower", "1.10.1");

	public String mavenNotation() {
		return "%s:%s:%s".formatted(group, module, version);
	}
}
