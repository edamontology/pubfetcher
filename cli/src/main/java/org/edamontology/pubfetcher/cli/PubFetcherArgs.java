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
import org.edamontology.pubfetcher.core.scrape.ScrapeSiteKey;

public class PubFetcherArgs {

	@ParametersDelegate
	FetcherTestArgs fetcherTestArgs = new FetcherTestArgs();

	@Parameter(names = { "--init-db" }, description = "TODO")
	String initDb = null;

	@Parameter(names = { "--commit-db" }, description = "TODO")
	String commitDb = null;

	@Parameter(names = { "--compact-db" }, description = "TODO")
	String compactDb = null;

	@Parameter(names = { "--publications-size" }, description = "TODO")
	String publicationsSize = null;

	@Parameter(names = { "--webpages-size" }, description = "TODO")
	String webpagesSize = null;

	@Parameter(names = { "--docs-size" }, description = "TODO")
	String docsSize = null;

	@Parameter(names = { "--dump-publications-map" }, description = "TODO")
	String dumpPublicationsMap = null;

	@Parameter(names = { "--dump-publications-map-reverse" }, description = "TODO")
	String dumpPublicationsMapReverse = null;

	@Parameter(names = { "--print-webpage-selector" }, arity = 4, description = "TODO")
	List<String> printWebpageSelector = null;

	@Parameter(names = { "--print-webpage-selector-html" }, arity = 4, description = "TODO")
	List<String> printWebpageSelectorHtml = null;

	@Parameter(names = { "--get-site" }, description = "TODO")
	String getSite = null;

	@Parameter(names = { "--get-selector" }, description = "TODO")
	ScrapeSiteKey getSelector = null;

	@Parameter(names = { "--get-webpage" }, description = "TODO")
	String getWebpage = null;

	@Parameter(names = { "--get-javascript" }, description = "TODO")
	String getJavascript = null;

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

	@Parameter(names = { "-pub-db" }, description = "TODO")
	String pubDb = null;

	@Parameter(names = { "-web-db" }, description = "TODO")
	String webDb = null;

	@Parameter(names = { "-doc-db" }, description = "TODO")
	String docDb = null;

	@Parameter(names = { "-all-db" }, description = "TODO")
	String allDb = null;

	@Parameter(names = { "-has-pmid" }, description = "TODO")
	boolean hasPmid = false;

	@Parameter(names = { "-not-has-pmid" }, description = "TODO")
	boolean notHasPmid = false;

	@Parameter(names = { "-pmid" }, description = "TODO")
	String pmid = null;

	@Parameter(names = { "-has-pmcid" }, description = "TODO")
	boolean hasPmcid = false;

	@Parameter(names = { "-not-has-pmcid" }, description = "TODO")
	boolean notHasPmcid = false;

	@Parameter(names = { "-pmcid" }, description = "TODO")
	String pmcid = null;

	@Parameter(names = { "-has-doi" }, description = "TODO")
	boolean hasDoi = false;

	@Parameter(names = { "-not-has-doi" }, description = "TODO")
	boolean notHasDoi = false;

	@Parameter(names = { "-doi" }, description = "TODO")
	String doi = null;

	@Parameter(names = { "-doi-registrant" }, variableArity = true, description = "TODO")
	List<String> doiRegistrant = null;

	@Parameter(names = { "-not-doi-registrant" }, variableArity = true, description = "TODO")
	List<String> notDoiRegistrant = null;

	@Parameter(names = { "-url" }, description = "TODO")
	String url = null;

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

	@Parameter(names = { "-db" }, description = "TODO")
	String db = null;

	@Parameter(names = { "-fetch" }, description = "TODO")
	boolean fetch = false;

	@Parameter(names = { "-db-fetch" }, description = "TODO")
	String dbFetch = null;

	@Parameter(names = { "-fetch-put" }, description = "TODO")
	String fetchPut = null;

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

