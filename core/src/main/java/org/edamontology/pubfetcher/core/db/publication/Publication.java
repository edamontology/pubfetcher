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

package org.edamontology.pubfetcher.core.db.publication;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import com.fasterxml.jackson.core.JsonGenerator;

import org.edamontology.pubfetcher.core.common.FetcherArgs;
import org.edamontology.pubfetcher.core.common.PubFetcher;
import org.edamontology.pubfetcher.core.db.DatabaseEntry;
import org.edamontology.pubfetcher.core.db.link.Link;

public class Publication extends DatabaseEntry<Publication> {

	private static final long serialVersionUID = -97834887073030847L;

	private static final Logger logger = LogManager.getLogger();

	private PublicationPartString pmid;
	private PublicationPartString pmcid;
	private PublicationPartString doi;

	private PublicationPartString title;

	private PublicationPartList<String> keywords;

	private PublicationPartList<MeshTerm> meshTerms;
	private PublicationPartList<MinedTerm> efoTerms;
	private PublicationPartList<MinedTerm> goTerms;

	private PublicationPartString theAbstract;

	private PublicationPartString fulltext;

	private boolean oa = false;

	private String journalTitle = "";

	private long pubDate = -1;

	private int citationsCount = -1;
	private long citationsTimestamp = -1;

	private List<CorrespAuthor> correspAuthor = new ArrayList<>();

	private Set<Link> visitedSites = new LinkedHashSet<>();

	private static final Pattern VERSIONED = Pattern.compile("^[._-][0-9]+$");
	private static final Pattern F1000_DOI = Pattern.compile("^10.12688/F1000RESEARCH\\..+$");
	private static final Pattern ZENODO = Pattern.compile("^https?://(www\\.)?zenodo\\.org/.+$");

	public Publication() {
		pmid = new PublicationPartString(PublicationPartName.pmid);
		pmcid = new PublicationPartString(PublicationPartName.pmcid);
		doi = new PublicationPartString(PublicationPartName.doi);

		reset();
	}

	public void reset() {
		title = new PublicationPartString(PublicationPartName.title);

		keywords = new PublicationPartList<>(PublicationPartName.keywords);

		meshTerms = new PublicationPartList<>(PublicationPartName.mesh);
		efoTerms = new PublicationPartList<>(PublicationPartName.efo);
		goTerms = new PublicationPartList<>(PublicationPartName.go);

		theAbstract = new PublicationPartString(PublicationPartName.theAbstract);

		fulltext = new PublicationPartString(PublicationPartName.fulltext);
	}

	@Override
	public boolean isEmpty() {
		return title.isEmpty() && keywords.isEmpty() &&
			meshTerms.isEmpty() && efoTerms.isEmpty() && goTerms.isEmpty() &&
			theAbstract.isEmpty() && fulltext.isEmpty();
	}

	@Override
	public boolean isUsable(FetcherArgs fetcherArgs) {
		return title.isUsable(fetcherArgs) || keywords.isUsable(fetcherArgs) ||
			meshTerms.isUsable(fetcherArgs) || efoTerms.isUsable(fetcherArgs) || goTerms.isUsable(fetcherArgs) ||
			theAbstract.isUsable(fetcherArgs) || fulltext.isUsable(fetcherArgs);
	}

	@Override
	public boolean isFinal(FetcherArgs fetcherArgs) {
		return title.isFinal(fetcherArgs) && theAbstract.isFinal(fetcherArgs) && fulltext.isFinal(fetcherArgs);
	}

	public boolean isTotallyFinal(FetcherArgs fetcherArgs) {
		return pmid.isFinal(fetcherArgs) && pmcid.isFinal(fetcherArgs) && doi.isFinal(fetcherArgs) &&
			title.isFinal(fetcherArgs) && keywords.isFinal(fetcherArgs) &&
			meshTerms.isFinal(fetcherArgs) && efoTerms.isFinal(fetcherArgs) && goTerms.isFinal(fetcherArgs) &&
			theAbstract.isFinal(fetcherArgs) && fulltext.isFinal(fetcherArgs);
	}

