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

package org.edamontology.pubfetcher.core.db.webpage;

import java.io.IOException;
import java.time.Instant;

import org.edamontology.pubfetcher.core.common.FetcherArgs;
import org.edamontology.pubfetcher.core.common.PubFetcher;
import org.edamontology.pubfetcher.core.db.DatabaseEntry;

import com.fasterxml.jackson.core.JsonGenerator;

public class Webpage extends DatabaseEntry<Webpage> {

	private static final long serialVersionUID = -768032243328952543L;

	private static final String HTTP_STATUS_URL = "https://http.cat/";

	private String startUrl = "";

	private String finalUrl = "";

	private String contentType = "";

	private int statusCode = 0;

	private String title = "";

	private String content = "";

	private long contentTime = 0;

	private String license = "";

	private String language = "";

	public Webpage() {}

	public void overwrite(Webpage webpage) {
		if (!startUrl.equals(webpage.startUrl)) {
			throw new IllegalArgumentException("Webpage start URL " + startUrl + " is not same as overwriting webpage start URL " + webpage.startUrl);
		}
		finalUrl = webpage.finalUrl;
		contentType = webpage.contentType;
		statusCode = webpage.statusCode;
		title = webpage.title;
		content = webpage.content;
		contentTime = webpage.contentTime;
		license = webpage.license;
		language = webpage.language;
		fetchException = webpage.fetchException;
	}

	@Override
	public boolean isEmpty() {
		return title.isEmpty() && content.isEmpty();
	}

	@Override
	public boolean isUsable(FetcherArgs fetcherArgs) {
		return !isBroken() && !isEmpty() && isFinal(fetcherArgs);
	}

	@Override
	public boolean isFinal(FetcherArgs fetcherArgs) {
		return title.length() + content.length() >= fetcherArgs.getWebpageMinLength();
	}

	public boolean isBroken() {
		return ((statusCode < 200 || statusCode >= 300) && (statusCode != 0 || finalUrl.isEmpty()));
	}

	@Override
	public String getStatusString(FetcherArgs fetcherArgs) {
		if (isBroken()) return "broken";
		if (isEmpty()) return "empty";
		if (!isFinal(fetcherArgs)) return "non-final";
		return "final";
	}

	public String getStartUrl() {
		return startUrl;
	}
	public void setStartUrl(String startUrl) {
		if (startUrl != null) {
			this.startUrl = startUrl.trim();
		}
	}

	public String getFinalUrl() {
		return finalUrl;
	}
	public void setFinalUrl(String finalUrl) {
		if (finalUrl != null) {
			this.finalUrl = finalUrl.trim();
		}
	}

	public String getContentType() {
		return contentType;
	}
	public void setContentType(String contentType) {
		if (contentType != null) {
			this.contentType = contentType.trim();
		}
	}

