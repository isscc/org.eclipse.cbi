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
package org.eclipse.cbi.maven.plugins.flatpakager;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.cbi.maven.ExceptionHandler;
import org.eclipse.cbi.maven.MavenLogger;
import org.eclipse.cbi.maven.http.AbstractCompletionListener;
import org.eclipse.cbi.maven.http.HttpClient;
import org.eclipse.cbi.maven.http.HttpRequest;
import org.eclipse.cbi.maven.http.HttpResult;
import org.eclipse.cbi.maven.http.RetryHttpClient;
import org.eclipse.cbi.maven.http.HttpRequest.Builder;
import org.eclipse.cbi.maven.http.apache.ApacheHttpClient;

/**
 * Create a Flatpak application from the file specified as the source and
 * exports the application to a Flatpak repository.
 */
@Mojo(name = "package-flatpak", defaultPhase = LifecyclePhase.PACKAGE)
public class CreateFlatpakMojo extends AbstractMojo {

	/**
	 * An {@code .tar.gz} or {@code .zip} file containing a Linux product from which
	 * to generate a Flatpak application.
	 * 
	 * @since 1.1.5
	 */
	@Parameter(required = true)
	private File source;

	/**
	 * The URL of the webservice for creating Flatpak applications.
	 *
	 * @since 1.1.5
	 */
	@Parameter(required = true, property = "cbi.flatpakager.serviceUrl", defaultValue = "http://build.eclipse.org:31338/flatpak-packager")
	private String serviceUrl;

	/**
	 * The repository to which the new Flatpak application should be exported. If
	 * not specified, a new repository will be created alongside the {@link #source}
	 * file.
	 * 
	 * @since 1.1.5
	 */
	@Parameter
	private File repository;

	/**
	 * Skips the execution of this plugin.
	 * 
	 * @since 1.1.5
	 */
	@Parameter(property = "cbi.flatpakager.skip", defaultValue = "false")
	private boolean skip;

	/**
	 * Whether the build should be stopped if the packaging process fails.
	 *
	 * @since 1.1.5
	 */
	@Parameter(property = "cbi.flatpakager.continueOnFail", defaultValue = "false")
	private boolean continueOnFail;

	/**
	 * Whether the Flatpak application should be GPG signed.
	 * 
	 * @since 1.1.5
	 */
	@Parameter(property = "cbi.flatpakager.sign", defaultValue = "false")
	private boolean sign;

	/**
	 * Determines the timeout in milliseconds for any communication with the
	 * packaging service.
	 * 
	 * A timeout value of zero is interpreted as an infinite timeout.
	 * 
	 * @since 1.1.5
	 */
	@Parameter(property = "cbi.flatpakager.connectTimeoutMillis", defaultValue = "0")
	private int connectTimeoutMillis;

	@Override
	public void execute() throws MojoExecutionException {
		if (skip) {
			getLog().info("Skipping packaging Flatpak application");
			return;
		}

		final ExceptionHandler exceptionHandler = new ExceptionHandler(getLog(), continueOnFail);
		try {
			callFlatpakService(exceptionHandler);
		} catch (IOException e) {
			exceptionHandler.handleError("Packaging of Flatpak application failed", e);
		}
	}

	private void callFlatpakService(ExceptionHandler exceptionHandler) throws IOException, MojoExecutionException {
		HttpClient httpClient = RetryHttpClient.retryRequestOn(ApacheHttpClient.create(new MavenLogger(getLog())))
				.maxRetries(3).waitBeforeRetry(10, TimeUnit.SECONDS).log(new MavenLogger(getLog())).build();

		Builder requestBuilder = HttpRequest.on(URI.create(serviceUrl));

		if (!source.exists()) {
			exceptionHandler.handleError("'source' file must exist");
			return;
		}
		if (!source.toPath().getFileName().toString().endsWith(".tar.gz")
				&& !source.toPath().getFileName().toString().endsWith(".zip")) {
			exceptionHandler.handleError("'source' file name must ends with '.tar.gz' or '.zip'");
			return;
		}
		requestBuilder.withParam("source", source.toPath());

		if (sign) {
			getLog().info("[" + new Date() + "] Creating and signing Flatpak application from '" + source + "'...");
		} else {
			getLog().info("[" + new Date() + "] Creating Flatpak application from '" + source + "'...");
		}
		processOnRemoteServer(httpClient, requestBuilder.build());
	}

	private void processOnRemoteServer(HttpClient httpClient, HttpRequest request) throws IOException {
		HttpRequest.Config requestConfig = HttpRequest.Config.builder().connectTimeoutMillis(connectTimeoutMillis)
				.build();
		httpClient.send(request, requestConfig,
				new AbstractCompletionListener(source.toPath().getParent(), source.toPath().getFileName().toString(),
						CreateFlatpakMojo.class.getSimpleName(), new MavenLogger(getLog())) {
					@Override
					public void onSuccess(HttpResult result) throws IOException {
						if (result.contentLength() == 0) {
							throw new IOException("Length of the returned content is 0");
						}
					}
				});
	}
}
