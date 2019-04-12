/*
 * Copyright © 2018, 2019 Erik Jaaniso
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

package org.edamontology.pubfetcher.core.common;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

public class FetcherArgs extends Args {

	private static final String emptyCooldownId = "emptyCooldown";
	private static final String emptyCooldownDescription = "If that many minutes have passed since last fetching attempt of an empty publication or empty webpage, then fetching can be attempted again, resetting the retry counter. Setting to 0 means fetching of empty database entries will always be attempted again. Setting to a negative value means refetching will never be done (and retry counter never reset) only because the entry is empty.";
	private static final Integer emptyCooldownDefault = 720; // 12 h
	@Parameter(names = { "--" + emptyCooldownId }, description = emptyCooldownDescription)
	private Integer emptyCooldown = emptyCooldownDefault;

	private static final String nonFinalCooldownId = "nonFinalCooldown";
	private static final String nonFinalCooldownDescription = "If that many minutes have passed since last fetching attempt of a non-final publication or non-final webpage (which are not empty), then fetching can be attempted again, resetting the retry counter. Setting to 0 means fetching of non-final database entries will always be attempted again. Setting to a negative value means refetching will never be done (and retry counter never reset) only because the entry is non-final.";
	private static final Integer nonFinalCooldownDefault = 10080; // a week
	@Parameter(names = { "--" + nonFinalCooldownId }, description = nonFinalCooldownDescription)
	private Integer nonFinalCooldown = nonFinalCooldownDefault;

	private static final String fetchExceptionCooldownId = "fetchExceptionCooldown";
	private static final String fetchExceptionCooldownDescription = "If that many minutes have passed since last fetching attempt of a publication or webpage with a fetching exception, then fetching can be attempted again, resetting the retry counter. Setting to 0 means fetching of database entries with fetching exception will always be attempted again. Setting to a negative value means refetching will never be done (and retry counter never reset) only because the fetching exception of the entry is \"true\".";
	private static final Integer fetchExceptionCooldownDefault = 1440; // a day
	@Parameter(names = { "--" + fetchExceptionCooldownId }, description = fetchExceptionCooldownDescription)
	private Integer fetchExceptionCooldown = fetchExceptionCooldownDefault;

	private static final String retryLimitId = "retryLimit";
	private static final String retryLimitDescription = "How many times can fetching be retried for an entry that is still empty, non-final or has a fetching exception after the initial attempt. Setting to 0 will disable retrying, unless the retry counter is reset by a cooldown in which case one initial attempt is allowed again. Setting to a negative value will disable this upper limit.";
	private static final Integer retryLimitDefault = 3;
	@Parameter(names = { "--" + retryLimitId }, description = retryLimitDescription)
	private Integer retryLimit = retryLimitDefault;

	private static final String titleMinLengthId = "titleMinLength";
	private static final String titleMinLengthDescription = "Minimum length of a usable publication title";
	private static final Integer titleMinLengthDefault = 4;
	@Parameter(names = { "--" + titleMinLengthId }, validateWith = PositiveInteger.class, description = titleMinLengthDescription)
	private Integer titleMinLength = titleMinLengthDefault;

	private static final String keywordsMinSizeId = "keywordsMinSize";
	private static final String keywordsMinSizeDescription = "Minimum size of a usable publication keywords/MeSH list";
	private static final Integer keywordsMinSizeDefault = 2;
	@Parameter(names = { "--" + keywordsMinSizeId }, validateWith = PositiveInteger.class, description = keywordsMinSizeDescription)
	private Integer keywordsMinSize = keywordsMinSizeDefault;

	private static final String minedTermsMinSizeId = "minedTermsMinSize";
	private static final String minedTermsMinSizeDescription = "Minimum size of a usable publication EFO/GO terms list";
	private static final Integer minedTermsMinSizeDefault = 1;
	@Parameter(names = { "--" + minedTermsMinSizeId }, validateWith = PositiveInteger.class, description = minedTermsMinSizeDescription)
	private Integer minedTermsMinSize = minedTermsMinSizeDefault;

	private static final String abstractMinLengthId = "abstractMinLength";
	private static final String abstractMinLengthDescription = "Minimum length of a usable publication abstract";
	private static final Integer abstractMinLengthDefault = 200;
	@Parameter(names = { "--" + abstractMinLengthId }, validateWith = PositiveInteger.class, description = abstractMinLengthDescription)
	private Integer abstractMinLength = abstractMinLengthDefault;

	private static final String fulltextMinLengthId = "fulltextMinLength";
	private static final String fulltextMinLengthDescription = "Minimum length of a usable publication fulltext";
	private static final Integer fulltextMinLengthDefault = 2000;
	@Parameter(names = { "--" + fulltextMinLengthId }, validateWith = PositiveInteger.class, description = fulltextMinLengthDescription)
	private Integer fulltextMinLength = fulltextMinLengthDefault;

	private static final String webpageMinLengthId = "webpageMinLength";
	private static final String webpageMinLengthDescription = "Minimum length of a usable webpage combined title and content";
	private static final Integer webpageMinLengthDefault = 50;
	@Parameter(names = { "--" + webpageMinLengthId }, validateWith = PositiveInteger.class, description = webpageMinLengthDescription)
	private Integer webpageMinLength = webpageMinLengthDefault;

	private static final String webpageMinLengthJavascriptId = "webpageMinLengthJavascript";
	private static final String webpageMinLengthJavascriptDescription = "If the length of a whole web page content fetched without JavaScript is below the specified limit and no scraping rules are found for the corresponding URL, then refetching using JavaScript support will be attempted";
	private static final Integer webpageMinLengthJavascriptDefault = 200;
	@Parameter(names = { "--" + webpageMinLengthJavascriptId }, validateWith = PositiveInteger.class, description = webpageMinLengthJavascriptDescription)
	private Integer webpageMinLengthJavascript = webpageMinLengthJavascriptDefault;

	private static final String timeoutId = "timeout";
	private static final String timeoutDescription = "Connect and read timeout of connections, in milliseconds";
	private static final Integer timeoutDefault = 15000; // ms
	@Parameter(names = { "--" + timeoutId }, validateWith = PositiveInteger.class, description = timeoutDescription)
	private Integer timeout = timeoutDefault;

	@Override
	protected void addArgs() {
		args.add(new Arg<>(this::getEmptyCooldown, this::setEmptyCooldown, emptyCooldownDefault, emptyCooldownId, "Empty cooldown", emptyCooldownDescription, null));
		args.add(new Arg<>(this::getNonFinalCooldown, this::setNonFinalCooldown, nonFinalCooldownDefault, nonFinalCooldownId, "Non-final cooldown", nonFinalCooldownDescription, null));
		args.add(new Arg<>(this::getFetchExceptionCooldown, this::setFetchExceptionCooldown, fetchExceptionCooldownDefault, fetchExceptionCooldownId, "Fetching exception cooldown", fetchExceptionCooldownDescription, null));
		args.add(new Arg<>(this::getRetryLimit, this::setRetryLimit, retryLimitDefault, retryLimitId, "Retry limit", retryLimitDescription, null));
		args.add(new Arg<>(this::getTitleMinLength, this::setTitleMinLength, titleMinLengthDefault, 0, null, titleMinLengthId, "Title min. length", titleMinLengthDescription, null));
		args.add(new Arg<>(this::getKeywordsMinSize, this::setKeywordsMinSize, keywordsMinSizeDefault, 0, null, keywordsMinSizeId, "Keywords min. size", keywordsMinSizeDescription, null));
		args.add(new Arg<>(this::getMinedTermsMinSize, this::setMinedTermsMinSize, minedTermsMinSizeDefault, 0, null, minedTermsMinSizeId, "Mined terms min. size", minedTermsMinSizeDescription, null));
		args.add(new Arg<>(this::getAbstractMinLength, this::setAbstractMinLength, abstractMinLengthDefault, 0, null, abstractMinLengthId, "Abstract min. length", abstractMinLengthDescription, null));
		args.add(new Arg<>(this::getFulltextMinLength, this::setFulltextMinLength, fulltextMinLengthDefault, 0, null, fulltextMinLengthId, "Fulltext min. length", fulltextMinLengthDescription, null));
		args.add(new Arg<>(this::getWebpageMinLength, this::setWebpageMinLength, webpageMinLengthDefault, 0, null, webpageMinLengthId, "Webpage min. length", webpageMinLengthDescription, null));
		args.add(new Arg<>(this::getWebpageMinLengthJavascript, this::setWebpageMinLengthJavascript, webpageMinLengthJavascriptDefault, 0, null, webpageMinLengthJavascriptId, "Webpage min. length JS", webpageMinLengthJavascriptDescription, null));
		args.add(new Arg<>(this::getTimeout, this::setTimeout, timeoutDefault, 0, null, timeoutId, "Timeout", timeoutDescription, null));
	}

	@ParametersDelegate
	private FetcherPrivateArgs privateArgs = new FetcherPrivateArgs();

	@Override
	public String getId() {
		return "fetcherArgs";
	}

	@Override
	public String getLabel() {
		return "Fetching";
	}

	public Integer getEmptyCooldown() {
		return emptyCooldown;
	}
	public void setEmptyCooldown(Integer emptyCooldown) {
		this.emptyCooldown = emptyCooldown;
	}

	public Integer getNonFinalCooldown() {
		return nonFinalCooldown;
	}
	public void setNonFinalCooldown(Integer nonFinalCooldown) {
		this.nonFinalCooldown = nonFinalCooldown;
	}

	public Integer getFetchExceptionCooldown() {
		return fetchExceptionCooldown;
	}
	public void setFetchExceptionCooldown(Integer fetchExceptionCooldown) {
		this.fetchExceptionCooldown = fetchExceptionCooldown;
	}

	public Integer getRetryLimit() {
		return retryLimit;
	}
	public void setRetryLimit(Integer retryLimit) {
		this.retryLimit = retryLimit;
	}

	public Integer getTitleMinLength() {
		return titleMinLength;
	}
	public void setTitleMinLength(Integer titleMinLength) {
		this.titleMinLength = titleMinLength;
	}

	public Integer getKeywordsMinSize() {
		return keywordsMinSize;
	}
	public void setKeywordsMinSize(Integer keywordsMinSize) {
		this.keywordsMinSize = keywordsMinSize;
	}

	public Integer getMinedTermsMinSize() {
		return minedTermsMinSize;
	}
	public void setMinedTermsMinSize(Integer minedTermsMinSize) {
		this.minedTermsMinSize = minedTermsMinSize;
	}

	public Integer getAbstractMinLength() {
		return abstractMinLength;
	}
	public void setAbstractMinLength(Integer abstractMinLength) {
		this.abstractMinLength = abstractMinLength;
	}

	public Integer getFulltextMinLength() {
		return fulltextMinLength;
	}
	public void setFulltextMinLength(Integer fulltextMinLength) {
		this.fulltextMinLength = fulltextMinLength;
	}

	public Integer getWebpageMinLength() {
		return webpageMinLength;
	}
	public void setWebpageMinLength(Integer webpageMinLength) {
		this.webpageMinLength = webpageMinLength;
	}

	public Integer getWebpageMinLengthJavascript() {
		return webpageMinLengthJavascript;
	}
	public void setWebpageMinLengthJavascript(Integer webpageMinLengthJavascript) {
		this.webpageMinLengthJavascript = webpageMinLengthJavascript;
	}

	public Integer getTimeout() {
		return timeout;
	}
	public void setTimeout(Integer timeout) {
		this.timeout = timeout;
	}

	public FetcherPrivateArgs getPrivateArgs() {
		return privateArgs;
	}
	public void setPrivateArgs(FetcherPrivateArgs privateArgs) {
		this.privateArgs = privateArgs;
	}
}
