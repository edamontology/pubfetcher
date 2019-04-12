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

import java.util.List;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

import org.edamontology.pubfetcher.core.common.PositiveInteger;
import org.edamontology.pubfetcher.core.db.publication.PublicationPartName;
import org.edamontology.pubfetcher.core.db.publication.PublicationPartType;
import org.edamontology.pubfetcher.core.fetching.FetcherTestArgs;

public class PubFetcherArgs {

	@ParametersDelegate
	FetcherTestArgs fetcherTestArgs = new FetcherTestArgs();

	@Parameter(names = { "--db-init" }, description = "Create an empty database file. This is the only way to make new databases.")
	String dbInit = null;

	@Parameter(names = { "--db-commit" }, description = "Commit all pending changes by merging all WAL files to the main database file. This has only an effect if WAL files are present beside the database file after an abrupt termination of the program, as normally committing is done in code where required.")
	String dbCommit = null;

	@Parameter(names = { "--db-compact" }, description = "Compaction reclaims space by removing deprecated records (left over after database updates)")
	String dbCompact = null;

	@Parameter(names = { "--db-publications-size" }, description = "Output the number of publications stored in the database to stdout")
	String dbPublicationsSize = null;

	@Parameter(names = { "--db-webpages-size" }, description = "Output the number of webpages stored in the database to stdout")
	String dbWebpagesSize = null;

	@Parameter(names = { "--db-docs-size" }, description = "Output the number of docs stored in the database to stdout")
	String dbDocsSize = null;

	@Parameter(names = { "--db-publications-map" }, description = "Output all PMID to primary ID, PMCID to primary ID and DOI to primary ID mapping pairs stored in the database to stdout")
	String dbPublicationsMap = null;

	@Parameter(names = { "--db-publications-map-reverse" }, description = "Output all mappings from primary ID to the triple [PMID, PMCID, DOI] stored in the database to stdout")
	String dbPublicationsMapReverse = null;

	@Parameter(names = { "--fetch-document" }, description = "Fetch a web page (without JavaScript support, i.e. using jsoup) and output its raw HTML to stdout")
	String fetchDocument = null;

	@Parameter(names = { "--fetch-document-javascript" }, description = "Fetch a web page (with JavaScript support, i.e. using HtmlUnit) and output its raw HTML to stdout")
	String fetchDocumentJavascript = null;

	@Parameter(names = { "--fetch-webpage-selector" }, arity = 4, description = "Fetch a webpage and output it to stdout in the format specified by the output modifiers --plain and --format. Works also for PDF files. \"Title\" and \"content\" args are CSS selectors as supported by jsoup. If the \"title selector\" is an empty string, then the page title will be the text content of the document's <title> element. If the \"content selector\" is an empty string, then content will be the whole text content parsed from the HTML/XML. If javascript arg is \"true\", then fetching will be done using JavaScript support (HtmlUnit), if \"false\", then without JavaScript (jsoup). If javascript arg is empty, then fetching will be done without JavaScript and if the text length of the returned document is less than --webpageMinLengthJavascript or if a <noscript> tag is found in it, a second fetch will happen with JavaScript support.")
	List<String> fetchWebpageSelector = null;

	@Parameter(names = { "--scrape-site" }, description = "Output found journal site name for the given URL to stdout (or \"null\" if not found or URL invalid)")
	String scrapeSite = null;

	@Parameter(names = { "--scrape-selector" }, arity = 2, description = "Output the CSS selector used for extracting the publication part represented by ScrapeSiteKey from the given URL")
	List<String> scrapeSelector = null;

	@Parameter(names = { "--scrape-javascript" }, description = "Output \"true\" or \"false\" depending on whether JavaScript will be used or not for fetching the given publication URL")
	String scrapeJavascript = null;

	@Parameter(names = { "--scrape-webpage" }, description = "Output all CSS selectors used for extracting webpage content and metadata from the given URL (or \"null\" if not found or URL invalid)")
	String scrapeWebpage = null;

	@Parameter(names = { "--is-pmid" }, description = "Output \"true\" or \"false\" depending on whether the given string is a valid PMID or not")
	String isPmid = null;

	@Parameter(names = { "--is-pmcid" }, description = "Output \"true\" or \"false\" depending on whether the given string is a valid PMCID or not")
	String isPmcid = null;

	@Parameter(names = { "--extract-pmcid" }, description = "Remove the prefix \"PMC\" from a PMCID and output the rest. Output an empty string if the given string is not a valid PMCID.")
	String extractPmcid = null;

	@Parameter(names = { "--is-doi" }, description = "Output \"true\" or \"false\" depending on whether the given string is a valid DOI or not")
	String isDoi = null;

	@Parameter(names = { "--normalise-doi" }, description = "Remove any valid prefix (e.g. \"https://doi.org/\", \"doi:\") from a DOI and output the rest, converting letters from the 7-bit ASCII set to uppercase. The validity of the input DOI is not checked.")
	String normaliseDoi = null;

	@Parameter(names = { "--extract-doi-registrant" }, description = "Output the registrant ID of a DOI (the substring after \"10.\" and before \"/\"). Output an empty string if the given string is not a valid DOI.")
	String extractDoiRegistrant = null;

	@Parameter(names = { "--escape-html" }, description = "Output the result of escaping necessary characters in the given string such that it can safely by used as text in a HTML document (without the string interacting with the document's markup)")
	String escapeHtml = null;

	@Parameter(names = { "--escape-html-attribute" }, description = "Output the result of escaping necessary characters in the given string such that it can safely by used as an HTML attribute value (without the string interacting with the document's markup)")
	String escapeHtmlAttribute = null;

	@Parameter(names = { "--check-publication-id" }, description = "Given one publication ID, output it in publication IDs form (\"<pmid>\\t<pmcid>\\t<doi>\") if it is a valid PMID, PMCID or DOI, or throw an exception if it is an invalid publication ID")
	String checkPublicationId = null;

	@Parameter(names = { "--check-publication-ids" }, arity = 3, description = "Given a PMID, a PMCID and a DOI, output them in publication IDs form (\"<pmid>\\t<pmcid>\\t<doi>\") if given IDs are a valid PMID, PMCID and DOI, or throw an exception if at least one is invalid")
	List<String> checkPublicationIds = null;

	@Parameter(names = { "--check-url" }, description = "Given a webpage ID (i.e. a URL), output the parsed URL, or throw an exception if it is an invalid URL")
	String checkUrl = null;

