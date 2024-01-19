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

import java.io.File;

import com.beust.jcommander.Parameter;

public class FetcherPrivateArgs extends Args {

	private static final String europepmcEmailId = "europepmcEmail";
	private static final String europepmcEmailDescription = "E-mail to send to the Europe PMC API";
	private static final String europepmcEmailDefault = "";
	@Parameter(names = { "--" + europepmcEmailId }, description = europepmcEmailDescription)
	private String europepmcEmail = europepmcEmailDefault;

	private static final String oadoiEmailId = "oadoiEmail";
	private static final String oadoiEmailDescription = "E-mail to send to the oaDOI (Unpaywall) API";
	private static final String oadoiEmailDefault = "@";
	@Parameter(names = { "--" + oadoiEmailId }, description = oadoiEmailDescription)
	private String oadoiEmail = oadoiEmailDefault;

	private static final String userAgentId = "userAgent";
	private static final String userAgentDescription = "HTTP User-Agent";
	// better to use a Desktop UA, like Firefox ESR
	private static final String userAgentDefault = "Mozilla/5.0 (X11; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/115.0";
	@Parameter(names = { "--" + userAgentId }, description = userAgentDescription)
	private String userAgent = userAgentDefault;

	private static final String journalsYamlId = "journalsYaml";
	private static final String journalsYamlDescription = "YAML file containing custom journals scrape rules to add to default ones";
	private static final String journalsYamlDefault = "";
	@Parameter(names = { "--" + journalsYamlId }, description = journalsYamlDescription)
	private String journalsYaml = journalsYamlDefault;

	private static final String webpagesYamlId = "webpagesYaml";
	private static final String webpagesYamlDescription = "YAML file containing custom web page scrape rules to add to default ones";
	private static final String webpagesYamlDefault = "";
	@Parameter(names = { "--" + webpagesYamlId }, description = webpagesYamlDescription)
	private String webpagesYaml = webpagesYamlDefault;

	private static final String seleniumId = "selenium";
	private static final String seleniumDescription = "Enable Selenium WebDriver (using Firefox) for JavaScript execution instead of HtmlUnit";
	private static final Boolean seleniumDefault = true;
	@Parameter(names = { "--" + seleniumId }, arity = 1, description = seleniumDescription)
	private Boolean selenium = seleniumDefault;

	private static final String seleniumGeckodriverId = "seleniumGeckodriver";
	private static final String seleniumGeckodriverDescription = "Location of the geckodriver executable used by Selenium WebDriver. If not specified Selenium Manager will download it automatically. Specifying the location automatically enables Selenium WebDriver usage.";
	private static final String seleniumGeckodriverDefault = "";
	@Parameter(names = { "--" + seleniumGeckodriverId }, description = seleniumGeckodriverDescription)
	private String seleniumGeckodriver = seleniumGeckodriverDefault;

	private static final String seleniumFirefoxId = "seleniumFirefox";
	private static final String seleniumFirefoxDescription = "Location of the firefox executable used by Selenium WebDriver. Not necessary to specify if the wanted firefox is in PATH. Specifying the location automatically enables Selenium WebDriver usage.";
	private static final String seleniumFirefoxDefault = "";
	@Parameter(names = { "--" + seleniumFirefoxId }, description = seleniumFirefoxDescription)
	private String seleniumFirefox = seleniumFirefoxDefault;

	@Override
	protected void addArgs() {
		args.add(new Arg<>(this::getEuropepmcEmail, this::setEuropepmcEmail, europepmcEmailDefault, europepmcEmailId, "Europe PMC e-mail", europepmcEmailDescription, null));
		args.add(new Arg<>(this::getOadoiEmail, this::setOadoiEmail, oadoiEmailDefault, oadoiEmailId, "oaDOI e-mail", oadoiEmailDescription, null));
		args.add(new Arg<>(this::getUserAgent, this::setUserAgent, userAgentDefault, userAgentId, "User Agent", userAgentDescription, null));
		args.add(new Arg<>(this::getJournalsYamlFilename, this::setJournalsYaml, journalsYamlDefault, journalsYamlId, "Journals scrape rules", journalsYamlDescription, null));
		args.add(new Arg<>(this::getWebpagesYamlFilename, this::setWebpagesYaml, webpagesYamlDefault, webpagesYamlId, "Webpages scrape rules", webpagesYamlDescription, null));
		args.add(new Arg<>(this::isSelenium, this::setSelenium, seleniumDefault, seleniumId, "Selenium", seleniumDescription, null));
		args.add(new Arg<>(this::getSeleniumGeckodriver, this::setSeleniumGeckodriver, seleniumGeckodriverDefault, seleniumGeckodriverId, "Selenium geckodriver", seleniumGeckodriverDescription, null));
		args.add(new Arg<>(this::getSeleniumFirefox, this::setSeleniumFirefox, seleniumFirefoxDefault, seleniumFirefoxId, "Selenium firefox", seleniumFirefoxDescription, null));
	}

	@Override
	public String getId() {
		return "fetcherPrivateArgs";
	}

	@Override
	public String getLabel() {
		return "Fetching (private)";
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

	public String getJournalsYaml() {
		return journalsYaml;
	}
	public String getJournalsYamlFilename() {
		return new File(journalsYaml).getName();
	}
	public void setJournalsYaml(String journalsYaml) {
		this.journalsYaml = journalsYaml;
	}

	public String getWebpagesYaml() {
		return webpagesYaml;
	}
	public String getWebpagesYamlFilename() {
		return new File(webpagesYaml).getName();
	}
	public void setWebpagesYaml(String webpagesYaml) {
		this.webpagesYaml = webpagesYaml;
	}

	public Boolean isSelenium() {
		return selenium;
	}
	public void setSelenium(Boolean selenium) {
		this.selenium = selenium;
	}

	public String getSeleniumGeckodriver() {
		return seleniumGeckodriver;
	}
	public void setSeleniumGeckodriver(String seleniumGeckodriver) {
		this.seleniumGeckodriver = seleniumGeckodriver;
	}

	public String getSeleniumFirefox() {
		return seleniumFirefox;
	}
	public void setSeleniumFirefox(String seleniumFirefox) {
		this.seleniumFirefox = seleniumFirefox;
	}
}
