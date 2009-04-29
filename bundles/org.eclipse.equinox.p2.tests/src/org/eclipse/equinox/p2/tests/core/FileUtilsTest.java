/*******************************************************************************
 * Copyright (c) 2008,2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource - Bug fix
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.core;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils.IPathComputer;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * @since 3.5
 */
public class FileUtilsTest extends AbstractProvisioningTest {

	/*
	 * Test that we can expand zip files and gzip'd tar files.
	 */
	public void testUnzip() {
		File temp = getTempFolder();
		File one = new File(temp, "a.txt");
		File two = new File(temp, "b/b.txt");

		File data = getTestData("1.0", "testData/core/a.zip");
		try {
			FileUtils.unzipFile(data, temp);
		} catch (IOException e) {
			fail("1.99", e);
		}
		assertTrue("1.1", one.exists());
		delete(one);
		assertTrue("1.2", !one.exists());

		data = getTestData("2.0", "testData/core/a2.zip");
		try {
			FileUtils.unzipFile(data, temp);
		} catch (IOException e) {
			fail("2.99", e);
		}
		assertTrue("2.1", one.exists());
		assertTrue("2.2", two.exists());
		delete(one);
		delete(two);
		assertTrue("2.3", !one.exists());
		assertTrue("2.4", !two.exists());

		data = getTestData("3.0", "testData/core/a.tar.gz");
		try {
			FileUtils.unzipFile(data, temp);
		} catch (IOException e) {
			fail("3.99", e);
		}
		assertTrue("3.1", one.exists());
		delete(one);
		assertTrue("3.2", !one.exists());

		data = getTestData("2.0", "testData/core/a2.tar.gz");
		try {
			FileUtils.unzipFile(data, temp);
		} catch (IOException e) {
			fail("3.99", e);
		}
		assertTrue("3.1", one.exists());
		assertTrue("3.2", two.exists());
		delete(one);
		delete(two);
		assertTrue("3.3", !one.exists());
		assertTrue("3.4", !two.exists());

	}

	public void testZipRootPathComputer() {
		File temp = getTempFolder();

		File data = getTestData("1.0", "testData/core/a");
		File archive = new File(temp, getUniqueString() + ".zip");
		try {
			FileUtils.zip(data.listFiles(), null, archive, FileUtils.createRootPathComputer(data));
		} catch (IOException e) {
			fail("1.99", e);
		}
		assertTrue("1.1", archive.exists());
		assertTrue("1.2", archive.length() > 0);
		assertExists("1.3", archive, "a.txt");

		data = getTestData("2.0", "testData/core/a2");
		archive = new File(temp, getUniqueString() + ".zip");
		try {
			FileUtils.zip(data.listFiles(), null, archive, FileUtils.createRootPathComputer(data));
		} catch (IOException e) {
			fail("2.99", e);
		}
		assertTrue("2.1", archive.exists());
		assertTrue("2.2", archive.length() > 0);
		assertExists("2.3", archive, "a.txt");
		assertExists("2.4", archive, "b/b.txt");
		assertExists("2.5", archive, "b/");
	}

	public void testZipDynamicPathComputer() {
		File temp = getTempFolder();

		File data = getTestData("1.0", "testData/core/a2");
		File archive = new File(temp, getUniqueString() + ".zip");
		try {
			FileUtils.zip(data.listFiles(), null, archive, FileUtils.createDynamicPathComputer(0));
			fail("1.99");
		} catch (IOException e) {
			// should fail
		}

		archive = new File(temp, getUniqueString() + ".zip");
		try {
			FileUtils.zip(data.listFiles(), null, archive, FileUtils.createDynamicPathComputer(1));
		} catch (IOException e) {
			fail("2.99", e);
		}
		assertTrue("2.1", archive.exists());
		assertTrue("2.2", archive.length() > 0);
		assertExists("2.3", archive, "a.txt");
		assertExists("2.4", archive, "b.txt");

		archive = new File(temp, getUniqueString() + ".zip");
		try {
			FileUtils.zip(data.listFiles(), null, archive, FileUtils.createDynamicPathComputer(2));
		} catch (IOException e) {
			fail("3.99", e);
		}
		assertTrue("3.1", archive.exists());
		assertTrue("3.2", archive.length() > 0);
		assertExists("3.3", archive, "a2/a.txt");
		assertExists("3.3.1", archive, "a2/");
		assertExists("3.4", archive, "b/b.txt");
		assertExists("3.4.1", archive, "b/");

		archive = new File(temp, getUniqueString() + ".zip");
		File[] input = new File[] {getTestData("4.0", "testData/core/x/y"), getTestData("4.0", "testData/core/z")};
		try {
			FileUtils.zip(input, null, archive, FileUtils.createDynamicPathComputer(2));
		} catch (IOException e) {
			fail("4.99", e);
		}
		assertTrue("4.1", archive.exists());
		assertTrue("4.2", archive.length() > 0);
		assertExists("4.3", archive, "features/feature.txt");
		assertExists("4.3.1", archive, "features/");
		assertExists("4.4", archive, "plugins/bundle.txt");
		assertExists("4.4.1", archive, "plugins/");
	}

