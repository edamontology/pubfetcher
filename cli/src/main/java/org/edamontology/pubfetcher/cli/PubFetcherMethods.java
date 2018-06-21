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

package org.edamontology.pubfetcher.cli;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import org.edamontology.pubfetcher.core.common.FetcherArgs;
import org.edamontology.pubfetcher.core.common.PubFetcher;
import org.edamontology.pubfetcher.core.common.Version;
import org.edamontology.pubfetcher.core.db.Database;
import org.edamontology.pubfetcher.core.db.DatabaseEntry;
import org.edamontology.pubfetcher.core.db.link.Link;
import org.edamontology.pubfetcher.core.db.publication.MeshTerm;
import org.edamontology.pubfetcher.core.db.publication.MinedTerm;
import org.edamontology.pubfetcher.core.db.publication.Publication;
import org.edamontology.pubfetcher.core.db.publication.PublicationIds;
import org.edamontology.pubfetcher.core.db.publication.PublicationPart;
import org.edamontology.pubfetcher.core.db.publication.PublicationPartList;
import org.edamontology.pubfetcher.core.db.publication.PublicationPartName;
import org.edamontology.pubfetcher.core.db.publication.PublicationPartString;
import org.edamontology.pubfetcher.core.db.publication.PublicationPartType;
import org.edamontology.pubfetcher.core.db.webpage.Webpage;
import org.edamontology.pubfetcher.core.fetching.Fetcher;
import org.edamontology.pubfetcher.core.fetching.FetcherTest;
import org.edamontology.pubfetcher.core.scrape.Scrape;
import org.edamontology.pubfetcher.core.scrape.ScrapeSiteKey;

public final class PubFetcherMethods {

	private static final Logger logger = LogManager.getLogger();

	private static String PUB_ID_SOURCE = "PubFetcher";

	private PubFetcherMethods() {}

	private static String timeHuman(Long time) {
		return Instant.ofEpochMilli(time).toString();
	}

	private static void initDb(String database) throws FileAlreadyExistsException {
		logger.info("Init database: {}", database);
		Database.init(database);
		logger.info("Init: success");
	}

	private static void commitDb(String database) throws IOException {
		logger.info("Commit database: {}", database);
		try (Database db = new Database(database)) {
			db.commit();
		}
		logger.info("Commit: success");
	}

	private static void compactDb(String database) throws IOException {
		logger.info("Compact database: {}", database);
		try (Database db = new Database(database)) {
			db.compact();
		}
		logger.info("Compact: success");
	}

	private static void publicationsSize(String database) throws IOException {
		try (Database db = new Database(database)) {
			System.out.println(db.getPublicationsSize());
		}
	}
	private static void webpagesSize(String database) throws IOException {
		try (Database db = new Database(database)) {
			System.out.println(db.getWebpagesSize());
		}
	}
	private static void docsSize(String database) throws IOException {
		try (Database db = new Database(database)) {
			System.out.println(db.getDocsSize());
		}
	}

	private static void dumpPublicationsMap(String database) throws IOException {
		try (Database db = new Database(database)) {
			System.out.print(db.dumpPublicationsMap());
		}
	}
	private static void dumpPublicationsMapReverse(String database) throws IOException {
		try (Database db = new Database(database)) {
			System.out.print(db.dumpPublicationsMapReverse());
		}
	}

	private static void printWebpageSelector(String webpageUrl, String title, String content, boolean javascript, boolean html, Fetcher fetcher, FetcherArgs fetcherArgs) {
		Webpage webpage = fetcher.initWebpage(webpageUrl);
		fetcher.getWebpage(webpage, title, content, javascript, fetcherArgs);
		if (html) System.out.println(webpage.toStringHtml(""));
		else System.out.println(webpage.toString());
	}

	private static void getSite(String site, Fetcher fetcher) {
		System.out.println(fetcher.getScrape().getSite(site));
	}
	private static void getSelector(String site, ScrapeSiteKey siteKey, Fetcher fetcher) {
		System.out.println(fetcher.getScrape().getSelector(fetcher.getScrape().getSite(site), siteKey));
	}
	private static void getWebpage(String url, Fetcher fetcher) {
		System.out.println(fetcher.getScrape().getWebpage(url));
	}
	private static void getJavascript(String doi, Fetcher fetcher) {
		System.out.println(fetcher.getScrape().getJavascript(PubFetcher.getDoiRegistrant(doi)));
	}

	// pubFile is in PubFetcher-Core
	// webFile is in PubFetcher-Core

	private static List<PublicationIds> pub(List<String> pubIds) {
		if (pubIds.isEmpty()) {
			logger.error("Check publication IDs: no publication IDs given");
			return Collections.emptyList();
		}
		logger.info("Check publication IDs: {} publication IDs given", pubIds.size());
		List<PublicationIds> publicationIds = pubIds.stream()
			.map(s -> PubFetcher.getPublicationIds(s, PUB_ID_SOURCE, false))
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
		if (publicationIds.size() < pubIds.size()) {
			logger.warn("{} publication IDs OK, {} not OK", publicationIds.size(), pubIds.size() - publicationIds.size());
		} else {
			logger.info("{} publication IDs OK", publicationIds.size());
		}
		return publicationIds;
	}

	private static List<PublicationIds> pubCheck(List<PublicationIds> pubIds) {
		if (pubIds.isEmpty()) {
			logger.error("Check publication IDs: no publication IDs given");
			return Collections.emptyList();
		}
		logger.info("Check publication IDs: {} publication IDs given", pubIds.size());
		List<PublicationIds> publicationIds = pubIds.stream()
			.map(pubId -> PubFetcher.getPublicationIds(pubId.getPmid(), pubId.getPmcid(), pubId.getDoi(), PUB_ID_SOURCE, false, true))
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
		if (publicationIds.size() < pubIds.size()) {
			logger.warn("{} publication IDs OK, {} not OK", publicationIds.size(), pubIds.size() - publicationIds.size());
		} else {
			logger.info("{} publication IDs OK", publicationIds.size());
		}
		return publicationIds;
	}

	private static List<String> web(List<String> webUrls) {
		if (webUrls.isEmpty()) {
			logger.error("Check webpage URLs: no webpage URLs given");
			return Collections.emptyList();
		}
		logger.info("Check webpage URLs: {} webpage URLs given", webUrls.size());
		List<String> webpageUrls = webUrls.stream()
			.map(s -> PubFetcher.getUrl(s, false))
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
		if (webpageUrls.size() < webUrls.size()) {
			logger.warn("{} webpage URLs OK, {} not OK", webpageUrls.size(), webUrls.size() - webpageUrls.size());
		} else {
			logger.info("{} webpage URLs OK", webpageUrls.size());
		}
		return webpageUrls;
	}

	private static Set<PublicationIds> pubDb(String database) throws IOException {
		Set<PublicationIds> publicationIds;
		logger.info("Get publication IDs from database: {}", database);
		try (Database db = new Database(database)) {
			publicationIds = db.getPublicationIds();
		}
		logger.info("Got {} publication IDs", publicationIds.size());
		return publicationIds;
	}
	private static Set<String> webDb(String database) throws IOException {
		Set<String> webpageUrls;
		logger.info("Get webpage URLs from database: {}", database);
		try (Database db = new Database(database)) {
			webpageUrls = db.getWebpageUrls();
		}
		logger.info("Got {} webpage URLs", webpageUrls.size());
		return webpageUrls;
	}
	private static Set<String> docDb(String database) throws IOException {
		Set<String> docUrls;
		logger.info("Get doc URLs from database: {}", database);
		try (Database db = new Database(database)) {
			docUrls = db.getDocUrls();
		}
		logger.info("Got {} doc URLs", docUrls.size());
		return docUrls;
	}

	private static void hasPmid(Set<PublicationIds> pubIds) {
		logger.info("Filter publication IDs with PMID: before {}", pubIds.size());
		for (Iterator<PublicationIds> it = pubIds.iterator(); it.hasNext(); ) {
			if (it.next().getPmid().isEmpty()) it.remove();
		}
		logger.info("Filter publication IDs with PMID: after {}", pubIds.size());
	}
	private static void notHasPmid(Set<PublicationIds> pubIds) {
		logger.info("Filter publication IDs with no PMID: before {}", pubIds.size());
		for (Iterator<PublicationIds> it = pubIds.iterator(); it.hasNext(); ) {
			if (!it.next().getPmid().isEmpty()) it.remove();
		}
		logger.info("Filter publication IDs with no PMID: after {}", pubIds.size());
	}
	private static void pmid(Set<PublicationIds> pubIds, String regex) {
		logger.info("Filter publication IDs with PMID matching {}: before {}", regex, pubIds.size());
		for (Iterator<PublicationIds> it = pubIds.iterator(); it.hasNext(); ) {
			if (!it.next().getPmid().matches(regex)) it.remove();
		}
		logger.info("Filter publication IDs with PMID matching {}: after {}", regex, pubIds.size());
	}

	private static void hasPmcid(Set<PublicationIds> pubIds) {
		logger.info("Filter publication IDs with PMCID: before {}", pubIds.size());
		for (Iterator<PublicationIds> it = pubIds.iterator(); it.hasNext(); ) {
			if (it.next().getPmcid().isEmpty()) it.remove();
		}
		logger.info("Filter publication IDs with PMCID: after {}", pubIds.size());
	}
	private static void notHasPmcid(Set<PublicationIds> pubIds) {
		logger.info("Filter publication IDs with no PMCID: before {}", pubIds.size());
		for (Iterator<PublicationIds> it = pubIds.iterator(); it.hasNext(); ) {
			if (!it.next().getPmcid().isEmpty()) it.remove();
		}
		logger.info("Filter publication IDs with no PMCID: after {}", pubIds.size());
	}
	private static void pmcid(Set<PublicationIds> pubIds, String regex) {
		logger.info("Filter publication IDs with PMCID matching {}: before {}", regex, pubIds.size());
		for (Iterator<PublicationIds> it = pubIds.iterator(); it.hasNext(); ) {
			if (!it.next().getPmcid().matches(regex)) it.remove();
		}
		logger.info("Filter publication IDs with PMCID matching {}: after {}", regex, pubIds.size());
	}

	private static void hasDoi(Set<PublicationIds> pubIds) {
		logger.info("Filter publication IDs with DOI: before {}", pubIds.size());
		for (Iterator<PublicationIds> it = pubIds.iterator(); it.hasNext(); ) {
			if (it.next().getDoi().isEmpty()) it.remove();
		}
		logger.info("Filter publication IDs with DOI: after {}", pubIds.size());
	}
	private static void notHasDoi(Set<PublicationIds> pubIds) {
		logger.info("Filter publication IDs with no DOI: before {}", pubIds.size());
		for (Iterator<PublicationIds> it = pubIds.iterator(); it.hasNext(); ) {
			if (!it.next().getDoi().isEmpty()) it.remove();
		}
		logger.info("Filter publication IDs with no DOI: after {}", pubIds.size());
	}
	private static void doi(Set<PublicationIds> pubIds, String regex) {
		logger.info("Filter publication IDs with DOI matching {}: before {}", regex, pubIds.size());
		for (Iterator<PublicationIds> it = pubIds.iterator(); it.hasNext(); ) {
			if (!it.next().getDoi().matches(regex)) it.remove();
		}
		logger.info("Filter publication IDs with DOI matching {}: after {}", regex, pubIds.size());
	}

	private static void doiRegistrant(Set<PublicationIds> pubIds, List<String> registrants, boolean not) {
		logger.info("Filter publication IDs with DOI {}of registrant {}: before {}", not ? "not " : "", registrants, pubIds.size());
		for (Iterator<PublicationIds> it = pubIds.iterator(); it.hasNext(); ) {
			String doi = it.next().getDoi();
			boolean matches = !doi.isEmpty() && registrants.contains(PubFetcher.getDoiRegistrant(doi));
			if (!not && !matches || not && matches) {
				it.remove();
			}
		}
		logger.info("Filter publication IDs with DOI {}of registrant {}: after {}", not ? "not " : "", registrants, pubIds.size());
	}

	private static void url(Set<String> webUrls, String regex) {
		logger.info("Filter webpage URLs matching {}: before {}", regex, webUrls.size());
		for (Iterator<String> it = webUrls.iterator(); it.hasNext(); ) {
			if (!it.next().matches(regex)) it.remove();
		}
		logger.info("Filter webpage URLs matching {}: after {}", regex, webUrls.size());
	}

	private static void urlHost(Set<String> webUrls, List<String> hosts, boolean not) {
		logger.info("Filter webpage URLs {}of host {}: before {}", not ? "not " : "", hosts, webUrls.size());
		for (Iterator<String> it = webUrls.iterator(); it.hasNext(); ) {
			boolean matches = false;
			try {
				if (hosts.contains(new URL(it.next()).getHost())) {
					matches = true;
				}
			} catch (MalformedURLException e) {
			}
			if (!not && !matches || not && matches) {
				it.remove();
			}
		}
		logger.info("Filter webpage URLs {}of host {}: after {}", not ? "not " : "", hosts, webUrls.size());
	}

