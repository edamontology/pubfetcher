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
import com.beust.jcommander.validators.PositiveInteger;

import org.edamontology.pubfetcher.core.db.publication.PublicationPartName;
import org.edamontology.pubfetcher.core.db.publication.PublicationPartType;
import org.edamontology.pubfetcher.core.fetching.FetcherTestArgs;

public class PubFetcherArgs {

	@ParametersDelegate
	FetcherTestArgs fetcherTestArgs = new FetcherTestArgs();

	@Parameter(names = { "--db-init" }, description = "TODO")
	String dbInit = null;

	@Parameter(names = { "--db-commit" }, description = "TODO")
	String dbCommit = null;

	@Parameter(names = { "--db-compact" }, description = "TODO")
	String dbCompact = null;

	@Parameter(names = { "--db-publications-size" }, description = "TODO")
	String dbPublicationsSize = null;

	@Parameter(names = { "--db-webpages-size" }, description = "TODO")
	String dbWebpagesSize = null;

	@Parameter(names = { "--db-docs-size" }, description = "TODO")
	String dbDocsSize = null;

	@Parameter(names = { "--db-publications-map" }, description = "TODO")
	String dbPublicationsMap = null;

	@Parameter(names = { "--db-publications-map-reverse" }, description = "TODO")
	String dbPublicationsMapReverse = null;

	@Parameter(names = { "--fetch-document" }, description = "TODO")
	String fetchDocument = null;

	@Parameter(names = { "--fetch-document-javascript" }, description = "TODO")
	String fetchDocumentJavascript = null;

	@Parameter(names = { "--fetch-webpage-selector" }, arity = 4, description = "TODO")
	List<String> fetchWebpageSelector = null;

	@Parameter(names = { "--fetch-webpage-selector-html" }, arity = 4, description = "TODO")
	List<String> fetchWebpageSelectorHtml = null;

	@Parameter(names = { "--scrape-site" }, description = "TODO")
	String scrapeSite = null;

	@Parameter(names = { "--scrape-selector" }, arity = 2, description = "TODO")
	List<String> scrapeSelector = null;

	@Parameter(names = { "--scrape-webpage" }, description = "TODO")
	String scrapeWebpage = null;

	@Parameter(names = { "--scrape-javascript" }, description = "TODO")
	String scrapeJavascript = null;

	@Parameter(names = { "--is-pmid" }, description = "TODO")
	String isPmid = null;

	@Parameter(names = { "--is-pmcid" }, description = "TODO")
	String isPmcid = null;

	@Parameter(names = { "--extract-pmcid" }, description = "TODO")
	String extractPmcid = null;

	@Parameter(names = { "--is-doi" }, description = "TODO")
	String isDoi = null;

	@Parameter(names = { "--normalise-doi" }, description = "TODO")
	String normaliseDoi = null;

	@Parameter(names = { "--extract-doi-registrant" }, description = "TODO")
	String extractDoiRegistrant = null;

	@Parameter(names = { "--escape-html" }, description = "TODO")
	String escapeHtml = null;

	@Parameter(names = { "--escape-html-attribute" }, description = "TODO")
	String escapeHtmlAttribute = null;

	@Parameter(names = { "--check-publication-id" }, description = "TODO")
	String checkPublicationId = null;

	@Parameter(names = { "--check-publication-ids" }, arity = 3, description = "TODO")
	List<String> checkPublicationIds = null;

	@Parameter(names = { "--check-url" }, description = "TODO")
	String checkUrl = null;

	@Parameter(names = { "-fetch-part" }, variableArity = true, description = "TODO")
	List<PublicationPartName> fetchPart = null;

	@Parameter(names = { "-not-fetch-part" }, variableArity = true, description = "TODO")
	List<PublicationPartName> notFetchPart = null;

	@Parameter(names = { "-pub-file" }, variableArity = true, description = "TODO")
	List<String> pubFile = null;

	@Parameter(names = { "-web-file" }, variableArity = true, description = "TODO")
	List<String> webFile = null;

	@Parameter(names = { "-doc-file" }, variableArity = true, description = "TODO")
	List<String> docFile = null;