	@Parameter(names = { "-pub" }, variableArity = true, description = "A space-separated list of publication IDs (either PMID, PMCID or DOI) to add")
	List<String> pub = null;

	@Parameter(names = { "-web" }, variableArity = true, description = "A space-separated list of webpage URLs to add")
	List<String> web = null;

	@Parameter(names = { "-doc" }, variableArity = true, description = "A space-separated list of doc URLs to add")
	List<String> doc = null;

	@Parameter(names = { "-pub-file" }, variableArity = true, description = "Load all publication IDs from the specified list of text files containing publication IDs in the form \"<pmid>\\t<pmcid>\\t<doi>\", one per line. Empty lines and lines beginning with \"#\" are ignored.")
	List<String> pubFile = null;

	@Parameter(names = { "-web-file" }, variableArity = true, description = "Load all webpage URLs from the specified list of text files containing webpage URLs, one per line. Empty lines and lines beginning with \"#\" are ignored.")
	List<String> webFile = null;

	@Parameter(names = { "-doc-file" }, variableArity = true, description = "Load all doc URLs from the specified list of text files containing doc URLs, one per line. Empty lines and lines beginning with \"#\" are ignored.")
	List<String> docFile = null;

	@Parameter(names = { "-pub-db" }, variableArity = true, description = "Load all publication IDs found in the specified database files")
	List<String> pubDb = null;

	@Parameter(names = { "-web-db" }, variableArity = true, description = "Load all webpage URLs found in the specified database files")
	List<String> webDb = null;

	@Parameter(names = { "-doc-db" }, variableArity = true, description = "Load all doc URLs found in the specified database files")
	List<String> docDb = null;

	@Parameter(names = { "-has-pmid" }, description = "Only keep publication IDs whose PMID is present")
	boolean hasPmid = false;

	@Parameter(names = { "-not-has-pmid" }, description = "Only keep publication IDs whose PMID is empty")
	boolean notHasPmid = false;

	@Parameter(names = { "-pmid" }, description = "Only keep publication IDs whose PMID has a match with the given regular expression")
	String pmid = null;

	@Parameter(names = { "-not-pmid" }, description = "Only keep publication IDs whose PMID does not have a match with the given regular expression")
	String notPmid = null;

	@Parameter(names = { "-pmid-url" }, description = "Only keep publication IDs whose PMID provenance URL has a match with the given regular expression")
	String pmidUrl = null;

	@Parameter(names = { "-not-pmid-url" }, description = "Only keep publication IDs whose PMID provenance URL does not have a match with the given regular expression")
	String notPmidUrl = null;

	@Parameter(names = { "-has-pmcid" }, description = "Only keep publication IDs whose PMCID is present")
	boolean hasPmcid = false;

	@Parameter(names = { "-not-has-pmcid" }, description = "Only keep publication IDs whose PMCID is empty")
	boolean notHasPmcid = false;

	@Parameter(names = { "-pmcid" }, description = "Only keep publication IDs whose PMCID has a match with the given regular expression")
	String pmcid = null;

	@Parameter(names = { "-not-pmcid" }, description = "Only keep publication IDs whose PMCID does not have a match with the given regular expression")
	String notPmcid = null;

	@Parameter(names = { "-pmcid-url" }, description = "Only keep publication IDs whose PMCID provenance URL has a match with the given regular expression")
	String pmcidUrl = null;

	@Parameter(names = { "-not-pmcid-url" }, description = "Only keep publication IDs whose PMCID provenance URL does not have a match with the given regular expression")
	String notPmcidUrl = null;

	@Parameter(names = { "-has-doi" }, description = "Only keep publication IDs whose DOI is present")
	boolean hasDoi = false;

	@Parameter(names = { "-not-has-doi" }, description = "Only keep publication IDs whose DOI is empty")
	boolean notHasDoi = false;

	@Parameter(names = { "-doi" }, description = "Only keep publication IDs whose DOI has a match with the given regular expression")
	String doi = null;

	@Parameter(names = { "-not-doi" }, description = "Only keep publication IDs whose DOI does not have a match with the given regular expression")
	String notDoi = null;

	@Parameter(names = { "-doi-url" }, description = "Only keep publication IDs whose DOI provenance URL has a match with the given regular expression")
	String doiUrl = null;

	@Parameter(names = { "-not-doi-url" }, description = "Only keep publication IDs whose DOI provenance URL does not have a match with the given regular expression")
	String notDoiUrl = null;

	@Parameter(names = { "-doi-registrant" }, variableArity = true, description = "Only keep publication IDs whose DOI registrant code (the bit after \"10.\" and before \"/\") is present in the given list of strings")
	List<String> doiRegistrant = null;

	@Parameter(names = { "-not-doi-registrant" }, variableArity = true, description = "Only keep publication IDs whose DOI registrant code (the bit after \"10.\" and before \"/\") is not present in the given list of strings")
	List<String> notDoiRegistrant = null;

	@Parameter(names = { "-url" }, description = "Only keep webpage URLs and doc URLs that have a match with the given regular expression")
	String url = null;

	@Parameter(names = { "-not-url" }, description = "Only keep webpage URLs and doc URLs that don't have a match with the given regular expression")
	String notUrl = null;

	@Parameter(names = { "-url-host" }, variableArity = true, description = "Only keep webpage URLs and doc URLs whose host part is present in the given list of strings (comparison is done case-insensitively and \"www.\" is removed)")
	List<String> urlHost = null;

	@Parameter(names = { "-not-url-host" }, variableArity = true, description = "Only keep webpage URLs and doc URLs whose host part is not present in the given list of strings (comparison is done case-insensitively and \"www.\" is removed)")
	List<String> notUrlHost = null;

	@Parameter(names = { "-in-db" }, description = "Only keep publication IDs, webpage URLs and doc URLs that are present in the given database file")
	String inDb = null;

	@Parameter(names = { "-not-in-db" }, description = "Only keep publication IDs, webpage URLs and doc URLs that are not present in the given database file")
	String notInDb = null;

	@Parameter(names = { "-asc-ids" }, description = "Sort publication IDs, webpage URLs and doc URLs in ascending order")
	boolean ascIds = false;

	@Parameter(names = { "-desc-ids" }, description = "Sort publication IDs, webpage URLs and doc URLs is descending order")
	boolean descIds = false;

	@Parameter(names = { "-head-ids" }, validateWith = PositiveInteger.class, description = "Only keep the first given number of publication IDs, webpage URLs and doc URLs")
	Integer headIds = null;

