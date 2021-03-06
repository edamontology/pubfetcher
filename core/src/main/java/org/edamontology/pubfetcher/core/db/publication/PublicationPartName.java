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

public enum PublicationPartName {
	pmid("PMID"),
	pmcid("PMCID"),
	doi("DOI"),
	title("Title"),
	keywords("Keywords"),
	mesh("MeSH terms"),
	efo("EFO terms"),
	go("GO terms"),
	theAbstract("Abstract"),
	fulltext("Fulltext");

	private final String name;

	private PublicationPartName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
