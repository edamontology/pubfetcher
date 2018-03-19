/*
 * Copyright © 2016, 2018 Erik Jaaniso
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MinedTerm implements Serializable {

	private static final long serialVersionUID = 4674690901850332360L;

	private static final String EFOlink = "https://www.ebi.ac.uk/efo/";
	private static final String GOlink = "http://amigo.geneontology.org/amigo/term/GO:";

	private String term = "";

	private int count = 0;

	private List<String> altNames = new ArrayList<>();

	private String dbName = "";

	private List<String> dbIds = new ArrayList<>();

	MinedTerm() {}

	public String getTerm() {
		return term;
	}
	void setTerm(String term) {
		if (term != null) {
			this.term = term.trim();
		}
	}

	public int getCount() {
		return count;
	}
	void setCount(int count) {
		this.count = count;
	}

	public List<String> getAltNames() {
		return altNames;
	}
	void setAltNames(List<String> altNames) {
		if (altNames != null) {
			this.altNames = altNames.stream()
				.filter(k -> k != null)
				.map(k -> k.trim())
				.filter(k -> !k.isEmpty())
				.collect(Collectors.toList());
		}
	}

	public String getDbName() {
		return dbName;
	}
	void setDbName(String dbName) {
		if (dbName != null) {
			this.dbName = dbName.trim();
		}
	}

	public List<String> getDbIds() {
		return dbIds;
	}
	void setDbIds(List<String> dbIds) {
		if (dbIds != null) {
			this.dbIds = dbIds.stream()
				.filter(k -> k != null)
				.map(k -> k.trim())
				.filter(k -> !k.isEmpty())
				.collect(Collectors.toList());
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
		StringBuilder sb = new StringBuilder();
		sb.append(term);
		if (!altNames.isEmpty()) {
			sb.append(" (");
			sb.append(String.join("; ", altNames));
			sb.append(")");
		}
		return sb.toString();
	}

	public String toStringLink() {
		StringBuilder sb = new StringBuilder();
		if (!dbIds.isEmpty()) {
			if (dbName.equalsIgnoreCase("efo")) {
				sb.append(EFOlink).append(dbIds.get(0));
			} else if (dbName.equalsIgnoreCase("GO")) {
				sb.append(GOlink).append(dbIds.get(0));
			}
		}
		return sb.toString();
	}

	public String toStringHtml() {
		StringBuilder sb = new StringBuilder();
		sb.append(FetcherCommon.getLinkHtml(toStringLink(), term.isEmpty() ? "NA" : term));
		if (count != 0) sb.append(" <small>").append(count).append("</small>");
		return sb.toString();
	}
}