	@Parameter(names = { "-tail-ids" }, validateWith = PositiveInteger.class, description = "Only keep the last given number of publication IDs, webpage URLs and doc URLs")
	Integer tailIds = null;

	@Parameter(names = { "-remove-ids" }, description = "From the given database, remove content corresponding to publication IDs, webpage URLs and doc URLs")
	String removeIds = null;

	@Parameter(names = { "-out-ids" }, description = "Output publication IDs, webpage URLs and doc URLs to stdout in the format specified by the output modifiers --plain and --format")
	boolean outIds = false;

	@Parameter(names = { "-txt-ids-pub" }, description = "Output publication IDs to the given file in the format specified by the output modifiers --plain and --format")
	String txtIdsPub = null;

	@Parameter(names = { "-txt-ids-web" }, description = "Output webpage URLs to the given file in the format specified by --format")
	String txtIdsWeb = null;

	@Parameter(names = { "-txt-ids-doc" }, description = "Output doc URLs to the given file in the format specified by --format")
	String txtIdsDoc = null;

	@Parameter(names = { "-count-ids" }, description = "Output count numbers for publication IDs, webpage URLs and doc URLs to stdout")
	boolean countIds = false;

	@Parameter(names = { "--fetch-part" }, variableArity = true, description = "List of publication parts that will be fetched from the Internet. All other parts will be empty (except the publication IDs which will be filled whenever possible). Fetching of resources not containing any specified parts will be skipped. If used, then --not-fetch-part must not be used. If neither of --fetch-part and --not-fetch-part is used, then all parts will be fetched.")
	List<PublicationPartName> fetchPart = null;

	@Parameter(names = { "--not-fetch-part" }, variableArity = true, description = "List of publication parts that will not be fetched from the Internet. All other parts will be fetched. Fetching of resources not containing any not specified parts will be skipped. If used, then --fetch-part must not be used.")
	List<PublicationPartName> notFetchPart = null;

	@Parameter(names = { "--pre-filter" }, description = "Normally, all content is loaded into memory before specified filtering is applied. This option ties the filtering step to the loading/fetching step for each individual entry, discarding entries not passing the filter right away, thus reducing memory usage. As a tradeoff, in case multiple filters are used, it won't be possible to see in the log how many entries were discarded by each filter.")
	boolean preFilter = false;

	@Parameter(names = { "--limit" }, validateWith = PositiveInteger.class, description = "Maximum number of publications, webpages and docs that can be loaded/fetched. In case the limit is applied, the concrete returned content depends on the order it is loaded/fetched, which depends on the order of content getting operations, then on whether there was a fetchException and last on the ordering of received IDs. If the multithreaded -db-fetch is used or a fetchException happen, then the concrete returned content can vary slightly between equal applications of limit. If --pre-filter is also used, then filters will be applied before the limit, otherwise the limit is applied beforehand and the filters can reduce the number of entries further. Set to 0 to disable.")
	int limit = 0;

	@Parameter(names = { "--threads" }, validateWith = PositiveInteger.class, description = "Number of threads used for getting content with -db-fetch and -db-fetch-end. Should not be bound by actual processor core count, as mostly threads sit idle, waiting for an answer from a remote host or waiting behind another thread to finish communicating with the same host.")
	int threads = 8;

	@Parameter(names = { "-db" }, description = "Get publications, webpages and docs from the given database")
	String db = null;

	@Parameter(names = { "-fetch" }, description = "Fetch publications, webpages and docs from the Internet. All entries for which some fetchException happens are fetched again in the end (this is done only once).")
	boolean fetch = false;

	@Parameter(names = { "-fetch-put" }, description = "Fetch publications, webpages and docs from the Internet and put each entry in the given database right after it has been fetched, ignoring any filters and overwriting any existing entries with equal IDs/URLs. All entries for which some fetchException happens are fetched and put to the database again in the end (this is done only once).")
	String fetchPut = null;

	@Parameter(names = { "-db-fetch" }, description = "First, get an entry from the given database (if found), then fetch the entry (if the entry can be fetched), then put the entry back to the database while ignoring any filters (if the entry was updated). All entries which have the fetchException set are got again in the end (this is done only once). This operation is multithreaded (in contrast to -fetch and -fetch-put), with --threads number of threads, thus it should be preferred for larger amounts of content.")
	String dbFetch = null;

	@Parameter(names = { "-db-fetch-end" }, description = "Like -db-fetch, except no content is kept in memory (saving back to the given database still happens), thus no further processing down the pipeline is possible. This is useful for avoiding large memory usage if only fetching and saving of content to the database is to be done and no further operations on content (like outputting it) are required.")
	String dbFetchEnd = null;

	@Parameter(names = { "-fetch-time-more" }, converter = ISO8601Converter.class, description = "Only keep publications, webpages and docs whose fetchTime is more than or equal to the given time")
	Long fetchTimeMore = null;

	@Parameter(names = { "-fetch-time-less" }, converter = ISO8601Converter.class, description = "Only keep publications, webpages and docs whose fetchTime is less than or equal to the given time")
	Long fetchTimeLess = null;

	@Parameter(names = { "-retry-counter" }, variableArity = true, validateWith = PositiveInteger.class, description = "Only keep publications, webpages and docs whose retryCounter is equal to one of given counts")
	List<Integer> retryCounter = null;

	@Parameter(names = { "-not-retry-counter" }, variableArity = true, validateWith = PositiveInteger.class, description = "Only keep publications, webpages and docs whose retryCounter is not equal to any of given counts")
	List<Integer> notRetryCounter = null;

	@Parameter(names = { "-retry-counter-more" }, validateWith = PositiveInteger.class, description = "Only keep publications, webpages and docs whose retryCounter is more than the given count")
	Integer retryCounterMore = null;

	@Parameter(names = { "-retry-counter-less" }, validateWith = PositiveInteger.class, description = "Only keep publications, webpages and docs whose retryCounter is less than the given count")
	Integer retryCounterLess = null;

	@Parameter(names = { "-fetch-exception" }, description = "Only keep publications, webpages and docs with a fetchException")
	boolean fetchException = false;

	@Parameter(names = { "-not-fetch-exception" }, description = "Only keep publications, webpages and docs without a fetchException")
	boolean notFetchException = false;

	@Parameter(names = { "-empty" }, description = "Only keep empty publications, empty webpages and empty docs")
	boolean empty = false;

	@Parameter(names = { "-not-empty" }, description = "Only keep non-empty publications, non-empty webpages and non-empty docs")
	boolean notEmpty = false;