	public void testZipParentPrefixComputer() {
		File temp = getTempFolder();

		File data = getTestData("1.0", "testData/core/a2");
		File archive = new File(temp, getUniqueString() + ".zip");
		try {
			FileUtils.zip(data.listFiles(), null, archive, FileUtils.createParentPrefixComputer(0));
			fail("0.99");
		} catch (IOException e) {
			// should fail
		}

		try {
			FileUtils.zip(data.listFiles(), null, archive, FileUtils.createParentPrefixComputer(1));
		} catch (IOException e) {
			fail("1.99", e);
		}
		assertTrue("1.1", archive.exists());
		assertTrue("1.2", archive.length() > 0);
		assertExists("1.3", archive, "a.txt");
		assertExists("1.4", archive, "b.txt");

		archive = new File(temp, getUniqueString() + ".zip");
		try {
			FileUtils.zip(data.listFiles(), null, archive, FileUtils.createParentPrefixComputer(2));
		} catch (IOException e) {
			fail("2.99", e);
		}
		assertTrue("2.1", archive.exists());
		assertTrue("2.2", archive.length() > 0);
		assertExists("2.3", archive, "a2/a.txt");
		assertExists("2.4", archive, "b/b.txt");

		archive = new File(temp, getUniqueString() + ".zip");
		File[] input = new File[] {getTestData("4.0", "testData/core/x/y"), getTestData("4.0", "testData/core/z")};
		try {
			FileUtils.zip(input, null, archive, FileUtils.createParentPrefixComputer(2));
		} catch (IOException e) {
			fail("3.99", e);
		}
		assertTrue("3.1", archive.exists());
		assertTrue("3.2", archive.length() > 0);
		assertExists("3.3", archive, "features/feature.txt");
		assertExists("3.4", archive, "plugins/bundle.txt");
	}

	private static void assertExists(String message, File archive, String entry) {
		if (!archive.exists())
			fail(message + " file does not exist.");
		ZipFile zip = null;
		try {
			zip = new ZipFile(archive);
			boolean found = false;
			for (Enumeration e = zip.entries(); !found && e.hasMoreElements();) {
				ZipEntry zipEntry = (ZipEntry) e.nextElement();
				if (entry.equals(zipEntry.getName()))
					found = true;
			}
			assertTrue(message, found);
		} catch (IOException e) {
			fail(message, e);
		} finally {
			if (zip != null)
				try {
					zip.close();
				} catch (IOException e) {
					// ignore
				}
		}
	}

	// TODO need to test some cases where there are actual File.isFile() == true inputs
	public void testDynamicPathComputer0() {
		IPathComputer computer = FileUtils.createDynamicPathComputer(0);
		validate(computer, "/foo", "");
		validate(computer, "/foo/bar", "bar");
		validate(computer, "/foo/bar/this", "bar/this");
		computer.reset();
		validate(computer, "/foo/bar", "");
		validate(computer, "/foo/bar/this", "this");
	}

	// TODO need to test some cases where there are actual File.isFile() == true inputs
	public void testDynamicPathComputer1() {
		IPathComputer computer = FileUtils.createDynamicPathComputer(1);
		validate(computer, "/foo", "/foo");
		validate(computer, "/foo/bar", "/foo/bar");
		validate(computer, "/foo/bar/this", "/foo/bar/this");
		computer.reset();
		validate(computer, "/foo/bar", "bar");
		validate(computer, "/foo/bar/this", "bar/this");
	}

	public void testParentPathComputer0() {
		IPathComputer computer = FileUtils.createParentPrefixComputer(0);
		validate(computer, "/foo", "");
		validate(computer, "/foo/bar", "");
		validate(computer, "/foo/bar/this", "");
	}

	public void testParentPathComputer1() {
		IPathComputer computer = FileUtils.createParentPrefixComputer(1);
		validate(computer, "/foo", "/foo");
		validate(computer, "/foo/bar", "bar");
		validate(computer, "/foo/bar/this", "this");
	}

	public void testParentPathComputer2() {
		IPathComputer computer = FileUtils.createParentPrefixComputer(2);
		validate(computer, "/foo", "/foo");
		validate(computer, "/foo/bar", "/foo/bar");
		validate(computer, "/foo/bar/this", "bar/this");
	}

	public void testRootPathComputer0() {
		IPathComputer computer = FileUtils.createRootPathComputer(new File("/"));
		validate(computer, "/foo", "/foo");
		validate(computer, "/foo/bar", "/foo/bar");
		validate(computer, "/foo/bar/this", "/foo/bar/this");
	}

	public void testRootPathComputer1() {
		IPathComputer computer = FileUtils.createRootPathComputer(new File("/foo"));
		validate(computer, "/foo", "");
		validate(computer, "/foo/bar", "bar");
		validate(computer, "/foo/bar/this", "bar/this");
	}

	public void testRootPathComputer2() {
		IPathComputer computer = FileUtils.createRootPathComputer(new File("/foo/bar"));
		validate(computer, "/foo", "");
		validate(computer, "/foo/bar", "");
		validate(computer, "/foo/bar/this", "this");
		validate(computer, "/foo/bar/this/that", "this/that");
	}

	private void validate(IPathComputer computer, String input, String output) {
		IPath computed = computer.computePath(new File(input));
		IPath desired = new Path(output);
		assertEquals(computed, desired);
	}

}
