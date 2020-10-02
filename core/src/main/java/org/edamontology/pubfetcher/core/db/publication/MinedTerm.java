/*
 * Copyright © 2016, 2018, 2020 Erik Jaaniso
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

package org.edamontology.pubfetcher.core.db.publication;

import java.io.Serializable;

import org.edamontology.pubfetcher.core.common.PubFetcher;

public class MinedTerm implements Serializable, Comparable<MinedTerm> {

	private static final long serialVersionUID = 245681198110119069L;

	private String term = "";

	private int count = 0;

	private String uri = "";

	public MinedTerm() {}

	public String getTerm() {
		return term;
	}
	public void setTerm(String term) {
		if (term != null) {
			this.term = term.trim();
		}
	}

	public int getCount() {
		return count;
	}
	public void setCount(int count) {
		this.count = count;
	}

	public String getUri() {
		return uri;
	}
	public void setUri(String uri) {
		if (uri != null) {
			this.uri = uri.trim();
		}
	}

	public double getFrequency(int fulltextWordCount) {
		double frequency = count;
		if (fulltextWordCount > 0) {
			frequency /= (double)fulltextWordCount;
		} else {
			frequency = 0;
		}
		if (frequency < 0) {
			frequency = 0;
		} else if (frequency > 1) {
			frequency = 1;
		}
		return frequency;
	}

	@Override
	public String toString() {
		return term;
	}

	public String toStringHtml() {
		StringBuilder sb = new StringBuilder();
		sb.append(PubFetcher.getLinkHtml(uri, term.isEmpty() ? "NA" : term));
		if (count != 0) sb.append(" <small>").append(count).append("</small>");
		return sb.toString();
	}

	@Override
	public int compareTo(MinedTerm o) {
		return o.count - count;
	}
}