	@Override
	public String getStatusString(FetcherArgs fetcherArgs) {
		if (isEmpty()) return "empty";
		if (!isUsable(fetcherArgs)) return "non-usable";
		if (!isFinal(fetcherArgs)) return "non-final";
		if (!isTotallyFinal(fetcherArgs)) return "final";
		return "totally final";
	}

	public int getIdCount() {
		int idCount = 0;
		if (!pmid.isEmpty()) ++idCount;
		if (!pmcid.isEmpty()) ++idCount;
		if (!doi.isEmpty()) ++idCount;
		return idCount;
	}

	public PublicationPartType getLowestType() {
		PublicationPartType lowest = title.getType();
		if (lowest.isBetterThan(keywords.getType())) lowest = keywords.getType();
		if (lowest.isBetterThan(theAbstract.getType())) lowest = theAbstract.getType();
		if (lowest.isBetterThan(fulltext.getType())) lowest = fulltext.getType();
		return lowest;
	}

	private void logSet(String s) {
		logger.info("        set {} for {}", s, toStringId());
	}

	private void logFinal(String s, boolean canMakeFinal, FetcherArgs fetcherArgs) {
		logger.info("        {} final for {}", s, toStringId());
		if (canMakeFinal && isFinal(fetcherArgs)) {
			logger.info("        {} is final", toStringId());
		}
		if (isTotallyFinal(fetcherArgs)) {
			logger.info("        {} is totally final", toStringId());
		}
	}

	private boolean setId(String content, PublicationPartType type, String url, FetcherArgs fetcherArgs, Function<String, Boolean> isId, PublicationPartString part) {
		if (content != null && !content.trim().isEmpty()) {
			content = content.trim();
			if (!isId.apply(content)) {
				logger.error("Unknown ID: {}", content);
			} else {
				if (!part.isEmpty() && !part.getContent().equals(content)) {
					logger.warn("Old ID {} is different from new ID {} from {} of type {}", part.getContent(), content, url, type);
					if (part.getContent().startsWith(content) && VERSIONED.matcher(part.getContent().substring(content.length())).matches()
							|| content.startsWith(part.getContent()) && VERSIONED.matcher(content.substring(part.getContent().length())).matches()
							|| F1000_DOI.matcher(part.getContent()).matches() && F1000_DOI.matcher(content).matches()
							|| ZENODO.matcher(url).matches()) {
						logger.warn("Setting ID to {} (instead of {})", part.getContent(), content);
						content = part.getContent();
					}
				}
				if (!part.isFinal(fetcherArgs) && type.isBetterThan(part.getType())) {
					part.set(content, type, url);
					logger.info("        set {} {}", part.getName(), part.getContent());
					if (pmid.isFinal(fetcherArgs) && pmcid.isFinal(fetcherArgs) && doi.isFinal(fetcherArgs)) {
						logFinal("ID", false, fetcherArgs);
					}
					return true;
				}
			}
		}
		return false;
	}
	private void set(String content, PublicationPartType type, String url, FetcherArgs fetcherArgs, PublicationPartString part, boolean canMakeFinal, boolean clean) {
		content = content.trim();
		if (clean) content = Jsoup.clean(content, new Whitelist());
		if (!part.isFinal(fetcherArgs) && !content.isEmpty()
			&& (type.isFinal() && part.getContent().length() < content.length()
				|| type.isBetterThan(part.getType()))) {
			part.set(content, type, url);
			logSet(part.getName().toString());
			if (part.isFinal(fetcherArgs)) {
				logFinal(part.getName().toString(), canMakeFinal, fetcherArgs);
			}
		}
	}
	private <T> void setList(List<T> list, PublicationPartType type, String url, FetcherArgs fetcherArgs, PublicationPartList<T> part) {
		if (!part.isFinal(fetcherArgs) && !list.isEmpty()
			&& (type.isFinal() && part.getList().size() < list.size()
				|| type.isBetterThan(part.getType()))) {
			part.set(list, type, url);
			logSet(part.getName().toString());
			if (part.isFinal(fetcherArgs)) {
				logFinal(part.getName().toString(), false, fetcherArgs);
			}
		}
	}

