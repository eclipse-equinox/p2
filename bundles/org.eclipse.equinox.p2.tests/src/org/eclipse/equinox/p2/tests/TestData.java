/*******************************************************************************
 * Copyright (c) 2007, 2021 compeople AG and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * 	compeople AG (Stefan Liebig) - initial API and implementation
 *  Red Hat, Inc. (Andrew Overholt) - add getFile method
 *******************************************************************************/
package org.eclipse.equinox.p2.tests;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.junit.Assert;

/**
 * <code>TestData</code> helps with handling of test data.
 */
public class TestData {

	private static final String TEST_DATA_ROOT_FOLDER = "testData";
	private static final String PREFIX_SEPERATOR = "~";

	/**
	 * Get an input stream from the resource testDataName within the folder
	 * testDataFolder of the testData folder of this project.
	 * @return input stream for the test data
	 */
	public static InputStream get(String testDataFolder, String testDataName) throws IOException {
		return new BufferedInputStream(TestActivator.getContext().getBundle().getEntry(TEST_DATA_ROOT_FOLDER + "/" + testDataFolder + "/" + testDataName).openStream());
	}

	/**
	 * Get a File from the resource testDataName within the folder
	 * testDataFolder of the testData folder of this project.
	 * @return test data File
	 */
	public static File getFile(String testDataFolder, String testDataName) throws IOException {
		return new File(FileLocator.toFileURL(TestActivator.getContext().getBundle().getEntry(TEST_DATA_ROOT_FOLDER + "/" + testDataFolder + "/" + testDataName)).getPath());
	}

	/**
	 * Create a temporary file for the test data. The temporary file will be deleted
	 * when the jvm exists. If testDataName contains an extension this extension will
	 * be used as suffix for the temporary file.
	 * @return temporary file with test data
	 */
	public static File getTempFile(String testDataFolder, String testDataName) throws IOException {
		File temp = createTempFile(testDataName);
		OutputStream out = new FileOutputStream(temp);
		FileUtils.copyStream(get(testDataFolder, testDataName), true, out, true);
		return temp;
	}

	/**
	 * Create a temporary file. This file will be deleted if the jvm exits.
	 * If testDataName contains an extension this extension will be used as
	 * suffix for the temporary file.
	 * @return temporary file
	 */
	public static File createTempFile(String testDataName) throws IOException {
		int i = testDataName.lastIndexOf('.');
		File temp = (i == -1) ? File.createTempFile(testDataName + PREFIX_SEPERATOR, ".tmp") : File.createTempFile(testDataName.substring(0, i) + PREFIX_SEPERATOR, testDataName.substring(i));
		temp.deleteOnExit();
		return temp;
	}

	/**
	 * Assert equality of files.
	 */
	public static void assertEquals(File expected, File actual) throws IOException {
		Assert.assertEquals("Files have different lengths.", expected.length(), actual.length());
		TestData.assertEquals(new BufferedInputStream(new FileInputStream(expected)), new BufferedInputStream(new FileInputStream(actual)));
	}

	/**
	 * Assert equality of input streams.
	 */
	public static void assertEquals(InputStream expected, InputStream actual) throws IOException {
		try {
			int readExpected = 0;
			int readActual = 0;
			int count = 0;
			while (readActual != -1 && readExpected != -1) {
				readActual = actual.read();
				readExpected = expected.read();
				Assert.assertEquals("Different bytes at " + count, readExpected, readActual);
				count++;
			}
		} finally {
			if (expected != null)
				expected.close();
			if (actual != null)
				actual.close();
		}
	}

	/**
	 * Assert equality of zip input streams.
	 */
	public static void assertEquals(ZipInputStream expected, ZipInputStream actual) throws IOException {
		Map<String, Object[]> expectedEntries = getEntries(expected);
		Map<String, Object[]> actualEntries = getEntries(actual);
		for (String name : expectedEntries.keySet()) {
			Object[] expectedFiles = expectedEntries.get(name);
			Object[] actualFiles = actualEntries.remove(name);
			Assert.assertNotNull(name + " entry is missing in actual zip stream (actual=" + actualEntries.keySet()
					+ ", expected=" + expectedEntries.keySet() + ")", actualFiles);

			ZipEntry entry1 = (ZipEntry) expectedFiles[0];
			ZipEntry entry2 = (ZipEntry) actualFiles[0];
			// compare the entries
			Assert.assertEquals(entry1.getName(), entry2.getName());
			Assert.assertEquals(entry1.getSize(), entry2.getSize());
			//	TODO for now skip over the timestamp as they seem to be different
			// assertTrue(entry1.getTime() == entry2.getTime());
			Assert.assertEquals(entry1.isDirectory(), entry2.isDirectory());
			Assert.assertEquals(entry1.getCrc(), entry2.getCrc());
			Assert.assertEquals(entry1.getMethod(), entry2.getMethod());

			// check the content of the entries
			Assert.assertArrayEquals((byte[]) expectedFiles[1], (byte[]) actualFiles[1]);
		}

		// ensure that we have consumed all of the entries in the second JAR
		Assert.assertEquals(0, actualEntries.size());
	}

	/**
	 * Asserts that the file bytes in <code>fileList</code> are contained in <code>input2</code>
	 * by matching the entry name with the root's path + fileList path.
	 *
	 * @param fileMap a map of files to verify in <code>input2</code> keyed by relative paths
	 * i.e. Map<String filePath, File fileBytes>
	 */
	public static void assertContains(Map<String, Object[]> fileMap, ZipInputStream input2, boolean compareContent) throws IOException {
		Map<String, Object[]> jar2 = getEntries(input2);

		for (String name : fileMap.keySet()) {
			Object[] file1 = fileMap.get(name);
			Object[] file2 = jar2.remove(name);
			Assert.assertNotNull(file2);

			File entry1 = (File) file1[0];
			ZipEntry entry2 = (ZipEntry) file2[0];
			Assert.assertEquals(entry1.isDirectory(), entry2.isDirectory());

			// check the content of the entries
			if (compareContent)
				Assert.assertArrayEquals((byte[]) file1[1], (byte[]) file2[1]);
		}
	}

	private static Map<String, Object[]> getEntries(ZipInputStream input) throws IOException {
		Map<String, Object[]> result = new HashMap<>();
		while (true) {
			ZipEntry entry = input.getNextEntry();
			if (entry == null)
				return result;
			ByteArrayOutputStream content = new ByteArrayOutputStream();
			FileUtils.copyStream(input, false, content, true);
			input.closeEntry();
			result.put(entry.getName(), new Object[] {entry, content.toByteArray()});
		}
	}

}