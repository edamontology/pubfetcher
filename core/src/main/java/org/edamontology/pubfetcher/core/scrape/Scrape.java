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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.regex.Pattern;

import org.yaml.snakeyaml.Yaml;

public class Scrape {

	private final Map<Pattern, String> regex;

	private final Map<String, Map<String, String>> site;

	private final Map<String, Boolean> javascript;

	private final Map<Pattern, Map<String, String>> webpages;

	@SuppressWarnings("unchecked")
	public Scrape() throws IOException, ParseException {

		String journal = "journal.yaml";
		try (BufferedReader br = new BufferedReader(new InputStreamReader(getResource(journal), StandardCharsets.UTF_8))) {
			Yaml yaml = new Yaml();
			Iterable<Object> it = yaml.loadAll(br);

			regex = makeRegex((Map<String, String>) it.iterator().next(), journal);

			site = (Map<String, Map<String, String>>) it.iterator().next();

			javascript = (Map<String, Boolean>) it.iterator().next();

			validateTooManySections(it, journal);

			validateJournal();
		}

		String webpages = "webpages.yaml";
		try (BufferedReader br = new BufferedReader(new InputStreamReader(getResource(webpages), StandardCharsets.UTF_8))) {
			Yaml yaml = new Yaml();
			Iterable<Object> it = yaml.loadAll(br);

			this.webpages = makeWebpages((Map<String, Map<String, String>>) it.iterator().next(), webpages);

			validateTooManySections(it, webpages);

			// TODO validateWebpages: title, content, javascript, license, language
		}
	}

	private InputStream getResource(String name) {
		InputStream resource = this.getClass().getResourceAsStream("/scrape/" + name);
		if (resource == null) {
			throw new MissingResourceException("Can't find scraping rules '" + name + "'!", this.getClass().getSimpleName(), name);
		}
		return resource;
	}

	private Map<Pattern, String> makeRegex(Map<String, String> regexString, String name) throws ParseException {
		Map<Pattern, String> regex = new HashMap<>();
		int i = 0;
		for (Map.Entry<String, String> r : regexString.entrySet()) {
			++i;
			String k = r.getKey();
			if (k.isEmpty()) {
				throw new ParseException("Regex cannot be empty in scraping rules '" + name + "'! (regex pos " + i + ")", i);
			}
			if (k.charAt(0) != '^') {
				k = "^https?://(www\\.)?" + k;
			}
			regex.put(Pattern.compile(k), r.getValue());
		}
		return regex;
	}

	private Map<Pattern, Map<String, String>> makeWebpages(Map<String, Map<String, String>> yaml, String name) throws ParseException {
		Map<Pattern, Map<String, String>> webpage = new HashMap<>();
		int i = 0;
		for (Map.Entry<String, Map<String, String>> r : yaml.entrySet()) {
			++i;
			String k = r.getKey();
			if (k.isEmpty()) {
				throw new ParseException("Regex key cannot be empty in scraping rules '" + name + "'! (pos " + i + ")", i);
			}
			if (k.charAt(0) != '^') {
				k = "^https?://(www\\.)?" + k;
			}
			webpage.put(Pattern.compile(k), r.getValue());
		}
		return webpage;
	}

	private void validateTooManySections(Iterable<Object> it, String name) throws ParseException {
		if (it.iterator().hasNext()) {
			throw new ParseException("Scraping rules '" + name + "' contains too many sections! (separated by ---)", 0);
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

			Map<String, Boolean> siteKeys = new HashMap<>();
			for (ScrapeSiteKey siteKey : ScrapeSiteKey.values()) {
				siteKeys.put(siteKey.toString(), false);
			}

			for (String k : s.getValue().keySet()) {
				if (siteKeys.containsKey(k)) {
					if (siteKeys.get(k)) {
						throw new ParseException("Key '" + k + "' defined more than once in site '" + s.getKey() + "'! (site pos " + i + ")", i);
					} else {
						siteKeys.put(k, true);
					}
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

			for (Map.Entry<String, String> v : s.getValue().entrySet()) {
				if (v.getValue() == null || v.getValue().trim().isEmpty()) {
					throw new ParseException("Value for key '" + v.getKey() + "' empty in site '" + s.getKey() + "'! (site pos " + i + ")", i);
				}
			}
		}
	}

	public String getSite(String url) {
		if (url == null || url.isEmpty()) return null;
		for (Pattern pattern : regex.keySet()) {
			if (pattern.matcher(url).find()) {
				return regex.get(pattern);
			}
		}
		return null;
	}

	public String getSelector(String site, ScrapeSiteKey siteKey) {
		if (site == null || this.site.get(site) == null) return null;
		return this.site.get(site).get(siteKey.toString());
	}

	public boolean getJavascript(String doiRegistrant) {
		if (doiRegistrant == null || doiRegistrant.isEmpty()) return false;
		Boolean js = javascript.get(doiRegistrant);
		return (js == null ? false : js);
	}

	public Map<String, String> getWebpage(String url) {
		if (url == null || url.isEmpty()) return null;
		for (Pattern pattern : webpages.keySet()) {
			if (pattern.matcher(url).find()) {
				return webpages.get(pattern);
			}
		}
		return null;
	}
}
