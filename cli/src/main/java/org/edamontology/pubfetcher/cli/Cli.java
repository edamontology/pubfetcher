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

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.edamontology.pubfetcher.core.common.BasicArgs;
import org.edamontology.pubfetcher.core.common.Version;
import org.edamontology.pubfetcher.core.fetching.Fetcher;

public final class Cli {

	private static Logger logger;

	public static void main(String[] argv) throws IOException, ReflectiveOperationException {
		Version version = new Version(Cli.class);

		CliArgs args = BasicArgs.parseArgs(argv, CliArgs.class, version, false);

		// logger must be called only after configuration changes have been made in BasicArgs.parseArgs()
		// otherwise invalid.log will be created if arg --log is null
		logger = LogManager.getLogger();
		logger.debug(String.join(" ", argv));
		logger.info("This is {} {} ({})", version.getName(), version.getVersion(), version.getUrl());

		try {
			PubFetcherMethods.run(args.pubFetcherArgs, new Fetcher(args.fetcherArgs.getPrivateArgs()), args.fetcherArgs, null, null, null, version, argv);
		} catch (Throwable e) {
			logger.error("Exception!", e);
			System.exit(1);
		}
	}
}
