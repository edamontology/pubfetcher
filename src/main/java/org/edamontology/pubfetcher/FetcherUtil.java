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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

public final class FetcherUtil {

	static final String EXTERNAL_ID_URL = "http://localhost/";

	private static String timeHuman(Long time) {
		return Instant.ofEpochMilli(time).toString();
	}

	private static void initDb(String database) throws FileAlreadyExistsException {
		System.out.println("Init database: " + database);
		Database.init(database);
		System.out.println("Init: success");
	}

	private static void commitDb(String database) throws IOException {
		System.out.println("Commit database: " + database);
		try (Database db = new Database(database)) {
			db.commit();
		}
		System.out.println("Commit: success");
	}

	private static void compactDb(String database) throws IOException {
		System.out.println("Compact database: " + database);
		try (Database db = new Database(database)) {
			db.compact();
		}
		System.out.println("Compact: success");
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

	private static void printWebpageSelector(String webpageUrl, String title, String content, boolean javascript, boolean html, Fetcher fetcher) {
		Webpage webpage = fetcher.initWebpage(webpageUrl);
		fetcher.getWebpage(webpage, title, content, javascript);
		if (html) System.out.println(webpage.toStringHtml());
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
		System.out.println(fetcher.getScrape().getJavascript(FetcherCommon.getDoiRegistrant(doi)));
	}

	private static String progress(int i, int size) {
		return i + "/" + size + " (" + Math.round(i / (double) size * 1000) / 10.0 + "%)";
	}

	private static InputStream getResource(String name, String prefix) {
		InputStream resource = FetcherUtil.class.getResourceAsStream("/" + prefix + "/" + name);
		if (resource == null) {
			throw new MissingResourceException("Can't find test CSV resource '" + name + "'!", FetcherUtil.class.getSimpleName(), name);
		}
		return resource;
	}

	private static List<String[]> getTest(String resource, String prefix, int columns) throws IOException {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(getResource(resource, prefix), StandardCharsets.UTF_8))) {
			return br.lines()
				.skip(1)
				.filter(l -> !l.isEmpty() && l.charAt(0) != '#')
				.map(l -> l.split(",", columns))
				.filter(l -> {
					if (l.length != columns) throw new RuntimeException("Invalid line in " + resource + ": starting with " + l[0]);
					return true;
				})
				.collect(Collectors.toList());
		}
	}

	private static int equal(String test, int actual, String label) {
		int testInt;
		try {
			testInt = Integer.valueOf(test);
		} catch (NumberFormatException e) {
			System.err.println(e);
			return 1;
		}
		if (testInt != actual) {
			System.err.println(label + " must be " + testInt + ", actually is " + actual);
			return 1;
		} else {
			return 0;
		}
	}

	private static int testXml(String[] test, Publication publication) {
		int mismatch = 0;
		mismatch += equal(test[1], publication.getPmid().getContent().length(), "PMID length");
		mismatch += equal(test[2], publication.getPmcid().getContent().length(), "PMCID length");
		mismatch += equal(test[3], publication.getDoi().getContent().length(), "DOI length");
		mismatch += equal(test[4], publication.getTitle().getContent().length(), "title length");
		mismatch += equal(test[5], publication.getKeywords().getList().size(), "keywords size");
		mismatch += equal(test[6], publication.getAbstract().getContent().length(), "abstract length");
		mismatch += equal(test[7], publication.getFulltext().getContent().length(), "fulltext length");
		return mismatch;
	}

	@SuppressWarnings("unused")
	private static int testHtml(String[] test, Publication publication) {
		int mismatch = testXml(test, publication);
		mismatch += equal(test[8], publication.getVisitedSites().size(), "visited sites size");
		return mismatch;
	}

	private static int testPubmed(String[] test, Publication publication) {
		int mismatch = 0;
		mismatch += equal(test[1], publication.getPmid().getContent().length(), "PMID length");
		mismatch += equal(test[2], publication.getPmcid().getContent().length(), "PMCID length");
		mismatch += equal(test[3], publication.getDoi().getContent().length(), "DOI length");
		mismatch += equal(test[4], publication.getTitle().getContent().length(), "title length");
		mismatch += equal(test[5], publication.getKeywords().getList().size(), "keywords size");
		mismatch += equal(test[6], publication.getMeshTerms().getList().size(), "MeSH terms size");
		mismatch += equal(test[7], publication.getAbstract().getContent().length(), "abstract length");
		return mismatch;
	}

	private static int testEuropepmc(String[] test, Publication publication, FetcherPublicationState state) {
		int mismatch = testPubmed(test, publication);
		mismatch += equal(test[8], publication.isOA() ? 1 : 0, "Open Access");
		mismatch += equal(test[9], state.europepmcHasFulltextHTML ? 1 : 0, "has HTML");
		mismatch += equal(test[10], state.europepmcHasPDF ? 1 : 0, "has PDF");
		mismatch += equal(test[11], state.europepmcHasMinedTerms ? 1 : 0, "has mined");
		return mismatch;
	}

	@SuppressWarnings("unused")
	private static int testOaDoi(String[] test, Publication publication) {
		int mismatch = 0;
		mismatch += equal(test[1], publication.isOA() ? 1 : 0, "Open Access");
		mismatch += equal(test[2], publication.getVisitedSites().size(), "visited sites size");
		mismatch += equal(test[3], publication.getTitle().getContent().length(), "title length");
		return mismatch;
	}

	@SuppressWarnings("unused")
	private static int testEuropepmcMined(String[] test, Publication publication) {
		int mismatch = 0;
		mismatch += equal(test[1], publication.getEfoTerms().getList().size(), "EFO terms size");
		mismatch += equal(test[2], publication.getGoTerms().getList().size(), "GO terms size");
		return mismatch;
	}

	private static int testSite(String[] test, Publication publication) {
		int mismatch = 0;
		mismatch += equal(test[0], publication.getPmid().getContent().length(), "PMID length");
		mismatch += equal(test[1], publication.getPmcid().getContent().length(), "PMCID length");
		mismatch += equal(test[2], publication.getDoi().getContent().length(), "DOI length");
		mismatch += equal(test[3], publication.getTitle().getContent().length(), "title length");
		mismatch += equal(test[4], publication.getKeywords().getList().size(), "keywords size");
		mismatch += equal(test[5], publication.getAbstract().getContent().length(), "abstract length");
		mismatch += equal(test[6], publication.getFulltext().getContent().length(), "fulltext length");
		mismatch += equal(test[7], publication.getVisitedSites().size(), "visited sites size");
		return mismatch;
	}

	private static void test(List<String[]> tests, String fetchMethod, String testMethod, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts) throws ReflectiveOperationException {
		int mismatch = 0;
		int i = 0;
		for (String[] test : tests) {
			++i;
			System.out.println(test[0] + " " + progress(i, tests.size()));
			Publication publication = (Publication) FetcherUtil.class.getDeclaredMethod(fetchMethod, test[0].getClass(), fetcher.getClass(), EnumMap.class)
				.invoke(null, test[0], fetcher, parts);
			if (publication != null) {
				mismatch += (Integer) FetcherUtil.class.getDeclaredMethod(testMethod, test.getClass(), publication.getClass())
					.invoke(null, test, publication);
			} else ++mismatch;
		}
		if (mismatch == 0) System.out.println("OK");
		else System.err.println("There were " + mismatch + " mismatches!");
	}

	private static Publication fetchEuropepmcXml(String pmcid, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts) {
		Publication publication = fetcher.initPublication(new PublicationIds("", pmcid, "", "", EXTERNAL_ID_URL, ""));
		if (publication != null) {
			FetcherPublicationState state = new FetcherPublicationState();
			state.europepmcHasFulltextXML = true;
			fetcher.fetchEuropepmcFulltextXml(publication, state, parts);
		}
		return publication;
	}

	private static void printEuropepmcXml(String pmcid, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts) {
		System.out.println(fetchEuropepmcXml(pmcid, fetcher, parts));
	}

	private static void testEuropepmcXml(Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts) throws IOException, ReflectiveOperationException {
		test(getTest("europepmc-xml.csv", "test", 8), "fetchEuropepmcXml", "testXml", fetcher, parts);
	}

	private static Publication fetchEuropepmcHtml(String pmcid, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts) {
		Publication publication = fetcher.initPublication(new PublicationIds("", pmcid, "", "", EXTERNAL_ID_URL, ""));
		if (publication != null) {
			FetcherPublicationState state = new FetcherPublicationState();
			state.europepmcHasFulltextHTML = true;
			Links links = new Links();
			fetcher.fetchEuropepmcFulltextHtml(publication, links, state, parts, false);
			for (Link link : links.getLinks()) publication.addVisitedSite(link);
		}
		return publication;
	}

	private static void printEuropepmcHtml(String pmcid, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts) {
		System.out.println(fetchEuropepmcHtml(pmcid, fetcher, parts));
	}

	private static void testEuropepmcHtml(Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts) throws IOException, ReflectiveOperationException {
		test(getTest("europepmc-html.csv", "test", 9), "fetchEuropepmcHtml", "testHtml", fetcher, parts);
	}

	private static Publication fetchPmcXml(String pmcid, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts) {
		Publication publication = fetcher.initPublication(new PublicationIds("", pmcid, "", "", EXTERNAL_ID_URL, ""));
		if (publication != null) {
			fetcher.fetchPmcXml(publication, new FetcherPublicationState(), parts);
		}
		return publication;
	}

	private static void printPmcXml(String pmcid, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts) {
		System.out.println(fetchPmcXml(pmcid, fetcher, parts));
	}

	private static void testPmcXml(Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts) throws IOException, ReflectiveOperationException {
		test(getTest("pmc-xml.csv", "test", 8), "fetchPmcXml", "testXml", fetcher, parts);
	}

	private static Publication fetchPmcHtml(String pmcid, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts) {
		Publication publication = fetcher.initPublication(new PublicationIds("", pmcid, "", "", EXTERNAL_ID_URL, ""));
		if (publication != null) {
			Links links = new Links();
			fetcher.fetchPmcHtml(publication, links, new FetcherPublicationState(), parts, false);
			for (Link link : links.getLinks()) publication.addVisitedSite(link);
		}
		return publication;
	}

	private static void printPmcHtml(String pmcid, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts) {
		System.out.println(fetchPmcHtml(pmcid, fetcher, parts));
	}

	private static void testPmcHtml(Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts) throws IOException, ReflectiveOperationException {
		test(getTest("pmc-html.csv", "test", 9), "fetchPmcHtml", "testHtml", fetcher, parts);
	}

	private static Publication fetchPubmedXml(String pmid, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts) {
		Publication publication = fetcher.initPublication(new PublicationIds(pmid, "", "", EXTERNAL_ID_URL, "", ""));
		if (publication != null) {
			fetcher.fetchPubmedXml(publication, new FetcherPublicationState(), parts);
		}
		return publication;
	}

	private static void printPubmedXml(String pmid, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts) {
		System.out.println(fetchPubmedXml(pmid, fetcher, parts));
	}

	private static void testPubmedXml(Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts) throws IOException, ReflectiveOperationException {
		test(getTest("pubmed-xml.csv", "test", 8), "fetchPubmedXml", "testPubmed", fetcher, parts);
	}

	private static Publication fetchPubmedHtml(String pmid, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts) {
		Publication publication = fetcher.initPublication(new PublicationIds(pmid, "", "", EXTERNAL_ID_URL, "", ""));
		if (publication != null) {
			fetcher.fetchPubmedHtml(publication, new FetcherPublicationState(), parts);
		}
		return publication;
	}

	private static void printPubmedHtml(String pmid, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts) {
		System.out.println(fetchPubmedHtml(pmid, fetcher, parts));
	}

	private static void testPubmedHtml(Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts) throws IOException, ReflectiveOperationException {
		test(getTest("pubmed-html.csv", "test", 8), "fetchPubmedHtml", "testPubmed", fetcher, parts);
	}

	private static void printEuropepmc(String pmid, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts) {
		Publication publication = fetcher.initPublication(new PublicationIds(pmid, "", "", EXTERNAL_ID_URL, "", ""));
		if (publication != null) {
			fetcher.fetchEuropepmc(publication, new FetcherPublicationState(), parts);
		}
		System.out.println(publication);
	}

	private static void testEuropepmc(Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts) throws IOException, ReflectiveOperationException {
		int mismatch = 0;
		List<String[]> tests = getTest("europepmc.csv", "test", 12);
		int i = 0;
		for (String[] test : tests) {
			++i;
			System.out.println(test[0] + " " + progress(i, tests.size()));
			Publication publication = fetcher.initPublication(new PublicationIds(test[0], "", "", EXTERNAL_ID_URL, "", ""));
			if (publication != null) {
				FetcherPublicationState state = new FetcherPublicationState();
				fetcher.fetchEuropepmc(publication, state, parts);
				mismatch += testEuropepmc(test, publication, state);
			} else ++mismatch;
		}
		if (mismatch == 0) System.out.println("OK");
		else System.err.println("There were " + mismatch + " mismatches!");
	}

	private static Publication fetchEuropepmcMined(String pmid, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts) {
		Publication publication = fetcher.initPublication(new PublicationIds(pmid, "", "", EXTERNAL_ID_URL, "", ""));
		if (publication != null) {
			FetcherPublicationState state = new FetcherPublicationState();
			state.europepmcHasMinedTerms = true;
			fetcher.fetchEuropepmcMinedTermsEfo(publication, state, parts);
			fetcher.fetchEuropepmcMinedTermsGo(publication, state, parts);
		}
		return publication;
	}

	private static void printEuropepmcMined(String pmid, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts) {
		System.out.println(fetchEuropepmcMined(pmid, fetcher, parts));
	}

	private static void testEuropepmcMined(Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts) throws IOException, ReflectiveOperationException {
		test(getTest("europepmc-mined.csv", "test", 3), "fetchEuropepmcMined", "testEuropepmcMined", fetcher, parts);
	}

	private static Publication fetchOaDoi(String doi, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts) {
		Publication publication = fetcher.initPublication(new PublicationIds("", "", doi, "", "", EXTERNAL_ID_URL));
		if (publication != null) {
			Links links = new Links();
			fetcher.fetchOaDoi(publication, links, new FetcherPublicationState(), parts);
			for (Link link : links.getLinks()) publication.addVisitedSite(link);
		}
		return publication;
	}

	private static void printOaDoi(String doi, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts) {
		System.out.println(fetchOaDoi(doi, fetcher, parts));
	}

	private static void testOaDoi(Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts) throws IOException, ReflectiveOperationException {
		test(getTest("oadoi.csv", "test", 4), "fetchOaDoi", "testOaDoi", fetcher, parts);
	}

	private static Publication fetchSite(String url, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts) {
		Publication publication = new Publication();
		Links links = new Links();
		fetcher.fetchSite(publication, url, PublicationPartType.doi, EXTERNAL_ID_URL, links, parts, false, true);
		for (Link link : links.getLinks()) publication.addVisitedSite(link);
		return publication;
	}

	private static void printSite(String url, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts) {
		System.out.println(fetchSite(url, fetcher, parts));
	}

	private static void testSite(Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts) throws IOException, ReflectiveOperationException {
		int mismatch = 0;
		List<String[]> tests = getTest("journal.csv", "scrape", 9);
		int i = 0;
		for (String[] test : tests) {
			++i;
			System.out.println(test[8] + " " + progress(i, tests.size()));
			Publication publication = fetchSite(test[8], fetcher, parts);
			mismatch += testSite(test, publication);
		}
		if (mismatch == 0) System.out.println("OK");
		else System.err.println("There were " + mismatch + " mismatches!");
	}

	private static Webpage fetchWebpage(String url, Fetcher fetcher) {
		Webpage webpage = fetcher.initWebpage(url);
		fetcher.getWebpage(webpage);
		return webpage;
	}

	private static void printWebpage(String url, Fetcher fetcher) {
		System.out.println(fetchWebpage(url, fetcher));
	}

	private static void testWebpage(Fetcher fetcher) throws IOException {
		int mismatch = 0;
		List<String[]> tests = getTest("webpages.csv", "scrape", 3);
		int i = 0;
		for (String[] test : tests) {
			++i;
			System.out.println(test[2] + " " + progress(i, tests.size()));
			Webpage webpage = fetchWebpage(test[2], fetcher);
			mismatch += equal(test[0], webpage.getTitle().length(), "title length");
			mismatch += equal(test[1], webpage.getContent().length(), "content length");
		}
		if (mismatch == 0) System.out.println("OK");
		else System.err.println("There were " + mismatch + " mismatches!");
	}

	public static List<PublicationIds> pubFile(List<String> files) throws IOException {
		List<PublicationIds> publicationIds = new ArrayList<>();
		System.out.println("Load publication IDs from file " + files);
		for (String file : files) {
			try (Stream<String> lines = Files.lines(Paths.get(file), StandardCharsets.UTF_8)) {
				publicationIds.addAll(lines.map(l -> l.split("\t", 3))
					.filter(l -> {
						if (l.length != 3) System.err.println("Invalid line in " + file + ": starting with " + l[0]);
						return l.length == 3;
					})
					.map(l -> new PublicationIds(l[0], l[1], l[2], FetcherUtil.EXTERNAL_ID_URL, FetcherUtil.EXTERNAL_ID_URL, FetcherUtil.EXTERNAL_ID_URL))
					.collect(Collectors.toList()));
			}
		}
		System.out.println("Loaded " + publicationIds.size() + " publication IDs");
		return publicationIds;
	}

	private static List<String> webFile(List<String> files) throws IOException {
		List<String> webpageUrls = new ArrayList<>();
		System.out.println("Load webpage URLs from file " + files);
		for (String file : files) {
			try (Stream<String> lines = Files.lines(Paths.get(file), StandardCharsets.UTF_8)) {
				webpageUrls.addAll(lines.collect(Collectors.toList()));
			}
		}
		System.out.println("Loaded " + webpageUrls.size() + " webpage URLs");
		return webpageUrls;
	}

	private static List<PublicationIds> pub(List<String> pubIds) {
		List<PublicationIds> publicationIds = new ArrayList<>();
		if (pubIds.isEmpty()) {
			System.err.println("Check publication IDs: no publication IDs given");
			return publicationIds;
		}
		System.out.println("Check publication IDs: " + pubIds.size() + " publication IDs given");
		for (String pubId : pubIds) {
			PublicationIds onePublicationIds =
				FetcherCommon.isPmid(pubId) ? new PublicationIds(pubId, "", "", EXTERNAL_ID_URL, "", "") : (
				FetcherCommon.isPmcid(pubId) ? new PublicationIds("", pubId, "", "", EXTERNAL_ID_URL, "") : (
				FetcherCommon.isDoi(pubId) ? new PublicationIds("", "", pubId, "", "", EXTERNAL_ID_URL) : (
				null)));
			if (onePublicationIds == null) {
				System.err.println("Unknown publication ID: " + pubId);
			} else {
				publicationIds.add(onePublicationIds);
			}
		}
		if (publicationIds.size() < pubIds.size()) {
			System.err.println(publicationIds.size() + " publication IDs OK, " + (pubIds.size() - publicationIds.size()) + " not OK");
		} else {
			System.out.println(publicationIds.size() + " publication IDs OK");
		}
		return publicationIds;
	}

	private static List<PublicationIds> pubCheck(List<PublicationIds> pubIds) {
		List<PublicationIds> publicationIds = new ArrayList<>();
		if (pubIds.isEmpty()) {
			System.err.println("Check publication IDs: no publication IDs given");
			return publicationIds;
		}
		System.out.println("Check publication IDs: " + pubIds.size() + " publication IDs given");
		for (PublicationIds pubId : pubIds) {
			String pmid = pubId.getPmid();
			if (!pmid.isEmpty() && !FetcherCommon.isPmid(pmid)) {
				System.err.println("Unknown PMID: " + pubId);
				pmid = "";
			}
			String pmcid = pubId.getPmcid();
			if (!pmcid.isEmpty() && !FetcherCommon.isPmcid(pmcid)) {
				System.err.println("Unknown PMCID: " + pubId);
				pmcid = "";
			}
			String doi = pubId.getDoi();
			if (!doi.isEmpty() && !FetcherCommon.isDoi(doi)) {
				System.err.println("Unknown DOI: " + pubId);
				doi = "";
			}
			if (pmid.isEmpty() && pmcid.isEmpty() && doi.isEmpty()) {
				System.err.println("Not adding empty publication ID");
			} else {
				publicationIds.add(new PublicationIds(pmid, pmcid, doi, pubId.getPmidUrl(), pubId.getPmcidUrl(), pubId.getDoiUrl()));
			}
		}
		if (publicationIds.size() < pubIds.size()) {
			System.err.println(publicationIds.size() + " publication IDs OK, " + (pubIds.size() - publicationIds.size()) + " not OK");
		} else {
			System.out.println(publicationIds.size() + " publication IDs OK");
		}
		return publicationIds;
	}

	private static List<String> web(List<String> webUrls) {
		List<String> webpageUrls = new ArrayList<>();
		if (webUrls.isEmpty()) {
			System.err.println("Check webpage URLs: no webpage URLs given");
			return webpageUrls;
		}
		System.out.println("Check webpage URLs: " + webUrls.size() + " webpage URLs given");
		for (String webUrl : webUrls) {
			try {
				new URL(webUrl);
			} catch (MalformedURLException e) {
				System.err.println("Malformed URL: " + webUrl);
				continue;
			}
			webpageUrls.add(webUrl);
		}
		if (webpageUrls.size() < webUrls.size()) {
			System.err.println(webpageUrls.size() + " webpage URLs OK, " + (webUrls.size() - webpageUrls.size()) + " not OK");
		} else {
			System.out.println(webpageUrls.size() + " webpage URLs OK");
		}
		return webpageUrls;
	}

	private static Set<PublicationIds> pubDb(String database) throws IOException {
		Set<PublicationIds> publicationIds;
		System.out.println("Get publication IDs from database: " + database);
		try (Database db = new Database(database)) {
			publicationIds = db.getPublicationIds();
		}
		System.out.println("Got " + publicationIds.size() + " publication IDs");
		return publicationIds;
	}
	private static Set<String> webDb(String database) throws IOException {
		Set<String> webpageUrls;
		System.out.println("Get webpage URLs from database: " + database);
		try (Database db = new Database(database)) {
			webpageUrls = db.getWebpageUrls();
		}
		System.out.println("Got " + webpageUrls.size() + " webpage URLs");
		return webpageUrls;
	}
	private static Set<String> docDb(String database) throws IOException {
		Set<String> docUrls;
		System.out.println("Get doc URLs from database: " + database);
		try (Database db = new Database(database)) {
			docUrls = db.getDocUrls();
		}
		System.out.println("Got " + docUrls.size() + " doc URLs");
		return docUrls;
	}

	private static void hasPmid(Set<PublicationIds> pubIds) {
		System.out.print("Filter publication IDs with PMID: before " + pubIds.size());
		for (Iterator<PublicationIds> it = pubIds.iterator(); it.hasNext(); ) {
			if (it.next().getPmid().isEmpty()) it.remove();
		}
		System.out.println(", after " + pubIds.size());
	}
	private static void notHasPmid(Set<PublicationIds> pubIds) {
		System.out.print("Filter publication IDs with no PMID: before " + pubIds.size());
		for (Iterator<PublicationIds> it = pubIds.iterator(); it.hasNext(); ) {
			if (!it.next().getPmid().isEmpty()) it.remove();
		}
		System.out.println(", after " + pubIds.size());
	}
	private static void pmid(Set<PublicationIds> pubIds, String regex) {
		System.out.print("Filter publication IDs with PMID matching " + regex + ": before " + pubIds.size());
		for (Iterator<PublicationIds> it = pubIds.iterator(); it.hasNext(); ) {
			if (!it.next().getPmid().matches(regex)) it.remove();
		}
		System.out.println(", after " + pubIds.size());
	}

	private static void hasPmcid(Set<PublicationIds> pubIds) {
		System.out.print("Filter publication IDs with PMCID: before " + pubIds.size());
		for (Iterator<PublicationIds> it = pubIds.iterator(); it.hasNext(); ) {
			if (it.next().getPmcid().isEmpty()) it.remove();
		}
		System.out.println(", after " + pubIds.size());
	}
	private static void notHasPmcid(Set<PublicationIds> pubIds) {
		System.out.print("Filter publication IDs with no PMCID: before " + pubIds.size());
		for (Iterator<PublicationIds> it = pubIds.iterator(); it.hasNext(); ) {
			if (!it.next().getPmcid().isEmpty()) it.remove();
		}
		System.out.println(", after " + pubIds.size());
	}
	private static void pmcid(Set<PublicationIds> pubIds, String regex) {
		System.out.print("Filter publication IDs with PMCID matching " + regex + ": before " + pubIds.size());
		for (Iterator<PublicationIds> it = pubIds.iterator(); it.hasNext(); ) {
			if (!it.next().getPmcid().matches(regex)) it.remove();
		}
		System.out.println(", after " + pubIds.size());
	}

	private static void hasDoi(Set<PublicationIds> pubIds) {
		System.out.print("Filter publication IDs with DOI: before " + pubIds.size());
		for (Iterator<PublicationIds> it = pubIds.iterator(); it.hasNext(); ) {
			if (it.next().getDoi().isEmpty()) it.remove();
		}
		System.out.println(", after " + pubIds.size());
	}
	private static void notHasDoi(Set<PublicationIds> pubIds) {
		System.out.print("Filter publication IDs with no DOI: before " + pubIds.size());
		for (Iterator<PublicationIds> it = pubIds.iterator(); it.hasNext(); ) {
			if (!it.next().getDoi().isEmpty()) it.remove();
		}
		System.out.println(", after " + pubIds.size());
	}
	private static void doi(Set<PublicationIds> pubIds, String regex) {
		System.out.print("Filter publication IDs with DOI matching " + regex + ": before " + pubIds.size());
		for (Iterator<PublicationIds> it = pubIds.iterator(); it.hasNext(); ) {
			if (!it.next().getDoi().matches(regex)) it.remove();
		}
		System.out.println(", after " + pubIds.size());
	}

	private static void doiRegistrant(Set<PublicationIds> pubIds, List<String> registrants, boolean not) {
		System.out.print("Filter publication IDs with DOI" + (not ? " not " : " ") + "of registrant " + registrants + ": before " + pubIds.size());
		for (Iterator<PublicationIds> it = pubIds.iterator(); it.hasNext(); ) {
			String doi = it.next().getDoi();
			boolean matches = !doi.isEmpty() && registrants.contains(FetcherCommon.getDoiRegistrant(doi));
			if (!not && !matches || not && matches) {
				it.remove();
			}
		}
		System.out.println(", after " + pubIds.size());
	}

	private static void url(Set<String> webUrls, String regex) {
		System.out.print("Filter webpage URLs matching " + regex + ": before " + webUrls.size());
		for (Iterator<String> it = webUrls.iterator(); it.hasNext(); ) {
			if (!it.next().matches(regex)) it.remove();
		}
		System.out.println(", after " + webUrls.size());
	}

	private static void urlHost(Set<String> webUrls, List<String> hosts, boolean not) {
		System.out.print("Filter webpage URLs" + (not ? " not " : " ") + "of host " + hosts + ": before " + webUrls.size());
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
		System.out.println(", after " + webUrls.size());
	}

	private static void inDbPub(Set<PublicationIds> pubIds, String database) throws IOException {
		System.out.println("Filter " + pubIds.size() + " publication IDs being present in database: " + database);
		try (Database db = new Database(database)) {
			for (Iterator<PublicationIds> it = pubIds.iterator(); it.hasNext(); ) {
				if (!db.containsPublication(it.next())) it.remove();
			}
		}
		System.out.println(pubIds.size() + " publication IDs were present in database");
	}
	private static void notInDbPub(Set<PublicationIds> pubIds, String database) throws IOException {
		System.out.println("Filter " + pubIds.size() + " publication IDs being not present in database: " + database);
		try (Database db = new Database(database)) {
			for (Iterator<PublicationIds> it = pubIds.iterator(); it.hasNext(); ) {
				if (db.containsPublication(it.next())) it.remove();
			}
		}
		System.out.println(pubIds.size() + " publication IDs were not present in database");
	}

	private static void inDbWeb(Set<String> webUrls, String database) throws IOException {
		System.out.println("Filter " + webUrls.size() + " webpage URLs being present in database: " + database);
		try (Database db = new Database(database)) {
			for (Iterator<String> it = webUrls.iterator(); it.hasNext(); ) {
				if (!db.containsWebpage(it.next())) it.remove();
			}
		}
		System.out.println(webUrls.size() + " webpage URLs were present in database");
	}
	private static void notInDbWeb(Set<String> webUrls, String database) throws IOException {
		System.out.println("Filter " + webUrls.size() + " webpage URLs being not present in database: " + database);
		try (Database db = new Database(database)) {
			for (Iterator<String> it = webUrls.iterator(); it.hasNext(); ) {
				if (db.containsWebpage(it.next())) it.remove();
			}
		}
		System.out.println(webUrls.size() + " webpage URLs were not present in database");
	}

	private static void inDbDoc(Set<String> docUrls, String database) throws IOException {
		System.out.println("Filter " + docUrls.size() + " doc URLs being present in database: " + database);
		try (Database db = new Database(database)) {
			for (Iterator<String> it = docUrls.iterator(); it.hasNext(); ) {
				if (!db.containsDoc(it.next())) it.remove();
			}
		}
		System.out.println(docUrls.size() + " doc URLs were present in database");
	}
	private static void notInDbDoc(Set<String> docUrls, String database) throws IOException {
		System.out.println("Filter " + docUrls.size() + " doc URLs being not present in database: " + database);
		try (Database db = new Database(database)) {
			for (Iterator<String> it = docUrls.iterator(); it.hasNext(); ) {
				if (db.containsDoc(it.next())) it.remove();
			}
		}
		System.out.println(docUrls.size() + " doc URLs were not present in database");
	}

	private static <T extends Comparable<T>> LinkedHashSet<T> ascIds(Set<T> ids) {
		System.out.println("Sort " + ids.size() + " IDs in ascending order");
		return ids.stream().sorted().collect(Collectors.toCollection(LinkedHashSet::new));
	}
	private static <T extends Comparable<T>> LinkedHashSet<T> descIds(Set<T> ids) {
		System.out.println("Sort " + ids.size() + " IDs in descending order");
		return ids.stream().sorted(Collections.reverseOrder()).collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private static void removeIdsPub(Set<PublicationIds> pubIds, String database) throws IOException {
		System.out.println("Remove " + pubIds.size() + " publication IDs from database: " + database);
		int fail = 0;
		try (Database db = new Database(database)) {
			for (PublicationIds pubId : pubIds) {
				if (!db.removePublication(pubId)) {
					System.err.println("Failed to remove publication ID: " + pubId);
					++fail;
				} else db.commit();
			}
		}
		if (fail > 0) System.err.println("Failed to remove " + fail + " publication IDs");
		else System.out.println("Remove publication IDs: success");
	}
	private static void removeIdsWeb(Set<String> webUrls, String database) throws IOException {
		System.out.println("Remove " + webUrls.size() + " webpage URLs from database: " + database);
		int fail = 0;
		try (Database db = new Database(database)) {
			for (String webUrl : webUrls) {
				if (!db.removeWebpage(webUrl)) {
					System.err.println("Failed to remove webpage URL: " + webUrl);
					++fail;
				} else db.commit();
			}
		}
		if (fail > 0) System.err.println("Failed to remove " + fail + " webpage URLs");
		else System.out.println("Remove webpage URLs: success");
	}
	private static void removeIdsDoc(Set<String> docUrls, String database) throws IOException {
		System.out.println("Remove " + docUrls.size() + " doc URLs from database: " + database);
		int fail = 0;
		try (Database db = new Database(database)) {
			for (String docUrl : docUrls) {
				if (!db.removeDoc(docUrl)) {
					System.err.println("Failed to remove doc URL: " + docUrl);
					++fail;
				} else db.commit();
			}
		}
		if (fail > 0) System.err.println("Failed to remove " + fail + " doc URLs");
		else System.out.println("Remove doc URLs: success");
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
		System.out.println("Output " + pubIds.size() + " publication IDs" + (html ? " in HTML" : ""));
		printIdsPub(System.out, pubIds, plain, html);
	}
	private static void txtIdsPub(Set<PublicationIds> pubIds, boolean plain, boolean html, String txt) throws IOException {
		System.out.println("Output " + pubIds.size() + " publication IDs to file " + txt + (html ? " in HTML" : ""));
		try (PrintStream ps = new PrintStream(new BufferedOutputStream(Files.newOutputStream(FetcherCommon.outputPath(txt, false))), true, "UTF-8")) {
			if (pubIds.size() == 0) return;
			printIdsPub(ps, pubIds, plain, html);
		}
	}

	private static void printIdsWeb(PrintStream ps, Set<String> webUrls, boolean html) throws IOException {
		if (html) ps.println("<ul>");
		for (String webUrl : webUrls) {
			if (html) ps.println("<li><a href=\"" + webUrl + "\">" + webUrl + "</a></li>");
			else ps.println(webUrl);
		}
		if (html) ps.println("</ul>");
	}
	private static void outIdsWeb(Set<String> webUrls, boolean html) throws IOException {
		if (webUrls.size() == 0) return;
		System.out.println("Output " + webUrls.size() + " webpage URLs" + (html ? " in HTML" : ""));
		printIdsWeb(System.out, webUrls, html);
	}
	private static void txtIdsWeb(Set<String> webUrls, boolean html, String txt) throws IOException {
		System.out.println("Output " + webUrls.size() + " webpage URLs to file " + txt + (html ? " in HTML" : ""));
		try (PrintStream ps = new PrintStream(new BufferedOutputStream(Files.newOutputStream(FetcherCommon.outputPath(txt, false))), true, "UTF-8")) {
			if (webUrls.size() == 0) return;
			printIdsWeb(ps, webUrls, html);
		}
	}

	private static void countIds(String label, Set<?> ids) {
		System.out.println(label + " : " + ids.size());
	}

	private static void gotLog(String what, int initial, int current) {
		if (current != initial) System.err.println("Got " + current + " " + what);
		else System.out.println("Got " + current + " " + what);
	}
	private static void fetchedLog(String what, int initial, int current) {
		if (current != initial) System.err.println("Fetched " + current + " " + what);
		else System.out.println("Fetched " + current + " " + what);
	}

	private static List<Publication> dbPub(Set<PublicationIds> pubIds, String database) throws IOException {
		List<Publication> publications;
		System.out.println("Get " + pubIds.size() + " publications from database: " + database);
		try (Database db = new Database(database)) {
			publications = pubIds.stream().map(db::getPublication).filter(Objects::nonNull).collect(Collectors.toList());
		}
		gotLog("publications", pubIds.size(), publications.size());
		return publications;
	}
	private static List<Webpage> dbWeb(Set<String> webUrls, String database) throws IOException {
		List<Webpage> webpages;
		System.out.println("Get " + webUrls.size() + " webpages from database: " + database);
		try (Database db = new Database(database)) {
			webpages = webUrls.stream().map(db::getWebpage).filter(Objects::nonNull).collect(Collectors.toList());
		}
		gotLog("webpages", webUrls.size(), webpages.size());
		return webpages;
	}
	private static List<Webpage> dbDoc(Set<String> docUrls, String database) throws IOException {
		List<Webpage> docs;
		System.out.println("Get " + docUrls.size() + " docs from database: " + database);
		try (Database db = new Database(database)) {
			docs = docUrls.stream().map(db::getDoc).filter(Objects::nonNull).collect(Collectors.toList());
		}
		gotLog("docs", docUrls.size(), docs.size());
		return docs;
	}

	private static List<Publication> fetchPub(Set<PublicationIds> pubIds, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts) throws IOException {
		List<Publication> publications = new ArrayList<>();
		List<PublicationIds> publicationsException = new ArrayList<>();
		int pubIdsSize = pubIds.size();
		System.out.println("Fetch " + pubIdsSize + " publications");
		int i = 0;
		for (PublicationIds pubId : pubIds) {
			++i;
			System.out.println("Fetch publication " + progress(i, pubIdsSize));
			Publication publication = fetcher.initPublication(pubId);
			if (publication != null && fetcher.getPublication(publication, parts)) {
				if (publication.isFetchException()) {
					publicationsException.add(pubId);
				} else {
					publications.add(publication);
				}
			}
		}
		int publicationsExceptionSize = publicationsException.size();
		if (publicationsExceptionSize > 0) {
			System.out.println("Refetch " + publicationsExceptionSize + " publications with exception");
			i = 0;
			for (PublicationIds pubId : publicationsException) {
				++i;
				System.out.println("Refetch publication " + progress(i, publicationsExceptionSize));
				Publication publication = fetcher.initPublication(pubId);
				if (publication != null && fetcher.getPublication(publication, parts)) {
					publications.add(publication);
				}
			}
		}
		fetchedLog("publications", pubIdsSize, publications.size());
		return publications;
	}
	private static List<Webpage> fetchWeb(Set<String> webUrls, Fetcher fetcher) throws IOException {
		List<Webpage> webpages = new ArrayList<>();
		List<String> webpagesException = new ArrayList<>();
		int webUrlsSize = webUrls.size();
		System.out.println("Fetch " + webUrlsSize + " webpages");
		int i = 0;
		for (String webUrl : webUrls) {
			++i;
			System.out.println("Fetch webpage " + progress(i, webUrlsSize));
			Webpage webpage = fetcher.initWebpage(webUrl);
			if (webpage != null && fetcher.getWebpage(webpage)) {
				if (webpage.isFetchException()) {
					webpagesException.add(webUrl);
				} else {
					webpages.add(webpage);
				}
			}
		}
		int webpagesExceptionSize = webpagesException.size();
		if (webpagesExceptionSize > 0) {
			System.out.println("Refetch " + webpagesExceptionSize + " webpages with exception");
			i = 0;
			for (String webUrl : webpagesException) {
				++i;
				System.out.println("Refetch webpage " + progress(i, webpagesExceptionSize));
				Webpage webpage = fetcher.initWebpage(webUrl);
				if (webpage != null && fetcher.getWebpage(webpage)) {
					fetcher.getWebpage(webpage);
				}
			}
		}
		fetchedLog("webpages", webUrlsSize, webpages.size());
		return webpages;
	}

	private static List<Publication> dbFetchPub(Set<PublicationIds> pubIds, String database, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts) throws IOException {
		List<Publication> publications = new ArrayList<>();
		List<PublicationIds> pubIdsException = new ArrayList<>();
		int pubIdsSize = pubIds.size();
		System.out.println("Get " + pubIdsSize + " publications from database: " + database + " (or fetch if not present)");
		try (Database db = new Database(database)) {
			int i = 0;
			for (PublicationIds pubId : pubIds) {
				++i;
				System.out.println("Fetch publication " + progress(i, pubIdsSize));
				Publication publication = FetcherCommon.getPublication(pubId, db, fetcher, parts);
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
				System.out.println("Refetch " + pubIdsExceptionSize + " publications with exception");
				i = 0;
				for (PublicationIds pubId : pubIdsException) {
					++i;
					System.out.println("Refetch publication " + progress(i, pubIdsExceptionSize));
					Publication publication = FetcherCommon.getPublication(pubId, db, fetcher, parts);
					if (publication != null) {
						publications.add(publication);
					}
				}
			}
		}
		gotLog("publications", pubIdsSize, publications.size());
		return publications;
	}
	private static List<Webpage> dbFetchWeb(Set<String> webUrls, String database, Fetcher fetcher) throws IOException {
		List<Webpage> webpages = new ArrayList<>();
		List<String> webUrlsException = new ArrayList<>();
		int webUrlsSize = webUrls.size();
		System.out.println("Get " + webUrlsSize + " webpages from database: " + database + " (or fetch if not present)");
		try (Database db = new Database(database)) {
			int i = 0;
			for (String webUrl : webUrls) {
				++i;
				System.out.println("Fetch webpage " + progress(i, webUrlsSize));
				Webpage webpage = FetcherCommon.getWebpage(webUrl, db, fetcher);
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
				System.out.println("Refetch " + webUrlsExceptionSize + " webpages with exception");
				i = 0;
				for (String webUrl : webUrlsException) {
					++i;
					System.out.println("Refetch webpage " + progress(i, webUrlsExceptionSize));
					Webpage webpage = FetcherCommon.getWebpage(webUrl, db, fetcher);
					if (webpage != null) {
						webpages.add(webpage);
					}
				}
			}
		}
		gotLog("webpages", webUrlsSize, webpages.size());
		return webpages;
	}
	private static List<Webpage> dbFetchDoc(Set<String> docUrls, String database, Fetcher fetcher) throws IOException {
		List<Webpage> docs = new ArrayList<>();
		List<String> docUrlsException = new ArrayList<>();
		int docUrlsSize = docUrls.size();
		System.out.println("Get " + docUrlsSize + " docs from database: " + database + " (or fetch if not present)");
		try (Database db = new Database(database)) {
			int i = 0;
			for (String docUrl : docUrls) {
				++i;
				System.out.println("Fetch doc " + progress(i, docUrlsSize));
				Webpage doc = FetcherCommon.getDoc(docUrl, db, fetcher);
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
				System.out.println("Refetch " + docUrlsExceptionSize + " docs with exception");
				i = 0;
				for (String docUrl : docUrlsException) {
					++i;
					System.out.println("Refetch doc " + progress(i, docUrlsExceptionSize));
					Webpage doc = FetcherCommon.getDoc(docUrl, db, fetcher);
					if (doc != null) {
						docs.add(doc);
					}
				}
			}
		}
		gotLog("docs", docUrlsSize, docs.size());
		return docs;
	}

	private static List<Publication> fetchPutPub(Set<PublicationIds> pubIds, String database, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts) throws IOException {
		List<Publication> publications = new ArrayList<>();
		List<PublicationIds> publicationsException = new ArrayList<>();
		int pubIdsSize = pubIds.size();
		System.out.println("Fetch " + pubIdsSize + " publications and put to database: " + database);
		try (Database db = new Database(database)) {
			int i = 0;
			for (PublicationIds pubId : pubIds) {
				++i;
				System.out.println("Fetch publication " + progress(i, pubIdsSize));
				Publication publication = fetcher.initPublication(pubId);
				if (publication != null && fetcher.getPublication(publication, parts)) {
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
				System.out.println("Refetch " + publicationsExceptionSize + " publications with exception");
				i = 0;
				for (PublicationIds pubId : publicationsException) {
					++i;
					System.out.println("Refetch publication " + progress(i, publicationsExceptionSize));
					Publication publication = fetcher.initPublication(pubId);
					if (publication != null && fetcher.getPublication(publication, parts)) {
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
	private static List<Webpage> fetchPutWeb(Set<String> webUrls, String database, Fetcher fetcher) throws IOException {
		List<Webpage> webpages = new ArrayList<>();
		List<String> webpagesException = new ArrayList<>();
		int webUrlsSize = webUrls.size();
		System.out.println("Fetch " + webUrlsSize + " webpages and put to database: " + database);
		try (Database db = new Database(database)) {
			int i = 0;
			for (String webUrl : webUrls) {
				++i;
				System.out.println("Fetch webpage " + progress(i, webUrlsSize));
				Webpage webpage = fetcher.initWebpage(webUrl);
				if (webpage != null && fetcher.getWebpage(webpage)) {
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
				System.out.println("Refetch " + webpagesExceptionSize + " webpages with exception");
				i = 0;
				for (String webUrl : webpagesException) {
					++i;
					System.out.println("Refetch webpage " + progress(i, webpagesExceptionSize));
					Webpage webpage = fetcher.initWebpage(webUrl);
					if (webpage != null && fetcher.getWebpage(webpage)) {
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
	private static List<Webpage> fetchPutDoc(Set<String> docUrls, String database, Fetcher fetcher) throws IOException {
		List<Webpage> docs = new ArrayList<>();
		List<String> docsException = new ArrayList<>();
		int docUrlsSize = docUrls.size();
		System.out.println("Fetch " + docUrlsSize + " docs and put to database: " + database);
		try (Database db = new Database(database)) {
			int i = 0;
			for (String docUrl : docUrls) {
				++i;
				System.out.println("Fetch doc " + progress(i, docUrlsSize));
				Webpage doc = fetcher.initWebpage(docUrl);
				if (doc != null && fetcher.getWebpage(doc)) {
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
				System.out.println("Refetch " + docsExceptionSize + " docs with exception");
				i = 0;
				for (String docUrl : docsException) {
					++i;
					System.out.println("Refetch doc " + progress(i, docsExceptionSize));
					Webpage doc = fetcher.initWebpage(docUrl);
					if (doc != null && fetcher.getWebpage(doc)) {
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
		System.out.print("Filter entries with fetch time more than " + timeHuman(time) + ": before " + entries.size());
		for (Iterator<? extends DatabaseEntry<?>> it = entries.iterator(); it.hasNext(); ) {
			if (it.next().getFetchTime() < time) it.remove();
		}
		System.out.println(", after " + entries.size());
	}
	private static void fetchTimeLess(List<? extends DatabaseEntry<?>> entries, Long time) {
		System.out.print("Filter entries with fetch time less than " + timeHuman(time) + ": before " + entries.size());
		for (Iterator<? extends DatabaseEntry<?>> it = entries.iterator(); it.hasNext(); ) {
			if (it.next().getFetchTime() > time) it.remove();
		}
		System.out.println(", after " + entries.size());
	}

	private static void retryCounter(List<? extends DatabaseEntry<?>> entries, List<Integer> counts) {
		System.out.print("Filter entries with retry count " + counts + ": before " + entries.size());
		for (Iterator<? extends DatabaseEntry<?>> it = entries.iterator(); it.hasNext(); ) {
			if (!counts.contains(it.next().getRetryCounter())) it.remove();
		}
		System.out.println(", after " + entries.size());
	}
	private static void notRetryCounter(List<? extends DatabaseEntry<?>> entries, List<Integer> counts) {
		System.out.print("Filter entries with retry count not " + counts + ": before " + entries.size());
		for (Iterator<? extends DatabaseEntry<?>> it = entries.iterator(); it.hasNext(); ) {
			if (counts.contains(it.next().getRetryCounter())) it.remove();
		}
		System.out.println(", after " + entries.size());
	}

	private static void retryCounterMore(List<? extends DatabaseEntry<?>> entries, int count) {
		System.out.print("Filter entries with retry count more than " + count + ": before " + entries.size());
		for (Iterator<? extends DatabaseEntry<?>> it = entries.iterator(); it.hasNext(); ) {
			if (it.next().getRetryCounter() <= count) it.remove();
		}
		System.out.println(", after " + entries.size());
	}
	private static void retryCounterLess(List<? extends DatabaseEntry<?>> entries, int count) {
		System.out.print("Filter entries with retry count less than " + count + ": before " + entries.size());
		for (Iterator<? extends DatabaseEntry<?>> it = entries.iterator(); it.hasNext(); ) {
			if (it.next().getRetryCounter() >= count) it.remove();
		}
		System.out.println(", after " + entries.size());
	}

	private static void empty(List<? extends DatabaseEntry<?>> entries) {
		System.out.print("Filter empty entries: before " + entries.size());
		for (Iterator<? extends DatabaseEntry<?>> it = entries.iterator(); it.hasNext(); ) {
			if (!it.next().isEmpty()) it.remove();
		}
		System.out.println(", after " + entries.size());
	}
	private static void notEmpty(List<? extends DatabaseEntry<?>> entries) {
		System.out.print("Filter not empty entries: before " + entries.size());
		for (Iterator<? extends DatabaseEntry<?>> it = entries.iterator(); it.hasNext(); ) {
			if (it.next().isEmpty()) it.remove();
		}
		System.out.println(", after " + entries.size());
	}

	private static void isFinal(List<? extends DatabaseEntry<?>> entries, FetcherArgs fetcherArgs) {
		System.out.print("Filter final entries: before " + entries.size());
		for (Iterator<? extends DatabaseEntry<?>> it = entries.iterator(); it.hasNext(); ) {
			if (!it.next().isFinal(fetcherArgs)) it.remove();
		}
		System.out.println(", after " + entries.size());
	}
	private static void notIsFinal(List<? extends DatabaseEntry<?>> entries, FetcherArgs fetcherArgs) {
		System.out.print("Filter not final entries: before " + entries.size());
		for (Iterator<? extends DatabaseEntry<?>> it = entries.iterator(); it.hasNext(); ) {
			if (it.next().isFinal(fetcherArgs)) it.remove();
		}
		System.out.println(", after " + entries.size());
	}

	private static void totallyFinal(List<Publication> publications, FetcherArgs fetcherArgs) {
		System.out.print("Filter totally final publications: before " + publications.size());
		for (Iterator<Publication> it = publications.iterator(); it.hasNext(); ) {
			if (!it.next().isTotallyFinal(fetcherArgs)) it.remove();
		}
		System.out.println(", after " + publications.size());
	}
	private static void notTotallyFinal(List<Publication> publications, FetcherArgs fetcherArgs) {
		System.out.print("Filter not totally final publications: before " + publications.size());
		for (Iterator<Publication> it = publications.iterator(); it.hasNext(); ) {
			if (it.next().isTotallyFinal(fetcherArgs)) it.remove();
		}
		System.out.println(", after " + publications.size());
	}

	private static void partEmpty(List<Publication> publications, List<PublicationPartName> names, boolean not) {
		System.out.print("Filter publications with parts " + names + (not ? " not " : " ") + "empty: before " + publications.size());
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
		System.out.println(", after " + publications.size());
	}

	private static void partFinal(List<Publication> publications, List<PublicationPartName> names, FetcherArgs fetcherArgs, boolean not) {
		System.out.print("Filter publications with parts " + names + (not ? " not " : " ") + "final: before " + publications.size());
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
		System.out.println(", after " + publications.size());
	}

	@SuppressWarnings({ "unchecked", "unused" })
	private static void partContent(ArrayList<Publication> publications, ArrayList<PublicationPartName> names, String regex, Boolean not) {
		System.out.print("Filter publications with parts " + names + " matching " + regex + ": before " + publications.size());
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
		System.out.println(", after " + publications.size());
	}

	@SuppressWarnings("unused")
	private static void partContentSize(ArrayList<Publication> publications, ArrayList<PublicationPartName> names, ArrayList<Integer> sizes, Boolean not) {
		System.out.print("Filter publications with parts " + names + (not ? " not " : " ") + "having size " + sizes + ": before " + publications.size());
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
		System.out.println(", after " + publications.size());
	}

	@SuppressWarnings("unused")
	private static void partContentSizeMore(ArrayList<Publication> publications, ArrayList<PublicationPartName> names, Integer size) {
		System.out.print("Filter publications with parts " + names + " having size more than " + size + ": before " + publications.size());
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
		System.out.println(", after " + publications.size());
	}
	@SuppressWarnings("unused")
	private static void partContentSizeLess(ArrayList<Publication> publications, ArrayList<PublicationPartName> names, Integer size) {
		System.out.print("Filter publications with parts " + names + " having size less than " + size + ": before " + publications.size());
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
		System.out.println(", after " + publications.size());
	}

	@SuppressWarnings("unused")
	private static void partType(ArrayList<Publication> publications, ArrayList<PublicationPartName> names, ArrayList<PublicationPartType> types, Boolean not) {
		System.out.print("Filter publications with parts " + names + (not ? " not " : " ") + "having type " + types + ": before " + publications.size());
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
		System.out.println(", after " + publications.size());
	}

	@SuppressWarnings("unused")
	private static void partTypeEquivalent(ArrayList<Publication> publications, ArrayList<PublicationPartName> names, PublicationPartType type) {
		System.out.print("Filter publications with parts " + names + " having type equivalent to " + type + ": before " + publications.size());
		for (Iterator<Publication> it = publications.iterator(); it.hasNext(); ) {
			Publication publication = it.next();
			for (PublicationPartName name : names) {
				if (!publication.getPart(name).getType().isEquivalent(type)) {
					it.remove();
					break;
				}
			}
		}
		System.out.println(", after " + publications.size());
	}
	@SuppressWarnings("unused")
	private static void partTypeMore(ArrayList<Publication> publications, ArrayList<PublicationPartName> names, PublicationPartType type) {
		System.out.print("Filter publications with parts " + names + " having type more than " + type + ": before " + publications.size());
		for (Iterator<Publication> it = publications.iterator(); it.hasNext(); ) {
			Publication publication = it.next();
			for (PublicationPartName name : names) {
				if (!publication.getPart(name).getType().isBetterThan(type)) {
					it.remove();
					break;
				}
			}
		}
		System.out.println(", after " + publications.size());
	}
	@SuppressWarnings("unused")
	private static void partTypeLess(ArrayList<Publication> publications, ArrayList<PublicationPartName> names, PublicationPartType type) {
		System.out.print("Filter publications with parts " + names + " having type less than " + type + ": before " + publications.size());
		for (Iterator<Publication> it = publications.iterator(); it.hasNext(); ) {
			Publication publication = it.next();
			for (PublicationPartName name : names) {
				if (publication.getPart(name).getType().isEquivalent(type) || publication.getPart(name).getType().isBetterThan(type)) {
					it.remove();
					break;
				}
			}
		}
		System.out.println(", after " + publications.size());
	}

	@SuppressWarnings("unused")
	private static void partUrl(ArrayList<Publication> publications, ArrayList<PublicationPartName> names, String regex) {
		System.out.print("Filter publications with parts " + names + " having url matching " + regex + ": before " + publications.size());
		for (Iterator<Publication> it = publications.iterator(); it.hasNext(); ) {
			Publication publication = it.next();
			for (PublicationPartName name : names) {
				if (!publication.getPart(name).getUrl().matches(regex)) {
					it.remove();
					break;
				}
			}
		}
		System.out.println(", after " + publications.size());
	}

	@SuppressWarnings("unused")
	private static void partUrlHost(ArrayList<Publication> publications, ArrayList<PublicationPartName> names, ArrayList<String> hosts, Boolean not) {
		System.out.print("Filter publications with parts " + names + (not ? " not " : " ") + " having url of host " + hosts + ": before " + publications.size());
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
		System.out.println(", after " + publications.size());
	}

	@SuppressWarnings("unused")
	private static void partTimeMore(ArrayList<Publication> publications, ArrayList<PublicationPartName> names, Long time) {
		System.out.print("Filter publications with parts " + names + " having time more than " + timeHuman(time) + ": before " + publications.size());
		for (Iterator<Publication> it = publications.iterator(); it.hasNext(); ) {
			Publication publication = it.next();
			for (PublicationPartName name : names) {
				if (publication.getPart(name).getTimestamp() < time) {
					it.remove();
					break;
				}
			}
		}
		System.out.println(", after " + publications.size());
	}
	@SuppressWarnings("unused")
	private static void partTimeLess(ArrayList<Publication> publications, ArrayList<PublicationPartName> names, Long time) {
		System.out.print("Filter publications with parts " + names + " having time less than " + timeHuman(time) + ": before " + publications.size());
		for (Iterator<Publication> it = publications.iterator(); it.hasNext(); ) {
			Publication publication = it.next();
			for (PublicationPartName name : names) {
				if (publication.getPart(name).getTimestamp() > time) {
					it.remove();
					break;
				}
			}
		}
		System.out.println(", after " + publications.size());
	}

	private static void oa(List<Publication> publications) {
		System.out.print("Filter publications that are Open Access: before " + publications.size());
		for (Iterator<Publication> it = publications.iterator(); it.hasNext(); ) {
			if (!it.next().isOA()) it.remove();
		}
		System.out.println(", after " + publications.size());
	}
	private static void notOa(List<Publication> publications) {
		System.out.print("Filter publications that are not Open Access: before " + publications.size());
		for (Iterator<Publication> it = publications.iterator(); it.hasNext(); ) {
			if (it.next().isOA()) it.remove();
		}
		System.out.println(", after " + publications.size());
	}

	private static void visited(List<Publication> publications, String regex) {
		System.out.print("Filter publications with visited site matching " + regex + ": before " + publications.size());
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
		System.out.println(", after " + publications.size());
	}

	private static void visitedHost(List<Publication> publications, List<String> hosts, boolean not) {
		System.out.print("Filter publications with" + (not ? " no " : " ") + "visited site of host " + hosts + ": before " + publications.size());
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
		System.out.println(", after " + publications.size());
	}

	private static void visitedType(List<Publication> publications, List<PublicationPartType> types, boolean not) {
		System.out.print("Filter publications with" + (not ? " no " : " ") + "visited site of type " + types + ": before " + publications.size());
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
		System.out.println(", after " + publications.size());
	}

	private static void visitedFrom(List<Publication> publications, String regex) {
		System.out.print("Filter publications with visited site from URL matching " + regex + ": before " + publications.size());
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
		System.out.println(", after " + publications.size());
	}

	private static void visitedFromHost(List<Publication> publications, List<String> hosts, boolean not) {
		System.out.print("Filter publications with" + (not ? " no " : " ") + "visited site from URL of host " + hosts + ": before " + publications.size());
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
		System.out.println(", after " + publications.size());
	}

	private static void visitedSize(List<Publication> publications, List<Integer> sizes) {
		System.out.print("Filter publications with visited sites size " + sizes + ": before " + publications.size());
		for (Iterator<Publication> it = publications.iterator(); it.hasNext(); ) {
			if (!sizes.contains(it.next().getVisitedSites().size())) it.remove();
		}
		System.out.println(", after " + publications.size());
	}
	private static void notVisitedSize(List<Publication> publications, List<Integer> sizes) {
		System.out.print("Filter publications with visited sites size not " + sizes + ": before " + publications.size());
		for (Iterator<Publication> it = publications.iterator(); it.hasNext(); ) {
			if (sizes.contains(it.next().getVisitedSites().size())) it.remove();
		}
		System.out.println(", after " + publications.size());
	}

	private static void visitedSizeMore(List<Publication> publications, int size) {
		System.out.print("Filter publications with visited sites size more than " + size + ": before " + publications.size());
		for (Iterator<Publication> it = publications.iterator(); it.hasNext(); ) {
			if (it.next().getVisitedSites().size() <= size) it.remove();
		}
		System.out.println(", after " + publications.size());
	}
	private static void visitedSizeLess(List<Publication> publications, int size) {
		System.out.print("Filter publications with visited sites size less than " + size + ": before " + publications.size());
		for (Iterator<Publication> it = publications.iterator(); it.hasNext(); ) {
			if (it.next().getVisitedSites().size() >= size) it.remove();
		}
		System.out.println(", after " + publications.size());
	}

	private static void startUrl(List<Webpage> webpages, String regex) {
		System.out.print("Filter webpages with start URL matching " + regex + ": before " + webpages.size());
		for (Iterator<Webpage> it = webpages.iterator(); it.hasNext(); ) {
			if (!it.next().getStartUrl().matches(regex)) it.remove();
		}
		System.out.println(", after " + webpages.size());
	}

	private static void startUrlHost(List<Webpage> webpages, List<String> hosts, boolean not) {
		System.out.print("Filter webpages with start URL" + (not ? " not " : " ") + "of host " + hosts + ": before " + webpages.size());
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
		System.out.println(", after " + webpages.size());
	}

	private static void finalUrl(List<Webpage> webpages, String regex) {
		System.out.print("Filter webpages with final URL matching " + regex + ": before " + webpages.size());
		for (Iterator<Webpage> it = webpages.iterator(); it.hasNext(); ) {
			if (!it.next().getFinalUrl().matches(regex)) it.remove();
		}
		System.out.println(", after " + webpages.size());
	}

	private static void finalUrlHost(List<Webpage> webpages, List<String> hosts, boolean not) {
		System.out.print("Filter webpages with final URL" + (not ? " not " : " ") + "of host " + hosts + ": before " + webpages.size());
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
		System.out.println(", after " + webpages.size());
	}

	private static void contentType(List<Webpage> webpages, String regex) {
		System.out.print("Filter webpages with content type matching " + regex + ": before " + webpages.size());
		for (Iterator<Webpage> it = webpages.iterator(); it.hasNext(); ) {
			if (!it.next().getContentType().matches(regex)) it.remove();
		}
		System.out.println(", after " + webpages.size());
	}

	private static void statusCode(List<Webpage> webpages, List<Integer> codes) {
		System.out.print("Filter webpages with status code " + codes + ": before " + webpages.size());
		for (Iterator<Webpage> it = webpages.iterator(); it.hasNext(); ) {
			if (!codes.contains(it.next().getStatusCode())) it.remove();
		}
		System.out.println(", after " + webpages.size());
	}
	private static void notStatusCode(List<Webpage> webpages, List<Integer> codes) {
		System.out.print("Filter webpages with status code not " + codes + ": before " + webpages.size());
		for (Iterator<Webpage> it = webpages.iterator(); it.hasNext(); ) {
			if (codes.contains(it.next().getStatusCode())) it.remove();
		}
		System.out.println(", after " + webpages.size());
	}

	private static void title(List<Webpage> webpages, String regex) {
		System.out.print("Filter webpages with title matching " + regex + ": before " + webpages.size());
		for (Iterator<Webpage> it = webpages.iterator(); it.hasNext(); ) {
			if (!it.next().getTitle().matches(regex)) it.remove();
		}
		System.out.println(", after " + webpages.size());
	}

	private static void titleMore(List<Webpage> webpages, int count) {
		System.out.print("Filter webpages with title length more than " + count + ": before " + webpages.size());
		for (Iterator<Webpage> it = webpages.iterator(); it.hasNext(); ) {
			if (it.next().getTitle().length() <= count) it.remove();
		}
		System.out.println(", after " + webpages.size());
	}
	private static void titleLess(List<Webpage> webpages, int count) {
		System.out.print("Filter webpages with title length less than " + count + ": before " + webpages.size());
		for (Iterator<Webpage> it = webpages.iterator(); it.hasNext(); ) {
			if (it.next().getTitle().length() >= count) it.remove();
		}
		System.out.println(", after " + webpages.size());
	}

	private static void content(List<Webpage> webpages, String regex) {
		System.out.print("Filter webpages with content matching " + regex + ": before " + webpages.size());
		for (Iterator<Webpage> it = webpages.iterator(); it.hasNext(); ) {
			if (!it.next().getContent().matches(regex)) it.remove();
		}
		System.out.println(", after " + webpages.size());
	}

	private static void contentMore(List<Webpage> webpages, int count) {
		System.out.print("Filter webpages with content length more than " + count + ": before " + webpages.size());
		for (Iterator<Webpage> it = webpages.iterator(); it.hasNext(); ) {
			if (it.next().getContent().length() <= count) it.remove();
		}
		System.out.println(", after " + webpages.size());
	}
	private static void contentLess(List<Webpage> webpages, int count) {
		System.out.print("Filter webpages with content length less than " + count + ": before " + webpages.size());
		for (Iterator<Webpage> it = webpages.iterator(); it.hasNext(); ) {
			if (it.next().getContent().length() >= count) it.remove();
		}
		System.out.println(", after " + webpages.size());
	}

	private static void contentTimeMore(List<Webpage> webpages, Long time) {
		System.out.print("Filter webpages with content time more than " + timeHuman(time) + ": before " + webpages.size());
		for (Iterator<Webpage> it = webpages.iterator(); it.hasNext(); ) {
			if (it.next().getContentTime() < time) it.remove();
		}
		System.out.println(", after " + webpages.size());
	}
	private static void contentTimeLess(List<Webpage> webpages, Long time) {
		System.out.print("Filter webpages with content time less than " + timeHuman(time) + ": before " + webpages.size());
		for (Iterator<Webpage> it = webpages.iterator(); it.hasNext(); ) {
			if (it.next().getContentTime() > time) it.remove();
		}
		System.out.println(", after " + webpages.size());
	}

	private static void grep(List<? extends DatabaseEntry<?>> entries, String regex) {
		System.out.print("Filter entries matching " + regex + ": before " + entries.size());
		Pattern pattern = Pattern.compile(regex);
		for (Iterator<? extends DatabaseEntry<?>> it = entries.iterator(); it.hasNext(); ) {
			if (!pattern.matcher(it.next().toStringPlain()).find()) it.remove(); // TODO
		}
		System.out.println(", after " + entries.size());
	}

	private static <T extends DatabaseEntry<T>> void asc(List<T> entries) {
		System.out.println("Sort " + entries.size() + " entries in ascending order");
		Collections.sort(entries);
	}
	private static <T extends DatabaseEntry<T>> void desc(List<T> entries) {
		System.out.println("Sort " + entries.size() + " entries in descending order");
		Collections.sort(entries, Collections.reverseOrder());
	}

	private static <T extends DatabaseEntry<T>> void ascTime(List<T> entries) {
		System.out.println("Sort " + entries.size() + " entries in ascending order by fetch time");
		Collections.sort(entries, (a, b) -> a.getFetchTime() < b.getFetchTime() ? -1 : a.getFetchTime() > b.getFetchTime() ? 1 : 0);
	}
	private static <T extends DatabaseEntry<T>> void descTime(List<T> entries) {
		System.out.println("Sort " + entries.size() + " entries in descending order by fetch time");
		Collections.sort(entries, (a, b) -> a.getFetchTime() < b.getFetchTime() ? 1 : a.getFetchTime() > b.getFetchTime() ? -1 : 0);
	}

	private static Map<String, Integer> topHostsPub(List<Publication> publications, Scrape scrape) {
		if (scrape == null) {
			System.out.println("Get top hosts from " + publications.size() + " publications");
		} else {
			System.out.println("Get top hosts without scrape rules from " + publications.size() + " publications");
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
		System.out.println("Got " + topHostsPub.size() + " top hosts");
		return topHostsPub;
	}

	private static Map<String, Integer> topHostsWeb(List<Webpage> webpages, Scrape scrape) {
		if (scrape == null) {
			System.out.println("Get top hosts from " + webpages.size() + " webpages");
		} else {
			System.out.println("Get top hosts without scrape rules from " + webpages.size() + " webpages");
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
				System.err.println("Malformed URL: " + webpage.getFinalUrl());
			}
		}
		Map<String, Integer> topHostsWeb = hosts.entrySet().stream()
			.sorted((c1, c2) -> c2.getValue().compareTo(c1.getValue()))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (k, v) -> { throw new AssertionError(); }, LinkedHashMap::new));
		System.out.println("Got " + topHostsWeb.size() + " top hosts");
		return topHostsWeb;
	}

	private static void head(Collection<?> entries, int count) {
		System.out.println("Limit to " + count + " first entries");
		int i = 0;
		for (Iterator<?> it = entries.iterator(); it.hasNext(); ++i) {
			it.next();
			if (i >= count) it.remove();
		}
	}
	private static void tail(Collection<?> entries, int count) {
		System.out.println("Limit to " + count + " last entries");
		int i = 0;
		int n = entries.size();
		for (Iterator<?> it = entries.iterator(); it.hasNext(); ++i) {
			it.next();
			if (n - i > count) it.remove();
		}
	}

	private static void putPub(List<Publication> publications, String database) throws IOException {
		System.out.println("Put " + publications.size() + " publications to database: " + database);
		try (Database db = new Database(database)) {
			for (Publication publication : publications) {
				db.putPublication(publication);
				db.commit();
			}
		}
		System.out.println("Put publications: success");
	}
	private static void putWeb(List<Webpage> webpages, String database) throws IOException {
		System.out.println("Put " + webpages.size() + " webpages to database: " + database);
		try (Database db = new Database(database)) {
			for (Webpage webpage : webpages) {
				db.putWebpage(webpage);
				db.commit();
			}
		}
		System.out.println("Put webpages: success");
	}
	private static void putDoc(List<Webpage> docs, String database) throws IOException {
		System.out.println("Put " + docs.size() + " docs to database: " + database);
		try (Database db = new Database(database)) {
			for (Webpage doc : docs) {
				db.putWebpage(doc);
				db.commit();
			}
		}
		System.out.println("Put docs: success");
	}

	private static void removePub(List<Publication> publications, String database) throws IOException {
		System.out.println("Remove " + publications.size() + " publications from database: " + database);
		int fail = 0;
		try (Database db = new Database(database)) {
			for (Publication publication : publications) {
				if (!db.removePublication(publication)) {
					System.err.println("Failed to remove publication: " + publication.toStringId());
					++fail;
				} else db.commit();
			}
		}
		if (fail > 0) System.err.println("Failed to remove " + fail + " publications");
		else System.out.println("Remove publications: success");
	}
	private static void removeWeb(List<Webpage> webpages, String database) throws IOException {
		System.out.println("Remove " + webpages.size() + " webpages from database: " + database);
		int fail = 0;
		try (Database db = new Database(database)) {
			for (Webpage webpage : webpages) {
				if (!db.removeWebpage(webpage)) {
					System.err.println("Failed to remove webpage: " + webpage.toStringId());
					++fail;
				} else db.commit();
			}
		}
		if (fail > 0) System.err.println("Failed to remove " + fail + " webpages");
		else System.out.println("Remove webpages: success");
	}
	private static void removeDoc(List<Webpage> docs, String database) throws IOException {
		System.out.println("Remove " + docs.size() + " docs from database: " + database);
		int fail = 0;
		try (Database db = new Database(database)) {
			for (Webpage doc : docs) {
				if (!db.removeDoc(doc)) {
					System.err.println("Failed to remove doc: " + doc.toStringId());
					++fail;
				} else db.commit();
			}
		}
		if (fail > 0) System.err.println("Failed to remove " + fail + " docs");
		else System.out.println("Remove docs: success");
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
				if (parts.contains(PublicationPartName.title)) pubString.add("<h2>" + publication.getTitle().toStringPlainHtml() + "</h2>");
				if (parts.contains(PublicationPartName.keywords)) pubString.add(publication.getKeywords().toStringPlainHtml());
				if (parts.contains(PublicationPartName.mesh)) pubString.add(publication.getMeshTerms().toStringPlainHtml());
				if (parts.contains(PublicationPartName.efo)) pubString.add(publication.getEfoTerms().toStringPlainHtml());
				if (parts.contains(PublicationPartName.go)) pubString.add(publication.getGoTerms().toStringPlainHtml());
				if (parts.contains(PublicationPartName.theAbstract)) pubString.add(publication.getAbstract().toStringPlainHtml());
				if (parts.contains(PublicationPartName.fulltext)) pubString.add(publication.getFulltext().toStringPlainHtml());
			} else {
				if (parts.contains(PublicationPartName.pmid)
						|| parts.contains(PublicationPartName.pmcid)
						|| parts.contains(PublicationPartName.doi)) {
					pubString.add(PublicationIds.toString(
						parts.contains(PublicationPartName.pmid) ? publication.getPmid().getContent() : "",
						parts.contains(PublicationPartName.pmcid) ? publication.getPmcid().getContent() : "",
						parts.contains(PublicationPartName.doi) ? publication.getDoi().getContent() : "", true));
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
				if (parts.contains(PublicationPartName.pmid)) pubString.add(publication.getPmid().toStringHtml());
				if (parts.contains(PublicationPartName.pmcid)) pubString.add(publication.getPmcid().toStringHtml());
				if (parts.contains(PublicationPartName.doi)) pubString.add(publication.getDoi().toStringHtml());
				if (parts.contains(PublicationPartName.title)) pubString.add(publication.getTitle().toStringHtml());
				if (parts.contains(PublicationPartName.keywords)) pubString.add(publication.getKeywords().toStringHtml());
				if (parts.contains(PublicationPartName.mesh)) pubString.add(publication.getMeshTerms().toStringHtml());
				if (parts.contains(PublicationPartName.efo)) pubString.add(publication.getEfoTerms().toStringHtml());
				if (parts.contains(PublicationPartName.go)) pubString.add(publication.getGoTerms().toStringHtml());
				if (parts.contains(PublicationPartName.theAbstract)) pubString.add(publication.getAbstract().toStringHtml());
				if (parts.contains(PublicationPartName.fulltext)) pubString.add(publication.getFulltext().toStringHtml());
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
				if (html) {
					ps.println("<p>" + entry.toStringIdHtml() + "</p>");
					ps.println(entry.toStringPlainHtml());
				} else {
					ps.println(entry.toStringId());
					ps.println();
					ps.println(entry.toStringPlain());
				}
			} else {
				if (html) ps.println(entry.toStringHtml());
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
		System.out.println("Output " + entries.size() + " entries" + (plain ? " without metadata" : "")
			+ (parts != null ? " with parts " + parts : "") + (html ? " in HTML" : ""));
		print(System.out, entries, plain, html, parts);
	}
	private static <T extends DatabaseEntry<T>> void txt(List<T> entries, boolean plain, boolean html, List<PublicationPartName> parts, String txt) throws IOException {
		System.out.println("Output " + entries.size() + " entries to file " + txt + (plain ? " without metadata" : "")
			+ (parts != null ? " with parts" + parts : "") + (html ? " in HTML" : ""));
		try (PrintStream ps = new PrintStream(new BufferedOutputStream(Files.newOutputStream(FetcherCommon.outputPath(txt, false))), true, "UTF-8")) {
			if (entries.size() == 0) return;
			print(ps, entries, plain, html, parts);
		}
	}

	private static void printTopHosts(PrintStream ps, Map<String, Integer> topHosts, boolean html) throws IOException {
		if (html) ps.println("<ul>");
		for (Map.Entry<String, Integer> topHost : topHosts.entrySet()) {
			if (html) ps.println("<li value=\"" + topHost.getValue() + "\">" + topHost.getKey() + "</li>");
			else ps.println(topHost.getKey() + "\t" + topHost.getValue());
		}
		if (html) ps.println("</ul>");
	}
	private static void outTopHosts(Map<String, Integer> topHosts, boolean html) throws IOException {
		if (topHosts.size() == 0) return;
		System.out.println("Output " + topHosts.size() + " top hosts" + (html ? " in HTML" : ""));
		printTopHosts(System.out, topHosts, html);
	}
	private static void txtTopHosts(Map<String, Integer> topHosts, boolean html, String txt) throws IOException {
		System.out.println("Output " + topHosts.size() + " top hosts to file " + txt + (html ? " in HTML" : ""));
		try (PrintStream ps = new PrintStream(new BufferedOutputStream(Files.newOutputStream(FetcherCommon.outputPath(txt, false))), true, "UTF-8")) {
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

	private static void checkPartArg(String partArgName, FetcherUtilArgs args) throws ReflectiveOperationException {
		String partArgPartName = partArgName + "Part";
		Field partArg = FetcherUtilArgs.class.getDeclaredField(partArgName);
		Field partArgPart = FetcherUtilArgs.class.getDeclaredField(partArgPartName);
		Object partArgObject = partArg.get(args);
		Object partArgPartObject = partArgPart.get(args);
		if (partArgObject == null && partArgPartObject != null || partArgObject != null && partArgPartObject == null) {
			String partArgParameter = Arrays.toString(FetcherUtilArgs.class.getDeclaredField(partArgName).getAnnotation(Parameter.class).names());
			String partArgPartParameter = Arrays.toString(FetcherUtilArgs.class.getDeclaredField(partArgPartName).getAnnotation(Parameter.class).names());
			if (partArgObject == null && partArgPartObject != null) {
				throw new ParameterException("If " + partArgPartParameter + " is specified, then " + partArgParameter + " must also be specified");
			} else {
				throw new ParameterException("If " + partArgParameter + " is specified, then " + partArgPartParameter + " must also be specified");
			}
		}
	}

	private static void invokePartArg(String partArgName, FetcherUtilArgs args, List<Publication> publications) throws ReflectiveOperationException {
		invokePartArg(partArgName, args, publications, null);
	}
	private static void invokePartArg(String partArgName, FetcherUtilArgs args, List<Publication> publications, Boolean not) throws ReflectiveOperationException {
		String partArgObjectName;
		if (not != null && not) {
			partArgObjectName = "not" + partArgName.substring(0, 1).toUpperCase(Locale.ROOT) + partArgName.substring(1);
		} else {
			partArgObjectName = partArgName;
		}
		Object partArgObject = FetcherUtilArgs.class.getDeclaredField(partArgObjectName).get(args);
		if (partArgObject != null) {
			Object partArgPartObject = FetcherUtilArgs.class.getDeclaredField(partArgObjectName + "Part").get(args);
			if (not != null) {
				FetcherUtil.class.getDeclaredMethod(partArgName, publications.getClass(), partArgPartObject.getClass(), partArgObject.getClass(), not.getClass())
					.invoke(null, publications, partArgPartObject, partArgObject, not);
			} else {
				FetcherUtil.class.getDeclaredMethod(partArgName, publications.getClass(), partArgPartObject.getClass(), partArgObject.getClass())
					.invoke(null, publications, partArgPartObject, partArgObject);
			}
		}
	}

	public static void run(FetcherUtilArgs args, Fetcher fetcher, List<PublicationIds> externalPublicationIds,
			List<String> externalWebpageUrls, List<String> externalDocUrls) throws IOException, ReflectiveOperationException {
		if (args == null) {
			throw new IllegalArgumentException("FetcherUtilArgs required!");
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
			String fetchPart = Arrays.toString(FetcherUtilArgs.class.getDeclaredField("fetchPart").getAnnotation(Parameter.class).names());
			String notFetchPart = Arrays.toString(FetcherUtilArgs.class.getDeclaredField("notFetchPart").getAnnotation(Parameter.class).names());
			throw new ParameterException("Parameters " + fetchPart + " and " + notFetchPart + " can't be specified at the same time");
		}

		FetcherArgs fetcherArgs = fetcher.getFetcherArgs();

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
				Boolean.valueOf(args.printWebpageSelector.get(3)), false, fetcher);
		}
		if (args.printWebpageSelectorHtml != null) {
			printWebpageSelector(args.printWebpageSelectorHtml.get(0), args.printWebpageSelectorHtml.get(1), args.printWebpageSelectorHtml.get(2),
				Boolean.valueOf(args.printWebpageSelectorHtml.get(3)), true, fetcher);
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

		if (args.printEuropepmcXml != null) printEuropepmcXml(args.printEuropepmcXml, fetcher, parts);
		if (args.testEuropepmcXml) testEuropepmcXml(fetcher, parts);

		if (args.printEuropepmcHtml != null) printEuropepmcHtml(args.printEuropepmcHtml, fetcher, parts);
		if (args.testEuropepmcHtml) testEuropepmcHtml(fetcher, parts);

		if (args.printPmcXml != null) printPmcXml(args.printPmcXml, fetcher, parts);
		if (args.testPmcXml) testPmcXml(fetcher, parts);

		if (args.printPmcHtml != null) printPmcHtml(args.printPmcHtml, fetcher, parts);
		if (args.testPmcHtml) testPmcHtml(fetcher, parts);

		if (args.printPubmedXml != null) printPubmedXml(args.printPubmedXml, fetcher, parts);
		if (args.testPubmedXml) testPubmedXml(fetcher, parts);

		if (args.printPubmedHtml != null) printPubmedHtml(args.printPubmedHtml, fetcher, parts);
		if (args.testPubmedHtml) testPubmedHtml(fetcher, parts);

		if (args.printEuropepmc != null) printEuropepmc(args.printEuropepmc, fetcher, parts);
		if (args.testEuropepmc) testEuropepmc(fetcher, parts);

		if (args.printEuropepmcMined != null) printEuropepmcMined(args.printEuropepmcMined, fetcher, parts);
		if (args.testEuropepmcMined) testEuropepmcMined(fetcher, parts);

		if (args.printOaDoi != null) printOaDoi(args.printOaDoi, fetcher, parts);
		if (args.testOaDoi) testOaDoi(fetcher, parts);

		if (args.printSite != null) printSite(args.printSite, fetcher, parts);
		if (args.testSite) testSite(fetcher, parts);

		if (args.printWebpage != null) printWebpage(args.printWebpage, fetcher);
		if (args.testWebpage) testWebpage(fetcher);

		// add IDs

		Set<PublicationIds> publicationIds = new LinkedHashSet<>();
		Set<String> webpageUrls = new LinkedHashSet<>();
		Set<String> docUrls = new LinkedHashSet<>();

		if (externalPublicationIds != null) {
			publicationIds.addAll(pubCheck(externalPublicationIds));
			System.out.println("Got " + publicationIds.size() + " new distinct external publication IDs");
		}
		if (externalWebpageUrls != null) {
			webpageUrls.addAll(web(externalWebpageUrls));
			System.out.println("Got " + webpageUrls.size() + " new distinct external webpage URLs");
		}
		if (externalDocUrls != null) {
			docUrls.addAll(web(externalDocUrls));
			System.out.println("Got " + docUrls.size() + " new distinct external doc URLs");
		}

		if (args.pubFile != null) {
			int sizeBefore = publicationIds.size();
			publicationIds.addAll(pubCheck(pubFile(args.pubFile)));
			System.out.println("Got " + (publicationIds.size() - sizeBefore) + " new distinct publication IDs from file " + args.pubFile);
		}
		if (args.webFile != null) {
			int sizeBefore = webpageUrls.size();
			webpageUrls.addAll(web(webFile(args.webFile)));
			System.out.println("Got " + (webpageUrls.size() - sizeBefore) + " new distinct webpage URLs from file " + args.webFile);
		}
		if (args.docFile != null) {
			int sizeBefore = docUrls.size();
			docUrls.addAll(web(webFile(args.docFile)));
			System.out.println("Got " + (docUrls.size() - sizeBefore) + " new distinct doc URLs from file " + args.docFile);
		}

		if (args.pub != null) {
			int sizeBefore = publicationIds.size();
			publicationIds.addAll(pub(args.pub));
			System.out.println("Got " + (publicationIds.size() - sizeBefore) + " new distinct publication IDs from command line");
		}
		if (args.web != null) {
			int sizeBefore = webpageUrls.size();
			webpageUrls.addAll(web(args.web));
			System.out.println("Got " + (webpageUrls.size() - sizeBefore) + " new distinct webpage URLs from command line");
		}
		if (args.doc != null) {
			int sizeBefore = docUrls.size();
			docUrls.addAll(web(args.doc));
			System.out.println("Got " + (docUrls.size() - sizeBefore) + " new distinct doc URLs from command line");
		}

		if (args.pubDb != null) {
			int sizeBefore = publicationIds.size();
			publicationIds.addAll(pubDb(args.pubDb));
			System.out.println("Got " + (publicationIds.size() - sizeBefore) + " new distinct publication IDs from database " + args.pubDb);
		}
		if (args.webDb != null) {
			int sizeBefore = webpageUrls.size();
			webpageUrls.addAll(webDb(args.webDb));
			System.out.println("Got " + (webpageUrls.size() - sizeBefore) + " new distinct webpage URLs from database " + args.webDb);
		}
		if (args.docDb != null) {
			int sizeBefore = docUrls.size();
			docUrls.addAll(docDb(args.docDb));
			System.out.println("Got " + (docUrls.size() - sizeBefore) + " new distinct doc URLs from database " + args.docDb);
		}
		if (args.allDb != null) {
			int sizeBefore = publicationIds.size();
			publicationIds.addAll(pubDb(args.pubDb));
			System.out.println("Got " + (publicationIds.size() - sizeBefore) + " new distinct publication IDs from database " + args.allDb);

			sizeBefore = webpageUrls.size();
			webpageUrls.addAll(webDb(args.webDb));
			System.out.println("Got " + (webpageUrls.size() - sizeBefore) + " new distinct webpage URLs from database " + args.allDb);

			sizeBefore = docUrls.size();
			docUrls.addAll(docDb(args.docDb));
			System.out.println("Got " + (docUrls.size() - sizeBefore) + " new distinct doc URLs from database " + args.allDb);
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
			publications.addAll(fetchPub(publicationIds, fetcher, parts));
			webpages.addAll(fetchWeb(webpageUrls, fetcher));
			docs.addAll(fetchWeb(docUrls, fetcher));
		}

		if (args.dbFetch != null) {
			publications.addAll(dbFetchPub(publicationIds, args.dbFetch, fetcher, parts));
			webpages.addAll(dbFetchWeb(webpageUrls, args.dbFetch, fetcher));
			docs.addAll(dbFetchDoc(docUrls, args.dbFetch, fetcher));
		}

		if (args.fetchPut != null) {
			publications.addAll(fetchPutPub(publicationIds, args.fetchPut, fetcher, parts));
			webpages.addAll(fetchPutWeb(webpageUrls, args.fetchPut, fetcher));
			docs.addAll(fetchPutDoc(docUrls, args.fetchPut, fetcher));
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

	public static <T extends MainArgs> T parseArgs(String[] argv, Class<T> clazz) throws ReflectiveOperationException {
		T args = clazz.getConstructor().newInstance();
		JCommander jcommander = new JCommander(args);
		try {
			jcommander.parse(argv);
		} catch (ParameterException e) {
			System.err.println(e);
			System.err.println("Use -h or --help for listing valid options");
			System.exit(1);
		}
		if (args.isHelp()) {
			jcommander.usage();
			System.exit(0);
		}
		return args;
	}
}
