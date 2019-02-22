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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.edamontology.pubfetcher.core.db.Database;
import org.edamontology.pubfetcher.core.db.publication.Publication;
import org.edamontology.pubfetcher.core.db.publication.PublicationIds;
import org.edamontology.pubfetcher.core.db.publication.PublicationPartName;
import org.edamontology.pubfetcher.core.db.webpage.Webpage;
import org.edamontology.pubfetcher.core.fetching.Fetcher;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Part of PubFetcher API
 * @author Erik Jaaniso
 */
public final class PubFetcher {

	private static final Logger logger = LogManager.getLogger();

	private static final int REDIRECT_LIMIT = 10;

	/** {@link Pattern} of a valid PubMed ID, without beginning (^) and end ($) of line boundary matchers. */
	public static final Pattern PMID = Pattern.compile("[1-9][0-9]*");
	private static final Pattern PMID_ONLY = Pattern.compile("^" + PMID.pattern() + "$");
	/** {@link Pattern} of a valid PubMed Central ID, without beginning (^) and end ($) of line boundary matchers. */
	public static final Pattern PMCID = Pattern.compile("PMC[1-9][0-9]*");
	private static final Pattern PMCID_ONLY = Pattern.compile("^" + PMCID.pattern() + "$");

	private static final String DOIprefixes = "http://doi.org/|https://doi.org/|http://dx.doi.org/|https://dx.doi.org/|doi:";
	private static final Pattern DOIprefix = Pattern.compile("^(" + DOIprefixes + ")");
	private static final String DOI_CORE = "(" + DOIprefixes + "|)10\\.\\p{Print}+/\\p{Print}*";
	/** {@link Pattern} of a valid Digital Object Identifier, without beginning (^) and end ($) of line boundary matchers. */
	public static final Pattern DOI = Pattern.compile("(?U)" + DOI_CORE);
	private static final Pattern DOI_ONLY = Pattern.compile("(?U)^" + DOI_CORE + "$");

	/** URL to add to the front of a valid PMID to get the web page of the article in PubMed. */
	public static final String PMIDlink = "https://www.ncbi.nlm.nih.gov/pubmed/?term=";
	/** URL to add to the front of a valid PMCID to get the web page of the article in PubMed Central. */
	public static final String PMCIDlink = "https://www.ncbi.nlm.nih.gov/pmc/articles/";
	/** URL to add to the front of a valid DOI with no prefix to get a DOI link which should resolve to the actual resource. */
	public static final String DOIlink = "https://doi.org/";

	/** URL to add to the front of a valid PMCID to get the web page of the article in Europe PMC. */
	public static final String EUROPEPMClink = "http://europepmc.org/articles/";

	private static final Pattern AMP = Pattern.compile("&");
	private static final Pattern LT = Pattern.compile("<");
	private static final Pattern GT = Pattern.compile(">");

	private static final Pattern NEWPARAGRAPH = Pattern.compile("\n\n");
	private static final Pattern NEWLINE = Pattern.compile("\n");

	private static Set<PublicationIds> activePubIds = new HashSet<>();
	private static Set<String> activeWebUrls = new HashSet<>();
	private static Set<String> activeDocUrls = new HashSet<>();

	private PubFetcher() {}

	/**
	 * Tests if the given String is a valid PubMed ID.
	 *
	 * @param s the string of a potential PMID
	 * @return <code>true</code> if the given string is a valid PMID; <code>false</code> otherwise
	 */
	public static boolean isPmid(String s) {
		if (s == null) return false;
		return PMID_ONLY.matcher(s).matches();
	}

	/**
	 * Tests if the given String is a valid PubMed Central ID.
	 *
	 * @param s the string of a potential PMCID
	 * @return <code>true</code> if the given string is a valid PMCID; <code>false</code> otherwise
	 */
	public static boolean isPmcid(String s) {
		if (s == null) return false;
		return PMCID_ONLY.matcher(s).matches();
	}

	/**
	 * Removes the prefix "PMC" from a valid PubMed Central ID.
	 *
	 * @param s the string of a potential PMCID
	 * @return the PMCID with the prefix "PMC" removed if the supplied PMCID was valid; an empty string otherwise
	 */
	public static String extractPmcid(String s) {
		if (!isPmcid(s)) return "";
		return s.substring(3);
	}

	/**
	 * Tests if the given String is a valid Digital Object Identifier.
	 *
	 * @param s the string of a potential DOI
	 * @return <code>true</code> if the given string is a valid DOI; <code>false</code> otherwise
	 */
	public static boolean isDoi(String s) {
		if (s == null) return false;
		return DOI_ONLY.matcher(s).matches();
	}

