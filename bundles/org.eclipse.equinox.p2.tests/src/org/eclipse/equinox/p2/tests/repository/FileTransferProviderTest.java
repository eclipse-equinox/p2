/*******************************************************************************
 * Copyright (c) 2026 Aleksandar Kurtakov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Aleksandar Kurtakov - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.repository;

import static org.junit.Assert.assertEquals;

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.FileTransfers;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.provider.IFileTransferProtocolToFactoryMapper;
import org.junit.Test;
import org.osgi.framework.Bundle;

/**
 * Tests that the http and https file transfer is done by the default provider
 * (<code>httpclientjava</code>), or by the one named by the system property
 * <code>org.eclipse.equinox.p2.ecf.filetransfer.httpProvider</code>.
 */
public class FileTransferProviderTest {

	private static final String PROVIDER_PROPERTY = "org.eclipse.equinox.p2.ecf.filetransfer.httpProvider";

	@Test
	public void testHttpProvider() throws Exception {
		Bundle bundle = Platform.getBundle("org.eclipse.equinox.p2.ecf.filetransfer");
		bundle.start(Bundle.START_TRANSIENT);
		String expected = System.getProperty(PROVIDER_PROPERTY, "httpclientjava");
		IFileTransferProtocolToFactoryMapper mapper = FileTransfers.getProtocolToFactoryMapper();
		// the providers are declarative services components, wait for them to show up
		long end = System.currentTimeMillis() + 10_000;
		while (!expected.equals(mapper.getRetrieveFileTransferFactoryId("https"))
				&& System.currentTimeMillis() < end) {
			Thread.sleep(100);
		}
		for (String protocol : new String[] { "http", "https" }) {
			assertEquals(protocol, expected, mapper.getRetrieveFileTransferFactoryId(protocol));
			assertEquals(protocol, expected, mapper.getBrowseFileTransferFactoryId(protocol));
		}
	}
}
