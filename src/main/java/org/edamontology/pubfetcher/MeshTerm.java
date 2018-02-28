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

public class MeshTerm implements Serializable {

	private static final long serialVersionUID = 5663855704880035618L;

	private String term = "";

	private boolean majorTopic = false;

	private String uniqueId = "";

	MeshTerm() {}

	public String getTerm() {
		return term;
	}
	void setTerm(String term) {
		if (term != null) {
			this.term = term.trim();
		}
	}

	public boolean isMajorTopic() {
		return majorTopic;
	}
	void setMajorTopic(boolean majorTopic) {
		this.majorTopic = majorTopic;
	}

	public String getUniqueId() {
		return uniqueId;
	}
	void setUniqueId(String uniqueId) {
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
}
