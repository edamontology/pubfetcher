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

import java.io.IOException;
import java.text.ParseException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class FetcherMain {

	private static Logger logger;

	public static void main(String[] argv) throws IOException, ParseException, ReflectiveOperationException {
		Version version = new Version(FetcherMain.class);

		FetcherMainArgs args = BasicArgs.parseArgs(argv, FetcherMainArgs.class, version);

		// logger must be called only after configuration changes have been made in BasicArgs.parseArgs()
		// otherwise invalid.log will be created if arg --log is null
		logger = LogManager.getLogger();
		logger.debug(String.join(" ", argv));
		logger.info("This is {} {}", version.getName(), version.getVersion());

		try {
			FetcherUtil.run(args.fetcherUtilArgs, new Fetcher(args.fetcherArgs), null, null, null);
		} catch (Throwable e) {
			logger.error("Exception!", e);
		}
	}
}
