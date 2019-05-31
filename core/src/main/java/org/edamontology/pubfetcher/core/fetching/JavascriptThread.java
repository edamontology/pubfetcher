/*
 * Copyright © 2019 Erik Jaaniso
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

import java.net.URL;

import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;

import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.edamontology.pubfetcher.core.common.FetcherArgs;
import org.edamontology.pubfetcher.core.db.webpage.Webpage;

public class JavascriptThread implements Runnable {

	private static final Logger logger = LogManager.getLogger();

	private final String url;

	private final Webpage webpage;

	private final FetcherArgs fetcherArgs;

	private Document doc = null;

	private Exception e = null;

	private volatile boolean finished = false;

	public JavascriptThread(String url, Webpage webpage, FetcherArgs fetcherArgs) {
		this.url = url;
		this.webpage = webpage;
		this.fetcherArgs = fetcherArgs;
	}

	public Document getDoc() {
		return doc;
	}

	public Exception getException() {
		return e;
	}

	public boolean isFinished() {
		return finished;
	}

	@Override
	public void run() {
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

			URL u = new URL(url);
			webClient.addRequestHeader("User-Agent", fetcherArgs.getPrivateArgs().getUserAgent());
			webClient.addRequestHeader("Referer", u.getProtocol() + "://" + u.getAuthority());

			webClient.setAjaxController(new NicelyResynchronizingAjaxController());
			webClient.setCssErrorHandler(new SilentCssErrorHandler());

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
				webClient.close(); // explicit closing after waiting may help in avoiding hung threads (background JavaScript tasks)?

				doc = Jsoup.parse(htmlPage.asXml(), finalUrl);
			} else {
				webClient.close();
				throw new UnsupportedMimeTypeException("Not a HTML page", contentType, finalUrl);
			}
		} catch (Exception e) {
			this.e = e;
			logger.warn(e);
		} catch (ThreadDeath e) {
			logger.error("ThreadDeath!");
			throw e;
		} catch (Error e) { // e.g. java.lang.StackOverflowError
			logger.error(e);
		} finally {
			finished = true;

			if (doc != null) {
				logger.info("    GOT {} (with JavaScript)", doc.location());
			} else {
				logger.error("Failed to get Document!");
			}
		}
	}
}
