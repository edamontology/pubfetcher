package org.edamontology.pubfetcher.core.fetching;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.MissingResourceException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.edamontology.pubfetcher.core.common.FetcherArgs;
import org.edamontology.pubfetcher.core.common.PubFetcher;
import org.edamontology.pubfetcher.core.db.link.Link;
import org.edamontology.pubfetcher.core.db.publication.CorrespAuthor;
import org.edamontology.pubfetcher.core.db.publication.Publication;
import org.edamontology.pubfetcher.core.db.publication.PublicationIds;
import org.edamontology.pubfetcher.core.db.publication.PublicationPartName;
import org.edamontology.pubfetcher.core.db.publication.PublicationPartType;
import org.edamontology.pubfetcher.core.db.webpage.Webpage;

public final class FetcherTest {

	private static final Logger logger = LogManager.getLogger();

	private static String PUB_ID_SOURCE = "PubFetcherTest";

	private static InputStream getResource(String name, String prefix) {
		InputStream resource = FetcherTest.class.getResourceAsStream("/" + prefix + "/" + name);
		if (resource == null) {
			throw new MissingResourceException("Can't find test CSV resource '" + name + "'!", FetcherTest.class.getSimpleName(), name);
		}
		return resource;
	}

	private static List<String[]> getTest(String resource, String prefix, int columns) throws IOException {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(getResource(resource, prefix), StandardCharsets.UTF_8))) {
			return br.lines()
				.skip(1)
				.map(String::trim)
				.filter(l -> !l.isEmpty() && l.charAt(0) != '#')
				.map(l -> l.split(",", columns))
				.filter(l -> {
					if (l.length != columns) throw new RuntimeException("Line containing " + l.length + " fields instead of required " + columns + " in " + resource + ": " + Arrays.stream(l).collect(Collectors.joining(",")));
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
			logger.error(e);
			return 1;
		}
		if (testInt != actual) {
			logger.error("{} must be {}, actually is {}" , label, testInt, actual);
			return 1;
		} else {
			return 0;
		}
	}
	private static int equal(String test, String actual, String label) {
		if (!test.equals(actual)) {
			logger.error("{} must be {}, actually is {}", label, test, actual);
			return 1;
		} else {
			return 0;
		}
	}

	private static boolean fetchPart(EnumMap<PublicationPartName, Boolean> parts, PublicationPartName part) {
		return parts == null || (parts.get(part) != null && parts.get(part));
	}

	private static int testPmc(String[] test, Publication publication, EnumMap<PublicationPartName, Boolean> parts) {
		int mismatch = 0;
		if (fetchPart(parts, PublicationPartName.pmid)) {
			mismatch += equal(test[1], publication.getPmid().getContent().length(), "PMID length");
		}
		if (fetchPart(parts, PublicationPartName.pmcid)) {
			mismatch += equal(test[2], publication.getPmcid().getContent().length(), "PMCID length");
		}
		if (fetchPart(parts, PublicationPartName.doi)) {
			mismatch += equal(test[3], publication.getDoi().getContent().length(), "DOI length");
		}
		if (fetchPart(parts, PublicationPartName.title)) {
			mismatch += equal(test[4], publication.getTitle().getContent().length(), "title length");
		}
		if (fetchPart(parts, PublicationPartName.keywords)) {
			mismatch += equal(test[5], publication.getKeywords().getList().size(), "keywords size");
		}
		if (fetchPart(parts, PublicationPartName.theAbstract)) {
			mismatch += equal(test[6], publication.getAbstract().getContent().length(), "abstract length");
		}
		if (fetchPart(parts, PublicationPartName.fulltext)) {
			mismatch += equal(test[7], publication.getFulltext().getContent().length(), "fulltext length");
		}
		mismatch += equal(test[8], CorrespAuthor.toString(publication.getCorrespAuthor()).length(), "corresponding author length");
		return mismatch;
	}

	@SuppressWarnings("unused")
	private static int testPmcXml(String[] test, Publication publication, EnumMap<PublicationPartName, Boolean> parts) {
		int mismatch = testPmc(test, publication, parts);
		mismatch += equal(test[9], publication.getJournalTitle().length(), "journal title length");
		return mismatch;
	}

	@SuppressWarnings("unused")
	private static int testPmcHtml(String[] test, Publication publication, EnumMap<PublicationPartName, Boolean> parts) {
		int mismatch = testPmc(test, publication, parts);
		mismatch += equal(test[9], publication.getVisitedSites().size(), "visited sites size");
		return mismatch;
	}

	private static int testPubmedHtml(String[] test, Publication publication, EnumMap<PublicationPartName, Boolean> parts) {
		int mismatch = 0;
		if (fetchPart(parts, PublicationPartName.pmid)) {
			mismatch += equal(test[1], publication.getPmid().getContent().length(), "PMID length");
		}
		if (fetchPart(parts, PublicationPartName.pmcid)) {
			mismatch += equal(test[2], publication.getPmcid().getContent().length(), "PMCID length");
		}
		if (fetchPart(parts, PublicationPartName.doi)) {
			mismatch += equal(test[3], publication.getDoi().getContent().length(), "DOI length");
		}
		if (fetchPart(parts, PublicationPartName.title)) {
			mismatch += equal(test[4], publication.getTitle().getContent().length(), "title length");
		}
		if (fetchPart(parts, PublicationPartName.keywords)) {
			mismatch += equal(test[5], publication.getKeywords().getList().size(), "keywords size");
		}
		if (fetchPart(parts, PublicationPartName.mesh)) {
			mismatch += equal(test[6], publication.getMeshTerms().getList().size(), "MeSH terms size");
		}
		if (fetchPart(parts, PublicationPartName.theAbstract)) {
			mismatch += equal(test[7], publication.getAbstract().getContent().length(), "abstract length");
		}
		return mismatch;
	}

	private static int testPubmedXml(String[] test, Publication publication, EnumMap<PublicationPartName, Boolean> parts) {
		int mismatch = testPubmedHtml(test, publication, parts);
		mismatch += equal(test[8], publication.getJournalTitle().length(), "journal title length");
		mismatch += equal(test[9], publication.getPubDateHuman(), "publication date");
		return mismatch;
	}

	private static int testEuropepmc(String[] test, Publication publication, FetcherPublicationState state, EnumMap<PublicationPartName, Boolean> parts) {
		int mismatch = testPubmedXml(test, publication, parts);
		mismatch += equal(test[10], publication.isOA() ? 1 : 0, "Open Access");
		mismatch += equal(test[11], state.europepmcHasFulltextHTML ? 1 : 0, "has HTML");
		mismatch += equal(test[12], state.europepmcHasPDF ? 1 : 0, "has PDF");
		mismatch += equal(test[13], state.europepmcHasMinedTerms ? 1 : 0, "has mined");
		return mismatch;
	}

	@SuppressWarnings("unused")
	private static int testEuropepmcMined(String[] test, Publication publication, EnumMap<PublicationPartName, Boolean> parts) {
		int mismatch = 0;
		if (fetchPart(parts, PublicationPartName.efo)) {
			mismatch += equal(test[1], publication.getEfoTerms().getList().size(), "EFO terms size");
		}
		if (fetchPart(parts, PublicationPartName.go)) {
			mismatch += equal(test[2], publication.getGoTerms().getList().size(), "GO terms size");
		}
		return mismatch;
	}

	@SuppressWarnings("unused")
	private static int testOaDoi(String[] test, Publication publication, EnumMap<PublicationPartName, Boolean> parts) {
		int mismatch = 0;
		mismatch += equal(test[1], publication.isOA() ? 1 : 0, "Open Access");
		mismatch += equal(test[2], publication.getVisitedSites().size(), "visited sites size");
		mismatch += equal(test[3], publication.getTitle().getContent().length(), "title length");
		mismatch += equal(test[4], publication.getJournalTitle().length(), "journal title length");
		return mismatch;
	}

	private static int testSite(String[] test, Publication publication, EnumMap<PublicationPartName, Boolean> parts) {
		int mismatch = 0;
		if (fetchPart(parts, PublicationPartName.pmid)) {
			mismatch += equal(test[0], publication.getPmid().getContent().length(), "PMID length");
		}
		if (fetchPart(parts, PublicationPartName.pmcid)) {
			mismatch += equal(test[1], publication.getPmcid().getContent().length(), "PMCID length");
		}
		if (fetchPart(parts, PublicationPartName.doi)) {
			mismatch += equal(test[2], publication.getDoi().getContent().length(), "DOI length");
		}
		if (fetchPart(parts, PublicationPartName.title)) {
			mismatch += equal(test[3], publication.getTitle().getContent().length(), "title length");
		}
		if (fetchPart(parts, PublicationPartName.keywords)) {
			mismatch += equal(test[4], publication.getKeywords().getList().size(), "keywords size");
		}
		if (fetchPart(parts, PublicationPartName.theAbstract)) {
			mismatch += equal(test[5], publication.getAbstract().getContent().length(), "abstract length");
		}
		if (fetchPart(parts, PublicationPartName.fulltext)) {
			mismatch += equal(test[6], publication.getFulltext().getContent().length(), "fulltext length");
		}
		mismatch += equal(test[7], publication.getVisitedSites().size(), "visited sites size");
		mismatch += equal(test[8], CorrespAuthor.toString(publication.getCorrespAuthor()).length(), "corresponding author length");
		return mismatch;
	}

	private static void test(List<String[]> tests, String fetchMethod, String testMethod, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) throws ReflectiveOperationException {
		int mismatch = 0;
		int i = 0;
		long start = System.currentTimeMillis();
		for (String[] test : tests) {
			++i;
			logger.info("Test {} {}", test[0], PubFetcher.progress(i, tests.size(), start));
			Publication publication = (Publication) FetcherTest.class.getDeclaredMethod(fetchMethod, test[0].getClass(), fetcher.getClass(), EnumMap.class, fetcherArgs.getClass())
				.invoke(null, test[0], fetcher, parts, fetcherArgs);
			if (publication != null) {
				mismatch += (Integer) FetcherTest.class.getDeclaredMethod(testMethod, test.getClass(), publication.getClass(), EnumMap.class)
					.invoke(null, test, publication, parts);
			} else ++mismatch;
		}
		if (mismatch == 0) logger.info("OK");
		else logger.error("There were {} mismatches!", mismatch);
	}

	private static Publication fetchEuropepmcXml(String pmcid, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) {
		Publication publication = fetcher.initPublication(new PublicationIds("", pmcid, "", "", PUB_ID_SOURCE, ""), fetcherArgs);
		if (publication != null) {
			FetcherPublicationState state = new FetcherPublicationState();
			state.europepmcHasFulltextXML = true;
			fetcher.fetchEuropepmcFulltextXml(publication, state, parts, fetcherArgs);
		}
		return publication;
	}

	private static void printEuropepmcXml(String pmcid, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) {
		System.out.println(fetchEuropepmcXml(pmcid, fetcher, parts, fetcherArgs));
	}

	private static void testEuropepmcXml(Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) throws IOException, ReflectiveOperationException {
		test(getTest("europepmc-xml.csv", "test", 10), "fetchEuropepmcXml", "testPmcXml", fetcher, parts, fetcherArgs);
	}

	private static Publication fetchEuropepmcHtml(String pmcid, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) {
		Publication publication = fetcher.initPublication(new PublicationIds("", pmcid, "", "", PUB_ID_SOURCE, ""), fetcherArgs);
		if (publication != null) {
			FetcherPublicationState state = new FetcherPublicationState();
			state.europepmcHasFulltextHTML = true;
			Links links = new Links();
			fetcher.fetchEuropepmcFulltextHtml(publication, links, state, parts, false, fetcherArgs);
			for (Link link : links.getLinks()) publication.addVisitedSite(link);
		}
		return publication;
	}

	private static void printEuropepmcHtml(String pmcid, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) {
		System.out.println(fetchEuropepmcHtml(pmcid, fetcher, parts, fetcherArgs));
	}

	private static void testEuropepmcHtml(Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) throws IOException, ReflectiveOperationException {
		test(getTest("europepmc-html.csv", "test", 10), "fetchEuropepmcHtml", "testPmcHtml", fetcher, parts, fetcherArgs);
	}

	private static Publication fetchPmcXml(String pmcid, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) {
		Publication publication = fetcher.initPublication(new PublicationIds("", pmcid, "", "", PUB_ID_SOURCE, ""), fetcherArgs);
		if (publication != null) {
			fetcher.fetchPmcXml(publication, new FetcherPublicationState(), parts, fetcherArgs);
		}
		return publication;
	}

	private static void printPmcXml(String pmcid, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) {
		System.out.println(fetchPmcXml(pmcid, fetcher, parts, fetcherArgs));
	}

	private static void testPmcXml(Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) throws IOException, ReflectiveOperationException {
		test(getTest("pmc-xml.csv", "test", 10), "fetchPmcXml", "testPmcXml", fetcher, parts, fetcherArgs);
	}

	private static Publication fetchPmcHtml(String pmcid, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) {
		Publication publication = fetcher.initPublication(new PublicationIds("", pmcid, "", "", PUB_ID_SOURCE, ""), fetcherArgs);
		if (publication != null) {
			Links links = new Links();
			fetcher.fetchPmcHtml(publication, links, new FetcherPublicationState(), parts, false, fetcherArgs);
			for (Link link : links.getLinks()) publication.addVisitedSite(link);
		}
		return publication;
	}

	private static void printPmcHtml(String pmcid, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) {
		System.out.println(fetchPmcHtml(pmcid, fetcher, parts, fetcherArgs));
	}

	private static void testPmcHtml(Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) throws IOException, ReflectiveOperationException {
		test(getTest("pmc-html.csv", "test", 10), "fetchPmcHtml", "testPmcHtml", fetcher, parts, fetcherArgs);
	}

	private static Publication fetchPubmedXml(String pmid, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) {
		Publication publication = fetcher.initPublication(new PublicationIds(pmid, "", "", PUB_ID_SOURCE, "", ""), fetcherArgs);
		if (publication != null) {
			fetcher.fetchPubmedXml(publication, new FetcherPublicationState(), parts, fetcherArgs);
		}
		return publication;
	}

	private static void printPubmedXml(String pmid, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) {
		System.out.println(fetchPubmedXml(pmid, fetcher, parts, fetcherArgs));
	}

	private static void testPubmedXml(Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) throws IOException, ReflectiveOperationException {
		test(getTest("pubmed-xml.csv", "test", 10), "fetchPubmedXml", "testPubmedXml", fetcher, parts, fetcherArgs);
	}

	private static Publication fetchPubmedHtml(String pmid, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) {
		Publication publication = fetcher.initPublication(new PublicationIds(pmid, "", "", PUB_ID_SOURCE, "", ""), fetcherArgs);
		if (publication != null) {
			fetcher.fetchPubmedHtml(publication, new FetcherPublicationState(), parts, fetcherArgs);
		}
		return publication;
	}

	private static void printPubmedHtml(String pmid, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) {
		System.out.println(fetchPubmedHtml(pmid, fetcher, parts, fetcherArgs));
	}

	private static void testPubmedHtml(Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) throws IOException, ReflectiveOperationException {
		test(getTest("pubmed-html.csv", "test", 8), "fetchPubmedHtml", "testPubmedHtml", fetcher, parts, fetcherArgs);
	}

	private static void printEuropepmc(String pmid, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) {
		Publication publication = fetcher.initPublication(new PublicationIds(pmid, "", "", PUB_ID_SOURCE, "", ""), fetcherArgs);
		if (publication != null) {
			fetcher.fetchEuropepmc(publication, new FetcherPublicationState(), parts, fetcherArgs);
		}
		System.out.println(publication);
	}

	private static void testEuropepmc(Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) throws IOException, ReflectiveOperationException {
		int mismatch = 0;
		List<String[]> tests = getTest("europepmc.csv", "test", 14);
		int i = 0;
		long start = System.currentTimeMillis();
		for (String[] test : tests) {
			++i;
			logger.info("Test {} {}", test[0], PubFetcher.progress(i, tests.size(), start));
			Publication publication = fetcher.initPublication(new PublicationIds(test[0], "", "", PUB_ID_SOURCE, "", ""), fetcherArgs);
			if (publication != null) {
				FetcherPublicationState state = new FetcherPublicationState();
				fetcher.fetchEuropepmc(publication, state, parts, fetcherArgs);
				mismatch += testEuropepmc(test, publication, state, parts);
			} else ++mismatch;
		}
		if (mismatch == 0) logger.info("OK");
		else logger.error("There were {} mismatches!", mismatch);
	}

	private static Publication fetchEuropepmcMined(String pmid, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) {
		Publication publication = fetcher.initPublication(new PublicationIds(pmid, "", "", PUB_ID_SOURCE, "", ""), fetcherArgs);
		if (publication != null) {
			FetcherPublicationState state = new FetcherPublicationState();
			state.europepmcHasMinedTerms = true;
			fetcher.fetchEuropepmcMinedTermsEfo(publication, state, parts, fetcherArgs);
			fetcher.fetchEuropepmcMinedTermsGo(publication, state, parts, fetcherArgs);
		}
		return publication;
	}

	private static void printEuropepmcMined(String pmid, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) {
		System.out.println(fetchEuropepmcMined(pmid, fetcher, parts, fetcherArgs));
	}

	private static void testEuropepmcMined(Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) throws IOException, ReflectiveOperationException {
		test(getTest("europepmc-mined.csv", "test", 3), "fetchEuropepmcMined", "testEuropepmcMined", fetcher, parts, fetcherArgs);
	}

	private static Publication fetchOaDoi(String doi, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) {
		Publication publication = fetcher.initPublication(new PublicationIds("", "", doi, "", "", PUB_ID_SOURCE), fetcherArgs);
		if (publication != null) {
			Links links = new Links();
			fetcher.fetchOaDoi(publication, links, new FetcherPublicationState(), parts, fetcherArgs);
			for (Link link : links.getLinks()) publication.addVisitedSite(link);
		}
		return publication;
	}

	private static void printOaDoi(String doi, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) {
		System.out.println(fetchOaDoi(doi, fetcher, parts, fetcherArgs));
	}

	private static void testOaDoi(Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) throws IOException, ReflectiveOperationException {
		test(getTest("oadoi.csv", "test", 5), "fetchOaDoi", "testOaDoi", fetcher, parts, fetcherArgs);
	}

	private static Publication fetchSite(String url, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) {
		Publication publication = new Publication();
		Links links = new Links();
		fetcher.fetchSite(publication, url, PublicationPartType.doi, PUB_ID_SOURCE, links, parts, false, true, fetcherArgs);
		for (Link link : links.getLinks()) publication.addVisitedSite(link);
		return publication;
	}

	private static void printSite(String url, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) {
		System.out.println(fetchSite(url, fetcher, parts, fetcherArgs));
	}

	private static void filterTests(List<String[]> tests, String regex, int column) {
		if (regex != null) {
			Pattern pattern = Pattern.compile(regex);
			for (Iterator<String[]> it = tests.iterator(); it.hasNext(); ) {
				if (!pattern.matcher(it.next()[column]).find()) {
					it.remove();
				}
			}
		}
	}

	private static void testSite(Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs, String regex) throws IOException, ReflectiveOperationException {
		int mismatch = 0;
		List<String[]> tests = getTest("journals.csv", "scrape", 10);
		filterTests(tests, regex, 9);
		int i = 0;
		long start = System.currentTimeMillis();
		for (String[] test : tests) {
			++i;
			logger.info("Test {} {}", test[9], PubFetcher.progress(i, tests.size(), start));
			Publication publication = fetchSite(test[9], fetcher, parts, fetcherArgs);
			mismatch += testSite(test, publication, parts);
		}
		if (mismatch == 0) logger.info("OK");
		else logger.error("There were {} mismatches!", mismatch);
	}

	private static Webpage fetchWebpage(String url, Fetcher fetcher, FetcherArgs fetcherArgs) {
		Webpage webpage = fetcher.initWebpage(url);
		fetcher.getWebpage(webpage, fetcherArgs);
		return webpage;
	}

	private static void printWebpage(String url, Fetcher fetcher, FetcherArgs fetcherArgs) {
		System.out.println(fetchWebpage(url, fetcher, fetcherArgs));
	}

	private static void testWebpage(Fetcher fetcher, FetcherArgs fetcherArgs, String regex) throws IOException {
		int mismatch = 0;
		List<String[]> tests = getTest("webpages.csv", "scrape", 5);
		filterTests(tests, regex, 4);
		int i = 0;
		long start = System.currentTimeMillis();
		for (String[] test : tests) {
			++i;
			logger.info("Test {} {}", test[4], PubFetcher.progress(i, tests.size(), start));
			Webpage webpage = fetchWebpage(test[4], fetcher, fetcherArgs);
			mismatch += equal(test[0], webpage.getTitle().length(), "title length");
			mismatch += equal(test[1], webpage.getContent().length(), "content length");
			mismatch += equal(test[2], webpage.getLicense().length(), "license length");
			mismatch += equal(test[3], webpage.getLanguage().length(), "language length");
		}
		if (mismatch == 0) logger.info("OK");
		else logger.error("There were {} mismatches!", mismatch);
	}

	public static void run(FetcherTestArgs args, Fetcher fetcher, FetcherArgs fetcherArgs, EnumMap<PublicationPartName, Boolean> parts, String pubIdSource) throws IOException, ReflectiveOperationException {
		PUB_ID_SOURCE = pubIdSource;

		if (args.printEuropepmcXml != null) printEuropepmcXml(args.printEuropepmcXml, fetcher, parts, fetcherArgs);
		if (args.testEuropepmcXml) testEuropepmcXml(fetcher, parts, fetcherArgs);

		if (args.printEuropepmcHtml != null) printEuropepmcHtml(args.printEuropepmcHtml, fetcher, parts, fetcherArgs);
		if (args.testEuropepmcHtml) testEuropepmcHtml(fetcher, parts, fetcherArgs);

		if (args.printPmcXml != null) printPmcXml(args.printPmcXml, fetcher, parts, fetcherArgs);
		if (args.testPmcXml) testPmcXml(fetcher, parts, fetcherArgs);

		if (args.printPmcHtml != null) printPmcHtml(args.printPmcHtml, fetcher, parts, fetcherArgs);
		if (args.testPmcHtml) testPmcHtml(fetcher, parts, fetcherArgs);

		if (args.printPubmedXml != null) printPubmedXml(args.printPubmedXml, fetcher, parts, fetcherArgs);
		if (args.testPubmedXml) testPubmedXml(fetcher, parts, fetcherArgs);

		if (args.printPubmedHtml != null) printPubmedHtml(args.printPubmedHtml, fetcher, parts, fetcherArgs);
		if (args.testPubmedHtml) testPubmedHtml(fetcher, parts, fetcherArgs);

		if (args.printEuropepmc != null) printEuropepmc(args.printEuropepmc, fetcher, parts, fetcherArgs);
		if (args.testEuropepmc) testEuropepmc(fetcher, parts, fetcherArgs);

		if (args.printEuropepmcMined != null) printEuropepmcMined(args.printEuropepmcMined, fetcher, parts, fetcherArgs);
		if (args.testEuropepmcMined) testEuropepmcMined(fetcher, parts, fetcherArgs);

		if (args.printOaDoi != null) printOaDoi(args.printOaDoi, fetcher, parts, fetcherArgs);
		if (args.testOaDoi) testOaDoi(fetcher, parts, fetcherArgs);

		if (args.printSite != null) printSite(args.printSite, fetcher, parts, fetcherArgs);
		if (args.testSite) testSite(fetcher, parts, fetcherArgs, null);
		if (args.testSiteRegex != null) testSite(fetcher, parts, fetcherArgs, args.testSiteRegex);

		if (args.printWebpage != null) printWebpage(args.printWebpage, fetcher, fetcherArgs);
		if (args.testWebpage) testWebpage(fetcher, fetcherArgs, null);
		if (args.testWebpageRegex != null) testWebpage(fetcher, fetcherArgs, args.testWebpageRegex);
	}
}