	public PublicationPartString getPmid() {
		return pmid;
	}
	public boolean setPmid(String pmid, PublicationPartType type, String url, FetcherArgs fetcherArgs) {
		return setId(pmid, type, url, fetcherArgs, PubFetcher::isPmid, this.pmid);
	}

	public PublicationPartString getPmcid() {
		return pmcid;
	}
	public boolean setPmcid(String pmcid, PublicationPartType type, String url, FetcherArgs fetcherArgs) {
		return setId(pmcid, type, url, fetcherArgs, PubFetcher::isPmcid, this.pmcid);
	}

	public PublicationPartString getDoi() {
		return doi;
	}
	public boolean setDoi(String doi, PublicationPartType type, String url, FetcherArgs fetcherArgs) {
		doi = PubFetcher.normaliseDoi(doi);
		return setId(doi, type, url, fetcherArgs, PubFetcher::isDoi, this.doi);
	}

	public PublicationPartString getTitle() {
		return title;
	}
	public void setTitle(String title, PublicationPartType type, String url, FetcherArgs fetcherArgs, boolean clean) {
		set(title, type, url, fetcherArgs, this.title, true, clean);
	}

	public PublicationPartList<String> getKeywords() {
		return keywords;
	}
	public void setKeywords(List<String> keywords, PublicationPartType type, String url, FetcherArgs fetcherArgs, boolean clean) {
		List<String> keywordsFull = keywords.stream()
			.filter(k -> k != null)
			.map(k -> k.trim())
			.map(k -> { if (clean) return Jsoup.clean(k, new Whitelist()); else return k; })
			.filter(k -> !k.isEmpty())
			.distinct()
			.collect(Collectors.toList());
		setList(keywordsFull, type, url, fetcherArgs, this.keywords);
	}

	public PublicationPartList<MeshTerm> getMeshTerms() {
		return meshTerms;
	}
	public void setMeshTerms(List<MeshTerm> meshTerms, PublicationPartType type, String url, FetcherArgs fetcherArgs) {
		List<MeshTerm> meshTermsFull = meshTerms.stream()
			.filter(k -> k != null && !k.getTerm().isEmpty())
			.collect(Collectors.toList());
		setList(meshTermsFull, type, url, fetcherArgs, this.meshTerms);
	}

	public PublicationPartList<MinedTerm> getEfoTerms() {
		return efoTerms;
	}
	public void setEfoTerms(List<MinedTerm> efoTerms, PublicationPartType type, String url, FetcherArgs fetcherArgs) {
		List<MinedTerm> efoTermsFull = efoTerms.stream()
			.filter(k -> k != null && !k.getTerm().isEmpty())
			.collect(Collectors.toList());
		setList(efoTermsFull, type, url, fetcherArgs, this.efoTerms);
	}

	public PublicationPartList<MinedTerm> getGoTerms() {
		return goTerms;
	}
	public void setGoTerms(List<MinedTerm> goTerms, PublicationPartType type, String url, FetcherArgs fetcherArgs) {
		List<MinedTerm> goTermsFull = goTerms.stream()
			.filter(k -> k != null && !k.getTerm().isEmpty())
			.collect(Collectors.toList());
		setList(goTermsFull, type, url, fetcherArgs, this.goTerms);
	}

	public PublicationPartString getAbstract() {
		return theAbstract;
	}
	public void setAbstract(String theAbstract, PublicationPartType type, String url, FetcherArgs fetcherArgs, boolean clean) {
		set(theAbstract, type, url, fetcherArgs, this.theAbstract, true, clean);
	}

