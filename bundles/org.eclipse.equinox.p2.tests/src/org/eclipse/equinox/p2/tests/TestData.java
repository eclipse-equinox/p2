/*******************************************************************************
 * Copyright (c) 2007, 2008 compeople AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	compeople AG (Stefan Liebig) - initial API and implementation
 *  Red Hat, Inc. (Andrew Overholt) - add getFile method
 *******************************************************************************/
package org.eclipse.equinox.p2.tests;

import java.io.*;
import java.util.*;
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
	 * @param testDataFolder
	 * @param testDataName
	 * @return input stream for the test data
	 * @throws IOException
	 */
	public static InputStream get(String testDataFolder, String testDataName) throws IOException {
		return new BufferedInputStream(TestActivator.getContext().getBundle().getEntry(TEST_DATA_ROOT_FOLDER + "/" + testDataFolder + "/" + testDataName).openStream());
	}

	/**
	 * Get a File from the resource testDataName within the folder 
	 * testDataFolder of the testData folder of this project.
	 * @param testDataFolder
	 * @return test data File
	 * @throws IOException
	 */
	public static File getFile(String testDataFolder, String testDataName) throws IOException {
		return new File(FileLocator.toFileURL(TestActivator.getContext().getBundle().getEntry(TEST_DATA_ROOT_FOLDER + "/" + testDataFolder + "/" + testDataName)).getPath());
	}

	/**
	 * Create a temporary file for the test data. The temporary file will be deleted 
	 * when the jvm exists. If testDataName contains an extension this extension will
	 * be used as suffix for the temporary file.
	 * @param testDataFolder
	 * @param testDataName
	 * @return temporary file with test data
	 * @throws IOException
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
	 * @param testDataName
	 * @return temporary file
	 * @throws IOException
	 */
	public static File createTempFile(String testDataName) throws IOException {
		int i = testDataName.lastIndexOf('.');
		File temp = (i == -1) ? File.createTempFile(testDataName + PREFIX_SEPERATOR, ".tmp") : File.createTempFile(testDataName.substring(0, i) + PREFIX_SEPERATOR, testDataName.substring(i));
		temp.deleteOnExit();
		return temp;
	}

	/**
	 * Assert equality of files.
	 * @param expected
	 * @param actual
	 * @throws IOException
	 */
	public static void assertEquals(File expected, File actual) throws IOException {
		Assert.assertEquals("Files have different lengths.", expected.length(), actual.length());
		TestData.assertEquals(new BufferedInputStream(new FileInputStream(expected)), new BufferedInputStream(new FileInputStream(actual)));
	}

	/**
	 * Assert equality of input streams.
	 * @param expected
	 * @param actual
	 * @throws IOException
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
	 * @param expected
	 * @param actual
	 * @throws IOException
	 */
	public static void assertEquals(ZipInputStream expected, ZipInputStream actual) throws IOException {
		Map jar1 = getEntries(expected);
		Map jar2 = getEntries(actual);
		for (Iterator i = jar1.keySet().iterator(); i.hasNext();) {
			String name = (String) i.next();
			Object[] file1 = (Object[]) jar1.get(name);
			Object[] file2 = (Object[]) jar2.remove(name);
			Assert.assertNotNull(file2);

			ZipEntry entry1 = (ZipEntry) file1[0];
			ZipEntry entry2 = (ZipEntry) file2[0];
			// compare the entries
			Assert.assertTrue(entry1.getName().equals(entry2.getName()));
			Assert.assertTrue(entry1.getSize() == entry2.getSize());
			//	TODO for now skip over the timestamp as they seem to be different
			// assertTrue(entry1.getTime() == entry2.getTime());
			Assert.assertTrue(entry1.isDirectory() == entry2.isDirectory());
			Assert.assertTrue(entry1.getCrc() == entry2.getCrc());
			Assert.assertTrue(entry1.getMethod() == entry2.getMethod());

			// check the content of the entries
			Assert.assertTrue(Arrays.equals((byte[]) file1[1], (byte[]) file2[1]));
		}

		// ensure that we have consumed all of the entries in the second JAR
		Assert.assertTrue(jar2.size() == 0);
	}

	/**
	 * Asserts that the file bytes in <code>fileList</code> are contained in <code>input2</code>
	 * by matching the entry name with the root's path + fileList path.
	 * 
	 * @param fileMap a map of files to verify in <code>input2</code> keyed by relative paths
	 * i.e. Map<String filePath, File fileBytes>
	 * @param input2
	 * @throws IOException
	 */
	public static void assertContains(Map fileMap, ZipInputStream input2, boolean compareContent) throws IOException {
		Map jar2 = getEntries(input2);

		for (Iterator i = fileMap.keySet().iterator(); i.hasNext();) {
			String name = (String) i.next();
			Object[] file1 = (Object[]) fileMap.get(name);
			Object[] file2 = (Object[]) jar2.remove(name);
			Assert.assertNotNull(file2);

			File entry1 = (File) file1[0];
			ZipEntry entry2 = (ZipEntry) file2[0];
			Assert.assertTrue(entry1.isDirectory() == entry2.isDirectory());

			// check the content of the entries
			if (compareContent)
				Assert.assertTrue(Arrays.equals((byte[]) file1[1], (byte[]) file2[1]));
		}
	}

	private static Map getEntries(ZipInputStream input) throws IOException {
		Map result = new HashMap();
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