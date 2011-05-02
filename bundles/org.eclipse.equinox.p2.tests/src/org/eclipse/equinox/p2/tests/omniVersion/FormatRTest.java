/*******************************************************************************
 * Copyright (c) 2009, 2010 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.p2.tests.omniVersion;

import junit.framework.TestCase;
import org.eclipse.equinox.p2.metadata.Version;

/**
 * Tests format(r)
 *
 */
public class FormatRTest extends TestCase {

	public void testNumeric() {
		Version v = Version.parseVersion("format(r):1");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:1"), v);
	}

	public void testNegativeNumeric() {
		Version v = Version.parseVersion("format(r):-1");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:-1"), v);
	}

	public void testString() {
		Version v = Version.parseVersion("format(r):'a'");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:'a'"), v);

		assertNotNull(v = Version.parseVersion("format(r):\"a\""));
		assertEquals(Version.parseVersion("raw:'a'"), v);
	}

	public void testConcatentatedStrings() {
		Version v = Version.parseVersion("format(r):'a''b'");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:'ab'"), v);

		assertNotNull(v = Version.parseVersion("format(r):'a has a \"hat\" it is '\"a's\""));
		assertEquals(Version.parseVersion("raw:'a has a \"hat\" it is '\"a's\""), v);
	}

	public void testMaxString() {
		Version v = Version.parseVersion("format(r):m");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:m"), v);
	}

	public void testMaxNumeric() {
		Version v = Version.parseVersion("format(r):M");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:M"), v);
	}

	public void testArray() {
		Version v = Version.parseVersion("format(r):<1>");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:<1>"), v);
	}

	public void testNonRElements() {
		try {
			Version.parseVersion("format(r):aaa");
			fail("a is not a valid raw element");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format(r):1,2");
			fail("comma is not a delimiter in raw format");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}
}