	@Parameter(names = { "-usable" }, description = "Only keep usable publications, usable webpages and usable docs")
	boolean usable = false;

	@Parameter(names = { "-not-usable" }, description = "Only keep non-usable publications, non-usable webpages and non-usable docs")
	boolean notUsable = false;

	@Parameter(names = { "-final" }, description = "Only keep final publications, final webpages and final docs")
	boolean isFinal = false;

	@Parameter(names = { "-not-final" }, description = "Only keep non-final publications, non-final webpages and non-final docs")
	boolean notIsFinal = false;

	@Parameter(names = { "-totally-final" }, description = "Only keep publications whose content is totally final")
	boolean totallyFinal = false;

	@Parameter(names = { "-not-totally-final" }, description = "Only keep publications whose content is not totally final")
	boolean notTotallyFinal = false;

	@Parameter(names = { "-broken" }, description = "Only keep webpages and docs that are broken")
	boolean broken = false;

	@Parameter(names = { "-not-broken" }, description = "Only keep webpages and docs that are not broken")
	boolean notBroken = false;

	@Parameter(names = { "-part-empty" }, variableArity = true, description = "Only keep publications with specified parts being empty")
	List<PublicationPartName> partEmpty = null;

	@Parameter(names = { "-not-part-empty" }, variableArity = true, description = "Only keep publications with specified parts not being empty")
	List<PublicationPartName> notPartEmpty = null;

	@Parameter(names = { "-part-usable" }, variableArity = true, description = "Only keep publications with specified parts being usable")
	List<PublicationPartName> partUsable = null;

	@Parameter(names = { "-not-part-usable" }, variableArity = true, description = "Only keep publications with specified parts not being usable")
	List<PublicationPartName> notPartUsable = null;

	@Parameter(names = { "-part-final" }, variableArity = true, description = "Only keep publications with specified parts being final")
	List<PublicationPartName> partFinal = null;

	@Parameter(names = { "-not-part-final" }, variableArity = true, description = "Only keep publications with specified parts not being final")
	List<PublicationPartName> notPartFinal = null;

	@Parameter(names = { "-part-content" }, description = "Only keep publications where the contents of all parts specified with -part-content-part have a match with the given regular expression")
	String partContent = null;

	@Parameter(names = { "-part-content-part" }, variableArity = true, description = "See -part-content")
	List<PublicationPartName> partContentPart = null;

	@Parameter(names = { "-not-part-content" }, description = "Only keep publications where the contents of all parts specified with -not-part-content-part do not have a match with the given regular expression")
	String notPartContent = null;

	@Parameter(names = { "-not-part-content-part" }, variableArity = true, description = "See -not-part-content")
	List<PublicationPartName> notPartContentPart = null;

	@Parameter(names = { "-part-size" }, variableArity = true, validateWith = PositiveInteger.class, description = "Only keep publications where the sizes of all parts specified with -part-size-part are equal to any of given sizes")
	List<Integer> partSize = null;

	@Parameter(names = { "-part-size-part" }, variableArity = true, description = "See -part-size")
	List<PublicationPartName> partSizePart = null;

	@Parameter(names = { "-not-part-size" }, variableArity = true, validateWith = PositiveInteger.class, description = "Only keep publications where the sizes of all parts specified with -not-part-size-part are not equal to any of given sizes")
	List<Integer> notPartSize = null;

	@Parameter(names = { "-not-part-size-part" }, variableArity = true, description = "See -not-part-size")
	List<PublicationPartName> notPartSizePart = null;

	@Parameter(names = { "-part-size-more" }, validateWith = PositiveInteger.class, description = "Only keep publications where the sizes of all parts specified with -part-size-more-part are more than the given size")
	Integer partSizeMore = null;

	@Parameter(names = { "-part-size-more-part" }, variableArity = true, description = "See -part-size-more")
	List<PublicationPartName> partSizeMorePart = null;

	@Parameter(names = { "-part-size-less" }, validateWith = PositiveInteger.class, description = "Only keep publications where the sizes of all parts specified with -part-size-less-part are less than the given size")
	Integer partSizeLess = null;

	@Parameter(names = { "-part-size-less-part" }, variableArity = true, description = "See -part-size-less")
	List<PublicationPartName> partSizeLessPart = null;

	@Parameter(names = { "-part-type" }, variableArity = true, description = "Only keep publications where the types of all parts specified with -part-type-part are equal to any of given types")
	List<PublicationPartType> partType = null;

	@Parameter(names = { "-part-type-part" }, variableArity = true, description = "See -part-type")
	List<PublicationPartName> partTypePart = null;

	@Parameter(names = { "-not-part-type" }, variableArity = true, description = "Only keep publications where the types of all parts specified with -not-part-type-part are not equal to any of given types")
	List<PublicationPartType> notPartType = null;

	@Parameter(names = { "-not-part-type-part" }, variableArity = true, description = "See -not-part-type")
	List<PublicationPartName> notPartTypePart = null;

	@Parameter(names = { "-part-type-more" }, description = "Only keep publications where the types of all parts specified with -part-type-more-type are better than the given type")
	PublicationPartType partTypeMore = null;

	@Parameter(names = { "-part-type-more-part" }, variableArity = true, description = "See -part-type-more")
	List<PublicationPartName> partTypeMorePart = null;

	@Parameter(names = { "-part-type-less" }, description = "Only keep publications where the types of all parts specified with -part-type-less-type are lesser than the given type")
	PublicationPartType partTypeLess = null;

	@Parameter(names = { "-part-type-less-part" }, variableArity = true, description = "See -part-type-less")
	List<PublicationPartName> partTypeLessPart = null;

	@Parameter(names = { "-part-type-final" }, description = "Only keep publications where the types of all parts specified with -part-type-final are of final type")
	boolean partTypeFinal = false;

	@Parameter(names = { "-part-type-final-part" }, variableArity = true, description = "See -part-type-final")
	List<PublicationPartName> partTypeFinalPart = null;

	@Parameter(names = { "-not-part-type-final" }, description = "Only keep publications where the types of all parts specified with -not-part-type-final are not of final type")
	boolean notPartTypeFinal = false;

	@Parameter(names = { "-not-part-type-final-part" }, variableArity = true, description = "See -not-part-type-final")
	List<PublicationPartName> notPartTypeFinalPart = null;

	@Parameter(names = { "-part-type-pdf" }, description = "Only keep publications where the types of all parts specified with -part-type-pdf-part are of PDF type")
	boolean partTypePdf = false;