	private static void inDbPub(Set<PublicationIds> pubIds, String database) throws IOException {
		logger.info("Filter {} publication IDs being present in database: {}", pubIds.size(), database);
		try (Database db = new Database(database)) {
			for (Iterator<PublicationIds> it = pubIds.iterator(); it.hasNext(); ) {
				if (!db.containsPublication(it.next())) it.remove();
			}
		}
		logger.info("{} publication IDs were present in database", pubIds.size());
	}
	private static void notInDbPub(Set<PublicationIds> pubIds, String database) throws IOException {
		logger.info("Filter {} publication IDs being not present in database: {}", pubIds.size(), database);
		try (Database db = new Database(database)) {
			for (Iterator<PublicationIds> it = pubIds.iterator(); it.hasNext(); ) {
				if (db.containsPublication(it.next())) it.remove();
			}
		}
		logger.info("{} publication IDs were not present in database", pubIds.size());
	}

	private static void inDbWeb(Set<String> webUrls, String database) throws IOException {
		logger.info("Filter {} webpage URLs being present in database: {}", webUrls.size(), database);
		try (Database db = new Database(database)) {
			for (Iterator<String> it = webUrls.iterator(); it.hasNext(); ) {
				if (!db.containsWebpage(it.next())) it.remove();
			}
		}
		logger.info("{} webpage URLs were present in database", webUrls.size());
	}
	private static void notInDbWeb(Set<String> webUrls, String database) throws IOException {
		logger.info("Filter {} webpage URLs being not present in database: {}", webUrls.size(), database);
		try (Database db = new Database(database)) {
			for (Iterator<String> it = webUrls.iterator(); it.hasNext(); ) {
				if (db.containsWebpage(it.next())) it.remove();
			}
		}
		logger.info("{} webpage URLs were not present in database", webUrls.size());
	}

	private static void inDbDoc(Set<String> docUrls, String database) throws IOException {
		logger.info("Filter {} doc URLs being present in database: {}", docUrls.size(), database);
		try (Database db = new Database(database)) {
			for (Iterator<String> it = docUrls.iterator(); it.hasNext(); ) {
				if (!db.containsDoc(it.next())) it.remove();
			}
		}
		logger.info("{} doc URLs were present in database", docUrls.size());
	}
	private static void notInDbDoc(Set<String> docUrls, String database) throws IOException {
		logger.info("Filter {} doc URLs being not present in database: {}", docUrls.size(), database);
		try (Database db = new Database(database)) {
			for (Iterator<String> it = docUrls.iterator(); it.hasNext(); ) {
				if (db.containsDoc(it.next())) it.remove();
			}
		}
		logger.info("{} doc URLs were not present in database", docUrls.size());
	}

