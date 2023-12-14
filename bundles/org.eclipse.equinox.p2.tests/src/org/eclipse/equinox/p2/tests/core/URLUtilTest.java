/*******************************************************************************
 * Copyright (c) 2008, 2023 IBM Corporation and others.
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
 *		compeople AG (Stefan Liebig) - Test fix for bug 121201 - Poor performance behind proxy/firewall
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.core;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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
	public void testToFile() throws MalformedURLException {
		File base = new File(System.getProperty("java.io.tmpdir"));
		for (int i = 0; i < testPaths.length; i++) {
			File original = new File(base, testPaths[i]);
			URI uri = original.toURI();
			URL encoded = uri.toURL();
			File result = URLUtil.toFile(encoded);
			assertEquals(original, result);
		}
	}

	/**
	 * Tests for {@link URLUtil#toFile(URL)}.
	 */
	public void testToFileRelative() throws URISyntaxException, MalformedURLException {
		for (int i = 0; i < testPaths.length; i++) {
			File original = new File(testPaths[i]);
			URI uri = new URI(null, testPaths[i], null);
			URL encoded = new URL("file:" + uri.getRawPath());
			File result = URLUtil.toFile(encoded);
			assertEquals(original, result);
		}
	}

	public void testToFileFromLocalURL() throws Exception {
		File original = new File(System.getProperty("java.io.tmpdir"), "repo");
		//this URL is technically not correct because it is not hierarchical, but ensure URLUtil is lenient.
		URL url = new URL("file:" + original.toString());
		File result = URLUtil.toFile(url);
		assertEquals(original, result);
	}

	public void testToFileFromUNC() throws Exception {
		File original = new File("//a/b c");
		// this tests the two slash UNC path that URL creates
		URL url = new URL("file:" + original.toString());
		File result = URLUtil.toFile(url);
		assertEquals(original, result);

		// this tests the four slash UNC path that URI creates
		url = original.toURI().toURL();
		result = URLUtil.toFile(url);
		assertEquals(original, result);
	}
}
