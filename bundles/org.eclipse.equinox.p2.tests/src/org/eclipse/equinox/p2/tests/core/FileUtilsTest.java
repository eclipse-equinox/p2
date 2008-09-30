/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.core;

import java.io.File;
import java.io.IOException;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
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

}
