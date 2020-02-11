/*******************************************************************************
 * Copyright (c) 2009, 2010 Cloudsmith Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.p2.tests.omniVersion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import org.eclipse.equinox.p2.metadata.Version;
import org.junit.Test;

/**
 * Tests format(r)
 *
 */
public class FormatRTest {
	@Test
	public void testNumeric() {
		Version v = Version.parseVersion("format(r):1");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:1"), v);
	}

	@Test
	public void testNegativeNumeric() {
		Version v = Version.parseVersion("format(r):-1");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:-1"), v);
	}

	@Test
	public void testString() {
		Version v = Version.parseVersion("format(r):'a'");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:'a'"), v);

		assertNotNull(v = Version.parseVersion("format(r):\"a\""));
		assertEquals(Version.parseVersion("raw:'a'"), v);
	}

	@Test
	public void testConcatentatedStrings() {
		Version v = Version.parseVersion("format(r):'a''b'");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:'ab'"), v);

		assertNotNull(v = Version.parseVersion("format(r):'a has a \"hat\" it is '\"a's\""));
		assertEquals(Version.parseVersion("raw:'a has a \"hat\" it is '\"a's\""), v);
	}

	@Test
	public void testMaxString() {
		Version v = Version.parseVersion("format(r):m");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:m"), v);
	}

	@Test
	public void testMaxNumeric() {
		Version v = Version.parseVersion("format(r):M");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:M"), v);
	}

	@Test
	public void testArray() {
		Version v = Version.parseVersion("format(r):<1>");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:<1>"), v);
	}

	@Test
	public void testNonRElements() {
		assertThrows("a is not a valid raw element", IllegalArgumentException.class,
				() -> Version.parseVersion("format(r):aaa"));
		assertThrows("comma is not a delimiter in raw format", IllegalArgumentException.class,
				() -> Version.parseVersion("format(r):1,2"));
	}
}