	@Parameter(names = { "-part-type-pdf-part" }, variableArity = true, description = "See -part-type-pdf")
	List<PublicationPartName> partTypePdfPart = null;

	@Parameter(names = { "-not-part-type-pdf" }, description = "Only keep publications where the types of all parts specified with -not-part-type-pdf-part are not of PDF type")
	boolean notPartTypePdf = false;

	@Parameter(names = { "-not-part-type-pdf-part" }, variableArity = true, description = "See -not-part-type-pdf")
	List<PublicationPartName> notPartTypePdfPart = null;

	@Parameter(names = { "-part-url" }, description = "Only keep publications where the URLs of all parts specified with -part-url-part have a match with the given regular expression")
	String partUrl = null;

	@Parameter(names = { "-part-url-part" }, variableArity = true, description = "See -part-url")
	List<PublicationPartName> partUrlPart = null;

	@Parameter(names = { "-not-part-url" }, description = "Only keep publications where the URLs of all parts specified with -not-part-url-part do not have a match with the given regular expression")
	String notPartUrl = null;

	@Parameter(names = { "-not-part-url-part" }, variableArity = true, description = "See -not-part-url")
	List<PublicationPartName> notPartUrlPart = null;

	@Parameter(names = { "-part-url-host" }, variableArity = true, description = "Only keep publications where the URL host parts of all parts specified with -part-url-host-part are present in the given list of strings (comparison is done case-insensitively and \"www.\" is removed)")
	List<String> partUrlHost = null;

	@Parameter(names = { "-part-url-host-part" }, variableArity = true, description = "See -part-url-host")
	List<PublicationPartName> partUrlHostPart = null;

	@Parameter(names = { "-not-part-url-host" }, variableArity = true, description = "Only keep publications where the URL host parts of all parts specified with -not-part-url-host-part are not present in the given list of strings (comparison is done case-insensitively and \"www.\" is removed)")
	List<String> notPartUrlHost = null;

	@Parameter(names = { "-not-part-url-host-part" }, variableArity = true, description = "See -not-part-url-host")
	List<PublicationPartName> notPartUrlHostPart = null;

	@Parameter(names = { "-part-time-more" }, converter = ISO8601Converter.class, description = "Only keep publications where the timestamps of all parts specified with -part-time-more-part are more than or equal to the given time")
	Long partTimeMore = null;

	@Parameter(names = { "-part-time-more-part" }, variableArity = true, description = "See -part-time-more")
	List<PublicationPartName> partTimeMorePart = null;

	@Parameter(names = { "-part-time-less" }, converter = ISO8601Converter.class, description = "Only keep publications where the timestamps of all parts specified with -part-time-less-part are less than or equal to the given time")
	Long partTimeLess = null;

	@Parameter(names = { "-part-time-less-part" }, variableArity = true, description = "See -part-time-less")
	List<PublicationPartName> partTimeLessPart = null;

	@Parameter(names = { "-oa" }, description = "Only keep publications that are Open Access")
	boolean oa = false;

	@Parameter(names = { "-not-oa" }, description = "Only keep publications that are not Open Access")
	boolean notOa = false;

	@Parameter(names = { "-journal-title" }, description = "Only keep publications whose journal title has a match with the given regular expression")
	String journalTitle = null;

	@Parameter(names = { "-not-journal-title" }, description = "Only keep publications whose journal title does not have a match with the given regular expression")
	String notJournalTitle = null;

	@Parameter(names = { "-journal-title-empty" }, description = "Only keep publications whose journal title is empty")
	boolean journalTitleEmpty = false;

	@Parameter(names = { "-not-journal-title-empty" }, description = "Only keep publications whose journal title is not empty")
	boolean notJournalTitleEmpty = false;

	@Parameter(names = { "-pub-date-more" }, converter = ISO8601Converter.class, description = "Only keep publications whose publication date is more than or equal to given time (add \"T00:00:00Z\" to the end to get an ISO-8601 time from a date)")
	Long pubDateMore = null;

	@Parameter(names = { "-pub-date-less" }, converter = ISO8601Converter.class, description = "Only keep publications whose publication date is less than or equal to given time (add \"T00:00:00Z\" to the end to get an ISO-8601 time from a date)")
	Long pubDateLess = null;

	@Parameter(names = { "-citations-count" }, variableArity = true, description = "Only keep publications whose citations count is equal to one of given counts")
	List<Integer> citationsCount = null;

	@Parameter(names = { "-not-citations-count" }, variableArity = true, description = "Only keep publications whose citations count is not equal to any of given counts")
	List<Integer> notCitationsCount = null;

	@Parameter(names = { "-citations-count-more" }, description = "Only keep publications whose citations count is more than the given count")
	Integer citationsCountMore = null;

	@Parameter(names = { "-citations-count-less" }, description = "Only keep publications whose citations count is less than the given count")
	Integer citationsCountLess = null;

	@Parameter(names = { "-citations-timestamp-more" }, converter = ISO8601Converter.class, description = "Only keep publications whose citations count last update timestamp is more than or equal to the given time")
	Long citationsTimestampMore = null;

	@Parameter(names = { "-citations-timestamp-less" }, converter = ISO8601Converter.class, description = "Only keep publications whose citations count last update timestamp is less than or equal to the given time")
	Long citationsTimestampLess = null;

	@Parameter(names = { "-corresp-author-name" }, description = "Only keep publications with a corresponding author name having a match with the given regular expression")
	String correspAuthorName = null;

	@Parameter(names = { "-not-corresp-author-name" }, description = "Only keep publications with no corresponding authors names having a match with the given regular expression")
	String notCorrespAuthorName = null;

	@Parameter(names = { "-corresp-author-name-empty" }, description = "Only keep publications whose corresponding authors names are empty")
	boolean correspAuthorNameEmpty = false;

	@Parameter(names = { "-not-corresp-author-name-empty" }, description = "Only keep publications with a corresponding author name that is not empty")
	boolean notCorrespAuthorNameEmpty = false;

	@Parameter(names = { "-corresp-author-orcid" }, description = "Only keep publications with a corresponding author ORCID iD having a match with the given regular expression")
	String correspAuthorOrcid = null;

	@Parameter(names = { "-not-corresp-author-orcid" }, description = "Only keep publications with no corresponding authors ORCID iDs having a match with the given regular expression")
	String notCorrespAuthorOrcid = null;

	@Parameter(names = { "-corresp-author-orcid-empty" }, description = "Only keep publications whose corresponding authors ORCID iDs are empty")
	boolean correspAuthorOrcidEmpty = false;

