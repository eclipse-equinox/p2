/*******************************************************************************
 * Copyright (c) 2007, 2008 compeople AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	compeople AG (Stefan Liebig) - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.optimizers;

import ie.wombat.jbdiff.JBDiff;
import java.io.*;
import java.util.Arrays;
import junit.framework.TestCase;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.p2.tests.optimizers.TestData;

/**
 * ... <code>Bug209233Test</code> ...
 */
public class Bug209233Test extends TestCase {

	//	public void testGenerateTestDataDiff() throws IOException {
	//		File predecessor = TestData.getTempFile("sar", "org.eclipse.jdt_3.2.0.v20060605-1400.sar");
	//		File current = TestData.getTempFile("sar", "org.eclipse.jdt_3.3.0.v20070607-1300.sar");
	//		File diff = File.createTempFile("org.eclipse.jdt_3.2.0-3.3.0~", ".jbdiff");
	//		JBDiff.bsdiff(predecessor, current, diff);
	//	}

	public void testDiffJdt32SarToJdt33Sar() throws IOException {

		InputStream current = TestData.get("sar", "org.eclipse.jdt_3.3.0.v20070607-1300.sar");
		ByteArrayOutputStream currentBS = new ByteArrayOutputStream();
		FileUtils.copyStream(current, true, currentBS, true);
		byte[] currentBytes = currentBS.toByteArray();

		InputStream predecessor = TestData.get("sar", "org.eclipse.jdt_3.2.0.v20060605-1400.sar");
		ByteArrayOutputStream predecessorBS = new ByteArrayOutputStream();
		FileUtils.copyStream(predecessor, true, predecessorBS, true);
		byte[] predecessorBytes = predecessorBS.toByteArray();

		byte[] actualBytes = JBDiff.bsdiff(predecessorBytes, predecessorBytes.length, currentBytes, currentBytes.length);

		InputStream expected = TestData.get("optimizers", "org.eclipse.jdt_3.2.0-3.3.0.jbdiff");
		ByteArrayOutputStream expectedBS = new ByteArrayOutputStream();
		FileUtils.copyStream(expected, true, expectedBS, true);
		byte[] expectedBytes = expectedBS.toByteArray();

		assertEquals("Different lengths.", expectedBytes.length, actualBytes.length);
		assertTrue("Different bytes.", Arrays.equals(expectedBytes, actualBytes));
	}
}
