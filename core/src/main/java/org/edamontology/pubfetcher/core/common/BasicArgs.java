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

package org.edamontology.pubfetcher.core.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.lookup.MainMapLookup;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

public abstract class BasicArgs extends Args {

	@Parameter(names = { "-h", "--help" }, help = true, description = "Print this help")
	private boolean help = false;

	public static final String logDescription = "Log file. Records will be appended in case of existing file. Missing parent directories will be created.";
	@Parameter(names = { "-l", "--log" }, description = logDescription)
	private String log = null;

	public boolean isHelp() {
		return help;
	}

	public String getLog() {
		return log;
	}

	private static void removeFileLog() {
		final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		final Configuration config = ctx.getConfiguration();
		config.getLoggerConfig("org.edamontology").removeAppender("Log");
		// TODO org.biotools
		ctx.updateLoggers();
	}

	public static <T extends BasicArgs> T parseArgs(String[] argv, Class<T> clazz, Version version, boolean externalLogPath) throws ReflectiveOperationException {
		T args = clazz.getConstructor().newInstance();
		JCommander jcommander = new JCommander(args);
		try {
			jcommander.parse(argv);
		} catch (ParameterException e) {
			System.err.println(version.getName() + " " + version.getVersion());
			System.err.println(e);
			System.err.println("Use -h or --help for listing valid options");
			System.exit(1);
		}
		if (args.isHelp()) {
			System.out.println(version.getName() + " " + version.getVersion());
			jcommander.usage();
			System.exit(0);
		}
		if (args.getLog() != null) {
			MainMapLookup.setMainArguments(new String[] { args.getLog() });
		} else if (!externalLogPath) {
			removeFileLog();
		}
		return args;
	}

	public static <T extends BasicArgs> void setExternalLogPath(T args, String logPath) {
		if (args.getLog() == null) {
			if (logPath != null) {
				MainMapLookup.setMainArguments(new String[] { logPath });
			} else {
				removeFileLog();
			}
		}
	}

	@Override
	protected void addArgs() {
	}

	@Override
	public String getId() {
		return "mainArgs";
	}

	@Override
	public String getLabel() {
		return "Main";
	}
}
