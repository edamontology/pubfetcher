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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import org.edamontology.pubfetcher.core.common.FetcherArgs;
import org.edamontology.pubfetcher.core.db.publication.Publication;
import org.edamontology.pubfetcher.core.db.publication.PublicationPartName;
import org.edamontology.pubfetcher.core.db.publication.PublicationPartType;

public final class HtmlMeta {

	private static final Logger logger = LogManager.getLogger();

	private static final String CITATION_PMID_SELECTOR = selectorCombinations("citation_pmid");
	private static final String CITATION_PMCID_SELECTOR = selectorCombinations("citation_pmcid");
	private static final String CITATION_DOI_SELECTOR = selectorCombinations("citation_doi");
	private static final String CITATION_TITLE_SELECTOR = selectorCombinations("citation_title");
	private static final String CITATION_KEYWORDS_SELECTOR = selectorCombinations("citation_keywords") + ", " + selectorCombinations("citation_keyword");
	private static final String CITATION_ABSTRACT_SELECTOR = selectorCombinations("citation_abstract");
	private static final String CITATION_FULLTEXT_SELECTOR = selectorCombinations("citation_fulltext_html_url") + ", " + selectorCombinations("citation_full_html_url");
	private static final String CITATION_FULLTEXT_PDF_SELECTOR = selectorCombinations("citation_pdf_url");

	private static final String EPRINTS_PMID_SELECTOR = selectorCombinations("eprints.pubmed_id");
	private static final String EPRINTS_TITLE_SELECTOR = selectorCombinations("eprints.title");
	private static final String EPRINTS_KEYWORDS_SELECTOR = selectorCombinations("eprints.keywords");
	private static final String EPRINTS_ABSTRACT_SELECTOR = selectorCombinations("eprints.abstract");
	private static final String EPRINTS_FULLTEXT_SELECTOR = selectorCombinations("eprints.document_url");

	private static final String BEPRESS_DOI_SELECTOR = selectorCombinations("bepress_citation_doi");
	private static final String BEPRESS_TITLE_SELECTOR = selectorCombinations("bepress_citation_title");
	private static final String BEPRESS_FULLTEXT_PDF_SELECTOR = selectorCombinations("bepress_citation_pdf_url");

	private static final String DC_DOI_SELECTOR = selectorCombinations("dc.doi") + ", " + selectorCombinations("dc.identifier") + ", " + selectorCombinations("dc.identifier.doi");
	private static final String DC_TITLE_SELECTOR = selectorCombinations("dc.title");
	private static final String DC_KEYWORDS_SELECTOR = selectorCombinations("dc.subject");
	private static final String DC_ABSTRACT_SELECTOR = selectorCombinations("dc.description") + ", " + selectorCombinations("dc.description.abstract");

	private static final String OG_TITLE_SELECTOR = selectorCombinations("og.title");
	private static final String OG_ABSTRACT_SELECTOR = selectorCombinations("og.description");

	private static final String TWITTER_TITLE_SELECTOR = selectorCombinations("twitter.title");
	private static final String TWITTER_ABSTRACT_SELECTOR = selectorCombinations("twitter.description");

	private static final String META_DOI_SELECTOR = selectorCombinations("doi");
	private static final String META_TITLE_SELECTOR = selectorCombinations("title");
	private static final String META_KEYWORDS_SELECTOR = selectorCombinations("keywords");
	private static final String META_ABSTRACT_SELECTOR = selectorCombinations("meta.description") + ", " + selectorCombinations("description") + ", " + selectorCombinations("abstract");
	private static final String META_FULLTEXT_SELECTOR = selectorCombinations("fulltext_html");
	private static final String META_FULLTEXT_PDF_SELECTOR = selectorCombinations("fulltext_pdf");

	private static final Pattern SEPARATOR = Pattern.compile("[,;|]");

