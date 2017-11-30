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

import java.io.IOException;
import java.nio.file.Path;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.cbi.webservice.servlet.ResponseFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class FlatpakPackagerServlet extends HttpServlet {

	private final static Logger logger = LoggerFactory.getLogger(FlatpakPackagerServlet.class);

	private static final long serialVersionUID = 686968756312222427L;

	FlatpakPackagerServlet() {
	}

	abstract Path tempFolder();

	public static Builder builder() {
		return new AutoValue_FlatpakPackagerServlet.Builder();
	}

	@AutoValue.Builder
	public static abstract class Builder {
		Builder() {
		}

		public abstract Builder tempFolder(Path tempFolder);

		public abstract FlatpakPackagerServlet build();
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		final ResponseFacade responseFacade = ResponseFacade.builder().servletResponse(resp).build();
		responseFacade.replyError(200, "OK");
	}
}