	public int getStatusCode() {
		return statusCode;
	}
	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		if (title != null) {
			this.title = title.trim();
		}
	}

	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		if (content != null) {
			this.content = content.trim();
			this.contentTime = System.currentTimeMillis();
		}
	}

	public long getContentTime() {
		return contentTime;
	}
	public String getContentTimeHuman() {
		return Instant.ofEpochMilli(contentTime).toString();
	}

	public String getLicense() {
		return license;
	}
	public void setLicense(String license) {
		if (license != null) {
			this.license = license.trim();
		}
	}

	public String getLanguage() {
		return language;
	}
	public void setLanguage(String language) {
		if (language != null) {
			this.language = language.trim();
		}
	}

	@Override
	public String toStringId() {
		return startUrl;
	}

	@Override
	public String toStringIdHtml() {
		return PubFetcher.getLinkHtml(startUrl);
	}

	@Override
	public void toStringIdJson(JsonGenerator generator) throws IOException {
		generator.writeString(startUrl);
	}

	@Override
	public String toStringPlain() {
		StringBuilder sb = new StringBuilder();
		sb.append(toStringId()).append("\n\n");
		sb.append(title).append("\n\n");
		sb.append(content);
		return sb.toString();
	}

	@Override
	public String toStringPlainHtml(String prepend) {
		StringBuilder sb = new StringBuilder();
		sb.append(prepend).append("<h2>").append(PubFetcher.getLinkHtml(toStringId(), title)).append("</h2>\n");
		sb.append(prepend).append(PubFetcher.getParagraphsHtml(content));
		return sb.toString();
	}

	@Override
	public void toStringPlainJson(JsonGenerator generator) throws IOException {
		generator.writeStartObject();
		generator.writeStringField("startUrl", toStringId());
		generator.writeStringField("title", title);
		generator.writeStringField("content", content);
		generator.writeEndObject();
	}

	@Override
	public String toStringMetaHtml(String prepend) {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toStringHtml(prepend)).append("\n");
		sb.append(prepend).append("<br>\n");
		sb.append(prepend).append("<div><span>Start URL:</span> <span>").append(PubFetcher.getLinkHtml(startUrl)).append("</span></div>\n");
		sb.append(prepend).append("<div><span>Final URL:</span> <span>").append(PubFetcher.getLinkHtml(finalUrl)).append("</span></div>\n");
		sb.append(prepend).append("<div><span>Content type:</span> <span>").append(PubFetcher.escapeHtml(contentType)).append("</span></div>\n");
		sb.append(prepend).append("<div><span>Status code:</span> <span>");
		if (statusCode > 0) {
			sb.append(PubFetcher.getLinkHtml(HTTP_STATUS_URL, Integer.toString(statusCode)));
		} else {
			sb.append(statusCode);
		}
		sb.append("</span></div>\n");
		sb.append(prepend).append("<div><span>Content time:</span> <span>").append(getContentTimeHuman()).append(" (").append(contentTime).append(")</span></div>\n");
		sb.append(prepend).append("<br>\n");
		sb.append(prepend).append("<div><span>License:</span> <span>").append(license).append("</span></div>\n");
		sb.append(prepend).append("<div><span>Language:</span> <span>").append(language).append("</span></div>\n");
		sb.append(prepend).append("<br>\n");
		sb.append(prepend).append("<div><span>Title length:</span> <span>").append(title.length()).append("</span></div>\n");
		sb.append(prepend).append("<div><span>Content length:</span> <span>").append(content.length()).append("</span></div>\n");
		sb.append(prepend).append("<br>\n");
		sb.append(prepend).append("<div><span>Title:</span> <span>").append(PubFetcher.escapeHtml(title)).append("</span></div>");
		return sb.toString();
	}

	@Override
	public void toStringMetaJson(JsonGenerator generator, FetcherArgs fetcherArgs) throws IOException {
		super.toStringJson(generator);
		generator.writeStringField("startUrl", startUrl);
		generator.writeStringField("finalUrl", finalUrl);
		generator.writeStringField("contentType", contentType);
		generator.writeNumberField("statusCode", statusCode);
		generator.writeNumberField("contentTime", contentTime);
		generator.writeStringField("contentTimeHuman", getContentTimeHuman());
		generator.writeStringField("license", license);
		generator.writeStringField("language", language);
		generator.writeNumberField("titleLength", title.length());
		generator.writeNumberField("contentLength", content.length());
		generator.writeStringField("title", title);
		generator.writeBooleanField("empty", isEmpty());
		generator.writeBooleanField("usable", isUsable(fetcherArgs));
		generator.writeBooleanField("final", isFinal(fetcherArgs));
		generator.writeBooleanField("broken", isBroken());
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString()).append("\n\n");
		sb.append("START URL: ").append(startUrl).append("\n");
		sb.append("FINAL URL: ").append(finalUrl).append("\n");
		sb.append("CONTENT TYPE: ").append(contentType).append("\n");
		sb.append("STATUS CODE: ").append(statusCode).append("\n");
		sb.append("CONTENT TIME: ").append(getContentTimeHuman()).append(" (").append(contentTime).append(")\n\n");
		sb.append("LICENSE: ").append(license).append("\n");
		sb.append("LANGUAGE: ").append(language).append("\n\n");
		sb.append("TITLE LENGTH: ").append(title.length()).append("\n");
		sb.append("CONTENT LENGTH: ").append(content.length()).append("\n\n");
		sb.append("TITLE: ").append(title).append("\n\n");
		sb.append("CONTENT: ").append(content);
		return sb.toString();
	}

	@Override
	public String toStringHtml(String prepend) {
		StringBuilder sb = new StringBuilder();
		sb.append(prepend).append("<h2>Webpage</h2>\n");
		sb.append(toStringMetaHtml(prepend)).append("\n");
		sb.append(prepend).append("<br>\n");
		sb.append(prepend).append(PubFetcher.getParagraphsHtml(content));
		return sb.toString();
	}

	public void toStringJson(JsonGenerator generator, FetcherArgs fetcherArgs, boolean webpageContent) throws IOException {
		generator.writeStartObject();
		toStringMetaJson(generator, fetcherArgs);
		if (webpageContent) {
			generator.writeStringField("content", content);
		}
		generator.writeEndObject();
	}

	@Override
	public int compareTo(Webpage o) {
		if (o == null) return 1;
		return startUrl.compareTo(o.startUrl);
	}
}
