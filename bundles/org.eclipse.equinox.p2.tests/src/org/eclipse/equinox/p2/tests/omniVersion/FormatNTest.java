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
 * Tests format(n) and format(N)
 *
 */
public class FormatNTest extends TestCase {
	public void testNonNegative() {
		Version v = Version.parseVersion("format(n):1");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:1"), v);

		assertNotNull(v = Version.parseVersion("format(n):0"));
		assertEquals(Version.parseVersion("raw:0"), v);

		try {
			Version.parseVersion("format(n):-1");
			fail("Uncaught exception: negative number in 'n' format");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testNegativeValues() {
		Version v = Version.parseVersion("format(N):-1");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:-1"), v);

		assertNotNull(v = Version.parseVersion("format(N):1"));
		assertEquals(Version.parseVersion("raw:1"), v);

		assertNotNull(v = Version.parseVersion("format(N):0"));
		assertEquals(Version.parseVersion("raw:0"), v);
	}

	public void testLeadingZeros() {
		Version v = Version.parseVersion("format(n):000001");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:1"), v);

		assertNotNull(v = Version.parseVersion("format(N):-000001"));
		assertEquals(Version.parseVersion("raw:-1"), v);
	}

	public void testExact() {
		Version v = Version.parseVersion("format(n={2};n={2};):1122");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:11.22"), v);

		assertNotNull(v = Version.parseVersion("format(N={4};N={1};):-1234"));
		assertEquals(Version.parseVersion("raw:-123.4"), v);

		assertNotNull(v = Version.parseVersion("format(N={4};N={3};):-001234"));
		assertEquals(Version.parseVersion("raw:-1.234"), v);

		try {
			v = Version.parseVersion("format(n={2};.;n={2};):1.2");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			v = Version.parseVersion("format(n={2};.;n={2};):111.2222");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testAtLeast() {
		Version v = Version.parseVersion("format(n={2,};.n={2,};):111.22222");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:111.22222"), v);
		try {
			v = Version.parseVersion("format(n={2,};.;n={2};):111.2");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testAtMost() {
		Version v = Version.parseVersion("format(n={2,3};.n={2,3};):111.22");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:111.22"), v);
		try {
			v = Version.parseVersion("format(n={2,3};.n={2,3};):111.2222");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			v = Version.parseVersion("format(n={2,3};.n={2,3};):1.222");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testNIsGreedy() {
		try {
			Version.parseVersion("format(nn):1010");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}
}
