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

import java.net.ConnectException;
import java.net.URI;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.repository.AuthenticationFailedException;
import org.eclipse.equinox.internal.p2.transport.ecf.RepositoryTransport;
import org.eclipse.equinox.p2.tests.testserver.helper.AbstractTestServerClientCase;

/**
 * Tests FileInfoReader.
 */
public class FileInfoReaderTest extends AbstractTestServerClientCase {

	public void testUnknownHost() throws Exception {
		RepositoryTransport transport = new RepositoryTransport();
		URI toDownload = new URI("http://bogus.nowhere/nothing.xml");
		IStatus status = null;
		try {
			transport.getLastModified(toDownload, new NullProgressMonitor());
		} catch (CoreException e) {
			status = e.getStatus();
		}
		assertEquals("Should be an error", status.getSeverity(), IStatus.ERROR);
		assertTrue("Should begin with 'Unknown Host'", status.getMessage().startsWith("Unknown Host"));
	}

	public void testBadPort() throws Exception {
		RepositoryTransport transport = new RepositoryTransport();
		URI toDownload = new URI("http://localhost:1/nothing.xml");
		IStatus status = null;
		try {
			transport.getLastModified(toDownload, new NullProgressMonitor());
		} catch (CoreException e) {
			status = e.getStatus();
		}

		assertEquals("Should be an error", status.getSeverity(), IStatus.ERROR);
		assertTrue("Should be a connect exception", status.getException() instanceof ConnectException);
		assertTrue("Should begin with 'Connection refused'", status.getException().getMessage().startsWith("Connection refused"));
	}

	public void testRedirect() throws Exception {
		this.setAladdinLoginService();
		RepositoryTransport transport = new RepositoryTransport();
		// apache http client accepts 100 redirects
		URI toDownload = new URI(getBaseURL() + "/redirect/101/public/index.html");
		boolean caught = false;
		try {
			transport.getLastModified(toDownload, new NullProgressMonitor());
		} catch (AuthenticationFailedException e) {
			caught = true;
		} catch (Throwable t) {
			failNotEquals("Wrong exception on 'redirected too many times'", AuthenticationFailedException.class, t.getClass());
			t.printStackTrace();
		}
		assertTrue("Should have caught AuthenticationFailedException", caught);
	}
	// TODO: test
	// timeout, cancel of timeout (TimeoutTest)
	// bad date returned, very old, and in the future
	// redirected many times = login

}
