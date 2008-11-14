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
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * Tests for the {@link URLUtil} class.
 */
public class URIUtilTest extends AbstractProvisioningTest {
	private static final String[] testPaths = new String[] {"abc", "with spaces", "with%percent"};

	/**
	 * Tests for {@link URLUtil#toFile(URL)}.
	 */
	public void testToFile() {
		File base = new File(System.getProperty("java.io.tmpdir"));
		for (int i = 0; i < testPaths.length; i++) {
			File original = new File(base, testPaths[i]);
			URI uri = original.toURI();
			File result = URIUtil.toFile(uri);
			assertEquals("1." + i, original, result);
		}
	}

	/**
	 * Tests for {@link URIUtil#toUnencodedString(URI)}.
	 */
	public void testToUnencodedString() throws URISyntaxException {
		assertEquals("1.0", "http://foo.bar", URIUtil.toUnencodedString(new URI("http://foo.bar")));
		assertEquals("1.1", "http://foo.bar#fragment", URIUtil.toUnencodedString(new URI("http://foo.bar#fragment")));
		assertEquals("1.2", "foo.bar#fragment", URIUtil.toUnencodedString(new URI("foo.bar#fragment")));
		assertEquals("1.3", "#fragment", URIUtil.toUnencodedString(new URI("#fragment")));

		//spaces
		assertEquals("2.1", "http://foo.bar/a b", URIUtil.toUnencodedString(new URI("http://foo.bar/a%20b")));
		assertEquals("2.2", "http://foo.bar/a#b c", URIUtil.toUnencodedString(new URI("http://foo.bar/a#b%20c")));
		assertEquals("2.3", "foo.bar/a b", URIUtil.toUnencodedString(new URI("foo.bar/a%20b")));
		assertEquals("2.4", "#a b", URIUtil.toUnencodedString(new URI("#a%20b")));
	}

	/**
	 * Tests for {@link URIUtil#fromString(String)}.
	 */
	public void testFromString() throws URISyntaxException {
		//spaces
		assertEquals("1.1", new URI("http://foo.bar/a%20b"), URIUtil.fromString("http://foo.bar/a b"));
		assertEquals("1.2", new URI("http://foo.bar/a#b%20c"), URIUtil.fromString("http://foo.bar/a#b c"));
		assertEquals("1.3", new URI("foo.bar/a%20b"), URIUtil.fromString("foo.bar/a b"));
		assertEquals("1.4", new URI("#a%20b"), URIUtil.fromString("#a b"));
		assertEquals("1.5", new URI("file:/C:/foo.bar/a%20b"), URIUtil.fromString("file:/C:/foo.bar/a b"));

		//percent character
		assertEquals("2.1", new URI("http://foo.bar/a%2520b"), URIUtil.fromString("http://foo.bar/a%20b"));
		assertEquals("2.2", new URI("http://foo.bar/a#b%2520c"), URIUtil.fromString("http://foo.bar/a#b%20c"));
		assertEquals("2.3", new URI("foo.bar/a%2520b"), URIUtil.fromString("foo.bar/a%20b"));
		assertEquals("2.4", new URI("#a%2520b"), URIUtil.fromString("#a%20b"));
		assertEquals("2.5", new URI("file:/C:/foo.bar/a%2520b"), URIUtil.fromString("file:/C:/foo.bar/a%20b"));
	}

	/**
	 * Tests for {@link URIUtil#toURI(java.net.URL)}.
	 */
	public void testURLtoURI() throws MalformedURLException, URISyntaxException {
		//spaces
		assertEquals("1.1", new URI("http://foo.bar/a%20b"), URIUtil.toURI(new URL("http://foo.bar/a b")));
		assertEquals("1.2", new URI("http://foo.bar/a#b%20c"), URIUtil.toURI(new URL("http://foo.bar/a#b c")));

		//% characters
		assertEquals("1.1", new URI("http://foo.bar/a%25b"), URIUtil.toURI(new URL("http://foo.bar/a%b")));
	}

