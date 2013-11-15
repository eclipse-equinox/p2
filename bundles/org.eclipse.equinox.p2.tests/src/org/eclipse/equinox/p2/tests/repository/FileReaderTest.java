/*******************************************************************************
 * Copyright (c) 2009, 2010 Cloudsmith Inc and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Cloudsmith Inc - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.p2.tests.repository;

import java.io.*;
import java.net.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.transport.ecf.RepositoryTransport;
import org.eclipse.equinox.p2.tests.testserver.helper.AbstractTestServerClientCase;

/**
 * Tests FileReader.
 */
public class FileReaderTest extends AbstractTestServerClientCase {

	public void testUnknownHost() throws URISyntaxException {
		RepositoryTransport transport = new RepositoryTransport();
		URI toDownload = new URI("http://bogus.nowhere/nothing.xml");
		OutputStream target = new ByteArrayOutputStream();
		IStatus status = transport.download(toDownload, target, new NullProgressMonitor());

		assertEquals("Should be an error", status.getSeverity(), IStatus.ERROR);
		assertTrue("Should begin with 'Unknown Host'", status.getMessage().startsWith("Unknown Host"));
	}

	public void testBadPort() throws URISyntaxException {
		RepositoryTransport transport = new RepositoryTransport();
		URI toDownload = new URI("http://localhost:1/nothing.xml");
		OutputStream target = new ByteArrayOutputStream();
		IStatus status = transport.download(toDownload, target, new NullProgressMonitor());

		assertEquals("Should be an error", status.getSeverity(), IStatus.ERROR);
		assertTrue("Should be a connect exception", status.getException() instanceof ConnectException);
		assertTrue("Should begin with 'Connection refused'", status.getException().getMessage().startsWith("Connection refused"));
	}

	/**
	 * Tests a successful read.
	 */
	public void testReadStream() throws URISyntaxException, CoreException, IOException {
		RepositoryTransport transport = new RepositoryTransport();
		URI toDownload = new URI(getBaseURL() + "/public/index.html");
		final NullProgressMonitor monitor = new NullProgressMonitor();
		InputStream stream = transport.stream(toDownload, monitor);
		stream.close();
		assertFalse("1.0", monitor.isCanceled());
	}

	/**
	 * Tests a successful read.
	 */
	public void testRead() throws URISyntaxException {
		RepositoryTransport transport = new RepositoryTransport();
		URI toDownload = new URI(getBaseURL() + "/public/index.html");
		OutputStream target = new ByteArrayOutputStream();
		final NullProgressMonitor monitor = new NullProgressMonitor();
		IStatus result = transport.download(toDownload, target, monitor);
		assertTrue("1.0", result.isOK());
	}
	// TODO: test
	// timeout, cancel of timeout (TimeoutTest)
	// bad date returned, very old, and in the future
	// redirected many times = login

	// handling of incorrect file size
	// 
}
