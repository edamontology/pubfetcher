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

package org.edamontology.pubfetcher.core.db;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

import org.edamontology.pubfetcher.core.db.publication.Publication;
import org.edamontology.pubfetcher.core.db.publication.PublicationIds;
import org.edamontology.pubfetcher.core.db.webpage.Webpage;

public class Database implements Closeable {

	private static final Logger logger = LogManager.getLogger();

	private final DB db;

	private final HTreeMap<String, String> publicationsMap;
	private final HTreeMap<String, PublicationIds> publicationsMapReverse;
	private final HTreeMap<String, Publication> publications;

	private final HTreeMap<String, Webpage> webpages;
	private final HTreeMap<String, Webpage> docs;

	@SuppressWarnings("unchecked")
	public Database(String database) throws FileNotFoundException {
		if (database == null || !(new File(database).canRead())) {
			throw new FileNotFoundException("Database file does not exist or is not readable!");
		}

		this.db = DBMaker.fileDB(database).closeOnJvmShutdown().transactionEnable().make();

		this.publicationsMap = db.hashMap("publicationsMap", Serializer.STRING, Serializer.STRING).counterEnable().open();
		this.publicationsMapReverse = db.hashMap("publicationsMapReverse", Serializer.STRING, Serializer.JAVA).counterEnable().open();
		this.publications = db.hashMap("publications", Serializer.STRING, Serializer.JAVA).counterEnable().open();

		this.webpages = db.hashMap("webpages", Serializer.STRING, Serializer.JAVA).counterEnable().open();
		this.docs = db.hashMap("docs", Serializer.STRING, Serializer.JAVA).counterEnable().open();
	}

	@SuppressWarnings("unchecked")
	public static void init(String database) throws FileAlreadyExistsException {
		if (database == null || new File(database).exists()) {
			throw new FileAlreadyExistsException(database);
		}

		DB db = DBMaker.fileDB(database).closeOnJvmShutdown().transactionEnable().make();

		db.hashMap("publicationsMap", Serializer.STRING, Serializer.STRING).counterEnable().create();
		db.hashMap("publicationsMapReverse", Serializer.STRING, Serializer.JAVA).counterEnable().create();
		db.hashMap("publications", Serializer.STRING, Serializer.JAVA).counterEnable().create();

		db.hashMap("webpages", Serializer.STRING, Serializer.JAVA).counterEnable().create();
		db.hashMap("docs", Serializer.STRING, Serializer.JAVA).counterEnable().create();

		db.commit();
		db.close();
	}