	/**
	 * Tests handling of Absolute file system paths on Windows incorrectly encoded as
	 * relative URIs (file:c:/tmp).
	 */
	public void testWindowsPathsFromURI() throws MalformedURLException, URISyntaxException {
		if (!isWindows())
			return;
		assertEquals("1.1", new URI("file:/c:/foo/bar.txt"), URIUtil.toURI(new URL("file:c:/foo/bar.txt")));
		assertEquals("1.2", new URI("file:/c:/foo/bar.txt"), URIUtil.toURI(new URL("file:/c:/foo/bar.txt")));
	}

	/**
	 * Tests handling of Absolute file system paths on Windows incorrectly encoded as
	 * relative URIs (file:c:/tmp).
	 */
	public void testWindowsPathsFromString() throws URISyntaxException {
		if (!isWindows())
			return;
		assertEquals("1.1", new URI("file:/c:/foo/bar.txt"), URIUtil.fromString("file:c:/foo/bar.txt"));
		assertEquals("1.2", new URI("file:/c:/foo/bar.txt"), URIUtil.fromString("file:/c:/foo/bar.txt"));
	}

	/**
	 * Tests handling of conversion from a File with spaces to URL and File to URI and equivalence of the resulting URI
	 */
	public void testFileWithSpaces() throws MalformedURLException, URISyntaxException {
		File fileWithSpaces = new File("/c:/with spaces/goo");
		URI correctURI = fileWithSpaces.toURI();
		URL fileURL = fileWithSpaces.toURL();
		URI fileURI = null;
		try {
			fileURI = fileURL.toURI();
			fail();
		} catch (URISyntaxException e) {
			fileURI = URIUtil.toURI(fileURL);
		}
		assertEquals("1.1", correctURI, fileURI);

		try {
			fileURI = new URI(fileURL.toString());
			fail();
		} catch (URISyntaxException e) {
			fileURI = URIUtil.fromString(fileURL.toString());
		}
		assertEquals("1.2", correctURI, fileURI);
	}

	/**
	 * Tests handling of conversion from a File with %20 to URL and File to URI and equivalence of the resulting URI
	 */
	public void testFileWithPercent20() throws MalformedURLException, URISyntaxException {
		File fileWithPercent20 = new File("/c:/with%20spaces/goo");
		URI correctURI = fileWithPercent20.toURI();

		URL fileURL = fileWithPercent20.toURL();
		assertNotSame("1.1", correctURI, fileURL.toURI());
		assertEquals("1.2", correctURI, URIUtil.toURI(fileURL));
		assertNotSame("1.3", correctURI, new URI(fileURL.toString()));
		// we expect these to not be the same because fromString assumes a decoded URL String
		assertNotSame("1.4", correctURI, URIUtil.fromString(fileURL.toString()));
	}

	public void testRemoveExtension() {
		try {
			URI uri1 = new URI("file:/foo/bar/zoo.txt");
			assertEquals(new URI("file:/foo/bar/zoo"), URIUtil.removeFileExtension(uri1));

			URI uri2 = new URI("file:/foo/bar.zoo/foo.txt");
			assertEquals(new URI("file:/foo/bar.zoo/foo"), URIUtil.removeFileExtension(uri2));

			URI uri3 = new URI("file:/foo/bar.zoo/foo");
			assertEquals(new URI("file:/foo/bar.zoo/foo"), URIUtil.removeFileExtension(uri3));

			URI uri4 = new URI("file:/C:/DOCUME~1/ADMINI~1/LOCALS~1/Temp/testRepo/plugins/org.junit_3.8.2.v200706111738.jar");
			assertEquals(new URI("file:/C:/DOCUME~1/ADMINI~1/LOCALS~1/Temp/testRepo/plugins/org.junit_3.8.2.v200706111738"), URIUtil.removeFileExtension(uri4));
		} catch (URISyntaxException e) {
			fail("URI syntax exception", e);
		}
	}

	public void testRemoveFileExtensionFromFile() throws URISyntaxException {
		String fileName = "/c:/some.dir/afile";
		File testFileWithExtension = new File(fileName + ".extension");
		File testFileWithOutExtension = new File(fileName);
		URI correctURI = testFileWithOutExtension.toURI();

		assertEquals(correctURI, URIUtil.removeFileExtension(testFileWithExtension.toURI()));
		assertEquals(correctURI, URIUtil.removeFileExtension(testFileWithOutExtension.toURI()));
	}

