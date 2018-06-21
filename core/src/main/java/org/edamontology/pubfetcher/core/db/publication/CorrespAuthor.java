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
import java.util.List;
import java.util.stream.Collectors;

public class CorrespAuthor implements Serializable {

	private static final long serialVersionUID = 2986141304432883587L;

	private String name = "";

	private String orcid = "";

	private String email = "";

	private String phone = "";

	private String uri = "";

	public boolean isEmpty() {
		return name.isEmpty() && orcid.isEmpty() && email.isEmpty() && phone.isEmpty() && uri.isEmpty();
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		if (name != null) {
			this.name = name.trim();
		}
	}

	public String getOrcid() {
		return orcid;
	}
	public void setOrcid(String orcid) {
		if (orcid != null) {
			this.orcid = orcid.trim();
		}
	}

	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		if (email != null) {
			this.email = email.trim();
		}
	}

	public String getPhone() {
		return phone;
	}
	public void setPhone(String phone) {
		if (phone != null) {
			this.phone = phone.trim();
		}
	}

	public String getUri() {
		return uri;
	}
	public void setUri(String uri) {
		if (uri != null) {
			this.uri = uri.trim();
		}
	}

	public static String toString(List<CorrespAuthor> correspAuthor) {
		return correspAuthor.stream().map(Object::toString).collect(Collectors.joining("; "));
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (!name.isEmpty()) {
			sb.append(name);
		}
		if (!orcid.isEmpty()) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append(orcid);
		}
		if (!email.isEmpty()) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append(email);
		}
		if (!phone.isEmpty()) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append(phone);
		}
		if (!uri.isEmpty()) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append(uri);
		}
		return sb.toString();
	}
}