	private boolean removeOldId(String primaryId, String newId, String oldId, boolean primaryRemoved) {
		if (!newId.isEmpty() && !oldId.isEmpty() && !newId.equals(oldId)) {
			if (!primaryRemoved && primaryId.equals(oldId)) {
				logger.warn("Removing old primary ID {} (overridden by {}) and corresponding publication from database", primaryId, newId);
				removePublication(primaryId, false);
				return true;
			}
			logger.warn("Removing old ID {} (overridden by {}) from database", oldId, newId);
			publicationsMap.remove(oldId);
		}
		if (primaryRemoved) return true;
		else return false;
	}
	private boolean removeOldIds(String primaryId, String pmid, String pmcid, String doi) {
		PublicationIds oldPublicationIds = publicationsMapReverse.get(primaryId);
		if (oldPublicationIds != null) {
			boolean primaryRemoved = false;
			primaryRemoved = removeOldId(primaryId, pmid, oldPublicationIds.getPmid(), primaryRemoved);
			primaryRemoved = removeOldId(primaryId, pmcid, oldPublicationIds.getPmcid(), primaryRemoved);
			primaryRemoved = removeOldId(primaryId, doi, oldPublicationIds.getDoi(), primaryRemoved);
			if (primaryRemoved) return true;
		} else {
			logger.error("Missing publication IDs for primary ID {} in database", primaryId);
		}
		return false;
	}
	public boolean putPublication(Publication publication) {
		if (publication == null) {
			logger.error("Not putting null publication to database");
			return false;
		}
		if (publication.getIdCount() < 1) {
			logger.error("Not putting publication with no IDs to database");
			return false;
		}

		String pmid = publication.getPmid().getContent();
		String pmcid = publication.getPmcid().getContent();
		String doi = publication.getDoi().getContent();

		String pmidPrimary = null;
		if (!pmid.isEmpty()) {
			pmidPrimary = publicationsMap.get(pmid);
			if (pmidPrimary != null) {
				boolean primaryRemoved = removeOldIds(pmidPrimary, pmid, pmcid, doi);
				if (primaryRemoved) {
					pmidPrimary = null;
				}
			}
		}
		String pmcidPrimary = null;
		if (!pmcid.isEmpty()) {
			pmcidPrimary = publicationsMap.get(pmcid);
			if (pmcidPrimary != null) {
				if (pmidPrimary != null && !pmidPrimary.equals(pmcidPrimary)) {
					logger.warn("Removing {}, equivalent to {}, merged by {}",
						publicationsMapReverse.get(pmcid), publicationsMapReverse.get(pmid), publication.toStringId());
					removePublication(pmcid, false);
					pmcidPrimary = null;
				} else if (pmidPrimary == null) {
					boolean primaryRemoved = removeOldIds(pmcidPrimary, pmid, pmcid, doi);
					if (primaryRemoved) {
						pmcidPrimary = null;
					}
				}
			}
		}
		String doiPrimary = null;
		if (!doi.isEmpty()) {
			doiPrimary = publicationsMap.get(doi);
			if (doiPrimary != null) {
				if (pmidPrimary != null && !pmidPrimary.equals(doiPrimary) || pmcidPrimary != null && !pmcidPrimary.equals(doiPrimary)) {
					if (pmidPrimary != null) {
						logger.warn("Removing {}, equivalent to {}, merged by {}",
							publicationsMapReverse.get(doi), publicationsMapReverse.get(pmid), publication.toStringId());
					} else {
						logger.warn("Removing {}, equivalent to {}, merged by {}",
							publicationsMapReverse.get(doi), publicationsMapReverse.get(pmcid), publication.toStringId());
					}
					removePublication(doi, false);
					doiPrimary = null;
				} else if (pmidPrimary == null && pmcidPrimary == null) {
					boolean primaryRemoved = removeOldIds(doiPrimary, pmid, pmcid, doi);
					if (primaryRemoved) {
						doiPrimary = null;
					}
				}
			}
		}

		String id = (pmidPrimary != null ? pmidPrimary : (pmcidPrimary != null ? pmcidPrimary : (doiPrimary != null ? doiPrimary : null)));

		String pmidUrl = publication.getPmid().getUrl();
		String pmcidUrl = publication.getPmcid().getUrl();
		String doiUrl = publication.getDoi().getUrl();

		PublicationIds publicationIds = null;
		if (id != null) {
			PublicationIds oldPublicationIds = publicationsMapReverse.get(id);
			if (oldPublicationIds != null) {
				publicationIds = new PublicationIds(
					!pmid.isEmpty() ? pmid : oldPublicationIds.getPmid(),
					!pmcid.isEmpty() ? pmcid : oldPublicationIds.getPmcid(),
					!doi.isEmpty() ? doi : oldPublicationIds.getDoi(),
					!pmidUrl.isEmpty() ? pmidUrl : oldPublicationIds.getPmidUrl(),
					!pmcidUrl.isEmpty() ? pmcidUrl : oldPublicationIds.getPmcidUrl(),
					!doiUrl.isEmpty() ? doiUrl : oldPublicationIds.getDoiUrl());
			} else {
				logger.error("Missing publication IDs for ID {} in database", id);
			}
		} else {
			if (!pmid.isEmpty()) {
				id = pmid;
			} else if (!pmcid.isEmpty()) {
				id = pmcid;
			} else {
				id = doi;
			}
		}
		if (publicationIds == null) {
			publicationIds = new PublicationIds(pmid, pmcid, doi, pmidUrl, pmcidUrl, doiUrl);
		}

		if (!pmid.isEmpty()) {
			publicationsMap.put(pmid, id);
		}
		if (!pmcid.isEmpty()) {
			publicationsMap.put(pmcid, id);
		}
		if (!doi.isEmpty()) {
			publicationsMap.put(doi, id);
		}
		publicationsMapReverse.put(id, publicationIds);
		publications.put(id, publication);

		return true;
	}

