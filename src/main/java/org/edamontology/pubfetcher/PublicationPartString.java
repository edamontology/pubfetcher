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

public class PublicationPartString extends PublicationPart {

	private static final long serialVersionUID = -2951993086464448994L;

	private String content;

	PublicationPartString(PublicationPartName name) {
		super(name);
		content = "";
	}

	public String getContent() {
		return content;
	}

	void set(String content, PublicationPartType type, String url) {
		if (content == null) {
			this.content = "";
			set(null, null);
		} else {
			this.content = content;
			set(type, url);
		}
	}

	@Override
	public int getSize() {
		return content.length();
	}

	@Override
	public boolean isEmpty() {
		return content.isEmpty();
	}

	@Override
	public String toStringPlain() {
		return content;
	}

	@Override
	public String toStringPlainHtml() {
		return FetcherCommon.getParagraphsHtml(content);
	}
}