	@Parameter(names = { "-pub" }, variableArity = true, description = "TODO")
	List<String> pub = null;

	@Parameter(names = { "-web" }, variableArity = true, description = "TODO")
	List<String> web = null;

	@Parameter(names = { "-doc" }, variableArity = true, description = "TODO")
	List<String> doc = null;

	@Parameter(names = { "-pub-db" }, variableArity = true, description = "TODO")
	List<String> pubDb = null;

	@Parameter(names = { "-web-db" }, variableArity = true, description = "TODO")
	List<String> webDb = null;

	@Parameter(names = { "-doc-db" }, variableArity = true, description = "TODO")
	List<String> docDb = null;

	@Parameter(names = { "-has-pmid" }, description = "TODO")
	boolean hasPmid = false;

	@Parameter(names = { "-not-has-pmid" }, description = "TODO")
	boolean notHasPmid = false;

	@Parameter(names = { "-pmid" }, description = "TODO")
	String pmid = null;

	@Parameter(names = { "-not-pmid" }, description = "TODO")
	String notPmid = null;

	@Parameter(names = { "-pmid-url" }, description = "TODO")
	String pmidUrl = null;

	@Parameter(names = { "-not-pmid-url" }, description = "TODO")
	String notPmidUrl = null;

	@Parameter(names = { "-has-pmcid" }, description = "TODO")
	boolean hasPmcid = false;

	@Parameter(names = { "-not-has-pmcid" }, description = "TODO")
	boolean notHasPmcid = false;

	@Parameter(names = { "-pmcid" }, description = "TODO")
	String pmcid = null;

	@Parameter(names = { "-not-pmcid" }, description = "TODO")
	String notPmcid = null;

	@Parameter(names = { "-pmcid-url" }, description = "TODO")
	String pmcidUrl = null;

	@Parameter(names = { "-not-pmcid-url" }, description = "TODO")
	String notPmcidUrl = null;

	@Parameter(names = { "-has-doi" }, description = "TODO")
	boolean hasDoi = false;

	@Parameter(names = { "-not-has-doi" }, description = "TODO")
	boolean notHasDoi = false;

	@Parameter(names = { "-doi" }, description = "TODO")
	String doi = null;

	@Parameter(names = { "-not-doi" }, description = "TODO")
	String notDoi = null;

	@Parameter(names = { "-doi-url" }, description = "TODO")
	String doiUrl = null;

	@Parameter(names = { "-not-doi-url" }, description = "TODO")
	String notDoiUrl = null;

	@Parameter(names = { "-doi-registrant" }, variableArity = true, description = "TODO")
	List<String> doiRegistrant = null;

	@Parameter(names = { "-not-doi-registrant" }, variableArity = true, description = "TODO")
	List<String> notDoiRegistrant = null;

	@Parameter(names = { "-url" }, description = "TODO")
	String url = null;

	@Parameter(names = { "-not-url" }, description = "TODO")
	String notUrl = null;

	@Parameter(names = { "-url-host" }, variableArity = true, description = "TODO")
	List<String> urlHost = null;

	@Parameter(names = { "-not-url-host" }, variableArity = true, description = "TODO")
	List<String> notUrlHost = null;

	@Parameter(names = { "-in-db" }, description = "TODO")
	String inDb = null;

	@Parameter(names = { "-not-in-db" }, description = "TODO")
	String notInDb = null;

	@Parameter(names = { "-asc-ids" }, description = "TODO")
	boolean ascIds = false;

	@Parameter(names = { "-desc-ids" }, description = "TODO")
	boolean descIds = false;

	@Parameter(names = { "-head-ids" }, validateWith = PositiveInteger.class, description = "TODO")
	Integer headIds = null;

	@Parameter(names = { "-tail-ids" }, validateWith = PositiveInteger.class, description = "TODO")
	Integer tailIds = null;

	@Parameter(names = { "-remove-ids" }, description = "TODO")
	String removeIds = null;

	@Parameter(names = { "-out-ids" }, description = "TODO")
	boolean outIds = false;

	@Parameter(names = { "-txt-ids-pub" }, description = "TODO")
	String txtIdsPub = null;

	@Parameter(names = { "-txt-ids-web" }, description = "TODO")
	String txtIdsWeb = null;