	@Parameter(names = { "-empty" }, description = "TODO")
	boolean empty = false;

	@Parameter(names = { "-not-empty" }, description = "TODO")
	boolean notEmpty = false;

	@Parameter(names = { "-final" }, description = "TODO")
	boolean isFinal = false;

	@Parameter(names = { "-not-final" }, description = "TODO")
	boolean notIsFinal = false;

	@Parameter(names = { "-totally-final" }, description = "TODO")
	boolean totallyFinal = false;

	@Parameter(names = { "-not-totally-final" }, description = "TODO")
	boolean notTotallyFinal = false;

	@Parameter(names = { "-part-empty" }, variableArity = true, description = "TODO")
	List<PublicationPartName> partEmpty = null;

	@Parameter(names = { "-not-part-empty" }, variableArity = true, description = "TODO")
	List<PublicationPartName> notPartEmpty = null;

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

	@Parameter(names = { "-part-content-size" }, variableArity = true, validateWith = PositiveInteger.class, description = "TODO")
	List<Integer> partContentSize = null;

	@Parameter(names = { "-part-content-size-part" }, variableArity = true, description = "TODO")
	List<PublicationPartName> partContentSizePart = null;

	@Parameter(names = { "-not-part-content-size" }, variableArity = true, validateWith = PositiveInteger.class, description = "TODO")
	List<Integer> notPartContentSize = null;

	@Parameter(names = { "-not-part-content-size-part" }, variableArity = true, description = "TODO")
	List<PublicationPartName> notPartContentSizePart = null;

	@Parameter(names = { "-part-content-size-more" }, validateWith = PositiveInteger.class, description = "TODO")
	Integer partContentSizeMore = null;

	@Parameter(names = { "-part-content-size-more-part" }, variableArity = true, description = "TODO")
	List<PublicationPartName> partContentSizeMorePart = null;

	@Parameter(names = { "-part-content-size-less" }, validateWith = PositiveInteger.class, description = "TODO")
	Integer partContentSizeLess = null;

	@Parameter(names = { "-part-content-size-less-part" }, variableArity = true, description = "TODO")
	List<PublicationPartName> partContentSizeLessPart = null;

	@Parameter(names = { "-part-type" }, variableArity = true, description = "TODO")
	List<PublicationPartType> partType = null;

	@Parameter(names = { "-part-type-part" }, variableArity = true, description = "TODO")
	List<PublicationPartName> partTypePart = null;

	@Parameter(names = { "-not-part-type" }, variableArity = true, description = "TODO")
	List<PublicationPartType> notPartType = null;

	@Parameter(names = { "-not-part-type-part" }, variableArity = true, description = "TODO")
	List<PublicationPartName> notPartTypePart = null;

	@Parameter(names = { "-part-type-equivalent" }, description = "TODO")
	PublicationPartType partTypeEquivalent = null;

	@Parameter(names = { "-part-type-equivalent-part" }, variableArity = true, description = "TODO")
	List<PublicationPartName> partTypeEquivalentPart = null;

	@Parameter(names = { "-part-type-more" }, description = "TODO")
	PublicationPartType partTypeMore = null;

	@Parameter(names = { "-part-type-more-part" }, variableArity = true, description = "TODO")
	List<PublicationPartName> partTypeMorePart = null;

	@Parameter(names = { "-part-type-less" }, description = "TODO")
	PublicationPartType partTypeLess = null;

	@Parameter(names = { "-part-type-less-part" }, variableArity = true, description = "TODO")
	List<PublicationPartName> partTypeLessPart = null;

	@Parameter(names = { "-part-url" }, description = "TODO")
	String partUrl = null;

	@Parameter(names = { "-part-url-part" }, variableArity = true, description = "TODO")
	List<PublicationPartName> partUrlPart = null;

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

	@Parameter(names = { "-visited" }, description = "TODO")
	String visited = null;

	@Parameter(names = { "-visited-host" }, variableArity = true, description = "TODO")
	List<String> visitedHost = null;

