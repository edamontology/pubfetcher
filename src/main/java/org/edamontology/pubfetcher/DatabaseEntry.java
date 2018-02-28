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

import java.io.Serializable;
import java.time.Instant;

public abstract class DatabaseEntry<T> implements Serializable, Comparable<T> {

	private static final long serialVersionUID = -2280508730664091054L;

	private long fetchTime = 0;

	private int retryCounter = 0;

	protected boolean fetchException = false;

	public abstract boolean isEmpty();

	public abstract boolean isFinal(FetcherArgs fetcherArgs);

	protected boolean canFetch(FetcherArgs fetcherArgs) {
		long currentTime = System.currentTimeMillis();
		if (fetchTime == 0
			|| (isEmpty() && currentTime > fetchTime + fetcherArgs.getEmptyCooldown() * 60 * 1000)
			|| (!isFinal(fetcherArgs) && !isEmpty() && currentTime > fetchTime + fetcherArgs.getNonFinalCooldown() * 60 * 1000)
			|| (fetchException && currentTime > fetchTime + fetcherArgs.getFetchExceptionCooldown() * 60 * 1000)
			|| ((isEmpty() || !isFinal(fetcherArgs) || fetchException) && (retryCounter < fetcherArgs.getRetryLimit() || fetcherArgs.getRetryLimit() < 0))) {
			return true;
		} else {
			return false;
		}
	}

	protected boolean updateCounters(FetcherArgs fetcherArgs) {
		long currentTime = System.currentTimeMillis();
		if (fetchTime == 0
			|| (isEmpty() && currentTime > fetchTime + fetcherArgs.getEmptyCooldown() * 60 * 1000)
			|| (!isFinal(fetcherArgs) && !isEmpty() && currentTime > fetchTime + fetcherArgs.getNonFinalCooldown() * 60 * 1000)
			|| (fetchException && currentTime > fetchTime + fetcherArgs.getFetchExceptionCooldown() * 60 * 1000)) {
			fetchTime = currentTime;
			retryCounter = 0;
			return true;
		} else if ((isEmpty() || !isFinal(fetcherArgs) || fetchException) && (retryCounter < fetcherArgs.getRetryLimit() || fetcherArgs.getRetryLimit() < 0)) {
			++retryCounter;
			return true;
		} else {
			return false;
		}
	}

	public long getFetchTime() {
		return fetchTime;
	}
	public String getFetchTimeHuman() {
		return Instant.ofEpochMilli(fetchTime).toString();
	}

	public int getRetryCounter() {
		return retryCounter;
	}

	public boolean isFetchException() {
		return fetchException;
	}
	public void setFetchException(boolean fetchException) {
		this.fetchException = fetchException;
	}

	public abstract String toStringId();

	public abstract String toStringIdHtml();

	public abstract String toStringPlain();

	public abstract String toStringPlainHtml();

	public String toStringHtml() {
		StringBuilder sb = new StringBuilder();
		sb.append("<dt>FETCH TIME</dt>\n").append("<dd>").append(fetchTime).append(" ").append(getFetchTimeHuman()).append("</dd>\n");
		sb.append("<dt>RETRY COUNTER</dt>\n").append("<dd>").append(retryCounter).append("</dd>\n");
		return sb.toString();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("FETCH TIME: ").append(fetchTime).append(" ").append(getFetchTimeHuman()).append("\n");
		sb.append("RETRY COUNTER: ").append(retryCounter);
		return sb.toString();
	}
}
