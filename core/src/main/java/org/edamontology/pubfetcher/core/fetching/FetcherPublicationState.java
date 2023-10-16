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

package org.edamontology.pubfetcher.core.fetching;

public class FetcherPublicationState {

	boolean europepmc = false;
	boolean europepmcPmid = false;
	boolean europepmcPmcid = false;
	boolean europepmcDoi = false;

	boolean europepmcHasFulltextXML = false;
	boolean europepmcHasFulltextHTML = false;
	boolean europepmcHasPDF = false;
	boolean europepmcHasMinedTerms = false;

	boolean europepmcFulltextXml = false;
	boolean europepmcFulltextXmlPmcid = false;
	boolean europepmcFulltextHtmlPmcid = false;
	boolean europepmcMinedTermsEfo = false;
	boolean europepmcMinedTermsEfoPmid = false;
	boolean europepmcMinedTermsEfoPmcid = false;
	boolean europepmcMinedTermsGo = false;
	boolean europepmcMinedTermsGoPmid = false;
	boolean europepmcMinedTermsGoPmcid = false;

	boolean pubmedXml = false;
	boolean pubmedXmlPmid = false;
	boolean pubmedHtmlPmid = false;

	boolean pmcXml = false;
	boolean pmcXmlPmcid = false;
	boolean pmcHtmlPmcid = false;

	boolean pmcManuscript = false;

	boolean doi = false;

	boolean oadoi = false;

	FetcherPublicationState() {}
}