	private static final Pattern BIOMEDCENTRAL = Pattern.compile("^https?://[a-zA-Z0-9.-]*biomedcentral\\.com/.+$");
	private static final Pattern CITESEERX = Pattern.compile("^https?://(www\\.)?citeseerx\\..+$");
	private static final Pattern F1000 = Pattern.compile("^https?://(www\\.)?f1000research\\.com/.+$");
	private static final Pattern NATURE = Pattern.compile("^https?://(www\\.)?nature\\.com/.+$");
	private static final Pattern WILEY = Pattern.compile("^https?://(www\\.)?onlinelibrary\\.wiley\\.com/.+$");

	private HtmlMeta() {}

	// jsoup selectors are already case-insensitive, so no need to make combinations for case
	private static void addCombinations(String combination, String[] parts, int i, List<String> combinations) {
		combination += parts[i];
		if (i + 1 >= parts.length) {
			combinations.add(combination);
		} else {
			addCombinations(combination + ".", parts, i + 1, combinations);
			addCombinations(combination + ":", parts, i + 1, combinations);
		}
	}

	private static String selectorCombinations(String selector) {
		List<String> combinations = new ArrayList<>();
		addCombinations("", selector.split("[.:]", -1), 0, combinations);
		return combinations.stream()
			.map(s -> "meta[name=" + s + "], meta[property=" + s + "]")
			.collect(Collectors.joining(", "));
	}

	private static void setIds(Publication publication, Document doc, PublicationPartType type, String pmidSelector, String pmcidSelector, String doiSelector, FetcherArgs fetcherArgs) {
		if (pmidSelector != null && type.isBetterThan(publication.getPmid().getType())) {
			for (Element metaPmid : doc.select(pmidSelector)) {
				logger.info("    Found PMID from meta {} in {}", type, doc.location());
				publication.setPmid(metaPmid.attr("content"), type, doc.location(), fetcherArgs);
			}
		}

		if (pmcidSelector != null && type.isBetterThan(publication.getPmcid().getType())) {
			for (Element metaPmcid : doc.select(pmcidSelector)) {
				logger.info("    Found PMCID from meta {} in {}", type, doc.location());
				publication.setPmcid(metaPmcid.attr("content"), type, doc.location(), fetcherArgs);
			}
		}

		if (doiSelector != null && type.isBetterThan(publication.getDoi().getType())) {
			for (Element metaDoi : doc.select(doiSelector)) {
				logger.info("    Found DOI from meta {} in {}", type, doc.location());
				publication.setDoi(metaDoi.attr("content"), type, doc.location(), fetcherArgs);
			}
		}
	}

	private static void setTitle(Publication publication, Document doc, PublicationPartType type, String titleSelector, FetcherArgs fetcherArgs, EnumMap<PublicationPartName, Boolean> parts) {
		if (parts == null || (parts.get(PublicationPartName.title) != null && parts.get(PublicationPartName.title))) {
			if (type.isBetterThan(publication.getTitle().getType())) {
				Element metaTitle = doc.select(titleSelector).first();
				if (metaTitle != null) {
					logger.info("    Found title from meta {} in {}", type, doc.location());
					publication.setTitle(metaTitle.attr("content"), type, doc.location(), fetcherArgs, true);
				}
			}
		}
	}

	private static void setKeywords(Publication publication, Document doc, PublicationPartType type, String keywordsSelector, FetcherArgs fetcherArgs, EnumMap<PublicationPartName, Boolean> parts) {
		if (parts == null || (parts.get(PublicationPartName.keywords) != null && parts.get(PublicationPartName.keywords))) {
			if (type.isBetterThan(publication.getKeywords().getType())) {
				Elements metaKeywords = doc.select(keywordsSelector);
				if (!metaKeywords.isEmpty()) {
					if (F1000.matcher(doc.location()).matches()) {
						for (Iterator<Element> it = metaKeywords.iterator(); it.hasNext(); ) {
							if (it.next().attr("content").indexOf('|') < 0) it.remove();
						}
					}
					List<String> keywords = metaKeywords.stream()
							.flatMap(k -> SEPARATOR.splitAsStream(k.attr("content")))
							.collect(Collectors.toList());
					logger.info("    Found keywords from meta {} in {}", type, doc.location());
					publication.setKeywords(keywords, type, doc.location(), fetcherArgs, true);
				}
			}
		}
	}

