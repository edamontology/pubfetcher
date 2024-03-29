/*
 * Copyright © 2016, 2017, 2018, 2020 Erik Jaaniso
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

package org.edamontology.pubfetcher.core.fetching;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.text.ParseException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.http.client.ClientProtocolException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.AdobePDFSchema;
import org.apache.xmpbox.schema.DublinCoreSchema;
import org.apache.xmpbox.xml.DomXmpParser;
import org.apache.xmpbox.xml.XmpParsingException;
import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.select.Selector.SelectorParseException;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;

import org.edamontology.pubfetcher.core.common.FetcherArgs;
import org.edamontology.pubfetcher.core.common.FetcherPrivateArgs;
import org.edamontology.pubfetcher.core.common.PubFetcher;
import org.edamontology.pubfetcher.core.db.link.Link;
import org.edamontology.pubfetcher.core.db.publication.CorrespAuthor;
import org.edamontology.pubfetcher.core.db.publication.MeshTerm;
import org.edamontology.pubfetcher.core.db.publication.MinedTerm;
import org.edamontology.pubfetcher.core.db.publication.Publication;
import org.edamontology.pubfetcher.core.db.publication.PublicationIds;
import org.edamontology.pubfetcher.core.db.publication.PublicationPartName;
import org.edamontology.pubfetcher.core.db.publication.PublicationPartType;
import org.edamontology.pubfetcher.core.db.webpage.Webpage;
import org.edamontology.pubfetcher.core.scrape.Scrape;
import org.edamontology.pubfetcher.core.scrape.ScrapeSiteKey;
import org.edamontology.pubfetcher.core.scrape.ScrapeWebpageKey;

public class Fetcher implements AutoCloseable {

	private static final Logger logger = LogManager.getLogger();

	private static int LINKS_LIMIT = 10;
	// private static long MAX_PDF_SIZE = 104857600; // 100 MiB
	private static long JAVASCRIPT_HARD_TIMEOUT = 120000; // 2 minutes

	private static final String EUROPEPMC = "https://www.ebi.ac.uk/europepmc/webservices/rest/";
	private static final String EUROPEPMC_ANNOTATIONS = "https://www.ebi.ac.uk/europepmc/annotations_api/annotationsByArticleIds?";
	private static final String EUTILS = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/";

	private static final Pattern KEYWORDS_BEGIN = Pattern.compile("(?i)^[\\p{Z}\\p{Cc}]*keywords?[\\p{Z}\\p{Cc}]*:*[\\p{Z}\\p{Cc}]*");
	private static final Pattern SEPARATOR = Pattern.compile("[,;|]");
	private static final Pattern MAILTO_BEGIN = Pattern.compile("(?i)^[\\p{Z}\\p{Cc}]*mailto[\\p{Z}\\p{Cc}]*:*[\\p{Z}\\p{Cc}]*");

	private static final Pattern PMID_EXTRACT = Pattern.compile("(?i)pmid[\\p{Z}\\p{Cc}]*:*[\\p{Z}\\p{Cc}]*(" + PubFetcher.PMID.pattern() + ")");
	private static final Pattern PMCID_EXTRACT = Pattern.compile("(?i)pmcid[\\p{Z}\\p{Cc}]*:*[\\p{Z}\\p{Cc}]*(" + PubFetcher.PMCID.pattern() + ")");
	private static final Pattern DOI_EXTRACT = Pattern.compile("(?i)doi[\\p{Z}\\p{Cc}]*:*[\\p{Z}\\p{Cc}]*(" + PubFetcher.DOI.pattern() + ")");

	private static final Pattern APPLICATION_PDF = Pattern.compile("(?i).*(application|image)/(pdf|octet-stream).*");
	private static final Pattern EXCEPTION_EXCEPTION = Pattern.compile("(?i)^https://doi\\.org/10\\.|\\.pdf$|\\.ps$|\\.gz$");

	private static final Pattern REGEX_ESCAPE = Pattern.compile("[^\\p{L}\\p{N}]");
	private static final Pattern REMOVE_AFTER_ASTERISK = Pattern.compile("^([^\\p{L}\\p{N}]+)[\\p{L}]+$");
	private static final Pattern ID_ESCAPE = Pattern.compile("[^\\p{L}\\p{N}._:-]");
	private static final String PHONE_ALLOWED = "[\\p{N} /.ext()+-]";
	private static final String PHONE_ALLOWED_END = "[\\p{N})]";
	private static final Pattern PHONE = Pattern.compile("(?i)(tel[:.e]*[\\p{Z}\\p{Cc}]*(phone[:.]*)?|phone[:.]*)[\\p{Z}\\p{Cc}]*(" + PHONE_ALLOWED + "+" + PHONE_ALLOWED_END + ")");
	private static final Pattern EMAIL = Pattern.compile("(?i)e-?mail[:.]*[\\p{Z}\\p{Cc}]*([a-zA-Z0-9+._-]+@[a-zA-Z0-9.-]+\\.[a-z]{2,})");
	private static final Pattern EMAIL_ONLY = Pattern.compile("(?i)[A-Z0-9+._-]+@[A-Z0-9.-]+\\.[a-z]{2,}");
	private static final Pattern WHITESPACE = Pattern.compile("[\\p{Z}\\p{Cc}\\p{Cf}]+");
	private static final Pattern PUNCTUATION_NUMBERS = Pattern.compile("[\\p{P}\\p{S}\\p{N}]+");
	private static final Pattern NAME_SEPARATOR = Pattern.compile("[ \\u002D\\u2010]+");
	private static final Pattern NAME_INITIAL_PERIOD = Pattern.compile("[.]");

	private static final Pattern ELSEVIER_REDIRECT = Pattern.compile("^https?://linkinghub\\.elsevier\\.com/retrieve/pii/(.+)$");
	private static final Pattern SCIENCEDIRECT = Pattern.compile("(?i)^https?://(www\\.)?sciencedirect\\.com/.+$");
	private static final String SCIENCEDIRECT_LINK = "https://www.sciencedirect.com/science/article/pii/";
	private static final String LIEBERTPUB_COOKIE_ABSENT = "https://www.liebertpub.com/action/cookieAbsent";

	private static final Pattern F1000_DOI = Pattern.compile("^10.12688/.+\\..+\\..+$");

	private static final Pattern HTTP_OR_HTTPS = Pattern.compile("(?i)^(http|https)://");
	private static final Pattern HTML = Pattern.compile("(?i)<[\\p{Z}\\p{Cc}\\p{Cf}]*html[>\\p{Z}\\p{Cc}\\p{Cf}]");

	private static Set<ActiveHost> activeHosts = new HashSet<>();
	private static final int ACTIVE_HOSTS_MAX = 4;

	private final List<WebDriver> drivers = new ArrayList<>();
	private final List<Boolean> driversFree = new ArrayList<>();
	private static final int DRIVERS_MAX = 4;

	private final Scrape scrape;

	public Fetcher(FetcherPrivateArgs fetcherPrivateArgs) throws IOException, ParseException {
		scrape = new Scrape(fetcherPrivateArgs.getJournalsYaml(), fetcherPrivateArgs.getWebpagesYaml());
	}

	public Scrape getScrape() {
		return scrape;
	}

	@Override
	public void close() {
		for (WebDriver driver : drivers) {
			if (driver != null) {
				driver.quit();
			}
		}
	}

	private static ActiveHost findActiveHost(String host) {
		ActiveHost activeHost = null;
		for (ActiveHost ah : activeHosts) {
			if (ah.getHost().equals(host)) {
				activeHost = ah;
				break;
			}
		}
		return activeHost;
	}

	private static String getHost(String url) {
		try {
			if (url == null) return null;
			String host = new URI(url).getHost();
			if (host == null) return url;
			host = host.toLowerCase(Locale.ROOT);
			return host.startsWith("www.") ? host.substring(4) : host;
		} catch (URISyntaxException e) {
			return url;
		}
	}

	private ActiveHost activateHost(String host) {
		if (host == null || host.equals("doi.org") || host.equals("dx.doi.org")) {
			return null;
		}
		ActiveHost activeHost = null;
		synchronized(activeHosts) {
			boolean waited = false;
			while ((activeHost = findActiveHost(host)) != null && activeHost.getCount() >= ACTIVE_HOSTS_MAX) {
				waited = true;
				logger.info("Waiting behind host {}", host);
				try {
					activeHosts.wait();
				} catch (InterruptedException e) {
					logger.error("Interrupt!", e);
					Thread.currentThread().interrupt();
					return null;
				}
			}
			if (waited) {
				logger.info("Resuming for host {}", host);
			}
			if (activeHost == null) {
				activeHost = new ActiveHost(host);
			} else {
				activeHost.increment();
			}
			activeHosts.add(activeHost);
		}
		return activeHost;
	}

	private WebDriver makeWebDriver(FetcherArgs fetcherArgs, int driversIndex) {
		logger.info("Making WebDriver {}", driversIndex);
		FirefoxOptions options = new FirefoxOptions();
		if (!fetcherArgs.getPrivateArgs().getSeleniumGeckodriver().isEmpty()) {
			System.setProperty("webdriver.gecko.driver", fetcherArgs.getPrivateArgs().getSeleniumGeckodriver());
		}
		if (!fetcherArgs.getPrivateArgs().getSeleniumFirefox().isEmpty()) {
			options.setBinary(fetcherArgs.getPrivateArgs().getSeleniumFirefox());
		}
		options.addArguments("-headless");
		FirefoxProfile profile = new FirefoxProfile();
		options.setProfile(profile);
		options.setAcceptInsecureCerts(true);
		options.setPageLoadTimeout(Duration.ofMillis(fetcherArgs.getTimeout() * 2)); // connect timeout plus read timeout
		//options.setScriptTimeout(Duration.ofMillis(10000));
		options.addPreference("general.useragent.override", fetcherArgs.getPrivateArgs().getUserAgent());
		return new FirefoxDriver(options);
	}

	private void setFetchException(Webpage webpage, Publication publication, String exceptionUrl) {
		if (webpage != null) {
			if (!webpage.isFetchException()) {
				logger.warn("Set fetching exception for webpage {}", webpage.toStringId());
				webpage.setFetchException(true);
			}
		}
		if (publication != null && (exceptionUrl == null || !EXCEPTION_EXCEPTION.matcher(exceptionUrl).find())) {
			if (!publication.isFetchException()) {
				logger.warn("Set fetching exception for publication {}", publication.toStringId());
				publication.setFetchException(true);
			}
		}
	}

	public Document postDoc(String url, Map<String, String> data, FetcherArgs fetcherArgs) {
		return getDoc(url, null, null, null, null, null, null, false, false, Method.POST, data, fetcherArgs, false);
	}

	public Document getDoc(String url, boolean javascript, FetcherArgs fetcherArgs) {
		return getDoc(url, null, null, null, null, null, null, javascript, false, Method.GET, null, fetcherArgs, false);
	}

	private Document getDoc(String url, Publication publication, FetcherArgs fetcherArgs) {
		return getDoc(url, null, publication, null, null, null, null, false, false, Method.GET, null, fetcherArgs, false);
	}

	private Document getDoc(Webpage webpage, boolean javascript, FetcherArgs fetcherArgs) {
		return getDoc(webpage.getStartUrl(), webpage, null, null, null, null, null, javascript, false, Method.GET, null, fetcherArgs, false);
	}

	private Document getDoc(String url, Publication publication, PublicationPartType type, String from, Links links, EnumMap<PublicationPartName, Boolean> parts, boolean javascript, FetcherArgs fetcherArgs) {
		return getDoc(url, null, publication, type, from, links, parts, javascript, false, Method.GET, null, fetcherArgs, false);
	}

	@SuppressWarnings("deprecation")
	private Document getDoc(String url, Webpage webpage, Publication publication, PublicationPartType type, String from, Links links, EnumMap<PublicationPartName, Boolean> parts, boolean javascript, boolean timeout, Method method, Map<String, String> data, FetcherArgs fetcherArgs, boolean reentry) {
		Document doc = null;

		logger.info("    {} {}{}{}", method, url, javascript ? " (with JavaScript)" : "", data != null ? (" (with data " + data + ")") : "");

		ActiveHost activeHost = null;
		if (!reentry) {
			activeHost = activateHost(getHost(url));
			if (Thread.currentThread().isInterrupted()) return null;
		}
		int driversIndex = -1;
		if (javascript && (fetcherArgs.getPrivateArgs().isSelenium() || !fetcherArgs.getPrivateArgs().getSeleniumGeckodriver().isEmpty() || !fetcherArgs.getPrivateArgs().getSeleniumFirefox().isEmpty())) {
			synchronized(driversFree) {
				boolean waited = false;
				while (true) {
					for (int i = 0; i < driversFree.size(); ++i) {
						if (driversFree.get(i)) {
							driversIndex = i;
							driversFree.set(i, false);
							break;
						}
					}
					if (driversIndex < 0 && driversFree.size() < DRIVERS_MAX) {
						driversIndex = driversFree.size();
						drivers.add(null);
						driversFree.add(false);
					}
					if (driversIndex >= 0) {
						break;
					}
					logger.info("Waiting for free WebDriver");
					waited = true;
					try {
						driversFree.wait();
					} catch (InterruptedException e) {
						logger.error("Interrupt!", e);
						Thread.currentThread().interrupt();
						return null;
					}
				}
				if (waited) {
					logger.info("Found free WebDriver {}", driversIndex);
				}
			}
		}

		try {
			if (webpage != null) {
				webpage.setStartUrl(url);
			}

			if (javascript) {
				if (driversIndex >= 0) {
					WebDriver driver = drivers.get(driversIndex);
					if (driver == null || scrape.getRestart(url) || driver.manage().timeouts().getPageLoadTimeout().toMillis() != fetcherArgs.getTimeout() * 2) {
						if (driver != null) {
							driver.quit();
						}
						driver = makeWebDriver(fetcherArgs, driversIndex);
						drivers.set(driversIndex, driver);
					}
					new URL(url);
					if (!HTTP_OR_HTTPS.matcher(url).find()) {
						throw new MalformedURLException("Must be http or https");
					}
					driver.get(url);
					String waitUntil = scrape.getSelector(scrape.getSite(driver.getCurrentUrl()), ScrapeSiteKey.wait_until);
					if (waitUntil == null) {
						Map<String, String> scrapeWebpage = scrape.getWebpage(driver.getCurrentUrl());
						if (scrapeWebpage != null) {
							waitUntil = scrapeWebpage.get(ScrapeWebpageKey.wait_until.toString());
						}
					}
					if (waitUntil != null) {
						WebDriverWait wait = new WebDriverWait(driver, Duration.ofMillis(fetcherArgs.getTimeout()));
						try {
							wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(waitUntil)));
						} catch (org.openqa.selenium.TimeoutException e) {
							logger.error(e);
						}
					}
					String pageSource = driver.getPageSource();
					if (pageSource.contains("<script src=\"resource://pdf.js/build/pdf.mjs\" type=\"module\"></script>")) {
						throw new UnsupportedMimeTypeException("Page is a PDF file", "application/pdf", driver.getCurrentUrl());
					}
					if (webpage != null) {
						if (pageSource.contains("<link rel=\"stylesheet\" href=\"resource://content-accessible/plaintext.css\">")) {
							webpage.setContentType("text/plain");
						} else if (HTML.matcher(pageSource).find()) {
							webpage.setContentType("text/html");
						} else {
							webpage.setContentType("text/plain");
						}
					}
					doc = Jsoup.parse(pageSource, driver.getCurrentUrl());
					logger.info("    GOT {} (with JavaScript)", doc.location());
				} else {
					JavascriptThread javascriptThread = new JavascriptThread(url, webpage, fetcherArgs);

					Thread t = new Thread(javascriptThread);
					t.setDaemon(true);
					t.start();

					long start = System.currentTimeMillis();

					while (!javascriptThread.isFinished()) {
						try {
							Thread.sleep(100);
							long elapsed = System.currentTimeMillis() - start;
							// 2 * timeout is expected timeout if htmlunit behaves, give twice that amount plus a fixed 1 second independent of timeout arg value
							// but cap max timeout for javascript at 2 minutes, because letting stuck htmlunit code run beyond that will start to cause errors for other fetching threads
							if (elapsed > 2 * 2 * fetcherArgs.getTimeout() + 1000 || elapsed > JAVASCRIPT_HARD_TIMEOUT) {
								t.stop(); // alternative is to call stop0(new ThreadDeath()) directly with Thread.class.getDeclaredMethod("stop0") and setAccessible(true)
								break;
							}
						} catch (InterruptedException e) {
							logger.error("Exception!", e);
							break;
						}
					}

					if (javascriptThread.getException() != null) {
						throw javascriptThread.getException();
					} else if (javascriptThread.getDoc() == null) {
						throw new NullPointerException("JavascriptThread has not created a Document!");
					} else {
						doc = javascriptThread.getDoc();
					}
				}
			} else {
				URL u = new URL(url);

				Connection con = Jsoup.connect(url)
					.userAgent(fetcherArgs.getPrivateArgs().getUserAgent())
					.referrer(u.getProtocol() + "://" + u.getAuthority())
					.timeout(fetcherArgs.getTimeout() * 2)
					.followRedirects(true)
					.ignoreHttpErrors(false)
					.ignoreContentType(false)
					.method(method);
				if (data != null) {
					con.data(data);
				}

				Response res = con.execute();

				if (webpage != null) {
					webpage.setContentType(res.contentType());
					webpage.setStatusCode(res.statusCode());
				}

				// TODO bufferUp() because of bug in Jsoup
				doc = res.bufferUp().parse();
			}

			if (webpage != null) {
				webpage.setFinalUrl(doc.location());
				webpage.setTitle(getFirstTrimmed(doc, "title", doc.location(), false, true));
				logger.info("        final url: {}", webpage.getFinalUrl());
				logger.info("        content type: {}", webpage.getContentType());
				logger.info("        status code: {}", webpage.getStatusCode());
				logger.info("        title: {}", webpage.getTitle());
				logger.info("        content length: {}", doc.text().length());
			} else if (!javascript) {
				logger.info("    {} {}", method == Method.GET ? "GOT" : (method == Method.POST ? "POSTED" : method), doc.location());
			}

			if (links != null) {
				links.addTriedLink(doc.location(), type, from);
			}
		} catch (MalformedURLException | ClientProtocolException e) {
			// if the request URL is not a HTTP or HTTPS URL, or is otherwise malformed
			logger.warn(e);
			if (webpage != null || publication != null) {
				fetchPdf(url, webpage, publication, type, from, links, parts, fetcherArgs, true);
			}
		} catch (HttpStatusException e) {
			// if the response is not OK and HTTP response errors are not ignored
			String host = getHost(e.getUrl());
			if (host == null || host.equals("doi.org") || host.equals("dx.doi.org")) {
				logger.error(e);
			} else {
				logger.warn(e);
			}
			if (webpage != null) {
				webpage.setFinalUrl(e.getUrl());
				webpage.setStatusCode(e.getStatusCode());
			}
			if (e.getStatusCode() == 503) {
				setFetchException(webpage, publication, null);
			} else {
				setFetchException(null, publication, e.getUrl());
			}
		} catch (FailingHttpStatusCodeException e) {
			String host = getHost(e.getResponse().getWebRequest().getUrl().toString());
			if (host == null || host.equals("doi.org") || host.equals("dx.doi.org")) {
				logger.error(e);
			} else {
				logger.warn(e);
			}
			if (webpage != null) {
				webpage.setFinalUrl(e.getResponse().getWebRequest().getUrl().toString());
				webpage.setStatusCode(e.getStatusCode());
			}
			if (e.getStatusCode() == 503) {
				setFetchException(webpage, publication, null);
			} else {
				setFetchException(null, publication, e.getResponse().getWebRequest().getUrl().toString());
			}
		} catch (java.net.ConnectException | java.net.NoRouteToHostException e) {
			logger.warn(e);
			setFetchException(webpage, publication, null);
		} catch (UnsupportedMimeTypeException e) {
			// if the response mime type is not supported and those errors are not ignored
			if (webpage != null) {
				webpage.setFinalUrl(e.getUrl());
				webpage.setContentType(e.getMimeType());
			}
			if (e.getMimeType() != null && (APPLICATION_PDF.matcher(e.getMimeType()).matches() || e.getMimeType().startsWith("PB"))) {
				// webpage/doc urls and doi links can point directly to PDF files
				if (webpage != null || publication != null) {
					fetchPdf(e.getUrl(), webpage, publication, type, from, links, parts, fetcherArgs, true);
				} else {
					logger.warn(e);
				}
			} else {
				logger.warn(e);
			}
		} catch (SocketTimeoutException | org.openqa.selenium.TimeoutException e) {
			// if the connection times out
			logger.warn(e);
			if (!timeout && !fetcherArgs.isQuick()) {
				doc = getDoc(url, webpage, publication, type, from, links, parts, javascript, true, method, data, fetcherArgs, true);
			} else {
				setFetchException(webpage, publication, null);
			}
		} catch (javax.net.ssl.SSLHandshakeException | javax.net.ssl.SSLProtocolException e) {
			logger.warn(e);
			// jsoup has deprecated validateTLSCertificates(false), so try with htmlunit and setUseInsecureSSL(true) or selenium and setAcceptInsecureCerts(true)
			// in jsoup, Connection.sslSocketFactory(SSLSocketFactory sslSocketFactory) provides a path to implement a workaround
			if (!javascript && method == Method.GET) {
				doc = getDoc(url, webpage, publication, type, from, links, parts, true, timeout, method, data, fetcherArgs, true);
			}
		} catch (IOException e) {
			// if a connection or read error occurs
			logger.warn(e);
		} catch (Exception e) {
			logger.warn("Exception!", e);
			setFetchException(webpage, publication, null);
		} finally {
			if (activeHost != null) {
				synchronized(activeHosts) {
					activeHost.decrement();
					if (activeHost.getCount() <= 0) {
						activeHosts.remove(activeHost);
					}
					activeHosts.notifyAll();
				}
			}
			if (driversIndex >= 0) {
				synchronized(driversFree) {
					driversFree.set(driversIndex, true);
					driversFree.notifyAll();
				}
			}
		}

		return doc;
	}

	private void fetchPdf(String url, Publication publication, PublicationPartType type, String from, Links links, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) {
		fetchPdf(url, null, publication, type, from, links, parts, fetcherArgs, false);
	}

	private void fetchPdf(String url, Webpage webpage, Publication publication, PublicationPartType type, String from, Links links, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs, boolean reentry) {
		// Don't fetch PDF if only keywords are missing
		// Also, getting a meaningful title or abstract from PDF (if previous methods have failed) is rather doubtful
		if (webpage == null && (publication == null
			|| isFinal(publication, new PublicationPartName[] {
					PublicationPartName.fulltext
				}, parts, false, fetcherArgs))) return;

		logger.info("    GET PDF {}", url);

		ActiveHost activeHost = null;
		if (!reentry) {
			activeHost = activateHost(getHost(url));
			if (Thread.currentThread().isInterrupted()) return;
		}

		try {
			URLConnection con;
			try {
				con = PubFetcher.newConnection(url, fetcherArgs.getTimeout(), fetcherArgs.getPrivateArgs().getUserAgent());
			} catch (IOException e) {
				logger.warn(e);
				return;
			}

			String finalUrl = con.getURL().toString();
			if (webpage != null) {
				webpage.setFinalUrl(finalUrl);
			}

			// TODO MAX_PDF_SIZE
			try (PDDocument doc = Loader.loadPDF(new RandomAccessReadBuffer(con.getInputStream()), IOUtils.createMemoryOnlyStreamCache())) {
				logger.info("    GOT PDF {}", finalUrl);
				if (webpage != null) {
					if (con instanceof HttpURLConnection) {
						HttpURLConnection httpCon = (HttpURLConnection) con;
						webpage.setStatusCode(httpCon.getResponseCode());
					}
				}

				if (type != null && !type.isPdf()) {
					type = type.toPdf();
				}

				if (links != null) {
					links.addTriedLink(finalUrl, type, from);
				}

				boolean titlePart = (parts == null || (parts.get(PublicationPartName.title) != null && parts.get(PublicationPartName.title)));
				boolean keywordsPart = (parts == null || (parts.get(PublicationPartName.keywords) != null && parts.get(PublicationPartName.keywords)));
				boolean abstractPart = (parts == null || (parts.get(PublicationPartName.theAbstract) != null && parts.get(PublicationPartName.theAbstract)));
				boolean fulltextPart = (parts == null || (parts.get(PublicationPartName.fulltext) != null && parts.get(PublicationPartName.fulltext)));

				if (webpage != null && webpage.getTitle().isEmpty()
					|| publication != null && (!publication.getTitle().isFinal(fetcherArgs) && titlePart || !publication.getKeywords().isFinal(fetcherArgs) && keywordsPart || !publication.getAbstract().isFinal(fetcherArgs) && abstractPart)) {
					PDDocumentInformation info = doc.getDocumentInformation();
					if (info != null) {
						if (webpage != null) {
							if (webpage.getTitle().isEmpty()) {
								String title = info.getTitle();
								if (title != null) {
									webpage.setTitle(title);
									if (!webpage.getTitle().isEmpty()) {
										logger.info("        title: {}", webpage.getTitle());
									}
								}
							}
						}
						if (publication != null) {
							if (!publication.getTitle().isFinal(fetcherArgs) && titlePart) {
								String title = info.getTitle();
								if (title != null) {
									publication.setTitle(title, type, finalUrl, fetcherArgs, true);
								}
							}
							if (!publication.getKeywords().isFinal(fetcherArgs) && keywordsPart) {
								String keywords = info.getKeywords();
								if (keywords != null) {
									publication.setKeywords(Arrays.asList(SEPARATOR.split(keywords)), type, finalUrl, fetcherArgs, true);
								}
							}
							if (!publication.getAbstract().isFinal(fetcherArgs) && abstractPart) {
								String theAbstract = info.getSubject();
								if (theAbstract != null) {
									publication.setAbstract(theAbstract, type, finalUrl, fetcherArgs, true);
								}
							}
						}
					}
				}

				if (webpage != null || publication != null && !publication.getFulltext().isFinal(fetcherArgs) && fulltextPart) {
					try {
						PDFTextStripper stripper = new PDFTextStripper();
						String pdfText = stripper.getText(doc);
						if (pdfText != null) {
							if (webpage != null) {
								webpage.setContent(pdfText);
								logger.info("        content length: {}", webpage.getContent().length());
							}
							if (publication != null && !publication.getFulltext().isFinal(fetcherArgs) && fulltextPart) {
								publication.setFulltext(pdfText, type, finalUrl, fetcherArgs);
							}
						}
					} catch (IOException e) {
						logger.warn(e);
					}
				}

				if (webpage != null && webpage.getTitle().isEmpty()
					|| publication != null && (!publication.getTitle().isFinal(fetcherArgs) && titlePart || !publication.getKeywords().isFinal(fetcherArgs) && keywordsPart || !publication.getAbstract().isFinal(fetcherArgs) && abstractPart)) {
					PDMetadata meta = doc.getDocumentCatalog().getMetadata();
					if (meta != null) {
						try (InputStream xmlInputStream = meta.createInputStream()) {
							XMPMetadata xmp = new DomXmpParser().parse(xmlInputStream);

							DublinCoreSchema dc = xmp.getDublinCoreSchema();
							if (dc != null) {
								if (webpage != null) {
									if (webpage.getTitle().isEmpty()) {
										String title = dc.getTitle();
										if (title != null) {
											webpage.setTitle(title);
											if (!webpage.getTitle().isEmpty()) {
												logger.info("        title: {}", webpage.getTitle());
											}
										}
									}
								}
								if (publication != null) {
									if (!publication.getTitle().isFinal(fetcherArgs) && titlePart) {
										String title = dc.getTitle();
										if (title != null) {
											publication.setTitle(title, type, finalUrl, fetcherArgs, true);
										}
									}
									if (!publication.getKeywords().isFinal(fetcherArgs) && keywordsPart) {
										List<String> keywords = dc.getSubjects();
										if (keywords != null) {
											publication.setKeywords(keywords, type, finalUrl, fetcherArgs, true);
										}
									}
									if (!publication.getAbstract().isFinal(fetcherArgs) && abstractPart) {
										String theAbstract = dc.getDescription();
										if (theAbstract != null) {
											publication.setAbstract(theAbstract, type, finalUrl, fetcherArgs, true);
										}
									}
								}
							}

							if (publication != null && !publication.getKeywords().isFinal(fetcherArgs) && keywordsPart) {
								AdobePDFSchema pdf = xmp.getAdobePDFSchema();
								if (pdf != null) {
									String keywords = pdf.getKeywords();
									if (keywords != null) {
										publication.setKeywords(Arrays.asList(SEPARATOR.split(keywords)), type, finalUrl, fetcherArgs, true);
									}
								}
							}
						} catch (IOException e) {
							logger.warn(e);
						} catch (XmpParsingException e) {
							logger.warn(e);
						} catch (IllegalArgumentException e) {
							logger.warn(e);
						}
					}
				}
			} catch (InvalidPasswordException e) {
				logger.warn(e);
			} catch (java.net.ConnectException | java.net.NoRouteToHostException e) {
				logger.warn(e);
				setFetchException(webpage, publication, null);
			} catch (SocketTimeoutException e) {
				logger.warn(e);
				setFetchException(webpage, publication, null);
			} catch (IOException e) {
				logger.warn(e);
			} catch (Exception e) {
				logger.warn("Exception!", e);
				setFetchException(webpage, publication, null);
			}
		} finally {
			if (activeHost != null) {
				synchronized(activeHosts) {
					activeHost.decrement();
					if (activeHost.getCount() <= 0) {
						activeHosts.remove(activeHost);
					}
					activeHosts.notifyAll();
				}
			}
		}
	}

	private static String getFirstTrimmed(Element element, String selector, String location, boolean logMissing, boolean formatText) {
		selector = selector.trim();
		if (selector.isEmpty()) {
			logger.error("Empty selector given for {}", location);
			return "";
		}
		Element tag = element.select(selector).first();
		if (tag != null) {
			String firstTrimmed = formatText ? CleanWebpage.formattedText(tag) : tag.text();
			if (logMissing && firstTrimmed.isEmpty()) {
				logger.warn("Empty content in element selected by {} in {}", selector, location);
			}
			return firstTrimmed;
		} else {
			if (logMissing) {
				logger.warn("No element found for selector {} in {}", selector, location);
			}
			return "";
		}
	}

	private static Elements getAll(Element element, String selector, String location, boolean logMissing) {
		selector = selector.trim();
		if (selector.isEmpty()) {
			logger.error("Empty selector given for {}", location);
			return new Elements();
		}
		Elements all = element.select(selector);
		if (logMissing && all.isEmpty()) {
			logger.warn("No elements found for selector {} in {}", selector, location);
		}
		return all;
	}

	private static String text(Element element, String selector, String location, boolean logMissing, boolean formatText) {
		selector = selector.trim();
		if (selector.isEmpty()) {
			logger.error("Empty selector given for {}", location);
			return "";
		}
		String text = getAll(element, selector, location, false).stream()
			.filter(e -> e.hasText())
			.map(e -> formatText ? CleanWebpage.formattedText(e) : e.text().replaceAll("<[hH][1-6]>", " ").replaceAll("</[hH][1-6]>", ". "))
			.collect(Collectors.joining("\n\n"));
		if (logMissing && text.isEmpty()) {
			logger.warn("No text found for selector {} in {}", selector, location);
		}
		return text;
	}

	private void setIds(Publication publication, Element element, PublicationPartType type, String pmid, String pmcid, String doi, boolean prependPMC, String location, boolean errorIfInvalid, FetcherArgs fetcherArgs) {
		if (pmid != null && !pmid.trim().isEmpty()) {
			String pmidText = getFirstTrimmed(element, pmid, location, false, false);
			if (PubFetcher.isPmid(pmidText)) {
				publication.setPmid(pmidText, type, location, fetcherArgs);
			} else if (!pmidText.isEmpty()) {
				String pmidExtracted = null;
				Matcher pmidMatcher = PMID_EXTRACT.matcher(pmidText);
				if (pmidMatcher.find()) {
					pmidExtracted = pmidMatcher.group(1);
				}
				if (pmidExtracted != null && PubFetcher.isPmid(pmidExtracted)) {
					publication.setPmid(pmidExtracted, type, location, fetcherArgs);
				} else {
					if (errorIfInvalid) {
						logger.error("Trying to set invalid PMID {} from {}", pmidText, location);
					} else {
						logger.warn("Trying to set invalid PMID {} from {}", pmidText, location);
					}
				}
			}
		}
		if (pmcid != null && !pmcid.trim().isEmpty()) {
			String pmcidText = getFirstTrimmed(element, pmcid, location, false, false);
			if (prependPMC) pmcidText = "PMC" + pmcidText;
			if (PubFetcher.isPmcid(pmcidText)) {
				publication.setPmcid(pmcidText, type, location, fetcherArgs);
			} else if (!pmcidText.isEmpty()) {
				String pmcidExtracted = null;
				Matcher pmcidMatcher = PMCID_EXTRACT.matcher(pmcidText);
				if (pmcidMatcher.find()) {
					pmcidExtracted = pmcidMatcher.group(1);
				}
				if (pmcidExtracted != null) {
					pmcidExtracted = pmcidExtracted.toUpperCase(Locale.ROOT);
				}
				if (pmcidExtracted != null && PubFetcher.isPmcid(pmcidExtracted)) {
					publication.setPmcid(pmcidExtracted, type, location, fetcherArgs);
				} else {
					if (errorIfInvalid) {
						logger.error("Trying to set invalid PMCID {} from {}", pmcidText, location);
					} else {
						logger.warn("Trying to set invalid PMCID {} from {}", pmcidText, location);
					}
				}
			}
		}
		if (doi != null && !doi.trim().isEmpty()) {
			String doiText = getFirstTrimmed(element, doi, location, false, false);
			if (PubFetcher.isDoi(doiText) && doiText.indexOf(" ") < 0) {
				publication.setDoi(doiText, type, location, fetcherArgs);
			} else if (!doiText.isEmpty()) {
				String doiExtracted = null;
				Matcher doiMatcher = DOI_EXTRACT.matcher(doiText);
				if (doiMatcher.find()) {
					doiExtracted = doiMatcher.group(1);
				}
				if (doiExtracted != null && PubFetcher.isDoi(doiExtracted) && doiExtracted.indexOf(" ") < 0) {
					publication.setDoi(doiExtracted, type, location, fetcherArgs);
				} else {
					if (errorIfInvalid) {
						logger.error("Trying to set invalid DOI {} from {}", doiText, location);
					} else {
						logger.warn("Trying to set invalid DOI {} from {}", doiText, location);
					}
				}
			}
		}
	}

	private String getTitleText(Element element, String title, String subtitle, String location) {
		String titleText = getFirstTrimmed(element, title, location, true, false);
		if (subtitle != null && !subtitle.trim().isEmpty()) {
			String subtitleText = getFirstTrimmed(element, subtitle, location, false, false);
			if (!subtitleText.isEmpty()) {
				titleText += " : " + subtitleText;
			}
		}
		return titleText;
	}

	private void setTitle(Publication publication, Element element, PublicationPartType type, String title, String subtitle, String location, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) {
		if (parts == null || (parts.get(PublicationPartName.title) != null && parts.get(PublicationPartName.title))) {
			if (!publication.getTitle().isFinal(fetcherArgs) && title != null && !title.trim().isEmpty()) {
				publication.setTitle(getTitleText(element, title, subtitle, location), type, location, fetcherArgs, false);
			}
		}
	}

	private void setKeywords(Publication publication, Element element, PublicationPartType type, String keywords, String location, boolean split, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) {
		if (parts == null || (parts.get(PublicationPartName.keywords) != null && parts.get(PublicationPartName.keywords))) {
			if (!publication.getKeywords().isFinal(fetcherArgs) && keywords != null && !keywords.trim().isEmpty()) {
				Elements keywordsElements = getAll(element, keywords, location, false); // false - don't complain about missing keywords
				if (!keywordsElements.isEmpty()) {
					List<String> keywordsList;
					if (split) {
						keywordsList = keywordsElements.stream()
							.map(e -> KEYWORDS_BEGIN.matcher(e.text()).replaceFirst(""))
							.flatMap(k -> SEPARATOR.splitAsStream(k))
							.collect(Collectors.toList());
					} else {
						keywordsList = keywordsElements.stream()
							.map(e -> e.text())
							.collect(Collectors.toList());
					}
					publication.setKeywords(keywordsList, type, location, fetcherArgs, false);
				}
			}
		}
	}

	private void setAbstract(Publication publication, Element element, PublicationPartType type, String theAbstract, String location, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) {
		if (parts == null || (parts.get(PublicationPartName.theAbstract) != null && parts.get(PublicationPartName.theAbstract))) {
			if (!publication.getAbstract().isFinal(fetcherArgs) && theAbstract != null && !theAbstract.trim().isEmpty()) {
				publication.setAbstract(text(element, theAbstract, location, true, false), type, location, fetcherArgs, false);
			}
		}
	}

	private void setFulltext(Publication publication, Element element, PublicationPartType type, String title, String subtitle, String theAbstract, String fulltext, String location, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) {
		if (parts == null || (parts.get(PublicationPartName.fulltext) != null && parts.get(PublicationPartName.fulltext))) {
			if (!publication.getFulltext().isFinal(fetcherArgs) && fulltext != null && !fulltext.trim().isEmpty()) {
				String fulltextText = text(element, fulltext, location, true, false);
				if (!fulltextText.isEmpty()) {
					StringBuilder sb = new StringBuilder();
					if (title != null && !title.trim().isEmpty()) {
						sb.append(getTitleText(element, title, subtitle, location));
						sb.append("\n\n");
					}
					if (theAbstract != null && !theAbstract.trim().isEmpty()) {
						sb.append(text(element, theAbstract, location, true, false));
						sb.append("\n\n");
					}
					sb.append(fulltextText);
					publication.setFulltext(sb.toString(), type, location, fetcherArgs);
				}
			}
		}
	}

	private void setJournalTitle(Publication publication, Element element, String selector, String location) {
		selector = selector.trim();
		if (selector.isEmpty()) {
			logger.error("Empty selector given for journal title in {}", location);
			return;
		}
		Element journalTitle = element.selectFirst(selector);
		if (journalTitle != null && journalTitle.hasText()) {
			publication.setJournalTitle(journalTitle.text());
		} else {
			logger.warn("Journal title not found in {}", location);
		}
	}

	private void setPubDate(Publication publication, Element element, String selector, String location, boolean separated) {
		selector = selector.trim();
		if (selector.isEmpty()) {
			logger.error("Empty selector given for publication date in {}", location);
			return;
		}
		Element pubDate = element.selectFirst(selector);
		if (pubDate != null && pubDate.hasText()) {
			if (separated) {
				String date = "";
				Element year = pubDate.selectFirst("Year");
				if (year != null && year.hasText()) {
					date += year.text();
					Element month = pubDate.selectFirst("Month");
					if (month != null && month.hasText()) {
						date += "-" + (month.text().length() == 1 ? "0" : "") + month.text();
						Element day = pubDate.selectFirst("Day");
						if (day != null && day.hasText()) {
							date += "-" + (day.text().length() == 1 ? "0" : "") + day.text();
						} else {
							date += "-01";
						}
					} else {
						date += "-01-01";
					}
				}
				if (!date.isEmpty()) {
					publication.setPubDate(date);
				} else {
					logger.warn("Publication date (separated) not found in {}", location);
				}
			} else {
				publication.setPubDate(pubDate.text());
			}
		} else {
			logger.warn("Publication date not found in {}", location);
		}
	}

	private boolean setCitationsCount(Publication publication, Element element, String selector, String location) {
		selector = selector.trim();
		if (selector.isEmpty()) {
			logger.error("Empty selector given for citations count in {}", location);
			return false;
		}
		Element citationsCount = element.selectFirst(selector);
		if (citationsCount != null && citationsCount.hasText()) {
			return publication.setCitationsCount(citationsCount.text());
		} else {
			logger.error("Citations count not found in {}", location);
			return false;
		}
	}

	private String getContribName(Element element) {
		String name = "";
		Element nameTag = element.selectFirst("name");
		if (nameTag != null) {
			Element surnameTag = nameTag.selectFirst("surname");
			if (surnameTag != null) {
				String surname = surnameTag.text();
				if (!surname.isEmpty()) {
					Element givenNamesTag = nameTag.selectFirst("given-names");
					if (givenNamesTag != null) {
						String givenNames = givenNamesTag.text();
						if (!givenNames.isEmpty()) {
							name += givenNames + " ";
						}
					}
					name += surname;
				}
			}
		}
		return name;
	}

	private String getContribSurname(Element element) {
		String surname = "";
		Element nameTag = element.selectFirst("name");
		if (nameTag != null) {
			Element surnameTag = nameTag.selectFirst("surname");
			if (surnameTag != null) {
				surname = surnameTag.text();
			}
		}
		return surname;
	}

	private void addEmailPhoneUriXml(Element corresp, CorrespAuthor ca) {
		if (ca.getEmail().isEmpty()) {
			for (Element emailTag : corresp.select("email")) {
				String email = emailTag.text();
				if (!email.isEmpty()) {
					ca.setEmail(email);
					break;
				}
			}
		}
		if (ca.getPhone().isEmpty()) {
			for (Element phoneTag : corresp.select("phone")) {
				String phone = phoneTag.text();
				if (!phone.isEmpty()) {
					ca.setPhone(phone);
					break;
				}
			}
		}
		if (ca.getUri().isEmpty()) {
			for (Element uriTag : corresp.select("uri, ext-link[ext-link-type=uri]")) {
				String uri = uriTag.text();
				if (!uri.isEmpty()) {
					ca.setUri(uri);
					break;
				}
			}
		}

		addEmail(corresp.text(), ca);
		addPhone(corresp.text(), ca);
	}

	private void addEmail(String text, CorrespAuthor ca) {
		if (ca.getEmail().isEmpty()) {
			Matcher emailMatcher = EMAIL.matcher(text);
			while (emailMatcher.find()) {
				String email = emailMatcher.group(1).trim();
				if (!email.isEmpty()) {
					ca.setEmail(email);
					break;
				}
			}
		}
	}

	private void addPhone(String text, CorrespAuthor ca) {
		if (ca.getPhone().isEmpty()) {
			Matcher phoneMatcher = PHONE.matcher(text);
			while (phoneMatcher.find()) {
				String phone = phoneMatcher.group(3).trim();
				if (!phone.isEmpty()) {
					ca.setPhone(phone);
					break;
				}
			}
		}
	}

	boolean matchEmail(String name, String email, int pass) {
		int emailAt = email.indexOf("@");
		if (emailAt > -1) {
			email = email.substring(0, emailAt);
		}
		email = email.toLowerCase(Locale.ROOT);
		email = Normalizer.normalize(email, Normalizer.Form.NFKD);
		email = WHITESPACE.matcher(email).replaceAll("");
		email = PUNCTUATION_NUMBERS.matcher(email).replaceAll("");
		if (email.isEmpty()) {
			return false;
		}

		name = name.toLowerCase(Locale.ROOT);
		name = Normalizer.normalize(name, Normalizer.Form.NFKD);
		if (pass == 0) {
			name = WHITESPACE.matcher(name).replaceAll("");
			name = PUNCTUATION_NUMBERS.matcher(name).replaceAll("");
			return name.equals(email);
		} else if (pass == 1) {
			name = NAME_INITIAL_PERIOD.matcher(name).replaceAll(". ");
			name = WHITESPACE.matcher(name).replaceAll(" ").trim();
			for (String namePart : NAME_SEPARATOR.split(name)) {
				namePart = PUNCTUATION_NUMBERS.matcher(namePart).replaceAll("");
				if (namePart.length() > 1 && email.contains(namePart)) {
					return true;
				}
			}
			return false;
		} else if (pass == 2) {
			name = NAME_INITIAL_PERIOD.matcher(name).replaceAll(". ");
			name = WHITESPACE.matcher(name).replaceAll(" ").trim();
			name = PUNCTUATION_NUMBERS.matcher(name).replaceAll("");
			StringBuilder acronym = new StringBuilder();
			if (name.length() > 0) {
				acronym.append(name.charAt(0));
			}
			int from = 0;
			int current = -1;
			while ((current = name.indexOf(" ", from)) > -1) {
				if (current + 1 < name.length()) {
					acronym.append(name.charAt(current + 1));
				}
				from = current + 1;
			}
			return acronym.length() > 1 && acronym.toString().equals(email);
		} else {
			name = NAME_INITIAL_PERIOD.matcher(name).replaceAll(". ");
			name = WHITESPACE.matcher(name).replaceAll(" ").trim();
			for (String namePart : NAME_SEPARATOR.split(name)) {
				namePart = PUNCTUATION_NUMBERS.matcher(namePart).replaceAll("");
				if (namePart.length() - pass + 2 > 0) {
					namePart = namePart.substring(0, namePart.length() - pass + 2);
				} else {
					namePart = "";
				}
				if (namePart.length() > 1 && email.contains(namePart)) {
					return true;
				}
			}
			return false;
		}
	}

	private void setCorrespAuthor(Publication publication, Element result, boolean europepmc) {
		List<CorrespAuthor> correspAuthor = new ArrayList<>();

		for (Element author : result.select(europepmc ? "authorList > author" : "AuthorList > Author")) {
			String email = null;
			for (Element affiliation : author.select(europepmc ? "affiliation" : "Affiliation")) {
				String affiliationText = affiliation.text();
				Matcher emailMatcher = EMAIL_ONLY.matcher(affiliation.text());
				if (emailMatcher.find()) {
					email = affiliationText.substring(emailMatcher.start(), emailMatcher.end());
					break;
				}
			}
			if (email != null) {
				CorrespAuthor ca = new CorrespAuthor();
				Element lastName = author.selectFirst(europepmc ? "lastName" : "LastName");
				if (lastName != null) {
					String name = lastName.text();
					Element firstName = author.selectFirst(europepmc ? "firstName" : "ForeName");
					if (firstName != null) {
						name = firstName.text() + " " + name;
					}
					ca.setName(name);
				}
				Element orcid = author.selectFirst(europepmc ? "authorId[type=ORCID]" : "Identifier[Source=ORCID]");
				if (orcid != null) {
					String orcidText = orcid.text();
					if (!orcidText.startsWith("http")) {
						orcidText = "https://orcid.org/" + orcidText;
					}
					ca.setOrcid(orcidText);
				}
				ca.setEmail(email);
				correspAuthor.add(ca);
			}
		}

		if (!correspAuthor.isEmpty()) {
			publication.setCorrespAuthor(correspAuthor);
		}
	}

	private void setCorrespAuthor(Publication publication, Element element, String location, boolean xml) {
		List<CorrespAuthor> correspAuthor = new ArrayList<>();

		if (xml) {
			Elements contribCorrespAll = element.select("contrib[corresp=yes], contrib:has(xref[ref-type=corresp])");
			if (contribCorrespAll.isEmpty()) {
				contribCorrespAll = element.select("contrib:has(xref[ref-type=author-notes]):has(xref[rid~=(?i)fn[0-9]+]), contrib:has(xref[ref-type=author-notes]):has(xref[rid~=(?i)^N[0-9a-fx.]+$])");
			}
			if (contribCorrespAll.isEmpty()) {
				for (Element contrib : element.select("contrib")) {
					for (Element corresp : element.select("author-notes > corresp")) {
						String surname = getContribSurname(contrib);
						if (!surname.isEmpty() && corresp.text().indexOf(surname) > -1) {
							contribCorrespAll.add(contrib);
							break;
						}
						String id = corresp.attr("id");
						if (id != null && !id.isEmpty()) {
							boolean found = false;
							for (Element xref : contrib.select("xref")) {
								String rid = xref.attr("rid");
								if (rid != null && !rid.isEmpty() && id.equals(rid)) {
									contribCorrespAll.add(contrib);
									found = true;
									break;
								}
							}
							if (found) {
								break;
							}
						}
					}
				}
			}

			List<String> emails = new ArrayList<>();
			List<String> phones = new ArrayList<>();
			List<String> uris = new ArrayList<>();
			List<Boolean> taken = new ArrayList<>();

			for (Element corresp : element.select("author-notes > corresp")) {
				List<String> emailsCorresp = new ArrayList<>();
				List<String> phonesCorresp = new ArrayList<>();
				List<String> urisCorresp = new ArrayList<>();

				for (Element emailTag : corresp.select("email")) {
					emailsCorresp.add(emailTag.text());
					emailTag.remove();
				}
				Matcher emailMatcher = EMAIL.matcher(corresp.text());
				while (emailMatcher.find()) {
					emailsCorresp.add(emailMatcher.group(1).trim());
				}

				for (Element phoneTag : corresp.select("phone")) {
					phonesCorresp.add(phoneTag.text());
					phoneTag.remove();
				}
				Matcher phoneMatcher = PHONE.matcher(corresp.text());
				while (phoneMatcher.find()) {
					phonesCorresp.add(phoneMatcher.group(3).trim());
				}

				for (Element uriTag : corresp.select("uri, ext-link[ext-link-type=uri]")) {
					urisCorresp.add(uriTag.text());
					uriTag.remove();
				}

				emails.addAll(emailsCorresp);
				for (int i = 0; i < emailsCorresp.size() && i < phonesCorresp.size(); ++i) {
					phones.add(phonesCorresp.get(i));
				}
				for (int i = phonesCorresp.size(); i < emailsCorresp.size(); ++i) {
					phones.add("");
				}
				for (int i = 0; i < emailsCorresp.size() && i < urisCorresp.size(); ++i) {
					uris.add(urisCorresp.get(i));
				}
				for (int i = urisCorresp.size(); i < emailsCorresp.size(); ++i) {
					uris.add("");
				}
				for (int i = 0; i < emailsCorresp.size(); ++i) {
					taken.add(false);
				}
			}

			for (Element contribCorresp : contribCorrespAll) {
				CorrespAuthor ca = new CorrespAuthor();

				String name = getContribName(contribCorresp);
				if (!name.isEmpty()) {
					ca.setName(name);
				}

				Element orcidTag = contribCorresp.selectFirst("contrib-id[contrib-id-type=orcid]");
				if (orcidTag != null) {
					ca.setOrcid(orcidTag.text());
				}

				addEmailPhoneUriXml(contribCorresp, ca);

				if (contribCorrespAll.size() == 1 && emails.size() == 1) {
					if (ca.getEmail().isEmpty()) {
						ca.setEmail(emails.get(0));
					}
					if (ca.getPhone().isEmpty()) {
						ca.setPhone(phones.get(0));
					}
					if (ca.getUri().isEmpty()) {
						ca.setUri(uris.get(0));
					}
					taken.set(0, true);
				}

				for (Element xref : contribCorresp.select("xref[ref-type=corresp], xref[ref-type=author-notes]")) {
					String rid = xref.attr("rid");
					if (rid != null && !rid.isEmpty()) {
						rid = ID_ESCAPE.matcher(rid).replaceAll("_");
						for (Element authorNotes : element.select("author-notes > fn[id=" + rid + "]")) {
							addEmailPhoneUriXml(authorNotes, ca);
						}
					}
				}

				if (!ca.isEmpty()) {
					correspAuthor.add(ca);
				}
			}

			int longestLength = 0;
			for (CorrespAuthor ca : correspAuthor) {
				String name = ca.getName();
				name = NAME_INITIAL_PERIOD.matcher(name).replaceAll(". ");
				name = WHITESPACE.matcher(name).replaceAll(" ").trim();
				for (String namePart : NAME_SEPARATOR.split(name)) {
					namePart = PUNCTUATION_NUMBERS.matcher(namePart).replaceAll("");
					if (namePart.length() > longestLength) {
						longestLength = namePart.length();
					}
				}
			}
			if (longestLength > 100) {
				longestLength = 100;
			}

			for (int i = 0; i <= longestLength; ++i) {
				for (CorrespAuthor ca : correspAuthor) {
					if (!ca.getEmail().isEmpty()) continue;
					for (int j = 0; j < emails.size(); ++j) {
						if (taken.get(j)) continue;
						if (matchEmail(ca.getName(), emails.get(j), i)) {
							ca.setEmail(emails.get(j));
							if (ca.getPhone().isEmpty()) {
								ca.setPhone(phones.get(j));
							}
							if (ca.getUri().isEmpty()) {
								ca.setUri(uris.get(j));
							}
							taken.set(j, true);
							break;
						}
					}
				}
			}
		} else {
			// TODO correspAuthor for html is most likely outdated
			for (Element contribCorrespSup : element.select(".contrib-group a ~ sup:has(img[alt=corresponding author])")) {
				Element contribCorresp = new Elements(contribCorrespSup).prevAll("a").first();
				if (contribCorresp != null) {
					String name = contribCorresp.text();
					if (!name.isEmpty()) {
						CorrespAuthor ca = new CorrespAuthor();
						ca.setName(name);
						try {
							String nameRegex = REGEX_ESCAPE.matcher(name).replaceAll("\\\\$0");
							Element contribEmail = element.selectFirst(".contrib-email:matchesOwn((?i)^" + nameRegex + "), .fm-authors-info > div:not(:has(.contrib-email)):matches((?i)" + nameRegex + ")");
							if (contribEmail != null) {
								for (Element emailTag : contribEmail.select(".oemail")) {
									String email = new StringBuilder(emailTag.text()).reverse().toString();
									if (!email.isEmpty() && ca.getEmail().isEmpty()) {
										ca.setEmail(email);
									}
								}
								addEmail(contribEmail.text(), ca);
								addPhone(contribEmail.text(), ca);
							}
						} catch (SelectorParseException e) {
							logger.error(e);
						}
						correspAuthor.add(ca);
					}
				}
			}

			Elements notesCorrespAll = element.select(".fm-authors-info div[id~=(?i)cor[0-9]+], .fm-authors-info div[id~=(?i)caf[0-9]+], .fm-authors-info div[id~=(?i)^c[0-9]+], .fm-authors-info div[id~=(?i)^cr[0-9]+], .fm-authors-info div[id~=(?i)^cor$]");
			if (notesCorrespAll.isEmpty()) {
				notesCorrespAll = element.select(".fm-authors-info div[id~=(?i)fn[0-9]+]");
				if (notesCorrespAll.isEmpty()) {
					notesCorrespAll = element.select(".fm-authors-info div[id~=(?i)^N[0-9a-fx.]+$]");
				}
			}
			int supCount = 0;
			for (Element notesCorresp : notesCorrespAll) {
				String sup = "";
				Element supTag = notesCorresp.selectFirst("sup");
				if (supTag != null) {
					sup = supTag.text();
				} else {
					String notesCorrespText = notesCorresp.text();
					if (!notesCorrespText.isEmpty()) {
						sup = notesCorrespText.split(" ")[0];
						Matcher asteriskMatcher = REMOVE_AFTER_ASTERISK.matcher(sup);
						if (asteriskMatcher.matches()) {
							sup = asteriskMatcher.group(1);
						}
					}
				}
				if (!sup.isEmpty()) {
					sup = REGEX_ESCAPE.matcher(sup).replaceAll("\\\\$0");
					try {
						for (Element contrib : element.select(".contrib-group a ~ sup:matchesOwn(^" + sup + ",?$), .contrib-group a ~ .other:matchesOwn(^" + sup + ",?$)")) {
							Element nameTag = new Elements(contrib).prevAll("a").first();
							if (nameTag != null) {
								String name = nameTag.text();
								if (!name.isEmpty()) {
									++supCount;
									boolean existingCa = false;
									for (CorrespAuthor ca : correspAuthor) {
										if (name.equals(ca.getName())) {
											existingCa = true;
										}
									}
									if (!existingCa) {
										CorrespAuthor ca = new CorrespAuthor();
										ca.setName(name);
										correspAuthor.add(ca);
									}
								}
							}
						}
					} catch (SelectorParseException e) {
						logger.error(e);
					}
				}
			}
			for (Element notesCorresp : notesCorrespAll) {
				CorrespAuthor aca = null;
				boolean existingCa = false;
				String notesCorrespText = notesCorresp.text();

				if (supCount == 1 && correspAuthor.size() > 0) {
					aca = correspAuthor.get(correspAuthor.size() - 1);
					existingCa = true;
				} else {
					for (CorrespAuthor ca : correspAuthor) {
						String[] nameParts = ca.getName().split(" ");
						String surname = nameParts[nameParts.length - 1];
						Element emailTag = notesCorresp.select(".oemail").first();
						if (!surname.isEmpty() && (notesCorrespText.indexOf(surname) > -1
								|| emailTag != null && new StringBuilder(emailTag.text()).reverse().toString().toLowerCase(Locale.ROOT).indexOf(surname.toLowerCase(Locale.ROOT)) > -1)) {
							aca = ca;
							existingCa = true;
							break;
						}
					}
				}
				if (!existingCa) {
					aca = new CorrespAuthor();
				}

				for (Element emailTag : notesCorresp.select(".oemail")) {
					String email = new StringBuilder(emailTag.text()).reverse().toString();
					if (!email.isEmpty() && aca.getEmail().isEmpty()) {
						aca.setEmail(email);
					}
				}
				addEmail(notesCorrespText, aca);
				addPhone(notesCorrespText, aca);

				if (!aca.isEmpty() && !existingCa) {
					correspAuthor.add(aca);
				}
			}
		}

		if (!correspAuthor.isEmpty()) {
			if (xml && (!correspAuthor.get(0).getEmail().isEmpty() || correspAuthor.size() > publication.getCorrespAuthor().size())) {
				publication.resetCorrespAuthor();
			}
			publication.setCorrespAuthor(correspAuthor);
		} else {
			logger.warn("Corresponding author not found in {}", location);
		}
	}

	private void setCorrespAuthor(Publication publication, Element element, String names, String emails, String location) {
		if (publication.getCorrespAuthor().isEmpty() && (names != null && !names.trim().isEmpty() || emails != null && !emails.trim().isEmpty())) {
			List<String> caNames = null;
			if (names != null && !names.trim().isEmpty()) {
				caNames = getAll(element, names, location, true).stream().map(e -> e.text()).collect(Collectors.toList());
			}
			List<String> caEmails = null;
			if (emails != null && !emails.trim().isEmpty()) {
				caEmails = getAll(element, emails, location, true).stream().map(e -> MAILTO_BEGIN.matcher(e.attr("abs:href").trim()).replaceFirst("")).collect(Collectors.toList());
			}
			if (caNames != null && !caNames.isEmpty() && caEmails != null && !caEmails.isEmpty() && (caEmails.size() == caNames.size() * caNames.size() && caNames.size() > 1 || caEmails.size() > 1 && caNames.size() == 1)) {
				boolean reduce = true;
				for (int i = 0; i < caNames.size(); ++i) {
					for (int j = i + caNames.size(); j < caEmails.size(); j += caNames.size()) {
						if (!caEmails.get(i).equals(caEmails.get(j))) {
							reduce = false;
						}
					}
				}
				if (reduce) {
					caEmails = caEmails.subList(0, caNames.size());
				}
			}
			if (caEmails != null && caEmails.size() > 1) {
				boolean namesEqual = false;
				if (caNames != null && caNames.size() == caEmails.size()) {
					namesEqual = true;
					String firstName = caNames.get(0);
					for (int i = 1; i < caNames.size(); ++i) {
						if (!firstName.equals(caNames.get(i))) {
							namesEqual = false;
							break;
						}
					}
				}
				boolean emailsEqual = true;
				String firstEmail = caEmails.get(0);
				for (int i = 1; i < caEmails.size(); ++i) {
					if (!firstEmail.equals(caEmails.get(i))) {
						emailsEqual = false;
						break;
					}
				}
				if (emailsEqual && !namesEqual) {
					caEmails = new ArrayList<>();
					caEmails.add(firstEmail);
				}
			}
			if ((caNames == null || caNames.isEmpty()) && (caEmails == null || caEmails.isEmpty())) {
				logger.warn("No corresponding authors found in {}", location);
			} else {
				if (caNames != null && !caNames.isEmpty() && caEmails != null && !caEmails.isEmpty() && caNames.size() != caEmails.size()) {
					logger.warn("Discarding corresponding author names as number of names ({}) is not equal to number of e-mails ({}) in {}", caNames.size(), caEmails.size(), location);
					caNames = null;
				}
				List<CorrespAuthor> correspAuthor = new ArrayList<>();
				int correspAuthorSize = 0;
				if (caNames != null && !caNames.isEmpty()) {
					correspAuthorSize = caNames.size();
				} else if (caEmails != null && !caEmails.isEmpty()) {
					correspAuthorSize = caEmails.size();
				}
				for (int i = 0; i < correspAuthorSize; ++i) {
					CorrespAuthor ca = new CorrespAuthor();
					if (caNames != null && !caNames.isEmpty()) {
						ca.setName(caNames.get(i));
					}
					if (caEmails != null && !caEmails.isEmpty()) {
						ca.setEmail(caEmails.get(i));
					}
					correspAuthor.add(ca);
				}
				publication.setCorrespAuthor(correspAuthor);
			}
		}
	}

	private boolean isFinal(Publication publication, PublicationPartName[] names, EnumMap<PublicationPartName, Boolean> parts, boolean oa, FetcherArgs fetcherArgs) {
		for (PublicationPartName name : names) {
			if (!publication.getPart(name).isFinal(fetcherArgs) && (parts == null || (parts.get(name) != null && parts.get(name)))) {
				return false;
			}
		}
		if (oa && !publication.isOA() && (parts == null || (parts.get(PublicationPartName.fulltext) != null && parts.get(PublicationPartName.fulltext)))) {
			return false;
		}
		return true;
	}

	private boolean idOnly(EnumMap<PublicationPartName, Boolean> parts) {
		if (parts == null) return false;
		if (parts.get(PublicationPartName.title) != null && parts.get(PublicationPartName.title)) return false;
		if (parts.get(PublicationPartName.keywords) != null && parts.get(PublicationPartName.keywords)) return false;
		if (parts.get(PublicationPartName.mesh) != null && parts.get(PublicationPartName.mesh)) return false;
		if (parts.get(PublicationPartName.efo) != null && parts.get(PublicationPartName.efo)) return false;
		if (parts.get(PublicationPartName.go) != null && parts.get(PublicationPartName.go)) return false;
		if (parts.get(PublicationPartName.theAbstract) != null && parts.get(PublicationPartName.theAbstract)) return false;
		if (parts.get(PublicationPartName.fulltext) != null && parts.get(PublicationPartName.fulltext)) return false;
		if (parts.get(PublicationPartName.pmid) != null && parts.get(PublicationPartName.pmid)) return true;
		if (parts.get(PublicationPartName.pmcid) != null && parts.get(PublicationPartName.pmcid)) return true;
		if (parts.get(PublicationPartName.doi) != null && parts.get(PublicationPartName.doi)) return true;
		return false;
	}

	private String getEuropepmcUri(Publication publication, FetcherPublicationState state, FetcherArgs fetcherArgs) {
		String europepmcQuery = "resulttype=core&format=xml";
		if (!publication.getPmid().isEmpty() && !state.europepmcPmid) {
			europepmcQuery += "&query=ext_id:" + publication.getPmid().getContent() + " src:med";
			state.europepmcPmid = true;
		} else if (!publication.getPmcid().isEmpty() && !state.europepmcPmcid) {
			europepmcQuery += "&query=pmcid:" + publication.getPmcid().getContent();
			state.europepmcPmcid = true;
		} else if (!publication.getDoi().isEmpty() && !state.europepmcDoi) {
			europepmcQuery += "&query=doi:" + publication.getDoi().getContent();
			state.europepmcDoi = true;
		} else {
			return null;
		}

		if (fetcherArgs.getPrivateArgs().getEuropepmcEmail() != null && !fetcherArgs.getPrivateArgs().getEuropepmcEmail().isEmpty()) {
			europepmcQuery += "&email=" + fetcherArgs.getPrivateArgs().getEuropepmcEmail();
		}

		String europepmcUri = null;
		try {
			europepmcUri = new URI("https", "www.ebi.ac.uk", "/europepmc/webservices/rest/search", europepmcQuery, null).toASCIIString();
		} catch (URISyntaxException e) {
			logger.error(e);
		}

		return europepmcUri;
	}

	private Element getEuropepmcResult(Document doc, Publication publication, FetcherPublicationState state) {
		Elements results = doc.select("resultList > result");

		Element hitCount = doc.getElementsByTag("hitCount").first();
		if (hitCount != null) {
			try {
				int count = Integer.parseInt(hitCount.text());
				if (count != results.size()) {
					logger.warn("Tag hitCount value ({}) does not match resultList size ({}) in {}", count, results.size(), doc.location());
				}
			} catch (NumberFormatException e) {
				logger.warn("Tag hitCount does not contain an integer in {}", doc.location());
			}
		} else {
			logger.warn("Tag hitCount not found in {}", doc.location());
		}

		if (results.size() > 1) {
			Element bestResult = null;
			String bestSource = null;
			for (Element result : results) {
				String pmid = null;
				Element pmidTag = result.selectFirst("pmid");
				if (pmidTag != null) {
					pmid = pmidTag.text();
					if (!PubFetcher.isPmid(pmid)) {
						logger.error("Invalid PMID {} in Europe PMC results {}", pmid, doc.location());
						pmid = null;
					}
				}
				String pmcid = null;
				Element pmcidTag = result.selectFirst("pmcid");
				if (pmcidTag != null) {
					pmcid = pmcidTag.text();
					if (!PubFetcher.isPmcid(pmcid)) {
						logger.error("Invalid PMCID {} in Europe PMC results {}", pmcid, doc.location());
						pmcid = null;
					}
				}
				String doi = null;
				Element doiTag = result.selectFirst("doi");
				if (doiTag != null) {
					doi = doiTag.text();
					if (!PubFetcher.isDoi(doi) || doi.indexOf(" ") > -1) {
						logger.error("Invalid DOI {} in Europe PMC results {}", doi, doc.location());
						doi = null;
					} else {
						doi = PubFetcher.normaliseDoi(doi);
					}
				}

				boolean mismatch = false;
				if (!publication.getPmid().isEmpty() && pmid != null && !publication.getPmid().getContent().equals(pmid)) {
					logger.error("Mismatch between current PMID {} and returned PMID {} in Europe PMC results {}", publication.getPmid().getContent(), pmid, doc.location());
					mismatch = true;
				}
				if (!publication.getPmcid().isEmpty() && pmcid != null && !publication.getPmcid().getContent().equals(pmcid)) {
					logger.error("Mismatch between current PMCID {} and returned PMCID {} in Europe PMC results {}", publication.getPmcid().getContent(), pmcid, doc.location());
					mismatch = true;
				}
				if (!publication.getDoi().isEmpty() && doi != null && !publication.getDoi().getContent().equals(doi)) {
					if (F1000_DOI.matcher(publication.getDoi().getContent()).matches() && F1000_DOI.matcher(doi).matches()) {
						logger.warn("Mismatch between current DOI {} and returned DOI {} in Europe PMC results {}", publication.getDoi().getContent(), doi, doc.location());
					} else {
						logger.error("Mismatch between current DOI {} and returned DOI {} in Europe PMC results {}", publication.getDoi().getContent(), doi, doc.location());
						mismatch = true;
					}
				}
				if (mismatch) continue;

				// https://europepmc.org/Help#whatserachingEPMC
				String source = "";
				Element sourceTag = result.selectFirst("source");
				if (sourceTag != null) {
					source = sourceTag.text();
				}
				if (bestSource == null
						|| source.equals("MED") && !bestSource.equals("MED")
						|| source.equals("PMC") && !bestSource.equals("MED") && !bestSource.equals("PMC")
						|| source.equals("PPR") && !bestSource.equals("MED") && !bestSource.equals("PMC") && !bestSource.equals("PPR")) {
					bestResult = result;
					bestSource = source;
				}
			}
			return bestResult;
		} else if (results.size() == 1) {
			return results.first();
		} else {
			if (state.europepmcDoi) {
				logger.warn("There are {} results for {}", results.size(), doc.location());
			} else {
				logger.error("There are {} results for {}", results.size(), doc.location());
			}
			return null;
		}
	}

	// https://europepmc.org/docs/EBI_Europe_PMC_Web_Service_Reference.pdf
	void fetchEuropepmc(Publication publication, FetcherPublicationState state, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) {
		if (state.europepmc) return;

		if (isFinal(publication, new PublicationPartName[] {
				PublicationPartName.pmid, PublicationPartName.pmcid, PublicationPartName.doi,
				PublicationPartName.title,
				PublicationPartName.keywords, PublicationPartName.mesh, PublicationPartName.efo, PublicationPartName.go,
				PublicationPartName.theAbstract, PublicationPartName.fulltext
			}, parts, true, fetcherArgs)) return;

		if (publication.getIdCount() < 1) {
			logger.error("Can't fetch publication with no IDs");
			return;
		}

		String europepmcUri = getEuropepmcUri(publication, state, fetcherArgs);
		if (europepmcUri == null) return;

		Document doc = getDoc(europepmcUri, publication, fetcherArgs);
		if (doc == null) return;

		Element europepmcResult = getEuropepmcResult(doc, publication, state);
		if (europepmcResult == null) return;

		state.europepmc = true;
		PublicationPartType type = PublicationPartType.europepmc;

		setIds(publication, europepmcResult, type, "pmid", "pmcid", "doi", false, doc.location(), true, fetcherArgs);

		// subtitle is already embedded in title
		setTitle(publication, europepmcResult, type, "result > title", null, doc.location(), parts, fetcherArgs);

		setKeywords(publication, europepmcResult, type, "keyword", doc.location(), false, parts, fetcherArgs);

		if (parts == null || (parts.get(PublicationPartName.mesh) != null && parts.get(PublicationPartName.mesh))) {
			if (!publication.getMeshTerms().isFinal(fetcherArgs)) {
				List<MeshTerm> meshTerms = new ArrayList<>();
				for (Element meshHeading : europepmcResult.getElementsByTag("meshHeading")) {
					MeshTerm meshTerm = new MeshTerm();

					Element majorTopic_YN = meshHeading.getElementsByTag("majorTopic_YN").first();
					if (majorTopic_YN != null) {
						meshTerm.setMajorTopic(majorTopic_YN.text().equalsIgnoreCase("Y"));
					} else {
						logger.warn("Tag majorTopic_YN not found in {}", doc.location());
					}

					Element descriptorName = meshHeading.getElementsByTag("descriptorName").first();
					if (descriptorName != null) {
						String descriptorNameText = descriptorName.text();
						if (descriptorNameText.isEmpty()) {
							logger.warn("Tag descriptorName has no content in {}", doc.location());
						}
						meshTerm.setTerm(descriptorNameText);
					} else {
						logger.warn("Tag descriptorName not found in {}", doc.location());
					}

					meshTerms.add(meshTerm);
				}
				publication.setMeshTerms(meshTerms, type, doc.location(), fetcherArgs);
			}
		}

		setAbstract(publication, europepmcResult, type, "abstractText", doc.location(), parts, fetcherArgs);

		Element isOpen = europepmcResult.getElementsByTag("isOpenAccess").first();
		if (isOpen != null && isOpen.text().equalsIgnoreCase("Y")) {
			state.europepmcHasFulltextXML = true;
			publication.setOA(true);
		}

		Element sourceTag = europepmcResult.selectFirst("source");
		if (sourceTag != null && sourceTag.text().equals("PPR")) {
			publication.setPreprint(true);
		}

		setJournalTitle(publication, europepmcResult, "journalInfo > journal > title", doc.location());

		// "The date of first publication, whichever is first, electronic or print publication. Where a date is not fully available e.g. year only, an algorithm is applied to determine the value"
		setPubDate(publication, europepmcResult, "firstPublicationDate", doc.location(), false);

		// "A count that indicates the number of times an article has been cited by other articles in our databases."
		setCitationsCount(publication, europepmcResult, "citedByCount", doc.location());

		setCorrespAuthor(publication, europepmcResult, true);

		Element inEPMC = europepmcResult.getElementsByTag("inEPMC").first();
		if (inEPMC != null && inEPMC.text().equalsIgnoreCase("Y")) {
			state.europepmcHasFulltextHTML = true;
		}

		Element hasPDFTag = europepmcResult.getElementsByTag("hasPDF").first();
		if (hasPDFTag != null && hasPDFTag.text().equalsIgnoreCase("Y")) {
			state.europepmcHasPDF = true;
		}

		Element isMined = europepmcResult.getElementsByTag("hasTextMinedTerms").first();
		if (isMined != null && isMined.text().equalsIgnoreCase("Y")) {
			state.europepmcHasMinedTerms = true;
		}
	}

	private boolean fetchCitationsCount(Publication publication, FetcherPublicationState state, FetcherArgs fetcherArgs) {
		if (state.europepmc) return false;

		if (publication.getIdCount() < 1) {
			logger.error("Can't fetch citations count for publication with no IDs");
			return false;
		}

		String europepmcUri = getEuropepmcUri(publication, state, fetcherArgs);
		if (europepmcUri == null) return false;

		Document doc = getDoc(europepmcUri, publication, fetcherArgs);
		if (doc == null) return false;

		Element europepmcResult = getEuropepmcResult(doc, publication, state);
		if (europepmcResult == null) return false;

		state.europepmc = true;

		// "A count that indicates the number of times an article has been cited by other articles in our databases."
		return setCitationsCount(publication, europepmcResult, "citedByCount", doc.location());
	}

	public boolean updateCitationsCount(Publication publication, FetcherArgs fetcherArgs) {
		FetcherPublicationState state = new FetcherPublicationState();
		if (fetchCitationsCount(publication, state, fetcherArgs)) return true;
		if (fetchCitationsCount(publication, state, fetcherArgs)) return true;
		if (fetchCitationsCount(publication, state, fetcherArgs)) return true;
		return false;
	}

	// https://www.ncbi.nlm.nih.gov/pmc/pmcdoc/tagging-guidelines/article/style.html
	// https://dtd.nlm.nih.gov/publishing/tag-library/2.3/
	// https://jats.nlm.nih.gov/publishing/tag-library/1.1/
	private boolean fillWithPubMedCentralXml(Publication publication, Document doc, PublicationPartType type, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) {
		if (doc.getElementsByTag("article").first() == null) {
			logger.error("No article found in {}", doc.location());
			return false;
		}

		for (Element texMath : doc.select("tex-math")) texMath.remove();

		setIds(publication, doc, type,
			"article > front article-id[pub-id-type=pmid]",
			type == PublicationPartType.europepmc_xml ? null : "article > front article-id[pub-id-type=pmcid], article > front article-id[pub-id-type=pmc]",
			"article > front article-id[pub-id-type=doi]", true, doc.location(), true, fetcherArgs);

		String titleSelector = "article > front title-group:first-of-type article-title";
		String subtitleSelector = "article > front title-group:first-of-type subtitle";
		setTitle(publication, doc, type, titleSelector, subtitleSelector, doc.location(), parts, fetcherArgs);

		setKeywords(publication, doc, type, "article > front kwd", doc.location(), false, parts, fetcherArgs);

		String abstractSelector = "article > front abstract > :not(sec), article > front abstract sec > :not(sec)";
		setAbstract(publication, doc, type, abstractSelector, doc.location(), parts, fetcherArgs);

		// includes supplementary-material and floats and also back matter glossary, notes and misc sections
		// but not signature block, acknowledgments, appendices, biography, footnotes and references
		// there might be omissions if references are contained somewhere deeper in the back matter structure
		setFulltext(publication, doc, type,	titleSelector, subtitleSelector, abstractSelector,
			"article > body > :not(sec):not(sig-block), article > body sec > :not(sec), " + // body
			"article > back > glossary term-head, article > back > glossary def-head, article > back > glossary term, article > back > glossary def, article > back > glossary td, " + // glossary
			"article > back > notes > :not(ref-list):not(:has(ref-list)), article > back > sec > :not(ref-list):not(:has(ref-list)), " + // notes, misc sections
			"article > floats-wrap > :not(ref-list):not(:has(ref-list)), article > floats-group > :not(ref-list):not(:has(ref-list))", doc.location(), parts, fetcherArgs); // floats

		setJournalTitle(publication, doc, "journal-title", doc.location());

		setCorrespAuthor(publication, doc, doc.location(), true);

		return true;
	}

	private void fillWithPubMedCentralHtml(Publication publication, Document doc, PublicationPartType type, EnumMap<PublicationPartName, Boolean> parts, boolean htmlMeta, FetcherArgs fetcherArgs, boolean europepmc) {
		setIds(publication, doc, type,
			europepmc ? ".epmc_citationName .abs_nonlink_metadata" : null,
			".article .fm-sec:first-of-type .fm-citation-pmcid .fm-citation-ids-label + span",
			".article .fm-sec:first-of-type .doi a", false, doc.location(), true, fetcherArgs);

		String titleSelector = ".article .fm-sec:first-of-type > .content-title";
		String subtitleSelector = ".article .fm-sec:first-of-type > .fm-subtitle";
		setTitle(publication, doc, type, titleSelector, subtitleSelector, doc.location(), parts, fetcherArgs);

		setKeywords(publication, doc, type, ".article .kwd-text", doc.location(), true, parts, fetcherArgs);

		String abstractSelector =
			".article h2[id^=__abstractid] ~ :not(div), " +
			".article h2[id^=__abstractid] ~ div > :not(.kwd-title):not(.kwd-text):not(.fig):not(.largeobj-link), " +
			".article h2[id^=Abs] ~ :not(div), " +
			".article h2[id^=Abs] ~ div > :not(.kwd-title):not(.kwd-text):not(.fig):not(.largeobj-link)" +
			(europepmc ? "" : (", " +
			".article h2[id^=idm]:matchesOwn((?i)^(Abstract|Significance|Synopsis|Author Summary)$) ~ :not(div), " +
			".article h2[id^=idm]:matchesOwn((?i)^(Abstract|Significance|Synopsis|Author Summary)$) ~ div > :not(.kwd-title):not(.kwd-text):not(.fig):not(.largeobj-link)"));
		setAbstract(publication, doc, type, abstractSelector, doc.location(), parts, fetcherArgs);

		setCorrespAuthor(publication, doc, doc.location(), false);

		String displayNoneSelector = "[style~=(?i)display[\\p{Z}\\p{Cc}]*:[\\p{Z}\\p{Cc}]*none]";
		for (Element displayNone : doc.select(displayNoneSelector)) displayNone.remove();

		if (parts == null || (parts.get(PublicationPartName.fulltext) != null && parts.get(PublicationPartName.fulltext))) {
			if (!publication.getFulltext().isFinal(fetcherArgs)) {
				String notFigTable = europepmc ? ":not(.fig):not(.table-wrap)" : "";
				String fulltext = text(doc,
					".article > div > [id].sec:not([id^=__]):not([id^=App]):not([id^=APP]):not([id~=-APP]):not([id^=Bib]):not([id^=ref]):not([id^=Abs]):not([id^=idm]):not([id^=rs]):not([id^=ack]) > :not(.sec):not(.goto)" + notFigTable + ", " +
					".article > div > [id].sec:not([id^=__]):not([id^=App]):not([id^=APP]):not([id~=-APP]):not([id^=Bib]):not([id^=ref]):not([id^=Abs]):not([id^=idm]):not([id^=rs]):not([id^=ack]) .sec > :not(.sec):not(.goto)" + notFigTable + ", " +
					".article > div > [id].bk-sec:not([id^=__]):not([id^=App]):not([id^=APP]):not([id~=-APP]):not([id^=Bib]):not([id^=ref]):not([id^=Abs]):not([id^=idm]):not([id^=rs]):not([id^=ack]) > :not(.goto), " +
					".article > div > [id~=^(__sec|__bodyid|__glossaryid|__notesid)].sec > :not(.sec):not(.goto)" + notFigTable + ", " +
					".article > div > [id~=^(__sec|__bodyid|__glossaryid|__notesid)].sec .sec > :not(.sec):not(.goto)" + notFigTable + ", " +
					".article > div > [id~=^(__sec|__bodyid|__glossaryid|__notesid)].bk-sec > :not(.goto)" +
					(europepmc ? "" : (", " +
					".article > div > [id^=idm].sec.headless > :not(.sec):not(.goto)" + notFigTable + ", " +
					".article > div > [id^=idm].sec.headless .sec > :not(.sec):not(.goto)" + notFigTable + ", " +
					".article > div > [id^=idm].sec:has(h2:matchesOwn((?i)^(Glossary|Abbreviations|Notes|Supplementary Materials?))) > :not(.sec):not(.goto), " +
					".article > div > [id^=idm].sec:has(h2:matchesOwn((?i)^(Glossary|Abbreviations|Notes|Supplementary Materials?))) .sec > :not(.sec):not(.goto)")), doc.location(), true, false);
				if (!fulltext.isEmpty()) {
					StringBuilder sb = new StringBuilder();
					sb.append(getTitleText(doc, titleSelector, subtitleSelector, doc.location()));
					sb.append("\n\n");
					sb.append(text(doc, abstractSelector, doc.location(), true, false));
					sb.append("\n\n");
					sb.append(fulltext);
					if (europepmc) {
						for (Element figTable : doc.select(".article [id].sec:not([id^=__abstractid]):not([id^=Abs]):not([id^=idm]:matchesOwn((?i)^(Abstract|Significance|Synopsis|Author Summary)$)) .fig > a, " +
								".article [id].sec:not([id^=__abstractid]):not([id^=Abs]):not([id^=idm]:matchesOwn((?i)^(Abstract|Significance|Synopsis|Author Summary)$)) .table-wrap > a")) {
							String figTableHref = figTable.attr("abs:href");
							if (!figTableHref.isEmpty()) {
								Document docFigTable = getDoc(figTableHref, publication, fetcherArgs);
								if (docFigTable != null) {
									for (Element displayNone : docFigTable.select(displayNoneSelector)) displayNone.remove();
									String figTableText = getFirstTrimmed(docFigTable, ".article > .fig, .article > .table-wrap, .table-wrap", doc.location(), true, false);
									if (!figTableText.isEmpty()) {
										sb.append("\n\n");
										sb.append(figTableText);
									}
								}
							} else {
								logger.warn("Missing href for .fig or .table-wrap link in {}", doc.location());
							}
						}
					}
					publication.setFulltext(sb.toString(), type, doc.location(), fetcherArgs);
				}
			}
		}

		if (htmlMeta) {
			// meta keywords are not good (citation_keywords is actually MeSH), so take only Ids
			HtmlMeta.fillWithIds(publication, doc, type, fetcherArgs);
		}
	}

	void fetchEuropepmcFulltextXml(Publication publication, FetcherPublicationState state, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) {
		if (state.europepmcFulltextXmlPmcid) return;
		if (!state.europepmcHasFulltextXML) return;

		if (isFinal(publication, new PublicationPartName[] {
				PublicationPartName.pmid, PublicationPartName.doi,
				PublicationPartName.title, PublicationPartName.keywords, PublicationPartName.theAbstract, PublicationPartName.fulltext
			}, parts, false, fetcherArgs)) return;

		String pmcid = publication.getPmcid().getContent();
		if (pmcid.isEmpty()) return;
		state.europepmcFulltextXmlPmcid = true;

		Document doc = getDoc(EUROPEPMC + pmcid + "/fullTextXML", publication, fetcherArgs);
		if (doc != null) {
			state.europepmcFulltextXml = fillWithPubMedCentralXml(publication, doc, PublicationPartType.europepmc_xml, parts, fetcherArgs);
		}
	}

	void fetchEuropepmcFulltextHtml(Publication publication, Links links, FetcherPublicationState state, EnumMap<PublicationPartName, Boolean> parts, boolean htmlMeta, FetcherArgs fetcherArgs) {
		if (state.europepmcFulltextHtmlPmcid) return;
		if (!state.europepmcHasFulltextHTML) return;

		if (isFinal(publication, new PublicationPartName[] {
				PublicationPartName.title, PublicationPartName.theAbstract, PublicationPartName.fulltext
			}, parts, false, fetcherArgs)
			&& (isFinal(publication, new PublicationPartName[] {
				PublicationPartName.pmid, PublicationPartName.pmcid, PublicationPartName.doi, PublicationPartName.keywords
			}, parts, false, fetcherArgs) || state.europepmcFulltextXml)) return;

		String pmcid = publication.getPmcid().getContent();
		if (pmcid.isEmpty()) return;
		state.europepmcFulltextHtmlPmcid = true;

		PublicationPartType type = PublicationPartType.europepmc_html;

		boolean pdfAdded = false;

		Document doc = getDoc(PubFetcher.EUROPEPMClink + pmcid, publication, fetcherArgs);
		if (doc != null) {
			fillWithPubMedCentralHtml(publication, doc, type, parts, htmlMeta, fetcherArgs, true);

			Element a = doc.select(".list_article_link a:containsOwn(PDF)").first();
			if (a != null) {
				String pdfHref = a.attr("abs:href");
				if (!pdfHref.isEmpty()) {
					links.add(pdfHref, type.toPdf(), doc.location(), publication, fetcherArgs, true);
					pdfAdded = true;
				} else {
					logger.warn("Missing href for PDF link in {}", doc.location());
				}
			} else {
				logger.warn("PDF link not found in {}", doc.location());
			}
		}

		if (!pdfAdded && state.europepmcHasPDF) {
			links.add(PubFetcher.EUROPEPMClink + pmcid + "?pdf=render", type.toPdf(), PubFetcher.EUROPEPMClink + pmcid, publication, fetcherArgs, true);
		}
	}

	private List<MinedTerm> getEuropepmcMinedTerms(String url, Publication publication, FetcherArgs fetcherArgs) {
		Map<String, List<String>> minedTerms = new LinkedHashMap<>();

		Document doc = getDoc(url, publication, fetcherArgs);

		if (doc != null) {
			Elements annotations = doc.getElementsByTag("annotation");
			if (annotations.isEmpty()) {
				logger.warn("No mined terms found in {}", doc.location());
			}
			for (Element annotation : annotations) {
				String term = "";
				Element termTag = annotation.getElementsByTag("exact").first();
				if (termTag != null) {
					term = termTag.text();
					if (term.isEmpty()) {
						logger.warn("Tag <exact> has no content in {}", doc.location());
					}
				} else {
					logger.warn("Tag <exact> not found in {}", doc.location());
				}

				int count = 1;
				Element countTag = annotation.getElementsByTag("frequency").first();
				if (countTag != null) {
					try {
						count = Integer.parseInt(countTag.text());
						if (count < 1) {
							logger.warn("Tag <frequency> has value less than 1 in {}", doc.location());
						}
					} catch (NumberFormatException e) {
						logger.warn("Tag <frequency> does not contain an integer in {}", doc.location());
					}
				}

				String uri = "";
				Element tags = annotation.getElementsByTag("tags").first();
				if (tags != null) {
					Element tag = annotation.getElementsByTag("tag").first();
					if (tag != null) {
						Element uriTag = annotation.getElementsByTag("uri").first();
						if (uriTag != null) {
							uri = uriTag.text();
							if (uri.isEmpty()) {
								logger.warn("Tag <uri> has no content in {}", doc.location());
							}
						} else {
							logger.warn("Tag <uri> not found in {}", doc.location());
						}
					} else {
						logger.warn("Tag <uri> not found in {}", doc.location());
					}
				} else {
					logger.warn("Tag <uri> not found in {}", doc.location());
				}

				if (!term.isEmpty() && count > 0 && !uri.isEmpty()) {
					if (minedTerms.get(uri) == null) {
						minedTerms.put(uri, new ArrayList<>());
					}
					for (int i = 0; i < count; ++i) {
						minedTerms.get(uri).add(term);
					}
				}
			}
		}

		List<MinedTerm> minedTermsList = new ArrayList<>();
		for (Map.Entry<String, List<String>> entry : minedTerms.entrySet()) {
			MinedTerm minedTerm = new MinedTerm();
			minedTerm.setTerm(entry.getValue().stream()
				.collect(Collectors.groupingBy(Function.identity(), Collectors.counting())).entrySet().stream()
					.max(Comparator.comparing(Map.Entry::getValue)).get().getKey());
			minedTerm.setCount(entry.getValue().size());
			minedTerm.setUri(entry.getKey());
			minedTermsList.add(minedTerm);
		}
		Collections.sort(minedTermsList);

		return minedTermsList;
	}

	void fetchEuropepmcMinedTermsEfo(Publication publication, FetcherPublicationState state, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) {
		if (state.europepmcMinedTermsEfo) return;
		if (!state.europepmcHasMinedTerms) return;

		if (isFinal(publication, new PublicationPartName[] { PublicationPartName.efo }, parts, false, fetcherArgs)) return;

		String articleIds = null;
		if (!publication.getPmcid().isEmpty() && !state.europepmcMinedTermsEfoPmcid) {
			articleIds = "articleIds=PMC%3A" + publication.getPmcid().getContent();
			state.europepmcMinedTermsEfoPmcid = true;
		} else if (!publication.getPmid().isEmpty() && !state.europepmcMinedTermsEfoPmid) {
			articleIds = "articleIds=MED%3A" + publication.getPmid().getContent();
			state.europepmcMinedTermsEfoPmid = true;
		} else {
			return;
		}

		String efo = EUROPEPMC_ANNOTATIONS + articleIds + "&type=Experimental%20Methods&format=XML";
		List<MinedTerm> efoTerms = getEuropepmcMinedTerms(efo, publication, fetcherArgs);
		if (!efoTerms.isEmpty()) {
			state.europepmcMinedTermsEfo = true;
			publication.setEfoTerms(efoTerms, PublicationPartType.europepmc, efo, fetcherArgs);
		}
	}

	void fetchEuropepmcMinedTermsGo(Publication publication, FetcherPublicationState state, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) {
		if (state.europepmcMinedTermsGo) return;
		if (!state.europepmcHasMinedTerms) return;

		if (isFinal(publication, new PublicationPartName[] { PublicationPartName.go }, parts, false, fetcherArgs)) return;

		String articleIds = null;
		if (!publication.getPmcid().isEmpty() && !state.europepmcMinedTermsGoPmcid) {
			articleIds = "articleIds=PMC%3A" + publication.getPmcid().getContent();
			state.europepmcMinedTermsGoPmcid = true;
		} else if (!publication.getPmid().isEmpty() && !state.europepmcMinedTermsGoPmid) {
			articleIds = "articleIds=MED%3A" + publication.getPmid().getContent();
			state.europepmcMinedTermsGoPmid = true;
		} else {
			return;
		}

		String go = EUROPEPMC_ANNOTATIONS + articleIds + "&type=Gene%20Ontology&format=XML";
		List<MinedTerm> goTerms = getEuropepmcMinedTerms(go, publication, fetcherArgs);
		if (!goTerms.isEmpty()) {
			state.europepmcMinedTermsGo = true;
			publication.setGoTerms(goTerms, PublicationPartType.europepmc, go, fetcherArgs);
		}
	}

	void fetchPubmedXml(Publication publication, FetcherPublicationState state, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) {
		if (state.pubmedXmlPmid) return;

		// keywords are usually missing (and if present, fetched from PMC)
		if (isFinal(publication, new PublicationPartName[] {
				PublicationPartName.pmid, PublicationPartName.pmcid, PublicationPartName.doi,
				PublicationPartName.title, PublicationPartName.mesh, PublicationPartName.theAbstract
			}, parts, false, fetcherArgs)) return;

		String pmid = publication.getPmid().getContent();
		if (pmid.isEmpty()) return;
		state.pubmedXmlPmid = true;

		PublicationPartType type = PublicationPartType.pubmed_xml;

		Document doc = getDoc(EUTILS + "efetch.fcgi?retmode=xml&db=pubmed&id=" + pmid, publication, fetcherArgs);
		if (doc != null) {
			if (doc.getElementsByTag("PubmedArticle").first() == null) {
				logger.error("No article found in {}", doc.location());
				return;
			}

			state.pubmedXml = true;

			setIds(publication, doc, type, "ArticleId[IdType=pubmed]", "ArticleId[IdType=pmc]", "ArticleId[IdType=doi]", false, doc.location(), true, fetcherArgs);

			// subtitle is already embedded in title
			setTitle(publication, doc, type, "ArticleTitle", null, doc.location(), parts, fetcherArgs);

			setKeywords(publication, doc, type, "Keyword", doc.location(), false, parts, fetcherArgs);

			if (parts == null || (parts.get(PublicationPartName.mesh) != null && parts.get(PublicationPartName.mesh))) {
				if (!publication.getMeshTerms().isFinal(fetcherArgs)) {
					List<MeshTerm> meshTerms = new ArrayList<>();
					for (Element descriptorName : doc.getElementsByTag("DescriptorName")) {
						MeshTerm meshTerm = new MeshTerm();

						String descriptorNameText = descriptorName.text();
						if (descriptorNameText.isEmpty()) {
							logger.warn("Tag DescriptorName has no content in {}", doc.location());
						}
						meshTerm.setTerm(descriptorNameText);

						String majorTopic_YN = descriptorName.attr("MajorTopicYN").trim();
						if (majorTopic_YN.isEmpty()) {
							logger.warn("Attribute MajorTopicYN has no content in {}", doc.location());
						}
						meshTerm.setMajorTopic(majorTopic_YN.equalsIgnoreCase("Y"));

						String descriptorNameUI = descriptorName.attr("UI").trim();
						if (descriptorNameUI.isEmpty()) {
							logger.warn("Attribute UI has no content in {}", doc.location());
						}
						meshTerm.setUniqueId(descriptorNameUI);

						meshTerms.add(meshTerm);
					}
					publication.setMeshTerms(meshTerms, type, doc.location(), fetcherArgs);
				}
			}

			setAbstract(publication, doc, type, "AbstractText", doc.location(), parts, fetcherArgs);

			setJournalTitle(publication, doc, "Journal > Title", doc.location());

			setPubDate(publication, doc, "ArticleDate", doc.location(), true);

			setCorrespAuthor(publication, doc, false);
		}
	}

	void fetchPubmedHtml(Publication publication, FetcherPublicationState state, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) {
		if (state.pubmedHtmlPmid) return;

		// keywords are usually missing (and if present, fetched from PMC)
		if (isFinal(publication, new PublicationPartName[] {
				PublicationPartName.title, PublicationPartName.theAbstract
			}, parts, false, fetcherArgs)
			&& (isFinal(publication, new PublicationPartName[] {
				PublicationPartName.pmid, PublicationPartName.pmcid, PublicationPartName.doi, PublicationPartName.mesh
			}, parts, false, fetcherArgs) || state.pubmedXml)) return;

		String pmid = publication.getPmid().getContent();
		if (pmid.isEmpty()) return;
		state.pubmedHtmlPmid = true;

		PublicationPartType type = PublicationPartType.pubmed_html;

		Document doc = getDoc(PubFetcher.PMIDlink + pmid, publication, fetcherArgs);
		if (doc != null) {
			if (doc.getElementById("article-details") == null) {
				logger.error("No article found in {}", doc.location());
				return;
			}

			setIds(publication, doc, type,
				"#article-details #full-view-identifiers .pubmed .current-id",
				"#article-details #full-view-identifiers .pmc .id-link",
				"#article-details #full-view-identifiers .doi .id-link", false, doc.location(), true, fetcherArgs);

			// subtitle is already embedded in title
			setTitle(publication, doc, type, "#article-details h1.heading-title", null, doc.location(), parts, fetcherArgs);

			setKeywords(publication, doc, type, "#article-details #abstract p:has(.sub-title:matchesOwn(^Keywords:$))", doc.location(), true, parts, fetcherArgs);

			if (parts == null || (parts.get(PublicationPartName.mesh) != null && parts.get(PublicationPartName.mesh))) {
				if (!publication.getMeshTerms().isFinal(fetcherArgs)) {
					List<MeshTerm> meshTerms = new ArrayList<>();
					String previousDescriptorNameText = "";
					for (Element descriptorName : doc.select("#article-details #mesh-terms button")) {
						MeshTerm meshTerm = new MeshTerm();

						String descriptorNameText = descriptorName.text();
						int slash = descriptorNameText.indexOf('/');
						if (slash >= 0) {
							descriptorNameText = descriptorNameText.substring(0, slash);
							if (descriptorNameText.equals(previousDescriptorNameText)) continue;
						} else if (!descriptorNameText.isEmpty() && descriptorNameText.charAt(descriptorNameText.length() - 1) == '*') {
							descriptorNameText = descriptorNameText.substring(0, descriptorNameText.length() - 1);
							meshTerm.setMajorTopic(true);
						}
						if (descriptorNameText.isEmpty()) {
							logger.warn("MeSH term has no content in {}", doc.location());
						}
						meshTerm.setTerm(descriptorNameText);

						previousDescriptorNameText = descriptorNameText;
						meshTerms.add(meshTerm);
					}
					publication.setMeshTerms(meshTerms, type, doc.location(), fetcherArgs);
				}
			}

			setAbstract(publication, doc, type, "#article-details #eng-abstract > p", doc.location(), parts, fetcherArgs);
		}
	}

	void fetchPmcXml(Publication publication, FetcherPublicationState state, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) {
		if (state.pmcXmlPmcid) return;

		if (isFinal(publication, new PublicationPartName[] {
				PublicationPartName.pmid, PublicationPartName.pmcid, PublicationPartName.doi,
				PublicationPartName.title, PublicationPartName.keywords, PublicationPartName.theAbstract, PublicationPartName.fulltext
			}, parts, false, fetcherArgs)) return;

		String pmcid = publication.getPmcid().getContent();
		if (pmcid.isEmpty()) return;
		state.pmcXmlPmcid = true;

		Document doc = getDoc(EUTILS + "efetch.fcgi?retmode=xml&db=pmc&id=" + PubFetcher.extractPmcid(pmcid), publication, fetcherArgs);
		if (doc != null) {
			state.pmcXml = fillWithPubMedCentralXml(publication, doc, PublicationPartType.pmc_xml, parts, fetcherArgs);
			if (state.pmcXml) {
				if (!doc.select("article-id[pub-id-type=manuscript]").isEmpty()) {
					state.pmcManuscript = true;
				}
			}
		}
	}

	void fetchPmcHtml(Publication publication, Links links, FetcherPublicationState state, EnumMap<PublicationPartName, Boolean> parts, boolean htmlMeta, FetcherArgs fetcherArgs) {
		if (state.pmcHtmlPmcid) return;

		if (isFinal(publication, new PublicationPartName[] {
				PublicationPartName.title, PublicationPartName.theAbstract, PublicationPartName.fulltext
			}, parts, false, fetcherArgs)
			&& (isFinal(publication, new PublicationPartName[] {
				PublicationPartName.pmcid, PublicationPartName.doi, PublicationPartName.keywords
			}, parts, false, fetcherArgs) || state.pmcXml)) return;

		String pmcid = publication.getPmcid().getContent();
		if (pmcid.isEmpty()) return;
		state.pmcHtmlPmcid = true;

		PublicationPartType type = PublicationPartType.pmc_html;

		boolean pdfAdded = false;

		Document doc = getDoc(PubFetcher.PMCIDlink + pmcid + "/", publication, fetcherArgs);
		if (doc != null) {
			fillWithPubMedCentralHtml(publication, doc, type, parts, htmlMeta, fetcherArgs, false);

			Element a = doc.select(".format-menu a:containsOwn(PDF)").first();
			if (a != null) {
				String pdfHref = a.attr("abs:href");
				if (!pdfHref.isEmpty()) {
					links.add(pdfHref, type.toPdf(), doc.location(), publication, fetcherArgs, true);
					pdfAdded = true;
				} else {
					logger.warn("Missing href for PDF link in {}", doc.location());
				}
			} else {
				logger.warn("PDF link not found in {}", doc.location());
			}
		}

		if (!pdfAdded && doc != null) {
			links.add(PubFetcher.PMCIDlink + pmcid + "/pdf/", type.toPdf(), PubFetcher.PMCIDlink + pmcid + "/", publication, fetcherArgs, true);
		}
	}

	private String getHrefSrcDst(Document doc, String src, String dst) {
		String href = null;
		if (src != null && dst != null) {
			String url = doc.location();
			if (url != null && !url.isEmpty()) {
				String newUrl = url.replaceFirst(src, dst);
				if (newUrl != null && !newUrl.isEmpty() && !newUrl.equals(url)) {
					try {
						new URL(newUrl);
						href = newUrl;
					} catch (MalformedURLException e) {
						logger.warn("New URL is malformed ({}) (old {}, src {}, dst {})", newUrl, url, src, dst);
					}
				} else {
					logger.warn("New URL ({}) is empty or equal to old URL ({}) (src {}, dst {})", newUrl, url, src, dst);
				}
			} else {
				logger.warn("Can't transform empty URL (src {}, dst {})", src, dst);
			}
		}
		return href;
	}

	private List<String> getHrefsA(Document doc, String a) {
		List<String> hrefs = new ArrayList<>();
		if (a != null && !a.trim().isEmpty()) {
			Elements aTags = doc.select(a.trim());
			if (aTags.isEmpty()) {
				logger.warn("Can't find link with {} in {}", a, doc.location());
			}
			for (Element aTag : aTags) {
				String aHref = aTag.attr("abs:href");
				if (aHref != null && !aHref.isEmpty()) {
					try {
						new URL(aHref);
						hrefs.add(aHref);
					} catch (MalformedURLException e) {
						logger.warn("Attribute href malformed for link found with {} in {}", a, doc.location());
					}
				} else {
					logger.warn("Attribute href empty for link found with {} in {}", a, doc.location());
				}
			}
		}
		return hrefs;
	}

	void fetchSite(Publication publication, String url, PublicationPartType type, String from, Links links, EnumMap<PublicationPartName, Boolean> parts, boolean htmlMeta, boolean keywords, FetcherArgs fetcherArgs) {
		if (keywords) {
			if (isFinal(publication, new PublicationPartName[] {
					PublicationPartName.pmid, PublicationPartName.pmcid, PublicationPartName.doi,
					PublicationPartName.title, PublicationPartName.keywords, PublicationPartName.theAbstract, PublicationPartName.fulltext
				}, parts, false, fetcherArgs)) return;
		} else {
			if (isFinal(publication, new PublicationPartName[] {
					PublicationPartName.pmid, PublicationPartName.pmcid, PublicationPartName.doi,
					PublicationPartName.title, PublicationPartName.theAbstract, PublicationPartName.fulltext
				}, parts, false, fetcherArgs)) return;
		}

		if (PubFetcher.isDoi(url)) {
			URL doiUrl = null;
			try {
				doiUrl = new URL(url);
				HttpURLConnection con = (HttpURLConnection) doiUrl.openConnection();
				con.setInstanceFollowRedirects(false);
				con.setConnectTimeout(fetcherArgs.getTimeout());
				con.setReadTimeout(fetcherArgs.getTimeout());
				con.setRequestProperty("User-Agent", fetcherArgs.getPrivateArgs().getUserAgent());
				con.addRequestProperty("Referer", doiUrl.getProtocol() + "://" + doiUrl.getAuthority());
				con.connect();
				if (con.getResponseCode() >= 300 && con.getResponseCode() < 400) {
					String location = con.getHeaderField("Location");
					if (location != null && !location.isEmpty()) {
						if (location.startsWith("/")) {
							location = doiUrl.getProtocol() + "://" + doiUrl.getAuthority() + location;
						}
						url = location;
						logger.info("    DOI {} redirects to {}", doiUrl, url);
					} else {
						logger.error("Empty DOI redirection for {}", doiUrl);
						return;
					}
				} else {
					logger.error("Illegal response code ({}) for DOI {}", con.getResponseCode(), doiUrl);
					return;
				}
			} catch (MalformedURLException e) {
			} catch (IOException e) {
				logger.error("Failed to connect to DOI " + doiUrl, e);
				return;
			}
		}

		if (publication.getFulltext().isFinal(fetcherArgs) || parts != null && (parts.get(PublicationPartName.fulltext) == null || !parts.get(PublicationPartName.fulltext))) {
			try {
				String host = new URL(url).getHost();
				for (Link visitedSite : publication.getVisitedSites()) {
					if (visitedSite.getUrl().getHost().equalsIgnoreCase(host)) {
						return;
					}
				}
			} catch (MalformedURLException e) {
			}
		}

		boolean javascript = scrape.getJavascript(url);

		Document doc = getDoc(url, publication, type, from, links, parts, javascript, fetcherArgs);

		// Elsevier uses JavaScript for redirecting, assuming it goes to ScienceDirect
		// better use API https://www.elsevier.com/solutions/sciencedirect/support/api
		if (doc != null) {
			Matcher elsevier_id = ELSEVIER_REDIRECT.matcher(doc.location());
			if (elsevier_id.matches()) {
				doc = getDoc(SCIENCEDIRECT_LINK + elsevier_id.group(1), publication, type, from, links, parts, true, fetcherArgs);
			}
		}

		if (doc != null && doc.location() == LIEBERTPUB_COOKIE_ABSENT) {
			return;
		}

		if (doc != null) {
			String finalUrl = doc.location();

			String site = scrape.getSite(finalUrl);
			if (site != null) {
				setIds(publication, doc, type, scrape.getSelector(site, ScrapeSiteKey.pmid), scrape.getSelector(site, ScrapeSiteKey.pmcid), scrape.getSelector(site, ScrapeSiteKey.doi), false, doc.location(), false, fetcherArgs);

				setTitle(publication, doc, type, scrape.getSelector(site, ScrapeSiteKey.title), scrape.getSelector(site, ScrapeSiteKey.subtitle), doc.location(), parts, fetcherArgs);

				setKeywords(publication, doc, type, scrape.getSelector(site, ScrapeSiteKey.keywords), doc.location(), false, parts, fetcherArgs);

				setKeywords(publication, doc, type, scrape.getSelector(site, ScrapeSiteKey.keywords_split), doc.location(), true, parts, fetcherArgs);

				setAbstract(publication, doc, type, scrape.getSelector(site, ScrapeSiteKey.theAbstract), doc.location(), parts, fetcherArgs);

				setFulltext(publication, doc, type, scrape.getSelector(site, ScrapeSiteKey.title), scrape.getSelector(site, ScrapeSiteKey.subtitle), scrape.getSelector(site, ScrapeSiteKey.theAbstract), scrape.getSelector(site, ScrapeSiteKey.fulltext), doc.location(), parts, fetcherArgs);

				String fulltextHrefSrcDst = getHrefSrcDst(doc, scrape.getSelector(site, ScrapeSiteKey.fulltext_src), scrape.getSelector(site, ScrapeSiteKey.fulltext_dst));
				if (fulltextHrefSrcDst != null) {
					links.add(fulltextHrefSrcDst, type, finalUrl, publication, fetcherArgs, false);
				}
				List<String> fulltextHrefsA = getHrefsA(doc, scrape.getSelector(site, ScrapeSiteKey.fulltext_a));
				for (String fulltextHrefA : fulltextHrefsA) {
					links.add(fulltextHrefA, type, finalUrl, publication, fetcherArgs, false);
				}

				String pdfHrefSrcDst = getHrefSrcDst(doc, scrape.getSelector(site, ScrapeSiteKey.pdf_src), scrape.getSelector(site, ScrapeSiteKey.pdf_dst));
				if (pdfHrefSrcDst != null) {
					links.add(pdfHrefSrcDst, type.toPdf(), finalUrl, publication, fetcherArgs, false);
				}
				List<String> pdfHrefsA = getHrefsA(doc, scrape.getSelector(site, ScrapeSiteKey.pdf_a));
				for (String pdfHrefA : pdfHrefsA) {
					links.add(pdfHrefA, type.toPdf(), finalUrl, publication, fetcherArgs, false);
				}

				setCorrespAuthor(publication, doc, scrape.getSelector(site, ScrapeSiteKey.corresp_author_names), scrape.getSelector(site, ScrapeSiteKey.corresp_author_emails), doc.location());
			} else {
				logger.warn("No scrape rules for {}", finalUrl);
				if (parts == null || (parts.get(PublicationPartName.title) != null && parts.get(PublicationPartName.title))) {
					publication.setTitle(getFirstTrimmed(doc, "title", doc.location(), false, true), PublicationPartType.webpage, finalUrl, fetcherArgs, false);
				}
				if (parts == null || (parts.get(PublicationPartName.fulltext) != null && parts.get(PublicationPartName.fulltext))) {
					publication.setFulltext(CleanWebpage.cleanedBody(doc, true), PublicationPartType.webpage, finalUrl, fetcherArgs);
				}
			}

			if (htmlMeta) {
				HtmlMeta.fillWith(publication, doc, type, links, fetcherArgs, parts, keywords, site);
			}

			try {
				publication.addVisitedSite(new Link(finalUrl, type, from));
			} catch (MalformedURLException e) {
				logger.error("Can't add malformed visited site {} found in {} of type {}", url, from, type);
			}
		}
	}

	private void fetchDoi(Publication publication, Links links, FetcherPublicationState state, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) {
		if (state.doi) return;

		if (isFinal(publication, new PublicationPartName[] {
				PublicationPartName.pmid, PublicationPartName.pmcid, PublicationPartName.doi,
				PublicationPartName.title, PublicationPartName.keywords, PublicationPartName.theAbstract, PublicationPartName.fulltext
			}, parts, false, fetcherArgs)) return;

		String doi = publication.getDoi().getContent();
		if (doi.isEmpty()) return;
		state.doi = true;

		String doiLink;
		try {
			doiLink = new URI("https", "doi.org", "/" + doi, null, null).toASCIIString();
		} catch (URISyntaxException e) {
			logger.error(e);
			return;
		}

		fetchSite(publication, doiLink, PublicationPartType.doi, PubFetcher.DOIlink + doi, links, parts, true, true, fetcherArgs);
	}

	void fetchOaDoi(Publication publication, Links links, FetcherPublicationState state, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) {
		if (state.oadoi) return;

		if (isFinal(publication, new PublicationPartName[] {
				PublicationPartName.title, PublicationPartName.theAbstract, PublicationPartName.fulltext
			}, parts, true, fetcherArgs)
			&& (isFinal(publication, new PublicationPartName[] {
				PublicationPartName.pmid, PublicationPartName.pmcid, PublicationPartName.doi,
			}, parts, true, fetcherArgs) || !idOnly(parts))) return;

		String doi = publication.getDoi().getContent();
		if (doi.isEmpty()) return;
		state.oadoi = true;

		String host = "api.unpaywall.org";
		ActiveHost activeHost = activateHost(host);
		if (Thread.currentThread().isInterrupted()) return;

		try {
			URLConnection con;
			try {
				String oaDOI = new URI("https", host, "/v2/" + doi, "email=" + fetcherArgs.getPrivateArgs().getOadoiEmail(), null).toASCIIString();
				logger.info("    GET oaDOI {}", oaDOI);
				con = PubFetcher.newConnection(oaDOI, fetcherArgs.getTimeout(), fetcherArgs.getPrivateArgs().getUserAgent());
			} catch (URISyntaxException | IOException e) {
				logger.error(e);
				return;
			}

			try (InputStreamReader reader = new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8)) {
				String finalUrl = con.getURL().toString();
				logger.info("    GOT oaDOI {}", finalUrl);

				ObjectMapper mapper = new ObjectMapper();
				mapper.enable(SerializationFeature.CLOSE_CLOSEABLE);
				JsonNode root = mapper.readTree(reader);

				if (!publication.isOA()) {
					JsonNode isOA = root.get("is_oa");
					if (isOA != null && isOA.asBoolean()) {
						publication.setOA(true);
					}
				}

				if (publication.getJournalTitle().isEmpty()) {
					JsonNode journalName = root.get("journal_name");
					if (journalName != null) {
						String journalNameText = journalName.asText();
						if (journalNameText != null && !journalNameText.trim().isEmpty() && !journalNameText.trim().equals("null")) {
							publication.setJournalTitle(journalNameText.trim());
						} else {
							logger.warn("Journal title empty in oaDOI {}", finalUrl);
						}
					} else {
						logger.warn("Journal title not found in oaDOI {}", finalUrl);
					}
				}

				JsonNode oaLocations = root.get("oa_locations");
				if (oaLocations != null) {
					for (JsonNode oaLocation : oaLocations) {
						String urlPdfText = null;
						JsonNode urlPdf = oaLocation.get("url_for_pdf");
						if (urlPdf != null) {
							urlPdfText = urlPdf.asText();
						}
						JsonNode url = oaLocation.get("url");
						if (url != null) {
							String urlText = url.asText();
							if (urlText != null && !urlText.isEmpty() && !urlText.equals("null") && !urlText.equals(urlPdfText)) {
								links.add(urlText, PublicationPartType.link_oadoi, finalUrl, publication, fetcherArgs, false);
							}
						}
						JsonNode urlLandingPage = oaLocation.get("url_for_landing_page");
						if (urlLandingPage != null) {
							String urlLandingPageText = urlLandingPage.asText();
							if (urlLandingPageText != null && !urlLandingPageText.isEmpty() && !urlLandingPageText.equals("null") && !urlLandingPageText.equals(urlPdfText)) {
								links.add(urlLandingPageText, PublicationPartType.link_oadoi, finalUrl, publication, fetcherArgs, false);
							}
						}
						if (urlPdfText != null && !urlPdfText.isEmpty() && !urlPdfText.equals("null")) {
							links.add(urlPdfText, PublicationPartType.pdf_oadoi, finalUrl, publication, fetcherArgs, false);
						}
					}
				}

				if (parts == null || (parts.get(PublicationPartName.title) != null && parts.get(PublicationPartName.title))) {
					// subtitle will be missing, for example:
					// https://api.oadoi.org/v2/10.1145/2618243.2618289?email=test@example.com
					// title: "MR-microT", should be "MR-microT: a MapReduce-based MicroRNA target prediction method"
					JsonNode title = root.get("title");
					if (title != null) {
						String titleText = title.asText();
						if (titleText != null && !titleText.isEmpty() && !titleText.equals("null")) {
							publication.setTitle(titleText, PublicationPartType.oadoi, finalUrl, fetcherArgs, false);
						}
					}
				}
			} catch (SocketTimeoutException e) {
				logger.warn(e);
				setFetchException(null, publication, null);
			} catch (IOException e) {
				logger.warn(e);
			} catch (Exception e) {
				// any checked exception
				logger.warn("Exception!", e);
				setFetchException(null, publication, null);
			}
		} finally {
			if (activeHost != null) {
				synchronized(activeHosts) {
					activeHost.decrement();
					if (activeHost.getCount() <= 0) {
						activeHosts.remove(activeHost);
					}
					activeHosts.notifyAll();
				}
			}
		}
	}

	private boolean fetchAll(Publication publication, Links links, FetcherPublicationState state, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) {
		logger.info("Fetch sources {}", publication.toStringId());

		String pmid = publication.getPmid().getContent();
		String pmcid = publication.getPmcid().getContent();
		String doi = publication.getDoi().getContent();

		// order and multiplicity is important
		fetchEuropepmc(publication, state, parts, fetcherArgs);
		fetchEuropepmc(publication, state, parts, fetcherArgs);
		fetchEuropepmc(publication, state, parts, fetcherArgs);
		fetchEuropepmcFulltextXml(publication, state, parts, fetcherArgs);

		// https://europepmc.org/developers
		// These protocols provide access to Open Access content and metadata.
		// It is not permissible to use any kind of automated process to bulk download other content from Europe PMC.
		//fetchEuropepmcFulltextHtml(publication, links, state, parts, true, fetcherArgs);

		fetchEuropepmcMinedTermsEfo(publication, state, parts, fetcherArgs);
		fetchEuropepmcMinedTermsEfo(publication, state, parts, fetcherArgs);
		fetchEuropepmcMinedTermsGo(publication, state, parts, fetcherArgs);
		fetchEuropepmcMinedTermsGo(publication, state, parts, fetcherArgs);
		fetchPubmedXml(publication, state, parts, fetcherArgs);
		fetchPubmedHtml(publication, state, parts, fetcherArgs);
		fetchPmcXml(publication, state, parts, fetcherArgs);

		// https://www.ncbi.nlm.nih.gov/pmc/tools/textmining/
		// Many of the articles in PMC are subject to traditional copyright restrictions and are not available for bulk downloading.
		//fetchPmcHtml(publication, links, state, parts, true, fetcherArgs);

		fetchDoi(publication, links, state, parts, fetcherArgs);
		fetchOaDoi(publication, links, state, parts, fetcherArgs);

		// https://www.ncbi.nlm.nih.gov/pmc/about/authorms/
		// The PMC Author Manuscript Dataset files are available for text mining.
		if (state.pmcManuscript) {
			fetchPmcHtml(publication, links, state, parts, true, fetcherArgs);
		}

		if (!pmid.isEmpty() && !publication.getPmid().isEmpty() && !pmid.equals(publication.getPmid().getContent())) {
			logger.error("PMID changed from {} to {}", pmid, publication.getPmid().getContent());
			return false;
		}
		if (!pmcid.isEmpty() && !publication.getPmcid().isEmpty() && !pmcid.equals(publication.getPmcid().getContent())) {
			logger.error("PMCID changed from {} to {}", pmcid, publication.getPmcid().getContent());
			return false;
		}
		if (!doi.isEmpty() && !publication.getDoi().isEmpty() && !doi.equals(publication.getDoi().getContent())) {
			logger.error("DOI changed from {} to {}", doi, publication.getDoi().getContent());
			return false;
		}

		return true;
	}

	private void fetchPublication(Publication publication, EnumMap<PublicationPartName, Boolean> parts, boolean reset, FetcherArgs fetcherArgs) {
		publication.setFetchException(false);

		if (reset) {
			logger.info("Resetting publication {}", publication.toStringId());
			publication.reset();
		}

		Links links = new Links();
		FetcherPublicationState state  = new FetcherPublicationState();

		boolean goon = true;

		int idCount = 0;
		while (publication.getIdCount() > idCount && goon) {
			idCount = publication.getIdCount();
			goon = fetchAll(publication, links, state, parts, fetcherArgs);
		}

		if (goon) {
			logger.info("Fetch links {}", publication.toStringId());
		}
		long start = System.currentTimeMillis();
		for (int linksFetched = 0; linksFetched < LINKS_LIMIT && goon; ++linksFetched) {
			if (fetcherArgs.isQuick() && System.currentTimeMillis() - start > fetcherArgs.getTimeout() * 3) {
				logger.info("Fetch links timeout");
				break;
			}

			if (isFinal(publication, new PublicationPartName[] {
					PublicationPartName.title, PublicationPartName.keywords, PublicationPartName.theAbstract, PublicationPartName.fulltext
				}, parts, false, fetcherArgs)
				&& (isFinal(publication, new PublicationPartName[] {
					PublicationPartName.pmid, PublicationPartName.pmcid, PublicationPartName.doi
				}, parts, false, fetcherArgs) || !idOnly(parts))) break;

			Link link = links.pop();
			if (link == null) break;

			if (!link.getType().isBetterThan(publication.getLowestType())) {
				break;
			}

			if (!link.getType().isPdf()) {
				if (link.getType() != PublicationPartType.link_oadoi) {
					fetchSite(publication, link.getUrl().toString(), link.getType(), link.getFrom(), links, parts, true, true, fetcherArgs);
				} else {
					fetchSite(publication, link.getUrl().toString(), link.getType(), link.getFrom(), links, parts, true, false, fetcherArgs);
				}
			} else if (SCIENCEDIRECT.matcher(link.getUrl().toString()).matches()) {
				getDoc(link.getUrl().toString(), publication, link.getType(), link.getFrom(), links, parts, true, fetcherArgs);
			} else {
				fetchPdf(link.getUrl().toString(), publication, link.getType(), link.getFrom(), links, parts, fetcherArgs);
			}

			while (publication.getIdCount() > idCount && goon) {
				idCount = publication.getIdCount();
				goon = fetchAll(publication, links, state, parts, fetcherArgs);
			}
		}

		if (!goon) {
			fetchPublication(publication, parts, true, fetcherArgs);
		}
	}

	public Publication initPublication(PublicationIds publicationIds, FetcherArgs fetcherArgs) {
		if (publicationIds == null) {
			logger.error("null IDs given for publication init");
			return null;
		}

		Publication publication = new Publication();

		String pmid = publicationIds.getPmid();
		String pmcid = publicationIds.getPmcid();
		String doi = publicationIds.getDoi();

		if (!pmid.isEmpty()) {
			if (PubFetcher.isPmid(pmid)) {
				publication.setPmid(pmid, PublicationPartType.external, publicationIds.getPmidUrl(), fetcherArgs);
			} else {
				logger.error("Unknown PMID: {}", pmid);
			}
		}

		if (!pmcid.isEmpty()) {
			if (PubFetcher.isPmcid(pmcid)) {
				publication.setPmcid(pmcid, PublicationPartType.external, publicationIds.getPmcidUrl(), fetcherArgs);
			} else {
				logger.error("Unknown PMCID: {}", pmcid);
			}
		}

		if (!doi.isEmpty()) {
			if (PubFetcher.isDoi(doi)) {
				publication.setDoi(doi, PublicationPartType.external, publicationIds.getDoiUrl(), fetcherArgs);
			} else {
				logger.error("Unknown DOI: {}", doi);
			}
		}

		if (publication.getIdCount() < 1) {
			logger.error("Can't init publication with no IDs");
			return null;
		}

		return publication;
	}

	public boolean getPublication(Publication publication, FetcherArgs fetcherArgs) {
		return getPublication(publication, null, fetcherArgs);
	}

	public boolean getPublication(Publication publication, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs) {
		if (publication == null) {
			logger.error("null publication given for getting publication");
			return false;
		}
		if (publication.getIdCount() < 1) {
			logger.error("Publication with no IDs given for getting publication");
			return false;
		}
		logger.info("Get publication {}", publication.toStringId());

		if (publication.canFetch(fetcherArgs)) {
			publication.updateCounters(fetcherArgs);

			fetchPublication(publication, parts, false, fetcherArgs);

			if (publication.isEmpty() && !idOnly(parts)) {
				logger.error("Empty publication returned for {}", publication.toStringId());
				// still return true, as publication metadata has been updated
			} else {
				logger.info("Got publication {}", publication.toStringId());
			}

			return true;
		} else {
			logger.info("Not fetching publication {}", publication.toStringId());
			return false;
		}
	}

	public Webpage initWebpage(String url) {
		if (url == null) {
			logger.error("null URL given for webpage init");
			return null;
		}
		try {
			new URL(url);
		} catch (MalformedURLException e) {
			logger.error("Malformed URL given for webpage init");
			return null;
		}
		Webpage webpage = new Webpage();
		webpage.setStartUrl(url);
		return webpage;
	}

	public boolean getWebpage(Webpage webpage, FetcherArgs fetcherArgs) {
		return getWebpage(webpage, null, null, null, fetcherArgs, true);
	}

	public boolean getWebpage(Webpage webpage, String title, String content, Boolean javascript, FetcherArgs fetcherArgs, boolean yaml) {
		if (webpage == null) {
			logger.error("null webpage given for getting webpage");
			return false;
		}
		if (webpage.getStartUrl().isEmpty()) {
			logger.error("Webpage with no start URL given for getting webpage");
			return false;
		}

		if (yaml) {
			title = null;
			content = null;
			javascript = null;
		}

		if (yaml) {
			logger.info("Get {}", webpage.getStartUrl());
		} else {
			logger.info("Get {}{} (with title selector '{}' and content selector '{}')", (javascript != null && javascript.equals(Boolean.valueOf(true))) ? "JavaScript " : "", webpage.getStartUrl(), title, content);
		}

		if (webpage.canFetch(fetcherArgs)) {
			webpage.updateCounters(fetcherArgs);

			Webpage newWebpage = new Webpage();
			newWebpage.setStartUrl(webpage.getStartUrl());

			if (yaml) {
				Map<String, String> startWebpage = scrape.getWebpage(newWebpage.getStartUrl());
				if (startWebpage != null) {
					String webpageJavascriptString = startWebpage.get(ScrapeWebpageKey.javascript.toString());
					if (webpageJavascriptString != null) {
						javascript = Boolean.valueOf(webpageJavascriptString);
					}
				} else {
					logger.warn("No scrape rules for start webpage {}", newWebpage.getStartUrl());
				}
			}

			Document doc = getDoc(newWebpage, javascript != null && javascript.equals(Boolean.valueOf(true)), fetcherArgs);

			if (doc != null && javascript == null) {
				boolean finalJavascript = !yaml;
				if (!finalJavascript) {
					Map<String, String> finalWebpage = scrape.getWebpage(newWebpage.getFinalUrl());
					finalJavascript = finalWebpage == null || finalWebpage.get(ScrapeWebpageKey.javascript.toString()) != null && Boolean.valueOf(finalWebpage.get(ScrapeWebpageKey.javascript.toString()));
				}
				if (finalJavascript) {
					int textLength = doc.text().length();
					if (textLength < fetcherArgs.getWebpageMinLengthJavascript() || !doc.select("noscript").isEmpty()) {
						String reason = null;
						if (textLength < fetcherArgs.getWebpageMinLengthJavascript()) {
							reason = "length " + textLength + " is less than min required " + fetcherArgs.getWebpageMinLengthJavascript();
						} else {
							reason = "webpage contains noscript tag";
						}
						logger.info("Refetching {} with JavaScript enabled as {}", newWebpage.getStartUrl(), reason);
						Webpage newWebpageJavascript = new Webpage();
						newWebpageJavascript.setStartUrl(newWebpage.getStartUrl());
						Document docJavascript = getDoc(newWebpageJavascript, true, fetcherArgs);
						if (docJavascript != null) {
							doc = docJavascript;
							newWebpage = newWebpageJavascript;
							int textLengthAfter = doc.text().length();
							if (textLength != textLengthAfter) {
								logger.info("Content length changed from {} to {}", textLength, textLengthAfter);
							} else {
								logger.info("Content length did not change with JavaScript");
							}
						} else {
							logger.warn("Discarding failed JavaScript webpage");
						}
					}
				}
			}

			if (doc != null) {
				if (yaml) {
					Map<String, String> finalWebpage = scrape.getWebpage(newWebpage.getFinalUrl());
					if (finalWebpage == null) {
						logger.warn("No scrape rules for final webpage {}", newWebpage.getFinalUrl());
					} else {
						if (finalWebpage.get(ScrapeWebpageKey.title.toString()) != null) {
							title = finalWebpage.get(ScrapeWebpageKey.title.toString());
						}
						if (finalWebpage.get(ScrapeWebpageKey.content.toString()) != null) {
							content = finalWebpage.get(ScrapeWebpageKey.content.toString());
						}
					}
					if (title != null && !title.isEmpty()) {
						newWebpage.setTitle(getFirstTrimmed(doc, title, doc.location(), true, true));
					} else if (finalWebpage != null && title != null && title.isEmpty()) {
						newWebpage.setTitle("");
					}
					if (content != null && !content.isEmpty()) {
						newWebpage.setContent(text(doc, content, doc.location(), true, true));
					} else if (finalWebpage != null && content != null && content.isEmpty()) {
						logger.info("Webpage content discarded");
					} else {
						newWebpage.setContent(CleanWebpage.cleanedBody(doc, false));
					}
					if (finalWebpage != null) {
						String license = finalWebpage.get(ScrapeWebpageKey.license.toString());
						if (license != null) {
							newWebpage.setLicense(getFirstTrimmed(doc, license, doc.location(), true, false));
						}
						String language = finalWebpage.get(ScrapeWebpageKey.language.toString());
						if (language != null) {
							newWebpage.setLanguage(text(doc, language, doc.location(), true, false).replaceAll("\n\n", ", "));
						}
					}
				} else {
					if (title != null && !title.isEmpty()) {
						newWebpage.setTitle(getFirstTrimmed(doc, title, doc.location(), true, true));
					}
					if (content != null && !content.isEmpty()) {
						newWebpage.setContent(text(doc, content, doc.location(), true, true));
					} else {
						newWebpage.setContent(CleanWebpage.cleanedBody(doc, false));
					}
				}
			}

			if (newWebpage.isEmpty()) {
				logger.error("Empty webpage returned for {}", newWebpage.getStartUrl());
			} else if (!newWebpage.isUsable(fetcherArgs)) {
				logger.warn("Non-usable webpage returned for {}", newWebpage.getStartUrl());
			}

			if (newWebpage.isFinal(fetcherArgs)
				|| !newWebpage.isFinal(fetcherArgs) && !newWebpage.isEmpty() && !webpage.isFinal(fetcherArgs)
				|| newWebpage.isEmpty() && webpage.isEmpty()) {
				webpage.overwrite(newWebpage);
				logger.info("Got webpage for {}", newWebpage.getStartUrl());
			} else {
				logger.warn("Not overwriting previous webpage for {}", newWebpage.getStartUrl());
			}

			return true;
		} else {
			logger.info("Not fetching webpage {}", webpage.getStartUrl());
			return false;
		}
	}
}
