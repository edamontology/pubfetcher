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

	public abstract boolean isUsable(FetcherArgs fetcherArgs);

	protected boolean canFetch(FetcherArgs fetcherArgs) {
		long currentTime = System.currentTimeMillis();
		String finalness = (isEmpty() ? "empty" : (!isFinal(fetcherArgs) ? "non-final" : "final"));
		boolean canFetch = false;
		if (fetchTime == 0) {
			System.out.println("    can fetch " + finalness + " entry: first fetch");
			canFetch = true;
		} else if (isEmpty() && currentTime > fetchTime + fetcherArgs.getEmptyCooldown() * 60 * 1000) {
			System.out.println("    can fetch " + finalness + " entry: more than " + fetcherArgs.getEmptyCooldown() + " min since " + getFetchTimeHuman());
			canFetch = true;
		} else if (!isFinal(fetcherArgs) && !isEmpty() && currentTime > fetchTime + fetcherArgs.getNonFinalCooldown() * 60 * 1000) {
			System.out.println("    can fetch " + finalness + " entry: more than " + fetcherArgs.getNonFinalCooldown() + " min since " + getFetchTimeHuman());
			canFetch = true;
		} else if (fetchException && currentTime > fetchTime + fetcherArgs.getFetchExceptionCooldown() * 60 * 1000) {
			System.out.println("    can fetch " + finalness + " entry with fetching exception: more than " + fetcherArgs.getFetchExceptionCooldown() + " min since " + getFetchTimeHuman());
			canFetch = true;
		} else if ((isEmpty() || !isFinal(fetcherArgs) || fetchException) && (retryCounter < fetcherArgs.getRetryLimit() || fetcherArgs.getRetryLimit() < 0)) {
			System.out.println("    can fetch " + finalness + " entry: retry count " + retryCounter + " has not reached limit " + fetcherArgs.getRetryLimit());
			canFetch = true;
		}
		if (canFetch) {
			return true;
		} else {
			System.out.println("    can not fetch " + finalness + " entry " + (fetchException ? "with fetching exception " : "") + "from " + fetchTime + " with retry count " + retryCounter);
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

	public abstract String toStringPlainHtml(String prepend);

	public abstract String toStringMetaHtml(String prepend);

	public String toStringHtml(String prepend) {
		StringBuilder sb = new StringBuilder();
		sb.append(prepend).append("<div><span>Fetch time:</span> <span>").append(getFetchTimeHuman()).append(" (").append(fetchTime).append(")</span></div>\n");
		sb.append(prepend).append("<div><span>Retry counter:</span> <span>").append(retryCounter).append("</span></div>");
		return sb.toString();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("FETCH TIME: ").append(getFetchTimeHuman()).append(" (").append(fetchTime).append(")\n");
		sb.append("RETRY COUNTER: ").append(retryCounter);
		return sb.toString();
	}
}
