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

public enum PublicationPartType {
	// order is important
	europepmc("Europe PubMed Central"),
	europepmc_xml("Europe PubMed Central fulltext XML"),
	europepmc_html("Europe PubMed Central fulltext HTML"),
	pubmed_xml("PubMed XML"),
	pubmed_html("PubMed HTML"),
	pmc_xml("PubMed Central XML"),
	pmc_html("PubMed Central HTML"),
	doi("DOI"),
	link("Link to publication"),
	link_oadoi("Link from oaDOI"),
	citation("HighWire"),
	eprints("EPrints"),
	bepress("bepress"),
	link_citation("Link from Highwire"),
	link_eprints("Link from EPrints"),
	dc("Dublin Core"),
	og("Open Graph"),
	twitter("Twitter"),
	meta("HTML meta"),
	link_meta("Link from HTML meta"),
	external("Externally supplied"),
	oadoi("oaDOI"),
	pdf_europepmc("PDF from Europe PubMed Central"),
	pdf_pmc("PDF from PubMed Central"),
	pdf_doi("PDF DOI"),
	pdf_link("PDF from link to publication"),
	pdf_oadoi("PDF from oaDOI"),
	pdf_citation("PDF from HighWire"),
	pdf_eprints("PDF from EPrints"),
	pdf_bepress("PDF from bepress"),
	pdf_meta("PDF from HTML meta"),
	webpage("Whole webpage"),
	na("NA");

	private final String type;

	private PublicationPartType(String type) {
		this.type = type;
	}

	public boolean isFinal() {
		if (this == europepmc ||
			this == europepmc_xml ||
			this == europepmc_html ||
			this == pubmed_xml ||
			this == pubmed_html ||
			this == pmc_xml ||
			this == pmc_html ||
			this == doi ||
			this == link ||
			this == link_oadoi) {
			return true;
		}
		return false;
	}

	public boolean isEquivalent(PublicationPartType type) {
		if (type == null) return false;
		if (isFinal() && type.isFinal()) return true;
		return this == type;
	}

	public boolean isBetterThan(PublicationPartType type) {
		if (type == null) return true;
		if (isEquivalent(type)) return false;
		return (ordinal() - type.ordinal() < 0);
	}

	public boolean isPdf() {
		if (this == pdf_europepmc ||
			this == pdf_pmc ||
			this == pdf_doi ||
			this == pdf_link ||
			this == pdf_oadoi ||
			this == pdf_citation ||
			this == pdf_eprints ||
			this == pdf_bepress ||
			this == pdf_meta) {
			return true;
		}
		return false;
	}

	public PublicationPartType toPdf() {
		switch(this) {
		case europepmc_html: return pdf_europepmc;
		case pmc_html: return pdf_pmc;
		case doi: return pdf_doi;
		case link: return pdf_link;
		case link_oadoi: return pdf_oadoi;
		case link_citation: return pdf_citation;
		case link_eprints: return pdf_eprints;
		case link_meta: return pdf_meta;
		default:
			if (isPdf()) {
				System.err.println("Publication part type " + this + " is already of PDF type");
			} else {
				System.err.println("Publication part type " + this + " can't be converted to PDF type");
			}
			return this;
		}
	}

	public String getType() {
		return type;
	}
}