	@Parameter(names = { "-not-corresp-author-orcid-empty" }, description = "Only keep publications with a corresponding author ORCID iD that is not empty")
	boolean notCorrespAuthorOrcidEmpty = false;

	@Parameter(names = { "-corresp-author-email" }, description = "Only keep publications with a corresponding author e-mail address having a match with the given regular expression")
	String correspAuthorEmail = null;

	@Parameter(names = { "-not-corresp-author-email" }, description = "Only keep publications with no corresponding authors e-mail addresses having a match with the given regular expression")
	String notCorrespAuthorEmail = null;

	@Parameter(names = { "-corresp-author-email-empty" }, description = "Only keep publications whose corresponding authors e-mail addresses are empty")
	boolean correspAuthorEmailEmpty = false;

	@Parameter(names = { "-not-corresp-author-email-empty" }, description = "Only keep publications with a corresponding author e-mail address that is not empty")
	boolean notCorrespAuthorEmailEmpty = false;

	@Parameter(names = { "-corresp-author-phone" }, description = "Only keep publications with a corresponding author telephone number having a match with the given regular expression")
	String correspAuthorPhone = null;

	@Parameter(names = { "-not-corresp-author-phone" }, description = "Only keep publications with no corresponding authors telephone numbers having a match with the given regular expression")
	String notCorrespAuthorPhone = null;

	@Parameter(names = { "-corresp-author-phone-empty" }, description = "Only keep publications whose corresponding authors telephone numbers are empty")
	boolean correspAuthorPhoneEmpty = false;

	@Parameter(names = { "-not-corresp-author-phone-empty" }, description = "Only keep publications with a corresponding author telephone number that is not empty")
	boolean notCorrespAuthorPhoneEmpty = false;

	@Parameter(names = { "-corresp-author-uri" }, description = "Only keep publications with a corresponding author web page address having a match with the given regular expression")
	String correspAuthorUri = null;

	@Parameter(names = { "-not-corresp-author-uri" }, description = "Only keep publications with no corresponding authors web page addresses having a match with the given regular expression")
	String notCorrespAuthorUri = null;

	@Parameter(names = { "-corresp-author-uri-empty" }, description = "Only keep publications whose corresponding authors web page addresses are empty")
	boolean correspAuthorUriEmpty = false;

	@Parameter(names = { "-not-corresp-author-uri-empty" }, description = "Only keep publications with a corresponding author web page address that is not empty")
	boolean notCorrespAuthorUriEmpty = false;

	@Parameter(names = { "-corresp-author-size" }, variableArity = true, validateWith = PositiveInteger.class, description = "Only keep publications whose corresponding authors size is equal to one of given sizes")
	List<Integer> correspAuthorSize = null;

	@Parameter(names = { "-not-corresp-author-size" }, variableArity = true, validateWith = PositiveInteger.class, description = "Only keep publications whose corresponding authors size is not equal to any of given sizes")
	List<Integer> notCorrespAuthorSize = null;

	@Parameter(names = { "-corresp-author-size-more" }, validateWith = PositiveInteger.class, description = "Only keep publications whose corresponding authors size is more than given size")
	Integer correspAuthorSizeMore = null;

	@Parameter(names = { "-corresp-author-size-less" }, validateWith = PositiveInteger.class, description = "Only keep publications whose corresponding authors size is less than given size")
	Integer correspAuthorSizeLess = null;

	@Parameter(names = { "-visited" }, description = "Only keep publications with a visited site whose URL has a match with the given regular expression")
	String visited = null;

	@Parameter(names = { "-not-visited" }, description = "Only keep publications with no visited sites whose URL has a match with the given regular expression")
	String notVisited = null;

	@Parameter(names = { "-visited-host" }, variableArity = true, description = "Only keep publications with a visited site whose URL host part is present in the given list of strings (comparison is done case-insensitively and \"www.\" is removed)")
	List<String> visitedHost = null;

	@Parameter(names = { "-not-visited-host" }, variableArity = true, description = "Only keep publications with no visited sites whose URL host part is present in the given list of strings (comparison is done case-insensitively and \"www.\" is removed)")
	List<String> notVisitedHost = null;

	@Parameter(names = { "-visited-type" }, variableArity = true, description = "Only keep publications with a visited site of type equal to one of given types")
	List<PublicationPartType> visitedType = null;

	@Parameter(names = { "-not-visited-type" }, variableArity = true, description = "Only keep publications with no visited sites of type equal to any of given types")
	List<PublicationPartType> notVisitedType = null;

	@Parameter(names = { "-visited-type-more" }, description = "Only keep publications with a visited site of better type than the given type")
	PublicationPartType visitedTypeMore = null;

	@Parameter(names = { "-visited-type-less" }, description = "Only keep publications with a visited site of lesser type than the given type")
	PublicationPartType visitedTypeLess = null;

	@Parameter(names = { "-visited-type-final" }, description = "Only keep publications with a visited site of final type")
	boolean visitedTypeFinal = false;

	@Parameter(names = { "-not-visited-type-final" }, description = "Only keep publications with no visited sites of final type")
	boolean notVisitedTypeFinal = false;

	@Parameter(names = { "-visited-type-pdf" }, description = "Only keep publications with a visited site of PDF type")
	boolean visitedTypePdf = false;

	@Parameter(names = { "-not-visited-type-pdf" }, description = "Only keep publications with no visited sites of PDF type")
	boolean notVisitedTypePdf = false;

	@Parameter(names = { "-visited-from" }, description = "Only keep publications with a visited site whose provenance URL has a match with the given regular expression")
	String visitedFrom = null;

	@Parameter(names = { "-not-visited-from" }, description = "Only keep publications with no visited sites whose provenance URL has a match with the given regular expression")
	String notVisitedFrom = null;

	@Parameter(names = { "-visited-from-host" }, variableArity = true, description = "Only keep publications with a visited site whose provenance URL host part is present in the given list of strings (comparison is done case-insensitively and \"www.\" is removed)")
	List<String> visitedFromHost = null;

	@Parameter(names = { "-not-visited-from-host" }, variableArity = true, description = "Only keep publications with no visited sites whose provenance URL host part is present in the given list of strings (comparison is done case-insensitively and \"www.\" is removed)")
	List<String> notVisitedFromHost = null;

	@Parameter(names = { "-visited-time-more" }, converter = ISO8601Converter.class, description = "Only keep publications with a visited site whose visit time is more than or equal to the given time")
	Long visitedTimeMore = null;

