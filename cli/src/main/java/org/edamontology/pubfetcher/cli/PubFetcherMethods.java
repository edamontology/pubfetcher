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
import java.io.StringWriter;
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
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.fasterxml.jackson.core.JsonGenerator;

import org.edamontology.pubfetcher.core.common.FetcherArgs;
import org.edamontology.pubfetcher.core.common.IllegalRequestException;
import org.edamontology.pubfetcher.core.common.PubFetcher;
import org.edamontology.pubfetcher.core.common.Version;
import org.edamontology.pubfetcher.core.db.Database;
import org.edamontology.pubfetcher.core.db.DatabaseEntry;
import org.edamontology.pubfetcher.core.db.DatabaseEntryType;
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

	private static void dbInit(String database) throws FileAlreadyExistsException {
		logger.info("Init database: {}", database);
		Database.init(database);
		logger.info("Init: success");
	}
	private static void dbCommit(String database) throws IOException {
		logger.info("Commit database: {}", database);
		try (Database db = new Database(database)) {
			db.commit();
		}
		logger.info("Commit: success");
	}
	private static void dbCompact(String database) throws IOException {
		logger.info("Compact database: {}", database);
		try (Database db = new Database(database)) {
			db.compact();
		}
		logger.info("Compact: success");
	}

	private static void dbPublicationsSize(String database) throws IOException {
		try (Database db = new Database(database)) {
			System.out.println(db.getPublicationsSize());
		}
	}
	private static void dbWebpagesSize(String database) throws IOException {
		try (Database db = new Database(database)) {
			System.out.println(db.getWebpagesSize());
		}
	}
	private static void dbDocsSize(String database) throws IOException {
		try (Database db = new Database(database)) {
			System.out.println(db.getDocsSize());
		}
	}

	private static void dbPublicationsMap(String database) throws IOException {
		try (Database db = new Database(database)) {
			System.out.print(db.dumpPublicationsMap());
		}
	}
	private static void dbPublicationsMapReverse(String database) throws IOException {
		try (Database db = new Database(database)) {
			System.out.print(db.dumpPublicationsMapReverse());
		}
	}

	private static void fetchDocument(String url, Fetcher fetcher, FetcherArgs fetcherArgs) {
		System.out.println(fetcher.getDoc(url, false, fetcherArgs));
	}
	private static void fetchDocumentJavascript(String url, Fetcher fetcher, FetcherArgs fetcherArgs) {
		System.out.println(fetcher.getDoc(url, true, fetcherArgs));
	}
	private static void postDocument(List<String> urlData, Fetcher fetcher, FetcherArgs fetcherArgs) {
		String url = urlData.get(0);
		Map<String, String> data = new LinkedHashMap<>();
		for (int i = 1; i < urlData.size(); i += 2) {
			if (i + 1 < urlData.size()) {
				data.put(urlData.get(i), urlData.get(i + 1));
			} else {
				data.put(urlData.get(i), "");
			}
		}
		System.out.println(fetcher.postDoc(url, data, fetcherArgs));
	}
	private static void fetchWebpageSelector(String webpageUrl, String title, String content, Boolean javascript, boolean plain, Format format, Version version, String[] argv, Fetcher fetcher, FetcherArgs fetcherArgs) throws IOException {
		Webpage webpage = fetcher.initWebpage(webpageUrl);
		if (webpage == null) return;
		fetcher.getWebpage(webpage, title, content, javascript, fetcherArgs, false);
		if (plain) {
			switch (format) {
				case text: System.out.println(webpage.toStringPlain()); break;
				case html: System.out.println(webpage.toStringPlainHtml("")); break;
				case json:
					StringWriter writer = new StringWriter();
					JsonGenerator generator = PubFetcher.getJsonGenerator(null, writer);
					PubFetcher.jsonBegin(generator, version, argv);
					webpage.toStringPlainJson(generator);
					PubFetcher.jsonEnd(generator);
					generator.close();
					System.out.println(writer.toString());
					break;
			}
		} else {
			switch (format) {
				case text: System.out.println(webpage.toString()); break;
				case html: System.out.println(webpage.toStringHtml("")); break;
				case json:
					StringWriter writer = new StringWriter();
					JsonGenerator generator = PubFetcher.getJsonGenerator(null, writer);
					PubFetcher.jsonBegin(generator, version, argv);
					webpage.toStringJson(generator, fetcherArgs, true);
					PubFetcher.jsonEnd(generator);
					generator.close();
					System.out.println(writer.toString());
					break;
			}
		}
	}

	private static void scrapeSite(String url, Fetcher fetcher) {
		System.out.println(fetcher.getScrape().getSite(url));
	}
	private static void scrapeSelector(String url, ScrapeSiteKey siteKey, Fetcher fetcher) {
		System.out.println(fetcher.getScrape().getSelector(fetcher.getScrape().getSite(url), siteKey));
	}
	private static void scrapeJavascript(String url, Fetcher fetcher) {
		System.out.println(fetcher.getScrape().getJavascript(url));
	}
	private static void scrapeOff(String url, Fetcher fetcher) {
		System.out.println(fetcher.getScrape().getOff(url));
	}
	private static void scrapeWebpage(String url, Fetcher fetcher) {
		System.out.println(fetcher.getScrape().getWebpage(url));
	}

	private static void isPmid(String s) {
		System.out.println(PubFetcher.isPmid(s));
	}
	private static void isPmcid(String s) {
		System.out.println(PubFetcher.isPmcid(s));
	}
	private static void extractPmcid(String s) {
		System.out.println(PubFetcher.extractPmcid(s));
	}
	private static void isDoi(String s) {
		System.out.println(PubFetcher.isDoi(s));
	}
	private static void normaliseDoi(String s) {
		System.out.println(PubFetcher.normaliseDoi(s));
	}
	private static void extractDoiRegistrant(String s) {
		System.out.println(PubFetcher.extractDoiRegistrant(s));
	}

	private static void escapeHtml(String input) {
		System.out.println(PubFetcher.escapeHtml(input));
	}
	private static void escapeHtmlAttribute(String input) {
		System.out.println(PubFetcher.escapeHtmlAttribute(input));
	}

	private static void checkPublicationId(String publicationId) {
		System.out.println(PubFetcher.getPublicationIds(publicationId, PUB_ID_SOURCE, true).toString(true));
	}
	private static void checkPublicationIds(String pmid, String pmcid, String doi) throws IllegalRequestException {
		System.out.println(PubFetcher.getPublicationIds(pmid, pmcid, doi, PUB_ID_SOURCE, PUB_ID_SOURCE, PUB_ID_SOURCE, true, true).toString(true));
	}
	private static void checkUrl(String url) throws IllegalRequestException {
		System.out.println(PubFetcher.getUrl(url, true));
	}

	// pubFile is in PubFetcher-Core
	// webFile is in PubFetcher-Core

	private static String getIdString(DatabaseEntryType type) {
		String id = "";
		switch (type) {
			case publication: id = "ID"; break;
			case webpage: case doc: id = "URL"; break;
		}
		return id;
	}

	private static List<? extends Object> idsCheck(List<? extends Object> ids, DatabaseEntryType type) {
		if (ids.isEmpty()) {
			logger.error("Check {} {}s: no {} {}s given", type, getIdString(type), type, getIdString(type));
			return Collections.emptyList();
		}
		logger.info("Check {} {} {}s", ids.size(), type, getIdString(type));
		List<? extends Object> idsChecked = ids.stream()
			.map(s -> {
				Object id = null;
				switch (type) {
				case publication:
					if (s instanceof String) {
						id = PubFetcher.getPublicationIds((String) s, PUB_ID_SOURCE, false);
					} else {
						PublicationIds pubId = (PublicationIds) s;
						id = PubFetcher.getPublicationIds(pubId.getPmid(), pubId.getPmcid(), pubId.getDoi(), pubId.getPmidUrl(), pubId.getPmcidUrl(), pubId.getDoiUrl(), false, true);
					}
					break;
				case webpage: case doc:
					id = PubFetcher.getUrl((String) s, false);
					break;
				}
				return id;
			})
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
		if (idsChecked.size() < ids.size()) {
			logger.warn("{} {} {}s OK, {} not OK", idsChecked.size(), type, getIdString(type), ids.size() - idsChecked.size());
		} else {
			logger.info("{} {} {}s OK", idsChecked.size(), type, getIdString(type));
		}
		return idsChecked;
	}

	private static List<? extends Object> idsDb(List<String> databases, DatabaseEntryType type) throws IOException {
		List<Object> ids = new ArrayList<>();
		logger.info("Get {} {}s from database: {}", type, getIdString(type), databases);
		for (String database : databases) {
			try (Database db = new Database(database)) {
				switch (type) {
					case publication: ids.addAll(db.getPublicationIds()); break;
					case webpage: ids.addAll(db.getWebpageUrls()); break;
					case doc: ids.addAll(db.getDocUrls()); break;
				}
			}
		}
		logger.info("Got {} {} {}s", ids.size(), type, getIdString(type));
		return ids;
	}

	private static <T> void filter(Collection<T> collection, Predicate<T> filter, String what, String condition, boolean yes, boolean log) {
		if (collection == null || collection.isEmpty()) return;
		if (log) logger.info("Filter {} with {}{}: before {}", what, yes ? "" : "not ", condition, collection.size());
		for (Iterator<T> it = collection.iterator(); it.hasNext(); ) {
			if (filter.test(it.next())) {
				if (!yes) it.remove();
			} else {
				if (yes) it.remove();
			}
		}
		if (log) logger.info("Filter {} with {}{}: after {}", what, yes ? "" : "not ", condition, collection.size());
	}

	private static <T> void filterRegex(Collection<T> collection, Function<T, String> mapper, String regex, String what, String field, boolean yes, boolean log) {
		if (collection == null || collection.isEmpty()) return;
		if (log) logger.info("Filter {} with {} {}matching {}: before {}", what, field, yes ? "" : "not ", regex, collection.size());
		Pattern pattern = Pattern.compile(regex);
		for (Iterator<T> it = collection.iterator(); it.hasNext(); ) {
			if (pattern.matcher(mapper.apply(it.next())).find()) {
				if (!yes) it.remove();
			} else {
				if (yes) it.remove();
			}
		}
		if (log) logger.info("Filter {} with {} {}matching {}: after {}", what, field, yes ? "" : "not ", regex, collection.size());
	}

	private static void normaliseHosts(List<String> hosts) {
		for (int i = 0; i < hosts.size(); ++i) {
			String host = hosts.get(i).toLowerCase(Locale.ROOT);
			if (host.startsWith("www.")) {
				host = host.substring(4);
			}
			hosts.set(i, host);
		}
	}

	private static boolean hostMatches(String url, List<String> hosts) {
		boolean matches = false;
		try {
			String host = new URL(url).getHost().toLowerCase(Locale.ROOT);
			if (host.startsWith("www.")) {
				host = host.substring(4);
			}
			if (hosts.contains(host)) {
				matches = true;
			}
		} catch (MalformedURLException e) {
		}
		return matches;
	}

	private static <T> void filterHost(Collection<T> collection, Function<T, String> mapper, List<String> hosts, String what, String field, boolean yes, boolean log) {
		if (collection == null || collection.isEmpty()) return;
		normaliseHosts(hosts);
		if (log) logger.info("Filter {} with {} {}having host {}: before {}", what, field, yes ? "" : "not ", hosts, collection.size());
		for (Iterator<T> it = collection.iterator(); it.hasNext(); ) {
			if (hostMatches(mapper.apply(it.next()), hosts)) {
				if (!yes) it.remove();
			} else {
				if (yes) it.remove();
			}
		}
		if (log) logger.info("Filter {} with {} {}having host {}: after {}", what, field, yes ? "" : "not ", hosts, collection.size());
	}

	private static <T, V> void filterList(Collection<T> collection, Function<T, Collection<V>> getList, Predicate<V> filter, String what, String field, String condition, boolean yes, boolean all, boolean log) {
		if (collection == null || collection.isEmpty()) return;
		if (log) logger.info("Filter {} with {} {}{}: before {}", what, field, yes ? "" : "not ", condition, collection.size());
		for (Iterator<T> it = collection.iterator(); it.hasNext(); ) {
			boolean matches = false;
			for (V li : getList.apply(it.next())) {
				if (all) {
					matches = filter.test(li);
					if (matches && !yes || !matches && yes) {
						it.remove();
						break;
					}
				} else {
					if (filter.test(li)) {
						matches = true;
						break;
					}
				}
			}
			if (!all) {
				if (matches && !yes || !matches && yes) {
					it.remove();
				}
			}
		}
		if (log) logger.info("Filter {} with {} {}{}: after {}", what, field, yes ? "" : "not ", condition, collection.size());
	}

	private static void filterPublicationPart(List<Publication> publications, List<PublicationPartName> names, Predicate<PublicationPart> filter, String condition, boolean yes, boolean log) {
		filterList(publications, p -> p.getParts(names), filter, "publications", "parts " + names, condition, yes, true, log);
	}

	private static <T, V> void filterListRegex(Collection<T> collection, Function<T, Collection<V>> getList, Function<V, String> mapper, String regex, String what, String field, boolean yes, boolean all, boolean log) {
		if (collection == null || collection.isEmpty()) return;
		if (log) logger.info("Filter {} with {} {}matching {}: before {}", what, field, yes ? "" : "not ", regex, collection.size());
		Pattern pattern = Pattern.compile(regex);
		for (Iterator<T> it = collection.iterator(); it.hasNext(); ) {
			boolean matches = false;
			for (V li : getList.apply(it.next())) {
				if (all) {
					matches = pattern.matcher(mapper.apply(li)).find();
					if (matches && !yes || !matches && yes) {
						it.remove();
						break;
					}
				} else {
					if (pattern.matcher(mapper.apply(li)).find()) {
						matches = true;
						break;
					}
				}
			}
			if (!all) {
				if (matches && !yes || !matches && yes) {
					it.remove();
				}
			}
		}
		if (log) logger.info("Filter {} with {} {}matching {}: after {}", what, field, yes ? "" : "not ", regex, collection.size());
	}

	private static void filterPublicationPartRegex(List<Publication> publications, List<PublicationPartName> names, Function<PublicationPart, String> mapper, String regex, String field, boolean yes, boolean log) {
		filterListRegex(publications, p -> p.getParts(names), mapper, regex, "publications", field + " of parts " + names, yes, true, log);
	}

	private static <T, V> void filterListHost(Collection<T> collection, Function<T, Collection<V>> getList, Function<V, String> mapper, List<String> hosts, String what, String field, boolean yes, boolean all, boolean log) {
		if (collection == null || collection.isEmpty()) return;
		normaliseHosts(hosts);
		if (log) logger.info("Filter {} with {} {}having host {}: before {}", what, field, yes ? "" : "not ", hosts, collection.size());
		for (Iterator<T> it = collection.iterator(); it.hasNext(); ) {
			boolean matches = false;
			for (V li : getList.apply(it.next())) {
				if (all) {
					matches = hostMatches(mapper.apply(li), hosts);
					if (matches && !yes || !matches && yes) {
						it.remove();
						break;
					}
				} else {
					if (hostMatches(mapper.apply(li), hosts)) {
						matches = true;
						break;
					}
				}
			}
			if (!all) {
				if (matches && !yes || !matches && yes) {
					it.remove();
				}
			}
		}
		if (log) logger.info("Filter {} with {} {}having host {}: after {}", what, field, yes ? "" : "not ", hosts, collection.size());
	}

	private static void filterPublicationPartHost(List<Publication> publications, List<PublicationPartName> names, Function<PublicationPart, String> mapper, List<String> hosts, String field, boolean yes, boolean log) {
		filterListHost(publications, p -> p.getParts(names), mapper, hosts, "publications", field + " of parts " + names, yes, true, log);
	}

	@SuppressWarnings("unchecked")
	private static void filterPublicationPartContent(List<Publication> publications, List<PublicationPartName> names, String regex, boolean yes, boolean log) {
		if (publications == null || publications.isEmpty()) return;
		if (log) logger.info("Filter publications with content of parts {} {}matching {}: before {}", names, yes ? "" : "not ", regex, publications.size());
		Pattern pattern = Pattern.compile(regex);
		for (Iterator<Publication> it = publications.iterator(); it.hasNext(); ) {
			for (PublicationPart part : it.next().getParts(names)) {
				boolean matches = false;
				if (part instanceof PublicationPartString) {
					matches = pattern.matcher(((PublicationPartString) part).getContent()).find();
				} else {
					List<?> list = ((PublicationPartList<?>) part).getList();
					if (!list.isEmpty()) {
						if (list.get(0) instanceof String) {
							for (String s : (List<String>) list) {
								if (pattern.matcher(s).find()) {
									matches = true;
									break;
								}
							}
						} else if (list.get(0) instanceof MeshTerm) {
							for (MeshTerm t : (List<MeshTerm>) list) {
								if (pattern.matcher(t.getTerm()).find()) {
									matches = true;
									break;
								}
							}
						} else {
							for (MinedTerm t : (List<MinedTerm>) list) {
								if (pattern.matcher(t.getTerm()).find()) {
									matches = true;
									break;
								}
							}
						}
					}
				}
				if (matches && !yes || !matches && yes) {
					it.remove();
					break;
				}
			}
		}
		if (log) logger.info("Filter publications with content of parts {} {}matching {}: after {}", names, yes ? "" : "not ", regex, publications.size());
	}

	private static <T extends Comparable<T>> LinkedHashSet<T> ascIds(Set<T> ids, String what) {
		if (ids.isEmpty()) return new LinkedHashSet<>();
		logger.info("Sort {} {} in ascending order", ids.size(), what);
		return ids.stream().sorted().collect(Collectors.toCollection(LinkedHashSet::new));
	}
	private static <T extends Comparable<T>> LinkedHashSet<T> descIds(Set<T> ids, String what) {
		if (ids.isEmpty()) return new LinkedHashSet<>();
		logger.info("Sort {} {} in descending order", ids.size(), what);
		return ids.stream().sorted(Collections.reverseOrder()).collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private static void removeIds(Set<? extends Object> ids, String database, DatabaseEntryType type) throws IOException {
		if (ids.isEmpty()) return;
		logger.info("Remove {} {}s from database {} (based on {})", ids.size(), type, database, getIdString(type));
		int fail = 0;
		try (Database db = new Database(database)) {
			for (Object id : ids) {
				boolean removed = false;
				switch (type) {
					case publication: removed = db.removePublication((PublicationIds) id); break;
					case webpage: removed = db.removeWebpage((String) id); break;
					case doc: removed = db.removeDoc((String) id); break;
				}
				if (!removed) {
					logger.warn("Failed to remove {} with {}: {}", type, getIdString(type), id);
					++fail;
				} else db.commit();
			}
		}
		if (fail > 0) logger.warn("Failed to remove {} {}s (based on {})", fail, type, getIdString(type));
		logger.info("Removed {} {}s (based on {})", ids.size() - fail, type, getIdString(type));
	}

	private static void printIdsPub(PrintStream ps, Set<PublicationIds> pubIds, boolean plain, Format format, JsonGenerator generator) throws IOException {
		if (format == Format.html) {
			if (plain) ps.println("<table border=\"1\">");
			else ps.println("<ul>");
		} else if (format == Format.json) {
			generator.writeFieldName("publicationIds");
			generator.writeStartArray();
		}
		for (PublicationIds pubId : pubIds) {
			switch (format) {
			case text:
				if (plain) ps.println(pubId.toString(true));
				else ps.println(pubId.toStringWithUrl());
				break;
			case html:
				if (plain) ps.println(pubId.toStringHtml(true));
				else ps.println("<li>" + pubId.toStringWithUrlHtml() + "</li>");
				break;
			case json:
				if (plain) pubId.toStringJson(generator);
				else pubId.toStringWithUrlJson(generator);
				break;
			}
		}
		if (format == Format.html) {
			if (plain) ps.println("</table>");
			else ps.println("</ul>");
		} else if (format == Format.json) {
			generator.writeEndArray();
		}
	}

	private static void printIdsWeb(PrintStream ps, Set<String> webUrls, Format format, JsonGenerator generator, DatabaseEntryType type) throws IOException {
		if (format == Format.html) {
			ps.println("<ul>");
		} else if (format == Format.json) {
			generator.writeFieldName(type + "Urls");
			generator.writeStartArray();
		}
		for (String webUrl : webUrls) {
			switch (format) {
				case text: ps.println(webUrl); break;
				case html: ps.println("<li>" + PubFetcher.getLinkHtml(webUrl) + "</li>"); break;
				case json: generator.writeString(webUrl); break;
			}
		}
		if (format == Format.html) {
			ps.println("</ul>");
		} else if (format == Format.json) {
			generator.writeEndArray();
		}
	}

	@SuppressWarnings("unchecked")
	private static void outIds(Set<? extends Object> ids, boolean plain, Format format, JsonGenerator generator, DatabaseEntryType type) throws IOException {
		if (ids.isEmpty()) return;
		logger.info("Output {} {} {}s in {}", ids.size(), type, getIdString(type), format.getName());
		switch (type) {
			case publication: printIdsPub(System.out, (Set<PublicationIds>) ids, plain, format, generator); break;
			case webpage: case doc: printIdsWeb(System.out, (Set<String>) ids, format, generator, type); break;
		}
	}

	@SuppressWarnings("unchecked")
	private static void txtIds(Set<? extends Object> ids, boolean plain, Format format, Version version, String[] argv, String txt, DatabaseEntryType type) throws IOException {
		logger.info("Output {} {} {}s to file {} in {}", ids.size(), type, getIdString(type), txt, format.getName());
		if (format == Format.json) {
			try (JsonGenerator generator = PubFetcher.getJsonGenerator(txt, null)) {
				PubFetcher.jsonBegin(generator, version, argv);
				switch (type) {
					case publication: printIdsPub(null, (Set<PublicationIds>) ids, plain, format, generator); break;
					case webpage: case doc: printIdsWeb(null, (Set<String>) ids, format, generator, type); break;
				}
				PubFetcher.jsonEnd(generator);
			}
		} else {
			try (PrintStream ps = new PrintStream(new BufferedOutputStream(Files.newOutputStream(PubFetcher.outputPath(txt))), true, "UTF-8")) {
				switch (type) {
					case publication: printIdsPub(ps, (Set<PublicationIds>) ids, plain, format, null); break;
					case webpage: case doc: printIdsWeb(ps, (Set<String>) ids, format, null, type); break;
				}
			}
		}
	}

	static boolean preFilter(PubFetcherArgs args, Fetcher fetcher, FetcherArgs fetcherArgs, DatabaseEntry<?> entry, DatabaseEntryType type) {
		if (args != null && args.preFilter) {
			boolean filter = false;
			switch (type) {
				case publication: filter = contentFilter(args, fetcher, fetcherArgs, Stream.of((Publication) entry).collect(Collectors.toList()), null, null, args.preFilter); break;
				case webpage: filter = contentFilter(args, fetcher, fetcherArgs, null, Stream.of((Webpage) entry).collect(Collectors.toList()), null, args.preFilter); break;
				case doc: filter = contentFilter(args, fetcher, fetcherArgs, null, null, Stream.of((Webpage) entry).collect(Collectors.toList()), args.preFilter); break;
			}
			return filter;
		} else {
			return true;
		}
	}

	private static void logGot(String got, int idsSize, int entriesSize, int nullCount, DatabaseEntryType type) {
		if (entriesSize != idsSize - nullCount) {
			if (nullCount > 0) logger.warn("{} {} {}s, {} filtered out, {} not found", got, entriesSize, type, idsSize - entriesSize - nullCount, nullCount);
			else logger.info("{} {} {}s, {} filtered out", got, entriesSize, type, idsSize - entriesSize);
		} else {
			if (nullCount > 0) logger.warn("{} {} {}s, {} not found", got, entriesSize, type, nullCount);
			else logger.info("{} {} {}s", got, entriesSize, type);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T extends DatabaseEntry<T>> List<T> db(PubFetcherArgs args, Fetcher fetcher, FetcherArgs fetcherArgs, Set<? extends Object> ids, String database, int limit, DatabaseEntryType type) throws IOException {
		if (ids.isEmpty() || limit <= 0) {
			return Collections.emptyList();
		}
		List<T> entries = new ArrayList<>();
		int nullCount = 0;
		logger.info("Get {} {}s from database: {}", ids.size(), type, database);
		try (Database db = new Database(database)) {
			for (Object id : ids) {
				T entry = null;
				switch (type) {
					case publication: entry = (T) db.getPublication((PublicationIds) id); break;
					case webpage: entry = (T) db.getWebpage((String) id, true); break;
					case doc: entry = (T) db.getDoc((String) id, true); break;
				}
				if (entry != null) {
					if (preFilter(args, fetcher, fetcherArgs, entry, type)) {
						entries.add(entry);
						if (entries.size() >= limit) break;
					}
				} else {
					++nullCount;
				}
			}
		}
		logGot("Got", ids.size(), entries.size(), nullCount, type);
		return entries;
	}

	@SuppressWarnings("unchecked")
	private static <T extends DatabaseEntry<T>> T fetchDatabaseEntry(Object id, Database db, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs, boolean put, DatabaseEntryType type) {
		T entry = null;
		switch (type) {
			case publication:
				entry = (T) fetcher.initPublication((PublicationIds) id, fetcherArgs);
				if (entry != null && fetcher.getPublication((Publication) entry, parts, fetcherArgs) && put) {
					db.putPublication((Publication) entry);
					db.commit();
				}
				break;
			case webpage:
				entry = (T) fetcher.initWebpage((String) id);
				if (entry != null && fetcher.getWebpage((Webpage) entry, fetcherArgs) && put) {
					db.putWebpage((Webpage) entry);
					db.commit();
				}
				break;
			case doc:
				entry = (T) fetcher.initWebpage((String) id);
				if (entry != null && fetcher.getWebpage((Webpage) entry, fetcherArgs) && put) {
					db.putDoc((Webpage) entry);
					db.commit();
				}
				break;
		}
		return entry;
	}

	private static <T extends DatabaseEntry<T>> List<T> fetch(PubFetcherArgs args, Set<? extends Object> ids, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs, int limit, boolean stderr, DatabaseEntryType type) throws IOException {
		if (ids.isEmpty() || limit <= 0) {
			return Collections.emptyList();
		}
		List<T> entries = new ArrayList<>(ids.size());
		for (int i = 0; i < ids.size(); ++i) {
			entries.add(null);
		}
		int entriesCount = 0;
		List<Integer> exceptionIndexes = new ArrayList<>();
		List<Object> exceptionIds = new ArrayList<>();
		int nullCount = 0;
		logger.info("Fetch {} {}s", ids.size(), type);
		int i = 0;
		long start = System.currentTimeMillis();
		for (Object id : ids) {
			logger.info("Fetch {} {}", type, PubFetcher.progress(i + 1, ids.size(), start));
			if (stderr) {
				System.err.print("Fetch " + type + " " + PubFetcher.progress(i + 1, ids.size(), start) + "  \r");
			}
			T entry = fetchDatabaseEntry(id, null, fetcher, parts, fetcherArgs, false, type);
			if (entry != null) {
				if (entry.isFetchException()) {
					exceptionIndexes.add(i);
					exceptionIds.add(id);
				} else {
					if (preFilter(args, fetcher, fetcherArgs, entry, type)) {
						entries.set(i, entry);
						++entriesCount;
						if (entriesCount >= limit) break;
					}
				}
			} else {
				++nullCount;
			}
			++i;
		}
		if (exceptionIndexes.size() > 0 && entriesCount < limit) {
			logger.info("Refetch {} {}s with exception", exceptionIndexes.size(), type);
			start = System.currentTimeMillis();
			for (int j = 0; j < exceptionIndexes.size(); ++j) {
				i = exceptionIndexes.get(j);
				logger.info("Refetch {} {}", type, PubFetcher.progress(i + 1, ids.size(), start));
				if (stderr) {
					System.err.print("Refetch " + type + " " + PubFetcher.progress(i + 1, ids.size(), start) + "  \r");
				}
				T entry = fetchDatabaseEntry(exceptionIds.get(j), null, fetcher, parts, fetcherArgs, false, type);
				if (entry != null) {
					if (preFilter(args, fetcher, fetcherArgs, entry, type)) {
						entries.set(i, entry);
						++entriesCount;
						if (entriesCount >= limit) break;
					}
				} else {
					++nullCount;
				}
			}
		}
		entries.removeIf(Objects::isNull);
		logGot("Fetched", ids.size(), entries.size(), nullCount, type);
		return entries;
	}

	private static <T extends DatabaseEntry<T>> List<T> fetchPut(PubFetcherArgs args, Set<? extends Object> ids, String database, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs, int limit, boolean stderr, DatabaseEntryType type) throws IOException {
		if (ids.isEmpty() || limit <= 0) {
			return Collections.emptyList();
		}
		List<T> entries = new ArrayList<>(ids.size());
		for (int i = 0; i < ids.size(); ++i) {
			entries.add(null);
		}
		int entriesCount = 0;
		List<Integer> exceptionIndexes = new ArrayList<>();
		List<Object> exceptionIds = new ArrayList<>();
		int nullCount = 0;
		logger.info("Fetch {} {}s and put to database: {}", ids.size(), type, database);
		try (Database db = new Database(database)) {
			int i = 0;
			long start = System.currentTimeMillis();
			for (Object id : ids) {
				logger.info("Fetch {} {}", type, PubFetcher.progress(i + 1, ids.size(), start));
				if (stderr) {
					System.err.print("Fetch " + type + " " + PubFetcher.progress(i + 1, ids.size(), start) + "  \r");
				}
				T entry = fetchDatabaseEntry(id, db, fetcher, parts, fetcherArgs, true, type);
				if (entry != null) {
					if (entry.isFetchException()) {
						exceptionIndexes.add(i);
						exceptionIds.add(id);
					} else {
						if (preFilter(args, fetcher, fetcherArgs, entry, type)) {
							entries.set(i, entry);
							++entriesCount;
							if (entriesCount >= limit) break;
						}
					}
				} else {
					++nullCount;
				}
				++i;
			}
			if (exceptionIndexes.size() > 0 && entriesCount < limit) {
				logger.info("Refetch {} {}s with exception", exceptionIndexes.size(), type);
				start = System.currentTimeMillis();
				for (int j = 0; j < exceptionIndexes.size(); ++j) {
					i = exceptionIndexes.get(j);
					logger.info("Refetch {} {}", type, PubFetcher.progress(i + 1, ids.size(), start));
					if (stderr) {
						System.err.print("Refetch " + type + " " + PubFetcher.progress(i + 1, ids.size(), start) + "  \r");
					}
					T entry = fetchDatabaseEntry(exceptionIds.get(j), db, fetcher, parts, fetcherArgs, true, type);
					if (entry != null) {
						if (preFilter(args, fetcher, fetcherArgs, entry, type)) {
							entries.set(i, entry);
							++entriesCount;
							if (entriesCount >= limit) break;
						}
					} else {
						++nullCount;
					}
				}
			}
		}
		entries.removeIf(Objects::isNull);
		logGot("Fetched", ids.size(), entries.size(), nullCount, type);
		return entries;
	}

	@SuppressWarnings("unchecked")
	public static <T extends DatabaseEntry<T>> List<T> dbFetch(PubFetcherArgs args, int threads, Set<? extends Object> ids, String database, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs, boolean end, int limit, boolean stderr, DatabaseEntryType type) throws IOException {
		if (ids.isEmpty() || limit <= 0) {
			return Collections.emptyList();
		}
		List<T> entries = (List<T>) DbFetch.init(type, ids, System.currentTimeMillis());
		logger.info("Get {} {}s from database: {} (or fetch if not present)", ids.size(), type, database);
		try (Database db = new Database(database)) {
			for (int i = 0; i < threads; ++i) {
				Thread t = new Thread(new DbFetch(args, db, fetcher, parts, fetcherArgs, end, limit, stderr));
				t.setDaemon(true);
				t.start();
			}

			synchronized(DbFetch.lock) {
				while (!DbFetch.lockDone || DbFetch.numThreads > 0) {
					try {
						DbFetch.lock.wait();
					} catch (InterruptedException e) {
						logger.error("Exception!", e);
						break;
					}
				}
			}
		}
		entries.removeIf(Objects::isNull);
		logGot("Got", ids.size(), entries.size(), DbFetch.nullCount.get(), type);
		return entries;
	}

	private static <T extends DatabaseEntry<T>> void asc(List<T> entries, String what) {
		if (entries.isEmpty()) return;
		logger.info("Sort {} {} in ascending order", entries.size(), what);
		Collections.sort(entries);
	}
	private static <T extends DatabaseEntry<T>> void desc(List<T> entries, String what) {
		if (entries.isEmpty()) return;
		logger.info("Sort {} {} in descending order", entries.size(), what);
		Collections.sort(entries, Collections.reverseOrder());
	}

	private static <T extends DatabaseEntry<T>> void ascTime(List<T> entries, String what) {
		if (entries.isEmpty()) return;
		logger.info("Sort {} {} in ascending order by fetch time", entries.size(), what);
		Collections.sort(entries, (a, b) -> a.getFetchTime() < b.getFetchTime() ? -1 : a.getFetchTime() > b.getFetchTime() ? 1 : 0);
	}
	private static <T extends DatabaseEntry<T>> void descTime(List<T> entries, String what) {
		if (entries.isEmpty()) return;
		logger.info("Sort {} {} in descending order by fetch time", entries.size(), what);
		Collections.sort(entries, (a, b) -> a.getFetchTime() < b.getFetchTime() ? 1 : a.getFetchTime() > b.getFetchTime() ? -1 : 0);
	}

	private static <T extends DatabaseEntry<T>> Map<String, Integer> topHosts(List<T> entries, Scrape scrape, Boolean hasScrape, DatabaseEntryType type) {
		if (entries.isEmpty()) {
			return Collections.emptyMap();
		}
		if (hasScrape == null) {
			logger.info("Get top hosts from {} {}s", entries.size(), type);
		} else if (hasScrape.booleanValue()) {
			logger.info("Get top hosts with scrape rules from {} {}s", entries.size(), type);
		} else {
			logger.info("Get top hosts without scrape rules from {} {}s", entries.size(), type);
		}
		Map<String, Integer> hosts = new LinkedHashMap<>();
		for (T entry : entries) {
			List<URL> urls = new ArrayList<>();
			switch (type) {
			case publication:
				for (Link link : ((Publication) entry).getVisitedSites()) {
					urls.add(link.getUrl());
				}
				break;
			case webpage: case doc:
				String finalUrl = ((Webpage) entry).getFinalUrl();
				if (!finalUrl.isEmpty()) {
					try {
						urls.add(new URL(finalUrl));
					} catch (MalformedURLException e) {
						logger.error("Malformed URL: {}", finalUrl);
					}
				}
				break;
			}
			for (URL url : urls) {
				if (hasScrape != null) {
					if (hasScrape.booleanValue()) {
						switch (type) {
							case publication: if (scrape.getSite(url.toString()) == null) continue; break;
							case webpage: case doc: if (scrape.getWebpage(url.toString()) == null) continue; break;
						}
					} else {
						switch (type) {
							case publication: if (scrape.getSite(url.toString()) != null) continue; break;
							case webpage: case doc: if (scrape.getWebpage(url.toString()) != null) continue; break;
						}
					}
				}
				String host = url.getHost().toLowerCase(Locale.ROOT);
				if (host.startsWith("www.")) {
					host = host.substring(4);
				}
				Integer count = hosts.get(host);
				if (count != null) {
					hosts.put(host, count + 1);
				} else {
					hosts.put(host, 1);
				}
			}
		}
		Map<String, Integer> topHosts = hosts.entrySet().stream()
			.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (k, v) -> { throw new AssertionError(); }, LinkedHashMap::new));
		logger.info("Got {} top hosts from {} {}s", topHosts.size(), entries.size(), type);
		return topHosts;
	}

	private static void head(Collection<?> entries, int count, String what) {
		if (entries.isEmpty() || entries.size() <= count) return;
		logger.info("Limit {} {} to {} first entries", entries.size(), what, count);
		int i = 0;
		for (Iterator<?> it = entries.iterator(); it.hasNext(); ++i) {
			it.next();
			if (i >= count) it.remove();
		}
	}
	private static void tail(Collection<?> entries, int count, String what) {
		if (entries.isEmpty() || entries.size() <= count) return;
		logger.info("Limit {} {} to {} last entries", entries.size(), what, count);
		int i = 0;
		int n = entries.size();
		for (Iterator<?> it = entries.iterator(); it.hasNext(); ++i) {
			it.next();
			if (n - i > count) it.remove();
		}
	}

	private static void updateCitationsCount(List<Publication> publications, String database, Fetcher fetcher, FetcherArgs fetcherArgs, boolean stderr) throws IOException {
		if (publications.isEmpty()) return;
		logger.info("Update citations count of {} publications and put successfully updated to database: {}", publications.size(), database);
		long start = System.currentTimeMillis();
		int fail = 0;
		try (Database db = new Database(database)) {
			for (int i = 0; i < publications.size(); ++i) {
				Publication publication = publications.get(i);
				logger.info("Update citations count {}", PubFetcher.progress(i + 1, publications.size(), start));
				if (stderr) {
					System.err.print("Update citations count " + PubFetcher.progress(i + 1, publications.size(), start) + "  \r");
				}
				if (fetcher.updateCitationsCount(publication, fetcherArgs)) {
					db.putPublication(publication);
					db.commit();
				} else {
					++fail;
				}
			}
		}
		if (fail > 0) logger.warn("Failed to update citations count of {} publications", fail);
		logger.info("Updated citations count of {} publications", publications.size() - fail);
	}

	private static <T extends DatabaseEntry<T>> void put(List<T> entries, String database, DatabaseEntryType type) throws IOException {
		if (entries.isEmpty()) return;
		logger.info("Put {} {}s to database: {}", entries.size(), type, database);
		int fail = 0;
		try (Database db = new Database(database)) {
			for (T entry : entries) {
				boolean success = false;
				switch (type) {
					case publication: success = db.putPublication((Publication) entry); break;
					case webpage: success = db.putWebpage((Webpage) entry); break;
					case doc: success = db.putDoc((Webpage) entry); break;
				}
				if (!success) ++fail;
				db.commit();
			}
		}
		if (fail > 0) logger.warn("Failed to put {} {}s", fail, type);
		logger.info("Put {} {}s", entries.size() - fail, type);
	}

	private static <T extends DatabaseEntry<T>> void remove(List<T> entries, String database, DatabaseEntryType type) throws IOException {
		if (entries.isEmpty()) return;
		logger.info("Remove {} {}s from database: {}", entries.size(), type, database);
		int fail = 0;
		try (Database db = new Database(database)) {
			for (T entry : entries) {
				boolean success = false;
				switch (type) {
					case publication: success = db.removePublication((Publication) entry); break;
					case webpage: success = db.removeWebpage((Webpage) entry); break;
					case doc: success = db.removeDoc((Webpage) entry); break;
				}
				if (!success) ++fail;
				db.commit();
			}
		}
		if (fail > 0) logger.warn("Failed to remove {} {}s", fail, type);
		logger.info("Removed {} {}s", entries.size() - fail, type);
	}

	private static void printPubParts(PrintStream ps, Publication publication, boolean plain, Format format, JsonGenerator generator, List<PublicationPartName> parts, FetcherArgs fetcherArgs, boolean idOnly) throws IOException {
		List<String> pubString = null;
		if (format != Format.json) {
			pubString = new ArrayList<>();
		} else {
			generator.writeStartObject();
		}
		if (plain) {
			switch (format) {
			case text:
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
				break;
			case html:
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
				break;
			case json:
				if (parts.contains(PublicationPartName.pmid)) publication.getPmid().toStringPlainJson(generator, true);
				if (parts.contains(PublicationPartName.pmcid)) publication.getPmcid().toStringPlainJson(generator, true);
				if (parts.contains(PublicationPartName.doi)) publication.getDoi().toStringPlainJson(generator, true);
				if (parts.contains(PublicationPartName.title)) publication.getTitle().toStringPlainJson(generator, true);
				if (parts.contains(PublicationPartName.keywords)) publication.getKeywords().toStringPlainJson(generator, true);
				if (parts.contains(PublicationPartName.mesh)) publication.getMeshTerms().toStringPlainJson(generator, true);
				if (parts.contains(PublicationPartName.efo)) publication.getEfoTerms().toStringPlainJson(generator, true);
				if (parts.contains(PublicationPartName.go)) publication.getGoTerms().toStringPlainJson(generator, true);
				if (parts.contains(PublicationPartName.theAbstract)) publication.getAbstract().toStringPlainJson(generator, "abstract");
				if (parts.contains(PublicationPartName.fulltext)) publication.getFulltext().toStringPlainJson(generator, true);
				break;
			}
		} else {
			switch (format) {
			case text:
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
				break;
			case html:
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
				break;
			case json:
				if (parts.contains(PublicationPartName.pmid)) publication.getPmid().toStringJson(generator, fetcherArgs);
				if (parts.contains(PublicationPartName.pmcid)) publication.getPmcid().toStringJson(generator, fetcherArgs);
				if (parts.contains(PublicationPartName.doi)) publication.getDoi().toStringJson(generator, fetcherArgs);
				if (parts.contains(PublicationPartName.title)) publication.getTitle().toStringJson(generator, fetcherArgs);
				if (parts.contains(PublicationPartName.keywords)) publication.getKeywords().toStringJson(generator, fetcherArgs);
				if (parts.contains(PublicationPartName.mesh)) publication.getMeshTerms().toStringJson(generator, fetcherArgs);
				if (parts.contains(PublicationPartName.efo)) publication.getEfoTerms().toStringJson(generator, fetcherArgs);
				if (parts.contains(PublicationPartName.go)) publication.getGoTerms().toStringJson(generator, fetcherArgs);
				if (parts.contains(PublicationPartName.theAbstract)) publication.getAbstract().toStringJson(generator, fetcherArgs, true, "abstract");
				if (parts.contains(PublicationPartName.fulltext)) publication.getFulltext().toStringJson(generator, fetcherArgs);
				break;
			}
		}
		if (format != Format.json) {
			ps.println(pubString.stream().collect(Collectors.joining("\n\n")));
		} else {
			generator.writeEndObject();
		}
	}

	private static <T extends DatabaseEntry<T>> void print(PrintStream ps, List<T> entries, boolean plain, Format format, JsonGenerator generator, List<PublicationPartName> parts, FetcherArgs fetcherArgs, DatabaseEntryType type) throws IOException {
		boolean idOnly = false;
		if (parts != null
				&& (parts.contains(PublicationPartName.pmid) || parts.contains(PublicationPartName.pmcid) || parts.contains(PublicationPartName.doi))
				&& !parts.contains(PublicationPartName.title)
				&& !parts.contains(PublicationPartName.keywords) && !parts.contains(PublicationPartName.mesh)
				&& !parts.contains(PublicationPartName.efo) && !parts.contains(PublicationPartName.go)
				&& !parts.contains(PublicationPartName.theAbstract) && !parts.contains(PublicationPartName.fulltext)) {
			idOnly = true;
		}
		if (format == Format.html) {
			if (idOnly && plain) ps.println("<table border=\"1\">");
		} else if (format == Format.json) {
			generator.writeFieldName(type + "s");
			generator.writeStartArray();
		}
		int i = 0;
		int n = entries.size();
		for (T entry : entries) {
			if (parts != null && entry instanceof Publication) {
				printPubParts(ps, (Publication) entry, plain, format, generator, parts, fetcherArgs, idOnly);
			} else if (plain) {
				switch (format) {
					case text: ps.println(entry.toStringPlain()); break;
					case html: ps.println(entry.toStringPlainHtml("")); break;
					case json: entry.toStringPlainJson(generator); break;
				}
			} else {
				if (entry instanceof Publication) {
					Publication publication = (Publication) entry;
					switch (format) {
						case text: ps.println(publication.toString()); break;
						case html: ps.println(publication.toStringHtml("")); break;
						case json: publication.toStringJson(generator, fetcherArgs, true); break;
					}
				} else {
					Webpage webpage = (Webpage) entry;
					switch (format) {
						case text: ps.println(webpage.toString()); break;
						case html: ps.println(webpage.toStringHtml("")); break;
						case json: webpage.toStringJson(generator, fetcherArgs, true); break;
					}
				}
			}
			if (format != Format.json) {
				++i;
				if (i < n) {
					if (!(plain && parts != null && entry instanceof Publication
							&& (parts.size() == 1 && parts.get(0) != PublicationPartName.theAbstract && parts.get(0) != PublicationPartName.fulltext
							|| (parts.size() == 2 && (parts.get(0) == PublicationPartName.pmid || parts.get(0) == PublicationPartName.pmcid || parts.get(0) == PublicationPartName.doi)
								&& (parts.get(1) == PublicationPartName.pmid || parts.get(1) == PublicationPartName.pmcid || parts.get(1) == PublicationPartName.doi))
							|| (parts.size() == 3 && (parts.get(0) == PublicationPartName.pmid || parts.get(0) == PublicationPartName.pmcid || parts.get(0) == PublicationPartName.doi)
								&& (parts.get(1) == PublicationPartName.pmid || parts.get(1) == PublicationPartName.pmcid || parts.get(1) == PublicationPartName.doi)
								&& (parts.get(2) == PublicationPartName.pmid || parts.get(2) == PublicationPartName.pmcid || parts.get(2) == PublicationPartName.doi))))) {
						if (format == Format.html) ps.println("\n<hr>\n");
						else ps.println("\n -----------------------------------------------------------------------------\n");
					}
				}
			}
		}
		if (format == Format.html) {
			if (idOnly && plain) ps.println("</table>");
		} else if (format == Format.json) {
			generator.writeEndArray();
		}
	}

	private static <T extends DatabaseEntry<T>> void out(List<T> entries, boolean plain, Format format, JsonGenerator generator, List<PublicationPartName> parts, FetcherArgs fetcherArgs, DatabaseEntryType type) throws IOException {
		if (entries.isEmpty()) return;
		logger.info("Output {} {}s{}{} in {}",
			entries.size(), type, plain ? " without metadata" : "", parts != null && type == DatabaseEntryType.publication ? " with parts " + parts : "", format.getName());
		print(System.out, entries, plain, format, generator, parts, fetcherArgs, type);
	}
	private static <T extends DatabaseEntry<T>> void txt(List<T> entries, boolean plain, Format format, Version version, String[] argv, List<PublicationPartName> parts, FetcherArgs fetcherArgs, String txt, DatabaseEntryType type) throws IOException {
		logger.info("Output {} {}s to file {}{}{} in {}",
				entries.size(), type, txt, plain ? " without metadata" : "", parts != null && type == DatabaseEntryType.publication ? " with parts " + parts : "", format.getName());
		if (format == Format.json) {
			try (JsonGenerator generator = PubFetcher.getJsonGenerator(txt, null)) {
				PubFetcher.jsonBegin(generator, version, argv);
				print(null, entries, plain, format, generator, parts, fetcherArgs, type);
				PubFetcher.jsonEnd(generator);
			}
		} else {
			try (PrintStream ps = new PrintStream(new BufferedOutputStream(Files.newOutputStream(PubFetcher.outputPath(txt))), true, "UTF-8")) {
				print(ps, entries, plain, format, null, parts, fetcherArgs, type);
			}
		}
	}

	private static void printTopHosts(PrintStream ps, Map<String, Integer> topHosts, Format format, JsonGenerator generator, DatabaseEntryType type) throws IOException {
		if (format == Format.html) {
			ps.println("<ul>");
		} else if (format == Format.json) {
			generator.writeFieldName(type + "TopHosts");
			generator.writeStartArray();
		}
		for (Map.Entry<String, Integer> topHost : topHosts.entrySet()) {
			switch (format) {
				case text: ps.println(topHost.getKey() + "\t" + topHost.getValue()); break;
				case html: ps.println("<li value=\"" + topHost.getValue() + "\">" + PubFetcher.escapeHtml(topHost.getKey()) + "</li>"); break;
				case json:
					generator.writeStartObject();
					generator.writeNumberField("count", topHost.getValue());
					generator.writeStringField("host", topHost.getKey());
					generator.writeEndObject();
					break;
			}
		}
		if (format == Format.html) {
			ps.println("</ul>");
		} else if (format == Format.json) {
			generator.writeEndArray();
		}
	}
	private static void outTopHosts(Map<String, Integer> topHosts, Format format, JsonGenerator generator, DatabaseEntryType type) throws IOException {
		if (topHosts.isEmpty()) return;
		logger.info("Output {} top hosts from {}s in {}", topHosts.size(), type, format.getName());
		printTopHosts(System.out, topHosts, format, generator, type);
	}
	private static void txtTopHosts(Map<String, Integer> topHosts, Format format, Version version, String[] argv, String txt, DatabaseEntryType type) throws IOException {
		logger.info("Output {} top hosts from {}s to file {} in {}", topHosts.size(), type, txt, format.getName());
		if (format == Format.json) {
			try (JsonGenerator generator = PubFetcher.getJsonGenerator(txt, null)) {
				PubFetcher.jsonBegin(generator, version, argv);
				printTopHosts(null, topHosts, format, generator, type);
				PubFetcher.jsonEnd(generator);
			}
		} else {
			try (PrintStream ps = new PrintStream(new BufferedOutputStream(Files.newOutputStream(PubFetcher.outputPath(txt))), true, "UTF-8")) {
				printTopHosts(ps, topHosts, format, null, type);
			}
		}
	}

	private static void count(String label, Collection<?> entries) {
		System.out.println(label + " : " + entries.size());
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

	static boolean contentFilter(PubFetcherArgs args, Fetcher fetcher, FetcherArgs fetcherArgs, List<Publication> publications, List<Webpage> webpages, List<Webpage> docs, boolean preFilter) {
		if (args.fetchTimeMore != null) {
			filter(publications, e -> e.getFetchTime() >= args.fetchTimeMore, "publications", "fetch time more than or equal to " + timeHuman(args.fetchTimeMore), true, !preFilter);
			filter(webpages, e -> e.getFetchTime() >= args.fetchTimeMore, "webpages", "fetch time more than or equal to " + timeHuman(args.fetchTimeMore), true, !preFilter);
			filter(docs, e -> e.getFetchTime() >= args.fetchTimeMore, "docs", "fetch time more than or equal to " + timeHuman(args.fetchTimeMore), true, !preFilter);
		}
		if (args.fetchTimeLess != null) {
			filter(publications, e -> e.getFetchTime() <= args.fetchTimeLess, "publications", "fetch time less than or equal to " + timeHuman(args.fetchTimeLess), true, !preFilter);
			filter(webpages, e -> e.getFetchTime() <= args.fetchTimeLess, "webpages", "fetch time less than or equal to " + timeHuman(args.fetchTimeLess), true, !preFilter);
			filter(docs, e -> e.getFetchTime() <= args.fetchTimeLess, "docs", "fetch time less than or equal to " + timeHuman(args.fetchTimeLess), true, !preFilter);
		}

		if (args.retryCounter != null) {
			filter(publications, e -> args.retryCounter.contains(e.getRetryCounter()), "publications", "retry count " + args.retryCounter, true, !preFilter);
			filter(webpages, e -> args.retryCounter.contains(e.getRetryCounter()), "webpages", "retry count " + args.retryCounter, true, !preFilter);
			filter(docs, e -> args.retryCounter.contains(e.getRetryCounter()), "docs", "retry count " + args.retryCounter, true, !preFilter);
		}
		if (args.notRetryCounter != null) {
			filter(publications, e -> args.notRetryCounter.contains(e.getRetryCounter()), "publications", "retry count " + args.notRetryCounter, false, !preFilter);
			filter(webpages, e -> args.notRetryCounter.contains(e.getRetryCounter()), "webpages", "retry count " + args.notRetryCounter, false, !preFilter);
			filter(docs, e -> args.notRetryCounter.contains(e.getRetryCounter()), "docs", "retry count " + args.notRetryCounter, false, !preFilter);
		}
		if (args.retryCounterMore != null) {
			filter(publications, e -> e.getRetryCounter() > args.retryCounterMore, "publications", "retry count more than " + args.retryCounterMore, true, !preFilter);
			filter(webpages, e -> e.getRetryCounter() > args.retryCounterMore, "webpages", "retry count more than " + args.retryCounterMore, true, !preFilter);
			filter(docs, e -> e.getRetryCounter() > args.retryCounterMore, "docs", "retry count more than " + args.retryCounterMore, true, !preFilter);
		}
		if (args.retryCounterLess != null) {
			filter(publications, e -> e.getRetryCounter() < args.retryCounterLess, "publications", "retry count less than " + args.retryCounterLess, true, !preFilter);
			filter(webpages, e -> e.getRetryCounter() < args.retryCounterLess, "webpages", "retry count less than " + args.retryCounterLess, true, !preFilter);
			filter(docs, e -> e.getRetryCounter() < args.retryCounterLess, "docs", "retry count less than " + args.retryCounterLess, true, !preFilter);
		}

		if (args.fetchException) {
			filter(publications, e -> e.isFetchException(), "publications", "fetching exception", true, !preFilter);
			filter(webpages, e -> e.isFetchException(), "webpages", "fetching exception", true, !preFilter);
			filter(docs, e -> e.isFetchException(), "docs", "fetching exception", true, !preFilter);
		}
		if (args.notFetchException) {
			filter(publications, e -> e.isFetchException(), "publications", "fetching exception", false, !preFilter);
			filter(webpages, e -> e.isFetchException(), "webpages", "fetching exception", false, !preFilter);
			filter(docs, e -> e.isFetchException(), "docs", "fetching exception", false, !preFilter);
		}

		if (args.empty) {
			filter(publications, e -> e.isEmpty(), "publications", "empty content", true, !preFilter);
			filter(webpages, e -> e.isEmpty(), "webpages", "empty content", true, !preFilter);
			filter(docs, e -> e.isEmpty(), "docs", "empty content", true, !preFilter);
		}
		if (args.notEmpty) {
			filter(publications, e -> e.isEmpty(), "publications", "empty content", false, !preFilter);
			filter(webpages, e -> e.isEmpty(), "webpages", "empty content", false, !preFilter);
			filter(docs, e -> e.isEmpty(), "docs", "empty content", false, !preFilter);
		}
		if (args.usable) {
			filter(publications, e -> e.isUsable(fetcherArgs), "publications", "usable content", true, !preFilter);
			filter(webpages, e -> e.isUsable(fetcherArgs), "webpages", "usable content", true, !preFilter);
			filter(docs, e -> e.isUsable(fetcherArgs), "docs", "usable content", true, !preFilter);
		}
		if (args.notUsable) {
			filter(publications, e -> e.isUsable(fetcherArgs), "publications", "usable content", false, !preFilter);
			filter(webpages, e -> e.isUsable(fetcherArgs), "webpages", "usable content", false, !preFilter);
			filter(docs, e -> e.isUsable(fetcherArgs), "docs", "usable content", false, !preFilter);
		}
		if (args.isFinal) {
			filter(publications, e -> e.isFinal(fetcherArgs), "publications", "final content", true, !preFilter);
			filter(webpages, e -> e.isFinal(fetcherArgs), "webpages", "final content", true, !preFilter);
			filter(docs, e -> e.isFinal(fetcherArgs), "docs", "final content", true, !preFilter);
		}
		if (args.notIsFinal) {
			filter(publications, e -> e.isFinal(fetcherArgs), "publications", "final content", false, !preFilter);
			filter(webpages, e -> e.isFinal(fetcherArgs), "webpages", "final content", false, !preFilter);
			filter(docs, e -> e.isFinal(fetcherArgs), "docs", "final content", false, !preFilter);
		}

		if (args.totallyFinal) {
			filter(publications, p -> p.isTotallyFinal(fetcherArgs), "publications", "totally final content", true, !preFilter);
		}
		if (args.notTotallyFinal) {
			filter(publications, p -> p.isTotallyFinal(fetcherArgs), "publications", "totally final content", false, !preFilter);
		}
		if (args.broken) {
			filter(webpages, w -> w.isBroken(), "webpages", "broken status", true, !preFilter);
			filter(docs, w -> w.isBroken(), "docs", "broken status", true, !preFilter);
		}
		if (args.notBroken) {
			filter(webpages, w -> w.isBroken(), "webpages", "broken status", false, !preFilter);
			filter(docs, w -> w.isBroken(), "docs", "broken status", false, !preFilter);
		}

		if (args.partEmpty != null) filterPublicationPart(publications, args.partEmpty, pp -> pp.isEmpty(), "being empty", true, !preFilter);
		if (args.notPartEmpty != null) filterPublicationPart(publications, args.notPartEmpty, pp -> pp.isEmpty(), "being empty", false, !preFilter);
		if (args.partUsable != null) filterPublicationPart(publications, args.partUsable, pp -> pp.isUsable(fetcherArgs), "being usable", true, !preFilter);
		if (args.notPartUsable != null) filterPublicationPart(publications, args.notPartUsable, pp -> pp.isUsable(fetcherArgs), "being usable", false, !preFilter);
		if (args.partFinal != null) filterPublicationPart(publications, args.partFinal, pp -> pp.isFinal(fetcherArgs), "being final", true, !preFilter);
		if (args.notPartFinal != null) filterPublicationPart(publications, args.notPartFinal, pp -> pp.isFinal(fetcherArgs), "being final", false, !preFilter);

		if (args.partContent != null) filterPublicationPartContent(publications, args.partContentPart, args.partContent, true, !preFilter);
		if (args.notPartContent != null) filterPublicationPartContent(publications, args.notPartContentPart, args.notPartContent, false, !preFilter);
		if (args.partSize != null) filterPublicationPart(publications, args.partSizePart, pp -> args.partSize.contains(pp.getSize()), "having size " + args.partSize, true, !preFilter);
		if (args.notPartSize != null) filterPublicationPart(publications, args.notPartSizePart, pp -> args.notPartSize.contains(pp.getSize()), "having size " + args.notPartSize, false, !preFilter);
		if (args.partSizeMore != null) filterPublicationPart(publications, args.partSizeMorePart, pp -> pp.getSize() > args.partSizeMore, "having size more than " + args.partSizeMore, true, !preFilter);
		if (args.partSizeLess != null) filterPublicationPart(publications, args.partSizeLessPart, pp -> pp.getSize() < args.partSizeLess, "having size less than " + args.partSizeLess, true, !preFilter);
		if (args.partType != null) filterPublicationPart(publications, args.partTypePart, pp -> args.partType.contains(pp.getType()), "having type " + args.partType, true, !preFilter);
		if (args.notPartType != null) filterPublicationPart(publications, args.notPartTypePart, pp -> args.notPartType.contains(pp.getType()), "having type " + args.notPartType, false, !preFilter);
		if (args.partTypeMore != null) filterPublicationPart(publications, args.partTypeMorePart, pp -> pp.getType().isBetterThan(args.partTypeMore), "having type more than " + args.partTypeMore, true, !preFilter);
		if (args.partTypeLess != null) filterPublicationPart(publications, args.partTypeLessPart, pp -> !(pp.getType().isEquivalent(args.partTypeLess) || pp.getType().isBetterThan(args.partTypeLess)), "having type less than " + args.partTypeLess, true, !preFilter);
		if (args.partTypeFinal) filterPublicationPart(publications, args.partTypeFinalPart, pp -> pp.getType().isFinal(), "having final type", true, !preFilter);
		if (args.notPartTypeFinal) filterPublicationPart(publications, args.notPartTypeFinalPart, pp -> pp.getType().isFinal(), "having final type", false, !preFilter);
		if (args.partTypePdf) filterPublicationPart(publications, args.partTypePdfPart, pp -> pp.getType().isPdf(), "having pdf type", true, !preFilter);
		if (args.notPartTypePdf) filterPublicationPart(publications, args.notPartTypePdfPart, pp -> pp.getType().isPdf(), "having pdf type", false, !preFilter);
		if (args.partUrl != null) filterPublicationPartRegex(publications, args.partUrlPart, pp -> pp.getUrl(), args.partUrl, "URL", true, !preFilter);
		if (args.notPartUrl != null) filterPublicationPartRegex(publications, args.notPartUrlPart, pp -> pp.getUrl(), args.notPartUrl, "URL", false, !preFilter);
		if (args.partUrlHost != null) filterPublicationPartHost(publications, args.partUrlHostPart, pp -> pp.getUrl(), args.partUrlHost, "URL", true, !preFilter);
		if (args.notPartUrlHost != null) filterPublicationPartHost(publications, args.notPartUrlHostPart, pp -> pp.getUrl(), args.notPartUrlHost, "URL", false, !preFilter);
		if (args.partTimeMore != null) filterPublicationPart(publications, args.partTimeMorePart, pp -> pp.getTimestamp() >= args.partTimeMore, "having time more or equal to " + timeHuman(args.partTimeMore), true, !preFilter);
		if (args.partTimeLess != null) filterPublicationPart(publications, args.partTimeLessPart, pp -> pp.getTimestamp() <= args.partTimeLess, "having time less or equal to " + timeHuman(args.partTimeLess), true, !preFilter);

		if (args.oa) filter(publications, p -> p.isOA(), "publications", "Open Access", true, !preFilter);
		if (args.notOa) filter(publications, p -> p.isOA(), "publications", "Open Access", false, !preFilter);
		if (args.journalTitle != null) filterRegex(publications, p -> p.getJournalTitle(), args.journalTitle, "publications", "journal title", true, !preFilter);
		if (args.notJournalTitle != null) filterRegex(publications, p -> p.getJournalTitle(), args.notJournalTitle, "publications", "journal title", false, !preFilter);
		if (args.journalTitleEmpty) filter(publications, p -> p.getJournalTitle().isEmpty(), "publications", "empty journal title", true, !preFilter);
		if (args.notJournalTitleEmpty) filter(publications, p -> p.getJournalTitle().isEmpty(), "publications", "empty journal title", false, !preFilter);
		if (args.pubDateMore != null) filter(publications, p -> p.getPubDate() >= args.pubDateMore, "publications", "publication date more than or equal to " + timeHuman(args.pubDateMore), true, !preFilter);
		if (args.pubDateLess != null) filter(publications, p -> p.getPubDate() <= args.pubDateLess, "publications", "publication date less than or equal to " + timeHuman(args.pubDateLess), true, !preFilter);
		if (args.citationsCount != null) filter(publications, p -> args.citationsCount.contains(p.getCitationsCount()), "publications", "citations count " + args.citationsCount, true, !preFilter);
		if (args.notCitationsCount != null) filter(publications, p -> args.notCitationsCount.contains(p.getCitationsCount()), "publications", "citations count " + args.notCitationsCount, false, !preFilter);
		if (args.citationsCountMore != null) filter(publications, p -> p.getCitationsCount() > args.citationsCountMore, "publications", "citations count more than " + args.citationsCountMore, true, !preFilter);
		if (args.citationsCountLess != null) filter(publications, p -> p.getCitationsCount() < args.citationsCountLess, "publications", "citations count less than " + args.citationsCountLess, true, !preFilter);
		if (args.citationsTimestampMore != null) filter(publications, p -> p.getCitationsTimestamp() >= args.citationsTimestampMore, "publications", "citations timestamp more than or equal to " + timeHuman(args.citationsTimestampMore), true, !preFilter);
		if (args.citationsTimestampLess != null) filter(publications, p -> p.getCitationsTimestamp() <= args.citationsTimestampLess, "publications", "citations timestamp less than or equal to " + timeHuman(args.citationsTimestampLess), true, !preFilter);

		if (args.correspAuthorName != null) filterListRegex(publications, p -> p.getCorrespAuthor(), ca -> ca.getName(), args.correspAuthorName, "publications", "a corresponding author name", true, false, !preFilter);
		if (args.notCorrespAuthorName != null) filterListRegex(publications, p -> p.getCorrespAuthor(), ca -> ca.getName(), args.notCorrespAuthorName, "publications", "corresponding authors names", false, true, !preFilter);
		if (args.correspAuthorNameEmpty) filterList(publications, p -> p.getCorrespAuthor(), ca -> ca.getName().isEmpty(), "publications", "corresponding authors names", "empty", true, true, !preFilter);
		if (args.notCorrespAuthorNameEmpty) filterList(publications, p -> p.getCorrespAuthor(), ca -> !ca.getName().isEmpty(), "publications", "a corresponding author name", "not empty", true, false, !preFilter);
		if (args.correspAuthorOrcid != null) filterListRegex(publications, p -> p.getCorrespAuthor(), ca -> ca.getOrcid(), args.correspAuthorOrcid, "publications", "a corresponding author ORCID iD", true, false, !preFilter);
		if (args.notCorrespAuthorOrcid != null) filterListRegex(publications, p -> p.getCorrespAuthor(), ca -> ca.getOrcid(), args.notCorrespAuthorOrcid, "publications", "corresponding authors ORCID iDs", false, true, !preFilter);
		if (args.correspAuthorOrcidEmpty) filterList(publications, p -> p.getCorrespAuthor(), ca -> ca.getOrcid().isEmpty(), "publications", "corresponding authors ORCID iDs", "empty", true, true, !preFilter);
		if (args.notCorrespAuthorOrcidEmpty) filterList(publications, p -> p.getCorrespAuthor(), ca -> !ca.getOrcid().isEmpty(), "publications", "a corresponding author ORCID iD", "not empty", true, false, !preFilter);
		if (args.correspAuthorEmail != null) filterListRegex(publications, p -> p.getCorrespAuthor(), ca -> ca.getEmail(), args.correspAuthorEmail, "publications", "a corresponding author e-mail", true, false, !preFilter);
		if (args.notCorrespAuthorEmail != null) filterListRegex(publications, p -> p.getCorrespAuthor(), ca -> ca.getEmail(), args.notCorrespAuthorEmail, "publications", "corresponding authors e-mails", false, true, !preFilter);
		if (args.correspAuthorEmailEmpty) filterList(publications, p -> p.getCorrespAuthor(), ca -> ca.getEmail().isEmpty(), "publications", "corresponding authors e-mails", "empty", true, true, !preFilter);
		if (args.notCorrespAuthorEmailEmpty) filterList(publications, p -> p.getCorrespAuthor(), ca -> !ca.getEmail().isEmpty(), "publications", "a corresponding author e-mail", "not empty", true, false, !preFilter);
		if (args.correspAuthorPhone != null) filterListRegex(publications, p -> p.getCorrespAuthor(), ca -> ca.getPhone(), args.correspAuthorPhone, "publications", "a corresponding author telephone", true, false, !preFilter);
		if (args.notCorrespAuthorPhone != null) filterListRegex(publications, p -> p.getCorrespAuthor(), ca -> ca.getPhone(), args.notCorrespAuthorPhone, "publications", "corresponding authors telephones", false, true, !preFilter);
		if (args.correspAuthorPhoneEmpty) filterList(publications, p -> p.getCorrespAuthor(), ca -> ca.getPhone().isEmpty(), "publications", "corresponding authors telephones", "empty", true, true, !preFilter);
		if (args.notCorrespAuthorPhoneEmpty) filterList(publications, p -> p.getCorrespAuthor(), ca -> !ca.getPhone().isEmpty(), "publications", "a corresponding author telephone", "not empty", true, false, !preFilter);
		if (args.correspAuthorUri != null) filterListRegex(publications, p -> p.getCorrespAuthor(), ca -> ca.getUri(), args.correspAuthorUri, "publications", "a corresponding author web page", true, false, !preFilter);
		if (args.notCorrespAuthorUri != null) filterListRegex(publications, p -> p.getCorrespAuthor(), ca -> ca.getUri(), args.notCorrespAuthorUri, "publications", "corresponding authors web pages", false, true, !preFilter);
		if (args.correspAuthorUriEmpty) filterList(publications, p -> p.getCorrespAuthor(), ca -> ca.getUri().isEmpty(), "publications", "corresponding authors web pages", "empty", true, true, !preFilter);
		if (args.notCorrespAuthorUriEmpty) filterList(publications, p -> p.getCorrespAuthor(), ca -> !ca.getUri().isEmpty(), "publications", "a corresponding author web page", "not empty", true, false, !preFilter);
		if (args.correspAuthorSize != null) filter(publications, p -> args.correspAuthorSize.contains(p.getCorrespAuthor().size()), "publications", "corresponding authors size " + args.correspAuthorSize, true, !preFilter);
		if (args.notCorrespAuthorSize != null) filter(publications, p -> args.notCorrespAuthorSize.contains(p.getCorrespAuthor().size()), "publications", "corresponding authors size " + args.notCorrespAuthorSize, false, !preFilter);
		if (args.correspAuthorSizeMore != null) filter(publications, p -> p.getCorrespAuthor().size() > args.correspAuthorSizeMore, "publications", "corresponding authors size more than " + args.correspAuthorSizeMore, true, !preFilter);
		if (args.correspAuthorSizeLess != null) filter(publications, p -> p.getCorrespAuthor().size() < args.correspAuthorSizeLess, "publications", "corresponding authors size less than " + args.correspAuthorSizeLess, true, !preFilter);

		if (args.visited != null) filterListRegex(publications, p -> p.getVisitedSites(), l -> l.getUrl().toString(), args.visited, "publications", "a visited site", true, false, !preFilter);
		if (args.notVisited != null) filterListRegex(publications, p -> p.getVisitedSites(), l -> l.getUrl().toString(), args.notVisited, "publications", "visited sites", false, true, !preFilter);
		if (args.visitedHost != null) filterListHost(publications, p -> p.getVisitedSites(), l -> l.getUrl().toString(), args.visitedHost, "publications", "a visited site", true, false, !preFilter);
		if (args.notVisitedHost != null) filterListHost(publications, p -> p.getVisitedSites(), l -> l.getUrl().toString(), args.notVisitedHost, "publications", "visited sites", false, true, !preFilter);
		if (args.visitedType != null) filterList(publications, p -> p.getVisitedSites(), l -> args.visitedType.contains(l.getType()), "publications", "a visited site", "of type " + args.visitedType, true, false, !preFilter);
		if (args.notVisitedType != null) filterList(publications, p -> p.getVisitedSites(), l -> args.notVisitedType.contains(l.getType()), "publications", "visited sites", "of type " + args.notVisitedType, false, true, !preFilter);
		if (args.visitedTypeMore != null) filterList(publications, p -> p.getVisitedSites(), l -> l.getType().isBetterThan(args.visitedTypeMore), "publications", "a visited site", "of type more than " + args.visitedTypeMore, true, false, !preFilter);
		if (args.visitedTypeLess != null) filterList(publications, p -> p.getVisitedSites(), l -> !(l.getType().isEquivalent(args.visitedTypeLess) || l.getType().isBetterThan(args.visitedTypeLess)), "publications", "a visited site", "of type less than " + args.visitedTypeLess, true, false, !preFilter);
		if (args.visitedTypeFinal) filterList(publications, p -> p.getVisitedSites(), l -> l.getType().isFinal(), "publications", "a visited site", "of final type", true, false, !preFilter);
		if (args.notVisitedTypeFinal) filterList(publications, p -> p.getVisitedSites(), l -> l.getType().isFinal(), "publications", "visited sites", "of final type", false, true, !preFilter);
		if (args.visitedTypePdf) filterList(publications, p -> p.getVisitedSites(), l -> l.getType().isPdf(), "publications", "a visited site", "of pdf type", true, false, !preFilter);
		if (args.notVisitedTypePdf) filterList(publications, p -> p.getVisitedSites(), l -> l.getType().isPdf(), "publications", "visited sites", "of pdf type", false, true, !preFilter);
		if (args.visitedFrom != null) filterListRegex(publications, p -> p.getVisitedSites(), l -> l.getFrom(), args.visitedFrom, "publications", "from of a visited site", true, false, !preFilter);
		if (args.notVisitedFrom != null) filterListRegex(publications, p -> p.getVisitedSites(), l -> l.getFrom(), args.notVisitedFrom, "publications", "from of visited sites", false, true, !preFilter);
		if (args.visitedFromHost != null) filterListHost(publications, p -> p.getVisitedSites(), l -> l.getFrom(), args.visitedFromHost, "publications", "from of a visited site", true, false, !preFilter);
		if (args.notVisitedFromHost != null) filterListHost(publications, p -> p.getVisitedSites(), l -> l.getFrom(), args.notVisitedFromHost, "publications", "from of visited sites", false, true, !preFilter);
		if (args.visitedTimeMore != null) filterList(publications, p -> p.getVisitedSites(), l -> l.getTimestamp() >= args.visitedTimeMore, "publications", "a visited time", "more than or equal to " + args.visitedTimeMore, true, false, !preFilter);
		if (args.visitedTimeLess != null) filterList(publications, p -> p.getVisitedSites(), l -> l.getTimestamp() <= args.visitedTimeLess, "publications", "a visited time", "less than or equal to " + args.visitedTimeLess, true, false, !preFilter);
		if (args.visitedSize != null) filter(publications, p -> args.visitedSize.contains(p.getVisitedSites().size()), "publications", "visited sites size " + args.visitedSize, true, !preFilter);
		if (args.notVisitedSize != null) filter(publications, p -> args.notVisitedSize.contains(p.getVisitedSites().size()), "publications", "visited sites size " + args.notVisitedSize, true, !preFilter);
		if (args.visitedSizeMore != null) filter(publications, p -> p.getVisitedSites().size() > args.visitedSizeMore, "publications", "visited sites size more than " + args.visitedSizeMore, true, !preFilter);
		if (args.visitedSizeLess != null) filter(publications, p -> p.getVisitedSites().size() < args.visitedSizeLess, "publications", "visited sites size more than " + args.visitedSizeLess, true, !preFilter);

		if (args.startUrl != null) {
			filterRegex(webpages, w -> w.getStartUrl(), args.startUrl, "webpages", "start URL", true, !preFilter);
			filterRegex(docs, w -> w.getStartUrl(), args.startUrl, "docs", "start URL", true, !preFilter);
		}
		if (args.notStartUrl != null) {
			filterRegex(webpages, w -> w.getStartUrl(), args.notStartUrl, "webpages", "start URL", false, !preFilter);
			filterRegex(docs, w -> w.getStartUrl(), args.notStartUrl, "docs", "start URL", false, !preFilter);
		}
		if (args.startUrlHost != null) {
			filterHost(webpages, w -> w.getStartUrl(), args.startUrlHost, "webpages", "start URL", true, !preFilter);
			filterHost(docs, w -> w.getStartUrl(), args.startUrlHost, "docs", "start URL", true, !preFilter);
		}
		if (args.notStartUrlHost != null) {
			filterHost(webpages, w -> w.getStartUrl(), args.notStartUrlHost, "webpages", "start URL", false, !preFilter);
			filterHost(docs, w -> w.getStartUrl(), args.notStartUrlHost, "docs", "start URL", false, !preFilter);
		}

		if (args.finalUrl != null) {
			filterRegex(webpages, w -> w.getFinalUrl(), args.finalUrl, "webpages", "final URL", true, !preFilter);
			filterRegex(docs, w -> w.getFinalUrl(), args.finalUrl, "docs", "final URL", true, !preFilter);
		}
		if (args.notFinalUrl != null) {
			filterRegex(webpages, w -> w.getFinalUrl(), args.notFinalUrl, "webpages", "final URL", false, !preFilter);
			filterRegex(docs, w -> w.getFinalUrl(), args.notFinalUrl, "docs", "final URL", false, !preFilter);
		}
		if (args.finalUrlHost != null) {
			filterHost(webpages, w -> w.getFinalUrl(), args.finalUrlHost, "webpages", "final URL", true, !preFilter);
			filterHost(docs, w -> w.getFinalUrl(), args.finalUrlHost, "docs", "final URL", true, !preFilter);
		}
		if (args.notFinalUrlHost != null) {
			filterHost(webpages, w -> w.getFinalUrl(), args.notFinalUrlHost, "webpages", "final URL", false, !preFilter);
			filterHost(docs, w -> w.getFinalUrl(), args.notFinalUrlHost, "docs", "final URL", false, !preFilter);
		}
		if (args.finalUrlEmpty) {
			filter(webpages, w -> w.getFinalUrl().isEmpty(), "webpages", "empty final URL", true, !preFilter);
			filter(docs, w -> w.getFinalUrl().isEmpty(), "docs", "empty final URL", true, !preFilter);
		}
		if (args.notFinalUrlEmpty) {
			filter(webpages, w -> w.getFinalUrl().isEmpty(), "webpages", "empty final URL", false, !preFilter);
			filter(docs, w -> w.getFinalUrl().isEmpty(), "docs", "empty final URL", false, !preFilter);
		}

		if (args.contentType != null) {
			filterRegex(webpages, w -> w.getContentType(), args.contentType, "webpages", "content type", true, !preFilter);
			filterRegex(docs, w -> w.getContentType(), args.contentType, "docs", "content type", true, !preFilter);
		}
		if (args.notContentType != null) {
			filterRegex(webpages, w -> w.getContentType(), args.notContentType, "webpages", "content type", false, !preFilter);
			filterRegex(docs, w -> w.getContentType(), args.notContentType, "docs", "content type", false, !preFilter);
		}
		if (args.contentTypeEmpty) {
			filter(webpages, w -> w.getContentType().isEmpty(), "webpages", "empty content type", true, !preFilter);
			filter(docs, w -> w.getContentType().isEmpty(), "docs", "empty content type", true, !preFilter);
		}
		if (args.notContentTypeEmpty) {
			filter(webpages, w -> w.getContentType().isEmpty(), "webpages", "empty content type", false, !preFilter);
			filter(docs, w -> w.getContentType().isEmpty(), "docs", "empty content type", false, !preFilter);
		}

		if (args.statusCode != null) {
			filter(webpages, w -> args.statusCode.contains(w.getStatusCode()), "webpages", "status code " + args.statusCode, true, !preFilter);
			filter(docs, w -> args.statusCode.contains(w.getStatusCode()), "docs", "status code " + args.statusCode, true, !preFilter);
		}
		if (args.notStatusCode != null) {
			filter(webpages, w -> args.notStatusCode.contains(w.getStatusCode()), "webpages", "status code " + args.notStatusCode, false, !preFilter);
			filter(docs, w -> args.notStatusCode.contains(w.getStatusCode()), "docs", "status code " + args.notStatusCode, false, !preFilter);
		}
		if (args.statusCodeMore != null) {
			filter(webpages, w -> w.getStatusCode() > args.statusCodeMore, "webpages", "status code more than " + args.statusCodeMore, true, !preFilter);
			filter(docs, w -> w.getStatusCode() > args.statusCodeMore, "docs", "status code more than " + args.statusCodeMore, true, !preFilter);
		}
		if (args.statusCodeLess != null) {
			filter(webpages, w -> w.getStatusCode() < args.statusCodeLess, "webpages", "status code less than " + args.statusCodeLess, true, !preFilter);
			filter(docs, w -> w.getStatusCode() < args.statusCodeLess, "docs", "status code less than " + args.statusCodeLess, true, !preFilter);
		}

		if (args.title != null) {
			filterRegex(webpages, w -> w.getTitle(), args.title, "webpages", "title", true, !preFilter);
			filterRegex(docs, w -> w.getTitle(), args.title, "docs", "title", true, !preFilter);
		}
		if (args.notTitle != null) {
			filterRegex(webpages, w -> w.getTitle(), args.notTitle, "webpages", "title", false, !preFilter);
			filterRegex(docs, w -> w.getTitle(), args.notTitle, "docs", "title", false, !preFilter);
		}
		if (args.titleSize != null) {
			filter(webpages, w -> args.titleSize.contains(w.getTitle().length()), "webpages", "title length " + args.titleSize, true, !preFilter);
			filter(docs, w -> args.titleSize.contains(w.getTitle().length()), "docs", "title length " + args.titleSize, true, !preFilter);
		}
		if (args.notTitleSize != null) {
			filter(webpages, w -> args.notTitleSize.contains(w.getTitle().length()), "webpages", "title length " + args.notTitleSize, false, !preFilter);
			filter(docs, w -> args.notTitleSize.contains(w.getTitle().length()), "docs", "title length " + args.notTitleSize, false, !preFilter);
		}
		if (args.titleSizeMore != null) {
			filter(webpages, w -> w.getTitle().length() > args.titleSizeMore, "webpages", "title length more than " + args.titleSizeMore, true, !preFilter);
			filter(docs, w -> w.getTitle().length() > args.titleSizeMore, "docs", "title length more than " + args.titleSizeMore, true, !preFilter);
		}
		if (args.titleSizeLess != null) {
			filter(webpages, w -> w.getTitle().length() < args.titleSizeLess, "webpages", "title length less than " + args.titleSizeLess, true, !preFilter);
			filter(docs, w -> w.getTitle().length() < args.titleSizeLess, "docs", "title length less than " + args.titleSizeLess, true, !preFilter);
		}

		if (args.content != null) {
			filterRegex(webpages, w -> w.getContent(), args.content, "webpages", "content", true, !preFilter);
			filterRegex(docs, w -> w.getContent(), args.content, "docs", "content", true, !preFilter);
		}
		if (args.notContent != null) {
			filterRegex(webpages, w -> w.getContent(), args.notContent, "webpages", "content", false, !preFilter);
			filterRegex(docs, w -> w.getContent(), args.notContent, "docs", "content", false, !preFilter);
		}
		if (args.contentSize != null) {
			filter(webpages, w -> args.contentSize.contains(w.getContent().length()), "webpages", "content length " + args.contentSize, true, !preFilter);
			filter(docs, w -> args.contentSize.contains(w.getContent().length()), "docs", "content length " + args.contentSize, true, !preFilter);
		}
		if (args.notContentSize != null) {
			filter(webpages, w -> args.notContentSize.contains(w.getContent().length()), "webpages", "content length " + args.notContentSize, false, !preFilter);
			filter(docs, w -> args.notContentSize.contains(w.getContent().length()), "docs", "content length " + args.notContentSize, false, !preFilter);
		}
		if (args.contentSizeMore != null) {
			filter(webpages, w -> w.getContent().length() > args.contentSizeMore, "webpages", "content length more than " + args.contentSizeMore, true, !preFilter);
			filter(docs, w -> w.getContent().length() > args.contentSizeMore, "docs", "content length more than " + args.contentSizeMore, true, !preFilter);
		}
		if (args.contentSizeLess != null) {
			filter(webpages, w -> w.getContent().length() < args.contentSizeLess, "webpages", "content length less than " + args.contentSizeLess, true, !preFilter);
			filter(docs, w -> w.getContent().length() < args.contentSizeLess, "docs", "content length less than " + args.contentSizeLess, true, !preFilter);
		}

		if (args.contentTimeMore != null) {
			filter(webpages, w -> w.getContentTime() >= args.contentTimeMore, "webpages", "content time more than or equal to " + timeHuman(args.contentTimeMore), true, !preFilter);
			filter(docs, w -> w.getContentTime() >= args.contentTimeMore, "docs", "content time more than or equal to " + timeHuman(args.contentTimeMore), true, !preFilter);
		}
		if (args.contentTimeLess != null) {
			filter(webpages, w -> w.getContentTime() <= args.contentTimeLess, "webpages", "content time less than or equal to " + timeHuman(args.contentTimeLess), true, !preFilter);
			filter(docs, w -> w.getContentTime() <= args.contentTimeLess, "docs", "content time less than or equal to " + timeHuman(args.contentTimeLess), true, !preFilter);
		}

		if (args.license != null) {
			filterRegex(webpages, w -> w.getLicense(), args.license, "webpages", "license", true, !preFilter);
			filterRegex(docs, w -> w.getLicense(), args.license, "docs", "license", true, !preFilter);
		}
		if (args.notLicense != null) {
			filterRegex(webpages, w -> w.getLicense(), args.notLicense, "webpages", "license", false, !preFilter);
			filterRegex(docs, w -> w.getLicense(), args.notLicense, "docs", "license", false, !preFilter);
		}
		if (args.licenseEmpty) {
			filter(webpages, w -> w.getLicense().isEmpty(), "webpages", "empty license", true, !preFilter);
			filter(docs, w -> w.getLicense().isEmpty(), "docs", "empty license", true, !preFilter);
		}
		if (args.notLicenseEmpty) {
			filter(webpages, w -> w.getLicense().isEmpty(), "webpages", "empty license", false, !preFilter);
			filter(docs, w -> w.getLicense().isEmpty(), "docs", "empty license", false, !preFilter);
		}

		if (args.language != null) {
			filterRegex(webpages, w -> w.getLanguage(), args.language, "webpages", "language", true, !preFilter);
			filterRegex(docs, w -> w.getLanguage(), args.language, "docs", "language", true, !preFilter);
		}
		if (args.notLanguage != null) {
			filterRegex(webpages, w -> w.getLanguage(), args.notLanguage, "webpages", "language", false, !preFilter);
			filterRegex(docs, w -> w.getLanguage(), args.notLanguage, "docs", "language", false, !preFilter);
		}
		if (args.languageEmpty) {
			filter(webpages, w -> w.getLanguage().isEmpty(), "webpages", "empty language", true, !preFilter);
			filter(docs, w -> w.getLanguage().isEmpty(), "docs", "empty language", true, !preFilter);
		}
		if (args.notLanguageEmpty) {
			filter(webpages, w -> w.getLanguage().isEmpty(), "webpages", "empty language", false, !preFilter);
			filter(docs, w -> w.getLanguage().isEmpty(), "docs", "empty language", false, !preFilter);
		}

		if (args.grep != null) {
			filterRegex(publications, e -> e.toStringPlain(), args.grep, "publications", "whole plain content", true, !preFilter);
			filterRegex(webpages, e -> e.toStringPlain(), args.grep, "webpages", "whole plain content", true, !preFilter);
			filterRegex(docs, e -> e.toStringPlain(), args.grep, "docs", "whole plain content", true, !preFilter);
		}
		if (args.notGrep != null) {
			filterRegex(publications, e -> e.toStringPlain(), args.notGrep, "publications", "whole plain content", false, !preFilter);
			filterRegex(webpages, e -> e.toStringPlain(), args.notGrep, "webpages", "whole plain content", false, !preFilter);
			filterRegex(docs, e -> e.toStringPlain(), args.notGrep, "docs", "whole plain content", false, !preFilter);
		}

		if (args.hasScrape) {
			filter(webpages, w -> fetcher.getScrape().getWebpage(w.getFinalUrl()) != null, "webpages", "scrape rules", true, !preFilter);
			filter(docs, w -> fetcher.getScrape().getWebpage(w.getFinalUrl()) != null, "docs", "scrape rules", true, !preFilter);
		}
		if (args.notHasScrape) {
			filter(webpages, w -> fetcher.getScrape().getWebpage(w.getFinalUrl()) != null, "webpages", "scrape rules", false, !preFilter);
			filter(docs, w -> fetcher.getScrape().getWebpage(w.getFinalUrl()) != null, "docs", "scrape rules", false, !preFilter);
		}

		int count = 0;
		if (publications != null) count += publications.size();
		if (webpages != null) count += webpages.size();
		if (docs != null) count += docs.size();
		return count > 0;
	}

	private static void checkPartArg(String partArgName, PubFetcherArgs args) throws ReflectiveOperationException {
		String partArgPartName = partArgName + "Part";
		Field partArg = PubFetcherArgs.class.getDeclaredField(partArgName);
		Field partArgPart = PubFetcherArgs.class.getDeclaredField(partArgPartName);
		Object partArgObject = partArg.get(args);
		Object partArgPartObject = partArgPart.get(args);
		boolean onlyPartArgSpecified = partArgObject != null && !(partArgObject instanceof Boolean) && partArgPartObject == null || partArgObject != null && partArgObject instanceof Boolean && ((Boolean) partArgObject) && partArgPartObject == null;
		boolean onlyPartArgPartSpecified = partArgObject == null && partArgPartObject != null || partArgObject != null && partArgObject instanceof Boolean && !((Boolean) partArgObject) && partArgPartObject != null;
		if (onlyPartArgSpecified || onlyPartArgPartSpecified) {
			String partArgParameter = Arrays.toString(PubFetcherArgs.class.getDeclaredField(partArgName).getAnnotation(Parameter.class).names());
			String partArgPartParameter = Arrays.toString(PubFetcherArgs.class.getDeclaredField(partArgPartName).getAnnotation(Parameter.class).names());
			if (onlyPartArgSpecified) {
				throw new ParameterException("If " + partArgParameter + " is specified, then " + partArgPartParameter + " must also be specified");
			} else {
				throw new ParameterException("If " + partArgPartParameter + " is specified, then " + partArgParameter + " must also be specified");
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static void run(PubFetcherArgs args, Fetcher fetcher, FetcherArgs fetcherArgs, List<PublicationIds> externalPublicationIds,
			List<String> externalWebpageUrls, List<String> externalDocUrls, Version version, String[] argv) throws IOException, ReflectiveOperationException {
		if (args == null) {
			throw new IllegalArgumentException("Args required!");
		}
		if (fetcher == null) {
			throw new IllegalArgumentException("Fetcher required!");
		}
		if (fetcherArgs == null) {
			throw new IllegalArgumentException("FetcherArgs required!");
		}

		checkPartArg("partContent", args);
		checkPartArg("notPartContent", args);
		checkPartArg("partSize", args);
		checkPartArg("notPartSize", args);
		checkPartArg("partSizeMore", args);
		checkPartArg("partSizeLess", args);
		checkPartArg("partType", args);
		checkPartArg("notPartType", args);
		checkPartArg("partTypeMore", args);
		checkPartArg("partTypeLess", args);
		checkPartArg("partTypeFinal", args);
		checkPartArg("notPartTypeFinal", args);
		checkPartArg("partTypePdf", args);
		checkPartArg("notPartTypePdf", args);
		checkPartArg("partUrl", args);
		checkPartArg("notPartUrl", args);
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

		if (args.dbInit != null) dbInit(args.dbInit);
		if (args.dbCommit != null) dbCommit(args.dbCommit);
		if (args.dbCompact != null) dbCompact(args.dbCompact);

		if (args.dbPublicationsSize != null) dbPublicationsSize(args.dbPublicationsSize);
		if (args.dbWebpagesSize != null) dbWebpagesSize(args.dbWebpagesSize);
		if (args.dbDocsSize != null) dbDocsSize(args.dbDocsSize);

		if (args.dbPublicationsMap != null) dbPublicationsMap(args.dbPublicationsMap);
		if (args.dbPublicationsMapReverse != null) dbPublicationsMapReverse(args.dbPublicationsMapReverse);

		if (args.fetchDocument != null) {
			fetchDocument(args.fetchDocument, fetcher, fetcherArgs);
		}
		if (args.fetchDocumentJavascript != null) {
			fetchDocumentJavascript(args.fetchDocumentJavascript, fetcher, fetcherArgs);
		}
		if (args.postDocument != null) {
			postDocument(args.postDocument, fetcher, fetcherArgs);
		}
		if (args.fetchWebpageSelector != null) {
			fetchWebpageSelector(args.fetchWebpageSelector.get(0), args.fetchWebpageSelector.get(1), args.fetchWebpageSelector.get(2),
				(args.fetchWebpageSelector.get(3) == null || args.fetchWebpageSelector.get(3).isEmpty()) ? null : Boolean.valueOf(args.fetchWebpageSelector.get(3)), args.plain, args.format, version, argv, fetcher, fetcherArgs);
		}

		if (args.scrapeSite != null) scrapeSite(args.scrapeSite, fetcher);
		if (args.scrapeSelector != null) scrapeSelector(args.scrapeSelector.get(0), ScrapeSiteKey.valueOf(args.scrapeSelector.get(1)), fetcher);
		if (args.scrapeWebpage != null) scrapeWebpage(args.scrapeWebpage, fetcher);
		if (args.scrapeJavascript != null) scrapeJavascript(args.scrapeJavascript, fetcher);
		if (args.scrapeOff != null) scrapeOff(args.scrapeOff, fetcher);

		if (args.isPmid != null) isPmid(args.isPmid);
		if (args.isPmcid != null) isPmcid(args.isPmcid);
		if (args.extractPmcid != null) extractPmcid(args.extractPmcid);
		if (args.isDoi != null) isDoi(args.isDoi);
		if (args.normaliseDoi != null) normaliseDoi(args.normaliseDoi);
		if (args.extractDoiRegistrant != null) extractDoiRegistrant(args.extractDoiRegistrant);

		if (args.escapeHtml != null) escapeHtml(args.escapeHtml);
		if (args.escapeHtmlAttribute != null) escapeHtmlAttribute(args.escapeHtmlAttribute);

		if (args.checkPublicationId != null) checkPublicationId(args.checkPublicationId);
		if (args.checkPublicationIds != null) checkPublicationIds(args.checkPublicationIds.get(0), args.checkPublicationIds.get(1), args.checkPublicationIds.get(2));
		if (args.checkUrl != null) checkUrl(args.checkUrl);

		// add IDs

		Set<PublicationIds> publicationIds = new LinkedHashSet<>();
		Set<String> webpageUrls = new LinkedHashSet<>();
		Set<String> docUrls = new LinkedHashSet<>();

		if (externalPublicationIds != null) {
			publicationIds.addAll((List<PublicationIds>) idsCheck(externalPublicationIds, DatabaseEntryType.publication));
			logger.info("Got {} new distinct external publication IDs", publicationIds.size());
		}
		if (externalWebpageUrls != null) {
			webpageUrls.addAll((List<String>) idsCheck(externalWebpageUrls, DatabaseEntryType.webpage));
			logger.info("Got {} new distinct external webpage URLs", webpageUrls.size());
		}
		if (externalDocUrls != null) {
			docUrls.addAll((List<String>) idsCheck(externalDocUrls, DatabaseEntryType.doc));
			logger.info("Got {} new distinct external doc URLs", docUrls.size());
		}

		if (args.pub != null) {
			int sizeBefore = publicationIds.size();
			publicationIds.addAll((List<PublicationIds>) idsCheck(args.pub, DatabaseEntryType.publication));
			logger.info("Got {} new distinct publication IDs from command line", publicationIds.size() - sizeBefore);
		}
		if (args.web != null) {
			int sizeBefore = webpageUrls.size();
			webpageUrls.addAll((List<String>) idsCheck(args.web, DatabaseEntryType.webpage));
			logger.info("Got {} new distinct webpage URLs from command line", webpageUrls.size() - sizeBefore);
		}
		if (args.doc != null) {
			int sizeBefore = docUrls.size();
			docUrls.addAll((List<String>) idsCheck(args.doc, DatabaseEntryType.doc));
			logger.info("Got {} new distinct doc URLs from command line", docUrls.size() - sizeBefore);
		}

		if (args.pubFile != null) {
			int sizeBefore = publicationIds.size();
			publicationIds.addAll((List<PublicationIds>) idsCheck(PubFetcher.pubFile(args.pubFile, PUB_ID_SOURCE), DatabaseEntryType.publication));
			logger.info("Got {} new distinct publication IDs from file {}", publicationIds.size() - sizeBefore, args.pubFile);
		}
		if (args.webFile != null) {
			int sizeBefore = webpageUrls.size();
			webpageUrls.addAll((List<String>) idsCheck(PubFetcher.webFile(args.webFile), DatabaseEntryType.webpage));
			logger.info("Got {} new distinct webpage URLs from file {}", webpageUrls.size() - sizeBefore, args.webFile);
		}
		if (args.docFile != null) {
			int sizeBefore = docUrls.size();
			docUrls.addAll((List<String>) idsCheck(PubFetcher.webFile(args.docFile), DatabaseEntryType.doc));
			logger.info("Got {} new distinct doc URLs from file {}", docUrls.size() - sizeBefore, args.docFile);
		}

		if (args.pubDb != null) {
			int sizeBefore = publicationIds.size();
			publicationIds.addAll((List<PublicationIds>) idsDb(args.pubDb, DatabaseEntryType.publication));
			logger.info("Got {} new distinct publication IDs from database {}", publicationIds.size() - sizeBefore, args.pubDb);
		}
		if (args.webDb != null) {
			int sizeBefore = webpageUrls.size();
			webpageUrls.addAll((List<String>) idsDb(args.webDb, DatabaseEntryType.webpage));
			logger.info("Got {} new distinct webpage URLs from database {}", webpageUrls.size() - sizeBefore, args.webDb);
		}
		if (args.docDb != null) {
			int sizeBefore = docUrls.size();
			docUrls.addAll((List<String>) idsDb(args.docDb, DatabaseEntryType.doc));
			logger.info("Got {} new distinct doc URLs from database {}", docUrls.size() - sizeBefore, args.docDb);
		}

		boolean publicationIdsGiven = !publicationIds.isEmpty();
		boolean webpageUrlsGiven = !webpageUrls.isEmpty();
		boolean docUrlsGiven = !docUrls.isEmpty();

		// filter IDs

		if (args.hasPmid) filter(publicationIds, id -> !id.getPmid().isEmpty(), "publication IDs", "PMID present", true, true);
		if (args.notHasPmid) filter(publicationIds, id -> !id.getPmid().isEmpty(), "publication IDs", "PMID present", false, true);
		if (args.pmid != null) filterRegex(publicationIds, id -> id.getPmid(), args.pmid, "publication IDs", "PMID", true, true);
		if (args.notPmid != null) filterRegex(publicationIds, id -> id.getPmid(), args.notPmid, "publication IDs", "PMID", false, true);
		if (args.pmidUrl != null) filterRegex(publicationIds, id -> id.getPmidUrl(), args.pmidUrl, "publication IDs", "PMID URL", true, true);
		if (args.notPmidUrl != null) filterRegex(publicationIds, id -> id.getPmidUrl(), args.notPmidUrl, "publication IDs", "PMID URL", false, true);

		if (args.hasPmcid) filter(publicationIds, id -> !id.getPmcid().isEmpty(), "publication IDs", "PMCID present", true, true);
		if (args.notHasPmcid) filter(publicationIds, id -> !id.getPmcid().isEmpty(), "publication IDs", "PMCID present", false, true);
		if (args.pmcid != null) filterRegex(publicationIds, id -> id.getPmcid(), args.pmcid, "publication IDs", "PMCID", true, true);
		if (args.notPmcid != null) filterRegex(publicationIds, id -> id.getPmcid(), args.notPmcid, "publication IDs", "PMCID", false, true);
		if (args.pmcidUrl != null) filterRegex(publicationIds, id -> id.getPmcidUrl(), args.pmcidUrl, "publication IDs", "PMCID URL", true, true);
		if (args.notPmcidUrl != null) filterRegex(publicationIds, id -> id.getPmcidUrl(), args.notPmcidUrl, "publication IDs", "PMCID URL", false, true);

		if (args.hasDoi) filter(publicationIds, id -> !id.getDoi().isEmpty(), "publication IDs", "DOI present", true, true);
		if (args.notHasDoi) filter(publicationIds, id -> !id.getDoi().isEmpty(), "publication IDs", "DOI present", false, true);
		if (args.doi != null) filterRegex(publicationIds, id -> id.getDoi(), args.doi, "publication IDs", "DOI", true, true);
		if (args.notDoi != null) filterRegex(publicationIds, id -> id.getDoi(), args.notDoi, "publication IDs", "DOI", false, true);
		if (args.doiUrl != null) filterRegex(publicationIds, id -> id.getDoiUrl(), args.doiUrl, "publication IDs", "DOI URL", true, true);
		if (args.notDoiUrl != null) filterRegex(publicationIds, id -> id.getDoiUrl(), args.notDoiUrl, "publication IDs", "DOI URL", false, true);
		if (args.doiRegistrant != null) filter(publicationIds, id -> args.doiRegistrant.contains(PubFetcher.extractDoiRegistrant(id.getDoi())), "publication IDs", "DOI registrant of " + args.doiRegistrant, true, true);
		if (args.notDoiRegistrant != null) filter(publicationIds, id -> args.notDoiRegistrant.contains(PubFetcher.extractDoiRegistrant(id.getDoi())), "publication IDs", "DOI registrant of " + args.notDoiRegistrant, false, true);

		if (args.url != null) {
			filterRegex(webpageUrls, url -> url, args.url, "webpage URLs", "URL", true, true);
			filterRegex(docUrls, url -> url, args.url, "doc URLs", "URL", true, true);
		}
		if (args.notUrl != null) {
			filterRegex(webpageUrls, url -> url, args.notUrl, "webpage URLs", "URL", false, true);
			filterRegex(docUrls, url -> url, args.notUrl, "doc URLs", "URL", false, true);
		}
		if (args.urlHost != null) {
			filterHost(webpageUrls, url -> url, args.urlHost, "webpage URLs", "URL", true, true);
			filterHost(docUrls, url -> url, args.urlHost, "doc URLs", "URL", true, true);
		}
		if (args.notUrlHost != null) {
			filterHost(webpageUrls, url -> url, args.notUrlHost, "webpage URLs", "URL", false, true);
			filterHost(docUrls, url -> url, args.notUrlHost, "doc URLs", "URL", false, true);
		}

		if (args.inDb != null) {
			try (Database db = new Database(args.inDb)) {
				filter(publicationIds, id -> db.containsPublication(id), "publication IDs", "presence in database " + args.inDb, true, true);
				filter(webpageUrls, url -> db.containsWebpage(url), "webpage URLs", "presence in database " + args.inDb, true, true);
				filter(docUrls, url -> db.containsDoc(url), "doc IDs", "presence in database " + args.inDb, true, true);
			}
		}
		if (args.notInDb != null) {
			try (Database db = new Database(args.notInDb)) {
				filter(publicationIds, id -> db.containsPublication(id), "publication IDs", "presence in database " + args.notInDb, false, true);
				filter(webpageUrls, url -> db.containsWebpage(url), "webpage URLs", "presence in database " + args.notInDb, false, true);
				filter(docUrls, url -> db.containsDoc(url), "doc IDs", "presence in database " + args.notInDb, false, true);
			}
		}

		// sort IDs

		if (args.ascIds) {
			publicationIds = ascIds(publicationIds, "publication IDs");
			webpageUrls = ascIds(webpageUrls, "webpage URLs");
			docUrls = ascIds(docUrls, "doc URLs");
		}
		if (args.descIds) {
			publicationIds = descIds(publicationIds, "publication IDs");
			webpageUrls = descIds(webpageUrls, "webpage URLs");
			docUrls = descIds(docUrls, "doc URLs");
		}

		// limit IDs

		if (args.headIds != null) {
			head(publicationIds, args.headIds, "publication IDs");
			head(webpageUrls, args.headIds, "webpage URLs");
			head(docUrls, args.headIds, "doc URLs");
		}
		if (args.tailIds != null) {
			tail(publicationIds, args.tailIds, "publication IDs");
			tail(webpageUrls, args.tailIds, "webpage URLs");
			tail(docUrls, args.tailIds, "doc URLs");
		}

		// remove from database by IDs

		if (args.removeIds != null) {
			removeIds(publicationIds, args.removeIds, DatabaseEntryType.publication);
			removeIds(webpageUrls, args.removeIds, DatabaseEntryType.webpage);
			removeIds(docUrls, args.removeIds, DatabaseEntryType.doc);
		}

		// output IDs

		if (args.outIds) {
			JsonGenerator generator = null;
			StringWriter writer = null;
			if (args.format == Format.json) {
				writer = new StringWriter();
				generator = PubFetcher.getJsonGenerator(null, writer);
				PubFetcher.jsonBegin(generator, version, argv);
			}
			outIds(publicationIds, args.plain, args.format, generator, DatabaseEntryType.publication);
			outIds(webpageUrls, args.plain, args.format, generator, DatabaseEntryType.webpage);
			outIds(docUrls, args.plain, args.format, generator, DatabaseEntryType.doc);
			if (args.format == Format.json) {
				PubFetcher.jsonEnd(generator);
				generator.close();
				System.out.println(writer.toString());
			}
		}

		if (args.txtIdsPub != null) txtIds(publicationIds, args.plain, args.format, version, argv, args.txtIdsPub, DatabaseEntryType.publication);
		if (args.txtIdsWeb != null) txtIds(webpageUrls, args.plain, args.format, version, argv, args.txtIdsWeb, DatabaseEntryType.webpage);
		if (args.txtIdsDoc != null) txtIds(docUrls, args.plain, args.format, version, argv, args.txtIdsDoc, DatabaseEntryType.doc);

		if (args.countIds) {
			if (publicationIdsGiven) count("Publication IDs", publicationIds);
			if (webpageUrlsGiven) count("Webpage URLs   ", webpageUrls);
			if (docUrlsGiven) count("Doc URLs       ", docUrls);
		}

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

		// get content

		List<Publication> publications = new ArrayList<>();
		List<Webpage> webpages = new ArrayList<>();
		List<Webpage> docs = new ArrayList<>();

		if (args.db != null) {
			publications.addAll(db(args, fetcher, fetcherArgs, publicationIds, args.db, args.limit <= 0 ? publicationIds.size() : args.limit - publications.size(), DatabaseEntryType.publication));
			webpages.addAll(db(args, fetcher, fetcherArgs, webpageUrls, args.db, args.limit <= 0 ? webpageUrls.size() : args.limit - webpages.size(), DatabaseEntryType.webpage));
			docs.addAll(db(args, fetcher, fetcherArgs, docUrls, args.db, args.limit <= 0 ? docUrls.size() : args.limit - docs.size(), DatabaseEntryType.doc));
		}

		if (args.fetch) {
			publications.addAll(fetch(args, publicationIds, fetcher, parts, fetcherArgs, args.limit <= 0 ? publicationIds.size() : args.limit - publications.size(), false, DatabaseEntryType.publication));
			webpages.addAll(fetch(args, webpageUrls, fetcher, null, fetcherArgs, args.limit <= 0 ? webpageUrls.size() : args.limit - webpages.size(), false, DatabaseEntryType.webpage));
			docs.addAll(fetch(args, docUrls, fetcher, null, fetcherArgs, args.limit <= 0 ? docUrls.size() : args.limit - docs.size(), false, DatabaseEntryType.doc));
		}

		if (args.fetchPut != null) {
			publications.addAll(fetchPut(args, publicationIds, args.fetchPut, fetcher, parts, fetcherArgs, args.limit <= 0 ? publicationIds.size() : args.limit - publications.size(), false, DatabaseEntryType.publication));
			webpages.addAll(fetchPut(args, webpageUrls, args.fetchPut, fetcher, null, fetcherArgs, args.limit <= 0 ? webpageUrls.size() : args.limit - webpages.size(), false, DatabaseEntryType.webpage));
			docs.addAll(fetchPut(args, docUrls, args.fetchPut, fetcher, null, fetcherArgs, args.limit <= 0 ? docUrls.size() : args.limit - docs.size(), false, DatabaseEntryType.doc));
		}

		if (args.dbFetch != null) {
			publications.addAll(dbFetch(args, args.threads, publicationIds, args.dbFetch, fetcher, parts, fetcherArgs, false, args.limit <= 0 ? publicationIds.size() : args.limit - publications.size(), false, DatabaseEntryType.publication));
			webpages.addAll(dbFetch(args, args.threads, webpageUrls, args.dbFetch, fetcher, null, fetcherArgs, false, args.limit <= 0 ? webpageUrls.size() : args.limit - webpages.size(), false, DatabaseEntryType.webpage));
			docs.addAll(dbFetch(args, args.threads, docUrls, args.dbFetch, fetcher, null, fetcherArgs, false, args.limit <= 0 ? docUrls.size() : args.limit - docs.size(), false, DatabaseEntryType.doc));
		}

		if (args.dbFetchEnd != null) {
			dbFetch(args, args.threads, publicationIds, args.dbFetchEnd, fetcher, parts, fetcherArgs, true, args.limit <= 0 ? publicationIds.size() : args.limit - publications.size(), false, DatabaseEntryType.publication);
			dbFetch(args, args.threads, webpageUrls, args.dbFetchEnd, fetcher, null, fetcherArgs, true, args.limit <= 0 ? webpageUrls.size() : args.limit - webpages.size(), false, DatabaseEntryType.webpage);
			dbFetch(args, args.threads, docUrls, args.dbFetchEnd, fetcher, null, fetcherArgs, true, args.limit <= 0 ? docUrls.size() : args.limit - docs.size(), false, DatabaseEntryType.doc);
		}

		// filter content

		if (!args.preFilter) {
			contentFilter(args, fetcher, fetcherArgs, publications, webpages, docs, false);
		}

		// sort content

		if (args.asc) {
			asc(publications, "publications");
			asc(webpages, "webpages");
			asc(docs, "docs");
		}
		if (args.desc) {
			desc(publications, "publications");
			desc(webpages, "webpages");
			desc(docs, "docs");
		}

		if (args.ascTime) {
			ascTime(publications, "publications");
			ascTime(webpages, "webpages");
			ascTime(docs, "docs");
		}
		if (args.descTime) {
			descTime(publications, "publications");
			descTime(webpages, "webpages");
			descTime(docs, "docs");
		}

		// top hosts

		boolean topHosts = args.outTopHosts
			|| args.txtTopHostsPub != null || args.txtTopHostsWeb != null || args.txtTopHostsDoc != null
			|| args.countTopHosts;

		Map<String, Integer> topHostsPublications = null;
		Map<String, Integer> topHostsWebpages = null;
		Map<String, Integer> topHostsDocs = null;

		if (topHosts) {
			Boolean hasScrape = null;
			if (args.hasScrape) hasScrape = Boolean.TRUE;
			if (args.notHasScrape) hasScrape = Boolean.FALSE;
			topHostsPublications = topHosts(publications, fetcher.getScrape(), hasScrape, DatabaseEntryType.publication);
			topHostsWebpages = topHosts(webpages, fetcher.getScrape(), hasScrape, DatabaseEntryType.webpage);
			topHostsDocs = topHosts(docs, fetcher.getScrape(), hasScrape, DatabaseEntryType.doc);
		}

		// limit content

		if (args.head != null) {
			head(publications, args.head, "publications");
			head(webpages, args.head, "webpages");
			head(docs, args.head, "docs");
			if (topHosts) {
				head(topHostsPublications.entrySet(), args.head, "top hosts from publications");
				head(topHostsWebpages.entrySet(), args.head, "top hosts from webpages");
				head(topHostsDocs.entrySet(), args.head, "top hosts from docs");
			}
		}

		if (args.tail != null) {
			tail(publications, args.tail, "publications");
			tail(webpages, args.tail, "webpages");
			tail(docs, args.tail, "docs");
			if (topHosts) {
				tail(topHostsPublications.entrySet(), args.tail, "top hosts from publications");
				tail(topHostsWebpages.entrySet(), args.tail, "top hosts from webpages");
				tail(topHostsDocs.entrySet(), args.tail, "top hosts from docs");
			}
		}

		// update citations count

		if (args.updateCitationsCount != null) updateCitationsCount(publications, args.updateCitationsCount, fetcher, fetcherArgs, false);

		// put to database

		if (args.put != null) {
			put(publications, args.put, DatabaseEntryType.publication);
			put(webpages, args.put, DatabaseEntryType.webpage);
			put(docs, args.put, DatabaseEntryType.doc);
		}

		// remove from database

		if (args.remove != null) {
			remove(publications, args.remove, DatabaseEntryType.publication);
			remove(webpages, args.remove, DatabaseEntryType.webpage);
			remove(docs, args.remove, DatabaseEntryType.doc);
		}

		// output

		if (args.out) {
			JsonGenerator generator = null;
			StringWriter writer = null;
			if (args.format == Format.json) {
				writer = new StringWriter();
				generator = PubFetcher.getJsonGenerator(null, writer);
				PubFetcher.jsonBegin(generator, version, argv);
			}
			out(publications, args.plain, args.format, generator, args.outPart, fetcherArgs, DatabaseEntryType.publication);
			out(webpages, args.plain, args.format, generator, null, fetcherArgs, DatabaseEntryType.webpage);
			out(docs, args.plain, args.format, generator, null, fetcherArgs, DatabaseEntryType.doc);
			if (args.format == Format.json) {
				PubFetcher.jsonEnd(generator);
				generator.close();
				System.out.println(writer.toString());
			}
		}

		if (args.txtPub != null) txt(publications, args.plain, args.format, version, argv, args.outPart, fetcherArgs, args.txtPub, DatabaseEntryType.publication);
		if (args.txtWeb != null) txt(webpages, args.plain, args.format, version, argv, null, fetcherArgs, args.txtWeb, DatabaseEntryType.webpage);
		if (args.txtDoc != null) txt(docs, args.plain, args.format, version, argv, null, fetcherArgs, args.txtDoc, DatabaseEntryType.doc);

		if (args.count) {
			if (publicationIdsGiven) count("Publications", publications);
			if (webpageUrlsGiven) count("Webpages    ", webpages);
			if (docUrlsGiven) count("Docs        ", docs);
		}

		if (args.outTopHosts) {
			JsonGenerator generator = null;
			StringWriter writer = null;
			if (args.format == Format.json) {
				writer = new StringWriter();
				generator = PubFetcher.getJsonGenerator(null, writer);
				PubFetcher.jsonBegin(generator, version, argv);
			}
			outTopHosts(topHostsPublications, args.format, generator, DatabaseEntryType.publication);
			outTopHosts(topHostsWebpages, args.format, generator, DatabaseEntryType.webpage);
			outTopHosts(topHostsDocs, args.format, generator, DatabaseEntryType.doc);
			if (args.format == Format.json) {
				PubFetcher.jsonEnd(generator);
				generator.close();
				System.out.println(writer.toString());
			}
		}

		if (args.txtTopHostsPub != null) txtTopHosts(topHostsPublications, args.format, version, argv, args.txtTopHostsPub, DatabaseEntryType.publication);
		if (args.txtTopHostsWeb != null) txtTopHosts(topHostsWebpages, args.format, version, argv, args.txtTopHostsWeb, DatabaseEntryType.webpage);
		if (args.txtTopHostsDoc != null) txtTopHosts(topHostsDocs, args.format, version, argv, args.txtTopHostsDoc, DatabaseEntryType.doc);

		if (args.countTopHosts) {
			if (publicationIdsGiven) count("Publications top hosts", topHostsPublications.entrySet());
			if (webpageUrlsGiven) count("Webpages top hosts    ", topHostsWebpages.entrySet());
			if (docUrlsGiven) count("Docs top hosts        ", topHostsDocs.entrySet());
		}

		if (args.partTable) partTable(publications);

		// test

		FetcherTest.run(args.fetcherTestArgs, fetcher, fetcherArgs, parts, PUB_ID_SOURCE);
	}
}
