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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumMap;
import java.util.regex.Pattern;

public final class FetcherCommon {

	private static int REDIRECT_LIMIT = 10;

	static final Pattern PMID = Pattern.compile("[1-9][0-9]*");
	private static final Pattern PMID_ONLY = Pattern.compile("^" + PMID.pattern() + "$");
	static final Pattern PMCID = Pattern.compile("PMC[1-9][0-9]*");
	private static final Pattern PMCID_ONLY = Pattern.compile("^" + PMCID.pattern() + "$");

	private static final String DOIprefixes = "http://doi.org/|https://doi.org/|http://dx.doi.org/|https://dx.doi.org/|doi:";
	private static final Pattern DOIprefix = Pattern.compile("^(" + DOIprefixes + ")");
	private static final String DOI_CORE = "(" + DOIprefixes + "|)10\\.\\p{Print}+/\\p{Print}*";
	static final Pattern DOI = Pattern.compile("(?U)" + DOI_CORE);
	private static final Pattern DOI_ONLY = Pattern.compile("(?U)^" + DOI_CORE + "$");

	static final String PMIDlink = "https://www.ncbi.nlm.nih.gov/pubmed/?term=";
	static final String PMCIDlink = "https://www.ncbi.nlm.nih.gov/pmc/articles/";
	static final String DOIlink = "https://doi.org/";

	static final String EUROPEPMClink = "http://europepmc.org/articles/";

	private static final String MESHlink = "https://www.ncbi.nlm.nih.gov/mesh/?term=";
	private static final String EFOlink = "https://www.ebi.ac.uk/efo/";
	private static final String GOlink = "http://amigo.geneontology.org/amigo/term/GO:";

	private FetcherCommon() {}

	public static boolean isPmid(String s) {
		if (s == null) return false;
		return PMID_ONLY.matcher(s).matches();
	}

	public static boolean isPmcid(String s) {
		if (s == null) return false;
		return PMCID_ONLY.matcher(s).matches();
	}

	public static String extractPmcid(String s) {
		if (!isPmcid(s)) return "";
		return s.substring(3);
	}

	public static boolean isDoi(String s) {
		if (s == null) return false;
		return DOI_ONLY.matcher(s).matches();
	}

	public static String normalizeDoi(String s) {
		if (s == null) return "";

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

	public static String getDoiRegistrant(String s) {
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

	public static String getPmidLink(String pmid) {
		if (isPmid(pmid)) return PMIDlink + pmid;
		else return null;
	}

	public static String getPmcidLink(String pmcid) {
		if (isPmcid(pmcid)) return PMCIDlink + pmcid + "/";
		else return null;
	}

	public static String getDoiLink(String doi) {
		if (isDoi(doi)) return DOIlink + doi;
		else return null;
	}

	public static String getMeshLink(MeshTerm mesh) {
		String link = null;
		if (!mesh.getUniqueId().isEmpty()) {
			link = MESHlink + "%22" + mesh.getUniqueId() + "%22";
		} else if (!mesh.getTerm().isEmpty()) {
			link = MESHlink + "%22" + mesh.getTerm().replaceAll(" ", "+") + "%22";
		}
		return link;
	}

	public static String getMinedLink(MinedTerm mined) {
		String link = null;
		if (!mined.getDbIds().isEmpty()) {
			if (mined.getDbName().equalsIgnoreCase("efo")) {
				link = EFOlink + mined.getDbIds().get(0);
			} else if (mined.getDbName().equalsIgnoreCase("GO")) {
				link = GOlink + mined.getDbIds().get(0);
			}
		}
		return link;
	}

	public static Publication getPublication(PublicationIds publicationIds, Database database, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts) {
		if (publicationIds == null) {
			System.err.println("null IDs given for getting publication");
			return null;
		}
		Publication publication = null;
		if (database != null) {
			publication = database.getPublication(publicationIds);
		}
		if (fetcher != null) {
			if (publication == null) {
				publication = fetcher.initPublication(publicationIds);
			}
			if (publication != null) {
				if (fetcher.getPublication(publication, parts)) {
					if (database != null) {
						database.putPublication(publication);
						database.commit();
					}
				}
			}
		}
		return publication;
	}

	public static Webpage getWebpage(String webpageUrl, Database database, Fetcher fetcher) {
		if (webpageUrl == null) {
			System.err.println("null start URL given for getting webpage");
			return null;
		}
		Webpage webpage = null;
		if (database != null) {
			webpage = database.getWebpage(webpageUrl);
		}
		if (fetcher != null) {
			if (webpage == null) {
				webpage = fetcher.initWebpage(webpageUrl);
			}
			if (webpage != null) {
				if (fetcher.getWebpage(webpage)) {
					if (database != null) {
						database.putWebpage(webpage);
						database.commit();
					}
				}
			}
		}
		return webpage;
	}

	public static Webpage getDoc(String docUrl, Database database, Fetcher fetcher) {
		if (docUrl == null) {
			System.err.println("null start URL given for getting doc");
			return null;
		}
		Webpage doc = null;
		if (database != null) {
			doc = database.getDoc(docUrl);
		}
		if (fetcher != null) {
			if (doc == null) {
				doc = fetcher.initWebpage(docUrl);
			}
			if (doc != null) {
				if (fetcher.getWebpage(doc)) {
					if (database != null) {
						database.putDoc(doc);
						database.commit();
					}
				}
			}
		}
		return doc;
	}

	public static URLConnection newConnection(String path, FetcherArgs fetcherArgs) throws MalformedURLException, IOException {
		URL url = new URL(path);
		URLConnection con = url.openConnection();
		con.setConnectTimeout(fetcherArgs.getConnectionTimeout());
		con.setReadTimeout(fetcherArgs.getConnectionTimeout());
		if (con instanceof HttpURLConnection) {
			CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
			int i = 0;
			while (con instanceof HttpURLConnection) {
				HttpURLConnection httpCon = (HttpURLConnection) con;
				httpCon.setRequestProperty("User-Agent", fetcherArgs.getUserAgent());
				httpCon.setRequestProperty("Referer", url.getProtocol() + "://" + url.getAuthority());
				// Java doesn't automatically redirect from http to https
				if (httpCon.getResponseCode() >= 300 && httpCon.getResponseCode() < 400) {
					++i;
					if (i > REDIRECT_LIMIT) {
						System.err.println("Too many redirects (" + i + ") in " + path + " (last location " + httpCon.getURL() + ")");
						break;
					}
					URL next = new URL(url, httpCon.getHeaderField("Location"));
					con = next.openConnection();
					con.setConnectTimeout(fetcherArgs.getConnectionTimeout());
					con.setReadTimeout(fetcherArgs.getConnectionTimeout());
				} else {
					break;
				}
			}
		}
		return con;
	}

	public static Path outputPath(String file, boolean allowEmptyPath) throws IOException {
		if (file == null || file.isEmpty()) {
			if (allowEmptyPath) {
				return null;
			} else {
				throw new FileNotFoundException("Empty path given!");
			}
		}

		Path path = Paths.get(file);
		Path parent = (path.getParent() != null ? path.getParent() : Paths.get("."));
		if (!Files.isDirectory(parent) || !Files.isWritable(parent)) {
			throw new AccessDeniedException(parent.toAbsolutePath().normalize() + " is not a writeable directory!");
		}
		if (Files.isDirectory(path)) {
			throw new FileAlreadyExistsException(path.toAbsolutePath().normalize() + " is an existing directory!");
		}
		return path;
	}
}