	@Parameter(names = { "-txt-ids-doc" }, description = "TODO")
	String txtIdsDoc = null;

	@Parameter(names = { "-count-ids" }, description = "TODO")
	boolean countIds = false;

	@Parameter(names = { "-pre-filter" }, description = "TODO")
	boolean preFilter = false;

	@Parameter(names = { "-limit" }, validateWith = PositiveInteger.class, description = "TODO")
	int limit = 0;

	@Parameter(names = { "-db" }, description = "TODO")
	String db = null;

	@Parameter(names = { "-fetch" }, description = "TODO")
	boolean fetch = false;

	@Parameter(names = { "-fetch-put" }, description = "TODO")
	String fetchPut = null;

	@Parameter(names = { "-db-fetch" }, description = "TODO")
	String dbFetch = null;

	@Parameter(names = { "-db-fetch-end" }, description = "TODO")
	String dbFetchEnd = null;

	@Parameter(names = { "-threads" }, validateWith = PositiveInteger.class, description = "TODO")
	int threads = 8;

	@Parameter(names = { "-fetch-time-more" }, converter = ISO8601Converter.class, description = "TODO more or equal")
	Long fetchTimeMore = null;

	@Parameter(names = { "-fetch-time-less" }, converter = ISO8601Converter.class, description = "TODO less or equal")
	Long fetchTimeLess = null;

	@Parameter(names = { "-retry-counter" }, variableArity = true, validateWith = PositiveInteger.class, description = "TODO")
	List<Integer> retryCounter = null;

	@Parameter(names = { "-not-retry-counter" }, variableArity = true, validateWith = PositiveInteger.class, description = "TODO")
	List<Integer> notRetryCounter = null;

	@Parameter(names = { "-retry-counter-more" }, validateWith = PositiveInteger.class, description = "TODO")
	Integer retryCounterMore = null;

	@Parameter(names = { "-retry-counter-less" }, validateWith = PositiveInteger.class, description = "TODO")
	Integer retryCounterLess = null;

	@Parameter(names = { "-fetch-exception" }, description = "TODO")
	boolean fetchException = false;

	@Parameter(names = { "-not-fetch-exception" }, description = "TODO")
	boolean notFetchException = false;

	@Parameter(names = { "-empty" }, description = "TODO")
	boolean empty = false;

	@Parameter(names = { "-not-empty" }, description = "TODO")
	boolean notEmpty = false;

	@Parameter(names = { "-usable" }, description = "TODO")
	boolean usable = false;

	@Parameter(names = { "-not-usable" }, description = "TODO")
	boolean notUsable = false;

	@Parameter(names = { "-final" }, description = "TODO")
	boolean isFinal = false;

	@Parameter(names = { "-not-final" }, description = "TODO")
	boolean notIsFinal = false;

	@Parameter(names = { "-totally-final" }, description = "TODO")
	boolean totallyFinal = false;

	@Parameter(names = { "-not-totally-final" }, description = "TODO")
	boolean notTotallyFinal = false;

	@Parameter(names = { "-broken" }, description = "TODO")
	boolean broken = false;

	@Parameter(names = { "-not-broken" }, description = "TODO")
	boolean notBroken = false;

	@Parameter(names = { "-part-empty" }, variableArity = true, description = "TODO")
	List<PublicationPartName> partEmpty = null;

	@Parameter(names = { "-not-part-empty" }, variableArity = true, description = "TODO")
	List<PublicationPartName> notPartEmpty = null;

	@Parameter(names = { "-part-usable" }, variableArity = true, description = "TODO")
	List<PublicationPartName> partUsable = null;

	@Parameter(names = { "-not-part-usable" }, variableArity = true, description = "TODO")
	List<PublicationPartName> notPartUsable = null;

	@Parameter(names = { "-part-final" }, variableArity = true, description = "TODO")
	List<PublicationPartName> partFinal = null;

	@Parameter(names = { "-not-part-final" }, variableArity = true, description = "TODO")
	List<PublicationPartName> notPartFinal = null;

	@Parameter(names = { "-part-content" }, description = "TODO")
	String partContent = null;

