/*******************************************************************************
 * Copyright (c) 2007, 2018 compeople AG and others.
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
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.sar;

import static org.junit.Assert.assertTrue;

import java.io.*;
import java.util.Arrays;
import org.eclipse.equinox.internal.p2.sar.SarUtil;
import org.eclipse.equinox.p2.tests.optimizers.TestData;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test the sar stuff.
 */
public class SarTest {

	@Test
	@Ignore
	public void testJarToSarForJdt320() throws IOException {
		InputStream jdt320Jar = TestData.get("optimizers", "org.eclipse.jdt_3.2.0.v20060605-1400.njar");
		InputStream jdt320Sar = TestData.get("sar", "org.eclipse.jdt_3.2.0.v20060605-1400.sar");
		doJarToSar(jdt320Jar, jdt320Sar);
	}

	@Test
	@Ignore
	public void testSarToJarForJdt320() throws IOException {
		InputStream jdt320Sar = TestData.get("sar", "org.eclipse.jdt_3.2.0.v20060605-1400.sar");
		InputStream jdt320Jar = TestData.get("optimizers", "org.eclipse.jdt_3.2.0.v20060605-1400.njar");
		doSarToJar(jdt320Sar, jdt320Jar);
	}

	@Test
	@Ignore
	public void testJarToSarForJdt330() throws IOException {
		InputStream jdt330Jar = TestData.get("optimizers", "org.eclipse.jdt_3.3.0.v20070607-1300.njar");
		InputStream jdt330Sar = TestData.get("sar", "org.eclipse.jdt_3.3.0.v20070607-1300.sar");
		doJarToSar(jdt330Jar, jdt330Sar);
	}

	@Test
	@Ignore
	public void testSarToJarForJdt330() throws IOException {
		InputStream jdt330Sar = TestData.get("sar", "org.eclipse.jdt_3.3.0.v20070607-1300.sar");
		InputStream jdt330Jar = TestData.get("optimizers", "org.eclipse.jdt_3.3.0.v20070607-1300.njar");
		doSarToJar(jdt330Sar, jdt330Jar);
	}

	/**
	 * @throws IOException
	 */
	private void doJarToSar(InputStream jar, InputStream expectedSar) throws IOException {
		File sar = TestData.createTempFile("doJarToSar.sar");
		OutputStream sarOut = new BufferedOutputStream(new FileOutputStream(sar));

		SarUtil.zipToSar(jar, sarOut);

		InputStream sarIn = new BufferedInputStream(new FileInputStream(sar));
		TestData.assertEquals(sarIn, expectedSar);
	}

	/**
	 * @throws IOException
	 */
	private void doSarToJar(InputStream sar, InputStream expectedJar) throws IOException {
		File jar = TestData.createTempFile("doSarToJar.jar");
		OutputStream jarOut = new BufferedOutputStream(new FileOutputStream(jar));

		SarUtil.sarToZip(sar, jarOut);

		InputStream jarIn = new BufferedInputStream(new FileInputStream(jar));
		TestData.assertEquals(jarIn, expectedJar);
	}

	@Test
	@Ignore
	public void testZipToSarAndBack() throws IOException {
		File originalZipFile = TestData.getTempFile("sar", "test.zip");
		File sarFile = TestData.createTempFile("test.sar");
		File recreatedZipFile = TestData.createTempFile("test.zip");

		long before = System.currentTimeMillis();
		SarUtil.zipToSar(originalZipFile, sarFile);
		System.out.println("zipToSar took: " + (System.currentTimeMillis() - before));

		before = System.currentTimeMillis();
		SarUtil.sarToZip(sarFile, recreatedZipFile);
		System.out.println("sarToZip took: " + (System.currentTimeMillis() - before));

		TestData.assertEquals(originalZipFile, recreatedZipFile);
	}

	@Test
	public void testNormalizeOnFiles() throws IOException {
		File alienZip = TestData.getTempFile("sar", "alien.zip");

		File normalizedAlienZip = TestData.createTempFile("normalizedalien.zip");
		SarUtil.normalize(alienZip, normalizedAlienZip);

		assertTrue(alienZip.length() != normalizedAlienZip.length());

		File renormalizedAlienZip = TestData.createTempFile("renormalizedalien.zip");
		SarUtil.normalize(normalizedAlienZip, renormalizedAlienZip);

		TestData.assertEquals(normalizedAlienZip, renormalizedAlienZip);
	}

	/**
	 * @throws IOException
	 */
	@Test
	public void testNormalizeOnStreames() throws IOException {
		try (InputStream alienZip = TestData.get("sar", "alien.zip")) {
			ByteArrayOutputStream normalizedAlienZip = new ByteArrayOutputStream();
			ByteArrayOutputStream renormalizedAlienZip = new ByteArrayOutputStream();
			SarUtil.normalize(alienZip, normalizedAlienZip);

			SarUtil.normalize(new ByteArrayInputStream(normalizedAlienZip.toByteArray()), renormalizedAlienZip);

			assertTrue(Arrays.equals(normalizedAlienZip.toByteArray(), renormalizedAlienZip.toByteArray()));
		}

	}

}
