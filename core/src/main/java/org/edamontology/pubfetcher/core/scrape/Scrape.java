/*
 * Copyright © 2016, 2018 Erik Jaaniso
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

package org.edamontology.pubfetcher.core.scrape;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

public class Scrape {

	private static final Logger logger = LogManager.getLogger();

	private final Map<Pattern, String> regex = new LinkedHashMap<>();

	private final Map<String, Map<String, String>> site = new LinkedHashMap<>();

	private final List<Pattern> javascript = new ArrayList<>();

	private final List<Pattern> restart = new ArrayList<>();

	private final Map<Pattern, Map<String, String>> webpages = new LinkedHashMap<>();

	public Scrape(String journals, String webpages) throws IOException, ParseException {

		String journalsDefault = "journals.yaml";
		try (BufferedReader br = new BufferedReader(new InputStreamReader(getResource(journalsDefault), StandardCharsets.UTF_8))) {
			parseJournals(br, journalsDefault);
		}

		String webpagesDefault = "webpages.yaml";
		try (BufferedReader br = new BufferedReader(new InputStreamReader(getResource(webpagesDefault), StandardCharsets.UTF_8))) {
			parseWebpages(br, webpagesDefault);
		}

		if (journals != null && !journals.isEmpty()) {
			try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(journals), StandardCharsets.UTF_8))) {
				parseJournals(br, journals);
			}
		}

		if (webpages != null && !webpages.isEmpty()) {
			try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(webpages), StandardCharsets.UTF_8))) {
				parseWebpages(br, webpages);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void parseJournals(BufferedReader br, String journals) throws ParseException {
		Yaml yaml = new Yaml();
		Iterator<Object> it = yaml.loadAll(br).iterator();

		Map<String, String> regexSection = (Map<String, String>) nextSection(it, journals, 1, 4, Map.class);
		if (regexSection != null) {
			regex.putAll(makeRegex(regexSection, journals));
		}

		Map<String, Map<String, String>> siteSection = (Map<String, Map<String, String>>) nextSection(it, journals, 2, 4, Map.class);
		if (siteSection != null) {
			site.putAll(siteSection);
		}

		List<String> javascriptSection = (List<String>) nextSection(it, journals, 3, 4, List.class);
		if (javascriptSection != null) {
			javascript.addAll(makeJavascriptRestart(javascriptSection, journals, true));
		}

		List<String> restartSection = (List<String>) nextSection(it, journals, 4, 4, List.class);
		if (restartSection != null) {
			restart.addAll(makeJavascriptRestart(restartSection, journals, false));
		}

		validateTooManySections(it, journals, 4);

		validateJournal();
	}

	@SuppressWarnings("unchecked")
	private void parseWebpages(BufferedReader br, String webpages) throws ParseException {
		Yaml yaml = new Yaml();
		Iterator<Object> it = yaml.loadAll(br).iterator();

		Map<String, Map<String, String>> webpagesSection = (Map<String, Map<String, String>>) nextSection(it, webpages, 1, 1, Map.class);
		if (webpagesSection != null) {
			this.webpages.putAll(makeWebpages(webpagesSection, webpages));
		}

		validateTooManySections(it, webpages, 1);

		validateWebpages();
	}

	private InputStream getResource(String name) {
		InputStream resource = this.getClass().getResourceAsStream("/scrape/" + name);
		if (resource == null) {
			throw new MissingResourceException("Can't find scraping rules '" + name + "'!", this.getClass().getSimpleName(), name);
		}
		return resource;
	}

	private Map<Pattern, String> makeRegex(Map<String, String> regexString, String name) throws ParseException {
		Map<Pattern, String> regex = new LinkedHashMap<>();
		int i = 0;
		for (Map.Entry<String, String> r : regexString.entrySet()) {
			++i;
			String k;
			try {
				k = r.getKey();
			} catch (ClassCastException e) {
				throw new ParseException("Syntax error in regex in scraping rules '" + name + "'! (regex pos " + i + ")\n" + e, i);
			}
			if (k.isEmpty()) {
				throw new ParseException("Regex cannot be empty in scraping rules '" + name + "'! (regex pos " + i + ")", i);
			}
			String v;
			try {
				v = r.getValue();
			} catch (ClassCastException e) {
				throw new ParseException("Syntax error in regex value in scraping rules '" + name + "'! (regex pos " + i + ")\n" + e, i);
			}
			if (v == null || v.isEmpty()) {
				throw new ParseException("Regex value cannot be empty in scraping rules '" + name + "'! (regex pos " + i + ")", i);
			}
			if (k.charAt(0) != '^') {
				k = "(?i)^https?://(www\\.)?" + k;
			}
			regex.put(Pattern.compile(k), v);
		}
		return regex;
	}
	private List<Pattern> makeJavascriptRestart(List<String> javascriptRestartString, String name, Boolean javascript) throws ParseException {
		List<Pattern> javascriptRestart = new ArrayList<>();
		for (int i = 0; i < javascriptRestartString.size(); ++i) {
			String j;
			try {
				j = javascriptRestartString.get(i);
			} catch (ClassCastException e) {
				throw new ParseException("Syntax error in " + (javascript ? "javascript" : "restart")  + " regex in scraping rules '" + name + "'! (" + (javascript ? "javascript" : "restart") + " pos " + (i + 1) + ")\n" + e, i + 1);
			}
			if (j == null || j.isEmpty()) {
				throw new ParseException((javascript ? "Javascript" : "Restart") + " regex cannot be empty in scraping rules '" + name + "'! (" + (javascript ? "javascript" : "restart") + " pos " + (i + 1) + ")", i + 1);
			}
			if (j.charAt(0) != '^') {
				j = "(?i)^https?://(www\\.)?" + j;
			}
			javascriptRestart.add(Pattern.compile(j));
		}
		return javascriptRestart;
	}

	private Map<Pattern, Map<String, String>> makeWebpages(Map<String, Map<String, String>> yaml, String name) throws ParseException {
		Map<Pattern, Map<String, String>> webpage = new LinkedHashMap<>();
		int i = 0;
		for (Map.Entry<String, Map<String, String>> r : yaml.entrySet()) {
			++i;
			String k = r.getKey();
			if (k.isEmpty()) {
				throw new ParseException("Regex key cannot be empty in scraping rules '" + name + "'! (pos " + i + ")", i);
			}
			if (k.charAt(0) != '^') {
				k = "(?i)^https?://(www\\.)?" + k;
			}
			webpage.put(Pattern.compile(k), r.getValue());
		}
		return webpage;
	}

	@SuppressWarnings("unchecked")
	private <T> T nextSection(Iterator<Object> it, String name, int count, int required, Class<T> clazz) throws ParseException {
		if (!it.hasNext()) {
			throw new ParseException("Scraping rules '" + name + "' contains " + (count - 1) + " sections instead of required " + required + "! (separated by ---)", count);
		}
		T next = null;
		try {
			next = (T) it.next();
		} catch (ClassCastException | YAMLException e) {
			throw new ParseException("Syntax error in section " + count + " of scraping rules '" + name + "'!\n" + e, count);
		}
		return next;
	}

	private void validateTooManySections(Iterator<Object> it, String name, int required) throws ParseException {
		if (it.hasNext()) {
			throw new ParseException("Scraping rules '" + name + "' contains more sections than required " + required + "! (separated by ---)", required + 1);
		}
	}

	private void validateJournal() throws ParseException {
		int i = 0;
		for (Map.Entry<Pattern, String> r : regex.entrySet()) {
			++i;
			if (r.getValue() == null) {
				throw new ParseException("Regex '" + r.getKey() + "' is empty! (regex pos " + i + ")", i);
			}
			if (!site.containsKey(r.getValue())) {
				throw new ParseException("Definition of site '" + r.getValue() + "' missing! (regex pos " + i + ")", i);
			}
		}

		i = 0;
		for (Map.Entry<String, Map<String, String>> s : site.entrySet()) {
			++i;
			if (s.getValue() == null) {
				throw new ParseException("Site '" + s.getKey() + "' is empty! (site pos " + i + ")", i);
			}
			if (!regex.containsValue(s.getKey())) {
				throw new ParseException("Mapping for site '" + s.getKey() + "' missing! (site pos " + i + ")", i);
			}
		}

		i = 0;
		for (Map.Entry<String, Map<String, String>> s : site.entrySet()) {
			++i;

			Map<String, String> siteMap = null;
			try {
				siteMap = s.getValue();
			} catch (ClassCastException e) {
				throw new ParseException("Syntax error in site '" + s.getKey() + "'! (site pos " + i + ")\n" + e, i);
			}

			Map<String, Boolean> siteKeys = new HashMap<>();
			for (ScrapeSiteKey siteKey : ScrapeSiteKey.values()) {
				siteKeys.put(siteKey.toString(), false);
			}

			for (String k : siteMap.keySet()) {
				if (siteKeys.containsKey(k)) {
					siteKeys.put(k, true);
				} else {
					throw new ParseException("Unknown key '" + k + "' in site '" + s.getKey() + "'! (site pos " + i + ")", i);
				}
			}

			if (siteKeys.get("fulltext_src") && !siteKeys.get("fulltext_dst")
				|| siteKeys.get("fulltext_dst") && !siteKeys.get("fulltext_src")) {
				throw new ParseException("If one of { fulltext_src, fulltext_dst } is present, the other must too, in site '" + s.getKey() + "'! (site pos " + i + ")", i);
			}
			if (siteKeys.get("pdf_src") && !siteKeys.get("pdf_dst")
				|| siteKeys.get("pdf_dst") && !siteKeys.get("pdf_src")) {
				throw new ParseException("If one of { pdf_src, pdf_dst } is present, the other must too, in site '" + s.getKey() + "'! (site pos " + i + ")", i);
			}

			for (Map.Entry<String, String> v : siteMap.entrySet()) {
				if (v.getValue() == null || v.getValue().trim().isEmpty()) {
					throw new ParseException("Value for key '" + v.getKey() + "' empty in site '" + s.getKey() + "'! (site pos " + i + ")", i);
				}
			}
		}
	}

	private void validateWebpages() throws ParseException {
		Set<String> webpageKeys = Arrays.stream(ScrapeWebpageKey.values()).map(k -> k.toString()).collect(Collectors.toSet());

		int i = 0;
		for (Map.Entry<Pattern, Map<String, String>> w : webpages.entrySet()) {
			++i;

			Map<String, String> webpageMap = null;
			try {
				webpageMap = w.getValue();
			} catch (ClassCastException e) {
				throw new ParseException("Syntax error in webpage pattern '" + w.getKey() + "'! (pos " + i + ")\n" + e, i);
			}

			if (webpageMap == null) {
				throw new ParseException("Webpage pattern '" + w.getKey() + "' is empty! (pos " + i + ")", i);
			}

			for (String k : webpageMap.keySet()) {
				if (!webpageKeys.contains(k)) {
					throw new ParseException("Unknown key '" + k + "' in webpage pattern '" + w.getKey() + "'! (pos " + i + ")", i);
				}
			}
		}
	}

	public String getSite(String url) {
		try {
			url = new URL(url).toString();
		} catch (MalformedURLException e) {
			logger.error(e);
			return null;
		}
		String site = null;
		for (Pattern pattern : regex.keySet()) {
			if (pattern.matcher(url).find()) {
				site = regex.get(pattern);
			}
		}
		return site;
	}

	public String getSelector(String site, ScrapeSiteKey siteKey) {
		if (site == null || this.site.get(site) == null) return null;
		return this.site.get(site).get(siteKey.toString());
	}

	public boolean getJavascript(String url) {
		try {
			url = new URL(url).toString();
		} catch (MalformedURLException e) {
			logger.error(e);
			return false;
		}
		for (Pattern pattern : javascript) {
			if (pattern.matcher(url).find()) {
				return true;
			}
		}
		return false;
	}
	public boolean getRestart(String url) {
		try {
			url = new URL(url).toString();
		} catch (MalformedURLException e) {
			logger.error(e);
			return false;
		}
		for (Pattern pattern : restart) {
			if (pattern.matcher(url).find()) {
				return true;
			}
		}
		return false;
	}

	public Map<String, String> getWebpage(String url) {
		try {
			url = new URL(url).toString();
		} catch (MalformedURLException e) {
			logger.error(e);
			return null;
		}
		Map<String, String> webpage = null;
		for (Pattern pattern : webpages.keySet()) {
			if (pattern.matcher(url).find()) {
				webpage = webpages.get(pattern);
			}
		}
		return webpage;
	}
}