	@Parameter(names = { "-visited-time-less" }, converter = ISO8601Converter.class, description = "Only keep publications with a visited site whose visit time is less than or equal to the given time")
	Long visitedTimeLess = null;

	@Parameter(names = { "-visited-size" }, variableArity = true, validateWith = PositiveInteger.class, description = "Only keep publications whose visited sites size is equal to one of given sizes")
	List<Integer> visitedSize = null;

	@Parameter(names = { "-not-visited-size" }, variableArity = true, validateWith = PositiveInteger.class, description = "Only keep publications whose visited sites size is not equal to any of given sizes")
	List<Integer> notVisitedSize = null;

	@Parameter(names = { "-visited-size-more" }, validateWith = PositiveInteger.class, description = "Only keep publications whose visited sites size is more than the given size")
	Integer visitedSizeMore = null;

	@Parameter(names = { "-visited-size-less" }, validateWith = PositiveInteger.class, description = "Only keep publications whose visited sites size is less than the given size")
	Integer visitedSizeLess = null;

	@Parameter(names = { "-start-url" }, description = "Only keep webpages and docs whose start URL has a match with the given regular expression")
	String startUrl = null;

	@Parameter(names = { "-not-start-url" }, description = "Only keep webpages and docs whose start URL does not have a match with the given regular expression")
	String notStartUrl = null;

	@Parameter(names = { "-start-url-host" }, variableArity = true, description = "Only keep webpages and docs whose start URL host part is present in the given list of strings (comparison is done case-insensitively and \"www.\" is removed)")
	List<String> startUrlHost = null;

	@Parameter(names = { "-not-start-url-host" }, variableArity = true, description = "Only keep webpages and docs whose start URL host part is not present in the given list of strings (comparison is done case-insensitively and \"www.\" is removed)")
	List<String> notStartUrlHost = null;

	@Parameter(names = { "-final-url" }, description = "Only keep webpages and docs whose final URL has a match with the given regular expression")
	String finalUrl = null;

	@Parameter(names = { "-not-final-url" }, description = "Only keep webpages and docs whose final URL does not have a match with the given regular expression")
	String notFinalUrl = null;

	@Parameter(names = { "-final-url-host" }, variableArity = true, description = "Only keep webpages and docs whose final URL host part is present in the given list of strings (comparison is done case-insensitively and \"www.\" is removed)")
	List<String> finalUrlHost = null;

	@Parameter(names = { "-not-final-url-host" }, variableArity = true, description = "Only keep webpages and docs whose final URL host part is not present in the given list of strings (comparison is done case-insensitively and \"www.\" is removed)")
	List<String> notFinalUrlHost = null;

	@Parameter(names = { "-final-url-empty" }, description = "Only keep webpages and docs whose final URL is empty")
	boolean finalUrlEmpty = false;

	@Parameter(names = { "-not-final-url-empty" }, description = "Only keep webpages and docs whose final URL is not empty")
	boolean notFinalUrlEmpty = false;

	@Parameter(names = { "-content-type" }, description = "Only keep webpages and docs whose HTTP Content-Type has a match with the given regular expression")
	String contentType = null;

	@Parameter(names = { "-not-content-type" }, description = "Only keep webpages and docs whose HTTP Content-Type does not have a match with the given regular expression")
	String notContentType = null;

	@Parameter(names = { "-content-type-empty" }, description = "Only keep webpages and docs whose HTTP Content-Type is empty")
	boolean contentTypeEmpty = false;

	@Parameter(names = { "-not-content-type-empty" }, description = "Only keep webpages and docs whose HTTP Content-Type is not empty")
	boolean notContentTypeEmpty = false;

	@Parameter(names = { "-status-code" }, variableArity = true, description = "Only keep webpages and docs whose HTTP status code is equal to one of given codes")
	List<Integer> statusCode = null;

	@Parameter(names = { "-not-status-code" }, variableArity = true, description = "Only keep webpages and docs whose HTTP status code is not equal to any of given codes")
	List<Integer> notStatusCode = null;

	@Parameter(names = { "-status-code-more" }, description = "Only keep webpages and docs whose HTTP status code is bigger than the given code")
	Integer statusCodeMore = null;

	@Parameter(names = { "-status-code-less" }, description = "Only keep webpages and docs whose HTTP status code is smaller than the given code")
	Integer statusCodeLess = null;

	@Parameter(names = { "-title" }, description = "Only keep webpages and docs whose page title has a match with the given regular expression")
	String title = null;

	@Parameter(names = { "-not-title" }, description = "Only keep webpages and docs whose page title does not have a match with the given regular expression")
	String notTitle = null;

	@Parameter(names = { "-title-size" }, variableArity = true, validateWith = PositiveInteger.class, description = "Only keep webpages and docs whose title length is equal to one of given lengths")
	List<Integer> titleSize = null;

	@Parameter(names = { "-not-title-size" }, variableArity = true, validateWith = PositiveInteger.class, description = "Only keep webpages and docs whose title length is not equal to any of given lengths")
	List<Integer> notTitleSize = null;

	@Parameter(names = { "-title-size-more" }, variableArity = true, validateWith = PositiveInteger.class, description = "Only keep webpages and docs whose title length is more than the given length")
	Integer titleSizeMore = null;

	@Parameter(names = { "-title-size-less" }, variableArity = true, validateWith = PositiveInteger.class, description = "Only keep webpages and docs whose title length is less than the given length")
	Integer titleSizeLess = null;

	@Parameter(names = { "-content" }, description = "Only keep webpages and docs whose content has a match with the given regular expression")
	String content = null;

	@Parameter(names = { "-not-content" }, description = "Only keep webpages and docs whose content does not have a match with the given regular expression")
	String notContent = null;

	@Parameter(names = { "-content-size" }, variableArity = true, validateWith = PositiveInteger.class, description = "Only keep webpages and docs whose content length is equal to one of given lengths")
	List<Integer> contentSize = null;

	@Parameter(names = { "-not-content-size" }, variableArity = true, validateWith = PositiveInteger.class, description = "Only keep webpages and docs whose content length is not equal to any of given lengths")
	List<Integer> notContentSize = null;

	@Parameter(names = { "-content-size-more" }, variableArity = true, validateWith = PositiveInteger.class, description = "Only keep webpages and docs whose content length is more than the given length")
	Integer contentSizeMore = null;

