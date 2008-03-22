/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.directorywatcher;

import java.io.File;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;

public class RepositoryListenerTest extends AbstractDirectoryWatcherTest {

	/*
	 * Constructor for the class.
	 */
	public RepositoryListenerTest(String name) {
		super(name);
	}

	/*
	 * Run all the tests in this class.
	 */
	public static Test suite() {
		return new TestSuite(RepositoryListenerTest.class);
	}

	/*
	 * Remove the files from the target, if they exist in the source.
	 */
	private void removeContents(File source, File target) {
		if (source.exists() && source.isDirectory() && target.exists() && target.isDirectory()) {
			File[] files = source.listFiles();
			for (int i = 0; i < files.length; i++)
				if (files[i] != null)
					delete(new File(target, files[i].getName()));
		}
	}

	public void testDirectoryWatcherListener() {
		File baseFolder = getTestData("0.99", "/testData/directorywatcher1");
		File baseFolder2 = getTestData("0.100", "/testData/directorywatcher2");

		// make sure we remove this file after we finish running the tests
		File folder = getTempFolder();
		toRemove.add(folder);

		// create the watcher
		TestRepositoryWatcher watcher = TestRepositoryWatcher.createWatcher(folder);

		// We should have an empty repository because we haven't done anything yet
		assertEquals("1.0", 0, watcher.getInstallableUnits().length);
		assertEquals("1.1", 0, watcher.getArtifactKeys().length);

		// copy the first set of data to the folder so it is discovered and then verify the contents
		copy("2.0", baseFolder, folder);
		watcher.poll();
		IArtifactKey[] keys = watcher.getArtifactKeys();
		for (int i = 0; i < keys.length; i++) {
			String file = watcher.getArtifactFile(keys[i]).getAbsolutePath();
			assertTrue("2.1." + file, file.startsWith(folder.getAbsolutePath()));
		}
		assertEquals("3.0", 2, watcher.getInstallableUnits().length);
		assertEquals("3.1", 2, watcher.getArtifactKeys().length);

		// copy the second data set to be discovered and then verify the number of entries
		copy("4.99", baseFolder2, folder);
		watcher.poll();
		assertEquals("5.0", 3, watcher.getInstallableUnits().length);
		assertEquals("5.1", 3, watcher.getArtifactKeys().length);

		// remove some of the data and then verify the contents
		removeContents(baseFolder, folder);
		watcher.poll();
		assertEquals("6.0", 1, watcher.getInstallableUnits().length);
		assertEquals("6.1", 1, watcher.getArtifactKeys().length);
	}

	/*
	 * Test to ensure that we convert bundles with Eclipse 2.x-style plugin.xml files into 
	 * proper OSGi manifest files so the bundle can be installed
	 */
	public void testPluginXMLConversion() {
		// test plugin.xml in a JAR'd bundle
		File oldBundleJAR = getTestData("0.1", "/testData/repositoryListener/oldBundleJAR.jar");
		// test plugin.xml in a directory-based bundle
		File oldBundleDirectory = getTestData("0.2", "/testData/repositoryListener/oldBundleDirectory");
		// test plugin.xml in the presence of an Ant Manifest file in a JAR'd bundle
		File oldBundleJARTwo = getTestData("0.3", "/testData/repositoryListener/oldBundleJARTwo.jar");
		// test plugin.xml in the presence of an Ant Manifest file in a directory-based bundle
		File oldBundleDirectoryTwo = getTestData("0.4", "/testData/repositoryListener/oldBundleDirectoryTwo");

		// copy the test bundles over to where we will be watching in the repo listener
		File folder = getTempFolder();
		toRemove.add(folder);
		copy("1.0", oldBundleJAR, new File(folder, oldBundleJAR.getName()));
		copy("1.1", oldBundleJARTwo, new File(folder, oldBundleJARTwo.getName()));
		copy("1.2", oldBundleDirectory, new File(folder, oldBundleDirectory.getName()));
		copy("1.3", oldBundleDirectoryTwo, new File(folder, oldBundleDirectoryTwo.getName()));

		// We should have an empty repository because we haven't done anything yet
		TestRepositoryWatcher watcher = TestRepositoryWatcher.createWatcher(folder);
		assertEquals("2.0", 0, watcher.getInstallableUnits().length);
		assertEquals("2.1", 0, watcher.getArtifactKeys().length);

		watcher.poll();
		// the repo should have added all 4 bundles
		assertEquals("3.0", 4, watcher.getInstallableUnits().length);
		assertEquals("3.1", 4, watcher.getArtifactKeys().length);
	}

}