	private static void setAbstract(Publication publication, Document doc, PublicationPartType type, String abstractSelector, FetcherArgs fetcherArgs, EnumMap<PublicationPartName, Boolean> parts) {
		if (parts == null || (parts.get(PublicationPartName.theAbstract) != null && parts.get(PublicationPartName.theAbstract))) {
			if (type.isBetterThan(publication.getAbstract().getType())) {
				Elements metaAbstract = doc.select(abstractSelector);
				if (!metaAbstract.isEmpty()) {
					String theAbstract = metaAbstract.stream()
						.map(a -> a.attr("content").trim())
						.filter(a -> !a.isEmpty())
						.collect(Collectors.joining("\n\n"));
					logger.info("    Found abstract from meta {} in {}", type, doc.location());
					publication.setAbstract(theAbstract, type, doc.location(), fetcherArgs, true);
				}
			}
		}
	}

	private static void addLinks(Publication publication, Document doc, PublicationPartType type, String fulltextSelector, Links links, FetcherArgs fetcherArgs) {
		Elements metaFulltext = doc.select(fulltextSelector);
		for (Element meta : metaFulltext) {
			String link = meta.attr("abs:content").trim();
			if (!link.isEmpty()) {
				links.add(link, type, doc.location(), publication, fetcherArgs, false);
			}
		}
	}

	private static PublicationPartType chooseType(PublicationPartType metaType, PublicationPartType type) {
		return (metaType.isBetterThan(type) ? type : metaType);
	}

