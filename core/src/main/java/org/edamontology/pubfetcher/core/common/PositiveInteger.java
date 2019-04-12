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

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

/*
 * A copy of com.beust.jcommander.validators.PositiveInteger.
 * To keep the behaviour that 0 is a positive integer.
 * https://github.com/cbeust/jcommander/issues/438
 */
public class PositiveInteger implements IParameterValidator {
	public void validate(String name, String value) throws ParameterException {
		int n = Integer.parseInt(value);
		if (n < 0) {
			throw new ParameterException("Parameter " + name + " should be positive (found " + value + ")");
		}
	}
}
