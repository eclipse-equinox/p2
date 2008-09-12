/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.reconciler.dropins;

import java.io.*;
import java.net.URL;
import java.util.*;
import org.eclipse.equinox.internal.p2.core.helpers.*;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.osgi.service.datalocation.Location;

/**
 * Abstract class for a reconciler test.
 */
public class AbstractReconcilerTest extends AbstractProvisioningTest {

	private static File output;
	private static Set toRemove = new HashSet();

	/*
	 * Constructor for the class.
	 */
	public AbstractReconcilerTest(String name) {
		super(name);
	}

	/*
	 * Set up the platform binary download and get it ready to run the tests.
	 * This method is not intended to be called by clients, it will be called
	 * automatically when the clients use a ReconcilerTestSuite.
	 */
	public void initialize() throws Exception {
		File file = getPlatformZip();
		output = getTempFolder();
		toRemove.add(output);
		try {
			FileUtils.unzipFile(file, output);
		} catch (IOException e) {
			fail("0.99", e);
		}
	}

	/*
	 * Helper method to return the install location. Return null if it is unavailable.
	 */
	public static File getInstallLocation() {
		Location installLocation = (Location) ServiceHelper.getService(TestActivator.getContext(), Location.class.getName(), Location.INSTALL_FILTER);
		if (installLocation == null || !installLocation.isSet())
			return null;
		URL url = installLocation.getURL();
		if (url == null)
			return null;
		return URLUtil.toFile(url);
	}

	/*
	 * Return a file handle pointing to the platform binary zip. Method never returns null because
	 * it will fail an assert before that.
	 */
	private File getPlatformZip() {
		// Check to see if the user set a system property first
		String property = TestActivator.getContext().getProperty("org.eclipse.equinox.p2.reconciler.tests.platform.archive");
		File file = null;
		if (property == null) {
			// the releng test framework copies the zip so let's look for it...
			// it will be a sibling of the eclipse/ folder that we are running
			File installLocation = getInstallLocation();
			if (installLocation != null) {
				File parent = installLocation.getParentFile();
				if (parent != null) {
					File[] children = parent.listFiles(new FileFilter() {
						public boolean accept(File pathname) {
							String name = pathname.getName();
							return name.startsWith("eclipse-platform-");
						}
					});
					if (children != null && children.length == 1)
						file = children[0];
				}
			}
		} else {
			file = new File(property);
		}
		String message = "Need to set the \"org.eclipse.equinox.p2.reconciler.tests.platform.archive\" system property with a valid path to the platform binary drop or copy the archive to be a sibling of the install folder.";
		assertNotNull(message, file);
		assertTrue(message, file.exists());
		return file;
	}

	/*
	 * Add the given bundle to the drop-ins folder (do a copy).
	 * If the file handle points to a directory, then do a deep copy.
	 */
	protected void addToDropins(String message, File file) {
		File dropins = new File(output, "eclipse/dropins");
		copy(message, file, new File(dropins, file.getName()));
	}

	/*
	 * Remove the given filename from the drop-ins folder.
	 */
	protected boolean removeFromDropins(String message, String filename) {
		File dropins = new File(output, "eclipse/dropins");
		File target = new File(dropins, filename);
		if (!target.exists())
			return false;
		return delete(target);
	}

	/*
	 * Return a boolean value indicating whether or not a bundle with the given id
	 * is installed in the system.
	 */
	protected boolean isInstalled(String bundleId) throws IOException {
		File bundlesInfo = new File(output, "eclipse/configuration/org.eclipse.equinox.simpleconfigurator/bundles.info");
		if (!bundlesInfo.exists())
			return false;
		String line;
		Exception exception = null;
		BufferedReader reader = new BufferedReader(new FileReader(bundlesInfo));
		try {
			while ((line = reader.readLine()) != null) {
				StringTokenizer tokenizer = new StringTokenizer(line, ",");
				if (bundleId.equals(tokenizer.nextToken()))
					return true;
			}
		} catch (IOException e) {
			exception = e;
		} finally {
			try {
				reader.close();
			} catch (IOException ex) {
				if (exception == null)
					throw ex;
			}
		}
		return false;
	}

	/*
	 * Run the reconciler to discover changes in the drop-ins folder and update the system state.
	 */
	protected void reconcile(String message) {
		String command = output.getAbsolutePath() + "/eclipse/eclipse -nosplash -application org.eclipse.equinox.p2.reconciler.application";
		try {
			Process process = Runtime.getRuntime().exec(command);
			process.waitFor();
		} catch (IOException e) {
			fail(message, e);
		} catch (InterruptedException e) {
			fail(message, e);
		}
	}

	/*
	 * Clean up the temporary data used to run the tests.
	 * This method is not intended to be called by clients, it will be called
	 * automatically when the clients use a ReconcilerTestSuite.
	 */
	public void cleanup() throws Exception {
		// rm -rf eclipse sub-dir
		for (Iterator iter = toRemove.iterator(); iter.hasNext();) {
			File next = (File) iter.next();
			FileUtils.deleteAll(next);
		}
		output = null;
		toRemove.clear();
	}

}