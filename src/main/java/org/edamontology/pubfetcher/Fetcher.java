/*
 * Copyright © 2016, 2017, 2018 Erik Jaaniso
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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
import org.jsoup.Connection.Response;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.select.Selector.SelectorParseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class Fetcher {

	private static int LINKS_LIMIT = 10;

	private static final String EUROPEPMC = "https://www.ebi.ac.uk/europepmc/webservices/rest/";
	private static final String EUTILS = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/";

	private static final Pattern KEYWORDS_BEGIN = Pattern.compile("(?i)^[\\p{Z}\\p{Cc}]*keywords?[\\p{Z}\\p{Cc}]*:*[\\p{Z}\\p{Cc}]*");
	private static final Pattern SEPARATOR = Pattern.compile("[,;|]");

	private static final Pattern PMID_EXTRACT = Pattern.compile("(?i)pmid[\\p{Z}\\p{Cc}]*:*[\\p{Z}\\p{Cc}]*(" + FetcherCommon.PMID.pattern() + ")");
	private static final Pattern PMCID_EXTRACT = Pattern.compile("(?i)pmcid[\\p{Z}\\p{Cc}]*:*[\\p{Z}\\p{Cc}]*(" + FetcherCommon.PMCID.pattern() + ")");
	private static final Pattern DOI_EXTRACT = Pattern.compile("(?i)doi[\\p{Z}\\p{Cc}]*:*[\\p{Z}\\p{Cc}]*(" + FetcherCommon.DOI.pattern() + ")");

	private static final Pattern APPLICATION_PDF = Pattern.compile("(?i).*(application|image)/(pdf|octet-stream).*");
	private static final Pattern EXCEPTION_EXCEPTION = Pattern.compile("(?i)^https://doi\\.org/10\\.|\\.pdf$|\\.ps$|\\.gz$");

	private static final Pattern REGEX_ESCAPE = Pattern.compile("[^\\p{L}\\p{N}]");
	private static final Pattern REMOVE_AFTER_ASTERISK = Pattern.compile("^([^\\p{L}\\p{N}]+)[\\p{L}]+$");
	private static final String PHONE_ALLOWED = "[\\p{N} /.ext()+-]";
	private static final String PHONE_ALLOWED_END = "[\\p{N})]";
	private static final Pattern PHONE1 = Pattern.compile("(?i)tel[:.e]*[\\p{Z}\\p{Cc}]*(phone[:.]*)?[\\p{Z}\\p{Cc}]*(" + PHONE_ALLOWED + "+" + PHONE_ALLOWED_END + ")");
	private static final Pattern PHONE2 = Pattern.compile("(?i)phone[:.]*[\\p{Z}\\p{Cc}]*(" + PHONE_ALLOWED + "+" + PHONE_ALLOWED_END + ")");
	private static final Pattern EMAIL = Pattern.compile("(?i)e-?mail[:.]*[\\p{Z}\\p{Cc}]*([a-zA-Z0-9+._-]+@[a-zA-Z0-9.-]+\\.[a-z]{2,})");

	private static final Pattern ELSEVIER_REDIRECT = Pattern.compile("^https?://linkinghub\\.elsevier\\.com/retrieve/pii/(.+)$");
	private static final Pattern SCIENCEDIRECT = Pattern.compile("^https?://(www\\.)?sciencedirect\\.com/.+$");
	private static final String SCIENCEDIRECT_LINK = "https://www.sciencedirect.com/science/article/pii/";

	private final Scrape scrape;

	private final FetcherArgs fetcherArgs;

	public Fetcher(FetcherArgs fetcherArgs) throws IOException, ParseException {
		if (fetcherArgs == null) {
			throw new IllegalArgumentException("fetcherArgs is null");
		}
		scrape = new Scrape();
		this.fetcherArgs = fetcherArgs;

		// TODO
		Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF);
	}

	public Scrape getScrape() {
		return scrape;
	}

	public FetcherArgs getFetcherArgs() {
		return fetcherArgs;
	}

	private void setFetchException(Webpage webpage, Publication publication, String exceptionUrl) {
		if (webpage != null) {
			if (!webpage.isFetchException()) {
				System.err.println("Set fetching exception for webpage " + webpage.toStringId());
				webpage.setFetchException(true);
			}
		}
		if (publication != null && (exceptionUrl == null || !EXCEPTION_EXCEPTION.matcher(exceptionUrl).find())) {
			if (!publication.isFetchException()) {
				System.err.println("Set fetching exception for publication " + publication.toStringId());
				publication.setFetchException(true);
			}
		}
	}

	public Document getDoc(String url, Publication publication) {
		return getDoc(url, null, publication, null, null, null, null, false, false);
	}

	private Document getDoc(Webpage webpage, boolean javascript) {
		return getDoc(webpage.getStartUrl(), webpage, null, null, null, null, null, javascript, false);
	}

	private Document getDoc(String url, Publication publication, PublicationPartType type, String from, Links links, EnumMap<PublicationPartName, Boolean> parts, boolean javascript) {
		return getDoc(url, null, publication, type, from, links, parts, javascript, false);
	}

	private Document getDoc(String url, Webpage webpage, Publication publication, PublicationPartType type, String from, Links links, EnumMap<PublicationPartName, Boolean> parts, boolean javascript, boolean timeout) {
		Document doc = null;

		System.out.println("    GET " + url + (javascript ? " (with JavaScript)" : ""));

		try {
			if (webpage != null) {
				webpage.setStartUrl(url);
			}

			if (javascript) {
				try (WebClient webClient = new WebClient()) {
					webClient.getOptions().setUseInsecureSSL(true);
					webClient.getOptions().setRedirectEnabled(true);
					webClient.getOptions().setJavaScriptEnabled(true);
					webClient.getOptions().setCssEnabled(true);
					webClient.getOptions().setGeolocationEnabled(false);
					webClient.getOptions().setDoNotTrackEnabled(false);
					webClient.getOptions().setPrintContentOnFailingStatusCode(false);
					webClient.getOptions().setThrowExceptionOnFailingStatusCode(true);
					webClient.getOptions().setThrowExceptionOnScriptError(false);
					webClient.getOptions().setActiveXNative(false);
					webClient.getOptions().setTimeout(fetcherArgs.getTimeout());
					webClient.getOptions().setDownloadImages(false);

					webClient.setAjaxController(new NicelyResynchronizingAjaxController());

					Page page = webClient.getPage(url);

					String contentType = page.getWebResponse().getContentType();
					int statusCode = page.getWebResponse().getStatusCode();
					String finalUrl = page.getWebResponse().getWebRequest().getUrl().toString();

					if (webpage != null) {
						webpage.setContentType(contentType);
						webpage.setStatusCode(statusCode);
					}

					if (page.isHtmlPage()) {
						HtmlPage htmlPage = (HtmlPage) page;

						webClient.waitForBackgroundJavaScript(fetcherArgs.getTimeout());

						doc = Jsoup.parse(htmlPage.asXml(), finalUrl);
					} else {
						throw new UnsupportedMimeTypeException("Not a HTML page", contentType, finalUrl);
					}
				}
			} else {
				URL u = new URL(url);

				Response res = Jsoup.connect(url)
					.userAgent(fetcherArgs.getUserAgent())
					.referrer(u.getProtocol() + "://" + u.getAuthority())
					.timeout(fetcherArgs.getTimeout())
					// .validateTLSCertificates(false) // TODO deprecated
					.followRedirects(true)
					.ignoreHttpErrors(false)
					.ignoreContentType(false)
					.execute();

				if (webpage != null) {
					webpage.setContentType(res.contentType());
					webpage.setStatusCode(res.statusCode());
				}

				// bufferUp() because of bug in Jsoup // TODO
				doc = res.bufferUp().parse();
			}

			if (webpage != null) {
				webpage.setFinalUrl(doc.location());
				webpage.setTitle(doc.title());
				System.out.println("        final url: " + webpage.getFinalUrl());
				System.out.println("        content type: " + webpage.getContentType());
				System.out.println("        status code: " + webpage.getStatusCode());
				System.out.println("        title: " + webpage.getTitle());
				System.out.println("        content length: " + doc.text().length());
			} else {
				System.out.println("    GOT " + doc.location());
			}

			if (links != null) {
				links.addTriedLink(doc.location(), type, from);
			}
		} catch (MalformedURLException e) {
			// if the request URL is not a HTTP or HTTPS URL, or is otherwise malformed
			System.err.println(e);
			if (webpage != null || publication != null) {
				fetchPdf(url, webpage, publication, type, from, links, parts);
			}
		} catch (HttpStatusException e) {
			// if the response is not OK and HTTP response errors are not ignored
			System.err.println(e);
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
			System.err.println(e);
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
			System.err.println(e);
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
					fetchPdf(e.getUrl(), webpage, publication, type, from, links, parts);
				} else {
					System.err.println(e);
				}
			} else {
				System.err.println(e);
			}
		} catch (SocketTimeoutException e) {
			// if the connection times out
			System.err.println(e);
			if (!timeout) {
				doc = getDoc(url, webpage, publication, type, from, links, parts, javascript, true);
			} else {
				setFetchException(webpage, publication, null);
			}
		} catch (javax.net.ssl.SSLHandshakeException | javax.net.ssl.SSLProtocolException e) {
			System.err.println(e);
			if (!javascript) {
				doc = getDoc(url, webpage, publication, type, from, links, parts, true, timeout);
			}
		} catch (IOException e) {
			// if a connection or read error occurs
			System.err.println(e);
		} catch (Exception | org.jsoup.UncheckedIOException e) {
			e.printStackTrace();
			setFetchException(webpage, publication, null);
		}

		return doc;
	}

	private void fetchPdf(String url, Publication publication, PublicationPartType type, String from, Links links, EnumMap<PublicationPartName, Boolean> parts) {
		fetchPdf(url, null, publication, type, from, links, parts);
	}

	private void fetchPdf(String url, Webpage webpage, Publication publication, PublicationPartType type, String from, Links links, EnumMap<PublicationPartName, Boolean> parts) {
		// Don't fetch PDF if only keywords are missing
		if (webpage == null && (publication == null
			|| isFinal(publication, new PublicationPartName[] {
					PublicationPartName.title, PublicationPartName.theAbstract, PublicationPartName.fulltext
				}, parts, false))) return;

		// TODO
		Logger.getLogger("org.apache.pdfbox").setLevel(Level.SEVERE);

		System.out.println("    GET PDF " + url);

		URLConnection con;
		try {
			con = FetcherCommon.newConnection(url, fetcherArgs);
		} catch (IOException e) {
			System.err.println(e);
			return;
		}

		String finalUrl = con.getURL().toString();
		if (webpage != null) {
			webpage.setFinalUrl(finalUrl);
		}

		try (PDDocument doc = PDDocument.load(con.getInputStream())) {
			System.out.println("    GOT PDF " + finalUrl);
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
				|| publication != null && (!publication.isTitleFinal(fetcherArgs) && titlePart || !publication.isKeywordsFinal(fetcherArgs) && keywordsPart || !publication.isAbstractFinal(fetcherArgs) && abstractPart)) {
				PDDocumentInformation info = doc.getDocumentInformation();
				if (info != null) {
					if (webpage != null) {
						if (webpage.getTitle().isEmpty()) {
							String title = info.getTitle();
							if (title != null) {
								webpage.setTitle(title);
								if (!webpage.getTitle().isEmpty()) {
									System.out.println("        title: " + webpage.getTitle());
								}
							}
						}
					}
					if (publication != null) {
						if (!publication.isTitleFinal(fetcherArgs) && titlePart) {
							String title = info.getTitle();
							if (title != null) {
								publication.setTitle(title, type, finalUrl, fetcherArgs, true);
							}
						}
						if (!publication.isKeywordsFinal(fetcherArgs) && keywordsPart) {
							String keywords = info.getKeywords();
							if (keywords != null) {
								publication.setKeywords(Arrays.asList(SEPARATOR.split(keywords)), type, finalUrl, fetcherArgs, true);
							}
						}
						if (!publication.isAbstractFinal(fetcherArgs) && abstractPart) {
							String theAbstract = info.getSubject();
							if (theAbstract != null) {
								publication.setAbstract(theAbstract, type, finalUrl, fetcherArgs, true);
							}
						}
					}
				}
			}

			if (webpage != null || publication != null && !publication.isFulltextFinal(fetcherArgs) && fulltextPart) {
				try {
					PDFTextStripper stripper = new PDFTextStripper();
					String pdfText = stripper.getText(doc);
					if (pdfText != null) {
						if (webpage != null) {
							webpage.setContent(pdfText);
							System.out.println("        content length: " + webpage.getContent().length());
						}
						if (publication != null && !publication.isFulltextFinal(fetcherArgs) && fulltextPart) {
							publication.setFulltext(pdfText, type, finalUrl, fetcherArgs);
						}
					}
				} catch (IOException e) {
					System.out.println(e);
				}
			}

			if (webpage != null && webpage.getTitle().isEmpty()
				|| publication != null && (!publication.isTitleFinal(fetcherArgs) && titlePart || !publication.isKeywordsFinal(fetcherArgs) && keywordsPart || !publication.isAbstractFinal(fetcherArgs) && abstractPart)) {
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
											System.out.println("        title: " + webpage.getTitle());
										}
									}
								}
							}
							if (publication != null) {
								if (!publication.isTitleFinal(fetcherArgs) && titlePart) {
									String title = dc.getTitle();
									if (title != null) {
										publication.setTitle(title, type, finalUrl, fetcherArgs, true);
									}
								}
								if (!publication.isKeywordsFinal(fetcherArgs) && keywordsPart) {
									List<String> keywords = dc.getSubjects();
									if (keywords != null) {
										publication.setKeywords(keywords, type, finalUrl, fetcherArgs, true);
									}
								}
								if (!publication.isAbstractFinal(fetcherArgs) && abstractPart) {
									String theAbstract = dc.getDescription();
									if (theAbstract != null) {
										publication.setAbstract(theAbstract, type, finalUrl, fetcherArgs, true);
									}
								}
							}
						}

						if (publication != null && !publication.isKeywordsFinal(fetcherArgs) && keywordsPart) {
							AdobePDFSchema pdf = xmp.getAdobePDFSchema();
							if (pdf != null) {
								String keywords = pdf.getKeywords();
								if (keywords != null) {
									publication.setKeywords(Arrays.asList(SEPARATOR.split(keywords)), type, finalUrl, fetcherArgs, true);
								}
							}
						}
					} catch (IOException e) {
						System.out.println(e);
					} catch (XmpParsingException e) {
						System.err.println(e);
					} catch (IllegalArgumentException e) {
						System.err.println(e);
					}
				}
			}
		} catch (InvalidPasswordException e) {
			System.err.println(e);
		} catch (java.net.ConnectException | java.net.NoRouteToHostException e) {
			System.err.println(e);
			setFetchException(webpage, publication, null);
		} catch (SocketTimeoutException e) {
			System.err.println(e);
			setFetchException(webpage, publication, null);
		} catch (IOException e) {
			System.err.println(e);
		} catch (Exception e) {
			e.printStackTrace();
			setFetchException(webpage, publication, null);
		}
	}

	private static String getFirstTrimmed(Document doc, String selector, boolean logMissing) {
		selector = selector.trim();
		if (selector.isEmpty()) {
			System.err.println("Empty selector given for " + doc.location());
			return "";
		}
		Element tag = doc.select(selector).first();
		if (tag != null) {
			String firstTrimmed = tag.text();
			if (logMissing && firstTrimmed.isEmpty()) {
				System.err.println("Empty content in element selected by " + selector + " in " + doc.location());
			}
			return firstTrimmed;
		} else {
			if (logMissing) {
				System.err.println("No element found for selector " + selector + " in " + doc.location());
			}
			return "";
		}
	}

	private static Elements getAll(Document doc, String selector, boolean logMissing) {
		selector = selector.trim();
		if (selector.isEmpty()) {
			System.err.println("Empty selector given for " + doc.location());
			return new Elements();
		}
		Elements all = doc.select(selector);
		if (logMissing && all.isEmpty()) {
			System.err.println("No elements found for selector " + selector + " in " + doc.location());
		}
		return all;
	}

	private static String text(Document doc, String selector, boolean logMissing) {
		selector = selector.trim();
		if (selector.isEmpty()) {
			System.err.println("Empty selector given for " + doc.location());
			return "";
		}
		String text = getAll(doc, selector, false).stream()
			.filter(e -> e.hasText())
			.map(e -> e.text())
			.collect(Collectors.joining("\n\n"));
		if (logMissing && text.isEmpty()) {
			System.err.println("No text found for selector " + selector + " in " + doc.location());
		}
		return text;
	}

	private void setIds(Publication publication, Document doc, PublicationPartType type, String pmid, String pmcid, String doi, boolean prependPMC) {
		if (pmid != null && !pmid.trim().isEmpty()) {
			String pmidText = getFirstTrimmed(doc, pmid, false);
			if (FetcherCommon.isPmid(pmidText)) {
				publication.setPmid(pmidText, type, doc.location(), fetcherArgs);
			} else if (!pmidText.isEmpty()) {
				String pmidExtracted = null;
				Matcher pmidMatcher = PMID_EXTRACT.matcher(pmidText);
				if (pmidMatcher.find()) {
					pmidExtracted = pmidMatcher.group(1);
				}
				if (pmidExtracted != null && FetcherCommon.isPmid(pmidExtracted)) {
					publication.setPmid(pmidExtracted, type, doc.location(),fetcherArgs);
				} else {
					System.err.println("Trying to set invalid PMID " + pmidText + " from " + doc.location());
				}
			}
		}
		if (pmcid != null && !pmcid.trim().isEmpty()) {
			String pmcidText = getFirstTrimmed(doc, pmcid, false);
			if (prependPMC) pmcidText = "PMC" + pmcidText;
			if (FetcherCommon.isPmcid(pmcidText)) {
				publication.setPmcid(pmcidText, type, doc.location(), fetcherArgs);
			} else if (!pmcidText.isEmpty()) {
				String pmcidExtracted = null;
				Matcher pmcidMatcher = PMCID_EXTRACT.matcher(pmcidText);
				if (pmcidMatcher.find()) {
					pmcidExtracted = pmcidMatcher.group(1);
				}
				if (pmcidExtracted != null) {
					pmcidExtracted = pmcidExtracted.toUpperCase(Locale.ROOT);
				}
				if (pmcidExtracted != null && FetcherCommon.isPmcid(pmcidExtracted)) {
					publication.setPmcid(pmcidExtracted, type, doc.location(), fetcherArgs);
				} else {
					System.err.println("Trying to set invalid PMCID " + pmcidText + " from " + doc.location());
				}
			}
		}
		if (doi != null && !doi.trim().isEmpty()) {
			String doiText = getFirstTrimmed(doc, doi, false);
			if (FetcherCommon.isDoi(doiText) && doiText.indexOf(" ") < 0) {
				publication.setDoi(doiText, type, doc.location(), fetcherArgs);
			} else if (!doiText.isEmpty()) {
				String doiExtracted = null;
				Matcher doiMatcher = DOI_EXTRACT.matcher(doiText);
				if (doiMatcher.find()) {
					doiExtracted = doiMatcher.group(1);
				}
				if (doiExtracted != null && FetcherCommon.isDoi(doiExtracted) && doiExtracted.indexOf(" ") < 0) {
					publication.setDoi(doiExtracted, type, doc.location(),fetcherArgs);
				} else {
					System.err.println("Trying to set invalid DOI " + doiText + " from " + doc.location());
				}
			}
		}
	}

	private String getTitleText(Document doc, String title, String subtitle) {
		String titleText = getFirstTrimmed(doc, title, true);
		if (subtitle != null && !subtitle.trim().isEmpty()) {
			String subtitleText = getFirstTrimmed(doc, subtitle, false);
			if (!subtitleText.isEmpty()) {
				titleText += " : " + subtitleText;
			}
		}
		return titleText;
	}

	private void setTitle(Publication publication, Document doc, PublicationPartType type, String title, String subtitle, EnumMap<PublicationPartName, Boolean> parts) {
		if (parts == null || (parts.get(PublicationPartName.title) != null && parts.get(PublicationPartName.title))) {
			if (!publication.isTitleFinal(fetcherArgs) && title != null && !title.trim().isEmpty()) {
				publication.setTitle(getTitleText(doc, title, subtitle), type, doc.location(), fetcherArgs, false);
			}
		}
	}

	private void setKeywords(Publication publication, Document doc, PublicationPartType type, String keywords, boolean split, EnumMap<PublicationPartName, Boolean> parts) {
		if (parts == null || (parts.get(PublicationPartName.keywords) != null && parts.get(PublicationPartName.keywords))) {
			if (!publication.isKeywordsFinal(fetcherArgs) && keywords != null && !keywords.trim().isEmpty()) {
				Elements keywordsElements = getAll(doc, keywords, false); // false - don't complain about missing keywords
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
					publication.setKeywords(keywordsList, type, doc.location(), fetcherArgs, false);
				}
			}
		}
	}

	private void setAbstract(Publication publication, Document doc, PublicationPartType type, String theAbstract, EnumMap<PublicationPartName, Boolean> parts) {
		if (parts == null || (parts.get(PublicationPartName.theAbstract) != null && parts.get(PublicationPartName.theAbstract))) {
			if (!publication.isAbstractFinal(fetcherArgs) && theAbstract != null && !theAbstract.trim().isEmpty()) {
				publication.setAbstract(text(doc, theAbstract, true), type, doc.location(), fetcherArgs, false);
			}
		}
	}

	private void setFulltext(Publication publication, Document doc, PublicationPartType type, String title, String subtitle, String theAbstract, String fulltext, EnumMap<PublicationPartName, Boolean> parts) {
		if (parts == null || (parts.get(PublicationPartName.fulltext) != null && parts.get(PublicationPartName.fulltext))) {
			if (!publication.isFulltextFinal(fetcherArgs) && fulltext != null && !fulltext.trim().isEmpty()) {
				String fulltextText = text(doc, fulltext, true);
				if (!fulltextText.isEmpty()) {
					StringBuilder sb = new StringBuilder();
					if (title != null && !title.trim().isEmpty()) {
						sb.append(getTitleText(doc, title, subtitle));
						sb.append("\n\n");
					}
					if (theAbstract != null && !theAbstract.trim().isEmpty()) {
						sb.append(text(doc, theAbstract, true));
						sb.append("\n\n");
					}
					sb.append(fulltextText);
					publication.setFulltext(sb.toString(), type, doc.location(), fetcherArgs);
				}
			}
		}
	}

	private void setJournalTitle(Publication publication, Document doc, String selector) {
		selector = selector.trim();
		if (selector.isEmpty()) {
			System.err.println("Empty selector given for journal title in " + doc.location());
			return;
		}
		Element journalTitle = doc.selectFirst(selector);
		if (journalTitle != null && journalTitle.hasText()) {
			publication.setJournalTitle(journalTitle.text());
		} else {
			System.err.println("Journal title not found in " + doc.location());
		}
	}

	private void setPubDate(Publication publication, Document doc, String selector, boolean separated) {
		selector = selector.trim();
		if (selector.isEmpty()) {
			System.err.println("Empty selector given for publication date in " + doc.location());
			return;
		}
		Element pubDate = doc.selectFirst(selector);
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
					System.err.println("Publication date (separated) not found in " + doc.location());
				}
			} else {
				publication.setPubDate(pubDate.text());
			}
		} else {
			System.err.println("Publication date not found in " + doc.location());
		}
	}

	private void setCitationsCount(Publication publication, Document doc, String selector) {
		selector = selector.trim();
		if (selector.isEmpty()) {
			System.err.println("Empty selector given for citations count in " + doc.location());
			return;
		}
		Element citationsCount = doc.selectFirst(selector);
		if (citationsCount != null && citationsCount.hasText()) {
			publication.setCitationsCount(citationsCount.text());
		} else {
			System.err.println("Citations count not found in " + doc.location());
		}
	}

	private void addEmailPhoneUriXml(Element corresp, List<String> emails, List<String> phones, List<String> uris) {
		List<String> emailsCopy = new ArrayList<String>(emails);
		List<String> phonesCopy = new ArrayList<String>(phones);
		List<String> urisCopy = new ArrayList<String>(uris);

		List<String> phonesLocal = new ArrayList<>();
		for (Element emailTag : corresp.select("email")) {
			String email = emailTag.text();
			if (!email.isEmpty() && !emailsCopy.contains(email)) {
				emails.add(email);
			}
		}
		for (Element phoneTag : corresp.select("phone")) {
			String phone = phoneTag.text();
			if (!phone.isEmpty() && !phonesCopy.contains(phone)) {
				phonesLocal.add(phone);
			}
		}
		for (Element uriTag : corresp.select("uri, ext-link[ext-link-type=uri]")) {
			String uri = uriTag.text();
			if (!uri.isEmpty() && !urisCopy.contains(uri)) {
				uris.add(uri);
			}
		}
		phones.addAll(phonesLocal);

		addEmail(corresp.text(), emails);
		addPhone(corresp.text(), phones, phonesLocal);
	}

	private void addEmail(String text, List<String> emails) {
		Matcher emailMatcher = EMAIL.matcher(text);
		while (emailMatcher.find()) {
			String email = emailMatcher.group(1).trim();
			if (!email.isEmpty() && !emails.contains(email) && !emails.contains(new StringBuilder(email).reverse().toString())) {
				emails.add(email);
			}
		}
	}

	private void addPhone(String text, List<String> phones, List<String> phonesExclude) {
		List<String> phonesLocal = new ArrayList<>();
		Matcher phone1Matcher = PHONE1.matcher(text);
		while (phone1Matcher.find()) {
			String phone = phone1Matcher.group(2).trim();
			if (!phone.isEmpty() && (phonesExclude == null || !phonesExclude.contains(phone))) {
				phonesLocal.add(phone);
			}
		}
		Matcher phone2Matcher = PHONE2.matcher(text);
		while (phone2Matcher.find()) {
			String phone = phone2Matcher.group(1).trim();
			if (!phone.isEmpty() && !phonesLocal.contains(phone) && (phonesExclude == null || !phonesExclude.contains(phone))) {
				phonesLocal.add(phone);
			}
		}
		phones.addAll(phonesLocal);
	}

	private void setCorrespAuthor(Publication publication, Document doc, boolean xml) {
		List<String> names = new ArrayList<>();
		List<String> emails = new ArrayList<>();
		List<String> phones = new ArrayList<>();
		List<String> uris = new ArrayList<>();

		if (xml) {
			Elements contribCorrespAll = doc.select("contrib[corresp=yes], contrib:has(xref[ref-type=corresp])");
			if (contribCorrespAll.isEmpty()) {
				contribCorrespAll = doc.select("contrib:has(xref[ref-type=author-notes]):has(xref[rid~=(?i)fn[0-9]+]), contrib:has(xref[ref-type=author-notes]):has(xref[rid~=(?i)^N[0-9a-fx.]+$])");
			}
			for (Element contribCorresp : contribCorrespAll) {
				Element nameTag = contribCorresp.selectFirst("name");
				if (nameTag != null) {
					Element surnameTag = nameTag.selectFirst("surname");
					if (surnameTag != null) {
						String surname = surnameTag.text();
						if (!surname.isEmpty()) {
							String name = "";
							Element givenNamesTag = nameTag.selectFirst("given-names");
							if (givenNamesTag != null) {
								String givenNames = givenNamesTag.text();
								if (!givenNames.isEmpty()) {
									name += givenNames + " ";
								}
							}
							name += surname;
							names.add(name);
						}
					}
				}

				addEmailPhoneUriXml(contribCorresp, emails, phones, uris);
			}

			Elements notesCorrespAll = doc.select("author-notes > corresp");
			if (notesCorrespAll.isEmpty()) {
				notesCorrespAll = doc.select("author-notes > fn[id~=(?i)fn[0-9]+], author-notes > fn[id~=(?i)^N[0-9a-fx.]+$]");
			}
			for (Element notesCorresp : notesCorrespAll) {
				addEmailPhoneUriXml(notesCorresp, emails, phones, uris);
			}
		} else {
			for (Element contribCorrespSup : doc.select(".contrib-group a ~ sup:has(img[alt=corresponding author])")) {
				Element contribCorresp = new Elements(contribCorrespSup).prevAll("a").first();
				if (contribCorresp != null) {
					String name = contribCorresp.text();
					if (!name.isEmpty()) {
						names.add(name);
						try {
							String nameRegex = REGEX_ESCAPE.matcher(name).replaceAll("\\\\$0");
							Element contribEmail = doc.selectFirst(".contrib-email:matchesOwn((?i)^" + nameRegex + "), .fm-authors-info > div:not(:has(.contrib-email)):matches((?i)" + nameRegex + ")");
							if (contribEmail != null) {
								for (Element emailTag : contribEmail.select(".oemail")) {
									String email = new StringBuilder(emailTag.text()).reverse().toString();
									if (!email.isEmpty()) {
										emails.add(email);
									}
								}
								addEmail(contribEmail.text(), emails);
								addPhone(contribEmail.text(), phones, null);
							}
						} catch (SelectorParseException e) {
							System.err.println(e);
						}
					}
				}
			}

			List<String> namesCopy = new ArrayList<String>(names);
			List<String> emailsCopy = new ArrayList<String>(emails);
			List<String> phonesCopy = new ArrayList<String>(phones);

			Elements notesCorrespAll = doc.select(".fm-authors-info div[id~=(?i)cor[0-9]+], .fm-authors-info div[id~=(?i)caf[0-9]+], .fm-authors-info div[id~=(?i)^c[0-9]+], .fm-authors-info div[id~=(?i)^cr[0-9]+], .fm-authors-info div[id~=(?i)^cor$]");
			if (notesCorrespAll.isEmpty()) {
				notesCorrespAll = doc.select(".fm-authors-info div[id~=(?i)fn[0-9]+], .fm-authors-info div[id~=(?i)^N[0-9a-fx.]+$]");
			}
			for (Element notesCorresp : notesCorrespAll) {
				for (Element emailTag : notesCorresp.select(".oemail")) {
					String email = new StringBuilder(emailTag.text()).reverse().toString();
					if (!email.isEmpty() && !emailsCopy.contains(email)) {
						emails.add(email);
					}
				}
				String notesCorrespText = notesCorresp.text();
				addEmail(notesCorrespText, emails);
				addPhone(notesCorrespText, phones, phonesCopy);
				String sup = "";
				Element supTag = notesCorresp.selectFirst("sup");
				if (supTag != null) {
					sup = supTag.text();
				} else {
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
						for (Element contrib : doc.select(".contrib-group a ~ sup:matchesOwn(^" + sup + ",?$), .contrib-group a ~ .other:matchesOwn(^" + sup + ",?$)")) {
							Element nameTag = new Elements(contrib).prevAll("a").first();
							if (nameTag != null) {
								String name = nameTag.text();
								if (!name.isEmpty() && !namesCopy.contains(name)) {
									names.add(name);
								}
							}
						}
					} catch (SelectorParseException e) {
						System.err.println(e);
					}
				}
			}
		}

		String correspAuthor = String.join(", ", names);
		if (!emails.isEmpty()) {
			if (!correspAuthor.isEmpty()) correspAuthor += "; ";
			correspAuthor += String.join(", ", emails);
		}
		if (!phones.isEmpty()) {
			if (!correspAuthor.isEmpty()) correspAuthor += "; ";
			correspAuthor += String.join(", ", phones);
		}
		if (!uris.isEmpty()) {
			if (!correspAuthor.isEmpty()) correspAuthor += "; ";
			correspAuthor += String.join(", ", uris);
		}

		if (!correspAuthor.isEmpty()) {
			publication.setCorrespAuthor(correspAuthor);
		} else {
			System.err.println("Corresponding author not found in " + doc.location());
		}
	}

	private boolean isFinal(Publication publication, PublicationPartName[] names, EnumMap<PublicationPartName, Boolean> parts, boolean oa) {
		for (PublicationPartName name : names) {
			if (!publication.isPartFinal(name, fetcherArgs) && (parts == null || (parts.get(name) != null && parts.get(name)))) {
				return false;
			}
		}
		if (oa && !publication.isOA() && parts == null) {
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

	// https://europepmc.org/docs/EBI_Europe_PMC_Web_Service_Reference.pdf
	void fetchEuropepmc(Publication publication, FetcherPublicationState state, EnumMap<PublicationPartName, Boolean> parts) {
		if (state.europepmc) return;

		if (isFinal(publication, new PublicationPartName[] {
				PublicationPartName.pmid, PublicationPartName.pmcid, PublicationPartName.doi,
				PublicationPartName.title,
				PublicationPartName.keywords, PublicationPartName.mesh, PublicationPartName.efo, PublicationPartName.go,
				PublicationPartName.theAbstract, PublicationPartName.fulltext
			}, parts, true)) return;

		if (publication.getIdCount() < 1) {
			System.err.println("Can't fetch publication with no IDs");
			return;
		}

		String europepmcQuery = "resulttype=core&pageSize=1&format=xml";
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
			return;
		}

		if (fetcherArgs.getEuropepmcEmail() != null && !fetcherArgs.getEuropepmcEmail().isEmpty()) {
			europepmcQuery += "&email=" + fetcherArgs.getEuropepmcEmail();
		}

		String europepmc;
		try {
			europepmc = new URI("https", "www.ebi.ac.uk", "/europepmc/webservices/rest/search", europepmcQuery, null).toASCIIString();
		} catch (URISyntaxException e) {
			System.err.println(e);
			return;
		}

		PublicationPartType type = PublicationPartType.europepmc;

		Document doc = getDoc(europepmc, publication);
		if (doc != null) {
			int count = 0;

			try {
				Element hitCount = doc.getElementsByTag("hitCount").first();
				if (hitCount != null) {
					count = Integer.parseInt(hitCount.text());
				} else {
					System.err.println("Tag hitCount not found in " + doc.location());
				}
			} catch (NumberFormatException e) {
				System.err.println("Tag hitCount does not contain an integer in " + doc.location());
			}

			int realCount = doc.select("resultList > result").size();
			if (count != realCount) {
				System.err.println("Tag hitCount value (" + count + ") does not match resultList size (" + realCount + ") in " + doc.location());
			}

			if (realCount == 1) {
				state.europepmc = true;

				setIds(publication, doc, type, "pmid", "pmcid", "doi", false);

				// subtitle is already embedded in title
				setTitle(publication, doc, type, "result > title", null, parts);

				setKeywords(publication, doc, type, "keyword", false, parts);

				if (parts == null || (parts.get(PublicationPartName.mesh) != null && parts.get(PublicationPartName.mesh))) {
					if (!publication.isMeshTermsFinal(fetcherArgs)) {
						List<MeshTerm> meshTerms = new ArrayList<>();
						for (Element meshHeading : doc.getElementsByTag("meshHeading")) {
							MeshTerm meshTerm = new MeshTerm();

							Element majorTopic_YN = meshHeading.getElementsByTag("majorTopic_YN").first();
							if (majorTopic_YN != null) {
								meshTerm.setMajorTopic(majorTopic_YN.text().equalsIgnoreCase("Y"));
							} else {
								System.err.println("Tag majorTopic_YN not found in " + doc.location());
							}

							Element descriptorName = meshHeading.getElementsByTag("descriptorName").first();
							if (descriptorName != null) {
								String descriptorNameText = descriptorName.text();
								if (descriptorNameText.isEmpty()) {
									System.err.println("Tag descriptorName has no content in " + doc.location());
								}
								meshTerm.setTerm(descriptorNameText);
							} else {
								System.err.println("Tag descriptorName not found in " + doc.location());
							}

							meshTerms.add(meshTerm);
						}
						publication.setMeshTerms(meshTerms, type, doc.location(), fetcherArgs);
					}
				}

				setAbstract(publication, doc, type, "abstractText", parts);

				Element isOpen = doc.getElementsByTag("isOpenAccess").first();
				if (isOpen != null && isOpen.text().equalsIgnoreCase("Y")) {
					state.europepmcHasFulltextXML = true;
					publication.setOA(true);
				}

				setJournalTitle(publication, doc, "journalInfo > journal > title");

				// "The date of first publication, whichever is first, electronic or print publication. Where a date is not fully available e.g. year only, an algorithm is applied to determine the value"
				setPubDate(publication, doc, "firstPublicationDate", false);

				// "A count that indicates the number of times an article has been cited by other articles in our databases."
				setCitationsCount(publication, doc, "citedByCount");

				Element inEPMC = doc.getElementsByTag("inEPMC").first();
				if (inEPMC != null && inEPMC.text().equalsIgnoreCase("Y")) {
					state.europepmcHasFulltextHTML = true;
				}

				Element hasPDFTag = doc.getElementsByTag("hasPDF").first();
				if (hasPDFTag != null && hasPDFTag.text().equalsIgnoreCase("Y")) {
					state.europepmcHasPDF = true;
				}

				Element isMined = doc.getElementsByTag("hasTextMinedTerms").first();
				if (isMined != null && isMined.text().equalsIgnoreCase("Y")) {
					state.europepmcHasMinedTerms = true;
				}
			} else {
				System.err.println("There are " + realCount + " results for " + doc.location());
			}
		}
	}

	// https://www.ncbi.nlm.nih.gov/pmc/pmcdoc/tagging-guidelines/article/style.html
	// https://dtd.nlm.nih.gov/publishing/tag-library/2.3/
	// https://jats.nlm.nih.gov/publishing/tag-library/1.1/
	private boolean fillWithPubMedCentralXml(Publication publication, Document doc, PublicationPartType type, EnumMap<PublicationPartName, Boolean> parts) {
		if (doc.getElementsByTag("article").first() == null) {
			System.err.println("No article found in " + doc.location());
			return false;
		}

		for (Element texMath : doc.select("tex-math")) texMath.remove();

		setIds(publication, doc, type,
			"article > front article-id[pub-id-type=pmid]",
			"article > front article-id[pub-id-type=pmcid], article > front article-id[pub-id-type=pmc]",
			"article > front article-id[pub-id-type=doi]", true);

		String titleSelector = "article > front title-group:first-of-type article-title";
		String subtitleSelector = "article > front title-group:first-of-type subtitle";
		setTitle(publication, doc, type, titleSelector, subtitleSelector, parts);

		setKeywords(publication, doc, type, "article > front kwd", false, parts);

		String abstractSelector = "article > front abstract > :not(sec), article > front abstract sec > :not(sec)";
		setAbstract(publication, doc, type, abstractSelector, parts);

		// includes supplementary-material and floats and also back matter glossary, notes and misc sections
		// but not signature block, acknowledgments, appendices, biography, footnotes and references
		// there might be omissions if references are contained somewhere deeper in the back matter structure
		setFulltext(publication, doc, type,	titleSelector, subtitleSelector, abstractSelector,
			"article > body > :not(sec):not(sig-block), article > body sec > :not(sec), " + // body
			"article > back > glossary term-head, article > back > glossary def-head, article > back > glossary term, article > back > glossary def, article > back > glossary td, " + // glossary
			"article > back > notes > :not(ref-list):not(:has(ref-list)), article > back > sec > :not(ref-list):not(:has(ref-list)), " + // notes, misc sections
			"article > floats-wrap > :not(ref-list):not(:has(ref-list)), article > floats-group > :not(ref-list):not(:has(ref-list))", parts); // floats

		setJournalTitle(publication, doc, "journal-title");

		setCorrespAuthor(publication, doc, true);

		return true;
	}

	private void fillWithPubMedCentralHtml(Publication publication, Document doc, PublicationPartType type, EnumMap<PublicationPartName, Boolean> parts, boolean htmlMeta) {
		setIds(publication, doc, type,
			".epmc_citationName .abs_nonlink_metadata", // only in europepmc
			".article .fm-sec:first-of-type .fm-citation-pmcid .fm-citation-ids-label + span",
			".article .fm-sec:first-of-type .doi a", false);

		String titleSelector = ".article .fm-sec:first-of-type > .content-title";
		String subtitleSelector = ".article .fm-sec:first-of-type > .fm-subtitle";
		setTitle(publication, doc, type, titleSelector, subtitleSelector, parts);

		setKeywords(publication, doc, type, ".article .kwd-text", true, parts);

		String abstractSelector =
			".article h2[id^=__abstractid] ~ :not(div), " +
			".article h2[id^=__abstractid] ~ div > :not(.kwd-title):not(.kwd-text), " +
			".article h2[id^=Abs] ~ :not(div), " +
			".article h2[id^=Abs] ~ div > :not(.kwd-title):not(.kwd-text)";
		setAbstract(publication, doc, type, abstractSelector, parts);

		setCorrespAuthor(publication, doc, false);

		String displayNoneSelector = "[style~=(?i)display[\\p{Z}\\p{Cc}]*:[\\p{Z}\\p{Cc}]*none]";
		for (Element displayNone : doc.select(displayNoneSelector)) displayNone.remove();

		if (parts == null || (parts.get(PublicationPartName.fulltext) != null && parts.get(PublicationPartName.fulltext))) {
			if (!publication.isFulltextFinal(fetcherArgs)) {
				String fulltext = text(doc,
					".article > div > [id].sec:not([id^=__]):not([id^=App]):not([id^=APP]):not([id~=-APP]):not([id^=Bib]):not([id^=ref]):not([id^=Abs]) > :not(.sec):not(.goto):not(.fig):not(.table-wrap), " +
					".article > div > [id].sec:not([id^=__]):not([id^=App]):not([id^=APP]):not([id~=-APP]):not([id^=Bib]):not([id^=ref]):not([id^=Abs]) .sec > :not(.sec):not(.goto):not(.fig):not(.table-wrap), " +
					".article > div > [id].bk-sec:not([id^=__]):not([id^=App]):not([id^=APP]):not([id~=-APP]):not([id^=Bib]):not([id^=ref]):not([id^=Abs]) > :not(.goto), " +
					".article > div > [id~=^(__sec|__bodyid|__glossaryid|__notesid)].sec > :not(.sec):not(.goto):not(.fig):not(.table-wrap), " +
					".article > div > [id~=^(__sec|__bodyid|__glossaryid|__notesid)].sec .sec > :not(.sec):not(.goto):not(.fig):not(.table-wrap), " +
					".article > div > [id~=^(__sec|__bodyid|__glossaryid|__notesid)].bk-sec > :not(.goto)", true);
				if (!fulltext.isEmpty()) {
					StringBuilder sb = new StringBuilder();
					sb.append(getTitleText(doc, titleSelector, subtitleSelector));
					sb.append("\n\n");
					sb.append(text(doc, abstractSelector, true));
					sb.append("\n\n");
					sb.append(fulltext);
					for (Element figTable : doc.select(".article [id].sec:not([id^=__abstractid]):not([id^=Abs]) .fig > a, " +
							".article [id].sec:not([id^=__abstractid]):not([id^=Abs]) .table-wrap > a")) {
						String figTableHref = figTable.attr("abs:href");
						if (!figTableHref.isEmpty()) {
							Document docFigTable = getDoc(figTableHref, publication);
							if (docFigTable != null) {
								for (Element displayNone : docFigTable.select(displayNoneSelector)) displayNone.remove();
								String figTableText = getFirstTrimmed(docFigTable, ".article > .fig, .article > .table-wrap", true);
								if (!figTableText.isEmpty()) {
									sb.append("\n\n");
									sb.append(figTableText);
								}
							}
						} else {
							System.err.println("Missing href for .fig or .table-wrap link in " + doc.location());
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

	void fetchEuropepmcFulltextXml(Publication publication, FetcherPublicationState state, EnumMap<PublicationPartName, Boolean> parts) {
		if (state.europepmcFulltextXmlPmcid) return;
		if (!state.europepmcHasFulltextXML) return;

		if (isFinal(publication, new PublicationPartName[] {
				PublicationPartName.pmid, PublicationPartName.pmcid, PublicationPartName.doi,
				PublicationPartName.title, PublicationPartName.keywords, PublicationPartName.theAbstract, PublicationPartName.fulltext
			}, parts, false)) return;

		String pmcid = publication.getPmcid().getContent();
		if (pmcid.isEmpty()) return;
		state.europepmcFulltextXmlPmcid = true;

		Document doc = getDoc(EUROPEPMC + pmcid + "/fullTextXML", publication);
		if (doc != null) {
			state.europepmcFulltextXml = fillWithPubMedCentralXml(publication, doc, PublicationPartType.europepmc_xml, parts);
		}
	}

	void fetchEuropepmcFulltextHtml(Publication publication, Links links, FetcherPublicationState state, EnumMap<PublicationPartName, Boolean> parts, boolean htmlMeta) {
		if (state.europepmcFulltextHtmlPmcid) return;
		if (!state.europepmcHasFulltextHTML) return;

		if (isFinal(publication, new PublicationPartName[] {
				PublicationPartName.title, PublicationPartName.theAbstract, PublicationPartName.fulltext
			}, parts, false)
			&& (isFinal(publication, new PublicationPartName[] {
				PublicationPartName.pmid, PublicationPartName.pmcid, PublicationPartName.doi, PublicationPartName.keywords
			}, parts, false) || state.europepmcFulltextXml)) return;

		String pmcid = publication.getPmcid().getContent();
		if (pmcid.isEmpty()) return;
		state.europepmcFulltextHtmlPmcid = true;

		PublicationPartType type = PublicationPartType.europepmc_html;

		boolean pdfAdded = false;

		Document doc = getDoc(FetcherCommon.EUROPEPMClink + pmcid, publication);
		if (doc != null) {
			fillWithPubMedCentralHtml(publication, doc, type, parts, htmlMeta);

			Element a = doc.select(".list_article_link a:containsOwn(PDF)").first();
			if (a != null) {
				String pdfHref = a.attr("abs:href");
				if (!pdfHref.isEmpty()) {
					links.add(pdfHref, type.toPdf(), doc.location(), publication, fetcherArgs, true);
					pdfAdded = true;
				} else {
					System.err.println("Missing href for PDF link in " + doc.location());
				}
			} else {
				System.err.println("PDF link not found in " + doc.location());
			}
		}

		if (!pdfAdded && state.europepmcHasPDF) {
			links.add(FetcherCommon.EUROPEPMClink + pmcid + "?pdf=render", type.toPdf(), FetcherCommon.EUROPEPMClink + pmcid, publication, fetcherArgs, true);
		}
	}

	private List<MinedTerm> getEuropepmcMinedTerms(String url, Publication publication) {
		List<MinedTerm> minedTerms = new ArrayList<>();

		Document doc = getDoc(url, publication);

		if (doc != null) {
			Elements elements = doc.getElementsByTag("tmSummary");
			if (elements.isEmpty()) {
				System.err.println("No mined terms found in " + doc.location());
			}
			for (Element tmSummary : elements) {
				MinedTerm minedTerm = new MinedTerm();

				Element term = tmSummary.getElementsByTag("term").first();
				if (term != null) {
					String termText = term.text();
					if (termText.isEmpty()) {
						System.err.println("Tag term has no content in " + doc.location());
					}
					minedTerm.setTerm(termText);
				} else {
					System.err.println("Tag term not found in " + doc.location());
				}

				Element count = tmSummary.getElementsByTag("count").first();
				if (count != null) {
					try {
						int countInt = Integer.parseInt(count.text());
						if (countInt < 1) {
							System.err.println("Tag count has value less than 1 in " + doc.location());
						}
						minedTerm.setCount(countInt);
					} catch (NumberFormatException e) {
						System.err.println("Tag count does not contain an integer in " + doc.location());
					}
				} else {
					System.err.println("Tag count not found in " + doc.location());
				}

				Element altNameList = tmSummary.getElementsByTag("altNameList").first();
				if (altNameList != null) {
					List<String> altNames = new ArrayList<>();
					for (Element altName : altNameList.getElementsByTag("altName")) {
						altNames.add(altName.text());
					}
					minedTerm.setAltNames(altNames);
				}

				Element dbName = tmSummary.getElementsByTag("dbName").first();
				if (dbName != null) {
					minedTerm.setDbName(dbName.text());
				}

				Element dbIdList = tmSummary.getElementsByTag("dbIdList").first();
				if (dbIdList != null) {
					List<String> dbIds = new ArrayList<>();
					for (Element dbId : dbIdList.getElementsByTag("dbId")) {
						dbIds.add(dbId.text());
					}
					minedTerm.setDbIds(dbIds);
				}

				minedTerms.add(minedTerm);
			}
		}

		return minedTerms;
	}

	void fetchEuropepmcMinedTermsEfo(Publication publication, FetcherPublicationState state, EnumMap<PublicationPartName, Boolean> parts) {
		if (state.europepmcMinedTermsEfo) return;
		if (!state.europepmcHasMinedTerms) return;

		if (isFinal(publication, new PublicationPartName[] { PublicationPartName.efo }, parts, false)) return;

		// src/ext_id/textMinedTerms/[semantic_type]/[page]/[pageSize]/[format]
		String ext_id = null;
		if (!publication.getPmcid().isEmpty() && !state.europepmcMinedTermsEfoPmcid) {
			ext_id = "PMC/" + publication.getPmcid().getContent() + "/textMinedTerms";
			state.europepmcMinedTermsEfoPmcid = true;
		} else if (!publication.getPmid().isEmpty() && !state.europepmcMinedTermsEfoPmid) {
			ext_id = "MED/" + publication.getPmid().getContent() + "/textMinedTerms";
			state.europepmcMinedTermsEfoPmid = true;
		} else {
			return;
		}

		String efo = EUROPEPMC + ext_id + "/EFO" + "/1/100";
		List<MinedTerm> efoTerms = getEuropepmcMinedTerms(efo, publication);
		if (!efoTerms.isEmpty()) {
			state.europepmcMinedTermsEfo = true;
			publication.setEfoTerms(efoTerms, PublicationPartType.europepmc, efo, fetcherArgs);
		}
	}

	void fetchEuropepmcMinedTermsGo(Publication publication, FetcherPublicationState state, EnumMap<PublicationPartName, Boolean> parts) {
		if (state.europepmcMinedTermsGo) return;
		if (!state.europepmcHasMinedTerms) return;

		if (isFinal(publication, new PublicationPartName[] { PublicationPartName.go }, parts, false)) return;

		// src/ext_id/textMinedTerms/[semantic_type]/[page]/[pageSize]/[format]
		String ext_id = null;
		if (!publication.getPmcid().isEmpty() && !state.europepmcMinedTermsGoPmcid) {
			ext_id = "PMC/" + publication.getPmcid().getContent() + "/textMinedTerms";
			state.europepmcMinedTermsGoPmcid = true;
		} else if (!publication.getPmid().isEmpty() && !state.europepmcMinedTermsGoPmid) {
			ext_id = "MED/" + publication.getPmid().getContent() + "/textMinedTerms";
			state.europepmcMinedTermsGoPmid = true;
		} else {
			return;
		}

		String go = EUROPEPMC + ext_id + "/GO_TERM" + "/1/100";
		List<MinedTerm> goTerms = getEuropepmcMinedTerms(go, publication);
		if (!goTerms.isEmpty()) {
			state.europepmcMinedTermsGo = true;
			publication.setGoTerms(goTerms, PublicationPartType.europepmc, go, fetcherArgs);
		}
	}

	void fetchPubmedXml(Publication publication, FetcherPublicationState state, EnumMap<PublicationPartName, Boolean> parts) {
		if (state.pubmedXmlPmid) return;

		// keywords are usually missing (and if present, fetched from PMC)
		if (isFinal(publication, new PublicationPartName[] {
				PublicationPartName.pmid, PublicationPartName.pmcid, PublicationPartName.doi,
				PublicationPartName.title, PublicationPartName.mesh, PublicationPartName.theAbstract
			}, parts, false)) return;

		String pmid = publication.getPmid().getContent();
		if (pmid.isEmpty()) return;
		state.pubmedXmlPmid = true;

		PublicationPartType type = PublicationPartType.pubmed_xml;

		Document doc = getDoc(EUTILS + "efetch.fcgi?retmode=xml&db=pubmed&id=" + pmid, publication);
		if (doc != null) {
			if (doc.getElementsByTag("PubmedArticle").first() == null) {
				System.err.println("No article found in " + doc.location());
				return;
			}

			state.pubmedXml = true;

			setIds(publication, doc, type, "ArticleId[IdType=pubmed]", "ArticleId[IdType=pmc]", "ArticleId[IdType=doi]", false);

			// subtitle is already embedded in title
			setTitle(publication, doc, type, "ArticleTitle", null, parts);

			setKeywords(publication, doc, type, "Keyword", false, parts);

			if (parts == null || (parts.get(PublicationPartName.mesh) != null && parts.get(PublicationPartName.mesh))) {
				if (!publication.isMeshTermsFinal(fetcherArgs)) {
					List<MeshTerm> meshTerms = new ArrayList<>();
					for (Element descriptorName : doc.getElementsByTag("DescriptorName")) {
						MeshTerm meshTerm = new MeshTerm();

						String descriptorNameText = descriptorName.text();
						if (descriptorNameText.isEmpty()) {
							System.err.println("Tag DescriptorName has no content in " + doc.location());
						}
						meshTerm.setTerm(descriptorNameText);

						String majorTopic_YN = descriptorName.attr("MajorTopicYN").trim();
						if (majorTopic_YN.isEmpty()) {
							System.err.println("Attribute MajorTopicYN has no content in " + doc.location());
						}
						meshTerm.setMajorTopic(majorTopic_YN.equalsIgnoreCase("Y"));

						String descriptorNameUI = descriptorName.attr("UI").trim();
						if (descriptorNameUI.isEmpty()) {
							System.err.println("Attribute UI has no content in " + doc.location());
						}
						meshTerm.setUniqueId(descriptorNameUI);

						meshTerms.add(meshTerm);
					}
					publication.setMeshTerms(meshTerms, type, doc.location(), fetcherArgs);
				}
			}

			setAbstract(publication, doc, type, "AbstractText", parts);

			setJournalTitle(publication, doc, "Journal > Title");

			setPubDate(publication, doc, "ArticleDate", true);
		}
	}

	void fetchPubmedHtml(Publication publication, FetcherPublicationState state, EnumMap<PublicationPartName, Boolean> parts) {
		if (state.pubmedHtmlPmid) return;

		// keywords are usually missing (and if present, fetched from PMC)
		if (isFinal(publication, new PublicationPartName[] {
				PublicationPartName.title, PublicationPartName.theAbstract
			}, parts, false)
			&& (isFinal(publication, new PublicationPartName[] {
				PublicationPartName.pmid, PublicationPartName.pmcid, PublicationPartName.doi, PublicationPartName.mesh
			}, parts, false) || state.pubmedXml)) return;

		String pmid = publication.getPmid().getContent();
		if (pmid.isEmpty()) return;
		state.pubmedHtmlPmid = true;

		PublicationPartType type = PublicationPartType.pubmed_html;

		Document doc = getDoc(FetcherCommon.PMIDlink + pmid, publication);
		if (doc != null) {
			if (doc.getElementsByClass("rprt").first() == null) {
				System.err.println("No article found in " + doc.location());
				return;
			}

			setIds(publication, doc, type,
				".rprt .rprtid dt:containsOwn(PMID:) + dd",
				".rprt .rprtid dt:containsOwn(PMCID:) + dd",
				".rprt .rprtid dt:containsOwn(DOI:) + dd", false);

			// subtitle is already embedded in title
			setTitle(publication, doc, type, ".rprt > h1", null, parts);

			setKeywords(publication, doc, type, ".rprt .keywords p", true, parts);

			if (parts == null || (parts.get(PublicationPartName.mesh) != null && parts.get(PublicationPartName.mesh))) {
				if (!publication.isMeshTermsFinal(fetcherArgs)) {
					List<MeshTerm> meshTerms = new ArrayList<>();
					String previousDescriptorNameText = "";
					for (Element descriptorName : doc.select(".rprt a[alsec=mesh]")) {
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
							System.err.println("MeSH term has no content in " + doc.location());
						}
						meshTerm.setTerm(descriptorNameText);

						previousDescriptorNameText = descriptorNameText;
						meshTerms.add(meshTerm);
					}
					publication.setMeshTerms(meshTerms, type, doc.location(), fetcherArgs);
				}
			}

			setAbstract(publication, doc, type, ".rprt abstracttext", parts);
		}
	}

	void fetchPmcXml(Publication publication, FetcherPublicationState state, EnumMap<PublicationPartName, Boolean> parts) {
		if (state.pmcXmlPmcid) return;

		if (isFinal(publication, new PublicationPartName[] {
				PublicationPartName.pmid, PublicationPartName.pmcid, PublicationPartName.doi,
				PublicationPartName.title, PublicationPartName.keywords, PublicationPartName.theAbstract, PublicationPartName.fulltext
			}, parts, false)) return;

		String pmcid = publication.getPmcid().getContent();
		if (pmcid.isEmpty()) return;
		state.pmcXmlPmcid = true;

		Document doc = getDoc(EUTILS + "efetch.fcgi?retmode=xml&db=pmc&id=" + FetcherCommon.extractPmcid(pmcid), publication);
		if (doc != null) {
			state.pmcXml = fillWithPubMedCentralXml(publication, doc, PublicationPartType.pmc_xml, parts);
		}
	}

	void fetchPmcHtml(Publication publication, Links links, FetcherPublicationState state, EnumMap<PublicationPartName, Boolean> parts, boolean htmlMeta) {
		if (state.pmcHtmlPmcid) return;

		if (isFinal(publication, new PublicationPartName[] {
				PublicationPartName.title, PublicationPartName.theAbstract, PublicationPartName.fulltext
			}, parts, false)
			&& (isFinal(publication, new PublicationPartName[] {
				PublicationPartName.pmcid, PublicationPartName.doi, PublicationPartName.keywords
			}, parts, false) || state.pmcXml)) return;

		String pmcid = publication.getPmcid().getContent();
		if (pmcid.isEmpty()) return;
		state.pmcHtmlPmcid = true;

		PublicationPartType type = PublicationPartType.pmc_html;

		boolean pdfAdded = false;

		Document doc = getDoc(FetcherCommon.PMCIDlink + pmcid + "/", publication);
		if (doc != null) {
			fillWithPubMedCentralHtml(publication, doc, type, parts, htmlMeta);

			Element a = doc.select(".format-menu a:containsOwn(PDF)").first();
			if (a != null) {
				String pdfHref = a.attr("abs:href");
				if (!pdfHref.isEmpty()) {
					links.add(pdfHref, type.toPdf(), doc.location(), publication, fetcherArgs, true);
					pdfAdded = true;
				} else {
					System.err.println("Missing href for PDF link in " + doc.location());
				}
			} else {
				System.err.println("PDF link not found in " + doc.location());
			}
		}

		if (!pdfAdded && doc != null) {
			links.add(FetcherCommon.PMCIDlink + pmcid + "/pdf/", type.toPdf(), FetcherCommon.PMCIDlink + pmcid + "/", publication, fetcherArgs, true);
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
						System.err.println("New URL is malformed (" + newUrl + ") (old " + url + ", src " + src + ", dst " + dst + ")");
					}
				} else {
					System.err.println("New URL (" + newUrl + ") is empty or equal to old URL (" + url + ") (src " + src + ", dst " + dst + ")");
				}
			} else {
				System.err.println("Can't transform empty URL (src " + src + ", dst " + dst + ")");
			}
		}
		return href;
	}

	private List<String> getHrefsA(Document doc, String a) {
		List<String> hrefs = new ArrayList<>();
		if (a != null && !a.trim().isEmpty()) {
			Elements aTags = doc.select(a.trim());
			if (aTags.isEmpty()) {
				System.err.println("Can't find link with " + a + " in " + doc.location());
			}
			for (Element aTag : aTags) {
				String aHref = aTag.attr("abs:href");
				if (aHref != null && !aHref.isEmpty()) {
					try {
						new URL(aHref);
						hrefs.add(aHref);
					} catch (MalformedURLException e) {
						System.err.println("Attribute href malformed for link found with " + a + " in " + doc.location());
					}
				} else {
					System.err.println("Attribute href empty for link found with " + a + " in " + doc.location());
				}
			}
		}
		return hrefs;
	}

	void fetchSite(Publication publication, String url, PublicationPartType type, String from, Links links, EnumMap<PublicationPartName, Boolean> parts, boolean htmlMeta, boolean keywords) {
		if (keywords) {
			if (isFinal(publication, new PublicationPartName[] {
					PublicationPartName.pmid, PublicationPartName.pmcid, PublicationPartName.doi,
					PublicationPartName.title, PublicationPartName.keywords, PublicationPartName.theAbstract, PublicationPartName.fulltext
				}, parts, false)) return;
		} else {
			if (isFinal(publication, new PublicationPartName[] {
					PublicationPartName.pmid, PublicationPartName.pmcid, PublicationPartName.doi,
					PublicationPartName.title, PublicationPartName.theAbstract, PublicationPartName.fulltext
				}, parts, false)) return;
		}

		boolean javascript = scrape.getJavascript(FetcherCommon.getDoiRegistrant(url));
		if (!javascript) {
			javascript = Boolean.valueOf(scrape.getSelector(scrape.getSite(url), ScrapeSiteKey.javascript));
		}

		Document doc = getDoc(url, publication, type, from, links, parts, javascript);

		// Elsevier uses JavaScript for redirecting, assuming it goes to ScienceDirect
		// better use API https://www.elsevier.com/solutions/sciencedirect/support/api
		if (doc != null) {
			Matcher elsevier_id = ELSEVIER_REDIRECT.matcher(doc.location());
			if (elsevier_id.matches()) {
				// using JavaScript does not help get the fulltext
				doc = getDoc(SCIENCEDIRECT_LINK + elsevier_id.group(1), publication, type, from, links, parts, false);
			}
		}

		if (doc != null) {
			String finalUrl = doc.location();

			String site = scrape.getSite(finalUrl);
			if (site != null) {
				if (!javascript && Boolean.valueOf(scrape.getSelector(site, ScrapeSiteKey.javascript))) {
					doc = getDoc(finalUrl, publication, type, from, links, parts, true);
				}

				setIds(publication, doc, type, scrape.getSelector(site, ScrapeSiteKey.pmid), scrape.getSelector(site, ScrapeSiteKey.pmcid), scrape.getSelector(site, ScrapeSiteKey.doi), false);

				setTitle(publication, doc, type, scrape.getSelector(site, ScrapeSiteKey.title), scrape.getSelector(site, ScrapeSiteKey.subtitle), parts);

				setKeywords(publication, doc, type, scrape.getSelector(site, ScrapeSiteKey.keywords), false, parts);

				setKeywords(publication, doc, type, scrape.getSelector(site, ScrapeSiteKey.keywords_split), true, parts);

				setAbstract(publication, doc, type, scrape.getSelector(site, ScrapeSiteKey.theAbstract), parts);

				setFulltext(publication, doc, type, scrape.getSelector(site, ScrapeSiteKey.title), scrape.getSelector(site, ScrapeSiteKey.subtitle), scrape.getSelector(site, ScrapeSiteKey.theAbstract), scrape.getSelector(site, ScrapeSiteKey.fulltext), parts);

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
			} else {
				System.err.println("No scrape rules for " + finalUrl);
				if (parts == null || (parts.get(PublicationPartName.title) != null && parts.get(PublicationPartName.title))) {
					publication.setTitle(doc.title().split("\\|", 2)[0], PublicationPartType.webpage, finalUrl, fetcherArgs, false);
				}
				if (parts == null || (parts.get(PublicationPartName.fulltext) != null && parts.get(PublicationPartName.fulltext))) {
					publication.setFulltext(doc.text(), PublicationPartType.webpage, finalUrl, fetcherArgs);
				}
			}

			if (htmlMeta) {
				HtmlMeta.fillWith(publication, doc, type, links, fetcherArgs, parts, keywords);
			}

			try {
				publication.addVisitedSite(new Link(finalUrl, type, from));
			} catch (MalformedURLException e) {
				System.err.println("Can't add malformed visited site " + url + " found in " + from + " of type " + type);
			}
		}
	}

	private void fetchDoi(Publication publication, Links links, FetcherPublicationState state, EnumMap<PublicationPartName, Boolean> parts) {
		if (state.doi) return;

		if (isFinal(publication, new PublicationPartName[] {
				PublicationPartName.pmid, PublicationPartName.pmcid, PublicationPartName.doi,
				PublicationPartName.title, PublicationPartName.keywords, PublicationPartName.theAbstract, PublicationPartName.fulltext
			}, parts, false)) return;

		String doi = publication.getDoi().getContent();
		if (doi.isEmpty()) return;
		state.doi = true;

		String doiLink;
		try {
			doiLink = new URI("https", "doi.org", "/" + doi, null, null).toASCIIString();
		} catch (URISyntaxException e) {
			System.err.println(e);
			return;
		}

		fetchSite(publication, doiLink, PublicationPartType.doi, FetcherCommon.DOIlink + doi, links, parts, true, true);
	}

	void fetchOaDoi(Publication publication, Links links, FetcherPublicationState state, EnumMap<PublicationPartName, Boolean> parts) {
		if (state.oadoi) return;

		if (isFinal(publication, new PublicationPartName[] {
				PublicationPartName.title, PublicationPartName.theAbstract, PublicationPartName.fulltext
			}, parts, true)
			&& (isFinal(publication, new PublicationPartName[] {
				PublicationPartName.pmid, PublicationPartName.pmcid, PublicationPartName.doi,
			}, parts, true) || !idOnly(parts))) return;

		String doi = publication.getDoi().getContent();
		if (doi.isEmpty()) return;
		state.oadoi = true;

		URLConnection con;
		try {
			String oaDOI = new URI("https", "api.oadoi.org", "/v2/" + doi, "email=" + fetcherArgs.getOadoiEmail(), null).toASCIIString();
			System.out.println("    GET oaDOI " + oaDOI);
			con = FetcherCommon.newConnection(oaDOI, fetcherArgs);
		} catch (URISyntaxException | IOException e) {
			System.err.println(e);
			return;
		}

		try (InputStreamReader reader = new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8)) {
			String finalUrl = con.getURL().toString();
			System.out.println("    GOT oaDOI " + finalUrl);

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
						System.err.println("Journal title empty in oaDOI " + finalUrl);
					}
				} else {
					System.err.println("Journal title not found in oaDOI " + finalUrl);
				}
			}

			JsonNode oaLocations = root.get("oa_locations");
			if (oaLocations != null) {
				for (JsonNode oaLocation : oaLocations) {
					JsonNode urlPdf = oaLocation.get("url_for_pdf");
					if (urlPdf != null) {
						String urlPdfText = urlPdf.asText();
						if (urlPdfText != null && !urlPdfText.isEmpty() && !urlPdfText.equals("null")) {
							links.add(urlPdfText, PublicationPartType.pdf_oadoi, finalUrl, publication, fetcherArgs, false);
						}
					}
					JsonNode url = oaLocation.get("url");
					if (url != null) {
						String urlText = url.asText();
						if (urlText != null && !urlText.isEmpty() && !urlText.equals("null")) {
							links.add(urlText, PublicationPartType.link_oadoi, finalUrl, publication, fetcherArgs, false);
						}
					}
					JsonNode urlLandingPage = oaLocation.get("url_for_landing_page");
					if (urlLandingPage != null) {
						String urlLandingPageText = urlLandingPage.asText();
						if (urlLandingPageText != null && !urlLandingPageText.isEmpty() && !urlLandingPageText.equals("null")) {
							links.add(urlLandingPageText, PublicationPartType.link_oadoi, finalUrl, publication, fetcherArgs, false);
						}
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
			System.err.println(e);
			setFetchException(null, publication, null);
		} catch (IOException e) {
			System.err.println(e);
		} catch (Exception e) {
			// any checked exception
			e.printStackTrace();
			setFetchException(null, publication, null);
		}
	}

	private boolean fetchAll(Publication publication, Links links, FetcherPublicationState state, EnumMap<PublicationPartName, Boolean> parts) {
		System.out.println("Fetch sources " + publication.toStringId());

		String pmid = publication.getPmid().getContent();
		String pmcid = publication.getPmcid().getContent();
		String doi = publication.getDoi().getContent();

		// order and multiplicity is important
		fetchEuropepmc(publication, state, parts);
		fetchEuropepmc(publication, state, parts);
		fetchEuropepmc(publication, state, parts);
		fetchEuropepmcFulltextXml(publication, state, parts);
		fetchEuropepmcFulltextHtml(publication, links, state, parts, true);
		fetchEuropepmcMinedTermsEfo(publication, state, parts);
		fetchEuropepmcMinedTermsEfo(publication, state, parts);
		fetchEuropepmcMinedTermsGo(publication, state, parts);
		fetchEuropepmcMinedTermsGo(publication, state, parts);
		fetchPubmedXml(publication, state, parts);
		fetchPubmedHtml(publication, state, parts);
		fetchPmcXml(publication, state, parts);
		fetchPmcHtml(publication, links, state, parts, true);
		fetchDoi(publication, links, state, parts);
		fetchOaDoi(publication, links, state, parts);

		if (!pmid.isEmpty() && !publication.getPmid().isEmpty() && !pmid.equals(publication.getPmid().getContent())) {
			System.err.println("PMID changed from " + pmid + " to " + publication.getPmid().getContent());
			return false;
		}
		if (!pmcid.isEmpty() && !publication.getPmcid().isEmpty() && !pmcid.equals(publication.getPmcid().getContent())) {
			System.err.println("PMCID changed from " + pmcid + " to " + publication.getPmcid().getContent());
			return false;
		}
		if (!doi.isEmpty() && !publication.getDoi().isEmpty() && !doi.equals(publication.getDoi().getContent())) {
			System.err.println("DOI changed from " + doi + " to " + publication.getDoi().getContent());
			return false;
		}

		return true;
	}

	private void fetchPublication(Publication publication, EnumMap<PublicationPartName, Boolean> parts, boolean reset) {
		publication.setFetchException(false);

		if (reset) {
			System.out.println("Resetting publication " + publication.toStringId());
			publication.reset();
		}

		Links links = new Links();
		FetcherPublicationState state  = new FetcherPublicationState();

		boolean goon = true;

		int idCount = 0;
		while (publication.getIdCount() > idCount && goon) {
			idCount = publication.getIdCount();
			goon = fetchAll(publication, links, state, parts);
		}

		if (goon) {
			System.out.println("Fetch links " + publication.toStringId());
		}
		for (int linksFetched = 0; linksFetched < LINKS_LIMIT && goon; ++linksFetched) {
			if (isFinal(publication, new PublicationPartName[] {
					PublicationPartName.title, PublicationPartName.keywords, PublicationPartName.theAbstract, PublicationPartName.fulltext
				}, parts, false)
				&& (isFinal(publication, new PublicationPartName[] {
					PublicationPartName.pmid, PublicationPartName.pmcid, PublicationPartName.doi
				}, parts, false) || !idOnly(parts))) break;

			Link link = links.pop();
			if (link == null) break;

			if (!link.getType().isBetterThan(publication.getLowestType())) {
				break;
			}

			if (!link.getType().isPdf()) {
				if (link.getType() != PublicationPartType.link_oadoi) {
					fetchSite(publication, link.getUrl().toString(), link.getType(), link.getFrom(), links, parts, true, true);
				} else {
					fetchSite(publication, link.getUrl().toString(), link.getType(), link.getFrom(), links, parts, true, false);
				}
			} else if (SCIENCEDIRECT.matcher(link.getUrl().toString()).matches()) {
				getDoc(link.getUrl().toString(), publication, link.getType(), link.getFrom(), links, parts, true);
			} else {
				fetchPdf(link.getUrl().toString(), publication, link.getType(), link.getFrom(), links, parts);
			}

			while (publication.getIdCount() > idCount && goon) {
				idCount = publication.getIdCount();
				goon = fetchAll(publication, links, state, parts);
			}
		}

		if (!goon) {
			fetchPublication(publication, parts, true);
		}
	}

	public Publication initPublication(PublicationIds publicationIds) {
		if (publicationIds == null) {
			System.err.println("null IDs given for publication init");
			return null;
		}

		Publication publication = new Publication();

		String pmid = publicationIds.getPmid();
		String pmcid = publicationIds.getPmcid();
		String doi = publicationIds.getDoi();

		if (!pmid.isEmpty()) {
			if (FetcherCommon.isPmid(pmid)) {
				publication.setPmid(pmid, PublicationPartType.external, publicationIds.getPmidUrl(), fetcherArgs);
			} else {
				System.err.println("Unknown PMID: " + pmid);
			}
		}

		if (!pmcid.isEmpty()) {
			if (FetcherCommon.isPmcid(pmcid)) {
				publication.setPmcid(pmcid, PublicationPartType.external, publicationIds.getPmcidUrl(), fetcherArgs);
			} else {
				System.err.println("Unknown PMCID: " + pmcid);
			}
		}

		if (!doi.isEmpty()) {
			if (FetcherCommon.isDoi(doi)) {
				publication.setDoi(doi, PublicationPartType.external, publicationIds.getDoiUrl(), fetcherArgs);
			} else {
				System.err.println("Unknown DOI: " + doi);
			}
		}

		if (publication.getIdCount() < 1) {
			System.err.println("Can't init publication with no IDs");
			return null;
		}

		return publication;
	}

	public boolean getPublication(Publication publication) {
		return getPublication(publication, null);
	}

	public boolean getPublication(Publication publication, EnumMap<PublicationPartName, Boolean> parts) {
		if (publication == null) {
			System.err.println("null publication given for getting publication");
			return false;
		}
		if (publication.getIdCount() < 1) {
			System.err.println("Publication with no IDs given for getting publication");
			return false;
		}
		System.out.println("Get publication " + publication.toStringId());

		if (publication.canFetch(fetcherArgs)) {
			publication.updateCounters(fetcherArgs);

			fetchPublication(publication, parts, false);

			if (publication.isEmpty()) {
				System.err.println("Empty publication returned for " + publication.toStringId());
				// still return true, as publication metadata has been updated
			} else {
				System.out.println("Got publication " + publication.toStringId());
			}

			return true;
		} else {
			System.out.println("Not fetching publication " + publication.toStringId());
			return false;
		}
	}

	public Webpage initWebpage(String url) {
		if (url == null) {
			System.err.println("null URL given for webpage init");
			return null;
		}
		try {
			new URL(url);
		} catch (MalformedURLException e) {
			System.err.println("Malformed URL given for webpage init");
			return null;
		}
		Webpage webpage = new Webpage();
		webpage.setStartUrl(url);
		return webpage;
	}

	public boolean getWebpage(Webpage webpage) {
		return getWebpage(webpage, null, null, false);
	}

	public boolean getWebpage(Webpage webpage, String title, String content, boolean javascript) {
		if (webpage == null) {
			System.err.println("null webpage given for getting webpage");
			return false;
		}
		if (webpage.getStartUrl().isEmpty()) {
			System.err.println("Webpage with no start URL given for getting webpage");
			return false;
		}
		if (title != null && !title.isEmpty() || content != null && !content.isEmpty()) {
			System.out.println("Get " + (javascript ? "javascript " : "") + "webpage " + webpage.getStartUrl()
				+ " (with title selector " + title + " and content selector " + content + ")");
		} else {
			System.out.println("Get " + (javascript ? "javascript " : "") + "webpage " + webpage.getStartUrl());
		}

		if (webpage.canFetch(fetcherArgs)) {
			webpage.updateCounters(fetcherArgs);

			Webpage newWebpage = new Webpage();
			newWebpage.setStartUrl(webpage.getStartUrl());

			Boolean webpageJavascript = false;
			Map<String, String> startWebpage = scrape.getWebpage(newWebpage.getStartUrl());
			if (startWebpage != null) {
				webpageJavascript = Boolean.valueOf(startWebpage.get(ScrapeWebpageKey.javascript.toString()));
			} else {
				System.err.println("No scrape rules for start webpage " + newWebpage.getStartUrl());
			}

			Document doc = getDoc(newWebpage, javascript || webpageJavascript);

			if (doc != null && !(javascript || webpageJavascript) && scrape.getWebpage(newWebpage.getFinalUrl()) == null) {
				int textLength = doc.text().length();
				if (textLength < fetcherArgs.getWebpageMinLengthJavascript() || !doc.select("noscript").isEmpty()) {
					System.err.println("Refetching " + newWebpage.getStartUrl() + " with JavaScript enabled");
					Webpage newWebpageJavascript = new Webpage();
					newWebpageJavascript.setStartUrl(newWebpage.getStartUrl());
					Document docJavascript = getDoc(newWebpageJavascript, true);
					if (docJavascript != null) {
						doc = docJavascript;
						newWebpage = newWebpageJavascript;
						int textLengthAfter = doc.text().length();
						if (textLength != textLengthAfter) {
							System.err.println("Content length changed from " + textLength + " to " + textLengthAfter);
						} else {
							System.err.println("Content length did not change with JavaScript");
						}
					} else {
						System.err.println("Discarding failed JavaScript webpage");
					}
				}
			}

			if (doc != null) {
				Map<String, String> finalWebpage = scrape.getWebpage(newWebpage.getFinalUrl());
				if (finalWebpage == null) {
					System.err.println("No scrape rules for final webpage " + newWebpage.getFinalUrl());
				}
				if (finalWebpage != null && (title == null || title.isEmpty()) && (content == null || content.isEmpty())) {
					if (finalWebpage.get(ScrapeWebpageKey.title.toString()) != null) {
						title = finalWebpage.get(ScrapeWebpageKey.title.toString());
					}
					if (finalWebpage.get(ScrapeWebpageKey.content.toString()) != null) {
						content = finalWebpage.get(ScrapeWebpageKey.content.toString());
					}
				}
				if (title != null && !title.isEmpty()) {
					newWebpage.setTitle(getFirstTrimmed(doc, title, true));
				}
				if (content != null && !content.isEmpty()) {
					newWebpage.setContent(text(doc, content, true));
				} else if (finalWebpage == null) {
					newWebpage.setContent(doc.text());
				}
			}

			if (newWebpage.isEmpty()) {
				System.err.println("Empty webpage returned for " + newWebpage.getStartUrl());
			}

			if (newWebpage.isFinal(fetcherArgs)
				|| !newWebpage.isFinal(fetcherArgs) && !newWebpage.isEmpty() && !webpage.isFinal(fetcherArgs)
				|| newWebpage.isEmpty() && webpage.isEmpty()) {
				webpage.overwrite(newWebpage);
				System.out.println("Got webpage for " + newWebpage.getStartUrl());
			} else {
				System.out.println("Not overwriting previous webpage for " + newWebpage.getStartUrl());
			}

			return true;
		} else {
			System.out.println("Not fetching webpage " + webpage.getStartUrl());
			return false;
		}
	}
}
