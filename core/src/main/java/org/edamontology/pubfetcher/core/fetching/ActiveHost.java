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

package org.edamontology.pubfetcher.core.fetching;

public class ActiveHost {

	private final String host;

	private int count;

	ActiveHost(String host) {
		this.host = host;
		count = 1;
	}

	String getHost() {
		return host;
	}

	int getCount() {
		return count;
	}

	void increment() {
		++count;
	}

	void decrement() {
		if (count > 0) {
			--count;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof ActiveHost)) return false;
		ActiveHost other = (ActiveHost) obj;
		if (host == null) {
			if (other.host != null) return false;
		} else if (!host.equals(other.host)) return false;
		return other.canEqual(this);
	}

	@Override
	public int hashCode() {
		if (host == null) return 0;
		return host.hashCode();
	}

	public boolean canEqual(Object other) {
		return (other instanceof ActiveHost);
	}
}
