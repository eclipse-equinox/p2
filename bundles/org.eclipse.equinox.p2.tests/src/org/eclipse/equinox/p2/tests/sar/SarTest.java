/*******************************************************************************
 * Copyright (c) 2007 compeople AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	compeople AG (Stefan Liebig) - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.sar;

import java.io.*;
import java.util.Arrays;
import junit.framework.TestCase;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.p2.sar.SarUtil;
import org.eclipse.equinox.p2.tests.TestActivator;

/**
 * Test the sar stuff.
 */
public class SarTest extends TestCase {

	public void testJarToSarForJdt320() throws IOException {
		InputStream jdt320Jar = getTestData("optimizers", "org.eclipse.jdt_3.2.0.v20060605-1400.njar");
		InputStream jdt320Sar = getTestData("org.eclipse.jdt_3.2.0.v20060605-1400.sar");
		doJarToSar(jdt320Jar, jdt320Sar);
	}

	public void testSarToJarForJdt320() throws IOException {
		InputStream jdt320Sar = getTestData("org.eclipse.jdt_3.2.0.v20060605-1400.sar");
		InputStream jdt320Jar = getTestData("optimizers", "org.eclipse.jdt_3.2.0.v20060605-1400.njar");
		doSarToJar(jdt320Sar, jdt320Jar);
	}

	public void testJarToSarForJdt330() throws IOException {
		InputStream jdt330Jar = getTestData("optimizers", "org.eclipse.jdt_3.3.0.v20070607-1300.njar");
		InputStream jdt330Sar = getTestData("org.eclipse.jdt_3.3.0.v20070607-1300.sar");
		doJarToSar(jdt330Jar, jdt330Sar);
	}

	public void testSarToJarForJdt330() throws IOException {
		InputStream jdt330Sar = getTestData("org.eclipse.jdt_3.3.0.v20070607-1300.sar");
		InputStream jdt330Jar = getTestData("optimizers", "org.eclipse.jdt_3.3.0.v20070607-1300.njar");
		doSarToJar(jdt330Sar, jdt330Jar);
	}

	/**
	 * @throws IOException
	 */
	private void doJarToSar(InputStream jar, InputStream expectedSar) throws IOException {
		File sar = null;
		try {
			sar = createTempFile("doJarToSar", ".sar");
			OutputStream sarOut = new BufferedOutputStream(new FileOutputStream(sar));

			SarUtil.zipToSar(jar, sarOut);

			InputStream sarIn = new BufferedInputStream(new FileInputStream(sar));
			assertEquals(sarIn, expectedSar);
		} finally {
			if (sar != null)
				sar.delete();
		}
	}

	/**
	 * @throws IOException
	 */
	private void doSarToJar(InputStream sar, InputStream expectedJar) throws IOException {
		File jar = null;
		try {
			jar = createTempFile("doSarToJar", ".jar");
			OutputStream jarOut = new BufferedOutputStream(new FileOutputStream(jar));

			SarUtil.sarToZip(sar, jarOut);

			InputStream jarIn = new BufferedInputStream(new FileInputStream(jar));
			assertEquals(jarIn, expectedJar);
		} finally {
			if (jar != null)
				jar.delete();
		}
	}

	/**
	 * @throws IOException
	 */
	public void testZipToSarAndBack() throws IOException {
		File sarFile = null;
		File recreatedZipFile = null;
		try {
			File originalZipFile = getTempFileForBundleData("test.zip");
			sarFile = File.createTempFile("test", ".sar");
			recreatedZipFile = File.createTempFile("test", ".zip");

			long before = System.currentTimeMillis();
			SarUtil.zipToSar(originalZipFile, sarFile);
			System.out.println("zipToSar took: " + (System.currentTimeMillis() - before));

			before = System.currentTimeMillis();
			SarUtil.sarToZip(sarFile, recreatedZipFile);
			System.out.println("sarToZip took: " + (System.currentTimeMillis() - before));

			assertEquals(originalZipFile, recreatedZipFile);
		} finally {
			if (sarFile != null)
				sarFile.delete();
			if (recreatedZipFile != null)
				recreatedZipFile.delete();

		}
	}

	/**
	 * @throws IOException
	 */
	public void testNormalizeOnFiles() throws IOException {
		File alienZip = getTempFileForBundleData("alien.zip");

		File normalizedAlienZip = null;
		File renormalizedAlienZip = null;
		try {
			normalizedAlienZip = File.createTempFile("normalizedalien", ".zip");
			SarUtil.normalize(alienZip, normalizedAlienZip);

			assertTrue(alienZip.length() != normalizedAlienZip.length());

			renormalizedAlienZip = File.createTempFile("renormalizedalien", ".zip");
			SarUtil.normalize(normalizedAlienZip, renormalizedAlienZip);

			assertEquals(normalizedAlienZip, renormalizedAlienZip);
		} finally {
			if (normalizedAlienZip != null)
				normalizedAlienZip.delete();
			if (renormalizedAlienZip != null)
				renormalizedAlienZip.delete();
		}

	}

	/**
	 * @throws IOException
	 */
	public void testNormalizeOnStreames() throws IOException {
		InputStream alienZip = getTestData("alien.zip");
		ByteArrayOutputStream normalizedAlienZip = new ByteArrayOutputStream();
		ByteArrayOutputStream renormalizedAlienZip = new ByteArrayOutputStream();
		try {
			SarUtil.normalize(alienZip, normalizedAlienZip);

			SarUtil.normalize(new ByteArrayInputStream(normalizedAlienZip.toByteArray()), renormalizedAlienZip);

			assertTrue(Arrays.equals(normalizedAlienZip.toByteArray(), renormalizedAlienZip.toByteArray()));
		} finally {
			if (alienZip != null)
				alienZip.close();
		}

	}

	private static File getTempFileForBundleData(String name) throws IOException {
		File file = null;
		try {
			file = createTempFile(name, ".tmp");
			OutputStream output = new BufferedOutputStream(new FileOutputStream(file));
			InputStream input = getTestData(name);
			FileUtils.copyStream(input, true, output, true);
			return file;
		} finally {
			if (file != null)
				file.deleteOnExit();
		}
	}

	private static File createTempFile(String prefix, String suffix) throws IOException {
		File temp = null;
		try {
			temp = File.createTempFile(prefix, suffix);
			return temp;
		} finally {
			if (temp != null)
				temp.deleteOnExit();
		}
	}

	private static InputStream getTestData(String name) throws IOException {
		return getTestData("sar", name);
	}

	private static InputStream getTestData(String folder, String name) throws IOException {
		return new BufferedInputStream(TestActivator.getContext().getBundle().getEntry("testData/" + folder + "/" + name).openStream());
	}

	private static void assertEquals(File expected, File actual) throws IOException {
		assertEquals("Files have different lengths.", expected.length(), actual.length());
		assertEquals(new BufferedInputStream(new FileInputStream(expected)), new BufferedInputStream(new FileInputStream(actual)));
	}

	private static void assertEquals(InputStream expected, InputStream actual) throws IOException {
		try {
			int readExpected = 0;
			int readActual = 0;
			int count = 0;
			while (readActual != -1 && readExpected != -1) {
				readActual = actual.read();
				readExpected = expected.read();
				assertEquals("Different bytes at " + count, readExpected, readActual);
				count++;
			}
		} finally {
			if (expected != null)
				expected.close();
			if (actual != null)
				actual.close();
		}
	}
}