	@Parameter(names = { "-part-content-part" }, variableArity = true, description = "TODO")
	List<PublicationPartName> partContentPart = null;

	@Parameter(names = { "-not-part-content" }, description = "TODO")
	String notPartContent = null;

	@Parameter(names = { "-not-part-content-part" }, variableArity = true, description = "TODO")
	List<PublicationPartName> notPartContentPart = null;

	@Parameter(names = { "-part-size" }, variableArity = true, validateWith = PositiveInteger.class, description = "TODO")
	List<Integer> partSize = null;

	@Parameter(names = { "-part-size-part" }, variableArity = true, description = "TODO")
	List<PublicationPartName> partSizePart = null;

	@Parameter(names = { "-not-part-size" }, variableArity = true, validateWith = PositiveInteger.class, description = "TODO")
	List<Integer> notPartSize = null;

	@Parameter(names = { "-not-part-size-part" }, variableArity = true, description = "TODO")
	List<PublicationPartName> notPartSizePart = null;

	@Parameter(names = { "-part-size-more" }, validateWith = PositiveInteger.class, description = "TODO")
	Integer partSizeMore = null;

	@Parameter(names = { "-part-size-more-part" }, variableArity = true, description = "TODO")
	List<PublicationPartName> partSizeMorePart = null;

	@Parameter(names = { "-part-size-less" }, validateWith = PositiveInteger.class, description = "TODO")
	Integer partSizeLess = null;

	@Parameter(names = { "-part-size-less-part" }, variableArity = true, description = "TODO")
	List<PublicationPartName> partSizeLessPart = null;

	@Parameter(names = { "-part-type" }, variableArity = true, description = "TODO")
	List<PublicationPartType> partType = null;

	@Parameter(names = { "-part-type-part" }, variableArity = true, description = "TODO")
	List<PublicationPartName> partTypePart = null;

	@Parameter(names = { "-not-part-type" }, variableArity = true, description = "TODO")
	List<PublicationPartType> notPartType = null;

	@Parameter(names = { "-not-part-type-part" }, variableArity = true, description = "TODO")
	List<PublicationPartName> notPartTypePart = null;

	@Parameter(names = { "-part-type-more" }, description = "TODO")
	PublicationPartType partTypeMore = null;

	@Parameter(names = { "-part-type-more-part" }, variableArity = true, description = "TODO")
	List<PublicationPartName> partTypeMorePart = null;

	@Parameter(names = { "-part-type-less" }, description = "TODO")
	PublicationPartType partTypeLess = null;

	@Parameter(names = { "-part-type-less-part" }, variableArity = true, description = "TODO")
	List<PublicationPartName> partTypeLessPart = null;

	@Parameter(names = { "-part-type-final" }, description = "TODO")
	boolean partTypeFinal = false;

	@Parameter(names = { "-part-type-final-part" }, variableArity = true, description = "TODO")
	List<PublicationPartName> partTypeFinalPart = null;

	@Parameter(names = { "-not-part-type-final" }, description = "TODO")
	boolean notPartTypeFinal = false;

	@Parameter(names = { "-not-part-type-final-part" }, variableArity = true, description = "TODO")
	List<PublicationPartName> notPartTypeFinalPart = null;

	@Parameter(names = { "-part-type-pdf" }, description = "TODO")
	boolean partTypePdf = false;

	@Parameter(names = { "-part-type-pdf-part" }, variableArity = true, description = "TODO")
	List<PublicationPartName> partTypePdfPart = null;

	@Parameter(names = { "-not-part-type-pdf" }, description = "TODO")
	boolean notPartTypePdf = false;

	@Parameter(names = { "-not-part-type-pdf-part" }, variableArity = true, description = "TODO")
	List<PublicationPartName> notPartTypePdfPart = null;

	@Parameter(names = { "-part-url" }, description = "TODO")
	String partUrl = null;

	@Parameter(names = { "-part-url-part" }, variableArity = true, description = "TODO")
	List<PublicationPartName> partUrlPart = null;

	@Parameter(names = { "-not-part-url" }, description = "TODO")
	String notPartUrl = null;

	@Parameter(names = { "-not-part-url-part" }, variableArity = true, description = "TODO")
	List<PublicationPartName> notPartUrlPart = null;