	static void fillWith(Publication publication, Document doc, PublicationPartType type, Links links, FetcherArgs fetcherArgs, EnumMap<PublicationPartName, Boolean> parts, boolean keywords) {

		fillWithIds(publication, doc, type, fetcherArgs);

		// citation
		PublicationPartType citationType = chooseType(PublicationPartType.citation, type);
		PublicationPartType citationLinkType = chooseType(PublicationPartType.link_citation, type);
		PublicationPartType citationPdfType = chooseType(PublicationPartType.pdf_citation, type);
		setTitle(publication, doc, citationType, CITATION_TITLE_SELECTOR, fetcherArgs, parts);
		if (keywords) {
			setKeywords(publication, doc, citationType, CITATION_KEYWORDS_SELECTOR, fetcherArgs, parts);
		}
		setAbstract(publication, doc, citationType, CITATION_ABSTRACT_SELECTOR, fetcherArgs, parts);
		addLinks(publication, doc, citationLinkType, CITATION_FULLTEXT_SELECTOR, links, fetcherArgs);
		addLinks(publication, doc, citationPdfType, CITATION_FULLTEXT_PDF_SELECTOR, links, fetcherArgs);

		// eprints
		PublicationPartType eprintsType = chooseType(PublicationPartType.eprints, type);
		PublicationPartType eprintsLinkType = chooseType(PublicationPartType.link_eprints, type);
		setTitle(publication, doc, eprintsType, EPRINTS_TITLE_SELECTOR, fetcherArgs, parts);
		if (keywords) {
			setKeywords(publication, doc, eprintsType, EPRINTS_KEYWORDS_SELECTOR, fetcherArgs, parts);
		}
		setAbstract(publication, doc, eprintsType, EPRINTS_ABSTRACT_SELECTOR, fetcherArgs, parts);
		addLinks(publication, doc, eprintsLinkType, EPRINTS_FULLTEXT_SELECTOR, links, fetcherArgs);

		// bepress
		PublicationPartType bepressType = chooseType(PublicationPartType.bepress, type);
		PublicationPartType bepressPdfType = chooseType(PublicationPartType.pdf_bepress, type);
		setTitle(publication, doc, bepressType, BEPRESS_TITLE_SELECTOR, fetcherArgs, parts);
		addLinks(publication, doc, bepressPdfType, BEPRESS_FULLTEXT_PDF_SELECTOR, links, fetcherArgs);

		// dc
		PublicationPartType dcType = chooseType(PublicationPartType.dc, type);;
		setTitle(publication, doc, dcType, DC_TITLE_SELECTOR, fetcherArgs, parts);
		if (keywords && !BIOMEDCENTRAL.matcher(doc.location()).matches()) {
			setKeywords(publication, doc, dcType, DC_KEYWORDS_SELECTOR, fetcherArgs, parts);
		}
		if (!NATURE.matcher(doc.location()).matches()) {
			setAbstract(publication, doc, dcType, DC_ABSTRACT_SELECTOR, fetcherArgs, parts);
		}

		// og
		PublicationPartType ogType = chooseType(PublicationPartType.og, type);;
		setTitle(publication, doc, ogType, OG_TITLE_SELECTOR, fetcherArgs, parts);
		if (!NATURE.matcher(doc.location()).matches() && !WILEY.matcher(doc.location()).matches()) {
			setAbstract(publication, doc, ogType, OG_ABSTRACT_SELECTOR, fetcherArgs, parts);
		}

		// twitter
		PublicationPartType twitterType = chooseType(PublicationPartType.twitter, type);;
		setTitle(publication, doc, twitterType, TWITTER_TITLE_SELECTOR, fetcherArgs, parts);
		if (!NATURE.matcher(doc.location()).matches()) {
			setAbstract(publication, doc, twitterType, TWITTER_ABSTRACT_SELECTOR, fetcherArgs, parts);
		}

		// meta
		PublicationPartType metaType = chooseType(PublicationPartType.meta, type);;
		PublicationPartType metaLinkType = chooseType(PublicationPartType.link_meta, type);;
		PublicationPartType metaPdfType = chooseType(PublicationPartType.pdf_meta, type);;
		setTitle(publication, doc, metaType, META_TITLE_SELECTOR, fetcherArgs, parts);
		if (keywords && !CITESEERX.matcher(doc.location()).matches()) {
			setKeywords(publication, doc, metaType, META_KEYWORDS_SELECTOR, fetcherArgs, parts);
		}
		if (!NATURE.matcher(doc.location()).matches()) {
			setAbstract(publication, doc, metaType, META_ABSTRACT_SELECTOR, fetcherArgs, parts);
		}
		addLinks(publication, doc, metaLinkType, META_FULLTEXT_SELECTOR, links, fetcherArgs);
		addLinks(publication, doc, metaPdfType, META_FULLTEXT_PDF_SELECTOR, links, fetcherArgs);
	}

	static void fillWithIds(Publication publication, Document doc, PublicationPartType type, FetcherArgs fetcherArgs) {

		// citation
		PublicationPartType citationType = chooseType(PublicationPartType.citation, type);;
		setIds(publication, doc, citationType, CITATION_PMID_SELECTOR, CITATION_PMCID_SELECTOR, CITATION_DOI_SELECTOR, fetcherArgs);

		// eprints
		PublicationPartType eprintsType = chooseType(PublicationPartType.eprints, type);;
		setIds(publication, doc, eprintsType, EPRINTS_PMID_SELECTOR, null, null, fetcherArgs);

		// bepress
		PublicationPartType bepressType = chooseType(PublicationPartType.bepress, type);;
		setIds(publication, doc, bepressType, null, null, BEPRESS_DOI_SELECTOR, fetcherArgs);

		// dc
		PublicationPartType dcType = chooseType(PublicationPartType.dc, type);;
		setIds(publication, doc, dcType, null, null, DC_DOI_SELECTOR, fetcherArgs);

		// meta
		PublicationPartType metaType = chooseType(PublicationPartType.meta, type);;
		setIds(publication, doc, metaType, null, null, META_DOI_SELECTOR, fetcherArgs);
	}
}