	@Parameter(names = { "-content-size-less" }, variableArity = true, validateWith = PositiveInteger.class, description = "Only keep webpages and docs whose content length is less than the given length")
	Integer contentSizeLess = null;

	@Parameter(names = { "-content-time-more" }, converter = ISO8601Converter.class, description = "Only keep webpages and docs whose content time is more than or equal to the given time")
	Long contentTimeMore = null;

	@Parameter(names = { "-content-time-less" }, converter = ISO8601Converter.class, description = "Only keep webpages and docs whose content time is less than or equal to the given time")
	Long contentTimeLess = null;

	@Parameter(names = { "-license" }, description = "Only keep webpages and docs whose software license has a match with the given regular expression")
	String license = null;

	@Parameter(names = { "-not-license" }, description = "Only keep webpages and docs whose software license does not have a match with the given regular expression")
	String notLicense = null;

	@Parameter(names = { "-license-empty" }, description = "Only keep webpages and docs whose software license is empty")
	boolean licenseEmpty = false;

	@Parameter(names = { "-not-license-empty" }, description = "Only keep webpages and docs whose software license is not empty")
	boolean notLicenseEmpty = false;

	@Parameter(names = { "-language" }, description = "Only keep webpages and docs whose programming language has a match with the given regular expression")
	String language = null;

	@Parameter(names = { "-not-language" }, description = "Only keep webpages and docs whose programming language does not have a match with the given regular expression")
	String notLanguage = null;

	@Parameter(names = { "-language-empty" }, description = "Only keep webpages and docs whose programming language is empty")
	boolean languageEmpty = false;

	@Parameter(names = { "-not-language-empty" }, description = "Only keep webpages and docs whose programming language is not empty")
	boolean notLanguageEmpty = false;

	@Parameter(names = { "-grep" }, description = "Only keep publications, webpages and docs whose whole content (as output using --plain) has a match with the given regular expression")
	String grep = null;

	@Parameter(names = { "-not-grep" }, description = "Only keep publications, webpages and docs whose whole content (as output using --plain) does not have a match with the given regular expression")
	String notGrep = null;

	@Parameter(names = { "-has-scrape" }, description = "Only keep webpages and docs that have scraping rules (based on final URL)")
	boolean hasScrape = false;

	@Parameter(names = { "-not-has-scrape" }, description = "Only keep webpages and docs that do not have scraping rules (based on final URL)")
	boolean notHasScrape = false;

	@Parameter(names = { "-asc" }, description = "Sort publications, webpages and docs by their ID/URL in ascending order")
	boolean asc = false;

	@Parameter(names = { "-desc" }, description = "Sort publications, webpages and docs by their ID/URL in descending order")
	boolean desc = false;

	@Parameter(names = { "-asc-time" }, description = "Sort publications, webpages and docs by their fetchTime in ascending order")
	boolean ascTime = false;

	@Parameter(names = { "-desc-time" }, description = "Sort publications, webpages and docs by their fetchTime in descending order")
	boolean descTime = false;

	@Parameter(names = { "-head" }, validateWith = PositiveInteger.class, description = "Only keep the first given number of publications, webpages and docs (same for top hosts from publications, webpages and docs)")
	Integer head = null;

	@Parameter(names = { "-tail" }, validateWith = PositiveInteger.class, description = "Only keep the last given number of publications, webpages and docs (same for top hosts from publications, webpages and docs)")
	Integer tail = null;

	@Parameter(names = { "-update-citations-count" }, description = "Fetch and update the citations count and citations count last update timestamp of all publications resulting from the pipeline and put successfully updated publications to the given database")
	String updateCitationsCount = null;

	@Parameter(names = { "-put" }, description = "Put all publications, webpages and docs resulting from the pipeline to the given database, overwriting any existing entries that have equal IDs/URLs")
	String put = null;

	@Parameter(names = { "-remove" }, description = "From the given database, remove all publications, webpages and docs with IDs corresponding to IDs of publications, webpages and docs resulting from the pipeline")
	String remove = null;

	@Parameter(names = { "--plain" }, description = "If specified, then any potential metadata will be omitted from the output")
	boolean plain = false;

	@Parameter(names = { "--format" }, description = "Can choose between plain text output format (\"text\"), HTML format (\"html\") and JSON format (\"json\")")
	Format format = Format.text;

	@Parameter(names = { "--out-part" }, variableArity = true, description = "If specified, then only the specified publication parts will be output (webpages and docs are not affected). Independent from the --fetch-part parameter.")
	List<PublicationPartName> outPart = null;

	@Parameter(names = { "-out" }, description = "Output publications (or publication parts specified by --out-part), webpages and docs to stdout in the format specified by the output modifiers --plain and --format")
	boolean out = false;

	@Parameter(names = { "-txt-pub" }, description = "Output publications (or publication parts specified by --out-part) to the given file in the format specified by the output modifiers --plain and --format")
	String txtPub = null;

	@Parameter(names = { "-txt-web" }, description = "Output webpages to the given file in the format specified by the output modifiers --plain and --format")
	String txtWeb = null;

	@Parameter(names = { "-txt-doc" }, description = "Output docs to the given file in the format specified by the output modifiers --plain and --format")
	String txtDoc = null;

	@Parameter(names = { "-count" }, description = "Output count numbers for publications, webpages and docs to stdout")
	boolean count = false;

	@Parameter(names = { "-out-top-hosts" }, description = "Output all host parts of URLs of visited sites of publications, of URLs of webpages and of URLs of docs to stdout, starting from most common and including count number")
	boolean outTopHosts = false;

	@Parameter(names = { "-txt-top-hosts-pub" }, description = "Output all host parts of URLs of visited sites of publications to the given file, starting from the most common and including count numbers")
	String txtTopHostsPub = null;

	@Parameter(names = { "-txt-top-hosts-web" }, description = "Output all host parts of URLs of webpages to the given file, starting from the most common and including count numbers")
	String txtTopHostsWeb = null;

	@Parameter(names = { "-txt-top-hosts-doc" }, description = "Output all host parts of URLs of docs to the given file, starting from the most common and including count numbers")
	String txtTopHostsDoc = null;

	@Parameter(names = { "-count-top-hosts" }, description = "Output number of different host parts of URLs of visited sites of publications, of URLs of webpages and of URLs of docs to stdout")
	boolean countTopHosts = false;

	@Parameter(names = { "-part-table" }, description = "Output a PublicationPartType vs PublicationPartName table in CSV format to stdout, i.e. how many publications have content for the given publication part fetched from the given resource type")
	boolean partTable = false;
}
