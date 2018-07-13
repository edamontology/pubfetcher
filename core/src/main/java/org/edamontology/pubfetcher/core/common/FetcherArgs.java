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
	@Parameter(names = { "--" + EMPTY_COOLDOWN }, validateWith = PositiveInteger.class, description = "If that many minutes have passed since last fetching attempt of an empty database entry, then fetching can be attempted again, resetting the retry limit")
	private int emptyCooldown = 720; // 12 h

	public static final String NON_FINAL_COOLDOWN = "nonFinalCooldown";
	@Parameter(names = { "--" + NON_FINAL_COOLDOWN }, validateWith = PositiveInteger.class, description = "If that many minutes have passed since last fetching attempt of a non-final database entry, then fetching can be attempted again, resetting the retry limit")
	private int nonFinalCooldown = 10080; // a week

	public static final String FETCH_EXCEPTION_COOLDOWN = "fetchExceptionCooldown";
	@Parameter(names = { "--" + FETCH_EXCEPTION_COOLDOWN }, validateWith = PositiveInteger.class, description = "If that many minutes have passed since last fetching attempt of a database entry with fetching error, then fetching can be attempted again, resetting the retry limit")
	private int fetchExceptionCooldown = 1440; // a day

	public static final String RETRY_LIMIT = "retryLimit";
	@Parameter(names = { "--" + RETRY_LIMIT }, description = "How many times can fetching of a still non-final database entry be retried after initial attempt. Setting to 0 will disable retrying, setting to a negative value will disable limit.")
	private int retryLimit = 3;

	public static final String TITLE_MIN_LENGTH = "titleMinLength";
	@Parameter(names = { "--" + TITLE_MIN_LENGTH }, validateWith = PositiveInteger.class, description = "Minimum length of a valid publication title")
	private int titleMinLength = 4;

	public static final String KEYWORDS_MIN_SIZE = "keywordsMinSize";
	@Parameter(names = { "--" + KEYWORDS_MIN_SIZE }, validateWith = PositiveInteger.class, description = "Minimum size of a valid publication keywords/MeSH list")
	private int keywordsMinSize = 2;

	public static final String MINED_TERMS_MIN_SIZE = "minedTermsMinSize";
	@Parameter(names = { "--" + MINED_TERMS_MIN_SIZE }, validateWith = PositiveInteger.class, description = "Minimum size of a valid publication EFO/GO terms list")
	private int minedTermsMinSize = 1;

	public static final String ABSTRACT_MIN_LENGTH = "abstractMinLength";
	@Parameter(names = { "--" + ABSTRACT_MIN_LENGTH }, validateWith = PositiveInteger.class, description = "Minimum length of a valid publication abstract")
	private int abstractMinLength = 200;

	public static final String FULLTEXT_MIN_LENGTH = "fulltextMinLength";
	@Parameter(names = { "--" + FULLTEXT_MIN_LENGTH }, validateWith = PositiveInteger.class, description = "Minimum length of a valid publication full text")
	private int fulltextMinLength = 2000;

	public static final String WEBPAGE_MIN_LENGTH = "webpageMinLength";
	@Parameter(names = { "--" + WEBPAGE_MIN_LENGTH }, validateWith = PositiveInteger.class, description = "Minimum length of a valid web page content")
	private int webpageMinLength = 100;

	public static final String WEBPAGE_MIN_LENGTH_JAVASCRIPT = "webpageMinLengthJavascript";
	@Parameter(names = { "--" + WEBPAGE_MIN_LENGTH_JAVASCRIPT }, validateWith = PositiveInteger.class, description = "If length of web page content is below this, then refetching using JavaScript support will be attempted")
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
