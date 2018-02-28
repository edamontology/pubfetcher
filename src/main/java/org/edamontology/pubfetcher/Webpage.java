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

import java.time.Instant;

public class Webpage extends DatabaseEntry<Webpage> {

	private static final long serialVersionUID = -768032243328952543L;

	private String startUrl = "";

	private String finalUrl = "";

	private String contentType = "";

	private int statusCode = 0;

	private String title = "";

	private String content = "";

	private long contentTime = 0;

	Webpage() {}

	void overwrite(Webpage webpage) {
		if (!startUrl.equals(webpage.startUrl)) {
			throw new IllegalArgumentException("Webpage start URL " + startUrl + " is not same as overwriting webpage start URL " + webpage.startUrl);
		}
		finalUrl = webpage.finalUrl;
		contentType = webpage.contentType;
		statusCode = webpage.statusCode;
		title = webpage.title;
		content = webpage.content;
		contentTime = webpage.contentTime;
		fetchException = webpage.fetchException;
	}

	@Override
	public boolean isEmpty() {
		return content.isEmpty();
	}

	@Override
	public boolean isFinal(FetcherArgs fetcherArgs) {
		return content.length() >= fetcherArgs.getWebpageMinLength();
	}

	public String getStartUrl() {
		return startUrl;
	}
	void setStartUrl(String startUrl) {
		if (startUrl != null) {
			this.startUrl = startUrl.trim();
		}
	}

	public String getFinalUrl() {
		return finalUrl;
	}
	void setFinalUrl(String finalUrl) {
		if (finalUrl != null) {
			this.finalUrl = finalUrl.trim();
		}
	}

	public String getContentType() {
		return contentType;
	}
	void setContentType(String contentType) {
		if (contentType != null) {
			this.contentType = contentType.trim();
		}
	}

	public int getStatusCode() {
		return statusCode;
	}
	void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

	public String getTitle() {
		return title;
	}
	void setTitle(String title) {
		if (title != null) {
			this.title = title.trim();
		}
	}

	public String getContent() {
		return content;
	}
	void setContent(String content) {
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

	@Override
	public String toStringId() {
		return startUrl;
	}

	@Override
	public String toStringIdHtml() {
		return "<a href=\"" + startUrl + "\">" + startUrl + "</a>";
	}

	@Override
	public String toStringPlain() {
		StringBuilder sb = new StringBuilder();
		sb.append(title).append("\n\n");
		sb.append(content);
		return sb.toString();
	}

	@Override
	public String toStringPlainHtml() {
		StringBuilder sb = new StringBuilder();
		sb.append("<h2>").append(title).append("</h2>\n\n");
		sb.append("<p>").append(content.replace("\n\n", "</p>\n<p>").replace("\n", "<br>\n")).append("</p>");
		return sb.toString();
	}

	@Override
	public String toStringHtml() {
		StringBuilder sb = new StringBuilder();
		sb.append("<dl>\n");
		sb.append(super.toStringHtml());
		sb.append("<dt>START URL</dt>\n").append("<dd><a href=\"").append(startUrl).append("\">").append(startUrl).append("</a></dd>\n");
		sb.append("<dt>FINAL URL</dt>\n").append("<dd><a href=\"").append(finalUrl).append("\">").append(finalUrl).append("</a></dd>\n");
		sb.append("<dt>CONTENT TYPE</dt>\n").append("<dd>").append(contentType).append("</dd>\n");
		sb.append("<dt>STATUS CODE</dt>\n").append("<dd>").append(statusCode).append("</dd>\n");
		sb.append("<dt>CONTENT TIME</dt>\n").append("<dd>").append(contentTime).append(" ").append(getContentTimeHuman()).append("</dd>\n");
		sb.append("<dt>TITLE LENGTH</dt>\n").append("<dd>").append(title.length()).append("</dd>\n");
		sb.append("<dt>CONTENT LENGTH</dt>\n").append("<dd>").append(content.length()).append("</dd>\n");
		sb.append("<dt>TITLE</dt>\n").append("<dd>").append(title).append("</dd>\n");
		sb.append("</dl>\n\n");
		sb.append(toStringPlainHtml());
		return sb.toString();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString()).append("\n\n");
		sb.append("START URL: ").append(startUrl).append("\n");
		sb.append("FINAL URL: ").append(finalUrl).append("\n");
		sb.append("CONTENT TYPE: ").append(contentType).append("\n");
		sb.append("STATUS CODE: ").append(statusCode).append("\n");
		sb.append("CONTENT TIME: ").append(contentTime).append(" ").append(getContentTimeHuman()).append("\n\n");
		sb.append("TITLE LENGTH: ").append(title.length()).append("\n");
		sb.append("CONTENT LENGTH: ").append(content.length()).append("\n\n");
		sb.append("TITLE: ").append(title).append("\n\n");
		sb.append("CONTENT: ").append(content);
		return sb.toString();
	}

	@Override
	public int compareTo(Webpage o) {
		if (o == null) return 1;
		return title.compareTo(o.title);
	}
}