	public PublicationPartString getFulltext() {
		return fulltext;
	}
	public void setFulltext(String fulltext, PublicationPartType type, String url, FetcherArgs fetcherArgs) {
		set(fulltext, type, url, fetcherArgs, this.fulltext, true, false);
	}

	public boolean isOA() {
		return oa;
	}
	public void setOA(boolean oa) {
		this.oa = oa;
	}

	public String getJournalTitle() {
		return journalTitle;
	}
	public void setJournalTitle(String journalTitle) {
		if (this.journalTitle.isEmpty()) {
			this.journalTitle = journalTitle.trim();
		}
	}

	public long getPubDate() {
		return pubDate;
	}
	public String getPubDateHuman() {
		return LocalDateTime.ofInstant(Instant.ofEpochMilli(pubDate), ZoneOffset.UTC).toLocalDate().toString();
	}
	public void setPubDate(String pubDate) {
		if (this.pubDate < 0) {
			try {
				this.pubDate = LocalDate.parse(pubDate).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
			} catch (DateTimeParseException e) {
				logger.warn("Could not set publication date from {} (error index {}): {}", e.getParsedString(), e.getErrorIndex(), e.getMessage());
			} catch (ArithmeticException e) {
				logger.error("Overflow in setting publication date");
			}
		}
	}

	public int getCitationsCount() {
		return citationsCount;
	}
	public boolean setCitationsCount(String citationsCount) {
		try {
			int citationsCountParsed = Integer.parseInt(citationsCount);
			if (citationsCountParsed >= 0) {
				this.citationsCount = citationsCountParsed;
				this.citationsTimestamp = System.currentTimeMillis();
				return true;
			} else {
				logger.error("Citations count is negative: {}", citationsCountParsed);
				return false;
			}
		} catch (NumberFormatException e) {
			logger.error("Illegal citations count: {}", citationsCount);
			return false;
		}
	}

	public long getCitationsTimestamp() {
		return citationsTimestamp;
	}
	public String getCitationsTimestampHuman() {
		return Instant.ofEpochMilli(citationsTimestamp).toString();
	}

	public List<CorrespAuthor> getCorrespAuthor() {
		return correspAuthor;
	}
	public void setCorrespAuthor(List<CorrespAuthor> correspAuthor) {
		if (this.correspAuthor.isEmpty()) {
			this.correspAuthor = correspAuthor;
		}
	}

	public Set<Link> getVisitedSites() {
		return visitedSites;
	}
	public void addVisitedSite(Link visitedSite) {
		visitedSites.add(visitedSite);
	}

	public PublicationPart getPart(PublicationPartName name) {
		PublicationPart part = null;
		switch (name) {
			case pmid: part = pmid; break;
			case pmcid: part = pmcid; break;
			case doi: part = doi; break;
			case title: part = title; break;
			case keywords: part = keywords; break;
			case mesh: part = meshTerms; break;
			case efo: part = efoTerms; break;
			case go: part = goTerms; break;
			case theAbstract: part = theAbstract; break;
			case fulltext: part = fulltext; break;
		}
		return part;
	}

	public List<PublicationPart> getParts(List<PublicationPartName> names) {
		List<PublicationPart> parts = new ArrayList<>();
		for (PublicationPartName name : names) {
			parts.add(getPart(name));
		}
		return parts;
	}

	@Override
	public String toStringId() {
		return "[" + PublicationIds.toString(pmid.getContent(), pmcid.getContent(), doi.getContent(), false) + "]";
	}

	@Override
	public String toStringIdHtml() {
		return PublicationIds.toStringHtml(pmid.getContent(), pmcid.getContent(), doi.getContent(), false);
	}

	@Override
	public void toStringIdJson(JsonGenerator generator) throws IOException {
		PublicationIds.toStringJson(pmid.getContent(), pmcid.getContent(), doi.getContent(), generator);
	}