	@Parameter(names = { "-part-url-host" }, variableArity = true, description = "TODO")
	List<String> partUrlHost = null;

	@Parameter(names = { "-part-url-host-part" }, variableArity = true, description = "TODO")
	List<PublicationPartName> partUrlHostPart = null;

	@Parameter(names = { "-not-part-url-host" }, variableArity = true, description = "TODO")
	List<String> notPartUrlHost = null;

	@Parameter(names = { "-not-part-url-host-part" }, variableArity = true, description = "TODO")
	List<PublicationPartName> notPartUrlHostPart = null;

	@Parameter(names = { "-part-time-more" }, converter = ISO8601Converter.class, description = "TODO")
	Long partTimeMore = null;

	@Parameter(names = { "-part-time-more-part" }, variableArity = true, description = "TODO")
	List<PublicationPartName> partTimeMorePart = null;

	@Parameter(names = { "-part-time-less" }, converter = ISO8601Converter.class, description = "TODO")
	Long partTimeLess = null;

	@Parameter(names = { "-part-time-less-part" }, variableArity = true, description = "TODO")
	List<PublicationPartName> partTimeLessPart = null;

	@Parameter(names = { "-oa" }, description = "TODO")
	boolean oa = false;

	@Parameter(names = { "-not-oa" }, description = "TODO")
	boolean notOa = false;

	@Parameter(names = { "-journal-title" }, description = "TODO")
	String journalTitle = null;

	@Parameter(names = { "-not-journal-title" }, description = "TODO")
	String notJournalTitle = null;

	@Parameter(names = { "-journal-title-empty" }, description = "TODO")
	boolean journalTitleEmpty = false;

	@Parameter(names = { "-not-journal-title-empty" }, description = "TODO")
	boolean notJournalTitleEmpty = false;

	@Parameter(names = { "-pub-date-more" }, converter = ISO8601Converter.class, description = "TODO more or equal")
	Long pubDateMore = null;

	@Parameter(names = { "-pub-date-less" }, converter = ISO8601Converter.class, description = "TODO less or equal")
	Long pubDateLess = null;

	@Parameter(names = { "-citations-count" }, variableArity = true, description = "TODO")
	List<Integer> citationsCount = null;

	@Parameter(names = { "-not-citations-count" }, variableArity = true, description = "TODO")
	List<Integer> notCitationsCount = null;

	@Parameter(names = { "-citations-count-more" }, description = "TODO")
	Integer citationsCountMore = null;

	@Parameter(names = { "-citations-count-less" }, description = "TODO")
	Integer citationsCountLess = null;

	@Parameter(names = { "-citations-timestamp-more" }, converter = ISO8601Converter.class, description = "TODO more or equal")
	Long citationsTimestampMore = null;

	@Parameter(names = { "-citations-timestamp-less" }, converter = ISO8601Converter.class, description = "TODO less or equal")
	Long citationsTimestampLess = null;

	@Parameter(names = { "-corresp-author-name" }, description = "TODO")
	String correspAuthorName = null;

	@Parameter(names = { "-not-corresp-author-name" }, description = "TODO")
	String notCorrespAuthorName = null;

	@Parameter(names = { "-corresp-author-name-empty" }, description = "TODO")
	boolean correspAuthorNameEmpty = false;

	@Parameter(names = { "-not-corresp-author-name-empty" }, description = "TODO")
	boolean notCorrespAuthorNameEmpty = false;

	@Parameter(names = { "-corresp-author-orcid" }, description = "TODO")
	String correspAuthorOrcid = null;

	@Parameter(names = { "-not-corresp-author-orcid" }, description = "TODO")
	String notCorrespAuthorOrcid = null;

	@Parameter(names = { "-corresp-author-orcid-empty" }, description = "TODO")
	boolean correspAuthorOrcidEmpty = false;

	@Parameter(names = { "-not-corresp-author-orcid-empty" }, description = "TODO")
	boolean notCorrespAuthorOrcidEmpty = false;

	@Parameter(names = { "-corresp-author-email" }, description = "TODO")
	String correspAuthorEmail = null;

