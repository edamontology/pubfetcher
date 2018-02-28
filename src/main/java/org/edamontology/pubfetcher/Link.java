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
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;

public class Link implements Serializable {

	private static final long serialVersionUID = 557410938898808162L;

	private URL url;

	private PublicationPartType type;

	private String from;

	private long timestamp;

	Link(String url, PublicationPartType type, String from) throws MalformedURLException {
		if (url == null) {
			throw new MalformedURLException("URL is null");
		} else {
			this.url = new URL(url.trim());
		}
		if (type == null) {
			this.type = PublicationPartType.na;
		} else {
			this.type = type;
		}
		if (from == null) {
			this.from = "";
		} else {
			this.from = from.trim();
		}
		this.timestamp = System.currentTimeMillis();
	}

	public URL getUrl() {
		return url;
	}

	public PublicationPartType getType() {
		return type;
	}

	public String getFrom() {
		return from;
	}

	public long getTimestamp() {
		return timestamp;
	}
	public String getTimestampHuman() {
		return Instant.ofEpochMilli(timestamp).toString();
	}

	private String getNormalUrl(URL url) {
		return url.getAuthority() + url.getFile(); // host + port + path + query
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Link)) return false;
		Link other = (Link) obj;
		if (url == null) {
			if (other.url != null) return false;
		} else if (other.url == null) {
			return false;
		} else if (!getNormalUrl(url).equals(getNormalUrl(other.url))) {
			return false;
		}
		return other.canEqual(this);
	}

	@Override
	public int hashCode() {
		if (url == null) return 0;
		return getNormalUrl(url).hashCode();
	}

	public boolean canEqual(Object other) {
		return (other instanceof Link);
	}

	public String toStringHtml() {
		StringBuilder sb = new StringBuilder();
		sb.append("<a href=\"").append(url).append("\">").append(url).append("</a>\n");
		sb.append("<dl>\n");
		sb.append("<dt>type</dt><dd>").append(type).append("</dd>\n");
		sb.append("<dt>from</dt><dd><a href=\"").append(from).append("\">").append(from).append("</dd>\n");
		sb.append("<dt>timestamp</dt><dd>").append(timestamp).append(" ").append(getTimestampHuman()).append("</dd>\n");
		sb.append("</dl>");
		return sb.toString();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(url).append("\n");
		sb.append("    type: ").append(type).append("\n");
		sb.append("    from: ").append(from).append("\n");
		sb.append("    timestamp: ").append(timestamp).append(" ").append(getTimestampHuman());
		return sb.toString();
	}
}
