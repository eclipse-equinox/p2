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
import java.net.*;
import org.eclipse.equinox.internal.p2.core.helpers.URLUtil;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * Tests for the {@link URLUtil} class.
 */
public class URLUtilTest extends AbstractProvisioningTest {
	private static final String[] testPaths = new String[] {"abc", "with spaces", "with%percent"};

	/**
	 * Tests for {@link URLUtil#toFile(URL)}.
	 */
	public void _testToFile() {
		File base = new File(System.getProperty("java.io.tmpdir"));
		for (int i = 0; i < testPaths.length; i++) {
			File original = new File(base, testPaths[i]);
			URI uri = original.toURI();
			try {
				URL encoded = uri.toURL();
				File result = URLUtil.toFile(encoded);
				assertEquals("1." + i, original, result);
			} catch (MalformedURLException e) {
				fail("1.99", e);
			}
		}
	}

	/**
	 * Tests for {@link URLUtil#toURI(URL)}.
	 */
	public void testToURI() {
		File base = new File(System.getProperty("java.io.tmpdir"));
		for (int i = 0; i < testPaths.length; i++) {
			File file = new File(base, testPaths[i]);
			URI original = file.toURI();
			try {
				URL encoded = file.toURL();
				URI result = URLUtil.toURI(encoded);
				assertEquals("1." + i, original, result);
			} catch (URISyntaxException e) {
				fail("1.99", e);
			} catch (MalformedURLException e) {
				fail("2.99", e);
			}
		}
	}

	/**
	 * Tests for {@link URLUtil#toURI(URL)}.
	 */
	public void testToFileFromLocalURL() throws Exception {
		File repo = new File(System.getProperty("java.io.tmpdir"), "repo");
		URL url = new URL("file:" + repo.toString());
		URLUtil.toFile(url);
	}
}
