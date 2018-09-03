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

public class FetcherPrivateArgs {

	public static final String EUROPEPMC_EMAIL = "europepmcEmail";
	@Parameter(names = { "--" + EUROPEPMC_EMAIL }, description = "E-mail to send to the Europe PMC API")
	private String europepmcEmail = "";

	public static final String OADOI_EMAIL = "oadoiEmail";
	@Parameter(names = { "--" + OADOI_EMAIL }, description = "E-mail to send to the oaDOI (Unpaywall) API")
	private String oadoiEmail = "test";

	public static final String USER_AGENT = "userAgent";
	@Parameter(names = { "--" + USER_AGENT }, description = "HTTP User-Agent")
	// better to use a Desktop UA
	private String userAgent = "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:60.0) Gecko/20100101 Firefox/60.0";

	public static final String JOURNALS_YAML = "journalsYaml";
	@Parameter(names = { "--" + JOURNALS_YAML }, description = "YAML file containing custom journals scrape rules to add to default ones")
	private String journalsYaml = "";

	public static final String WEBPAGES_YAML = "webpagesYaml";
	@Parameter(names = { "--" + WEBPAGES_YAML }, description = "YAML file containing custom web page scrape rules to add to default ones")
	private String webpagesYaml = "";

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

	public String getJournalsYaml() {
		return journalsYaml;
	}
	public void setJournalsYaml(String journalsYaml) {
		this.journalsYaml = journalsYaml;
	}

	public String getWebpagesYaml() {
		return webpagesYaml;
	}
	public void setWebpagesYaml(String webpagesYaml) {
		this.webpagesYaml = webpagesYaml;
	}
}