	/**
	 * Normalises the given Digital Object Identifier. Meaning that a valid DOI prefix (like "https://doi.org/" or "doi:") is removed from the DOI and all
	 * letters that are part of the 7-bit ASCII set are converted to uppercase. The validity of the input DOI is not checked, therefore an invalid DOI is
	 * output if an invalid DOI is supplied.
	 *
	 * @param s the string of a potential DOI
	 * @return the normalised DOI; or some invalid DOI if an invalid DOI was supplied
	 */
	public static String normaliseDoi(String s) {
		if (s == null || s.isEmpty()) return "";

		// http://www.doi.org/doi_handbook/2_Numbering.html#2.4
		// DOI names are case insensitive, using ASCII case folding for comparison of text.
		// (Case insensitivity for DOI names applies only to ASCII characters. DOI names which differ in the case of non-ASCII Unicode characters may be different identifiers.)
		// 10.123/ABC is identical to 10.123/AbC.
		// All DOI names are converted to upper case upon registration, which is a common practice for making any kind of service case insensitive.

		char[] c = DOIprefix.matcher(s).replaceFirst("").toCharArray();
		for (int i = 0; i < c.length; ++i) {
			if (c[i] >= 'a' && c[i] <= 'z') {
				c[i] -= 32;
			}
		}
		return new String(c);
	}

	/**
	 * Extracts the registrant code from the given Digital Object Identifier. The registrant ID is the substring after "10." and before the first "/".
	 *
	 * @param s the string of a potential DOI
	 * @return the string of the extracted registrant code; or an empty string if an invalid DOI was supplied
	 */
	public static String extractDoiRegistrant(String s) {
		if (!isDoi(s)) return "";
		String doiRegistrant = "";
		int begin = s.indexOf("10.");
		if (begin != -1) {
			int end = s.indexOf("/", begin + 3);
			if (end != -1) {
				doiRegistrant = s.substring(begin + 3, end);
			}
		}
		return doiRegistrant;
	}

