/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Mat Booth (Red Hat) - initial implementation
 *******************************************************************************/
package org.eclipse.cbi.webservice.flatpakaging;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.cbi.webservice.server.EmbeddedServer;
import org.eclipse.cbi.webservice.server.EmbeddedServerConfiguration;
import org.eclipse.cbi.webservice.server.EmbeddedServerProperties;
import org.eclipse.cbi.webservice.util.PropertiesReader;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionHandlerFilter;

public class FlatpakPackagerServer {

	@Option(name = "-c", usage = "configuration file")
	private String configurationFilePath = "flatpak-packaging-service.properties";

	@Argument
	private List<String> arguments = new ArrayList<String>();

	public static void main(String[] args) throws Exception {
		new FlatpakPackagerServer().doMain(FileSystems.getDefault(), args);
	}

	private void doMain(FileSystem fs, String[] args) throws Exception, InterruptedException {
		if (parseCmdLineArguments(fs, args)) {
			final Path confPath = fs.getPath(configurationFilePath);
			final EmbeddedServerConfiguration serverConf = new EmbeddedServerProperties(
					PropertiesReader.create(confPath));
			final Path tempFolder = serverConf.getTempFolder();

//			final ProcessExecutor executor = new ProcessExecutor.BasicImpl();
//			final FlatpakPackagerProperties conf = new FlatpakPackagerProperties(PropertiesReader.create(confPath));

			final FlatpakPackagerServlet servlet = FlatpakPackagerServlet.builder().tempFolder(tempFolder).build();

			final EmbeddedServer server = EmbeddedServer.builder().port(serverConf.getServerPort())
					.accessLogFile(serverConf.getAccessLogFile()).servicePathSpec(serverConf.getServicePathSpec())
					.appendServiceVersionToPathSpec(serverConf.isServiceVersionAppendedToPathSpec()).servlet(servlet)
					.tempFolder(tempFolder).log4jConfiguration(serverConf.getLog4jProperties()).build();

			server.start();
		}
	}

	private boolean parseCmdLineArguments(FileSystem fs, String[] args) {
		CmdLineParser parser = new CmdLineParser(this);
		parser.getProperties().withUsageWidth(80);

		try {
			// parse the arguments.
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			System.err.println("java -jar flatpak-packaging-service-x.y.z.jar [options...]");
			// print the list of available options
			parser.printUsage(System.err);
			System.err.println();

			// print option sample. This is useful some time
			System.err.println("  Example: java -jar flatpak-packaging-service-x.y.z.jar "
					+ parser.printExample(OptionHandlerFilter.REQUIRED));

			return false;
		}

		if (!Files.exists(fs.getPath(configurationFilePath))) {
			System.err.println("Configuration file does not exist: '" + configurationFilePath + "'");
			parser.printUsage(System.err);
			System.err.println();
			return false;
		}

		return true;
	}
}
