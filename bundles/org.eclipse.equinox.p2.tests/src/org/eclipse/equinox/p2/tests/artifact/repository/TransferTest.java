/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.repository;

import java.io.*;
import java.net.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

public class TransferTest extends AbstractProvisioningTest {

	public void testGZFileAreNotUnzipped() throws URISyntaxException {
		FileOutputStream fos = null;
		File f = null;
		try {
			f = File.createTempFile("TransferTest", "pack.gz");
			fos = new FileOutputStream(f);
			Platform.getBundle("org.eclipse.ecf.provider.filetransfer").start();
		} catch (IOException e) {
			fail("1.0", e);
		} catch (BundleException e) {
			fail("1.5", e);
		}
		final URI toDownload = new URI("http://download.eclipse.org/eclipse/updates/3.4/plugins/javax.servlet.jsp_2.0.0.v200806031607.jar.pack.gz");
		IStatus s = getTransport().download(toDownload, fos, new NullProgressMonitor());
		assertOK("2.0", s);
		int httpSize = -1;
		URL u;
		try {
			u = toDownload.toURL();
			HttpURLConnection c = (HttpURLConnection) u.openConnection();
			httpSize = c.getContentLength();
		} catch (MalformedURLException e1) {
			httpSize = -1;
		} catch (IOException e) {
			httpSize = -1;
		}
		try {
			fos.close();
			if (f != null) {
				String[] ecfPlugins = new String[] {"org.eclipse.ecf", "org.eclipse.ecf.identity", "org.eclipse.ecf.filetransfer", "org.eclipse.ecf.provider.filetransfer", "org.eclipse.ecf.provider.filetransfer.httpclient4"};
				StringBuffer buffer = new StringBuffer();
				for (int i = 0; i < ecfPlugins.length; i++) {
					Bundle bundle = Platform.getBundle(ecfPlugins[i]);
					buffer.append(bundle.getSymbolicName()).append('-').append(bundle.getVersion()).append('\n');
				}
				assertTrue("4.0 - length found: " + f.length() + " using ECF bundles: " + buffer.toString(), f.length() < 50000);
				assertTrue("5.0", httpSize == -1 ? true : (httpSize == f.length()));
			}
		} catch (IOException e) {
			fail("5.0", e);
		} finally {
			if (f != null)
				f.delete();
		}
	}
}
