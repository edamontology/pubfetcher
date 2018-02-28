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

package org.edamontology.pubfetcher;

import java.time.Instant;
import java.time.format.DateTimeParseException;

import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.BaseConverter;

public class ISO8601Converter extends BaseConverter<Long> {

	public ISO8601Converter(String optionName) {
		super(optionName);
	}

	public Long convert(String value) {
		try {
			return Long.valueOf(Instant.parse(value).toEpochMilli());
		} catch (DateTimeParseException e) {
			throw new ParameterException(getErrorString(value, "an ISO-8601 time"));
		}
	}
}
