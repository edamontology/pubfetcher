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

import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;
import java.util.Locale;

import org.edamontology.pubfetcher.core.common.FetcherArgs;
import org.edamontology.pubfetcher.core.common.PubFetcher;

import com.fasterxml.jackson.core.JsonGenerator;

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

	public abstract boolean isUsable(FetcherArgs fetcherArgs);

	public boolean isFinal(FetcherArgs fetcherArgs) {
		return type.isFinal() && isUsable(fetcherArgs);
	}

	public String getStatusString(FetcherArgs fetcherArgs) {
		if (isEmpty() && !isUsable(fetcherArgs)) return "empty";
		if (!isUsable(fetcherArgs)) return "non-usable";
		if (!isFinal(fetcherArgs)) return "non-final";
		return "final";
	}

	public abstract String toStringPlain();

	public abstract String toStringPlainHtml();

	public abstract void toStringPlainJson(JsonGenerator generator, boolean withName) throws IOException;

	public String toStringMetaHtml(String prepend) {
		StringBuilder sb = new StringBuilder();
		sb.append(prepend).append("<div><span>Type:</span> <span>").append(type.getType()).append("</span></div>\n");
		sb.append(prepend).append("<div><span>URL:</span> <span>").append(PubFetcher.getLinkHtml(url)).append("</span></div>\n");
		sb.append(prepend).append("<div><span>Timestamp:</span> <span>").append(getTimestampHuman()).append(" (").append(timestamp).append(")</span></div>\n");
		sb.append(prepend).append("<div><span>Size:</span> <span>").append(getSize()).append("</span></div>");
		return sb.toString();
	}

	public void toStringMetaJson(JsonGenerator generator, FetcherArgs fetcherArgs) throws IOException {
		generator.writeStringField("type", type.name());
		generator.writeStringField("url", url);
		generator.writeNumberField("timestamp", timestamp);
		generator.writeStringField("timestampHuman", getTimestampHuman());
		generator.writeNumberField("size", getSize());
		generator.writeBooleanField("empty", isEmpty());
		generator.writeBooleanField("usable", isUsable(fetcherArgs));
		generator.writeBooleanField("final", isFinal(fetcherArgs));
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(name.getName().toUpperCase(Locale.ROOT)).append(": ");
		sb.append(toStringPlain()).append("\n");
		sb.append("    type: ").append(type.getType()).append("\n");
		sb.append("    url: ").append(url).append("\n");
		sb.append("    timestamp: ").append(getTimestampHuman()).append(" (").append(timestamp).append(")\n");
		sb.append("    size: ").append(getSize());
		return sb.toString();
	}

	public String toStringHtml(String prepend) {
		StringBuilder sb = new StringBuilder();
		sb.append(prepend).append("<h3>").append(name.getName()).append("</h3>\n");
		sb.append(prepend).append(toStringPlainHtml()).append("\n");
		sb.append(toStringMetaHtml(prepend));
		return sb.toString();
	}

	public void toStringJson(JsonGenerator generator, FetcherArgs fetcherArgs) throws IOException {
		toStringJson(generator, fetcherArgs, true, name.name());
	}
	public void toStringJson(JsonGenerator generator, FetcherArgs fetcherArgs, boolean includeContent, String name) throws IOException {
		generator.writeFieldName(name);
		generator.writeStartObject();
		if (includeContent) {
			toStringPlainJson(generator, false);
		}
		toStringMetaJson(generator, fetcherArgs);
		generator.writeEndObject();
	}
}
