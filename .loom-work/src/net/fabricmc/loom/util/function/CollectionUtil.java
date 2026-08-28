/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2020-2022 FabricMC
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

package net.fabricmc.loom.util.function;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Stream-like utilities for working with collections.
 *
 * @author Juuz
 */
public final class CollectionUtil {
	/**
	 * Finds the first element matching the predicate.
	 *
	 * @param collection the collection to be searched
	 * @param filter     the predicate to be matched
	 * @param <E>        the element type
	 * @return the first matching element, or empty if none match
	 */
	public static <E> Optional<E> find(Iterable<? extends E> collection, Predicate<? super E> filter) {
		for (E e : collection) {
			if (filter.test(e)) {
				return Optional.of(e);
			}
		}

		return Optional.empty();
	}

	/**
	 * Finds the index of the first element matching the predicate.
	 *
	 * @param list   the list to be searched
	 * @param filter the predicate to be matched
	 * @param <E>    the element type
	 * @return the index of the first matching element, or -1 if none match
	 */
	public static <E> int indexOf(List<? extends E> list, Predicate<? super E> filter) {
		for (int i = 0; i < list.size(); i++) {
			if (filter.test(list.get(i))) {
				return i;
			}
		}

		return -1;
	}

	/**
	 * Transforms the collection with a function.
	 *
	 * @param collection the source collection
	 * @param transform  the transformation function
	 * @param <A> the source type
	 * @param <B> the target type
	 * @return a mutable list with the transformed entries
	 */
	public static <A, B> List<B> map(Iterable<? extends A> collection, Function<? super A, ? extends B> transform) {
		return mapTo(collection, new ArrayList<>(), transform);
	}

	/**
	 * Transforms the collection with a function, storing the results in a target collection.
	 *
	 * @param collection the source collection
	 * @param target     the target collection
	 * @param transform  the transformation function
	 * @param <A> the source type
	 * @param <B> the target type
	 * @param <C> the target collection type
	 * @return the target collection
	 */
	public static <A, B, C extends Collection<B>> C mapTo(Iterable<? extends A> collection, C target, Function<? super A, ? extends B> transform) {
		for (A a : collection) {
			target.add(transform.apply(a));
		}

		return target;
	}

	/**
	 * Filters a list based on a predicate.
	 *
	 * @param collection the source collection
	 * @param filter     the filtering predicate
	 * @param <A> the element type
	 * @return a mutable list with the filtered entries
	 */
	public static <A> List<A> filter(Collection<? extends A> collection, Predicate<? super A> filter) {
		ArrayList<A> result = new ArrayList<>(collection);
		result.removeIf(filter.negate());
		return result;
	}

	/**
	 * Gets the single element of an iterable.
	 *
	 * @param iterable the iterable
	 * @param <A> the element type
	 * @return the single element, or empty if there are no elements or multiple elements
	 */
	public static <A> Optional<A> single(Iterable<? extends A> iterable) {
		final Iterator<? extends A> iter = iterable.iterator();

		// No elements
		if (!iter.hasNext()) {
			return Optional.empty();
		}

		A single = iter.next();

		// Too many elements
		if (iter.hasNext()) {
			return Optional.empty();
		}

		return Optional.of(single);
	}

	/**
	 * Gets the first element of an iterable.
	 *
	 * @param iterable the iterable
	 * @param <A> the element type
	 * @return the first element, or empty if there are no elements
	 */
	public static <A> Optional<A> first(Iterable<? extends A> iterable) {
		final Iterator<? extends A> iter = iterable.iterator();

		// No elements
		if (!iter.hasNext()) {
			return Optional.empty();
		}

		return Optional.of(iter.next());
	}
}
