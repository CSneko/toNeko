/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2022 FabricMC
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

package net.fabricmc.loom.configuration.providers.forge.mcpconfig;

import static net.fabricmc.loom.configuration.providers.forge.ConfigValue.PREVIOUS_OUTPUT_SUFFIX;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import net.fabricmc.loom.configuration.providers.forge.ConfigValue;
import net.fabricmc.loom.util.function.CollectionUtil;

public final class DependencySet {
	private final Map<String, McpConfigStep> allSteps;
	private final List<String> stepNames;
	private final List<Predicate<McpConfigStep>> skipRules = new ArrayList<>();
	private final Set<String> steps = new HashSet<>();
	private Predicate<McpConfigStep> ignoreDependenciesFilter = data -> false;

	public DependencySet(List<McpConfigStep> allSteps) {
		this.allSteps = allSteps.stream().collect(Collectors.toMap(McpConfigStep::name, Function.identity()));
		this.stepNames = CollectionUtil.map(allSteps, McpConfigStep::name);
	}

	public void clear() {
		steps.clear();
	}

	public void add(String step) {
		if (!allSteps.containsKey(step)) {
			return;
		}

		steps.add(step);
	}

	public void skip(String step) {
		skip(data -> data.name().equals(step));
	}

	public void skip(Predicate<McpConfigStep> rule) {
		skipRules.add(rule);
	}

	public void setIgnoreDependenciesFilter(Predicate<McpConfigStep> ignoreDependenciesFilter) {
		this.ignoreDependenciesFilter = ignoreDependenciesFilter;
	}

	public SortedSet<String> buildExecutionSet() {
		SortedSet<String> steps = new TreeSet<>(Comparator.comparingInt(stepNames::indexOf));
		Queue<String> queue = new ArrayDeque<>(this.steps);

		while (!queue.isEmpty()) {
			String step = queue.remove();
			McpConfigStep data = allSteps.get(step);
			if (!allSteps.containsKey(step) || skipRules.stream().anyMatch(rule -> rule.test(data))) continue;
			steps.add(step);

			if (!ignoreDependenciesFilter.test(allSteps.get(step))) {
				allSteps.get(step).config().values().forEach(value -> {
					if (value instanceof ConfigValue.Variable var) {
						String name = var.name();

						if (name.endsWith(PREVIOUS_OUTPUT_SUFFIX) && name.length() > PREVIOUS_OUTPUT_SUFFIX.length()) {
							String substep = name.substring(0, name.length() - PREVIOUS_OUTPUT_SUFFIX.length());
							queue.offer(substep);
						}
					}
				});
			}
		}

		return steps;
	}
}
