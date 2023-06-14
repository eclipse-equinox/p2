/*******************************************************************************
 * Copyright (c) 2008,2017 IBM Corporation and others.
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
 *     EclipseSource - Bug fix
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarOutputStream;
import org.eclipse.core.runtime.IPath;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils.IPathComputer;
import org.eclipse.equinox.internal.p2.touchpoint.natives.Util;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;

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

	public void testUnzipEscapeZipRoot() throws IOException {
		File badZip = TestActivator.getContext().getDataFile(getName() + ".zip");
		try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(badZip))) {
			zos.putNextEntry(new ZipEntry("../../escapeRoot.txt"));
			zos.write("test data".getBytes());
			zos.closeEntry();
		}
		File temp = getTempFolder();
		try {
			FileUtils.unzipFile(badZip, temp);
		} catch (IOException e) {
			// expected
			assertTrue("Wrong message: " + e.getMessage(), e.getMessage().contains("Invalid path: "));
		}

		try {
			Util.unzipFile(badZip, temp, null, null, null);
		} catch (IOException e) {
			// expected
			assertTrue("Wrong message: " + e.getMessage(), e.getMessage().contains("Invalid path: "));
		}
	}

	public void testBug266844zip() throws IOException {
		File zip = TestActivator.getContext().getDataFile(getName() + ".zip");
		try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip))) {
			zos.putNextEntry(new ZipEntry("./"));
			zos.putNextEntry(new ZipEntry("./content.txt"));
			zos.write("test data".getBytes());
			zos.closeEntry();
		}
		File temp = getTempFolder();
		FileUtils.unzipFile(zip, temp);
		File extracted = new File(temp, "content.txt");
		assertEquals("test data", Files.readString(extracted.toPath()));
		assertTrue("File not deleted", extracted.delete());
		Util.unzipFile(zip, temp, null, null, null);
		assertEquals("test data", Files.readString(extracted.toPath()));
		assertTrue("File not deleted", extracted.delete());
	}

	public void testBug266844tar() throws IOException {
		File tar = TestActivator.getContext().getDataFile(getName() + ".tar.gz");
		try (TarOutputStream tos = new TarOutputStream(new FileOutputStream(tar))) {
			tos.putNextEntry(new TarEntry("./"));
			TarEntry entry = new TarEntry("./content.txt");
			entry.setSize(9);
			tos.putNextEntry(entry);
			tos.write("test data".getBytes());
			tos.closeEntry();
		}
		File temp = getTempFolder();
		FileUtils.unzipFile(tar, temp);
		File extracted = new File(temp, "content.txt");
		assertEquals("test data", Files.readString(extracted.toPath()));
		assertTrue("File not deleted", extracted.delete());
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
		try (ZipFile zip = new ZipFile(archive)) {
			boolean found = false;
			for (Enumeration<? extends ZipEntry> e = zip.entries(); !found && e.hasMoreElements();) {
				ZipEntry zipEntry = e.nextElement();
				if (entry.equals(zipEntry.getName()))
					found = true;
			}
			assertTrue(message, found);
		} catch (IOException e) {
			fail(message, e);
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
		IPath desired = IPath.fromOSString(output);
		assertEquals(computed, desired);
	}

}
