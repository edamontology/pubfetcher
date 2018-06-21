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

package org.edamontology.pubfetcher.core.db.publication;

import java.io.Serializable;

import org.edamontology.pubfetcher.core.common.PubFetcher;

public class PublicationIds implements Serializable, Comparable<PublicationIds> {

	private static final long serialVersionUID = 8402524782038529149L;

	private final String pmid;

	private final String pmcid;

	private final String doi;

	private final String pmidUrl;

	private final String pmcidUrl;

	private final String doiUrl;

	public PublicationIds(String pmid, String pmcid, String doi, String pmidUrl, String pmcidUrl, String doiUrl) {
		if (pmid == null) {
			this.pmid = "";
		} else {
			this.pmid = pmid.trim();
		}
		if (pmcid == null) {
			this.pmcid = "";
		} else {
			this.pmcid = pmcid.trim();
		}
		if (doi == null) {
			this.doi = "";
		} else {
			doi = doi.trim();
			if (PubFetcher.isDoi(doi)) {
				this.doi = PubFetcher.normalizeDoi(doi);
			} else {
				this.doi = doi;
			}
		}
		if (pmidUrl == null || this.pmid.isEmpty()) {
			this.pmidUrl = "";
		} else {
			this.pmidUrl = pmidUrl.trim();
		}
		if (pmcidUrl == null || this.pmcid.isEmpty()) {
			this.pmcidUrl = "";
		} else {
			this.pmcidUrl = pmcidUrl.trim();
		}
		if (doiUrl == null || this.doi.isEmpty()) {
			this.doiUrl = "";
		} else {
			this.doiUrl = doiUrl.trim();
		}
	}

	public String getPmid() {
		return pmid;
	}

	public String getPmcid() {
		return pmcid;
	}

	public String getDoi() {
		return doi;
	}

	public String getPmidUrl() {
		return pmidUrl;
	}

	public String getPmcidUrl() {
		return pmcidUrl;
	}

	public String getDoiUrl() {
		return doiUrl;
	}

	public boolean isEmpty() {
		return (pmid.isEmpty() && pmcid.isEmpty() && doi.isEmpty());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof PublicationIds)) return false;
		PublicationIds other = (PublicationIds) obj;
		if (pmid == null) {
			if (other.pmid != null) return false;
		} else if (!pmid.equals(other.pmid)) return false;
		if (pmcid == null) {
			if (other.pmcid != null) return false;
		} else if (!pmcid.equals(other.pmcid)) return false;
		if (doi == null) {
			if (other.doi != null) return false;
		} else if (!doi.equals(other.doi)) return false;
		return other.canEqual(this);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((pmid == null) ? 0 : pmid.hashCode());
		result = prime * result + ((pmcid == null) ? 0 : pmcid.hashCode());
		result = prime * result + ((doi == null) ? 0 : doi.hashCode());
		return result;
	}

	public boolean canEqual(Object other) {
		return (other instanceof PublicationIds);
	}

	public static String toStringHtml(String pmid, String pmcid, String doi, boolean tab) {
		StringBuilder sb = new StringBuilder();
		if (tab) sb.append("<tr><td>");
		String pmidLink = PubFetcher.getPmidLinkHtml(pmid);
		if (pmidLink != null && !pmidLink.isEmpty()) {
			sb.append(pmidLink);
		}
		if (tab) sb.append("</td><td>");
		String pmcidLink = PubFetcher.getPmcidLinkHtml(pmcid);
		if (pmcidLink != null && !pmcidLink.isEmpty()) {
			if (sb.length() > 0 && !tab) sb.append(", ");
			sb.append(pmcidLink);
		}
		if (tab) sb.append("</td><td>");
		String doiLink = PubFetcher.getDoiLinkHtml(doi);
		if (doiLink != null && !doiLink.isEmpty()) {
			if (sb.length() > 0 && !tab) sb.append(", ");
			sb.append(doiLink);
		}
		if (tab) sb.append("</td></tr>");
		return sb.toString();
	}

	public static String toString(String pmid, String pmcid, String doi, boolean tab) {
		StringBuilder sb = new StringBuilder();
		if (pmid != null && !pmid.isEmpty()) {
			sb.append(pmid);
		}
		if (tab) sb.append("\t");
		if (pmcid != null && !pmcid.isEmpty()) {
			if (sb.length() > 0 && !tab) sb.append(", ");
			sb.append(pmcid);
		}
		if (tab) sb.append("\t");
		if (doi != null && !doi.isEmpty()) {
			if (sb.length() > 0 && !tab) sb.append(", ");
			sb.append(doi);
		}
		return sb.toString();
	}

	public String toStringHtml() {
		return toStringHtml(pmid, pmcid, doi, false);
	}
	public String toStringHtml(boolean tab) {
		return toStringHtml(pmid, pmcid, doi, tab);
	}

	@Override
	public String toString() {
		return "[" + toString(pmid, pmcid, doi, false) + "]";
	}
	public String toString(boolean tab) {
		if (tab) return toString(pmid, pmcid, doi, true);
		else return "[" + toString(pmid, pmcid, doi, false) + "]";
	}

	public String toStringWithUrlHtml() {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		if (!pmid.isEmpty()) {
			sb.append(PubFetcher.getLinkHtml(pmidUrl));
		}
		if (!pmcid.isEmpty()) {
			if (sb.length() > 1) sb.append(", ");
			sb.append(PubFetcher.getLinkHtml(pmcidUrl));
		}
		if (!doi.isEmpty()) {
			if (sb.length() > 1) sb.append(", ");
			sb.append(PubFetcher.getLinkHtml(doiUrl));
		}
		sb.append(")");
		return toStringHtml() + " " + sb.toString();
	}

	public String toStringWithUrl() {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		if (!pmid.isEmpty()) {
			sb.append(pmidUrl);
		}
		if (!pmcid.isEmpty()) {
			if (sb.length() > 1) sb.append(", ");
			sb.append(pmcidUrl);
		}
		if (!doi.isEmpty()) {
			if (sb.length() > 1) sb.append(", ");
			sb.append(doiUrl);
		}
		sb.append(")");
		return "[" + toString(pmid, pmcid, doi, false) + "] " + sb.toString();
	}

	public static int compareTo(String pmid, String pmcid, String doi, String oPmid, String oPmcid, String oDoi) {
		if (!pmid.isEmpty()) {
			if (oPmid.isEmpty()) return 1;
			else return pmid.compareTo(oPmid);
		} else if (!pmcid.isEmpty()) {
			if (!oPmid.isEmpty()) return -1;
			else if (oPmcid.isEmpty()) return 1;
			else return pmcid.compareTo(oPmcid);
		} else if (!doi.isEmpty()) {
			if (!oPmid.isEmpty() || !oPmcid.isEmpty()) return -1;
			else if (oDoi.isEmpty()) return 1;
			else return doi.compareTo(oDoi);
		} else {
			if (!oPmid.isEmpty() || !oPmcid.isEmpty() || !oDoi.isEmpty()) return -1;
			else return 0;
		}
	}

	@Override
	public int compareTo(PublicationIds o) {
		if (o == null) return 1;
		return compareTo(pmid, pmcid, doi, o.pmid, o.pmcid, o.doi);
	}
}
