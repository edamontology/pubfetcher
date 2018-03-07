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

package org.edamontology.pubfetcher;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

public class Publication extends DatabaseEntry<Publication> {

	private static final long serialVersionUID = -97834887073030847L;

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

	private String correspAuthor = "";

	private Set<Link> visitedSites = new LinkedHashSet<>();

	private static final Pattern VERSIONED = Pattern.compile("^[._-][0-9]+$");

	Publication() {
		pmid = new PublicationPartString(PublicationPartName.pmid);
		pmcid = new PublicationPartString(PublicationPartName.pmcid);
		doi = new PublicationPartString(PublicationPartName.doi);

		reset();
	}

	void reset() {
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
	public boolean isFinal(FetcherArgs fetcherArgs) {
		return isTitleFinal(fetcherArgs) && isAbstractFinal(fetcherArgs) && isFulltextFinal(fetcherArgs);
	}

	public boolean isTotallyFinal(FetcherArgs fetcherArgs) {
		return isIdFinal() && isTitleFinal(fetcherArgs) && isKeywordsFinal(fetcherArgs) &&
			isMeshTermsFinal(fetcherArgs) && isEfoTermsFinal(fetcherArgs) && isGoTermsFinal(fetcherArgs) &&
			isAbstractFinal(fetcherArgs) && isFulltextFinal(fetcherArgs);
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
		System.out.println("        set " + s + " for " + toStringId());
	}

	private void logFinal(String s, boolean canMakeFinal, FetcherArgs fetcherArgs) {
		System.out.println("        " + s + " final for " + toStringId());
		if (canMakeFinal && isFinal(fetcherArgs)) {
			System.out.println("        " + toStringId() + " is final");
		}
		if (isTotallyFinal(fetcherArgs)) {
			System.out.println("        " + toStringId() + " is totally final");
		}
	}

	private boolean setId(String content, PublicationPartType type, String url, FetcherArgs fetcherArgs, BooleanSupplier isFinal, Function<String, Boolean> isId, PublicationPartString part) {
		if (content != null && !content.trim().isEmpty()) {
			content = content.trim();
			if (!isId.apply(content)) {
				System.err.println("Unknown ID: " + content);
			} else {
				if (!part.isEmpty() && !part.getContent().equals(content)) {
					System.err.println("Old ID " + part.getContent() + " is different from new ID " + content + " from " + url + " of type " + type);
					if (part.getContent().startsWith(content) && VERSIONED.matcher(part.getContent().substring(content.length())).matches()
							|| content.startsWith(part.getContent()) && VERSIONED.matcher(content.substring(part.getContent().length())).matches()) {
						System.err.println("Setting ID to " + part.getContent() + " (instead of " + content + ")");
						content = part.getContent();
					}
				}
				if (!isFinal.getAsBoolean() && type.isBetterThan(part.getType())) {
					part.set(content, type, url);
					System.out.println("        set " + part.getName() + " " + part.getContent());
					if (isIdFinal()) {
						logFinal("ID", false, fetcherArgs);
					}
					return true;
				}
			}
		}
		return false;
	}
	private void set(String content, PublicationPartType type, String url, FetcherArgs fetcherArgs, Function<FetcherArgs, Boolean> isFinal, PublicationPartString part, boolean canMakeFinal, boolean clean) {
		content = content.trim();
		if (clean) content = Jsoup.clean(content, new Whitelist());
		if (!isFinal.apply(fetcherArgs) && !content.isEmpty()
			&& (type.isFinal() && part.getContent().length() < content.length()
				|| type.isBetterThan(part.getType()))) {
			part.set(content, type, url);
			logSet(part.getName().toString());
			if (isFinal.apply(fetcherArgs)) {
				logFinal(part.getName().toString(), canMakeFinal, fetcherArgs);
			}
		}
	}
	private <T> void setList(List<T> list, PublicationPartType type, String url, FetcherArgs fetcherArgs, Function<FetcherArgs, Boolean> isFinal, PublicationPartList<T> part) {
		if (!isFinal.apply(fetcherArgs) && !list.isEmpty()
			&& (type.isFinal() && part.getList().size() < list.size()
				|| type.isBetterThan(part.getType()))) {
			part.set(list, type, url);
			logSet(part.getName().toString());
			if (isFinal.apply(fetcherArgs)) {
				logFinal(part.getName().toString(), false, fetcherArgs);
			}
		}
	}

	public PublicationPartString getPmid() {
		return pmid;
	}
	boolean setPmid(String pmid, PublicationPartType type, String url, FetcherArgs fetcherArgs) {
		return setId(pmid, type, url, fetcherArgs, this::isPmidFinal, FetcherCommon::isPmid, this.pmid);
	}
	public boolean isPmidFinal() {
		return pmid.getType().isFinal() && !pmid.isEmpty();
	}

	public PublicationPartString getPmcid() {
		return pmcid;
	}
	boolean setPmcid(String pmcid, PublicationPartType type, String url, FetcherArgs fetcherArgs) {
		return setId(pmcid, type, url, fetcherArgs, this::isPmcidFinal, FetcherCommon::isPmcid, this.pmcid);
	}
	public boolean isPmcidFinal() {
		return pmcid.getType().isFinal() && !pmcid.isEmpty();
	}

	public PublicationPartString getDoi() {
		return doi;
	}
	boolean setDoi(String doi, PublicationPartType type, String url, FetcherArgs fetcherArgs) {
		doi = FetcherCommon.normalizeDoi(doi);
		return setId(doi, type, url, fetcherArgs, this::isDoiFinal, FetcherCommon::isDoi, this.doi);
	}
	public boolean isDoiFinal() {
		return doi.getType().isFinal() && !doi.isEmpty();
	}

	public boolean isIdFinal() {
		return isPmidFinal() && isPmcidFinal() && isDoiFinal();
	}

	public PublicationPartString getTitle() {
		return title;
	}
	void setTitle(String title, PublicationPartType type, String url, FetcherArgs fetcherArgs, boolean clean) {
		set(title, type, url, fetcherArgs, this::isTitleFinal, this.title, true, clean);
	}
	public boolean isTitleFinal(FetcherArgs fetcherArgs) {
		return title.getType().isFinal() && title.getContent().length() >= fetcherArgs.getTitleMinLength();
	}

	public PublicationPartList<String> getKeywords() {
		return keywords;
	}
	void setKeywords(List<String> keywords, PublicationPartType type, String url, FetcherArgs fetcherArgs, boolean clean) {
		List<String> keywordsFull = keywords.stream()
			.filter(k -> k != null)
			.map(k -> k.trim())
			.map(k -> { if (clean) return Jsoup.clean(k, new Whitelist()); else return k; })
			.filter(k -> !k.isEmpty())
			.distinct()
			.collect(Collectors.toList());
		setList(keywordsFull, type, url, fetcherArgs, this::isKeywordsFinal, this.keywords);
	}
	public boolean isKeywordsFinal(FetcherArgs fetcherArgs) {
		return keywords.getType().isFinal() && keywords.getList().size() >= fetcherArgs.getKeywordsMinSize();
	}

	public PublicationPartList<MeshTerm> getMeshTerms() {
		return meshTerms;
	}
	void setMeshTerms(List<MeshTerm> meshTerms, PublicationPartType type, String url, FetcherArgs fetcherArgs) {
		List<MeshTerm> meshTermsFull = meshTerms.stream()
			.filter(k -> k != null && !k.getTerm().isEmpty())
			.collect(Collectors.toList());
		setList(meshTermsFull, type, url, fetcherArgs, this::isMeshTermsFinal, this.meshTerms);
	}
	public boolean isMeshTermsFinal(FetcherArgs fetcherArgs) {
		return meshTerms.getType().isFinal() && meshTerms.getList().size() >= fetcherArgs.getKeywordsMinSize();
	}

	public PublicationPartList<MinedTerm> getEfoTerms() {
		return efoTerms;
	}
	void setEfoTerms(List<MinedTerm> efoTerms, PublicationPartType type, String url, FetcherArgs fetcherArgs) {
		List<MinedTerm> efoTermsFull = efoTerms.stream()
			.filter(k -> k != null && !k.getTerm().isEmpty())
			.collect(Collectors.toList());
		setList(efoTermsFull, type, url, fetcherArgs, this::isEfoTermsFinal, this.efoTerms);
	}
	public boolean isEfoTermsFinal(FetcherArgs fetcherArgs) {
		return efoTerms.getType().isFinal() && efoTerms.getList().size() >= fetcherArgs.getMinedTermsMinSize();
	}

	public PublicationPartList<MinedTerm> getGoTerms() {
		return goTerms;
	}
	void setGoTerms(List<MinedTerm> goTerms, PublicationPartType type, String url, FetcherArgs fetcherArgs) {
		List<MinedTerm> goTermsFull = goTerms.stream()
			.filter(k -> k != null && !k.getTerm().isEmpty())
			.collect(Collectors.toList());
		setList(goTermsFull, type, url, fetcherArgs, this::isGoTermsFinal, this.goTerms);
	}
	public boolean isGoTermsFinal(FetcherArgs fetcherArgs) {
		return goTerms.getType().isFinal() && goTerms.getList().size() >= fetcherArgs.getMinedTermsMinSize();
	}

	public PublicationPartString getAbstract() {
		return theAbstract;
	}
	void setAbstract(String theAbstract, PublicationPartType type, String url, FetcherArgs fetcherArgs, boolean clean) {
		set(theAbstract, type, url, fetcherArgs, this::isAbstractFinal, this.theAbstract, true, clean);
	}
	public boolean isAbstractFinal(FetcherArgs fetcherArgs) {
		return theAbstract.getType().isFinal() && theAbstract.getContent().length() >= fetcherArgs.getAbstractMinLength();
	}

	public PublicationPartString getFulltext() {
		return fulltext;
	}
	void setFulltext(String fulltext, PublicationPartType type, String url, FetcherArgs fetcherArgs) {
		set(fulltext, type, url, fetcherArgs, this::isFulltextFinal, this.fulltext, true, false);
	}
	public boolean isFulltextFinal(FetcherArgs fetcherArgs) {
		return fulltext.getType().isFinal() && fulltext.getContent().length() >= fetcherArgs.getFulltextMinLength();
	}

	public boolean isOA() {
		return oa;
	}
	void setOA(boolean oa) {
		this.oa = oa;
	}

	public String getJournalTitle() {
		return journalTitle;
	}
	void setJournalTitle(String journalTitle) {
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
	void setPubDate(String pubDate) {
		if (this.pubDate < 0) {
			try {
				this.pubDate = LocalDate.parse(pubDate).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
			} catch (DateTimeParseException e) {
				System.err.println("Could not set publication date from " + e.getParsedString() + " (error index " + e.getErrorIndex() + "): " + e.getMessage());
			} catch (ArithmeticException e) {
				System.err.println("Overflow in setting publication date");
			}
		}
	}

	public int getCitationsCount() {
		return citationsCount;
	}
	void setCitationsCount(String citationsCount) {
		if (this.citationsCount < 0) {
			try {
				int citationsCountParsed = Integer.parseInt(citationsCount);
				if (citationsCountParsed >= 0) {
					this.citationsCount = citationsCountParsed;
					this.citationsTimestamp = System.currentTimeMillis();
				} else {
					System.err.println("Citations count is negative: " + citationsCountParsed);
				}
			} catch (NumberFormatException e) {
				System.err.println("Illegal citations count: " + citationsCount);
			}
		}
	}

	public long getCitationsTimestamp() {
		return citationsTimestamp;
	}
	public String getCitationsTimestampHuman() {
		return Instant.ofEpochMilli(citationsTimestamp).toString();
	}

	public String getCorrespAuthor() {
		return correspAuthor;
	}
	void setCorrespAuthor(String correspAuthor) {
		if (this.correspAuthor.isEmpty()) {
			this.correspAuthor = correspAuthor;
		}
	}

	public Set<Link> getVisitedSites() {
		return visitedSites;
	}
	void addVisitedSite(Link visitedSite) {
		visitedSites.add(visitedSite);
	}

	public PublicationPart getPart(PublicationPartName name) {
		switch (name) {
		case pmid: return pmid;
		case pmcid: return pmcid;
		case doi: return doi;
		case title: return title;
		case keywords: return keywords;
		case mesh: return meshTerms;
		case efo: return efoTerms;
		case go: return goTerms;
		case theAbstract: return theAbstract;
		case fulltext: return fulltext;
		default: return null;
		}
	}

	public boolean isPartFinal(PublicationPartName name, FetcherArgs fetcherArgs) {
		switch (name) {
		case pmid: return isPmidFinal();
		case pmcid: return isPmcidFinal();
		case doi: return isDoiFinal();
		case title: return isTitleFinal(fetcherArgs);
		case keywords: return isKeywordsFinal(fetcherArgs);
		case mesh: return isMeshTermsFinal(fetcherArgs);
		case efo: return isEfoTermsFinal(fetcherArgs);
		case go: return isGoTermsFinal(fetcherArgs);
		case theAbstract: return isAbstractFinal(fetcherArgs);
		case fulltext: return isFulltextFinal(fetcherArgs);
		default: return false;
		}
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
	public String toStringPlain() {
		StringBuilder sb = new StringBuilder();
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
	public String toStringPlainHtml() {
		StringBuilder sb = new StringBuilder();
		sb.append("<h2>").append(title.toStringPlainHtml()).append("</h2>\n\n");
		sb.append(keywords.toStringPlainHtml()).append("\n");
		sb.append(meshTerms.toStringPlainHtml()).append("\n");
		sb.append(efoTerms.toStringPlainHtml()).append("\n");
		sb.append(goTerms.toStringPlainHtml()).append("\n\n");
		sb.append(theAbstract.toStringPlainHtml()).append("\n\n");
		sb.append(fulltext.toStringPlainHtml());
		return sb.toString();
	}

	@Override
	public String toStringHtml() {
		StringBuilder sb = new StringBuilder();
		sb.append("<dl>\n").append(super.toString()).append("</dl>\n\n");
		sb.append(pmid.toStringHtml()).append("\n\n");
		sb.append(pmcid.toStringHtml()).append("\n\n");
		sb.append(doi.toStringHtml()).append("\n\n");
		sb.append(title.toStringHtml()).append("\n\n");
		sb.append(keywords.toStringHtml()).append("\n\n");
		sb.append(meshTerms.toStringHtml()).append("\n\n");
		sb.append(efoTerms.toStringHtml()).append("\n\n");
		sb.append(goTerms.toStringHtml()).append("\n\n");
		sb.append(theAbstract.toStringHtml()).append("\n\n");
		sb.append(fulltext.toStringHtml()).append("\n\n");
		sb.append("<dl>\n");
		sb.append("<dt>OPEN ACCESS</dt><dd>").append(oa).append("</dd>\n");
		sb.append("<dt>JOURNAL TITLE</dt><dd>").append(journalTitle).append("</dd>\n");
		sb.append("<dt>PUBLICATION DATE</dt><dd>").append(pubDate).append(" ").append(getPubDateHuman()).append("</dd>\n");
		sb.append("<dt>CITATIONS</dt><dd>").append(citationsCount).append(" (").append(citationsTimestamp).append(" ").append(getCitationsTimestampHuman()).append(")</dd>\n");
		sb.append("<dt>CORRESPONDING AUTHOR</dt><dd>").append(correspAuthor).append("</dd>\n");
		sb.append("<dt>VISITED LINKS</dt><dd><ul>\n");
		for (Link link : visitedSites) {
			sb.append("<li>").append(link.toStringHtml()).append("</li>\n");
		}
		sb.append("</ul></dd></dl>");
		return sb.toString();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString()).append("\n\n");
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
		sb.append("OPEN ACCESS: ").append(oa).append("\n");
		sb.append("JOURNAL TITLE: ").append(journalTitle).append("\n");
		sb.append("PUBLICATION DATE: ").append(pubDate).append(" ").append(getPubDateHuman()).append("\n");
		sb.append("CITATIONS: ").append(citationsCount).append(" (").append(citationsTimestamp).append(" ").append(getCitationsTimestampHuman()).append(")\n");
		sb.append("CORRESPONDING AUTHOR: ").append(correspAuthor).append("\n\n");
		sb.append("VISITED SITES:");
		for (Link link : visitedSites) {
			sb.append("\n").append(link);
		}
		return sb.toString();
	}

	@Override
	public int compareTo(Publication o) {
		if (o == null) return 1;
		return PublicationIds.compareTo(pmid.getContent(), pmcid.getContent(), doi.getContent(),
			o.pmid.getContent(), o.pmcid.getContent(), o.doi.getContent());
	}
}
