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

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.edamontology.pubfetcher.core.common.FetcherArgs;
import org.edamontology.pubfetcher.core.common.PubFetcher;
import org.edamontology.pubfetcher.core.db.link.Link;
import org.edamontology.pubfetcher.core.db.publication.Publication;
import org.edamontology.pubfetcher.core.db.publication.PublicationPartType;

public class Links {

	private static final Logger logger = LogManager.getLogger();

	private static final Pattern EUROPEPMC = Pattern.compile("EUROPEPMC\\.ORG/ARTICLES/(" + PubFetcher.PMCID.pattern() + ")");
	private static final Pattern EUROPEPMC_PMID = Pattern.compile("EUROPEPMC\\.ORG/ABSTRACT/MED/(" + PubFetcher.PMID.pattern() + ")");
	private static final Pattern PMC = Pattern.compile("NCBI\\.NLM\\.NIH\\.GOV/PMC/ARTICLES/(" + PubFetcher.PMCID.pattern() + ")");
	private static final Pattern PMC_PMID = Pattern.compile("NCBI\\.NLM\\.NIH\\.GOV/PMC/ARTICLES/PMID/(" + PubFetcher.PMID.pattern() + ")");
	private static final Pattern PMCCANADA = Pattern.compile("PUBMEDCENTRALCANADA\\.CA/PMCC/ARTICLES/(" + PubFetcher.PMCID.pattern() + ")");
	private static final Pattern PMCCANADA_PMID = Pattern.compile("PUBMEDCENTRALCANADA\\.CA/PMCC/ARTICLES/PMID/(" + PubFetcher.PMID.pattern() + ")");

	private List<Link> links;

	private Set<Link> triedLinks;

	Links() {
		links = new ArrayList<>();
		triedLinks = new LinkedHashSet<>();
	}

	boolean isEmpty() {
		return links.isEmpty();
	}

	Link pop() {
		while (!links.isEmpty()) {
			Link link = links.remove(0);
			if (!triedLinks.contains(link)) {
				triedLinks.add(link);
				return link;
			}
		}
		return null;
	}

	List<Link> getLinks() {
		return links;
	}

	private void extract(Pattern pmcidPattern, Pattern pmidPattern, String url, Publication publication, PublicationPartType type, String from, FetcherArgs fetcherArgs) {
		String urlUpper = url.toUpperCase(Locale.ROOT);
		if (publication.getPmcid().getType() != PublicationPartType.external && type.isBetterThan(publication.getPmcid().getType())) {
			Matcher pmcidMatcher = pmcidPattern.matcher(urlUpper);
			if (pmcidMatcher.find()) {
				String pmcid = pmcidMatcher.group(1);
				if (pmcid != null) {
					if (publication.setPmcid(pmcid, type, from, fetcherArgs)) {
						logger.info("    Extracted PMCID {} from {} found in {} of type {}", pmcid, url, from, type);
					}
				}
			}
		}
		if (publication.getPmid().getType() != PublicationPartType.external && type.isBetterThan(publication.getPmid().getType())) {
			Matcher pmidMatcher = pmidPattern.matcher(urlUpper);
			if (pmidMatcher.find()) {
				String pmid = pmidMatcher.group(1);
				if (pmid != null) {
					if (publication.setPmid(pmid, type, from, fetcherArgs)) {
						logger.info("    Extracted PMID {} from {} found in {} of type {}", pmid, url, from, type);
					}
				}
			}
		}
	}

	void add(String url, PublicationPartType type, String from, Publication publication, FetcherArgs fetcherArgs, boolean pmcPdf) {
		Link link = null;
		try {
			link = new Link(url, type, from);
		} catch (MalformedURLException e) {
			logger.warn("Can't add malformed link {} found in {} of type {}", url, from, type);
			return;
		}
		String urlString = link.getUrl().toString();

		if (PubFetcher.isDoi(urlString)) {
			return;
		}
		if (link.getUrl().getHost().equalsIgnoreCase("europepmc.org")) {
			if (publication != null) {
				extract(EUROPEPMC, EUROPEPMC_PMID, urlString, publication, type, from, fetcherArgs);
			}
			if (!type.isPdf() || !pmcPdf) return;
		}
		if (link.getUrl().getHost().equalsIgnoreCase("ncbi.nlm.nih.gov") || link.getUrl().getHost().equalsIgnoreCase("www.ncbi.nlm.nih.gov")) {
			if (publication != null) {
				extract(PMC, PMC_PMID, urlString, publication, type, from, fetcherArgs);
			}
			if (!type.isPdf() || !pmcPdf) return;
		}
		if (link.getUrl().getHost().equalsIgnoreCase("pubmedcentralcanada.ca")) {
			if (publication != null) {
				extract(PMCCANADA, PMCCANADA_PMID, urlString, publication, type, from, fetcherArgs);
			}
			if (!type.isPdf() || !pmcPdf) return;
		}

		if (triedLinks.contains(link)) {
			return;
		}

		int equalIndex = links.indexOf(link);
		if (equalIndex > -1) {
			Link equalLink = links.get(equalIndex);
			if (type.isBetterThan(equalLink.getType())) {
				logger.info("    Remove link {} found in {} of type {} from position {}", equalLink.getUrl(), equalLink.getFrom(), equalLink.getType(), equalIndex);
				links.remove(equalIndex);
			} else {
				return;
			}
		}

		int i;
		boolean added = false;
		for (i = 0; i < links.size(); ++i) {
			if (type.isBetterThan(links.get(i).getType())) {
				links.add(i, link);
				added = true;
				break;
			}
		}
		if (!added) {
			links.add(link);
		}
		logger.info("    Add link {} found in {} of type {} to position {}", urlString, from, type, i);
	}

	void addTriedLink(String url, PublicationPartType type, String from) {
		try {
			triedLinks.add(new Link(url, type, from));
		} catch (MalformedURLException e) {
			logger.error("Can't add malformed tried link {} found in {} of type {}", url, from, type);
		}
	}
}