	@Parameter(names = { "-not-visited-host" }, variableArity = true, description = "TODO")
	List<String> notVisitedHost = null;

	@Parameter(names = { "-visited-type" }, variableArity = true, description = "TODO")
	List<PublicationPartType> visitedType = null;

	@Parameter(names = { "-not-visited-type" }, variableArity = true, description = "TODO")
	List<PublicationPartType> notVisitedType = null;

	@Parameter(names = { "-visited-from" }, description = "TODO")
	String visitedFrom = null;

	@Parameter(names = { "-visited-from-host" }, variableArity = true, description = "TODO")
	List<String> visitedFromHost = null;

	@Parameter(names = { "-not-visited-from-host" }, variableArity = true, description = "TODO")
	List<String> notVisitedFromHost = null;

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

	@Parameter(names = { "-start-url-host" }, variableArity = true, description = "TODO")
	List<String> startUrlHost = null;

	@Parameter(names = { "-not-start-url-host" }, variableArity = true, description = "TODO")
	List<String> notStartUrlHost = null;

	@Parameter(names = { "-final-url" }, description = "TODO")
	String finalUrl = null;

	@Parameter(names = { "-final-url-host" }, variableArity = true, description = "TODO")
	List<String> finalUrlHost = null;

	@Parameter(names = { "-not-final-url-host" }, variableArity = true, description = "TODO")
	List<String> notFinalUrlHost = null;

	@Parameter(names = { "-content-type" }, description = "TODO")
	String contentType = null;

	@Parameter(names = { "-status-code" }, variableArity = true, description = "TODO")
	List<Integer> statusCode = null;

	@Parameter(names = { "-not-status-code" }, variableArity = true, description = "TODO")
	List<Integer> notStatusCode = null;

	@Parameter(names = { "-title" }, description = "TODO")
	String title = null;

	@Parameter(names = { "-title-more" }, validateWith = PositiveInteger.class, description = "TODO")
	Integer titleMore = null;

	@Parameter(names = { "-title-less" }, validateWith = PositiveInteger.class, description = "TODO")
	Integer titleLess = null;

	@Parameter(names = { "-content" }, description = "TODO")
	String content = null;

	@Parameter(names = { "-content-more" }, validateWith = PositiveInteger.class, description = "TODO")
	Integer contentMore = null;

	@Parameter(names = { "-content-less" }, validateWith = PositiveInteger.class, description = "TODO")
	Integer contentLess = null;

	@Parameter(names = { "-content-time-more" }, converter = ISO8601Converter.class, description = "TODO more or equal")
	Long contentTimeMore = null;

	@Parameter(names = { "-content-time-less" }, converter = ISO8601Converter.class, description = "TODO less or equal")
	Long contentTimeLess = null;

	@Parameter(names = { "-grep" }, description = "TODO")
	String grep = null;

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

	@Parameter(names = { "-put" }, description = "TODO")
	String put = null;

	@Parameter(names = { "-remove" }, description = "TODO")
	String remove = null;

	@Parameter(names = { "-html" }, description = "TODO")
	boolean html = false;

	@Parameter(names = { "-plain" }, description = "TODO")
	boolean plain = false;

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

	@Parameter(names = { "-out-top-hosts-no-scrape" }, description = "TODO")
	boolean outTopHostsNoScrape = false;

	@Parameter(names = { "-txt-top-hosts-pub-no-scrape" }, description = "TODO")
	String txtTopHostsPubNoScrape = null;

	@Parameter(names = { "-txt-top-hosts-web-no-scrape" }, description = "TODO")
	String txtTopHostsWebNoScrape = null;

	@Parameter(names = { "-txt-top-hosts-doc-no-scrape" }, description = "TODO")
	String txtTopHostsDocNoScrape = null;

	@Parameter(names = { "-part-table" }, description = "TODO")
	boolean partTable = false;
}
