/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.repository.ECFTransport;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.osgi.framework.BundleException;

public class TransferTest extends AbstractProvisioningTest {

	public void testGZFileAreNotUnzipped() {
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
		IStatus s = ECFTransport.getInstance().download("http://download.eclipse.org/eclipse/updates/3.5-I-builds/plugins/javax.servlet.jsp_2.0.0.v200806031607.jar.pack.gz", fos, new NullProgressMonitor());
		assertOK("2.0", s);
		try {
			fos.close();
			if (f != null) {
				assertTrue("4.0", f.length() < 50000);
			}
		} catch (IOException e) {
			fail("5.0", e);
		} finally {
			if (f != null)
				f.delete();
		}
	}
}
