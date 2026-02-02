/*******************************************************************************
 * Copyright (c) 2007, 2026 IBM Corporation and others.
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
package org.eclipse.equinox.p2.tests.directorywatcher;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactDescriptor;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.ITouchpointData;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;

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

	public static boolean isZipped(Collection<ITouchpointData> data) {
		if (data == null || data.size() == 0) {
			return false;
		}
		for (ITouchpointData td : data) {
			if (td.getInstruction("zipped") != null) { //$NON-NLS-1$
				return true;
			}
		}
		return false;
	}

	/*
	 * Remove the files from the target, if they exist in the source.
	 */
	private void removeContents(File source, File target) {
		if (source.exists() && source.isDirectory() && target.exists() && target.isDirectory()) {
			File[] files = source.listFiles();
			for (File file : files) {
				if (file != null) {
					delete(new File(target, file.getName()));
				}
			}
		}
	}

	public void testDirectoryWatcherListener() throws IOException {
		File baseFolder = getTestData("0.99", "/testData/directorywatcher1");
		File baseFolder2 = getTestData("0.100", "/testData/directorywatcher2");

		// make sure we remove this file after we finish running the tests
		File folder = getTempFolder();
		toRemove.add(folder);

		// create the watcher
		TestRepositoryWatcher watcher = TestRepositoryWatcher.createWatcher(folder);
		watcher.poll();

		// We should have an empty repository because we haven't done anything yet
		assertEquals(0, watcher.getInstallableUnits().length);
		assertEquals(0, watcher.getArtifactKeys().length);

		// copy the first set of data to the folder so it is discovered and then verify the contents
		copy(baseFolder, folder);
		watcher.poll();
		IArtifactKey[] keys = watcher.getArtifactKeys();
		for (IArtifactKey key : keys) {
			String file = watcher.getArtifactFile(key).getAbsolutePath();
			assertTrue("2.1." + file, file.startsWith(folder.getAbsolutePath()));
		}
		assertEquals(2, watcher.getInstallableUnits().length);
		assertEquals(2, watcher.getArtifactKeys().length);

		// copy the second data set to be discovered and then verify the number of entries
		copy(baseFolder2, folder);
		watcher.poll();
		assertEquals(3, watcher.getInstallableUnits().length);
		assertEquals(3, watcher.getArtifactKeys().length);

		// remove some of the data and then verify the contents
		removeContents(baseFolder, folder);
		watcher.poll();
		assertEquals(1, watcher.getInstallableUnits().length);
		assertEquals(1, watcher.getArtifactKeys().length);
	}

	public void testBundleShape() throws IOException {

		// make sure we remove this file after we finish running the tests
		File folder = getTempFolder();
		toRemove.add(folder);

		// create the watcher
		TestRepositoryWatcher watcher = TestRepositoryWatcher.createWatcher(folder);

		// this folder contains a jared plugin and a directory plugin
		File baseFolder = getTestData("0.99", "/testData/directorywatcher1");
		copy(baseFolder, folder);
		watcher.poll();

		// verify metadata
		IInstallableUnit jaredIU = null;
		IInstallableUnit directoryIU = null;
		IInstallableUnit[] ius = watcher.getInstallableUnits();
		assertEquals(2, ius.length);
		for (IInstallableUnit iu : ius) {
			if (isZipped(iu.getTouchpointData())) {
				assertNull(jaredIU);
				jaredIU = iu;
			} else {
				assertNull(directoryIU);
				directoryIU = iu;
			}
		}
		assertTrue(directoryIU != null && jaredIU != null);

		// verify artifact descriptors
		IArtifactDescriptor jaredDescriptor = null;
		IArtifactDescriptor directoryDescriptor = null;
		IArtifactKey[] keys = watcher.getArtifactKeys();
		assertEquals(2, keys.length);
		for (IArtifactKey key : keys) {
			IArtifactDescriptor[] descriptors = watcher.getArtifactDescriptors(key);
			assertEquals(1, descriptors.length);
			SimpleArtifactDescriptor descriptor = (SimpleArtifactDescriptor) descriptors[0];
			String isFolder = descriptor.getRepositoryProperty("artifact.folder");
			if (Boolean.parseBoolean(isFolder)) {
				assertNull(directoryDescriptor);
				directoryDescriptor = descriptors[0];
			} else {
				assertNull(jaredDescriptor);
				jaredDescriptor = descriptors[0];
			}
		}
		assertTrue(jaredDescriptor != null && directoryDescriptor != null);
	}

	/*
	 * Test to ensure that we convert bundles with Eclipse 2.x-style plugin.xml files into
	 * proper OSGi manifest files so the bundle can be installed
	 * This test has been commented out because support for non-OSGi bundles was
	 * removed. See bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=411892.
	 */
	public void _testPluginXMLConversion() throws IOException {
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
		copy(oldBundleJAR, new File(folder, oldBundleJAR.getName()));
		copy(oldBundleJARTwo, new File(folder, oldBundleJARTwo.getName()));
		copy(oldBundleDirectory, new File(folder, oldBundleDirectory.getName()));
		copy(oldBundleDirectoryTwo, new File(folder, oldBundleDirectoryTwo.getName()));

		// We should have an empty repository because we haven't done anything yet
		TestRepositoryWatcher watcher = TestRepositoryWatcher.createWatcher(folder);
		assertEquals(0, watcher.getInstallableUnits().length);
		assertEquals(0, watcher.getArtifactKeys().length);

		watcher.poll();
		// the repo should have added all 4 bundles
		assertEquals(4, watcher.getInstallableUnits().length);
		assertEquals(4, watcher.getArtifactKeys().length);
	}

}