	/**
	 * Escapes HTML characters. The necessary characters in the given string are escaped such that it can safely by used as text in a HTML document (without
	 * the string interacting with the document's markup).
	 *
	 * @param input the string to escape
	 * @return the escaped string
	 */
	public static String escapeHtml(String input) {
		return GT.matcher(LT.matcher(AMP.matcher(input).replaceAll("&amp;")).replaceAll("&lt;")).replaceAll("&gt;");
	}
	/**
	 * Escapes HTML attribute characters. The necessary characters in the given string are escaped such that it can safely by used as an HTML attribute value
	 * (without the string interacting with the document's markup).
	 *
	 * @param input the string to escape
	 * @return the escaped string
	 */
	public static String escapeHtmlAttribute(String input) {
		StringBuilder sb = new StringBuilder();
		for (char c : input.toCharArray()) {
			if (c < 128 && !(c >= '0' && c <= '9') && !(c >= 'A' && c <= 'Z') && !(c >= 'a' && c <= 'z')) {
				sb.append("&#").append((int) c).append(";");
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	/**
	 * Escapes the given string with {@link #escapeHtml} and converts the result to HTML. The conversion is done such that paragraphs are surrounded with
	 * &lt;p&gt; and &lt;/p&gt; (the separating two line breaks "\n\n" are removed) and line breaks inside paragraphs (i.e. one "\n") are replaced with
	 * &lt;br&gt;.
	 *
	 * @param input the string to convert to HTML paragraphs
	 * @return a HTML version of the input string
	 */
	public static String getParagraphsHtml(String input) {
		StringBuilder sb = new StringBuilder();
		sb.append("<p>");
		sb.append(NEWLINE.matcher(NEWPARAGRAPH.matcher(escapeHtml(input)).replaceAll("</p><p>")).replaceAll("<br>"));
		sb.append("</p>");
		return sb.toString();
	}

	/**
	 * Converts the given URL string to a HTML link. The given string will be output both as the text shown (escaped using {@link #escapeHtml}) and the
	 * destination using the <code>href</code> attribute (escaped using {@link #escapeHtmlAttribute}) in the output HTML. If the given URL string does not
	 * begin with schema "http://", "https://" or "ftp://", then it is only output as text (escaped using {@link #escapeHtml}).
	 *
	 * @param link the URL string to convert to a HTML link
	 * @return a HTML link (<code>&lt;a&gt;</code>) of the input string
	 */
	public static String getLinkHtml(String link) {
		if (link == null || link.isEmpty()) return "";
		if (link.startsWith("http://") || link.startsWith("https://") || link.startsWith("ftp://")) {
			return "<a href=\"" + escapeHtmlAttribute(link) + "\">" + escapeHtml(link) + "</a>";
		} else {
			return escapeHtml(link);
		}
	}
	/**
	 * Converts the given URL string and the given text string to a HTML link. The given text string will be output as the text shown (escaped using
	 * {@link #escapeHtml}) and the given URL string will be output as the destination using the <code>href</code> attribute (escaped using
	 * {@link #escapeHtmlAttribute}). If the given URL string does not begin with schema "http://", "https://" or "ftp://", then only the text string is
	 * output (escaped using {@link #escapeHtml}).
	 *
	 * @param href the URL string to convert to the destination of the HTML link
	 * @param text the text string to convert to the text shown as the HTML link
	 * @return a HTML link (<code>&lt;a&gt;</code>) of the input URL and text strings
	 */
	public static String getLinkHtml(String href, String text) {
		if (text == null || text.isEmpty()) return "";
		if (href == null || href.isEmpty()) return escapeHtml(text);
		if (href.startsWith("http://") || href.startsWith("https://") || href.startsWith("ftp://")) {
			return "<a href=\"" + escapeHtmlAttribute(href) + "\">" + escapeHtml(text) + "</a>";
		} else {
			return escapeHtml(text);
		}
	}

	/**
	 * Prepends {@link #PMIDlink} to a valid PMID to get the URL string of the web page of the article in PubMed.
	 *
	 * @param pmid the PMID to get a link for
	 * @return the URL string of the web page of the given PMID in PubMed; <code>null</code> if given PMID is invalid
	 */
	public static String getPmidLink(String pmid) {
		if (isPmid(pmid)) return PMIDlink + pmid;
		else return null;
	}
	/**
	 * Prepends {@link #PMCIDlink} to a valid PMCID to get the URL string of the web page of the article in PubMed Central.
	 *
	 * @param pmcid the PMCID to get a link for
	 * @return the URL string of the web page of the given PMCID in PubMed Central; <code>null</code> if given PMCID is invalid
	 */
	public static String getPmcidLink(String pmcid) {
		if (isPmcid(pmcid)) return PMCIDlink + pmcid + "/";
		else return null;
	}
	/**
	 * Prepends {@link #DOIlink} to a valid DOI to get the URL string of the DOI link which should resolve to the actual resource.
	 *
	 * @param doi the DOI to get a link for. The given DOI will be normalised.
	 * @return the URL string of the DOI; <code>null</code> if given DOI is invalid
	 */
	public static String getDoiLink(String doi) {
		if (isDoi(doi)) return DOIlink + normaliseDoi(doi);
		else return null;
	}

	/**
	 * Prepends {@link #PMIDlink} to a valid PMID to get the HTML link of the web page of the article in PubMed.
	 *
	 * @param pmid the PMID to get a link for
	 * @return the HTML link (<code>&lt;a&gt;</code>) of the web page of the given PMID in PubMed, where link text is the PMID; link destination will be
	 * missing if given PMID is invalid
	 */
	public static String getPmidLinkHtml(String pmid) {
		return getLinkHtml(getPmidLink(pmid), pmid);
	}
	/**
	 * Prepends {@link #PMCIDlink} to a valid PMCID to get the HTML link of the web page of the article in PubMed Central.
	 *
	 * @param pmcid the PMCID to get a link for
	 * @return the HTML link (<code>&lt;a&gt;</code>) of the web page of the given PMCID in PubMed Central, where link text is the PMCID; link destination
	 * will be missing if given PMCID is invalid
	 */
	public static String getPmcidLinkHtml(String pmcid) {
		return getLinkHtml(getPmcidLink(pmcid), pmcid);
	}
	/**
	 * Prepends {@link #DOIlink} to a valid DOI to get the HTML link of the DOI which should resolve to the actual resource.
	 *
	 * @param doi the DOI to get a link for. The given DOI will be normalised.
	 * @return the HTML link (<code>&lt;a&gt;</code>) of the DOI, where link text is the DOI; link destination will be missing if given DOI is invalid
	 */
	public static String getDoiLinkHtml(String doi) {
		return getLinkHtml(getDoiLink(doi), doi);
	}

	/**
	 * Gets the URL string corresponding to the given publication ID. Prepends {@link #PMIDlink}, {@link #PMCIDlink} or {@link #DOIlink} to the first
	 * non-empty and valid ID (i.e., the PMID, the PMCID or the normalised DOI of the publication) in the given publication ID.
	 *
	 * @param publicationIds the {@link PublicationIds} to get an URL string for
	 * @return the URL string for the first found valid ID of publication ID; <code>null</code> if there are no valid IDs in the given publication ID
	 */
	public static String getIdLink(PublicationIds publicationIds) {
		if (publicationIds == null) return null;
		String link = getPmidLink(publicationIds.getPmid());
		if (link == null) link = getPmcidLink(publicationIds.getPmcid());
		if (link == null) link = getDoiLink(publicationIds.getDoi());
		return link;
	}
	/**
	 * Gets the HTML link corresponding to the given publication ID. Prepends {@link #PMIDlink}, {@link #PMCIDlink} or {@link #DOIlink} to the first
	 * non-empty and valid ID (i.e., the PMID, the PMCID or the normalised DOI of the publication) in the given publication ID.
	 *
	 * @param publicationIds the {@link PublicationIds} to get a HTML link for
	 * @return the HTML link (<code>&lt;a&gt;</code>) for the first found valid ID of publication ID, where link text is the ID; an empty string if there are
	 * no valid IDs in the given publication ID
	 */
	public static String getIdLinkHtml(PublicationIds publicationIds) {
		return getLinkHtml(getIdLink(publicationIds));
	}
	/**
	 * Gets the HTML link corresponding to the given publication ID, with link text equal to the given text string. Prepends {@link #PMIDlink},
	 * {@link #PMCIDlink} or {@link #DOIlink} to the first non-empty and valid ID (i.e., the PMID, the PMCID or the normalised DOI of the publication) in the
	 * given publication ID.
	 *
	 * @param publicationIds the {@link PublicationIds} to get a HTML link for
	 * @param text the string to use as the text of the HTML link
	 * @return the HTML link (<code>&lt;a&gt;</code>) for the first found valid ID of publication ID, where link text is the supplied text; link destination
	 * will be missing if there are no valid IDs in the given publication ID
	 */
	public static String getIdLinkHtml(PublicationIds publicationIds, String text) {
		return getLinkHtml(getIdLink(publicationIds), text);
	}

	/**
	 * Converts the given string, which must be either a PMID, a PMCID or a DOI, to the {@link PublicationIds} structure.
	 *
	 * @param publicationId a string representing a PMID, a PMCID or a DOI
	 * @param url the string of the URL where the given PMID, PMCID or DOI was found
	 * @param throwException if <code>true</code> then an {@link IllegalRequestException} will be thrown if the given publication ID string is not a valid ID;
	 * if <code>false</code> then only an error message will be logged if the ID is invalid
	 * @return a {@link PublicationIds} for the given publication ID string and ID provenance URL; <code>null</code> if the given ID is not valid
	 * @throws IllegalRequestException if the given ID is invalid and <code>throwException</code> is <code>true</code>
	 */
	public static PublicationIds getPublicationIds(String publicationId, String url, boolean throwException) throws IllegalRequestException {
		if (publicationId == null || publicationId.trim().isEmpty()) return null;
		PublicationIds publicationIds =
			isPmid(publicationId) ? new PublicationIds(publicationId, null, null, url, null, null) : (
			isPmcid(publicationId) ? new PublicationIds(null, publicationId, null, null, url, null) : (
			isDoi(publicationId) ? new PublicationIds(null, null, publicationId, null, null, url) : (
			null)));
		if (publicationIds == null) {
			if (throwException) {
				throw new IllegalRequestException("Invalid publication ID: " + publicationId);
			} else {
				logger.error("Invalid publication ID: {}", publicationId);
			}
		}
		return publicationIds;
	}

	/**
	 * Converts the given PMID, PMCID and DOI strings to the {@link PublicationIds} structure.
	 *
	 * @param pmid the string representing the PMID of the publication
	 * @param pmcid the string representing the PMCID of the publication
	 * @param doi the string representing the DOI of the publication
	 * @param url the string of the URL where the given PMID, PMCID and DOI were found
	 * @param throwException if <code>true</code> then an {@link IllegalRequestException} will be thrown if any of the given non-empty PMID, PMCID or DOI is
	 * not valid or no valid IDs are given; if <code>false</code> then only error messages will be logged for invalid IDs
	 * @param logEmpty if <code>true</code> then an error message will be logged if no valid IDs are given
	 * @return a {@link PublicationIds} for the given PMID, PMCID and DOI string and ID provenance URL; <code>null</code> if no valid IDs are given
	 * @throws IllegalRequestException if any of the given non-empty PMID, PMCID or DOI is not valid or no valid IDs are given, and
	 * <code>throwException</code> is <code>true</code>
	 */
	public static PublicationIds getPublicationIds(String pmid, String pmcid, String doi, String url, boolean throwException, boolean logEmpty) throws IllegalRequestException {
		if (pmid == null || pmid.trim().isEmpty()) pmid = null;
		else if (!isPmid(pmid)) {
			logger.error("Invalid PMID: {}", pmid);
			if (throwException) {
				throw new IllegalRequestException("Invalid PMID: " + pmid);
			}
			pmid = null;
		}
		if (pmcid == null || pmcid.trim().isEmpty()) pmcid = null;
		else if (!isPmcid(pmcid)) {
			logger.error("Invalid PMCID: {}", pmcid);
			if (throwException) {
				throw new IllegalRequestException("Invalid PMCID: " + pmcid);
			}
			pmcid = null;
		}
		if (doi == null || doi.trim().isEmpty()) doi = null;
		else if (!isDoi(doi)) {
			logger.error("Invalid DOI: {}", doi);
			if (throwException) {
				throw new IllegalRequestException("Invalid DOI: " + doi);
			}
			doi = null;
		}
		if (pmid == null && pmcid == null && doi == null) {
			if (logEmpty) {
				logger.error("Publication ID is empty");
			}
			if (throwException) {
				throw new IllegalRequestException("Publication ID is empty");
			}
			return null;
		}
		return new PublicationIds(pmid, pmcid, doi,
			pmid == null ? null : url, pmcid == null ? null : url, doi == null ? null : url);
	}

	/**
	 * Checks if the given string is a valid URL. Outputs the same string or the string of an equivalent URL (an error message is logged if the input string
	 * is changed to an equivalent one).
	 *
	 * @param url the string representing the URL of the webpage or doc
	 * @param throwException if <code>true</code> then an {@link IllegalRequestException} will be thrown if the given string is not a valid URL; if
	 * <code>false</code> then only an error message will be logged if the URL is invalid
	 * @return a string representing a valid URL, being equal or equivalent to the given string; <code>null</code> if the given URL is not valid
	 * @throws IllegalRequestException if the given URL is not valid and <code>throwException</code> is <code>true</code>
	 */
	public static String getUrl(String url, boolean throwException) throws IllegalRequestException {
		if (url == null || url.trim().isEmpty()) return null;
		try {
			String newUrl = new URL(url).toString();
			if (!newUrl.equals(url)) {
				logger.error("URL changed from {} to {}", url, newUrl);
			}
			return newUrl;
		} catch (MalformedURLException e) {
			if (throwException) {
				throw new IllegalRequestException("Malformed URL: " + url);
			} else {
				logger.error("Malformed URL: {}", url);
			}
			return null;
		}
	}

	private static boolean isActivePubId(PublicationIds pubId) {
		if (pubId == null) return false;
		for (PublicationIds activePubId : activePubIds) {
			if (pubId.getPmid() != null && !pubId.getPmid().isEmpty() && pubId.getPmid().equals(activePubId.getPmid())
				|| pubId.getPmcid() != null && !pubId.getPmcid().isEmpty() && pubId.getPmcid().equals(activePubId.getPmcid())
				|| pubId.getDoi() != null && !pubId.getDoi().isEmpty() && pubId.getDoi().equals(activePubId.getDoi())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Gets a publication.
	 * <p>
	 * If a {@link Database} is given, then the publication is first got from that database, if present for given {@link PublicationIds}. Then, if a
	 * {@link Fetcher} is given, the publication is fetched if {@link DatabaseEntry#canFetch(FetcherArgs)} returns <code>true</code> for the publication. If a
	 * publication was fetched, it is put to the given Database, overwriting the previous publication entry there (if it was present).
	 * <p>
	 * The method is thread safe, that is, there are locks in place that prevent running it concurrently for PublicationIds with some equal IDs because of
	 * potential race conditions with the shared Database.
	 * <p>
	 * The method is the preferred way for getting a {@link Publication}.
	 *
	 * @param publicationIds the PublicationIds to get a publication for
	 * @param database the Database to get the publication from, and where to put a fetched publication. If <code>null</code>, then the publication is only
	 * fetched.
	 * @param fetcher the Fetcher to use for fetching the publication. If <code>null</code>, then the publication is only got from the database.
	 * @param parts a map where publication parts, which will be fetched, are set to <code>true</code>. If <code>null</code>, then all publication parts will
	 * be fetched.
	 * @param fetcherArgs the {@link FetcherArgs} to use for fetching
	 * @return the Publication corresponding to the given PublicationIds; <code>null</code> if PublicationIds is <code>null</code> or contains no valid IDs or
	 * Fetcher is <code>null</code> and the publication was not found in the database
	 */
	public static Publication getPublication(PublicationIds publicationIds, Database database, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) {
		if (publicationIds == null) {
			logger.error("null IDs given for getting publication");
			return null;
		}
		synchronized(activePubIds) {
			boolean waited = false;
			while (isActivePubId(publicationIds)) {
				waited = true;
				logger.info("Waiting behind Publication ID {}", publicationIds);
				try {
					activePubIds.wait();
				} catch (InterruptedException e) {
					logger.error("Interrupt!", e);
					Thread.currentThread().interrupt();
					return null;
				}
			}
			if (waited) {
				logger.info("Resuming for Publication ID {}", publicationIds);
			}
			activePubIds.add(publicationIds);
		}
		try {
			Publication publication = null;
			if (database != null) {
				publication = database.getPublication(publicationIds);
			}
			if (fetcher != null) {
				if (publication == null) {
					publication = fetcher.initPublication(publicationIds, fetcherArgs);
				}
				if (publication != null) {
					if (fetcher.getPublication(publication, parts, fetcherArgs)) {
						if (database != null) {
							database.putPublication(publication);
							database.commit();
						}
					}
				}
			}
			return publication;
		} finally {
			synchronized(activePubIds) {
				activePubIds.remove(publicationIds);
				activePubIds.notifyAll();
			}
		}
	}

	/**
	 * Gets a webpage.
	 * <p>
	 * If a {@link Database} is given, then the webpage is first got from that database, if present for given URL. Then, if a {@link Fetcher} is given, the
	 * webpage is fetched if {@link DatabaseEntry#canFetch(FetcherArgs)} returns <code>true</code> for the webpage. If a webpage was fetched, it is put to the
	 * given Database, overwriting the previous webpage entry there (if it was present).
	 * <p>
	 * The method is thread safe, that is, there are locks in place that prevent running it concurrently for equal URLs because of potential race conditions
	 * with the shared Database.
	 * <p>
	 * The method is the preferred way for getting a {@link Webpage}.
	 *
	 * @param webpageUrl the string of the URL to get a webpage for
	 * @param database the Database to get the webpage from, and where to put a fetched webpage. If <code>null</code>, then the webpage is only fetched.
	 * @param fetcher the Fetcher to use for fetching the webpage. If <code>null</code>, then the webpage is only got from the database.
	 * @param fetcherArgs the {@link FetcherArgs} to use for fetching
	 * @return the Webpage corresponding to the given URL; <code>null</code> if the URL is <code>null</code> or malformed or if Fetcher is <code>null</code>
	 * and the webpage was not found in the database
	 */
	public static Webpage getWebpage(String webpageUrl, Database database, Fetcher fetcher, FetcherArgs fetcherArgs) {
		if (webpageUrl == null) {
			logger.error("null start URL given for getting webpage");
			return null;
		}
		synchronized(activeWebUrls) {
			boolean waited = false;
			while (activeWebUrls.contains(webpageUrl)) {
				waited = true;
				logger.info("Waiting behind Webpage URL {}", webpageUrl);
				try {
					activeWebUrls.wait();
				} catch (InterruptedException e) {
					logger.error("Interrupt!", e);
					Thread.currentThread().interrupt();
					return null;
				}
			}
			if (waited) {
				logger.info("Resuming for Webpage URL {}", webpageUrl);
			}
			activeWebUrls.add(webpageUrl);
		}
		try {
			Webpage webpage = null;
			if (database != null) {
				webpage = database.getWebpage(webpageUrl);
			}
			if (fetcher != null) {
				if (webpage == null) {
					webpage = fetcher.initWebpage(webpageUrl);
				}
				if (webpage != null) {
					if (fetcher.getWebpage(webpage, fetcherArgs)) {
						if (database != null) {
							database.putWebpage(webpage);
							database.commit();
						}
					}
				}
			}
			return webpage;
		} finally {
			synchronized(activeWebUrls) {
				activeWebUrls.remove(webpageUrl);
				activeWebUrls.notifyAll();
			}
		}
	}

	/**
	 * Gets a doc.
	 * <p>
	 * If a {@link Database} is given, then the doc is first got from that database, if present for given URL. Then, if a {@link Fetcher} is given, the
	 * doc is fetched if {@link DatabaseEntry#canFetch(FetcherArgs)} returns <code>true</code> for the doc. If a doc was fetched, it is put to the
	 * given Database, overwriting the previous doc entry there (if it was present).
	 * <p>
	 * The method is thread safe, that is, there are locks in place that prevent running it concurrently for equal URLs because of potential race conditions
	 * with the shared Database.
	 * <p>
	 * The method is the preferred way for getting a doc.
	 *
	 * @param docUrl the string of the URL to get a doc for
	 * @param database the Database to get the doc from, and where to put a fetched doc. If <code>null</code>, then the doc is only fetched.
	 * @param fetcher the Fetcher to use for fetching the doc. If <code>null</code>, then the doc is only got from the database.
	 * @param fetcherArgs the {@link FetcherArgs} to use for fetching
	 * @return the doc corresponding to the given URL; <code>null</code> if the URL is <code>null</code> or malformed or if Fetcher is <code>null</code>
	 * and the doc was not found in the database
	 */
	public static Webpage getDoc(String docUrl, Database database, Fetcher fetcher, FetcherArgs fetcherArgs) {
		if (docUrl == null) {
			logger.error("null start URL given for getting doc");
			return null;
		}
		synchronized(activeDocUrls) {
			boolean waited = false;
			while (activeDocUrls.contains(docUrl)) {
				waited = true;
				logger.info("Waiting behind Doc URL {}", docUrl);
				try {
					activeDocUrls.wait();
				} catch (InterruptedException e) {
					logger.error("Interrupt!", e);
					Thread.currentThread().interrupt();
					return null;
				}
			}
			if (waited) {
				logger.info("Resuming for Doc URL {}", docUrl);
			}
			activeDocUrls.add(docUrl);
		}
		try {
			Webpage doc = null;
			if (database != null) {
				doc = database.getDoc(docUrl);
			}
			if (fetcher != null) {
				if (doc == null) {
					doc = fetcher.initWebpage(docUrl);
				}
				if (doc != null) {
					if (fetcher.getWebpage(doc, fetcherArgs)) {
						if (database != null) {
							database.putDoc(doc);
							database.commit();
						}
					}
				}
			}
			return doc;
		} finally {
			synchronized(activeDocUrls) {
				activeDocUrls.remove(docUrl);
				activeDocUrls.notifyAll();
			}
		}
	}

	/**
	 * Gets all {@link PublicationIds} from the given text files. Each line is expected to be in the form "&lt;pmid&gt;\t&lt;pmcid&gt;\t&lt;doi&gt;". The
	 * presence of the two tab characters is required, but the validity of the IDs is not checked. Empty lines and lines starting with the "#" character are
	 * ignored. The order of PublicationIds is preserved.
	 *
	 * @param files a list of text files to read the PublicationIds from
	 * @param pubIdSource a string that will be used as the provenance URL for the IDs in the PublicationIds
	 * @return a list of PublicationIds read from the given files
	 * @throws IOException if some I/O exception occurred while reading a file
	 */
	public static List<PublicationIds> pubFile(List<String> files, String pubIdSource) throws IOException {
		List<PublicationIds> publicationIds = new ArrayList<>();
		logger.info("Load publication IDs from file {}", files);
		for (String file : files) {
			try (Stream<String> lines = Files.lines(Paths.get(file), StandardCharsets.UTF_8)) {
				publicationIds.addAll(lines
					.filter(l -> !l.isEmpty() && !l.startsWith("#"))
					.map(l -> l.split("\t", -1))
					.filter(l -> {
						if (l.length != 3) logger.error("Line containing {} tabs instead of required 2 in {}: {}", l.length - 1, file, Arrays.stream(l).collect(Collectors.joining(" \\t ")));
						return l.length == 3;
					})
					.map(l -> new PublicationIds(l[0], l[1], l[2], pubIdSource, pubIdSource, pubIdSource))
					.collect(Collectors.toList()));
			}
		}
		logger.info("Loaded {} publication IDs", publicationIds.size());
		return publicationIds;
	}

	/**
	 * Gets publications from the given database for the {@link PublicationIds} read with {@link #pubFile} from the given text files. The order of
	 * Publications is preserved.
	 *
	 * @param database the path string of the {@link Database} to read the publications from
	 * @param pubFile a list of text files to read the PublicationIds from
	 * @param pubIdSource a string that will be used as the provenance URL for the IDs in the PublicationIds
	 * @return a list of Publications got from given database for PublicationIds read from given files
	 * @throws IOException if some I/O exception occurred while reading a file or opening the database
	 */
	public static List<Publication> getPublications(String database, List<String> pubFile, String pubIdSource) throws IOException {
		List<Publication> publications = new ArrayList<>();
		try (Database db = new Database(database)) {
			for (PublicationIds publicationIds : pubFile(pubFile, pubIdSource)) {
				Publication publication = db.getPublication(publicationIds);
				if (publication != null) {
					publications.add(publication);
				}
			}
		}
		logger.info("Loaded {} publications", publications.size());
		return publications;
	}

	/**
	 * Gets all webpage/doc URLs from the given text files. Each line is expected to be an URL, but the validity of these URLs is not checked. Empty lines and
	 * lines starting with the "#" character are ignored. The order of URLs is preserved.
	 *
	 * @param files a list of text files to read the URLs from
	 * @return a list of webpage/doc URLs read from the given files
	 * @throws IOException if some I/O exception occurred while reading a file
	 */
	public static List<String> webFile(List<String> files) throws IOException {
		List<String> webpageUrls = new ArrayList<>();
		logger.info("Load webpage/doc URLs from file {}", files);
		for (String file : files) {
			try (Stream<String> lines = Files.lines(Paths.get(file), StandardCharsets.UTF_8)) {
				webpageUrls.addAll(lines.filter(l -> !l.isEmpty() && !l.startsWith("#")).collect(Collectors.toList()));
			}
		}
		logger.info("Loaded {} webpage/doc URLs", webpageUrls.size());
		return webpageUrls;
	}

	/**
	 * Outputs a string describing the progress of some countable operation. The progress is specified by the given parameters. In the beginning of the
	 * string, a progress bar is drawn, then the completion percentage is output and as last the estimated time remaining is output.
	 *
	 * @param i number of the item currently processed as part of the operation. The first item has number 1.
	 * @param size number of items to process as part of the operation
	 * @param start time when the operation started. In milliseconds elapsed since January 1, 1970 UTC.
	 * @return the progress string for the given item number
	 */
	public static String progress(int i, int size, long start) {
		StringBuilder sb = new StringBuilder();
		sb.append("|");
		final int length = 25;
		int done = (int) (i / (double) size * length);
		for (int j = 0; j < done; ++j) sb.append("=");
		if (done < length) sb.append(">");
		for (int j = 0; j < length - done - 1; ++j) sb.append(" ");
		sb.append("|");
		sb.append(" ").append(i).append("/").append(size);
		sb.append(" (").append(Math.floor(i / (double) size * 1000) / 10.0).append("%");
		if (i > 1) {
			sb.append(", remaining ");
			long s = (System.currentTimeMillis() - start) / (i - 1) * (size - i + 1) / 1000;
			sb.append(String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60));
		}
		sb.append(")");
		return sb.toString();
	}

	/**
	 * Open a connection to the given URL. Redirects are followed in case of a HTTP connection, with a limit of {@value #REDIRECT_LIMIT} redirections. Written
	 * partly because Java doesn't redirect from HTTP to HTTPS.
	 *
	 * @param path the string of the URL to connect to
	 * @param timeout the value of the connect timeout and the read timeout, in milliseconds
	 * @param userAgent the value set as the User-Agent string in the HTTP request headers
	 * @return an {@link URLConnection} for the given URL with all redirections followed
	 * @throws MalformedURLException if the given URL is invalid
	 * @throws IOException if some I/O exception occurred while opening the connection
	 */
	public static URLConnection newConnection(String path, int timeout, String userAgent) throws MalformedURLException, IOException {
		URL url = new URL(path);
		URLConnection con = url.openConnection();
		con.setConnectTimeout(timeout);
		con.setReadTimeout(timeout);
		if (con instanceof HttpURLConnection) {
			CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
			int i = 0;
			while (con instanceof HttpURLConnection) {
				HttpURLConnection httpCon = (HttpURLConnection) con;
				httpCon.setRequestProperty("User-Agent", userAgent);
				httpCon.setRequestProperty("Referer", url.getProtocol() + "://" + url.getAuthority());
				// Java doesn't automatically redirect from http to https
				if (httpCon.getResponseCode() >= 300 && httpCon.getResponseCode() < 400) {
					++i;
					if (i > REDIRECT_LIMIT) {
						logger.error("Too many redirects ({}) in {} (last location {})", i, path, httpCon.getURL());
						break;
					}
					URL next = new URL(url, httpCon.getHeaderField("Location"));
					con = next.openConnection();
					con.setConnectTimeout(timeout);
					con.setReadTimeout(timeout);
				} else {
					break;
				}
			}
		}
		return con;
	}

	/**
	 * Gets all valid lines from the specified resource file. Any leading and trailing whitespace is removed from each line. Empty lines and lines beginning
	 * with the character "#" are ignored.
	 *
	 * @param clazz the class that the resource is associated with
	 * @param name the absolute name of the resource
	 * @return list of valid lines from the given resource file
	 * @throws IOException if some I/O exception occurred while reading the resource file
	 */
	public static List<String> getResource(Class<?> clazz, String name) throws IOException {
		InputStream resource = clazz.getResourceAsStream("/" + name);
		if (resource == null) {
			throw new MissingResourceException("Can't find resource '" + name + "'!", clazz.getSimpleName(), name);
		}
		try (BufferedReader br = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8))) {
			return br.lines()
				.map(String::trim)
				.filter(s -> !s.isEmpty() && !s.startsWith("#"))
				.collect(Collectors.toList());
		}
	}

	/**
	 * Converts the given path string to a {@link Path} while checking that certain conditions are met. Namely, the specified path must not be empty and the
	 * parent directory of the specified path must be writeable. Additionally, the specified path must not be an existing directory and must be a writeable
	 * file, if it exists.
	 *
	 * @param file the path string to convert
	 * @return the Path corresponding to the given path string
	 * @throws IOException if some of the specified conditions are not met
	 */
	public static Path outputPath(String file) throws IOException {
		return outputPath(file, false, false);
	}

	/**
	 * Converts the given path string to a {@link Path} while checking that certain conditions are met. Namely, the specified path must not be empty and the
	 * parent directory of the specified path must be writeable. Other conditions depend on the value of the <code>directory</code> and
	 * <code>existingDirectory</code> parameters.
	 *
	 * @param file the path string to convert
	 * @param directory if <code>true</code> (and <code>existingDirectory</code> is <code>false</code>), then path must not be an existing directory or file;
	 * if <code>false</code>, then path must not be an existing directory and must be a writeable file, if it exists
	 * @param existingDirectory if <code>true</code>, while <code>directory</code> is also <code>true</code>, then path must be a writeable directory
	 * @return the Path corresponding to the given path string
	 * @throws IOException if some of the specified conditions are not met
	 */
	public static Path outputPath(String file, boolean directory, boolean existingDirectory) throws IOException {
		if (file == null || file.isEmpty()) {
			throw new FileNotFoundException("Empty path given!");
		}

		Path path = Paths.get(file);
		Path parent = (path.getParent() != null ? path.getParent() : Paths.get("."));
		if (!Files.isDirectory(parent) || !Files.isWritable(parent)) {
			throw new AccessDeniedException(parent.toAbsolutePath().normalize() + " is not a writeable directory!");
		}
		if (directory && existingDirectory) {
			if (!Files.isDirectory(path) || !Files.isWritable(path)) {
				throw new AccessDeniedException(path.toAbsolutePath().normalize() + " is not a writeable directory!");
			}
		} else {
			if (Files.isDirectory(path)) {
				throw new FileAlreadyExistsException(path.toAbsolutePath().normalize() + " is an existing directory!");
			}
			if (directory) {
				if (Files.exists(path)) {
					throw new FileAlreadyExistsException(path.toAbsolutePath().normalize() + " is an existing file!");
				}
			} else {
				if (Files.exists(path) && !Files.isWritable(path)) {
					throw new AccessDeniedException(path.toAbsolutePath().normalize() + " is not a writeable file!");
				}
			}
		}
		return path;
	}

	/**
	 * Constructs a {@link JsonGenerator} that can be used to output publications, webpages and docs in JSON to a file or a string. The output is pretty
	 * printed. The constructed JsonGenerator can be supplied to the {@link Publication#toStringJson}, {@link Webpage#toStringJson} and similar methods.
	 *
	 * @param path the string path of the file to output the JSON to; if <code>null</code> then JSON will be output to a StringWriter instead
	 * @param writer the {@link StringWriter} that JSON will be output to. Used if <code>path</code> is <code>null</code>. The StringWriter can be read out as
	 * a string when finished.
	 * @return a new JsonGenerator
	 * @throws IOException if some I/O exception occurred while creating the generator
	 */
	public static JsonGenerator getJsonGenerator(String path, StringWriter writer) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		mapper.enable(SerializationFeature.CLOSE_CLOSEABLE);
		JsonFactory factory = mapper.getFactory();
		JsonGenerator generator;
		if (path == null) {
			generator = factory.createGenerator(writer);
		} else {
			generator = factory.createGenerator(Paths.get(path).toFile(), JsonEncoding.UTF8);
		}
		generator.useDefaultPrettyPrinter();
		return generator;
	}

	/**
	 * Writes the start of a JSON using the supplied {@link JsonGenerator}, including the program {@link Version} and arguments.
	 *
	 * @param generator the JsonGenerator to start
	 * @param version the Version to include at the start of the JSON
	 * @param argv the program arguments, as an array of strings, to include at the start of the JSON
	 * @throws IOException if some I/O exception occurred while writing to the JsonGenerator
	 */
	public static void jsonBegin(JsonGenerator generator, Version version, String[] argv) throws IOException {
		generator.writeStartObject();
		generator.writeObjectField("version", version);
		generator.writeObjectField("argv", argv);
	}

	/**
	 * Writes the end of a JSON using the supplied {@link JsonGenerator}.
	 *
	 * @param generator the JsonGenerator to end
	 * @throws IOException if some I/O exception occurred while writing to the JsonGenerator
	 */
	public static void jsonEnd(JsonGenerator generator) throws IOException {
		generator.writeEndObject();
	}
}
