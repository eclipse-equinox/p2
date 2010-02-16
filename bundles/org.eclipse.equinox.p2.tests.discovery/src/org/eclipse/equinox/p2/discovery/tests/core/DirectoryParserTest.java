/*******************************************************************************
 * Copyright (c) 2009 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.p2.discovery.tests.core;

import java.io.IOException;
import java.io.StringReader;

import junit.framework.TestCase;

import org.eclipse.equinox.internal.p2.discovery.compatibility.Directory;
import org.eclipse.equinox.internal.p2.discovery.compatibility.DirectoryParser;

/**
 * @author David Green
 */
public class DirectoryParserTest extends TestCase {

	private DirectoryParser parser;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		parser = new DirectoryParser();
	}

	public void testParse() throws IOException {
		Directory directory = parser.parse(new StringReader(
				"<directory xmlns=\"http://www.eclipse.org/mylyn/discovery/directory/\"><entry url=\"http://foo.bar.nodomain/baz.jar\"/></directory>"));
		assertNotNull(directory);
		assertEquals(1, directory.getEntries().size());
		assertEquals("http://foo.bar.nodomain/baz.jar", directory.getEntries().get(0).getLocation());
	}

	public void testParseBadFormat() throws IOException {
		try {
			parser.parse(new StringReader(
					"<directory2 xmlns=\"http://www.eclipse.org/mylyn/discovery/directory/\"><entry url=\"http://foo.bar.nodomain/baz.jar\"/></directory2>"));
			fail("Expected exception");
		} catch (IOException e) {
			// expected
		}
	}

	public void testParseMalformed() throws IOException {
		try {
			parser.parse(new StringReader(
					"<directory xmlns=\"http://www.eclipse.org/mylyn/discovery/directory/\"><entry url=\"http://foo.bar.nodomain/baz.jar\">"));
			fail("Expected exception");
		} catch (IOException e) {
			// expected
		}
	}

	public void testParseUnexpectedElementsAndAttributes() throws IOException {
		Directory directory = parser.parse(new StringReader(
				"<directory xmlns=\"http://www.eclipse.org/mylyn/discovery/directory/\"><entry url=\"http://foo.bar.nodomain/baz.jar\" id=\"asdf\"><baz/></entry><foo/></directory>"));
		assertNotNull(directory);
		assertEquals(1, directory.getEntries().size());
		assertEquals("http://foo.bar.nodomain/baz.jar", directory.getEntries().get(0).getLocation());
	}

	public void testParseNoNS() throws IOException {
		Directory directory = parser.parse(new StringReader(
				"<directory><entry url=\"http://foo.bar.nodomain/baz.jar\"/></directory>"));
		assertNotNull(directory);
		assertEquals(1, directory.getEntries().size());
		assertEquals("http://foo.bar.nodomain/baz.jar", directory.getEntries().get(0).getLocation());
	}

	public void testParsePermitCategoriesTrue() throws IOException {
		Directory directory = parser.parse(new StringReader(
				"<directory xmlns=\"http://www.eclipse.org/mylyn/discovery/directory/\"><entry url=\"http://foo.bar.nodomain/baz.jar\" permitCategories=\"true\"/></directory>"));
		assertNotNull(directory);
		assertEquals(1, directory.getEntries().size());
		assertEquals(true, directory.getEntries().get(0).isPermitCategories());
	}

	public void testParsePermitCategoriesFalse() throws IOException {
		Directory directory = parser.parse(new StringReader(
				"<directory xmlns=\"http://www.eclipse.org/mylyn/discovery/directory/\"><entry url=\"http://foo.bar.nodomain/baz.jar\" permitCategories=\"false\"/></directory>"));
		assertNotNull(directory);
		assertEquals(1, directory.getEntries().size());
		assertEquals(false, directory.getEntries().get(0).isPermitCategories());
	}

	public void testParsePermitCategoriesNotSpecified() throws IOException {
		Directory directory = parser.parse(new StringReader(
				"<directory xmlns=\"http://www.eclipse.org/mylyn/discovery/directory/\"><entry url=\"http://foo.bar.nodomain/baz.jar\"/></directory>"));
		assertNotNull(directory);
		assertEquals(1, directory.getEntries().size());
		assertEquals(false, directory.getEntries().get(0).isPermitCategories());
	}

	public void testParsePermitCategoriesSpecifiedBadly() throws IOException {
		Directory directory = parser.parse(new StringReader(
				"<directory xmlns=\"http://www.eclipse.org/mylyn/discovery/directory/\"><entry url=\"http://foo.bar.nodomain/baz.jar\" permitCategories=\"\"/></directory>"));
		assertNotNull(directory);
		assertEquals(1, directory.getEntries().size());
		assertEquals(false, directory.getEntries().get(0).isPermitCategories());
	}

	public void testParsePermitCategoriesSpecifiedBadly2() throws IOException {
		Directory directory = parser.parse(new StringReader(
				"<directory xmlns=\"http://www.eclipse.org/mylyn/discovery/directory/\"><entry url=\"http://foo.bar.nodomain/baz.jar\" permitCategories=\"asdf\"/></directory>"));
		assertNotNull(directory);
		assertEquals(1, directory.getEntries().size());
		assertEquals(false, directory.getEntries().get(0).isPermitCategories());
	}
}
