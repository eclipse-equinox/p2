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
import java.io.IOException;
import java.net.URL;
import java.util.*;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IFileArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.directorywatcher.DirectoryWatcher;
import org.eclipse.equinox.internal.provisional.p2.directorywatcher.RepositoryListener;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.osgi.framework.BundleContext;

public class RepositoryListenerTest extends AbstractProvisioningTest {

	/*
	 * Helper class for testing directory watchers with repository listeners. Allows easy access to the
	 * information known by the metadata and artifact repositories.
	 */
	static class TestRepositoryWatcher extends DirectoryWatcher {

		private RepositoryListener listener;

		/*
		 * Create and return a new test directory watcher class which will listen on the given folder.
		 */
		public static TestRepositoryWatcher createWatcher(File folder) {
			RepositoryListener listener = new RepositoryListener(TestActivator.getContext(), AbstractProvisioningTest.getUniqueString());
			Dictionary props = new Hashtable();
			props.put(DirectoryWatcher.DIR, folder.getAbsolutePath());
			props.put(DirectoryWatcher.POLL, "500");
			TestRepositoryWatcher result = new TestRepositoryWatcher(props, TestActivator.getContext());
			result.addListener(listener);
			return result;
		}

		/*
		 * Constructor for the class.
		 */
		private TestRepositoryWatcher(Dictionary props, BundleContext context) {
			super(props, context);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.equinox.internal.provisional.p2.directorywatcher.DirectoryWatcher#addListener(org.eclipse.equinox.internal.provisional.p2.directorywatcher.DirectoryChangeListener)
		 */
		public synchronized void addListener(RepositoryListener repoListener) {
			super.addListener(repoListener);
			this.listener = repoListener;
		}

		/*
		 * Return the list of all the IUs known to the metadata repository this watcher's listener.
		 */
		public IInstallableUnit[] getInstallableUnits() {
			return (IInstallableUnit[]) listener.getMetadataRepository().query(InstallableUnitQuery.ANY, new Collector(), null).toArray(IInstallableUnit.class);
		}

		/*
		 * Return the list of artifact keys known to this listener's repository.
		 */
		public IArtifactKey[] getArtifactKeys() {
			return listener.getArtifactRepository().getArtifactKeys();
		}

		/*
		 * Return the file associated with the given artifact key.
		 */
		public File getArtifactFile(IArtifactKey key) {
			return ((IFileArtifactRepository) listener.getArtifactRepository()).getArtifactFile(key);
		}
	}

	// list of File objects to remove later during teardown
	private Set toRemove = new HashSet();

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

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.tests.AbstractProvisioningTest#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
		for (Iterator iter = toRemove.iterator(); iter.hasNext();)
			delete((File) iter.next());
		toRemove = new HashSet();
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

	/*
	 * Look up and return a file handle to the given entry in the bundle.
	 */
	private File getTestData(String message, String entry) {
		URL base = TestActivator.getContext().getBundle().getEntry(entry);
		try {
			return new File(FileLocator.toFileURL(base).getPath());
		} catch (IOException e) {
			fail(message, e);
		}
		// avoid compile error... should never reach this code
		return null;
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
		// TODO test plugin.xml in a JAR'd bundle

		// TODO test plugin.xml in a directory-based bundle

		// TODO test plugin.xml in the presence of an Ant Manifest file in a JAR'd bundle

		// TODO test plugin.xml in the presence of an Ant Manifest file in a directory-based bundle

	}
}
