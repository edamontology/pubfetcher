/*
 * Copyright © 2018 Erik Jaaniso
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.MissingResourceException;
import java.util.Properties;

public class Version {

	private final String name;

	private final String url;

	private final String version;

	public Version(Class<?> clazz) throws IOException {
		InputStream resource = clazz.getResourceAsStream("/project.properties");
		if (resource != null) {
			try (BufferedReader br = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8))) {
				Properties properties = new Properties();
				properties.load(br);
				name = properties.getProperty("name");
				url = properties.getProperty("url");
				version = properties.getProperty("version");
			}
		} else {
			throw new MissingResourceException("Can't find project properties", clazz.getSimpleName(), "project.properties");
		}
		InputStream resourceSystem = clazz.getResourceAsStream("/system.properties");
		if (resourceSystem != null) {
			try (BufferedReader br = new BufferedReader(new InputStreamReader(resourceSystem, StandardCharsets.UTF_8))) {
				Properties properties = new Properties(System.getProperties());
				properties.load(br);
				System.setProperties(properties);
			}
		} else {
			throw new MissingResourceException("Can't find system properties", clazz.getSimpleName(), "system.properties");
		}
	}

	public String getName() {
		return name;
	}

	public String getUrl() {
		return url;
	}

	public String getVersion() {
		return version;
	}
}