	public void testMakeAbsolute() throws URISyntaxException {
		URI[][] data = new URI[][] {
		// simple path
		/*
		new URI[] {new URI("b"), new URI("file:/c:/a/"), new URI("file:/c:/a/b")}, //
		new URI[] {new URI("b"), new URI("file:/c:/a"), new URI("file:/c:/a/b")},
		// common root
		new URI[] {new URI("plugins/foo.jar"), new URI("file:/c:/eclipse/"), new URI("file:/c:/eclipse/plugins/foo.jar")},
		// different drives
		new URI[] {new URI("file:/c:/a/b"), new URI("file:/d:/a/x"), new URI("file:/c:/a/b")}, //
		new URI[] {new URI("file:/c:/eclipse/plugins/foo.jar"), new URI("file:/d:/eclipse/"), new URI("file:/c:/eclipse/plugins/foo.jar")},
		// non-local
		new URI[] {new URI("http:/c:/a/b"), new URI("file:/c:/a/x"), new URI("http:/c:/a/b")}, //
		new URI[] {new URI("file:/c:/a/b"), new URI("http:/c:/a/x"), new URI("file:/c:/a/b")}, //
		//
		new URI[] {new URI("b"), new URI("file:/C:/a/"), new URI("file:/C:/a/b")}, //
		new URI[] {new URI("b"), new URI("file:/C:/a"), new URI("file:/C:/a/b")}, //
		new URI[] {new URI("file:/c:/"), new URI("file:/d:/"), new URI("file:/c:/")}, //
		new URI[] {new URI(""), new URI("file:/c:/"), new URI("file:/c:/")}, //
		//
		new URI[] {new URI("../plugins/foo.jar"), new URI("file:/c:/eclipse/configuration"), new URI("file:/c:/eclipse/plugins/foo.jar")}, //
		*/
		new URI[] {new URI("file:../plugins/foo.jar"), new URI("file:/c:/eclipse/configuration"), new URI("file:/c:/eclipse/plugins/foo.jar")}, //
		};

		for (int i = 0; i < data.length; i++) {
			URI location = data[i][0];
			URI root = data[i][1];
			URI expected = data[i][2];
			URI actual = URIUtil.makeAbsolute(location, root);
			assertEquals(Integer.toString(i), expected, actual);
		}
	}

	public void testMakeRelative() throws URISyntaxException {
		URI[][] data = new URI[][] {
		// simple path
				new URI[] {new URI("file:/c:/a/b"), new URI("file:/c:/a/x"), new URI("b")},
				// common root
				new URI[] {new URI("file:/c:/eclipse/plugins/foo.jar"), new URI("file:/c:/eclipse/"), new URI("plugins/foo.jar")},
				// different drives
				new URI[] {new URI("file:/c:/a/b"), new URI("file:/d:/a/x"), new URI("file:/c:/a/b")}, //
				new URI[] {new URI("file:/c:/eclipse/plugins/foo.jar"), new URI("file:/d:/eclipse/"), new URI("file:/c:/eclipse/plugins/foo.jar")},
				// non-local
				new URI[] {new URI("http:/c:/a/b"), new URI("file:/c:/a/x"), new URI("http:/c:/a/b")}, //
				new URI[] {new URI("file:/c:/a/b"), new URI("http:/c:/a/x"), new URI("file:/c:/a/b")}, //
				//
				new URI[] {new URI("file:/c:/a/b"), new URI("file:/C:/a/x"), new URI("b")}, //
				new URI[] {new URI("file:/c:/"), new URI("file:/d:/"), new URI("file:/c:/")}, //
				new URI[] {new URI("file:/c:/"), new URI("file:/c:/"), new URI("")}, //
		};

		for (int i = 0; i < data.length; i++) {
			URI location = data[i][0];
			URI root = data[i][1];
			URI expected = data[i][2];
			URI actual = URIUtil.makeRelative(location, root);
			assertEquals(Integer.toString(i), expected, actual);
		}
	}
}