	@Parameter(names = { "-not-corresp-author-email" }, description = "TODO")
	String notCorrespAuthorEmail = null;

	@Parameter(names = { "-corresp-author-email-empty" }, description = "TODO")
	boolean correspAuthorEmailEmpty = false;

	@Parameter(names = { "-not-corresp-author-email-empty" }, description = "TODO")
	boolean notCorrespAuthorEmailEmpty = false;

	@Parameter(names = { "-corresp-author-phone" }, description = "TODO")
	String correspAuthorPhone = null;

	@Parameter(names = { "-not-corresp-author-phone" }, description = "TODO")
	String notCorrespAuthorPhone = null;

	@Parameter(names = { "-corresp-author-phone-empty" }, description = "TODO")
	boolean correspAuthorPhoneEmpty = false;

	@Parameter(names = { "-not-corresp-author-phone-empty" }, description = "TODO")
	boolean notCorrespAuthorPhoneEmpty = false;

	@Parameter(names = { "-corresp-author-uri" }, description = "TODO")
	String correspAuthorUri = null;

	@Parameter(names = { "-not-corresp-author-uri" }, description = "TODO")
	String notCorrespAuthorUri = null;

	@Parameter(names = { "-corresp-author-uri-empty" }, description = "TODO")
	boolean correspAuthorUriEmpty = false;

	@Parameter(names = { "-not-corresp-author-uri-empty" }, description = "TODO")
	boolean notCorrespAuthorUriEmpty = false;

	@Parameter(names = { "-corresp-author-size" }, variableArity = true, validateWith = PositiveInteger.class, description = "TODO")
	List<Integer> correspAuthorSize = null;

	@Parameter(names = { "-not-corresp-author-size" }, variableArity = true, validateWith = PositiveInteger.class, description = "TODO")
	List<Integer> notCorrespAuthorSize = null;

	@Parameter(names = { "-corresp-author-size-more" }, validateWith = PositiveInteger.class, description = "TODO")
	Integer correspAuthorSizeMore = null;

	@Parameter(names = { "-corresp-author-size-less" }, validateWith = PositiveInteger.class, description = "TODO")
	Integer correspAuthorSizeLess = null;

	@Parameter(names = { "-visited" }, description = "TODO")
	String visited = null;

	@Parameter(names = { "-not-visited" }, description = "TODO")
	String notVisited = null;

	@Parameter(names = { "-visited-host" }, variableArity = true, description = "TODO")
	List<String> visitedHost = null;

	@Parameter(names = { "-not-visited-host" }, variableArity = true, description = "TODO")
	List<String> notVisitedHost = null;

	@Parameter(names = { "-visited-type" }, variableArity = true, description = "TODO")
	List<PublicationPartType> visitedType = null;

	@Parameter(names = { "-not-visited-type" }, variableArity = true, description = "TODO")
	List<PublicationPartType> notVisitedType = null;

	@Parameter(names = { "-visited-type-more" }, description = "TODO")
	PublicationPartType visitedTypeMore = null;

	@Parameter(names = { "-visited-type-less" }, description = "TODO")
	PublicationPartType visitedTypeLess = null;

	@Parameter(names = { "-visited-type-final" }, description = "TODO")
	boolean visitedTypeFinal = false;

	@Parameter(names = { "-not-visited-type-final" }, description = "TODO")
	boolean notVisitedTypeFinal = false;

	@Parameter(names = { "-visited-type-pdf" }, description = "TODO")
	boolean visitedTypePdf = false;

	@Parameter(names = { "-not-visited-type-pdf" }, description = "TODO")
	boolean notVisitedTypePdf = false;

	@Parameter(names = { "-visited-from" }, description = "TODO")
	String visitedFrom = null;

	@Parameter(names = { "-not-visited-from" }, description = "TODO")
	String notVisitedFrom = null;

	@Parameter(names = { "-visited-from-host" }, variableArity = true, description = "TODO")
	List<String> visitedFromHost = null;

	@Parameter(names = { "-not-visited-from-host" }, variableArity = true, description = "TODO")
	List<String> notVisitedFromHost = null;

	@Parameter(names = { "-visited-time-more" }, converter = ISO8601Converter.class, description = "TODO more or equal")
	Long visitedTimeMore = null;

