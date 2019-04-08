/*
 * Copyright © 2019 Erik Jaaniso
 *
 * This file is part of PubFetcher.
 *
 * PubFetcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PubFetcher is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PubFetcher.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.edamontology.pubfetcher.core.common;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class Arg<T, E extends Enum<E>> {

	private final Supplier<T> getValue;

	private final Consumer<T> setValue;

	private final T defaultValue;

	private final T min;

	private final T max;

	private final String id;

	private final String label;

	private final String description;

	private final Class<E> enumClass;

	private final String url;

	public Arg(Supplier<T> getValue, Consumer<T> setValue, T defaultValue, T min, T max, String id, String label, String description, Class<E> enumClass, String url) {
		this.getValue = getValue;
		this.setValue = setValue;
		this.defaultValue = defaultValue;
		this.min = min;
		this.max = max;
		this.id = id;
		this.label = label;
		this.description = description;
		this.enumClass = enumClass;
		this.url = url;
	}
	public Arg(Supplier<T> getValue, Consumer<T> setValue, T defaultValue, T min, T max, String id, String label, String description, Class<E> enumClass) {
		this(getValue, setValue, defaultValue, min, max, id, label, description, enumClass, null);
	}
	public Arg(Supplier<T> getValue, Consumer<T> setValue, T defaultValue, String id, String label, String description, Class<E> enumClass, String url) {
		this(getValue, setValue, defaultValue, null, null, id, label, description, enumClass, url);
	}
	public Arg(Supplier<T> getValue, Consumer<T> setValue, T defaultValue, String id, String label, String description, Class<E> enumClass) {
		this(getValue, setValue, defaultValue, null, null, id, label, description, enumClass, null);
	}

	public T getValue() {
		return getValue.get();
	}
	public void setValue(T value) {
		setValue.accept(value);
	}

	public T getDefault() {
		return defaultValue;
	}

	public T getMin() {
		return min;
	}

	public T getMax() {
		return max;
	}

	public String getId() {
		return id;
	}

	public String getLabel() {
		return label;
	}

	public String getDescription() {
		return description;
	}

	public Class<E> getEnumClass() {
		return enumClass;
	}

	public String getUrl() {
		return url;
	}
}
