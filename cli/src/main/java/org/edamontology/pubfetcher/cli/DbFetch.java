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

package org.edamontology.pubfetcher.cli;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.edamontology.pubfetcher.core.common.FetcherArgs;
import org.edamontology.pubfetcher.core.common.PubFetcher;
import org.edamontology.pubfetcher.core.db.Database;
import org.edamontology.pubfetcher.core.db.DatabaseEntry;
import org.edamontology.pubfetcher.core.db.DatabaseEntryType;
import org.edamontology.pubfetcher.core.db.publication.PublicationIds;
import org.edamontology.pubfetcher.core.db.publication.PublicationPartName;
import org.edamontology.pubfetcher.core.fetching.Fetcher;

public class DbFetch implements Runnable {

	private static final Logger logger = LogManager.getLogger();

	static Object lock = new Object();

	static boolean lockDone;
	static int numThreads;
	private static int index;

	static AtomicInteger nullCount;

	private static DatabaseEntryType databaseEntryType;
	private static List<? extends Object> databaseEntryIds;

	private static long startMillis;

	private final PubFetcherArgs args;
	private final Database db;
	private final Fetcher fetcher;
	private final EnumMap<PublicationPartName, Boolean> parts;
	private final FetcherArgs fetcherArgs;
	private final boolean end;
	private final int databaseEntriesLimit;
	private final boolean stderr;

	private static LinkedList<Integer> exceptionIndexes;

	private static List<DatabaseEntry<?>> databaseEntries;
	private static int databaseEntriesCount;

	DbFetch(PubFetcherArgs args, Database db, Fetcher fetcher, EnumMap<PublicationPartName, Boolean> parts, FetcherArgs fetcherArgs, boolean end, int limit, boolean stderr) {
		this.args = args;
		this.db = db;
		this.fetcher = fetcher;
		this.parts = parts;
		this.fetcherArgs = fetcherArgs;
		this.end = end;
		this.databaseEntriesLimit = limit;
		this.stderr = stderr;
	}

	static List<? extends DatabaseEntry<?>> init(DatabaseEntryType type, Set<? extends Object> ids, long start) {
		lockDone = false;
		numThreads = 0;
		index = 0;

		nullCount = new AtomicInteger(0);

		databaseEntryType = type;
		databaseEntryIds = new ArrayList<>(ids);

		startMillis = start;

		exceptionIndexes = new LinkedList<>();

		databaseEntries = new ArrayList<>(ids.size());
		for (int i = 0; i < ids.size(); ++i) {
			databaseEntries.add(null);
		}
		databaseEntriesCount = 0;

		return databaseEntries;
	}

	@Override
	public void run() {
		synchronized(lock) {
			++numThreads;
			lockDone = true;
		}
		try {
			while (true) {
				Object id;
				int localIndex;
				boolean exceptionIndex = false;
				long progressStart;
				synchronized(databaseEntryIds) {
					progressStart = startMillis;
					if (index >= databaseEntryIds.size()) {
						synchronized(exceptionIndexes) {
							if (exceptionIndexes.isEmpty()) {
								break;
							} else {
								localIndex = exceptionIndexes.pop();
								id = databaseEntryIds.get(localIndex);
								exceptionIndex = true;
							}
						}
					} else {
						localIndex = index;
						id = databaseEntryIds.get(localIndex);
						++index;
						if (index >= databaseEntryIds.size()) {
							startMillis = System.currentTimeMillis();
						}
					}
				}

				logger.info((exceptionIndex ? "Refetch" : "Fetch") + " {} {}", databaseEntryType, PubFetcher.progress(localIndex + 1, databaseEntryIds.size(), progressStart));
				if (stderr) {
					System.err.print((exceptionIndex ? "Refetch" : "Fetch") + " " + databaseEntryType + " " + PubFetcher.progress(localIndex + 1, databaseEntryIds.size(), progressStart) + "  \r");
				}

				DatabaseEntry<?> databaseEntry = null;
				switch (databaseEntryType) {
					case publication: databaseEntry = PubFetcher.getPublication((PublicationIds) id, db, fetcher, parts, fetcherArgs); break;
					case webpage: databaseEntry = PubFetcher.getWebpage((String) id, db, fetcher, fetcherArgs); break;
					case doc: databaseEntry = PubFetcher.getDoc((String) id, db, fetcher, fetcherArgs); break;
				}

				if (databaseEntry != null) {
					if (databaseEntry.isFetchException() && !exceptionIndex) {
						synchronized(exceptionIndexes) {
							exceptionIndexes.add(localIndex);
						}
					} else {
						if (PubFetcherMethods.preFilter(args, fetcher, fetcherArgs, databaseEntry, databaseEntryType) && !end) {
							synchronized(databaseEntries) {
								if (databaseEntriesCount >= databaseEntriesLimit) break;
								databaseEntries.set(localIndex, databaseEntry);
								++databaseEntriesCount;
							}
						}
					}
				} else {
					nullCount.getAndIncrement();
				}
			}
		} finally {
			synchronized(lock) {
				--numThreads;
				lock.notifyAll();
			}
		}
	}
}