	private static <T extends Comparable<T>> LinkedHashSet<T> ascIds(Set<T> ids) {
		logger.info("Sort {} IDs in ascending order", ids.size());
		return ids.stream().sorted().collect(Collectors.toCollection(LinkedHashSet::new));
	}
	private static <T extends Comparable<T>> LinkedHashSet<T> descIds(Set<T> ids) {
		logger.info("Sort {} IDs in descending order", ids.size());
		return ids.stream().sorted(Collections.reverseOrder()).collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private static void removeIdsPub(Set<PublicationIds> pubIds, String database) throws IOException {
		logger.info("Remove {} publication IDs from database: {}", pubIds.size(), database);
		int fail = 0;
		try (Database db = new Database(database)) {
			for (PublicationIds pubId : pubIds) {
				if (!db.removePublication(pubId)) {
					logger.warn("Failed to remove publication ID: {}", pubId);
					++fail;
				} else db.commit();
			}
		}
		if (fail > 0) logger.warn("Failed to remove {} publication IDs", fail);
		else logger.info("Remove publication IDs: success");
	}
	private static void removeIdsWeb(Set<String> webUrls, String database) throws IOException {
		logger.info("Remove {} webpage URLs from database: {}", webUrls.size(), database);
		int fail = 0;
		try (Database db = new Database(database)) {
			for (String webUrl : webUrls) {
				if (!db.removeWebpage(webUrl)) {
					logger.warn("Failed to remove webpage URL: {}", webUrl);
					++fail;
				} else db.commit();
			}
		}
		if (fail > 0) logger.warn("Failed to remove {} webpage URLs", fail);
		else logger.info("Remove webpage URLs: success");
	}
	private static void removeIdsDoc(Set<String> docUrls, String database) throws IOException {
		logger.info("Remove {} doc URLs from database: {}", docUrls.size(), database);
		int fail = 0;
		try (Database db = new Database(database)) {
			for (String docUrl : docUrls) {
				if (!db.removeDoc(docUrl)) {
					logger.warn("Failed to remove doc URL: {}", docUrl);
					++fail;
				} else db.commit();
			}
		}
		if (fail > 0) logger.warn("Failed to remove {} doc URLs", fail);
		else logger.info("Remove doc URLs: success");
	}

	private static void printIdsPub(PrintStream ps, Set<PublicationIds> pubIds, boolean plain, boolean html) throws IOException {
		if (html) {
			if (plain) ps.println("<table border=\"1\">");
			else ps.println("<ul>");
		}
		for (PublicationIds pubId : pubIds) {
			if (html) {
				if (plain) ps.println(pubId.toStringHtml(true));
				else ps.println("<li>" + pubId.toStringWithUrlHtml() + "</li>");
			} else {
				if (plain) ps.println(pubId.toString(true));
				else ps.println(pubId.toStringWithUrl());
			}
		}
		if (html) {
			if (plain) ps.println("</table>");
			else ps.println("</ul>");
		}
	}
	private static void outIdsPub(Set<PublicationIds> pubIds, boolean plain, boolean html) throws IOException {
		if (pubIds.size() == 0) return;
		logger.info("Output {} publication IDs{}", pubIds.size(), html ? " in HTML" : "");
		printIdsPub(System.out, pubIds, plain, html);
	}
	private static void txtIdsPub(Set<PublicationIds> pubIds, boolean plain, boolean html, String txt) throws IOException {
		logger.info("Output {} publication IDs to file {}{}", pubIds.size(), txt, html ? " in HTML" : "");
		try (PrintStream ps = new PrintStream(new BufferedOutputStream(Files.newOutputStream(PubFetcher.outputPath(txt))), true, "UTF-8")) {
			if (pubIds.size() == 0) return;
			printIdsPub(ps, pubIds, plain, html);
		}
	}

	private static void printIdsWeb(PrintStream ps, Set<String> webUrls, boolean html) throws IOException {
		if (html) ps.println("<ul>");
		for (String webUrl : webUrls) {
			if (html) ps.println("<li>" + PubFetcher.getLinkHtml(webUrl) + "</li>");
			else ps.println(webUrl);
		}
		if (html) ps.println("</ul>");
	}
	private static void outIdsWeb(Set<String> webUrls, boolean html) throws IOException {
		if (webUrls.size() == 0) return;
		logger.info("Output {} webpage URLs{}", webUrls.size(), html ? " in HTML" : "");
		printIdsWeb(System.out, webUrls, html);
	}
	private static void txtIdsWeb(Set<String> webUrls, boolean html, String txt) throws IOException {
		logger.info("Output {} webpage URLs to file {}{}", webUrls.size(), txt, html ? " in HTML" : "");
		try (PrintStream ps = new PrintStream(new BufferedOutputStream(Files.newOutputStream(PubFetcher.outputPath(txt))), true, "UTF-8")) {
			if (webUrls.size() == 0) return;
			printIdsWeb(ps, webUrls, html);
		}
	}

	private static void countIds(String label, Set<?> ids) {
		System.out.println(label + " : " + ids.size());
	}

	private static void gotLog(String what, int initial, int current) {
		if (current != initial) logger.warn("Got {} {}", current, what);
		else logger.info("Got {} {}", current, what);
	}
	private static void fetchedLog(String what, int initial, int current) {
		if (current != initial) logger.warn("Fetched {} {}", current, what);
		else logger.info("Fetched {} {}", current, what);
	}

	private static List<Publication> dbPub(Set<PublicationIds> pubIds, String database) throws IOException {
		List<Publication> publications;
		logger.info("Get {} publications from database: {}", pubIds.size(), database);
		try (Database db = new Database(database)) {
			publications = pubIds.stream().map(db::getPublication).filter(Objects::nonNull).collect(Collectors.toList());
		}
		gotLog("publications", pubIds.size(), publications.size());
		return publications;
	}
	private static List<Webpage> dbWeb(Set<String> webUrls, String database) throws IOException {
		List<Webpage> webpages;
		logger.info("Get {} webpages from database: {}", webUrls.size(), database);
		try (Database db = new Database(database)) {
			webpages = webUrls.stream().map(db::getWebpage).filter(Objects::nonNull).collect(Collectors.toList());
		}
		gotLog("webpages", webUrls.size(), webpages.size());
		return webpages;
	}
	private static List<Webpage> dbDoc(Set<String> docUrls, String database) throws IOException {
		List<Webpage> docs;
		logger.info("Get {} docs from database: {}", docUrls.size(), database);
		try (Database db = new Database(database)) {
			docs = docUrls.stream().map(db::getDoc).filter(Objects::nonNull).collect(Collectors.toList());
		}
		gotLog("docs", docUrls.size(), docs.size());
		return docs;
	}

	private static List<Publication> fetchPub(Set<PublicationIds> pubIds, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) throws IOException {
		List<Publication> publications = new ArrayList<>();
		List<PublicationIds> publicationsException = new ArrayList<>();
		int pubIdsSize = pubIds.size();
		logger.info("Fetch {} publications", pubIdsSize);
		int i = 0;
		for (PublicationIds pubId : pubIds) {
			++i;
			logger.info("Fetch publication {}", PubFetcher.progress(i, pubIdsSize));
			Publication publication = fetcher.initPublication(pubId, fetcherArgs);
			if (publication != null && fetcher.getPublication(publication, parts, fetcherArgs)) {
				if (publication.isFetchException()) {
					publicationsException.add(pubId);
				} else {
					publications.add(publication);
				}
			}
		}
		int publicationsExceptionSize = publicationsException.size();
		if (publicationsExceptionSize > 0) {
			logger.info("Refetch {} publications with exception", publicationsExceptionSize);
			i = 0;
			for (PublicationIds pubId : publicationsException) {
				++i;
				logger.info("Refetch publication {}", PubFetcher.progress(i, publicationsExceptionSize));
				Publication publication = fetcher.initPublication(pubId, fetcherArgs);
				if (publication != null && fetcher.getPublication(publication, parts, fetcherArgs)) {
					publications.add(publication);
				}
			}
		}
		fetchedLog("publications", pubIdsSize, publications.size());
		return publications;
	}
	private static List<Webpage> fetchWeb(Set<String> webUrls, Fetcher fetcher, FetcherArgs fetcherArgs) throws IOException {
		List<Webpage> webpages = new ArrayList<>();
		List<String> webpagesException = new ArrayList<>();
		int webUrlsSize = webUrls.size();
		logger.info("Fetch {} webpages", webUrlsSize);
		int i = 0;
		for (String webUrl : webUrls) {
			++i;
			logger.info("Fetch webpage {}", PubFetcher.progress(i, webUrlsSize));
			Webpage webpage = fetcher.initWebpage(webUrl);
			if (webpage != null && fetcher.getWebpage(webpage, fetcherArgs)) {
				if (webpage.isFetchException()) {
					webpagesException.add(webUrl);
				} else {
					webpages.add(webpage);
				}
			}
		}
		int webpagesExceptionSize = webpagesException.size();
		if (webpagesExceptionSize > 0) {
			logger.info("Refetch {} webpages with exception", webpagesExceptionSize);
			i = 0;
			for (String webUrl : webpagesException) {
				++i;
				logger.info("Refetch webpage {}", PubFetcher.progress(i, webpagesExceptionSize));
				Webpage webpage = fetcher.initWebpage(webUrl);
				if (webpage != null && fetcher.getWebpage(webpage, fetcherArgs)) {
					fetcher.getWebpage(webpage, fetcherArgs);
				}
			}
		}
		fetchedLog("webpages", webUrlsSize, webpages.size());
		return webpages;
	}

	private static List<Publication> dbFetchPub(Set<PublicationIds> pubIds, String database, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) throws IOException {
		List<Publication> publications = new ArrayList<>();
		List<PublicationIds> pubIdsException = new ArrayList<>();
		int pubIdsSize = pubIds.size();
		logger.info("Get {} publications from database: {} (or fetch if not present)", pubIdsSize, database);
		try (Database db = new Database(database)) {
			int i = 0;
			for (PublicationIds pubId : pubIds) {
				++i;
				logger.info("Fetch publication {}", PubFetcher.progress(i, pubIdsSize));
				Publication publication = PubFetcher.getPublication(pubId, db, fetcher, parts, fetcherArgs);
				if (publication != null) {
					if (publication.isFetchException()) {
						pubIdsException.add(pubId);
					} else {
						publications.add(publication);
					}
				}
			}
			int pubIdsExceptionSize = pubIdsException.size();
			if (pubIdsExceptionSize > 0) {
				logger.info("Refetch {} publications with exception", pubIdsExceptionSize);
				i = 0;
				for (PublicationIds pubId : pubIdsException) {
					++i;
					logger.info("Refetch publication {}", PubFetcher.progress(i, pubIdsExceptionSize));
					Publication publication = PubFetcher.getPublication(pubId, db, fetcher, parts, fetcherArgs);
					if (publication != null) {
						publications.add(publication);
					}
				}
			}
		}
		gotLog("publications", pubIdsSize, publications.size());
		return publications;
	}
	private static List<Webpage> dbFetchWeb(Set<String> webUrls, String database, Fetcher fetcher, FetcherArgs fetcherArgs) throws IOException {
		List<Webpage> webpages = new ArrayList<>();
		List<String> webUrlsException = new ArrayList<>();
		int webUrlsSize = webUrls.size();
		logger.info("Get {} webpages from database: {} (or fetch if not present)", webUrlsSize, database);
		try (Database db = new Database(database)) {
			int i = 0;
			for (String webUrl : webUrls) {
				++i;
				logger.info("Fetch webpage {}", PubFetcher.progress(i, webUrlsSize));
				Webpage webpage = PubFetcher.getWebpage(webUrl, db, fetcher, fetcherArgs);
				if (webpage != null) {
					if (webpage.isFetchException()) {
						webUrlsException.add(webUrl);
					} else {
						webpages.add(webpage);
					}
				}
			}
			int webUrlsExceptionSize = webUrlsException.size();
			if (webUrlsExceptionSize > 0) {
				logger.info("Refetch {} webpages with exception", webUrlsExceptionSize);
				i = 0;
				for (String webUrl : webUrlsException) {
					++i;
					logger.info("Refetch webpage {}", PubFetcher.progress(i, webUrlsExceptionSize));
					Webpage webpage = PubFetcher.getWebpage(webUrl, db, fetcher, fetcherArgs);
					if (webpage != null) {
						webpages.add(webpage);
					}
				}
			}
		}
		gotLog("webpages", webUrlsSize, webpages.size());
		return webpages;
	}
	private static List<Webpage> dbFetchDoc(Set<String> docUrls, String database, Fetcher fetcher, FetcherArgs fetcherArgs) throws IOException {
		List<Webpage> docs = new ArrayList<>();
		List<String> docUrlsException = new ArrayList<>();
		int docUrlsSize = docUrls.size();
		logger.info("Get {} docs from database: {} (or fetch if not present)", docUrlsSize, database);
		try (Database db = new Database(database)) {
			int i = 0;
			for (String docUrl : docUrls) {
				++i;
				logger.info("Fetch doc {}", PubFetcher.progress(i, docUrlsSize));
				Webpage doc = PubFetcher.getDoc(docUrl, db, fetcher, fetcherArgs);
				if (doc != null) {
					if (doc.isFetchException()) {
						docUrlsException.add(docUrl);
					} else {
						docs.add(doc);
					}
				}
			}
			int docUrlsExceptionSize = docUrlsException.size();
			if (docUrlsExceptionSize > 0) {
				logger.info("Refetch {} docs with exception", docUrlsExceptionSize);
				i = 0;
				for (String docUrl : docUrlsException) {
					++i;
					logger.info("Refetch doc {}", PubFetcher.progress(i, docUrlsExceptionSize));
					Webpage doc = PubFetcher.getDoc(docUrl, db, fetcher, fetcherArgs);
					if (doc != null) {
						docs.add(doc);
					}
				}
			}
		}
		gotLog("docs", docUrlsSize, docs.size());
		return docs;
	}

	private static List<Publication> fetchPutPub(Set<PublicationIds> pubIds, String database, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) throws IOException {
		List<Publication> publications = new ArrayList<>();
		List<PublicationIds> publicationsException = new ArrayList<>();
		int pubIdsSize = pubIds.size();
		logger.info("Fetch {} publications and put to database: {}", pubIdsSize, database);
		try (Database db = new Database(database)) {
			int i = 0;
			for (PublicationIds pubId : pubIds) {
				++i;
				logger.info("Fetch publication {}", PubFetcher.progress(i, pubIdsSize));
				Publication publication = fetcher.initPublication(pubId, fetcherArgs);
				if (publication != null && fetcher.getPublication(publication, parts, fetcherArgs)) {
					if (publication.isFetchException()) {
						publicationsException.add(pubId);
					} else {
						db.putPublication(publication);
						db.commit();
						publications.add(publication);
					}
				}
			}
			int publicationsExceptionSize = publicationsException.size();
			if (publicationsExceptionSize > 0) {
				logger.info("Refetch {} publications with exception", publicationsExceptionSize);
				i = 0;
				for (PublicationIds pubId : publicationsException) {
					++i;
					logger.info("Refetch publication {}", PubFetcher.progress(i, publicationsExceptionSize));
					Publication publication = fetcher.initPublication(pubId, fetcherArgs);
					if (publication != null && fetcher.getPublication(publication, parts, fetcherArgs)) {
						db.putPublication(publication);
						db.commit();
						publications.add(publication);
					}
				}
			}
		}
		fetchedLog("publications", pubIdsSize, publications.size());
		return publications;
	}
	private static List<Webpage> fetchPutWeb(Set<String> webUrls, String database, Fetcher fetcher, FetcherArgs fetcherArgs) throws IOException {
		List<Webpage> webpages = new ArrayList<>();
		List<String> webpagesException = new ArrayList<>();
		int webUrlsSize = webUrls.size();
		logger.info("Fetch {} webpages and put to database: {}", webUrlsSize, database);
		try (Database db = new Database(database)) {
			int i = 0;
			for (String webUrl : webUrls) {
				++i;
				logger.info("Fetch webpage {}", PubFetcher.progress(i, webUrlsSize));
				Webpage webpage = fetcher.initWebpage(webUrl);
				if (webpage != null && fetcher.getWebpage(webpage, fetcherArgs)) {
					if (webpage.isFetchException()) {
						webpagesException.add(webUrl);
					} else {
						db.putWebpage(webpage);
						db.commit();
						webpages.add(webpage);
					}
				}
			}
			int webpagesExceptionSize = webpagesException.size();
			if (webpagesExceptionSize > 0) {
				logger.info("Refetch {} webpages with exception", webpagesExceptionSize);
				i = 0;
				for (String webUrl : webpagesException) {
					++i;
					logger.info("Refetch webpage {}", PubFetcher.progress(i, webpagesExceptionSize));
					Webpage webpage = fetcher.initWebpage(webUrl);
					if (webpage != null && fetcher.getWebpage(webpage, fetcherArgs)) {
						db.putWebpage(webpage);
						db.commit();
						webpages.add(webpage);
					}
				}
			}
		}
		fetchedLog("webpages", webUrlsSize, webpages.size());
		return webpages;
	}
	private static List<Webpage> fetchPutDoc(Set<String> docUrls, String database, Fetcher fetcher, FetcherArgs fetcherArgs) throws IOException {
		List<Webpage> docs = new ArrayList<>();
		List<String> docsException = new ArrayList<>();
		int docUrlsSize = docUrls.size();
		logger.info("Fetch {} docs and put to database: {}", docUrlsSize, database);
		try (Database db = new Database(database)) {
			int i = 0;
			for (String docUrl : docUrls) {
				++i;
				logger.info("Fetch doc {}", PubFetcher.progress(i, docUrlsSize));
				Webpage doc = fetcher.initWebpage(docUrl);
				if (doc != null && fetcher.getWebpage(doc, fetcherArgs)) {
					if (doc.isFetchException()) {
						docsException.add(docUrl);
					} else {
						db.putDoc(doc);
						db.commit();
						docs.add(doc);
					}
				}
			}
			int docsExceptionSize = docsException.size();
			if (docsExceptionSize > 0) {
				logger.info("Refetch {} docs with exception", docsExceptionSize);
				i = 0;
				for (String docUrl : docsException) {
					++i;
					logger.info("Refetch doc {}", PubFetcher.progress(i, docsExceptionSize));
					Webpage doc = fetcher.initWebpage(docUrl);
					if (doc != null && fetcher.getWebpage(doc, fetcherArgs)) {
						db.putDoc(doc);
						db.commit();
						docs.add(doc);
					}
				}
			}
		}
		fetchedLog("docs", docUrlsSize, docs.size());
		return docs;
	}

	private static void fetchTimeMore(List<? extends DatabaseEntry<?>> entries, Long time) {
		logger.info("Filter entries with fetch time more than {}: before {}", timeHuman(time), entries.size());
		for (Iterator<? extends DatabaseEntry<?>> it = entries.iterator(); it.hasNext(); ) {
			if (it.next().getFetchTime() < time) it.remove();
		}
		logger.info("Filter entries with fetch time more than {}: after {}", timeHuman(time), entries.size());
	}
	private static void fetchTimeLess(List<? extends DatabaseEntry<?>> entries, Long time) {
		logger.info("Filter entries with fetch time less than {}: before {}", timeHuman(time), entries.size());
		for (Iterator<? extends DatabaseEntry<?>> it = entries.iterator(); it.hasNext(); ) {
			if (it.next().getFetchTime() > time) it.remove();
		}
		logger.info("Filter entries with fetch time less than {}: after {}", timeHuman(time), entries.size());
	}

	private static void retryCounter(List<? extends DatabaseEntry<?>> entries, List<Integer> counts) {
		logger.info("Filter entries with retry count {}: before {}", counts, entries.size());
		for (Iterator<? extends DatabaseEntry<?>> it = entries.iterator(); it.hasNext(); ) {
			if (!counts.contains(it.next().getRetryCounter())) it.remove();
		}
		logger.info("Filter entries with retry count {}: after {}", counts, entries.size());
	}
	private static void notRetryCounter(List<? extends DatabaseEntry<?>> entries, List<Integer> counts) {
		logger.info("Filter entries with retry count not {}: before {}", counts, entries.size());
		for (Iterator<? extends DatabaseEntry<?>> it = entries.iterator(); it.hasNext(); ) {
			if (counts.contains(it.next().getRetryCounter())) it.remove();
		}
		logger.info("Filter entries with retry count not {}: after {}", counts, entries.size());
	}

	private static void retryCounterMore(List<? extends DatabaseEntry<?>> entries, int count) {
		logger.info("Filter entries with retry count more than {}: before {}", count, entries.size());
		for (Iterator<? extends DatabaseEntry<?>> it = entries.iterator(); it.hasNext(); ) {
			if (it.next().getRetryCounter() <= count) it.remove();
		}
		logger.info("Filter entries with retry count more than {}: after {}", count, entries.size());
	}
	private static void retryCounterLess(List<? extends DatabaseEntry<?>> entries, int count) {
		logger.info("Filter entries with retry count less than {}: before {}", count, entries.size());
		for (Iterator<? extends DatabaseEntry<?>> it = entries.iterator(); it.hasNext(); ) {
			if (it.next().getRetryCounter() >= count) it.remove();
		}
		logger.info("Filter entries with retry count less than {}: after {}", count, entries.size());
	}

	private static void empty(List<? extends DatabaseEntry<?>> entries) {
		logger.info("Filter empty entries: before {}", entries.size());
		for (Iterator<? extends DatabaseEntry<?>> it = entries.iterator(); it.hasNext(); ) {
			if (!it.next().isEmpty()) it.remove();
		}
		logger.info("Filter empty entries: after {}", entries.size());
	}
	private static void notEmpty(List<? extends DatabaseEntry<?>> entries) {
		logger.info("Filter not empty entries: before {}", entries.size());
		for (Iterator<? extends DatabaseEntry<?>> it = entries.iterator(); it.hasNext(); ) {
			if (it.next().isEmpty()) it.remove();
		}
		logger.info("Filter not empty entries: after {}", entries.size());
	}

	private static void isFinal(List<? extends DatabaseEntry<?>> entries, FetcherArgs fetcherArgs) {
		logger.info("Filter final entries: before {}", entries.size());
		for (Iterator<? extends DatabaseEntry<?>> it = entries.iterator(); it.hasNext(); ) {
			if (!it.next().isFinal(fetcherArgs)) it.remove();
		}
		logger.info("Filter final entries: after {}", entries.size());
	}
	private static void notIsFinal(List<? extends DatabaseEntry<?>> entries, FetcherArgs fetcherArgs) {
		logger.info("Filter not final entries: before {}", entries.size());
		for (Iterator<? extends DatabaseEntry<?>> it = entries.iterator(); it.hasNext(); ) {
			if (it.next().isFinal(fetcherArgs)) it.remove();
		}
		logger.info("Filter not final entries: after {}", entries.size());
	}

	private static void totallyFinal(List<Publication> publications, FetcherArgs fetcherArgs) {
		logger.info("Filter totally final publications: before {}", publications.size());
		for (Iterator<Publication> it = publications.iterator(); it.hasNext(); ) {
			if (!it.next().isTotallyFinal(fetcherArgs)) it.remove();
		}
		logger.info("Filter totally final publications: after {}", publications.size());
	}
	private static void notTotallyFinal(List<Publication> publications, FetcherArgs fetcherArgs) {
		logger.info("Filter not totally final publications: before {}", publications.size());
		for (Iterator<Publication> it = publications.iterator(); it.hasNext(); ) {
			if (it.next().isTotallyFinal(fetcherArgs)) it.remove();
		}
		logger.info("Filter not totally final publications: after {}", publications.size());
	}

	private static void partEmpty(List<Publication> publications, List<PublicationPartName> names, boolean not) {
		logger.info("Filter publications with parts {} {}empty: before {}", names, not ? "not " : "", publications.size());
		for (Iterator<Publication> it = publications.iterator(); it.hasNext(); ) {
			Publication publication = it.next();
			for (PublicationPartName name : names) {
				boolean matches = publication.getPart(name).isEmpty();
				if (!not && !matches || not && matches) {
					it.remove();
					break;
				}
			}
		}
		logger.info("Filter publications with parts {} {}empty: after {}", names, not ? "not " : "", publications.size());
	}

	private static void partFinal(List<Publication> publications, List<PublicationPartName> names, FetcherArgs fetcherArgs, boolean not) {
		logger.info("Filter publications with parts {} {}final: before {}", names, not ? "not " : "", publications.size());
		for (Iterator<Publication> it = publications.iterator(); it.hasNext(); ) {
			Publication publication = it.next();
			for (PublicationPartName name : names) {
				boolean matches = publication.isPartFinal(name, fetcherArgs);
				if (!not && !matches || not && matches) {
					it.remove();
					break;
				}
			}
		}
		logger.info("Filter publications with parts {} {}final: after {}", names, not ? "not " : "", publications.size());
	}

	@SuppressWarnings({ "unchecked", "unused" })
	private static void partContent(ArrayList<Publication> publications, ArrayList<PublicationPartName> names, String regex, Boolean not) {
		logger.info("Filter publications with parts {} matching {}: before {}", names, regex, publications.size());
		for (Iterator<Publication> it = publications.iterator(); it.hasNext(); ) {
			Publication publication = it.next();
			for (PublicationPartName name : names) {
				PublicationPart publicationPart = publication.getPart(name);
				boolean matches = false;
				if (publicationPart instanceof PublicationPartString) {
					matches = ((PublicationPartString) publicationPart).getContent().matches(regex);
				} else {
					List<?> list = ((PublicationPartList<?>) publicationPart).getList();
					if (!list.isEmpty()) {
						if (list.get(0) instanceof String) {
							for (String s : (List<String>) list) {
								if (s.matches(regex)) {
									matches = true;
									break;
								}
							}
						} else if (list.get(0) instanceof MeshTerm) {
							for (MeshTerm t : (List<MeshTerm>) list) {
								if (t.getTerm().matches(regex)) {
									matches = true;
									break;
								}
							}
						} else {
							for (MinedTerm t : (List<MinedTerm>) list) {
								if (t.getTerm().matches(regex)) {
									matches = true;
									break;
								}
							}
						}
					}
				}
				if (!not && !matches || not && matches) {
					it.remove();
					break;
				}
			}
		}
		logger.info("Filter publications with parts {} matching {}: after {}", names, regex, publications.size());
	}

	@SuppressWarnings("unused")
	private static void partContentSize(ArrayList<Publication> publications, ArrayList<PublicationPartName> names, ArrayList<Integer> sizes, Boolean not) {
		logger.info("Filter publications with parts {} {}having size {}: before {}", names, not ? "not " : "", sizes, publications.size());
		for (Iterator<Publication> it = publications.iterator(); it.hasNext(); ) {
			Publication publication = it.next();
			for (PublicationPartName name : names) {
				boolean matches = sizes.contains(publication.getPart(name).getSize());
				if (!not && !matches || not && matches) {
					it.remove();
					break;
				}
			}
		}
		logger.info("Filter publications with parts {} {}having size {}: after {}", names, not ? "not " : "", sizes, publications.size());
	}

	@SuppressWarnings("unused")
	private static void partContentSizeMore(ArrayList<Publication> publications, ArrayList<PublicationPartName> names, Integer size) {
		logger.info("Filter publications with parts {} having size more than {}: before {}", names, size, publications.size());
		for (Iterator<Publication> it = publications.iterator(); it.hasNext(); ) {
			Publication publication = it.next();
			for (PublicationPartName name : names) {
				boolean matches = publication.getPart(name).getSize() > size;
				if (!matches) {
					it.remove();
					break;
				}
			}
		}
		logger.info("Filter publications with parts {} having size more than {}: after {}", names, size, publications.size());
	}
	@SuppressWarnings("unused")
	private static void partContentSizeLess(ArrayList<Publication> publications, ArrayList<PublicationPartName> names, Integer size) {
		logger.info("Filter publications with parts {} having size less than {}: before {}", names, size, publications.size());
		for (Iterator<Publication> it = publications.iterator(); it.hasNext(); ) {
			Publication publication = it.next();
			for (PublicationPartName name : names) {
				boolean matches = publication.getPart(name).getSize() < size;
				if (!matches) {
					it.remove();
					break;
				}
			}
		}
		logger.info("Filter publications with parts {} having size less than {}: after {}", names, size, publications.size());
	}

	@SuppressWarnings("unused")
	private static void partType(ArrayList<Publication> publications, ArrayList<PublicationPartName> names, ArrayList<PublicationPartType> types, Boolean not) {
		logger.info("Filter publications with parts {} {}having type {}: before {}", names, not ? "not " : "", types, publications.size());
		for (Iterator<Publication> it = publications.iterator(); it.hasNext(); ) {
			Publication publication = it.next();
			for (PublicationPartName name : names) {
				boolean matches = types.contains(publication.getPart(name).getType());
				if (!not && !matches || not && matches) {
					it.remove();
					break;
				}
			}
		}
		logger.info("Filter publications with parts {} {}having type {}: after {}", names, not ? "not " : "", types, publications.size());
	}

	@SuppressWarnings("unused")
	private static void partTypeEquivalent(ArrayList<Publication> publications, ArrayList<PublicationPartName> names, PublicationPartType type) {
		logger.info("Filter publications with parts {} having type equivalent to {}: before {}", names, type, publications.size());
		for (Iterator<Publication> it = publications.iterator(); it.hasNext(); ) {
			Publication publication = it.next();
			for (PublicationPartName name : names) {
				if (!publication.getPart(name).getType().isEquivalent(type)) {
					it.remove();
					break;
				}
			}
		}
		logger.info("Filter publications with parts {} having type equivalent to {}: after {}", names, type, publications.size());
	}
	@SuppressWarnings("unused")
	private static void partTypeMore(ArrayList<Publication> publications, ArrayList<PublicationPartName> names, PublicationPartType type) {
		logger.info("Filter publications with parts {} having type more than {}: before {}", names, type, publications.size());
		for (Iterator<Publication> it = publications.iterator(); it.hasNext(); ) {
			Publication publication = it.next();
			for (PublicationPartName name : names) {
				if (!publication.getPart(name).getType().isBetterThan(type)) {
					it.remove();
					break;
				}
			}
		}
		logger.info("Filter publications with parts {} having type more than {}: after {}", names, type, publications.size());
	}
	@SuppressWarnings("unused")
	private static void partTypeLess(ArrayList<Publication> publications, ArrayList<PublicationPartName> names, PublicationPartType type) {
		logger.info("Filter publications with parts {} having type less than {}: before {}", names, type, publications.size());
		for (Iterator<Publication> it = publications.iterator(); it.hasNext(); ) {
			Publication publication = it.next();
			for (PublicationPartName name : names) {
				if (publication.getPart(name).getType().isEquivalent(type) || publication.getPart(name).getType().isBetterThan(type)) {
					it.remove();
					break;
				}
			}
		}
		logger.info("Filter publications with parts {} having type less than {}: after {}", names, type, publications.size());
	}

	@SuppressWarnings("unused")
	private static void partUrl(ArrayList<Publication> publications, ArrayList<PublicationPartName> names, String regex) {
		logger.info("Filter publications with parts {} having url matching {}: before {}", names, regex, publications.size());
		for (Iterator<Publication> it = publications.iterator(); it.hasNext(); ) {
			Publication publication = it.next();
			for (PublicationPartName name : names) {
				if (!publication.getPart(name).getUrl().matches(regex)) {
					it.remove();
					break;
				}
			}
		}
		logger.info("Filter publications with parts {} having url matching {}: after {}", names, regex, publications.size());
	}

	@SuppressWarnings("unused")
	private static void partUrlHost(ArrayList<Publication> publications, ArrayList<PublicationPartName> names, ArrayList<String> hosts, Boolean not) {
		logger.info("Filter publications with parts {} {}having url of host {}: before {}", names, not ? "not " : "", hosts, publications.size());
		for (Iterator<Publication> it = publications.iterator(); it.hasNext(); ) {
			Publication publication = it.next();
			for (PublicationPartName name : names) {
				boolean matches = false;
				try {
					if (hosts.contains(new URL(publication.getPart(name).getUrl()).getHost())) {
						matches = true;
					}
				} catch (MalformedURLException e) {
				}
				if (!not && !matches || not && matches) {
					it.remove();
					break;
				}
			}
		}
		logger.info("Filter publications with parts {} {}having url of host {}: after {}", names, not ? "not " : "", hosts, publications.size());
	}

	@SuppressWarnings("unused")
	private static void partTimeMore(ArrayList<Publication> publications, ArrayList<PublicationPartName> names, Long time) {
		logger.info("Filter publications with parts {} having time more than {}: before {}", names, timeHuman(time), publications.size());
		for (Iterator<Publication> it = publications.iterator(); it.hasNext(); ) {
			Publication publication = it.next();
			for (PublicationPartName name : names) {
				if (publication.getPart(name).getTimestamp() < time) {
					it.remove();
					break;
				}
			}
		}
		logger.info("Filter publications with parts {} having time more than {}: after {}", names, timeHuman(time), publications.size());
	}
	@SuppressWarnings("unused")
	private static void partTimeLess(ArrayList<Publication> publications, ArrayList<PublicationPartName> names, Long time) {
		logger.info("Filter publications with parts {} having time less than {}: before {}", names, timeHuman(time), publications.size());
		for (Iterator<Publication> it = publications.iterator(); it.hasNext(); ) {
			Publication publication = it.next();
			for (PublicationPartName name : names) {
				if (publication.getPart(name).getTimestamp() > time) {
					it.remove();
					break;
				}
			}
		}
		logger.info("Filter publications with parts {} having time less than {}: after {}", names, timeHuman(time), publications.size());
	}

	private static void oa(List<Publication> publications) {
		logger.info("Filter publications that are Open Access: before {}", publications.size());
		for (Iterator<Publication> it = publications.iterator(); it.hasNext(); ) {
			if (!it.next().isOA()) it.remove();
		}
		logger.info("Filter publications that are Open Access: after {}", publications.size());
	}
	private static void notOa(List<Publication> publications) {
		logger.info("Filter publications that are not Open Access: before {}", publications.size());
		for (Iterator<Publication> it = publications.iterator(); it.hasNext(); ) {
			if (it.next().isOA()) it.remove();
		}
		logger.info("Filter publications that are not Open Access: after {}", publications.size());
	}

	// TODO journalTitle, pubDate, citationsCount, citationsTimestamp, correspAuthor

	private static void visited(List<Publication> publications, String regex) {
		logger.info("Filter publications with visited site matching {}: before {}", regex, publications.size());
		for (Iterator<Publication> it = publications.iterator(); it.hasNext(); ) {
			boolean matches = false;
			for (Link link : it.next().getVisitedSites()) {
				if (link.getUrl().toString().matches(regex)) {
					matches = true;
					break;
				}
			}
			if (!matches) {
				it.remove();
			}
		}
		logger.info("Filter publications with visited site matching {}: after {}", regex, publications.size());
	}

	private static void visitedHost(List<Publication> publications, List<String> hosts, boolean not) {
		logger.info("Filter publications with {}visited site of host {}: before {}", not ? "no " : "", hosts, publications.size());
		for (Iterator<Publication> it = publications.iterator(); it.hasNext(); ) {
			boolean matches = false;
			for (Link link : it.next().getVisitedSites()) {
				if (hosts.contains(link.getUrl().getHost())) {
					matches = true;
					break;
				}
			}
			if (!not && !matches || not && matches) {
				it.remove();
			}
		}
		logger.info("Filter publications with {}visited site of host {}: after {}", not ? "no " : "", hosts, publications.size());
	}

	private static void visitedType(List<Publication> publications, List<PublicationPartType> types, boolean not) {
		logger.info("Filter publications with {}visited site of type {}: before {}", not ? "no " : "", types, publications.size());
		for (Iterator<Publication> it = publications.iterator(); it.hasNext(); ) {
			boolean matches = false;
			for (Link link : it.next().getVisitedSites()) {
				if (types.contains(link.getType())) {
					matches = true;
					break;
				}
			}
			if (!not && !matches || not && matches) {
				it.remove();
			}
		}
		logger.info("Filter publications with {}visited site of type {}: after {}", not ? "no " : "", types, publications.size());
	}

	private static void visitedFrom(List<Publication> publications, String regex) {
		logger.info("Filter publications with visited site from URL matching {}: before {}", regex, publications.size());
		for (Iterator<Publication> it = publications.iterator(); it.hasNext(); ) {
			boolean matches = false;
			for (Link link : it.next().getVisitedSites()) {
				if (link.getFrom().matches(regex)) {
					matches = true;
					break;
				}
			}
			if (!matches) {
				it.remove();
			}
		}
		logger.info("Filter publications with visited site from URL matching {}: after {}", regex, publications.size());
	}

	private static void visitedFromHost(List<Publication> publications, List<String> hosts, boolean not) {
		logger.info("Filter publications with {}visited site from URL of host {}: before {}", not ? "no " : "", hosts, publications.size());
		for (Iterator<Publication> it = publications.iterator(); it.hasNext(); ) {
			boolean matches = false;
			for (Link link : it.next().getVisitedSites()) {
				try {
					if (hosts.contains(new URL(link.getFrom()).getHost())) {
						matches = true;
						break;
					}
				} catch (MalformedURLException e) {
				}
			}
			if (!not && !matches || not && matches) {
				it.remove();
			}
		}
		logger.info("Filter publications with {}visited site from URL of host {}: after {}", not ? "no " : "", hosts, publications.size());
	}

	private static void visitedSize(List<Publication> publications, List<Integer> sizes) {
		logger.info("Filter publications with visited sites size {}: before {}", sizes, publications.size());
		for (Iterator<Publication> it = publications.iterator(); it.hasNext(); ) {
			if (!sizes.contains(it.next().getVisitedSites().size())) it.remove();
		}
		logger.info("Filter publications with visited sites size {}: after {}", sizes, publications.size());
	}
	private static void notVisitedSize(List<Publication> publications, List<Integer> sizes) {
		logger.info("Filter publications with visited sites size not {}: before {}", sizes, publications.size());
		for (Iterator<Publication> it = publications.iterator(); it.hasNext(); ) {
			if (sizes.contains(it.next().getVisitedSites().size())) it.remove();
		}
		logger.info("Filter publications with visited sites size not {}: after {}", sizes, publications.size());
	}

	private static void visitedSizeMore(List<Publication> publications, int size) {
		logger.info("Filter publications with visited sites size more than {}: before {}", size, publications.size());
		for (Iterator<Publication> it = publications.iterator(); it.hasNext(); ) {
			if (it.next().getVisitedSites().size() <= size) it.remove();
		}
		logger.info("Filter publications with visited sites size more than {}: after {}", size, publications.size());
	}
	private static void visitedSizeLess(List<Publication> publications, int size) {
		logger.info("Filter publications with visited sites size less than {}: before {}", size, publications.size());
		for (Iterator<Publication> it = publications.iterator(); it.hasNext(); ) {
			if (it.next().getVisitedSites().size() >= size) it.remove();
		}
		logger.info("Filter publications with visited sites size less than {}: after {}", size, publications.size());
	}

	private static void startUrl(List<Webpage> webpages, String regex) {
		logger.info("Filter webpages with start URL matching {}: before {}", regex, webpages.size());
		for (Iterator<Webpage> it = webpages.iterator(); it.hasNext(); ) {
			if (!it.next().getStartUrl().matches(regex)) it.remove();
		}
		logger.info("Filter webpages with start URL matching {}: after {}", regex, webpages.size());
	}

	private static void startUrlHost(List<Webpage> webpages, List<String> hosts, boolean not) {
		logger.info("Filter webpages with start URL {}of host {}: before {}", not ? "not " : "", hosts, webpages.size());
		for (Iterator<Webpage> it = webpages.iterator(); it.hasNext(); ) {
			boolean matches = false;
			try {
				if (hosts.contains(new URL(it.next().getStartUrl()).getHost())) {
					matches = true;
				}
			} catch (MalformedURLException e) {
			}
			if (!not && !matches || not && matches) {
				it.remove();
			}
		}
		logger.info("Filter webpages with start URL {}of host {}: after {}", not ? "not " : "", hosts, webpages.size());
	}

	private static void finalUrl(List<Webpage> webpages, String regex) {
		logger.info("Filter webpages with final URL matching {}: before {}", regex, webpages.size());
		for (Iterator<Webpage> it = webpages.iterator(); it.hasNext(); ) {
			if (!it.next().getFinalUrl().matches(regex)) it.remove();
		}
		logger.info("Filter webpages with final URL matching {}: after {}", regex, webpages.size());
	}

	private static void finalUrlHost(List<Webpage> webpages, List<String> hosts, boolean not) {
		logger.info("Filter webpages with final URL {}of host {}: before {}", not ? "not " : "", hosts, webpages.size());
		for (Iterator<Webpage> it = webpages.iterator(); it.hasNext(); ) {
			boolean matches = false;
			try {
				if (hosts.contains(new URL(it.next().getFinalUrl()).getHost())) {
					matches = true;
				}
			} catch (MalformedURLException e) {
			}
			if (!not && !matches || not && matches) {
				it.remove();
			}
		}
		logger.info("Filter webpages with final URL {}of host {}: after {}", not ? "not " : "", hosts, webpages.size());
	}

	private static void contentType(List<Webpage> webpages, String regex) {
		logger.info("Filter webpages with content type matching {}: before {}", regex, webpages.size());
		for (Iterator<Webpage> it = webpages.iterator(); it.hasNext(); ) {
			if (!it.next().getContentType().matches(regex)) it.remove();
		}
		logger.info("Filter webpages with content type matching {}: after {}", regex, webpages.size());
	}

	private static void statusCode(List<Webpage> webpages, List<Integer> codes) {
		logger.info("Filter webpages with status code {}: before {}", codes, webpages.size());
		for (Iterator<Webpage> it = webpages.iterator(); it.hasNext(); ) {
			if (!codes.contains(it.next().getStatusCode())) it.remove();
		}
		logger.info("Filter webpages with status code {}: after {}", codes, webpages.size());
	}
	private static void notStatusCode(List<Webpage> webpages, List<Integer> codes) {
		logger.info("Filter webpages with status code not {}: before {}", codes, webpages.size());
		for (Iterator<Webpage> it = webpages.iterator(); it.hasNext(); ) {
			if (codes.contains(it.next().getStatusCode())) it.remove();
		}
		logger.info("Filter webpages with status code not {}: after {}", codes, webpages.size());
	}

	private static void title(List<Webpage> webpages, String regex) {
		logger.info("Filter webpages with title matching {}: before {}", regex, webpages.size());
		for (Iterator<Webpage> it = webpages.iterator(); it.hasNext(); ) {
			if (!it.next().getTitle().matches(regex)) it.remove();
		}
		logger.info("Filter webpages with title matching {}: after {}", regex, webpages.size());
	}

	private static void titleMore(List<Webpage> webpages, int count) {
		logger.info("Filter webpages with title length more than {}: before {}", count, webpages.size());
		for (Iterator<Webpage> it = webpages.iterator(); it.hasNext(); ) {
			if (it.next().getTitle().length() <= count) it.remove();
		}
		logger.info("Filter webpages with title length more than {}: after {}", count, webpages.size());
	}
	private static void titleLess(List<Webpage> webpages, int count) {
		logger.info("Filter webpages with title length less than {}: before {}", count, webpages.size());
		for (Iterator<Webpage> it = webpages.iterator(); it.hasNext(); ) {
			if (it.next().getTitle().length() >= count) it.remove();
		}
		logger.info("Filter webpages with title length less than {}: after {}", count, webpages.size());
	}

	private static void content(List<Webpage> webpages, String regex) {
		logger.info("Filter webpages with content matching {}: before {}", regex, webpages.size());
		for (Iterator<Webpage> it = webpages.iterator(); it.hasNext(); ) {
			if (!it.next().getContent().matches(regex)) it.remove();
		}
		logger.info("Filter webpages with content matching {}: after {}", regex, webpages.size());
	}

	private static void contentMore(List<Webpage> webpages, int count) {
		logger.info("Filter webpages with content length more than {}: before {}", count, webpages.size());
		for (Iterator<Webpage> it = webpages.iterator(); it.hasNext(); ) {
			if (it.next().getContent().length() <= count) it.remove();
		}
		logger.info("Filter webpages with content length more than {}: after {}", count, webpages.size());
	}
	private static void contentLess(List<Webpage> webpages, int count) {
		logger.info("Filter webpages with content length less than {}: before {}", count, webpages.size());
		for (Iterator<Webpage> it = webpages.iterator(); it.hasNext(); ) {
			if (it.next().getContent().length() >= count) it.remove();
		}
		logger.info("Filter webpages with content length less than {}: after {}", count, webpages.size());
	}

	private static void contentTimeMore(List<Webpage> webpages, Long time) {
		logger.info("Filter webpages with content time more than {}: before {}", timeHuman(time), webpages.size());
		for (Iterator<Webpage> it = webpages.iterator(); it.hasNext(); ) {
			if (it.next().getContentTime() < time) it.remove();
		}
		logger.info("Filter webpages with content time more than {}: after {}", timeHuman(time), webpages.size());
	}
	private static void contentTimeLess(List<Webpage> webpages, Long time) {
		logger.info("Filter webpages with content time less than {}: before {}", timeHuman(time), webpages.size());
		for (Iterator<Webpage> it = webpages.iterator(); it.hasNext(); ) {
			if (it.next().getContentTime() > time) it.remove();
		}
		logger.info("Filter webpages with content time less than {}: after {}", timeHuman(time), webpages.size());
	}

	private static void grep(List<? extends DatabaseEntry<?>> entries, String regex) {
		logger.info("Filter entries matching {}: before {}", regex, entries.size());
		Pattern pattern = Pattern.compile(regex);
		for (Iterator<? extends DatabaseEntry<?>> it = entries.iterator(); it.hasNext(); ) {
			if (!pattern.matcher(it.next().toStringPlain()).find()) it.remove(); // TODO
		}
		logger.info("Filter entries matching {}: after {}", regex, entries.size());
	}

	private static <T extends DatabaseEntry<T>> void asc(List<T> entries) {
		logger.info("Sort {} entries in ascending order", entries.size());
		Collections.sort(entries);
	}
	private static <T extends DatabaseEntry<T>> void desc(List<T> entries) {
		logger.info("Sort {} entries in descending order", entries.size());
		Collections.sort(entries, Collections.reverseOrder());
	}

	private static <T extends DatabaseEntry<T>> void ascTime(List<T> entries) {
		logger.info("Sort {} entries in ascending order by fetch time", entries.size());
		Collections.sort(entries, (a, b) -> a.getFetchTime() < b.getFetchTime() ? -1 : a.getFetchTime() > b.getFetchTime() ? 1 : 0);
	}
	private static <T extends DatabaseEntry<T>> void descTime(List<T> entries) {
		logger.info("Sort {} entries in descending order by fetch time", entries.size());
		Collections.sort(entries, (a, b) -> a.getFetchTime() < b.getFetchTime() ? 1 : a.getFetchTime() > b.getFetchTime() ? -1 : 0);
	}

	private static Map<String, Integer> topHostsPub(List<Publication> publications, Scrape scrape) {
		if (scrape == null) {
			logger.info("Get top hosts from {} publications", publications.size());
		} else {
			logger.info("Get top hosts without scrape rules from {} publications", publications.size());
		}
		Map<String, Integer> hosts = new LinkedHashMap<>();
		for (Publication publication : publications) {
			for (Link link : publication.getVisitedSites()) {
				if (scrape != null && scrape.getSite(link.getUrl().toString()) != null) {
					continue;
				}
				String host = link.getUrl().getHost();
				Integer count = hosts.get(host);
				if (count != null) {
					hosts.put(host, count + 1);
				} else {
					hosts.put(host, 1);
				}
			}
		}
		Map<String, Integer> topHostsPub = hosts.entrySet().stream()
			.sorted((c1, c2) -> c2.getValue().compareTo(c1.getValue()))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (k, v) -> { throw new AssertionError(); }, LinkedHashMap::new));
		logger.info("Got {} top hosts", topHostsPub.size());
		return topHostsPub;
	}

	private static Map<String, Integer> topHostsWeb(List<Webpage> webpages, Scrape scrape) {
		if (scrape == null) {
			logger.info("Get top hosts from {} webpages", webpages.size());
		} else {
			logger.info("Get top hosts without scrape rules from {} webpages", webpages.size());
		}
		Map<String, Integer> hosts = new LinkedHashMap<>();
		for (Webpage webpage : webpages) {
			try {
				if (scrape != null && scrape.getSite(webpage.getFinalUrl()) != null) {
					continue;
				}
				String host = new URL(webpage.getFinalUrl()).getHost();
				Integer count = hosts.get(host);
				if (count != null) {
					hosts.put(host, count + 1);
				} else {
					hosts.put(host, 1);
				}
			} catch (MalformedURLException e) {
				logger.error("Malformed URL: {}", webpage.getFinalUrl());
			}
		}
		Map<String, Integer> topHostsWeb = hosts.entrySet().stream()
			.sorted((c1, c2) -> c2.getValue().compareTo(c1.getValue()))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (k, v) -> { throw new AssertionError(); }, LinkedHashMap::new));
		logger.info("Got {} top hosts", topHostsWeb.size());
		return topHostsWeb;
	}

	private static void head(Collection<?> entries, int count) {
		logger.info("Limit to {} first entries", count);
		int i = 0;
		for (Iterator<?> it = entries.iterator(); it.hasNext(); ++i) {
			it.next();
			if (i >= count) it.remove();
		}
	}
	private static void tail(Collection<?> entries, int count) {
		logger.info("Limit to {} last entries", count);
		int i = 0;
		int n = entries.size();
		for (Iterator<?> it = entries.iterator(); it.hasNext(); ++i) {
			it.next();
			if (n - i > count) it.remove();
		}
	}

	private static void putPub(List<Publication> publications, String database) throws IOException {
		logger.info("Put {} publications to database: {}", publications.size(), database);
		try (Database db = new Database(database)) {
			for (Publication publication : publications) {
				db.putPublication(publication);
				db.commit();
			}
		}
		logger.info("Put publications: success");
	}
	private static void putWeb(List<Webpage> webpages, String database) throws IOException {
		logger.info("Put {} webpages to database: {}", webpages.size(), database);
		try (Database db = new Database(database)) {
			for (Webpage webpage : webpages) {
				db.putWebpage(webpage);
				db.commit();
			}
		}
		logger.info("Put webpages: success");
	}
	private static void putDoc(List<Webpage> docs, String database) throws IOException {
		logger.info("Put {} docs to database: {}", docs.size(), database);
		try (Database db = new Database(database)) {
			for (Webpage doc : docs) {
				db.putWebpage(doc);
				db.commit();
			}
		}
		logger.info("Put docs: success");
	}

	private static void removePub(List<Publication> publications, String database) throws IOException {
		logger.info("Remove {} publications from database: {}", publications.size(), database);
		int fail = 0;
		try (Database db = new Database(database)) {
			for (Publication publication : publications) {
				if (!db.removePublication(publication)) {
					logger.warn("Failed to remove publication: {}", publication.toStringId());
					++fail;
				} else db.commit();
			}
		}
		if (fail > 0) logger.warn("Failed to remove {} publications", fail);
		else logger.info("Remove publications: success");
	}
	private static void removeWeb(List<Webpage> webpages, String database) throws IOException {
		logger.info("Remove {} webpages from database: {}", webpages.size(), database);
		int fail = 0;
		try (Database db = new Database(database)) {
			for (Webpage webpage : webpages) {
				if (!db.removeWebpage(webpage)) {
					logger.warn("Failed to remove webpage: {}", webpage.toStringId());
					++fail;
				} else db.commit();
			}
		}
		if (fail > 0) logger.warn("Failed to remove {} webpages", fail);
		else logger.info("Remove webpages: success");
	}
	private static void removeDoc(List<Webpage> docs, String database) throws IOException {
		logger.info("Remove {} docs from database: {}", docs.size(), database);
		int fail = 0;
		try (Database db = new Database(database)) {
			for (Webpage doc : docs) {
				if (!db.removeDoc(doc)) {
					logger.warn("Failed to remove doc: {}", doc.toStringId());
					++fail;
				} else db.commit();
			}
		}
		if (fail > 0) logger.warn("Failed to remove {} docs", fail);
		else logger.info("Remove docs: success");
	}

	private static String toStringPubParts(Publication publication, boolean plain, boolean html, List<PublicationPartName> parts, boolean idOnly) {
		List<String> pubString = new ArrayList<>();
		if (plain) {
			if (html) {
				if (idOnly) {
					pubString.add(PublicationIds.toStringHtml(
						parts.contains(PublicationPartName.pmid) ? publication.getPmid().getContent() : "",
						parts.contains(PublicationPartName.pmcid) ? publication.getPmcid().getContent() : "",
						parts.contains(PublicationPartName.doi) ? publication.getDoi().getContent() : "", true));
				} else if (parts.contains(PublicationPartName.pmid)
						|| parts.contains(PublicationPartName.pmcid)
						|| parts.contains(PublicationPartName.doi)) {
					pubString.add(PublicationIds.toStringHtml(
						parts.contains(PublicationPartName.pmid) ? publication.getPmid().getContent() : "",
						parts.contains(PublicationPartName.pmcid) ? publication.getPmcid().getContent() : "",
						parts.contains(PublicationPartName.doi) ? publication.getDoi().getContent() : "", false));
				}
				if (parts.contains(PublicationPartName.title)) pubString.add(publication.getTitle().toStringPlainHtml());
				if (parts.contains(PublicationPartName.keywords)) pubString.add(publication.getKeywords().toStringPlainHtml());
				if (parts.contains(PublicationPartName.mesh)) pubString.add(publication.getMeshTerms().toStringPlainHtml());
				if (parts.contains(PublicationPartName.efo)) pubString.add(publication.getEfoTerms().toStringPlainHtml());
				if (parts.contains(PublicationPartName.go)) pubString.add(publication.getGoTerms().toStringPlainHtml());
				if (parts.contains(PublicationPartName.theAbstract)) pubString.add(publication.getAbstract().toStringPlainHtml());
				if (parts.contains(PublicationPartName.fulltext)) pubString.add(publication.getFulltext().toStringPlainHtml());
			} else {
				if (idOnly) {
					pubString.add(PublicationIds.toString(
						parts.contains(PublicationPartName.pmid) ? publication.getPmid().getContent() : "",
						parts.contains(PublicationPartName.pmcid) ? publication.getPmcid().getContent() : "",
						parts.contains(PublicationPartName.doi) ? publication.getDoi().getContent() : "", true));
				} else if (parts.contains(PublicationPartName.pmid)
						|| parts.contains(PublicationPartName.pmcid)
						|| parts.contains(PublicationPartName.doi)) {
					pubString.add(PublicationIds.toString(
						parts.contains(PublicationPartName.pmid) ? publication.getPmid().getContent() : "",
						parts.contains(PublicationPartName.pmcid) ? publication.getPmcid().getContent() : "",
						parts.contains(PublicationPartName.doi) ? publication.getDoi().getContent() : "", false));
				}
				if (parts.contains(PublicationPartName.title)) pubString.add(publication.getTitle().toStringPlain());
				if (parts.contains(PublicationPartName.keywords)) pubString.add(publication.getKeywords().toStringPlain());
				if (parts.contains(PublicationPartName.mesh)) pubString.add(publication.getMeshTerms().toStringPlain());
				if (parts.contains(PublicationPartName.efo)) pubString.add(publication.getEfoTerms().toStringPlain());
				if (parts.contains(PublicationPartName.go)) pubString.add(publication.getGoTerms().toStringPlain());
				if (parts.contains(PublicationPartName.theAbstract)) pubString.add(publication.getAbstract().toStringPlain());
				if (parts.contains(PublicationPartName.fulltext)) pubString.add(publication.getFulltext().toStringPlain());
			}
		} else {
			if (html) {
				if (parts.contains(PublicationPartName.pmid)) pubString.add(publication.getPmid().toStringHtml(""));
				if (parts.contains(PublicationPartName.pmcid)) pubString.add(publication.getPmcid().toStringHtml(""));
				if (parts.contains(PublicationPartName.doi)) pubString.add(publication.getDoi().toStringHtml(""));
				if (parts.contains(PublicationPartName.title)) pubString.add(publication.getTitle().toStringHtml(""));
				if (parts.contains(PublicationPartName.keywords)) pubString.add(publication.getKeywords().toStringHtml(""));
				if (parts.contains(PublicationPartName.mesh)) pubString.add(publication.getMeshTerms().toStringHtml(""));
				if (parts.contains(PublicationPartName.efo)) pubString.add(publication.getEfoTerms().toStringHtml(""));
				if (parts.contains(PublicationPartName.go)) pubString.add(publication.getGoTerms().toStringHtml(""));
				if (parts.contains(PublicationPartName.theAbstract)) pubString.add(publication.getAbstract().toStringHtml(""));
				if (parts.contains(PublicationPartName.fulltext)) pubString.add(publication.getFulltext().toStringHtml(""));
			} else {
				if (parts.contains(PublicationPartName.pmid)) pubString.add(publication.getPmid().toString());
				if (parts.contains(PublicationPartName.pmcid)) pubString.add(publication.getPmcid().toString());
				if (parts.contains(PublicationPartName.doi)) pubString.add(publication.getDoi().toString());
				if (parts.contains(PublicationPartName.title)) pubString.add(publication.getTitle().toString());
				if (parts.contains(PublicationPartName.keywords)) pubString.add(publication.getKeywords().toString());
				if (parts.contains(PublicationPartName.mesh)) pubString.add(publication.getMeshTerms().toString());
				if (parts.contains(PublicationPartName.efo)) pubString.add(publication.getEfoTerms().toString());
				if (parts.contains(PublicationPartName.go)) pubString.add(publication.getGoTerms().toString());
				if (parts.contains(PublicationPartName.theAbstract)) pubString.add(publication.getAbstract().toString());
				if (parts.contains(PublicationPartName.fulltext)) pubString.add(publication.getFulltext().toString());
			}
		}
		return pubString.stream().collect(Collectors.joining("\n\n"));
	}

	private static <T extends DatabaseEntry<T>> void print(PrintStream ps, List<T> entries, boolean plain, boolean html, List<PublicationPartName> parts) throws IOException {
		int i = 0;
		int n = entries.size();
		boolean idOnly = false;
		if (parts != null
				&& (parts.contains(PublicationPartName.pmid) || parts.contains(PublicationPartName.pmcid) || parts.contains(PublicationPartName.doi))
				&& !parts.contains(PublicationPartName.title)
				&& !parts.contains(PublicationPartName.keywords) && !parts.contains(PublicationPartName.mesh)
				&& !parts.contains(PublicationPartName.efo) && !parts.contains(PublicationPartName.go)
				&& !parts.contains(PublicationPartName.theAbstract) && !parts.contains(PublicationPartName.fulltext)) {
			idOnly = true;
		}
		if (idOnly && html) {
			ps.println("<table border=\"1\">");
		}
		for (T entry : entries) {
			if (parts != null && entry instanceof Publication) {
				ps.println(toStringPubParts((Publication) entry, plain, html, parts, idOnly));
			} else if (plain) {
				if (html) ps.println(entry.toStringPlainHtml(""));
				else ps.println(entry.toStringPlain());
			} else {
				if (html) ps.println(entry.toStringHtml(""));
				else ps.println(entry.toString());
			}
			++i;
			if (i < n) {
				if (!(plain && parts != null && entry instanceof Publication
						&& (parts.size() == 1 && parts.get(0) != PublicationPartName.theAbstract && parts.get(0) != PublicationPartName.fulltext
						|| (parts.size() == 2 && (parts.get(0) == PublicationPartName.pmid || parts.get(0) == PublicationPartName.pmcid || parts.get(0) == PublicationPartName.doi)
							&& (parts.get(1) == PublicationPartName.pmid || parts.get(1) == PublicationPartName.pmcid || parts.get(1) == PublicationPartName.doi))
						|| (parts.size() == 3 && (parts.get(0) == PublicationPartName.pmid || parts.get(0) == PublicationPartName.pmcid || parts.get(0) == PublicationPartName.doi)
							&& (parts.get(1) == PublicationPartName.pmid || parts.get(1) == PublicationPartName.pmcid || parts.get(1) == PublicationPartName.doi)
							&& (parts.get(2) == PublicationPartName.pmid || parts.get(2) == PublicationPartName.pmcid || parts.get(2) == PublicationPartName.doi))))) {
					if (html) ps.println("\n<hr>\n");
					else ps.println("\n -----------------------------------------------------------------------------\n");
				}
			}
		}
		if (idOnly && html) {
			ps.println("</table>");
		}
	}

	private static <T extends DatabaseEntry<T>> void out(List<T> entries, boolean plain, boolean html, List<PublicationPartName> parts) throws IOException {
		if (entries.size() == 0) return;
		logger.info("Output {} entries{}{}{}",
			entries.size(), plain ? " without metadata" : "", parts != null ? " with parts " + parts : "", html ? " in HTML" : "");
		print(System.out, entries, plain, html, parts);
	}
	private static <T extends DatabaseEntry<T>> void txt(List<T> entries, boolean plain, boolean html, List<PublicationPartName> parts, String txt) throws IOException {
		logger.info("Output {} entries to file {}{}{}{}",
				entries.size(), txt, plain ? " without metadata" : "", parts != null ? " with parts " + parts : "", html ? " in HTML" : "");
		try (PrintStream ps = new PrintStream(new BufferedOutputStream(Files.newOutputStream(PubFetcher.outputPath(txt))), true, "UTF-8")) {
			if (entries.size() == 0) return;
			print(ps, entries, plain, html, parts);
		}
	}

	private static void printTopHosts(PrintStream ps, Map<String, Integer> topHosts, boolean html) throws IOException {
		if (html) ps.println("<ul>");
		for (Map.Entry<String, Integer> topHost : topHosts.entrySet()) {
			if (html) ps.println("<li value=\"" + topHost.getValue() + "\">" + PubFetcher.escapeHtml(topHost.getKey()) + "</li>");
			else ps.println(topHost.getKey() + "\t" + topHost.getValue());
		}
		if (html) ps.println("</ul>");
	}
	private static void outTopHosts(Map<String, Integer> topHosts, boolean html) throws IOException {
		if (topHosts.size() == 0) return;
		logger.info("Output {} top hosts{}", topHosts.size(), html ? " in HTML" : "");
		printTopHosts(System.out, topHosts, html);
	}
	private static void txtTopHosts(Map<String, Integer> topHosts, boolean html, String txt) throws IOException {
		logger.info("Output {} top hosts to file {}{}", topHosts.size(), txt, html ? " in HTML" : "");
		try (PrintStream ps = new PrintStream(new BufferedOutputStream(Files.newOutputStream(PubFetcher.outputPath(txt))), true, "UTF-8")) {
			if (topHosts.size() == 0) return;
			printTopHosts(ps, topHosts, html);
		}
	}

	private static void count(String label, Collection<?> entries) {
		System.out.println(label + ": " + entries.size());
	}

	private static void partTable(List<Publication> publications) {
		System.out.print("type");
		for (PublicationPartName name : PublicationPartName.values()) {
			System.out.print("," + name);
		}
		System.out.println(",total");
		for (PublicationPartType type : PublicationPartType.values()) {
			System.out.print(type);
			long countTotal = 0;
			for (PublicationPartName name : PublicationPartName.values()) {
				long count = publications.stream().filter(p -> p.getPart(name).getType() == type).count();
				System.out.print("," + count);
				countTotal += count;
			}
			System.out.println("," + countTotal);
		}
	}

	private static void checkPartArg(String partArgName, PubFetcherArgs args) throws ReflectiveOperationException {
		String partArgPartName = partArgName + "Part";
		Field partArg = PubFetcherArgs.class.getDeclaredField(partArgName);
		Field partArgPart = PubFetcherArgs.class.getDeclaredField(partArgPartName);
		Object partArgObject = partArg.get(args);
		Object partArgPartObject = partArgPart.get(args);
		if (partArgObject == null && partArgPartObject != null || partArgObject != null && partArgPartObject == null) {
			String partArgParameter = Arrays.toString(PubFetcherArgs.class.getDeclaredField(partArgName).getAnnotation(Parameter.class).names());
			String partArgPartParameter = Arrays.toString(PubFetcherArgs.class.getDeclaredField(partArgPartName).getAnnotation(Parameter.class).names());
			if (partArgObject == null && partArgPartObject != null) {
				throw new ParameterException("If " + partArgPartParameter + " is specified, then " + partArgParameter + " must also be specified");
			} else {
				throw new ParameterException("If " + partArgParameter + " is specified, then " + partArgPartParameter + " must also be specified");
			}
		}
	}

	private static void invokePartArg(String partArgName, PubFetcherArgs args, List<Publication> publications) throws ReflectiveOperationException {
		invokePartArg(partArgName, args, publications, null);
	}
	private static void invokePartArg(String partArgName, PubFetcherArgs args, List<Publication> publications, Boolean not) throws ReflectiveOperationException {
		String partArgObjectName;
		if (not != null && not) {
			partArgObjectName = "not" + partArgName.substring(0, 1).toUpperCase(Locale.ROOT) + partArgName.substring(1);
		} else {
			partArgObjectName = partArgName;
		}
		Object partArgObject = PubFetcherArgs.class.getDeclaredField(partArgObjectName).get(args);
		if (partArgObject != null) {
			Object partArgPartObject = PubFetcherArgs.class.getDeclaredField(partArgObjectName + "Part").get(args);
			if (not != null) {
				PubFetcherMethods.class.getDeclaredMethod(partArgName, publications.getClass(), partArgPartObject.getClass(), partArgObject.getClass(), not.getClass())
					.invoke(null, publications, partArgPartObject, partArgObject, not);
			} else {
				PubFetcherMethods.class.getDeclaredMethod(partArgName, publications.getClass(), partArgPartObject.getClass(), partArgObject.getClass())
					.invoke(null, publications, partArgPartObject, partArgObject);
			}
		}
	}

	public static void run(PubFetcherArgs args, Fetcher fetcher, FetcherArgs fetcherArgs, List<PublicationIds> externalPublicationIds,
			List<String> externalWebpageUrls, List<String> externalDocUrls, Version version) throws IOException, ReflectiveOperationException {
		if (args == null) {
			throw new IllegalArgumentException("Args required!");
		}
		if (fetcher == null) {
			throw new IllegalArgumentException("Fetcher required!");
		}

		checkPartArg("partContent", args);
		checkPartArg("notPartContent", args);
		checkPartArg("partContentSize", args);
		checkPartArg("notPartContentSize", args);
		checkPartArg("partContentSizeMore", args);
		checkPartArg("partContentSizeLess", args);
		checkPartArg("partType", args);
		checkPartArg("notPartType", args);
		checkPartArg("partTypeEquivalent", args);
		checkPartArg("partTypeMore", args);
		checkPartArg("partTypeLess", args);
		checkPartArg("partUrl", args);
		checkPartArg("partUrlHost", args);
		checkPartArg("notPartUrlHost", args);
		checkPartArg("partTimeMore", args);
		checkPartArg("partTimeLess", args);

		if (args.fetchPart != null && args.notFetchPart != null) {
			String fetchPart = Arrays.toString(PubFetcherArgs.class.getDeclaredField("fetchPart").getAnnotation(Parameter.class).names());
			String notFetchPart = Arrays.toString(PubFetcherArgs.class.getDeclaredField("notFetchPart").getAnnotation(Parameter.class).names());
			throw new ParameterException("Parameters " + fetchPart + " and " + notFetchPart + " can't be specified at the same time");
		}

		if (version != null) {
			PUB_ID_SOURCE = version.getName() + " " + version.getVersion();
		}

		if (args.initDb != null) initDb(args.initDb);
		if (args.commitDb != null) commitDb(args.commitDb);
		if (args.compactDb != null) compactDb(args.compactDb);

		if (args.publicationsSize != null) publicationsSize(args.publicationsSize);
		if (args.webpagesSize != null) webpagesSize(args.webpagesSize);
		if (args.docsSize != null) docsSize(args.docsSize);

		if (args.dumpPublicationsMap != null) dumpPublicationsMap(args.dumpPublicationsMap);
		if (args.dumpPublicationsMapReverse != null) dumpPublicationsMapReverse(args.dumpPublicationsMapReverse);

		if (args.printWebpageSelector != null) {
			printWebpageSelector(args.printWebpageSelector.get(0), args.printWebpageSelector.get(1), args.printWebpageSelector.get(2),
				Boolean.valueOf(args.printWebpageSelector.get(3)), false, fetcher, fetcherArgs);
		}
		if (args.printWebpageSelectorHtml != null) {
			printWebpageSelector(args.printWebpageSelectorHtml.get(0), args.printWebpageSelectorHtml.get(1), args.printWebpageSelectorHtml.get(2),
				Boolean.valueOf(args.printWebpageSelectorHtml.get(3)), true, fetcher, fetcherArgs);
		}

		if (args.getSite != null) getSite(args.getSite, fetcher);
		if (args.getSelector != null) getSelector(args.getSite, args.getSelector, fetcher);
		if (args.getWebpage != null) getWebpage(args.getWebpage, fetcher);
		if (args.getJavascript != null) getJavascript(args.getJavascript, fetcher);

		// parts

		EnumMap<PublicationPartName, Boolean> parts = null;

		if (args.fetchPart != null) {
			parts = new EnumMap<>(PublicationPartName.class);
			for (PublicationPartName name : PublicationPartName.values()) {
				parts.put(name, false);
			}
			for (PublicationPartName name : args.fetchPart) {
				parts.put(name, true);
			}
		}

		if (args.notFetchPart != null) {
			parts = new EnumMap<>(PublicationPartName.class);
			for (PublicationPartName name : PublicationPartName.values()) {
				parts.put(name, true);
			}
			for (PublicationPartName name : args.notFetchPart) {
				parts.put(name, false);
			}
		}

		// test

		FetcherTest.run(args.fetcherTestArgs, fetcher, fetcherArgs, parts, PUB_ID_SOURCE);

		// add IDs

		Set<PublicationIds> publicationIds = new LinkedHashSet<>();
		Set<String> webpageUrls = new LinkedHashSet<>();
		Set<String> docUrls = new LinkedHashSet<>();

		if (externalPublicationIds != null) {
			publicationIds.addAll(pubCheck(externalPublicationIds));
			logger.info("Got {} new distinct external publication IDs", publicationIds.size());
		}
		if (externalWebpageUrls != null) {
			webpageUrls.addAll(web(externalWebpageUrls));
			logger.info("Got {} new distinct external webpage URLs", webpageUrls.size());
		}
		if (externalDocUrls != null) {
			docUrls.addAll(web(externalDocUrls));
			logger.info("Got {} new distinct external doc URLs", docUrls.size());
		}

		if (args.pubFile != null) {
			int sizeBefore = publicationIds.size();
			publicationIds.addAll(pubCheck(PubFetcher.pubFile(args.pubFile, PUB_ID_SOURCE)));
			logger.info("Got {} new distinct publication IDs from file {}", publicationIds.size() - sizeBefore, args.pubFile);
		}
		if (args.webFile != null) {
			int sizeBefore = webpageUrls.size();
			webpageUrls.addAll(web(PubFetcher.webFile(args.webFile)));
			logger.info("Got {} new distinct webpage URLs from file {}", webpageUrls.size() - sizeBefore, args.webFile);
		}
		if (args.docFile != null) {
			int sizeBefore = docUrls.size();
			docUrls.addAll(web(PubFetcher.webFile(args.docFile)));
			logger.info("Got {} new distinct doc URLs from file {}", docUrls.size() - sizeBefore, args.docFile);
		}

		if (args.pub != null) {
			int sizeBefore = publicationIds.size();
			publicationIds.addAll(pub(args.pub));
			logger.info("Got {} new distinct publication IDs from command line", publicationIds.size() - sizeBefore);
		}
		if (args.web != null) {
			int sizeBefore = webpageUrls.size();
			webpageUrls.addAll(web(args.web));
			logger.info("Got {} new distinct webpage URLs from command line", webpageUrls.size() - sizeBefore);
		}
		if (args.doc != null) {
			int sizeBefore = docUrls.size();
			docUrls.addAll(web(args.doc));
			logger.info("Got {} new distinct doc URLs from command line", docUrls.size() - sizeBefore);
		}

		if (args.pubDb != null) {
			int sizeBefore = publicationIds.size();
			publicationIds.addAll(pubDb(args.pubDb));
			logger.info("Got {} new distinct publication IDs from database {}", publicationIds.size() - sizeBefore, args.pubDb);
		}
		if (args.webDb != null) {
			int sizeBefore = webpageUrls.size();
			webpageUrls.addAll(webDb(args.webDb));
			logger.info("Got {} new distinct webpage URLs from database {}", webpageUrls.size() - sizeBefore, args.webDb);
		}
		if (args.docDb != null) {
			int sizeBefore = docUrls.size();
			docUrls.addAll(docDb(args.docDb));
			logger.info("Got {} new distinct doc URLs from database {}", docUrls.size() - sizeBefore, args.docDb);
		}
		if (args.allDb != null) {
			int sizeBefore = publicationIds.size();
			publicationIds.addAll(pubDb(args.pubDb));
			logger.info("Got {} new distinct publication IDs from database {}", publicationIds.size() - sizeBefore, args.allDb);

			sizeBefore = webpageUrls.size();
			webpageUrls.addAll(webDb(args.webDb));
			logger.info("Got {} new distinct webpage URLs from database {}", webpageUrls.size() - sizeBefore, args.allDb);

			sizeBefore = docUrls.size();
			docUrls.addAll(docDb(args.docDb));
			logger.info("Got {} new distinct doc URLs from database {}", docUrls.size() - sizeBefore, args.allDb);
		}

		// filter IDs

		if (args.hasPmid) hasPmid(publicationIds);
		if (args.notHasPmid) notHasPmid(publicationIds);
		if (args.pmid != null) pmid(publicationIds, args.pmid);

		if (args.hasPmcid) hasPmcid(publicationIds);
		if (args.notHasPmcid) notHasPmcid(publicationIds);
		if (args.pmcid != null) pmcid(publicationIds, args.pmcid);

		if (args.hasDoi) hasDoi(publicationIds);
		if (args.notHasDoi) notHasDoi(publicationIds);
		if (args.doi != null) doi(publicationIds, args.doi);
		if (args.doiRegistrant != null) doiRegistrant(publicationIds, args.doiRegistrant, false);
		if (args.notDoiRegistrant != null) doiRegistrant(publicationIds, args.notDoiRegistrant, true);

		if (args.url != null) {
			url(webpageUrls, args.url);
			url(docUrls, args.url);
		}
		if (args.urlHost != null) {
			urlHost(webpageUrls, args.urlHost, false);
			urlHost(docUrls, args.urlHost, false);
		}
		if (args.notUrlHost != null) {
			urlHost(webpageUrls, args.notUrlHost, true);
			urlHost(docUrls, args.notUrlHost, true);
		}

		if (args.inDb != null) {
			inDbPub(publicationIds, args.inDb);
			inDbWeb(webpageUrls, args.inDb);
			inDbDoc(docUrls, args.inDb);
		}
		if (args.notInDb != null) {
			notInDbPub(publicationIds, args.notInDb);
			notInDbWeb(webpageUrls, args.notInDb);
			notInDbDoc(docUrls, args.notInDb);
		}

		// sort IDs

		if (args.ascIds) {
			publicationIds = ascIds(publicationIds);
			webpageUrls = ascIds(webpageUrls);
			docUrls = ascIds(docUrls);
		}
		if (args.descIds) {
			publicationIds = descIds(publicationIds);
			webpageUrls = descIds(webpageUrls);
			docUrls = descIds(docUrls);
		}

		// limit IDs

		if (args.headIds != null) {
			head(publicationIds, args.headIds);
			head(webpageUrls, args.headIds);
			head(docUrls, args.headIds);
		}
		if (args.tailIds != null) {
			tail(publicationIds, args.tailIds);
			tail(webpageUrls, args.tailIds);
			tail(docUrls, args.tailIds);
		}

		// remove from database by IDs

		if (args.removeIds != null) {
			removeIdsPub(publicationIds, args.removeIds);
			removeIdsWeb(webpageUrls, args.removeIds);
			removeIdsDoc(docUrls, args.removeIds);
		}

		// output IDs

		if (args.outIds) {
			outIdsPub(publicationIds, args.plain, args.html);
			outIdsWeb(webpageUrls, args.html);
			outIdsWeb(docUrls, args.html);
		}

		if (args.txtIdsPub != null) txtIdsPub(publicationIds, args.plain, args.html, args.txtIdsPub);
		if (args.txtIdsWeb != null) txtIdsWeb(webpageUrls, args.html, args.txtIdsWeb);
		if (args.txtIdsDoc != null) txtIdsWeb(docUrls, args.html, args.txtIdsDoc);

		if (args.countIds) {
			countIds("Publication IDs", publicationIds);
			countIds("Webpage URLs   ", webpageUrls);
			countIds("Doc URLs       ", docUrls);
		}

		// get content

		List<Publication> publications = new ArrayList<>();
		List<Webpage> webpages = new ArrayList<>();
		List<Webpage> docs = new ArrayList<>();

		if (args.db != null) {
			publications.addAll(dbPub(publicationIds, args.db));
			webpages.addAll(dbWeb(webpageUrls, args.db));
			docs.addAll(dbDoc(docUrls, args.db));
		}

		if (args.fetch) {
			publications.addAll(fetchPub(publicationIds, fetcher, parts, fetcherArgs));
			webpages.addAll(fetchWeb(webpageUrls, fetcher, fetcherArgs));
			docs.addAll(fetchWeb(docUrls, fetcher, fetcherArgs));
		}

		if (args.dbFetch != null) {
			publications.addAll(dbFetchPub(publicationIds, args.dbFetch, fetcher, parts, fetcherArgs));
			webpages.addAll(dbFetchWeb(webpageUrls, args.dbFetch, fetcher, fetcherArgs));
			docs.addAll(dbFetchDoc(docUrls, args.dbFetch, fetcher, fetcherArgs));
		}

		if (args.fetchPut != null) {
			publications.addAll(fetchPutPub(publicationIds, args.fetchPut, fetcher, parts, fetcherArgs));
			webpages.addAll(fetchPutWeb(webpageUrls, args.fetchPut, fetcher, fetcherArgs));
			docs.addAll(fetchPutDoc(docUrls, args.fetchPut, fetcher, fetcherArgs));
		}

		// filter content

		if (args.fetchTimeMore != null) {
			fetchTimeMore(publications, args.fetchTimeMore);
			fetchTimeMore(webpages, args.fetchTimeMore);
			fetchTimeMore(docs, args.fetchTimeMore);
		}
		if (args.fetchTimeLess != null) {
			fetchTimeLess(publications, args.fetchTimeLess);
			fetchTimeLess(webpages, args.fetchTimeLess);
			fetchTimeLess(docs, args.fetchTimeLess);
		}

		if (args.retryCounter != null) {
			retryCounter(publications, args.retryCounter);
			retryCounter(webpages, args.retryCounter);
			retryCounter(docs, args.retryCounter);
		}
		if (args.notRetryCounter != null) {
			notRetryCounter(publications, args.notRetryCounter);
			notRetryCounter(webpages, args.notRetryCounter);
			notRetryCounter(docs, args.notRetryCounter);
		}

		if (args.retryCounterMore != null) {
			retryCounterMore(publications, args.retryCounterMore);
			retryCounterMore(webpages, args.retryCounterMore);
			retryCounterMore(docs, args.retryCounterMore);
		}
		if (args.retryCounterLess != null) {
			retryCounterLess(publications, args.retryCounterLess);
			retryCounterLess(webpages, args.retryCounterLess);
			retryCounterLess(docs, args.retryCounterLess);
		}

		if (args.empty) {
			empty(publications);
			empty(webpages);
			empty(docs);
		}
		if (args.notEmpty) {
			notEmpty(publications);
			notEmpty(webpages);
			notEmpty(docs);
		}
		if (args.isFinal) {
			isFinal(publications, fetcherArgs);
			isFinal(webpages, fetcherArgs);
			isFinal(docs, fetcherArgs);
		}
		if (args.notIsFinal) {
			notIsFinal(publications, fetcherArgs);
			notIsFinal(webpages, fetcherArgs);
			notIsFinal(docs, fetcherArgs);
		}

		if (args.totallyFinal) totallyFinal(publications, fetcherArgs);
		if (args.notTotallyFinal) notTotallyFinal(publications, fetcherArgs);

		if (args.partEmpty != null) partEmpty(publications, args.partEmpty, false);
		if (args.notPartEmpty != null) partEmpty(publications, args.notPartEmpty, true);
		if (args.partFinal != null) partFinal(publications, args.partFinal, fetcherArgs, false);
		if (args.notPartFinal != null) partFinal(publications, args.notPartFinal, fetcherArgs, true);

		invokePartArg("partContent", args, publications, false);
		invokePartArg("partContent", args, publications, true);
		invokePartArg("partContentSize", args, publications, false);
		invokePartArg("partContentSize", args, publications, true);
		invokePartArg("partContentSizeMore", args, publications);
		invokePartArg("partContentSizeLess", args, publications);
		invokePartArg("partType", args, publications, false);
		invokePartArg("partType", args, publications, true);
		invokePartArg("partTypeEquivalent", args, publications);
		invokePartArg("partTypeMore", args, publications);
		invokePartArg("partTypeLess", args, publications);
		invokePartArg("partUrl", args, publications);
		invokePartArg("partUrlHost", args, publications, false);
		invokePartArg("partUrlHost", args, publications, true);
		invokePartArg("partTimeMore", args, publications);
		invokePartArg("partTimeLess", args, publications);

		if (args.oa) oa(publications);
		if (args.notOa) notOa(publications);

		if (args.visited != null) visited(publications, args.visited);
		if (args.visitedHost != null) visitedHost(publications, args.visitedHost, false);
		if (args.notVisitedHost != null) visitedHost(publications, args.notVisitedHost, true);

		if (args.visitedType != null) visitedType(publications, args.visitedType, false);
		if (args.notVisitedType != null) visitedType(publications, args.notVisitedType, true);

		if (args.visitedFrom != null) visitedFrom(publications, args.visitedFrom);
		if (args.visitedFromHost != null) visitedFromHost(publications, args.visitedFromHost, false);
		if (args.notVisitedFromHost != null) visitedFromHost(publications, args.notVisitedFromHost, true);

		if (args.visitedSize != null) visitedSize(publications, args.visitedSize);
		if (args.notVisitedSize != null) notVisitedSize(publications, args.notVisitedSize);
		if (args.visitedSizeMore != null) visitedSizeMore(publications, args.visitedSizeMore);
		if (args.visitedSizeLess != null) visitedSizeLess(publications, args.visitedSizeLess);

		if (args.startUrl != null) {
			startUrl(webpages, args.startUrl);
			startUrl(docs, args.startUrl);
		}
		if (args.startUrlHost != null) {
			startUrlHost(webpages, args.startUrlHost, false);
			startUrlHost(docs, args.startUrlHost, false);
		}
		if (args.notStartUrlHost != null) {
			startUrlHost(webpages, args.notStartUrlHost, true);
			startUrlHost(docs, args.notStartUrlHost, true);
		}

		if (args.finalUrl != null) {
			finalUrl(webpages, args.finalUrl);
			finalUrl(docs, args.finalUrl);
		}
		if (args.finalUrlHost != null) {
			finalUrlHost(webpages, args.finalUrlHost, false);
			finalUrlHost(docs, args.finalUrlHost, false);
		}
		if (args.notFinalUrlHost != null) {
			finalUrlHost(webpages, args.notFinalUrlHost, true);
			finalUrlHost(docs, args.notFinalUrlHost, true);
		}

		if (args.contentType != null) {
			contentType(webpages, args.contentType);
			contentType(docs, args.contentType);
		}

		if (args.statusCode != null) {
			statusCode(webpages, args.statusCode);
			statusCode(docs, args.statusCode);
		}
		if (args.notStatusCode != null) {
			notStatusCode(webpages, args.notStatusCode);
			notStatusCode(docs, args.notStatusCode);
		}

		if (args.title != null) {
			title(webpages, args.title);
			title(docs, args.title);
		}
		if (args.titleMore != null) {
			titleMore(webpages, args.titleMore);
			titleMore(docs, args.titleMore);
		}
		if (args.titleLess != null) {
			titleLess(webpages, args.titleLess);
			titleLess(docs, args.titleLess);
		}

		if (args.content != null) {
			content(webpages, args.content);
			content(docs, args.content);
		}
		if (args.contentMore != null) {
			contentMore(webpages, args.contentMore);
			contentMore(docs, args.contentMore);
		}
		if (args.contentLess != null) {
			contentLess(webpages, args.contentLess);
			contentLess(docs, args.contentLess);
		}

		if (args.contentTimeMore != null) {
			contentTimeMore(webpages, args.contentTimeMore);
			contentTimeMore(docs, args.contentTimeMore);
		}
		if (args.contentTimeLess != null) {
			contentTimeLess(webpages, args.contentTimeLess);
			contentTimeLess(docs, args.contentTimeLess);
		}

		if (args.grep != null) {
			grep(publications, args.grep);
			grep(webpages, args.grep);
			grep(docs, args.grep);
		}

		// sort content

		if (args.asc) {
			asc(publications);
			asc(webpages);
			asc(docs);
		}
		if (args.desc) {
			desc(publications);
			desc(webpages);
			desc(docs);
		}

		if (args.ascTime) {
			ascTime(publications);
			ascTime(webpages);
			ascTime(docs);
		}
		if (args.descTime) {
			descTime(publications);
			descTime(webpages);
			descTime(docs);
		}

		// top hosts

		boolean topHosts = args.outTopHosts
			|| args.txtTopHostsPub != null || args.txtTopHostsWeb != null || args.txtTopHostsDoc != null
			|| args.countTopHosts;

		Map<String, Integer> topHostsPublications = null;
		Map<String, Integer> topHostsWebpages = null;
		Map<String, Integer> topHostsDocs = null;

		if (topHosts) {
			topHostsPublications = topHostsPub(publications, null);
			topHostsWebpages = topHostsWeb(webpages, null);
			topHostsDocs = topHostsWeb(docs, null);
		}

		boolean topHostsNoScrape = args.outTopHostsNoScrape
			|| args.txtTopHostsPubNoScrape != null || args.txtTopHostsWebNoScrape != null || args.txtTopHostsDocNoScrape != null;

		Map<String, Integer> topHostsPublicationsNoScrape = null;
		Map<String, Integer> topHostsWebpagesNoScrape = null;
		Map<String, Integer> topHostsDocsNoScrape = null;

		if (topHostsNoScrape) {
			topHostsPublicationsNoScrape = topHostsPub(publications, fetcher.getScrape());
			topHostsWebpagesNoScrape = topHostsWeb(webpages, fetcher.getScrape());
			topHostsDocsNoScrape = topHostsWeb(docs, fetcher.getScrape());
		}

		// limit content

		if (args.head != null) {
			head(publications, args.head);
			head(webpages, args.head);
			head(docs, args.head);
			if (topHosts) {
				head(topHostsPublications.entrySet(), args.head);
				head(topHostsWebpages.entrySet(), args.head);
				head(topHostsDocs.entrySet(), args.head);
			}
		}

		if (args.tail != null) {
			tail(publications, args.tail);
			tail(webpages, args.tail);
			tail(docs, args.tail);
			if (topHosts) {
				tail(topHostsPublications.entrySet(), args.tail);
				tail(topHostsWebpages.entrySet(), args.tail);
				tail(topHostsDocs.entrySet(), args.tail);
			}
		}

		// put to database

		if (args.put != null) {
			putPub(publications, args.put);
			putWeb(webpages, args.put);
			putDoc(docs, args.put);
		}

		// remove from database

		if (args.remove != null) {
			removePub(publications, args.remove);
			removeWeb(webpages, args.remove);
			removeDoc(docs, args.remove);
		}

		// output

		if (args.out) {
			out(publications, args.plain, args.html, args.outPart);
			out(webpages, args.plain, args.html, null);
			out(docs, args.plain, args.html, null);
		}

		if (args.txtPub != null) txt(publications, args.plain, args.html, args.outPart, args.txtPub);
		if (args.txtWeb != null) txt(publications, args.plain, args.html, null, args.txtWeb);
		if (args.txtDoc != null) txt(publications, args.plain, args.html, null, args.txtDoc);

		if (args.count) {
			count("Publications", publications);
			count("Webpages    ", webpages);
			count("Docs        ", docs);
		}

		if (args.outTopHosts) {
			outTopHosts(topHostsPublications, args.html);
			outTopHosts(topHostsWebpages, args.html);
			outTopHosts(topHostsDocs, args.html);
		}

		if (args.txtTopHostsPub != null) txtTopHosts(topHostsPublications, args.html, args.txtTopHostsPub);
		if (args.txtTopHostsWeb != null) txtTopHosts(topHostsWebpages, args.html, args.txtTopHostsWeb);
		if (args.txtTopHostsDoc != null) txtTopHosts(topHostsDocs, args.html, args.txtTopHostsDoc);

		if (args.countTopHosts) {
			count("Publications top hosts", topHostsPublications.entrySet());
			count("Webpages top hosts    ", topHostsWebpages.entrySet());
			count("Docs top hosts        ", topHostsDocs.entrySet());
		}

		if (args.outTopHostsNoScrape) {
			outTopHosts(topHostsPublicationsNoScrape, args.html);
			outTopHosts(topHostsWebpagesNoScrape, args.html);
			outTopHosts(topHostsDocsNoScrape, args.html);
		}

		if (args.txtTopHostsPubNoScrape != null) txtTopHosts(topHostsPublicationsNoScrape, args.html, args.txtTopHostsPubNoScrape);
		if (args.txtTopHostsWebNoScrape != null) txtTopHosts(topHostsWebpagesNoScrape, args.html, args.txtTopHostsWebNoScrape);
		if (args.txtTopHostsDocNoScrape != null) txtTopHosts(topHostsDocsNoScrape, args.html, args.txtTopHostsDocNoScrape);

		if (args.partTable) partTable(publications);
	}
}
