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

import org.jsoup.helper.HttpConnection;

import com.beust.jcommander.Parameter;

public class FetcherArgs {
	@Parameter(names = { "--empty-cooldown" }, description = "TODO in minutes")
	private int emptyCooldown = 720; // 12 h

	@Parameter(names = { "--non-final-cooldown" }, description = "TODO in minutes")
	private int nonFinalCooldown = 10080; // a week

	@Parameter(names = { "--fetch-exception-cooldown" }, description = "TODO in minutes")
	private int fetchExceptionCooldown = 1440; // a day

	@Parameter(names = { "--retry-limit" }, description = "TODO")
	private int retryLimit = 3;

	@Parameter(names = { "--title-min-length" }, description = "TODO")
	private int titleMinLength = 4;

	@Parameter(names = { "--keywords-min-size" }, description = "TODO")
	private int keywordsMinSize = 2;

	@Parameter(names = { "--mined-terms-min-size" }, description = "TODO")
	private int minedTermsMinSize = 1;

	@Parameter(names = { "--abstract-min-length" }, description = "TODO")
	private int abstractMinLength = 200;

	@Parameter(names = { "--fulltext-min-length" }, description = "TODO")
	private int fulltextMinLength = 2000;

	@Parameter(names = { "--webpage-min-length" }, description = "TODO")
	private int webpageMinLength = 100; // TODO test default value

	@Parameter(names = { "--webpage-min-length-javascript" }, description = "TODO")
	private int webpageMinLengthJavascript = 200;

	@Parameter(names = { "--europepmc-email" }, description = "TODO")
	private String europepmcEmail = "";

	@Parameter(names = { "--oadoi-email" }, description = "TODO")
	private String oadoiEmail = "test@example.com";

	@Parameter(names = { "--user-agent" }, description = "TODO")
	private String userAgent = HttpConnection.DEFAULT_UA;

	@Parameter(names = { "--connection-timeout" }, description = "TODO")
	private int connectionTimeout = 15000; // ms

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

	public String getEuropepmcEmail() {
		return europepmcEmail;
	}
	public void setEuropepmcEmail(String europepmcEmail) {
		this.europepmcEmail = europepmcEmail;
	}

	public String getOadoiEmail() {
		return oadoiEmail;
	}
	public void setOadoiEmail(String oadoiEmail) {
		this.oadoiEmail = oadoiEmail;
	}

	public String getUserAgent() {
		return userAgent;
	}
	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public int getConnectionTimeout() {
		return connectionTimeout;
	}
	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}
}