	public boolean putWebpage(Webpage webpage) {
		if (webpage == null) {
			logger.error("Not putting null webpage to database");
			return false;
		}
		if (webpage.getStartUrl().isEmpty()) {
			logger.error("Not putting webpage with no start URL to database");
			return false;
		}
		webpages.put(webpage.getStartUrl(), webpage);
		return true;
	}
	public boolean putDoc(Webpage doc) {
		if (doc == null) {
			logger.error("Not putting null doc to database");
			return false;
		}
		if (doc.getStartUrl().isEmpty()) {
			logger.error("Not putting doc with no start URL to database");
			return false;
		}
		docs.put(doc.getStartUrl(), doc);
		return true;
	}

	public boolean removePublication(String publicationId, boolean alreadyRemoved) {
		if (publicationId == null) {
			logger.error("No ID given for publication removal from database");
			return false;
		}
		String id = publicationsMap.get(publicationId);
		if (id != null) {
			if (alreadyRemoved) {
				logger.warn("Another publication was already removed with an ID corresponding to {}", publicationId);
			}
			Publication publication = publications.get(id);
			if (publication != null) {
				String pmid = publication.getPmid().getContent();
				if (!pmid.isEmpty()) {
					publicationsMap.remove(pmid);
				}
				String pmcid = publication.getPmcid().getContent();
				if (!pmcid.isEmpty()) {
					publicationsMap.remove(pmcid);
				}
				String doi = publication.getDoi().getContent();
				if (!doi.isEmpty()) {
					publicationsMap.remove(doi);
				}
				PublicationIds removedPublicationIds = publicationsMapReverse.remove(id);
				Publication removedPublication = publications.remove(id);
				if (removedPublicationIds == null) {
					logger.error("Can't remove publication IDs for primary ID {} from database", id);
				}
				if (removedPublication == null) {
					logger.error("Can't remove publication for primary ID {} from database", id);
				}
				if (removedPublicationIds == null || removedPublication == null) {
					return false;
				} else {
					return true;
				}
			} else {
				logger.error("Can't find publication with primary ID {} for removal from database", id);
				return false;
			}
		} else {
			if (!alreadyRemoved) {
				logger.warn("Can't find publication with ID {} for removal from database", publicationId);
			}
			return false;
		}
	}
	public boolean removePublication(PublicationIds publicationIds) {
		if (publicationIds == null) {
			logger.error("No IDs given for publication removal from database");
			return false;
		}
		if (publicationIds.isEmpty()) {
			logger.error("Given IDs are empty for publication removal from database");
			return false;
		}
		boolean removedPmid = !publicationIds.getPmid().isEmpty() && removePublication(publicationIds.getPmid(), false);
		boolean removedPmcid = !publicationIds.getPmcid().isEmpty() && removePublication(publicationIds.getPmcid(), removedPmid);
		boolean removedDoi = !publicationIds.getDoi().isEmpty() && removePublication(publicationIds.getDoi(), removedPmid || removedPmcid);
		return removedPmid || removedPmcid || removedDoi;
	}
	public boolean removePublication(Publication publication) {
		if (publication == null) {
			logger.error("null publication given for publication removal from database");
			return false;
		}
		if (publication.getIdCount() < 1) {
			logger.error("publication with no IDs given for publication removal from database");
			return false;
		}
		boolean removedPmid = !publication.getPmid().isEmpty() && removePublication(publication.getPmid().getContent(), false);
		boolean removedPmcid = !publication.getPmcid().isEmpty() && removePublication(publication.getPmcid().getContent(), removedPmid);
		boolean removedDoi = !publication.getDoi().isEmpty() && removePublication(publication.getDoi().getContent(), removedPmid || removedPmcid);
		return removedPmid || removedPmcid || removedDoi;
	}