	@Override
	public String toStringPlain() {
		StringBuilder sb = new StringBuilder();
		sb.append(toStringId()).append("\n\n");
		sb.append(title.toStringPlain()).append("\n\n");
		sb.append(keywords.toStringPlain()).append("\n");
		sb.append(meshTerms.toStringPlain()).append("\n");
		sb.append(efoTerms.toStringPlain()).append("\n");
		sb.append(goTerms.toStringPlain()).append("\n\n");
		sb.append(theAbstract.toStringPlain()).append("\n\n");
		sb.append(fulltext.toStringPlain());
		return sb.toString();
	}

	@Override
	public String toStringPlainHtml(String prepend) {
		StringBuilder sb = new StringBuilder();
		sb.append(prepend).append("<h2>").append(toStringIdHtml()).append("</h2>\n");
		sb.append(title.toStringPlainHtml()).append("\n");
		sb.append(keywords.toStringPlainHtml()).append("\n");
		sb.append(meshTerms.toStringPlainHtml()).append("\n");
		sb.append(efoTerms.toStringPlainHtml()).append("\n");
		sb.append(goTerms.toStringPlainHtml()).append("\n");
		sb.append(theAbstract.toStringPlainHtml()).append("\n");
		sb.append(fulltext.toStringPlainHtml());
		return sb.toString();
	}

	@Override
	public void toStringPlainJson(JsonGenerator generator) throws IOException {
		generator.writeStartObject();
		pmid.toStringPlainJson(generator, true);
		pmcid.toStringPlainJson(generator, true);
		doi.toStringPlainJson(generator, true);
		title.toStringPlainJson(generator, true);
		keywords.toStringPlainJson(generator, true);
		meshTerms.toStringPlainJson(generator, true);
		efoTerms.toStringPlainJson(generator, true);
		goTerms.toStringPlainJson(generator, true);
		theAbstract.toStringPlainJson(generator, "abstract");
		fulltext.toStringPlainJson(generator, true);
		generator.writeEndObject();
	}