	@Parameter(names = { "-visited-time-less" }, converter = ISO8601Converter.class, description = "TODO more or equal")
	Long visitedTimeLess = null;

	@Parameter(names = { "-visited-size" }, variableArity = true, validateWith = PositiveInteger.class, description = "TODO")
	List<Integer> visitedSize = null;

	@Parameter(names = { "-not-visited-size" }, variableArity = true, validateWith = PositiveInteger.class, description = "TODO")
	List<Integer> notVisitedSize = null;

	@Parameter(names = { "-visited-size-more" }, validateWith = PositiveInteger.class, description = "TODO")
	Integer visitedSizeMore = null;

	@Parameter(names = { "-visited-size-less" }, validateWith = PositiveInteger.class, description = "TODO")
	Integer visitedSizeLess = null;

	@Parameter(names = { "-start-url" }, description = "TODO")
	String startUrl = null;

	@Parameter(names = { "-not-start-url" }, description = "TODO")
	String notStartUrl = null;

	@Parameter(names = { "-start-url-host" }, variableArity = true, description = "TODO")
	List<String> startUrlHost = null;

	@Parameter(names = { "-not-start-url-host" }, variableArity = true, description = "TODO")
	List<String> notStartUrlHost = null;

	@Parameter(names = { "-final-url" }, description = "TODO")
	String finalUrl = null;

	@Parameter(names = { "-not-final-url" }, description = "TODO")
	String notFinalUrl = null;

	@Parameter(names = { "-final-url-host" }, variableArity = true, description = "TODO")
	List<String> finalUrlHost = null;

	@Parameter(names = { "-not-final-url-host" }, variableArity = true, description = "TODO")
	List<String> notFinalUrlHost = null;

	@Parameter(names = { "-final-url-empty" }, description = "TODO")
	boolean finalUrlEmpty = false;

	@Parameter(names = { "-not-final-url-empty" }, description = "TODO")
	boolean notFinalUrlEmpty = false;

	@Parameter(names = { "-content-type" }, description = "TODO")
	String contentType = null;

	@Parameter(names = { "-not-content-type" }, description = "TODO")
	String notContentType = null;

	@Parameter(names = { "-content-type-empty" }, description = "TODO")
	boolean contentTypeEmpty = false;

	@Parameter(names = { "-not-content-type-empty" }, description = "TODO")
	boolean notContentTypeEmpty = false;

	@Parameter(names = { "-status-code" }, variableArity = true, description = "TODO")
	List<Integer> statusCode = null;

	@Parameter(names = { "-not-status-code" }, variableArity = true, description = "TODO")
	List<Integer> notStatusCode = null;

	@Parameter(names = { "-status-code-more" }, description = "TODO")
	Integer statusCodeMore = null;

	@Parameter(names = { "-status-code-less" }, description = "TODO")
	Integer statusCodeLess = null;

	@Parameter(names = { "-title" }, description = "TODO")
	String title = null;

	@Parameter(names = { "-not-title" }, description = "TODO")
	String notTitle = null;

	@Parameter(names = { "-title-size" }, variableArity = true, validateWith = PositiveInteger.class, description = "TODO")
	List<Integer> titleSize = null;

	@Parameter(names = { "-not-title-size" }, variableArity = true, validateWith = PositiveInteger.class, description = "TODO")
	List<Integer> notTitleSize = null;

	@Parameter(names = { "-title-size-more" }, variableArity = true, validateWith = PositiveInteger.class, description = "TODO")
	Integer titleSizeMore = null;

	@Parameter(names = { "-title-size-less" }, variableArity = true, validateWith = PositiveInteger.class, description = "TODO")
	Integer titleSizeLess = null;

	@Parameter(names = { "-content" }, description = "TODO")
	String content = null;

	@Parameter(names = { "-not-content" }, description = "TODO")
	String notContent = null;

	@Parameter(names = { "-content-size" }, variableArity = true, validateWith = PositiveInteger.class, description = "TODO")
	List<Integer> contentSize = null;

	@Parameter(names = { "-not-content-size" }, variableArity = true, validateWith = PositiveInteger.class, description = "TODO")
	List<Integer> notContentSize = null;

