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

package org.edamontology.pubfetcher.core.db.publication;

import java.io.Serializable;

import org.edamontology.pubfetcher.core.common.PubFetcher;

public class MeshTerm implements Serializable {

	private static final long serialVersionUID = 5663855704880035618L;

	private static final String MESHlink = "https://www.ncbi.nlm.nih.gov/mesh/?term=";

	private String term = "";

	private boolean majorTopic = false;

	private String uniqueId = "";

	public MeshTerm() {}

	public String getTerm() {
		return term;
	}
	public void setTerm(String term) {
		if (term != null) {
			this.term = term.trim();
		}
	}

	public boolean isMajorTopic() {
		return majorTopic;
	}
	public void setMajorTopic(boolean majorTopic) {
		this.majorTopic = majorTopic;
	}

	public String getUniqueId() {
		return uniqueId;
	}
	public void setUniqueId(String uniqueId) {
		if (uniqueId != null) {
			this.uniqueId = uniqueId.trim();
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(term);
		return sb.toString();
	}

	public String toStringLink() {
		StringBuilder sb = new StringBuilder();
		if (!uniqueId.isEmpty()) {
			sb.append(MESHlink).append("%22").append(uniqueId).append("%22");
		} else if (!term.isEmpty()) {
			sb.append(MESHlink).append("%22").append(term.replaceAll(" ", "+")).append("%22");
		}
		return sb.toString();
	}

	public String toStringHtml() {
		StringBuilder sb = new StringBuilder();
		sb.append(PubFetcher.getLinkHtml(toStringLink(), (term.isEmpty() ? "NA" : term) + (majorTopic ? "*" : "")));
		return sb.toString();
	}
}
