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

import java.nio.file.Path;

import org.eclipse.cbi.webservice.util.PropertiesReader;

public class FlatpakPackagerProperties {

	// Signing stuff
	private static final String GPG_KEY = "gpg.key";
	private static final String GPG_HOME = "gpg.home";

	private final PropertiesReader propertiesReader;

	public FlatpakPackagerProperties(PropertiesReader propertiesReader) {
		this.propertiesReader = propertiesReader;
	}

	public String getGpgKey() {
		return propertiesReader.getString(GPG_KEY);
	}

	public Path getGpgHome() {
		return propertiesReader.getPath(GPG_HOME);
	}
}
