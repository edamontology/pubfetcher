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
import java.util.Locale;

public abstract class PublicationPart implements Serializable {

	private static final long serialVersionUID = -6967157557987158221L;

	private PublicationPartName name;

	private PublicationPartType type;

	private String url;

	private long timestamp;

	protected PublicationPart(PublicationPartName name) {
		if (name == null) {
			throw new IllegalArgumentException("Publication part name can't be null");
		}
		this.name = name;
		set(null, null);
	}

	protected void set(PublicationPartType type, String url) {
		if (type == null) {
			this.type = PublicationPartType.na;
		} else {
			this.type = type;
		}
		if (url == null) {
			this.url = "";
		} else {
			this.url = url.trim();
		}
		this.timestamp = System.currentTimeMillis();
	}

	public PublicationPartName getName() {
		return name;
	}

	public PublicationPartType getType() {
		return type;
	}

	public String getUrl() {
		return url;
	}

	public long getTimestamp() {
		return timestamp;
	}
	public String getTimestampHuman() {
		return Instant.ofEpochMilli(timestamp).toString();
	}

	public abstract int getSize();

	public abstract boolean isEmpty();

	public abstract String toStringPlain();

	public abstract String toStringPlainHtml();

	public String toStringHtml() {
		StringBuilder sb = new StringBuilder();
		sb.append("<dl>\n");
		sb.append("<dt>").append(name.getName().toUpperCase(Locale.ROOT)).append("</dt>\n");
		sb.append("<dd>\n");
		sb.append(toStringPlainHtml()).append("\n");
		sb.append("<dl>\n");
		sb.append("<dt>type</dt><dd>").append(type.getType()).append("</dd>\n");
		sb.append("<dt>url</dt><dd><a href=\"").append(url).append("\">").append(url).append("</a></dd>\n");
		sb.append("<dt>timestamp</dt><dd>").append(timestamp).append(" ").append(getTimestampHuman()).append("</dd>\n");
		sb.append("<dt>size</dt><dd>").append(getSize()).append("</dd>\n");
		sb.append("</dl>\n");
		sb.append("</dd>\n");
		sb.append("</dl>");
		return sb.toString();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(name.getName().toUpperCase(Locale.ROOT)).append(": ");
		sb.append(toStringPlain()).append("\n");
		sb.append("    type: ").append(type.getType()).append("\n");
		sb.append("    url: ").append(url).append("\n");
		sb.append("    timestamp: ").append(timestamp).append(" ").append(getTimestampHuman()).append("\n");
		sb.append("    size: ").append(getSize());
		return sb.toString();
	}
}