	@Override
	public String toStringMetaHtml(String prepend) {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toStringHtml(prepend)).append("\n");
		sb.append(prepend).append("<br>\n");
		sb.append(prepend).append("<div><span>Open Access:</span> <span>").append(oa).append("</span></div>\n");
		sb.append(prepend).append("<div><span>Journal title:</span> <span>").append(PubFetcher.escapeHtml(journalTitle)).append("</span></div>\n");
		sb.append(prepend).append("<div><span>Pub. date:</span> <span>").append(getPubDateHuman()).append(" (").append(pubDate).append(")</span></div>\n");
		sb.append(prepend).append("<div><span>Citations count:</span> <span>").append(citationsCount).append(" (").append(getCitationsTimestampHuman()).append(" (").append(citationsTimestamp).append("))</span></div>\n");
		sb.append(prepend).append("<div><span>Corresp. author:</span> <span>").append(PubFetcher.escapeHtml(CorrespAuthor.toString(correspAuthor))).append("</span></div>\n");
		sb.append(prepend).append("<br>\n");
		sb.append(prepend).append("<div><span>Visited sites:</span></div>\n");
		sb.append(prepend).append("<ul>\n");
		for (Link link : visitedSites) {
			sb.append(prepend).append("<li>\n");
			sb.append(link.toStringHtml(prepend)).append("\n");
			sb.append(prepend).append("</li>\n");
		}
		sb.append(prepend).append("</ul>");
		return sb.toString();
	}

	@Override
	public void toStringMetaJson(JsonGenerator generator, FetcherArgs fetcherArgs) throws IOException {
		super.toStringJson(generator);
		generator.writeBooleanField("oa", oa);
		generator.writeStringField("journalTitle", journalTitle);
		generator.writeNumberField("pubDate", pubDate);
		generator.writeStringField("pubDateHuman", getPubDateHuman());
		generator.writeNumberField("citationsCount", citationsCount);
		generator.writeNumberField("citationsTimestamp", citationsTimestamp);
		generator.writeStringField("citationsTimestampHuman", getCitationsTimestampHuman());
		generator.writeFieldName("correspAuthor");
		generator.writeStartArray();
		for (CorrespAuthor ca : correspAuthor) {
			ca.toStringJson(generator);
		}
		generator.writeEndArray();
		generator.writeObjectField("visitedSites", visitedSites);
		generator.writeBooleanField("empty", isEmpty());
		generator.writeBooleanField("usable", isUsable(fetcherArgs));
		generator.writeBooleanField("final", isFinal(fetcherArgs));
		generator.writeBooleanField("totallyFinal", isTotallyFinal(fetcherArgs));
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(pmid).append("\n");
		sb.append(pmcid).append("\n");
		sb.append(doi).append("\n\n");
		sb.append(title).append("\n\n");
		sb.append(keywords).append("\n");
		sb.append(meshTerms).append("\n");
		sb.append(efoTerms).append("\n");
		sb.append(goTerms).append("\n\n");
		sb.append(theAbstract).append("\n\n");
		sb.append(fulltext).append("\n\n");
		sb.append(super.toString()).append("\n\n");
		sb.append("OPEN ACCESS: ").append(oa).append("\n");
		sb.append("JOURNAL TITLE: ").append(journalTitle).append("\n");
		sb.append("PUB. DATE: ").append(getPubDateHuman()).append(" (").append(pubDate).append(")\n");
		sb.append("CITATIONS: ").append(citationsCount).append(" (").append(getCitationsTimestampHuman()).append(" (").append(citationsTimestamp).append("))\n");
		sb.append("CORRESP. AUTHOR: ").append(CorrespAuthor.toString(correspAuthor)).append("\n\n");
		sb.append("VISITED SITES:");
		for (Link link : visitedSites) {
			sb.append("\n").append(link);
		}
		return sb.toString();
	}

	@Override
	public String toStringHtml(String prepend) {
		StringBuilder sb = new StringBuilder();
		sb.append(prepend).append("<h2>Publication</h2>\n");
		sb.append(pmid.toStringHtml(prepend)).append("\n");
		sb.append(pmcid.toStringHtml(prepend)).append("\n");
		sb.append(doi.toStringHtml(prepend)).append("\n");
		sb.append(title.toStringHtml(prepend)).append("\n");
		sb.append(keywords.toStringHtml(prepend)).append("\n");
		sb.append(meshTerms.toStringHtml(prepend)).append("\n");
		sb.append(efoTerms.toStringHtml(prepend)).append("\n");
		sb.append(goTerms.toStringHtml(prepend)).append("\n");
		sb.append(theAbstract.toStringHtml(prepend)).append("\n");
		sb.append(fulltext.toStringHtml(prepend)).append("\n");
		sb.append(prepend).append("<br>\n");
		sb.append(toStringMetaHtml(prepend));
		return sb.toString();
	}

	public void toStringJson(JsonGenerator generator, FetcherArgs fetcherArgs, boolean fulltextContent) throws IOException {
		generator.writeStartObject();
		toStringMetaJson(generator, fetcherArgs);
		pmid.toStringJson(generator, fetcherArgs);
		pmcid.toStringJson(generator, fetcherArgs);
		doi.toStringJson(generator, fetcherArgs);
		title.toStringJson(generator, fetcherArgs);
		keywords.toStringJson(generator, fetcherArgs);
		meshTerms.toStringJson(generator, fetcherArgs);
		efoTerms.toStringJson(generator, fetcherArgs);
		goTerms.toStringJson(generator, fetcherArgs);
		theAbstract.toStringJson(generator, fetcherArgs, true, "abstract");
		fulltext.toStringJson(generator, fetcherArgs, fulltextContent, "fulltext");
		generator.writeEndObject();
	}

	@Override
	public int compareTo(Publication o) {
		if (o == null) return 1;
		return PublicationIds.compareTo(pmid.getContent(), pmcid.getContent(), doi.getContent(),
			o.pmid.getContent(), o.pmcid.getContent(), o.doi.getContent());
	}
}
