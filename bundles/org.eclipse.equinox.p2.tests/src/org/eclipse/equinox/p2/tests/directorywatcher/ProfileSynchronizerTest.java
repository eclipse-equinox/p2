/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
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
import java.util.*;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.equinox.p2.metadata.*;

/**
 * @since 1.0
 */
public class ProfileSynchronizerTest extends AbstractDirectoryWatcherTest {

	/*
	 * Constructor for the class.
	 */
	public ProfileSynchronizerTest(String name) {
		super(name);
	}

	/*
	 * Run all the tests in this class.
	 */
	public static Test suite() {
		return new TestSuite(ProfileSynchronizerTest.class);
	}

	public void test() {
		// empty test because the others aren't enabled yet
	}

	/*
	 * Test that we only try to install bundles with matching platform filters.
	 * 
	 * TODO we don't want to test to see if the bundles are in the repo, but if
	 * the bundles are filtered when they are installed into a profile.
	 */
	public void _testPlatformFilter() {
		String base = "/testData/profileSynchronizer/";
		String[] extensions = new String[] {"bbb_1.0.0.jar", "bbb.linux_1.0.0.jar", "bbb.win32_1.0.0.jar"};
		Set jars = new HashSet();
		for (int i = 0; i < extensions.length; i++)
			jars.add(getTestData("0.99", base + extensions[i]));
		File folder = getTempFolder();
		toRemove.add(folder);
		for (Iterator iter = jars.iterator(); iter.hasNext();) {
			File next = (File) iter.next();
			copy("1.0 " + next.getAbsolutePath(), next, new File(folder, next.getName()));
		}

		// We should have an empty repository because we haven't done anything yet
		TestRepositoryWatcher watcher = TestRepositoryWatcher.createWatcher(folder);
		assertEquals("2.0", 0, watcher.getInstallableUnits().length);
		assertEquals("2.1", 0, watcher.getArtifactKeys().length);

		watcher.poll();

		// which IUs we are expecting is dependent on which OS we are running
		Set expected = new HashSet();
		expected.add("bbb");
		String os = System.getProperty("osgi.os");
		if ("win32".equals(os)) {
			expected.add("bbb.win32");
		} else if ("linux".equals(os)) {
			expected.add("bbb.linux");
		}

		IInstallableUnit[] ius = watcher.getInstallableUnits();
		assertEquals("3.0", expected.size(), ius.length);
		for (int i = 0; i < ius.length; i++)
			assertTrue("3.1 " + ius[i].getId(), expected.contains(ius[i].getId()));
		assertEquals("3.2", expected.size(), watcher.getArtifactKeys().length);
	}

	/*
	 * Test to ensure that we only try to install the highest version of singleton bundles
	 * where multiple versions exist.
	 * 
	 * TODO we don't want to test to see if the bundles are in the repo, but if
	 * the bundles are filtered when they are installed into a profile.
	 */
	public void _testMultipleVersions() {
		File one = getTestData("0.1", "/testData/profileSynchronizer/ccc_1.0.0.jar");
		File two = getTestData("0.2", "/testData/profileSynchronizer/ccc_2.0.0.jar");
		File folder = getTempFolder();
		toRemove.add(folder);

		copy("1.0", one, new File(folder, one.getName()));
		copy("1.1", two, new File(folder, two.getName()));

		TestRepositoryWatcher watcher = TestRepositoryWatcher.createWatcher(folder);

		// We should have an empty repository because we haven't done anything yet
		assertEquals("2.0", 0, watcher.getInstallableUnits().length);
		assertEquals("2.1", 0, watcher.getArtifactKeys().length);

		watcher.poll();

		IInstallableUnit[] ius = watcher.getInstallableUnits();
		IArtifactKey[] artifacts = watcher.getArtifactKeys();
		assertEquals("3.0", 1, ius.length);
		assertEquals("3.1", "ccc", ius[0].getId());
		assertEquals("3.2", Version.create("2.0.0"), ius[0].getVersion());
		assertEquals("4.0", 1, artifacts.length);
	}

}
