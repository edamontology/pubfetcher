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

package org.edamontology.pubfetcher.core.scrape;

public enum ScrapeSiteKey {
	pmid,
	pmcid,
	doi,
	title,
	subtitle,
	keywords,
	keywords_split,
	theAbstract("abstract"),
	fulltext,
	fulltext_src,
	fulltext_dst,
	fulltext_a,
	pdf_src,
	pdf_dst,
	pdf_a,
	corresp_author_names,
	corresp_author_emails,
	wait_until;

	private String key;

	private ScrapeSiteKey() {
		this.key = name();
	}
	private ScrapeSiteKey(String key) {
		this.key = key;
	}

	@Override
	public String toString() {
		return key;
	}
}
