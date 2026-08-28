/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2024 FabricMC
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Strings;
import com.google.common.collect.Ordering;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A simple version class that can be used to compare versions.
 * This class allows for versions that are not strictly following the semver specification,
 * but are still allowed in the context of gradle versioning.
 *
 * <p>This class is intentionally very flexible and does not enforce any specific versioning scheme,
 * and should be very similar to the versioning used by gradle itself.
 */
public record Version(int major, int minor, int micro, int patch, @Nullable String qualifier) implements Comparable<Version> {
	private static final Pattern REGEX = Pattern.compile("(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?()(?:[.-]([^+\\s]*))?(?:\\+.*)?");
	private static final Pattern REGEX_WITH_PATCH = Pattern.compile("(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?(?:\\.(\\d+))?(?:[+.-]([^+\\s]*))?(?:\\+.*)?");
	public static final Version UNKNOWN = new Version(0, 0, 0, 0, null);

	public static Version parse(String version) {
		return parse(version, false);
	}

	public static Version parse(String version, boolean withPatch) {
		Matcher matcher = (withPatch ? REGEX_WITH_PATCH : REGEX).matcher(version);

		if (!matcher.matches()) {
			return UNKNOWN;
		}

		int major = Integer.parseInt(matcher.group(1));
		int minor = !Strings.isNullOrEmpty(matcher.group(2)) ? Integer.parseInt(matcher.group(2)) : 0;
		int micro = !Strings.isNullOrEmpty(matcher.group(3)) ? Integer.parseInt(matcher.group(3)) : 0;
		int patch = !Strings.isNullOrEmpty(matcher.group(4)) ? Integer.parseInt(matcher.group(4)) : 0;
		String qualifier = matcher.group(5);

		return new Version(major, minor, micro, patch, qualifier == null ? null : qualifier.toLowerCase(Locale.ROOT));
	}

	public Version asBaseVersion() {
		return new Version(this.major, this.minor, this.micro, this.patch, null);
	}

	@Override
	public int compareTo(@NotNull Version other) {
		if (this.major != other.major) {
			return this.major - other.major;
		} else if (this.minor != other.minor) {
			return this.minor - other.minor;
		} else if (this.micro != other.micro) {
			return this.micro - other.micro;
		} else if (this.patch != other.patch) {
			return this.patch - other.patch;
		} else {
			return Ordering.natural().nullsLast()
					.compare(this.qualifier, other.qualifier);
		}
	}
}