	@Parameter(names = { "-content-size-more" }, variableArity = true, validateWith = PositiveInteger.class, description = "TODO")
	Integer contentSizeMore = null;

	@Parameter(names = { "-content-size-less" }, variableArity = true, validateWith = PositiveInteger.class, description = "TODO")
	Integer contentSizeLess = null;

	@Parameter(names = { "-content-time-more" }, converter = ISO8601Converter.class, description = "TODO more or equal")
	Long contentTimeMore = null;

	@Parameter(names = { "-content-time-less" }, converter = ISO8601Converter.class, description = "TODO less or equal")
	Long contentTimeLess = null;

	@Parameter(names = { "-license" }, description = "TODO")
	String license = null;

	@Parameter(names = { "-not-license" }, description = "TODO")
	String notLicense = null;

	@Parameter(names = { "-license-empty" }, description = "TODO")
	boolean licenseEmpty = false;

	@Parameter(names = { "-not-license-empty" }, description = "TODO")
	boolean notLicenseEmpty = false;

	@Parameter(names = { "-language" }, description = "TODO")
	String language = null;

	@Parameter(names = { "-not-language" }, description = "TODO")
	String notLanguage = null;

	@Parameter(names = { "-language-empty" }, description = "TODO")
	boolean languageEmpty = false;

	@Parameter(names = { "-not-language-empty" }, description = "TODO")
	boolean notLanguageEmpty = false;

	@Parameter(names = { "-grep" }, description = "TODO")
	String grep = null;

	@Parameter(names = { "-not-grep" }, description = "TODO")
	String notGrep = null;

	@Parameter(names = { "-has-scrape" }, description = "TODO")
	boolean hasScrape = false;

	@Parameter(names = { "-not-has-scrape" }, description = "TODO")
	boolean notHasScrape = false;

	@Parameter(names = { "-asc" }, description = "TODO")
	boolean asc = false;

	@Parameter(names = { "-desc" }, description = "TODO")
	boolean desc = false;

	@Parameter(names = { "-asc-time" }, description = "TODO")
	boolean ascTime = false;

	@Parameter(names = { "-desc-time" }, description = "TODO")
	boolean descTime = false;

	@Parameter(names = { "-head" }, validateWith = PositiveInteger.class, description = "TODO")
	Integer head = null;

	@Parameter(names = { "-tail" }, validateWith = PositiveInteger.class, description = "TODO")
	Integer tail = null;

	@Parameter(names = { "-update-citations-count" }, description = "TODO")
	boolean updateCitationsCount = false;

	@Parameter(names = { "-put" }, description = "TODO")
	String put = null;

	@Parameter(names = { "-remove" }, description = "TODO")
	String remove = null;

	@Parameter(names = { "-plain" }, description = "TODO")
	boolean plain = false;

	@Parameter(names = { "-format" }, description = "TODO")
	Format format = Format.text;

	@Parameter(names = { "-out-part" }, variableArity = true, description = "TODO")
	List<PublicationPartName> outPart = null;

	@Parameter(names = { "-out" }, description = "TODO")
	boolean out = false;

	@Parameter(names = { "-txt-pub" }, description = "TODO")
	String txtPub = null;

	@Parameter(names = { "-txt-web" }, description = "TODO")
	String txtWeb = null;

	@Parameter(names = { "-txt-doc" }, description = "TODO")
	String txtDoc = null;

	@Parameter(names = { "-count" }, description = "TODO")
	boolean count = false;

	@Parameter(names = { "-out-top-hosts" }, description = "TODO")
	boolean outTopHosts = false;

	@Parameter(names = { "-txt-top-hosts-pub" }, description = "TODO")
	String txtTopHostsPub = null;

	@Parameter(names = { "-txt-top-hosts-web" }, description = "TODO")
	String txtTopHostsWeb = null;

	@Parameter(names = { "-txt-top-hosts-doc" }, description = "TODO")
	String txtTopHostsDoc = null;

	@Parameter(names = { "-count-top-hosts" }, description = "TODO")
	boolean countTopHosts = false;

	@Parameter(names = { "-part-table" }, description = "TODO")
	boolean partTable = false;
}
