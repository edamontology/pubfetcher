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

public class FetcherPrivateArgs {

	public static final String EUROPEPMC_EMAIL = "europepmc-email";
	@Parameter(names = { "--" + EUROPEPMC_EMAIL }, description = "E-mail to send to the Europe PMC API")
	private String europepmcEmail = "";

	public static final String OADOI_EMAIL = "oadoi-email";
	@Parameter(names = { "--" + OADOI_EMAIL }, description = "E-mail to send to the oaDOI API")
	private String oadoiEmail = "test@example.com";

	public static final String USER_AGENT = "user-agent";
	@Parameter(names = { "--" + USER_AGENT }, description = "HTTP User-Agent")
	private String userAgent = HttpConnection.DEFAULT_UA;

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
}