	public boolean removeWebpage(String webpageUrl) {
		if (webpageUrl == null) {
			logger.error("null start URL given for webpage removal from database");
			return false;
		}
		Webpage removed = webpages.remove(webpageUrl);
		if (removed == null) {
			logger.warn("Can't find webpage with start URL {} for removal from database", webpageUrl);
			return false;
		}
		else return true;
	}
	public boolean removeDoc(String docUrl) {
		if (docUrl == null) {
			logger.error("null start URL given for doc removal from database");
			return false;
		}
		Webpage removed = docs.remove(docUrl);
		if (removed == null) {
			logger.warn("Can't find doc with start URL {} for removal from database", docUrl);
			return false;
		}
		else return true;
	}

	public boolean removeWebpage(Webpage webpage) {
		if (webpage == null) {
			logger.error("null webpage given for webpage removal from database");
			return false;
		}
		return removeWebpage(webpage.getStartUrl());
	}
	public boolean removeDoc(Webpage doc) {
		if (doc == null) {
			logger.error("null doc given for doc removal from database");
			return false;
		}
		return removeDoc(doc.getStartUrl());
	}

	public boolean containsPublication(String publicationId) {
		if (publicationId == null) {
			logger.error("No publication ID given for availability checking in database");
			return false;
		}
		return publicationsMap.containsKey(publicationId);
	}
	public boolean containsPublication(PublicationIds publicationIds) {
		if (publicationIds == null) {
			logger.error("No publication IDs given for availability checking in database");
			return false;
		}
		if (publicationIds.isEmpty()) {
			logger.error("Empty publication IDs given for availability checking in database");
			return false;
		}
		return ((publicationIds.getPmid().isEmpty() || publicationsMap.containsKey(publicationIds.getPmid()))
			&& (publicationIds.getPmcid().isEmpty() || publicationsMap.containsKey(publicationIds.getPmcid()))
			&& (publicationIds.getDoi().isEmpty() || publicationsMap.containsKey(publicationIds.getDoi())));
	}

	public boolean containsWebpage(String webpageUrl) {
		if (webpageUrl == null) {
			logger.error("No webpage start URL given for availability checking in database");
			return false;
		}
		return webpages.containsKey(webpageUrl);
	}
	public boolean containsDoc(String docUrl) {
		if (docUrl == null) {
			logger.error("No doc start URL given for availability checking in database");
			return false;
		}
		return docs.containsKey(docUrl);
	}

	@SuppressWarnings("unchecked")
	public Set<PublicationIds> getPublicationIds() {
		Set<PublicationIds> publicationIds = new LinkedHashSet<>();
		publicationIds.addAll(publicationsMapReverse.values());
		return publicationIds;
	}
	@SuppressWarnings("unchecked")
	public Set<String> getPublicationIdsFlat() {
		Set<String> publicationIdsFlat = new LinkedHashSet<>();
		publicationIdsFlat.addAll(publicationsMap.keySet());
		return publicationIdsFlat;
	}

