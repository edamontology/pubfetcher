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

package org.edamontology.pubfetcher.core.common;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;
import com.beust.jcommander.validators.PositiveInteger;

public class FetcherArgs {

	public static final String EMPTY_COOLDOWN = "emptyCooldown";
	@Parameter(names = { "--" + EMPTY_COOLDOWN }, description = "If that many minutes have passed since last fetching attempt of an empty publication or empty webpage, then fetching can be attempted again, resetting the retry counter. Setting to 0 means fetching of empty database entries will always be attempted again. Setting to a negative value means refetching will never be done (and retry counter never reset) only because the entry is empty.")
	private int emptyCooldown = 720; // 12 h

	public static final String NON_FINAL_COOLDOWN = "nonFinalCooldown";
	@Parameter(names = { "--" + NON_FINAL_COOLDOWN }, description = "If that many minutes have passed since last fetching attempt of a non-final publication or non-final webpage (which are not empty), then fetching can be attempted again, resetting the retry counter. Setting to 0 means fetching of non-final database entries will always be attempted again. Setting to a negative value means refetching will never be done (and retry counter never reset) only because the entry is non-final.")
	private int nonFinalCooldown = 10080; // a week

	public static final String FETCH_EXCEPTION_COOLDOWN = "fetchExceptionCooldown";
	@Parameter(names = { "--" + FETCH_EXCEPTION_COOLDOWN }, description = "If that many minutes have passed since last fetching attempt of a publication or webpage with a fetching exception, then fetching can be attempted again, resetting the retry counter. Setting to 0 means fetching of database entries with fetching exception will always be attempted again. Setting to a negative value means refetching will never be done (and retry counter never reset) only because the fetching exception of the entry is \"true\".")
	private int fetchExceptionCooldown = 1440; // a day

	public static final String RETRY_LIMIT = "retryLimit";
	@Parameter(names = { "--" + RETRY_LIMIT }, description = "How many times can fetching be retried for an entry that is still empty, non-final or has a fetching exception after the initial attempt. Setting to 0 will disable retrying, unless the retry counter is reset by a cooldown in which case one initial attempt is allowed again. Setting to a negative value will disable this upper limit.")
	private int retryLimit = 3;

	public static final String TITLE_MIN_LENGTH = "titleMinLength";
	@Parameter(names = { "--" + TITLE_MIN_LENGTH }, validateWith = PositiveInteger.class, description = "Minimum length of a usable publication title")
	private int titleMinLength = 4;

	public static final String KEYWORDS_MIN_SIZE = "keywordsMinSize";
	@Parameter(names = { "--" + KEYWORDS_MIN_SIZE }, validateWith = PositiveInteger.class, description = "Minimum size of a usable publication keywords/MeSH list")
	private int keywordsMinSize = 2;

	public static final String MINED_TERMS_MIN_SIZE = "minedTermsMinSize";
	@Parameter(names = { "--" + MINED_TERMS_MIN_SIZE }, validateWith = PositiveInteger.class, description = "Minimum size of a usable publication EFO/GO terms list")
	private int minedTermsMinSize = 1;

	public static final String ABSTRACT_MIN_LENGTH = "abstractMinLength";
	@Parameter(names = { "--" + ABSTRACT_MIN_LENGTH }, validateWith = PositiveInteger.class, description = "Minimum length of a usable publication abstract")
	private int abstractMinLength = 200;

	public static final String FULLTEXT_MIN_LENGTH = "fulltextMinLength";
	@Parameter(names = { "--" + FULLTEXT_MIN_LENGTH }, validateWith = PositiveInteger.class, description = "Minimum length of a usable publication fulltext")
	private int fulltextMinLength = 2000;

	public static final String WEBPAGE_MIN_LENGTH = "webpageMinLength";
	@Parameter(names = { "--" + WEBPAGE_MIN_LENGTH }, validateWith = PositiveInteger.class, description = "Minimum length of a final webpage combined title and content")
	private int webpageMinLength = 50;

	public static final String WEBPAGE_MIN_LENGTH_JAVASCRIPT = "webpageMinLengthJavascript";
	@Parameter(names = { "--" + WEBPAGE_MIN_LENGTH_JAVASCRIPT }, validateWith = PositiveInteger.class, description = "If the length of a whole web page content fetched without JavaScript is below the specified limit and no scraping rules are found for the corresponding URL, then refetching using JavaScript support will be attempted")
	private int webpageMinLengthJavascript = 200;

	public static final String TIMEOUT = "timeout";
	@Parameter(names = { "--" + TIMEOUT }, validateWith = PositiveInteger.class, description = "Connect and read timeout of connections, in milliseconds")
	private int timeout = 15000; // ms

	@ParametersDelegate
	private FetcherPrivateArgs privateArgs = new FetcherPrivateArgs();

	public int getEmptyCooldown() {
		return emptyCooldown;
	}
	public void setEmptyCooldown(int emptyCooldown) {
		this.emptyCooldown = emptyCooldown;
	}

	public int getNonFinalCooldown() {
		return nonFinalCooldown;
	}
	public void setNonFinalCooldown(int nonFinalCooldown) {
		this.nonFinalCooldown = nonFinalCooldown;
	}

	public int getFetchExceptionCooldown() {
		return fetchExceptionCooldown;
	}
	public void setFetchExceptionCooldown(int fetchExceptionCooldown) {
		this.fetchExceptionCooldown = fetchExceptionCooldown;
	}

	public int getRetryLimit() {
		return retryLimit;
	}
	public void setRetryLimit(int retryLimit) {
		this.retryLimit = retryLimit;
	}

	public int getTitleMinLength() {
		return titleMinLength;
	}
	public void setTitleMinLength(int titleMinLength) {
		this.titleMinLength = titleMinLength;
	}

	public int getKeywordsMinSize() {
		return keywordsMinSize;
	}
	public void setKeywordsMinSize(int keywordsMinSize) {
		this.keywordsMinSize = keywordsMinSize;
	}

	public int getMinedTermsMinSize() {
		return minedTermsMinSize;
	}
	public void setMinedTermsMinSize(int minedTermsMinSize) {
		this.minedTermsMinSize = minedTermsMinSize;
	}

	public int getAbstractMinLength() {
		return abstractMinLength;
	}
	public void setAbstractMinLength(int abstractMinLength) {
		this.abstractMinLength = abstractMinLength;
	}

	public int getFulltextMinLength() {
		return fulltextMinLength;
	}
	public void setFulltextMinLength(int fulltextMinLength) {
		this.fulltextMinLength = fulltextMinLength;
	}

	public int getWebpageMinLength() {
		return webpageMinLength;
	}
	public void setWebpageMinLength(int webpageMinLength) {
		this.webpageMinLength = webpageMinLength;
	}

	public int getWebpageMinLengthJavascript() {
		return webpageMinLengthJavascript;
	}
	public void setWebpageMinLengthJavascript(int webpageMinLengthJavascript) {
		this.webpageMinLengthJavascript = webpageMinLengthJavascript;
	}

	public int getTimeout() {
		return timeout;
	}
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public FetcherPrivateArgs getPrivateArgs() {
		return privateArgs;
	}
	public void setPrivateArgs(FetcherPrivateArgs privateArgs) {
		this.privateArgs = privateArgs;
	}
}