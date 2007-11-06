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
package ie.wombat.jbdiff.test;

import ie.wombat.jbdiff.JBDiff;
import ie.wombat.jbdiff.JBPatch;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import junit.framework.TestCase;

public class DiffPatchTest extends TestCase {

	public void testNullData() throws Exception {
		bench("null.data", "null.data");
	}

	public void testOneData() throws Exception {
		bench("one.data", "one.data");
	}

	public void testOneHundredData() throws Exception {
		bench("onehundred.data", "onehundred.data");
	}

	public void testOneHundredXData() throws Exception {
		bench("onehundred.data", "onehundredX.data");
	}

	public void testOneXHundredXData() throws Exception {
		bench("onehundred.data", "oneXhundredX.data");
	}

	public void testPdeCoreJar() throws Exception {
		bench("org.eclipse.pde.core_3.2.jar", "org.eclipse.pde.core_3.3.jar");
	}

	public void testEclipse() throws Exception {
		bench("eclipse-3.2.exe", "eclipse-3.3.exe");
	}

	private void bench(String resource1, String resource2) throws Exception {

		byte[] oldData = getTestData(resource1);
		byte[] newData = getTestData(resource2);

		System.out.println(resource1 + "(" + (oldData.length / 1024)
				+ " kb) -> " + resource2 + "(" + (newData.length / 1024)
				+ " kb)");

		diffAndPatchJBDiff(oldData, newData);

		System.out.println("");
	}

	/**
	 * @param resource1
	 * @param resource2
	 * @throws IOException
	 */
	private void diffAndPatchJBDiff(byte[] oldData, byte[] newData)
			throws IOException {

		try {

			long start = System.currentTimeMillis();

			byte[] diff = JBDiff.bsdiff(oldData, oldData.length, newData,
					newData.length);

			long diffEnd = System.currentTimeMillis();

			byte[] patch = JBPatch.bspatch(oldData, oldData.length, diff);

			long patchEnd = System.currentTimeMillis();

			System.out.println("JBDiff: Size= " + diff.length + " b ("
					+ diff.length / 1024 + " kb), Diffing " + (diffEnd - start)
					+ " ms, Patching: " + (patchEnd - diffEnd) + " ms");

			assertTrue(Arrays.equals(newData, patch));

		} catch (RuntimeException re) {
			System.err.println("JBDiff: error: " + re.getMessage());
		}
	}

	private static byte[] getTestData(String name) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		InputStream input = new BufferedInputStream(Activator.getContext()
				.getBundle().getEntry("testData/" + name).openStream());
		int r;
		while ((r = input.read()) != -1) {
			out.write(r);
		}
		input.close();
		out.close();
		return out.toByteArray();
	}
}