	public String dumpPublicationsMap() {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> entry : publicationsMap.getEntries()) {
			sb.append(entry.getKey()).append(" -> ").append(entry.getValue()).append("\n");
		}
		return sb.toString();
	}
	public String dumpPublicationsMapReverse() {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, PublicationIds> entry : publicationsMapReverse.getEntries()) {
			sb.append(entry.getKey()).append(" -> ").append(entry.getValue().toStringWithUrl()).append("\n");
		}
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	public Set<String> getWebpageUrls() {
		Set<String> webpageUrls = new LinkedHashSet<>();
		webpageUrls.addAll(webpages.keySet());
		return webpageUrls;
	}
	@SuppressWarnings("unchecked")
	public Set<String> getDocUrls() {
		Set<String> docUrls = new LinkedHashSet<>();
		docUrls.addAll(docs.keySet());
		return docUrls;
	}

	public Publication getPublication(String publicationId, boolean logMissing) {
		if (publicationId == null) {
			logger.error("No ID given for getting publication from database");
			return null;
		}
		String id = publicationsMap.get(publicationId);
		if (id != null) {
			Publication publication = publications.get(id);
			if (publication != null) {
				return publication;
			} else {
				logger.error("No publication found for primary ID {} in database", id);
				return null;
			}
		} else {
			if (logMissing) {
				logger.warn("No publication found for ID {} in database", publicationId);
			}
			return null;
		}
	}
	private void checkGetPublicationMismatch(String given, String present, String query) {
		if (!given.isEmpty() && !present.isEmpty() && !given.equals(present)) {
			logger.warn("Mismatch between ID given ({}) and ID present ({}) in publication got using ID {}", given, present, query);
		}
	}
	public Publication getPublication(PublicationIds publicationIds) {
		if (publicationIds == null) {
			logger.error("No IDs given for getting publication from database");
			return null;
		}
		if (publicationIds.isEmpty()) {
			logger.error("Empty IDs given for getting publication from database");
			return null;
		}
		if (!publicationIds.getPmid().isEmpty()) {
			Publication publication = getPublication(publicationIds.getPmid(), false);
			if (publication != null) {
				checkGetPublicationMismatch(publicationIds.getPmcid(), publication.getPmcid().getContent(), publicationIds.getPmid());
				checkGetPublicationMismatch(publicationIds.getDoi(), publication.getDoi().getContent(), publicationIds.getPmid());
				return publication;
			}
		}
		if (!publicationIds.getPmcid().isEmpty()) {
			Publication publication = getPublication(publicationIds.getPmcid(), false);
			if (publication != null) {
				checkGetPublicationMismatch(publicationIds.getPmid(), publication.getPmid().getContent(), publicationIds.getPmcid());
				checkGetPublicationMismatch(publicationIds.getDoi(), publication.getDoi().getContent(), publicationIds.getPmcid());
				return publication;
			}
		}
		if (!publicationIds.getDoi().isEmpty()) {
			Publication publication = getPublication(publicationIds.getDoi(), false);
			if (publication != null) {
				checkGetPublicationMismatch(publicationIds.getPmid(), publication.getPmid().getContent(), publicationIds.getDoi());
				checkGetPublicationMismatch(publicationIds.getPmcid(), publication.getPmcid().getContent(), publicationIds.getDoi());
				return publication;
			}
		}
		logger.warn("No publication found for IDs {} in database", publicationIds);
		return null;
	}

	public Webpage getWebpage(String webpageUrl) {
		if (webpageUrl == null) {
			logger.error("No start URL given for getting webpage from database");
			return null;
		}
		return webpages.get(webpageUrl);
	}
	public Webpage getDoc(String docUrl) {
		if (docUrl == null) {
			logger.error("No start URL given for getting doc from database");
			return null;
		}
		return docs.get(docUrl);
	}

	public long getPublicationsSize() {
		return publications.sizeLong();
	}

	public long getWebpagesSize() {
		return webpages.sizeLong();
	}
	public long getDocsSize() {
		return docs.sizeLong();
	}

	public void commit() {
		db.commit();
	}

	public void compact() {
		db.getStore().compact();
	}

	@Override
	public void close() throws IOException {
		db.close();
	}
}
