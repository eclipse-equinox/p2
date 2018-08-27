/*******************************************************************************
 * Copyright (c) 2009, 2018 Tasktop Technologies and others.
 *
 * This pro8gram and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.p2.discovery.tests.core;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringReader;
import org.eclipse.equinox.internal.p2.discovery.compatibility.Directory;
import org.eclipse.equinox.internal.p2.discovery.compatibility.DirectoryParser;
import org.junit.Before;
import org.junit.Test;

/**
 * @author David Green
 */
public class DirectoryParserTest {

	private DirectoryParser parser;

	@Before
	public void setUp() {
		parser = new DirectoryParser();
	}

	@Test
	public void testParse() throws IOException {
		Directory directory = parser.parse(new StringReader(
				"<directory xmlns=\"http://www.eclipse.org/mylyn/discovery/directory/\"><entry url=\"http://foo.bar.nodomain/baz.jar\"/></directory>")); //$NON-NLS-1$
		assertNotNull(directory);
		assertEquals(1, directory.getEntries().size());
		assertEquals("http://foo.bar.nodomain/baz.jar", directory.getEntries().get(0).getLocation()); //$NON-NLS-1$
	}

	@Test
	public void testParseBadFormat() {
		try {
			parser.parse(new StringReader(
					"<directory2 xmlns=\"http://www.eclipse.org/mylyn/discovery/directory/\"><entry url=\"http://foo.bar.nodomain/baz.jar\"/></directory2>")); //$NON-NLS-1$
			fail("Expected exception"); //$NON-NLS-1$
		} catch (IOException e) {
			// expected
		}
	}

	@Test
	public void testParseMalformed() {
		try {
			parser.parse(new StringReader(
					"<directory xmlns=\"http://www.eclipse.org/mylyn/discovery/directory/\"><entry url=\"http://foo.bar.nodomain/baz.jar\">")); //$NON-NLS-1$
			fail("Expected exception"); //$NON-NLS-1$
		} catch (IOException e) {
			// expected
		}
	}

	@Test
	public void testParseUnexpectedElementsAndAttributes() throws IOException {
		Directory directory = parser.parse(new StringReader(
				"<directory xmlns=\"http://www.eclipse.org/mylyn/discovery/directory/\"><entry url=\"http://foo.bar.nodomain/baz.jar\" id=\"asdf\"><baz/></entry><foo/></directory>")); //$NON-NLS-1$
		assertNotNull(directory);
		assertEquals(1, directory.getEntries().size());
		assertEquals("http://foo.bar.nodomain/baz.jar", directory.getEntries().get(0).getLocation()); //$NON-NLS-1$
	}

	@Test
	public void testParseNoNS() throws IOException {
		Directory directory = parser
				.parse(new StringReader("<directory><entry url=\"http://foo.bar.nodomain/baz.jar\"/></directory>")); //$NON-NLS-1$
		assertNotNull(directory);
		assertEquals(1, directory.getEntries().size());
		assertEquals("http://foo.bar.nodomain/baz.jar", directory.getEntries().get(0).getLocation()); //$NON-NLS-1$
	}

	@Test
	public void testParsePermitCategoriesTrue() throws IOException {
		Directory directory = parser.parse(new StringReader(
				"<directory xmlns=\"http://www.eclipse.org/mylyn/discovery/directory/\"><entry url=\"http://foo.bar.nodomain/baz.jar\" permitCategories=\"true\"/></directory>")); //$NON-NLS-1$
		assertNotNull(directory);
		assertEquals(1, directory.getEntries().size());
		assertEquals(true, directory.getEntries().get(0).isPermitCategories());
	}

	@Test
	public void testParsePermitCategoriesFalse() throws IOException {
		Directory directory = parser.parse(new StringReader(
				"<directory xmlns=\"http://www.eclipse.org/mylyn/discovery/directory/\"><entry url=\"http://foo.bar.nodomain/baz.jar\" permitCategories=\"false\"/></directory>")); //$NON-NLS-1$
		assertNotNull(directory);
		assertEquals(1, directory.getEntries().size());
		assertEquals(false, directory.getEntries().get(0).isPermitCategories());
	}

	@Test
	public void testParsePermitCategoriesNotSpecified() throws IOException {
		Directory directory = parser.parse(new StringReader(
				"<directory xmlns=\"http://www.eclipse.org/mylyn/discovery/directory/\"><entry url=\"http://foo.bar.nodomain/baz.jar\"/></directory>")); //$NON-NLS-1$
		assertNotNull(directory);
		assertEquals(1, directory.getEntries().size());
		assertEquals(false, directory.getEntries().get(0).isPermitCategories());
	}

	@Test
	public void testParsePermitCategoriesSpecifiedBadly() throws IOException {
		Directory directory = parser.parse(new StringReader(
				"<directory xmlns=\"http://www.eclipse.org/mylyn/discovery/directory/\"><entry url=\"http://foo.bar.nodomain/baz.jar\" permitCategories=\"\"/></directory>")); //$NON-NLS-1$
		assertNotNull(directory);
		assertEquals(1, directory.getEntries().size());
		assertEquals(false, directory.getEntries().get(0).isPermitCategories());
	}

	@Test
	public void testParsePermitCategoriesSpecifiedBadly2() throws IOException {
		Directory directory = parser.parse(new StringReader(
				"<directory xmlns=\"http://www.eclipse.org/mylyn/discovery/directory/\"><entry url=\"http://foo.bar.nodomain/baz.jar\" permitCategories=\"asdf\"/></directory>")); //$NON-NLS-1$
		assertNotNull(directory);
		assertEquals(1, directory.getEntries().size());
		assertEquals(false, directory.getEntries().get(0).isPermitCategories());
	}
}
