/*******************************************************************************
 * Copyright (c) 2009, 2010 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.repository;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.osgi.framework.BundleException;

/**
 * Test supposed to be used interactively to monitor the error message output.
 *
 */
public class TransferExceptionsTest extends AbstractProvisioningTest {

	public void testErrorMessages() {
		FileOutputStream fos = null;
		File f = null;
		try {
			f = File.createTempFile("TransferTest", "dummy.txt");
			fos = new FileOutputStream(f);
			Platform.getBundle("org.eclipse.ecf.provider.filetransfer").start();
		} catch (IOException e) {
			fail("1.0", e);
		} catch (BundleException e) {
			fail("1.5", e);
		}
		try {
			IStatus s = getTransport().download(new URI("bogus!bogus"), fos, new NullProgressMonitor());
			assertNotOK(s);
			printStatus("1", s);
			s = getTransport().download(new URI("bogus://somewhere.else"), fos, new NullProgressMonitor());
			assertNotOK(s);
			printStatus("2", s);
			s = getTransport().download(new URI("http:bogusURL"), fos, new NullProgressMonitor());
			assertNotOK(s);
			printStatus("3", s);
			s = getTransport().download(new URI("http://bogusURL:80/"), fos, new NullProgressMonitor());
			assertNotOK(s);
			printStatus("4", s);
			s = getTransport().download(new URI("http:/bogusURL:999999999999/"), fos, new NullProgressMonitor());
			assertNotOK(s);
			printStatus("5", s);
			s = getTransport().download(new URI("http://bogus.nowhere"), fos, new NullProgressMonitor());
			assertNotOK(s);
			printStatus("6", s);
			s = getTransport().download(new URI("http://www.eclipse.org/AFileThatDoesNotExist.foo"), fos, new NullProgressMonitor());
			assertNotOK(s);
			printStatus("7", s);
		} catch (URISyntaxException e) {
			fail("URI syntax exception where none was expected: " + e.getMessage());
		}
	}

	private static void printStatus(String msg, IStatus s) {
		System.err.print("TEST OUTPUT: " + msg + "\n");
		System.err.print("     ");
		System.err.print("Message [" + s.getMessage() + "] Exception Class[" + s.getException().getClass().getName() + "] ExceptionMessage[ ");
		System.err.print(s.getException().getMessage() + "]\n");

	}

}
