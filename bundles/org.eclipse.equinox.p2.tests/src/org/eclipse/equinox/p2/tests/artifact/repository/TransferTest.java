/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.repository;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.osgi.framework.Bundle;

public class TransferTest extends AbstractProvisioningTest {

	public void testGZFileAreNotUnzipped() throws Exception {
		File f = File.createTempFile("TransferTest", "pack.gz");
		try (OutputStream fos = Files.newOutputStream(f.toPath())) {
			Platform.getBundle("org.eclipse.ecf.provider.filetransfer").start();
			final URI toDownload = new URI(
					"https://download.eclipse.org/eclipse/updates/4.11/R-4.11-201903070500/plugins/javax.servlet.jsp_2.2.0.v201112011158.jar.pack.gz");
			IStatus s = getTransport().download(toDownload, fos, new NullProgressMonitor());
			assertOK("2.0", s);
			int httpSize = -1;
			try {
				URL u = toDownload.toURL();
				HttpURLConnection c = (HttpURLConnection) u.openConnection();
				httpSize = c.getContentLength();
			} catch (IOException e1) {
				httpSize = -1;
			}
			if (f != null) {
				String[] ecfPlugins = new String[] { "org.eclipse.ecf", "org.eclipse.ecf.identity",
						"org.eclipse.ecf.filetransfer", "org.eclipse.ecf.provider.filetransfer",
						"org.eclipse.ecf.provider.filetransfer.httpclientjava" };
				StringBuilder buffer = new StringBuilder();
				for (String ecfPlugin : ecfPlugins) {
					Bundle bundle = Platform.getBundle(ecfPlugin);
					buffer.append(bundle.getSymbolicName()).append('-').append(bundle.getVersion()).append('\n');
				}
				assertTrue("4.0 - length found: " + f.length() + " using ECF bundles: " + buffer.toString(),
						f.length() < 60000);
				assertTrue("5.0", httpSize == -1 ? true : (httpSize == f.length()));
			}
		} finally {
			if (f != null)
				f.delete();
		}
	}
}
