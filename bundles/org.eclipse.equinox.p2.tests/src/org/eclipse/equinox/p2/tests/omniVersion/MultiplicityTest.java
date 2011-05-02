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
 * Tests {n.m} in different combinations and the special +?*
 *
 */
public class MultiplicityTest extends TestCase {
	public void test01() {
		// n? == [n] == n{0,1} 
		Version v = Version.parseVersion("format(n?):1");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:1"), v);

		assertNotNull(v = Version.parseVersion("format(n[.n]):1"));
		assertEquals(Version.parseVersion("raw:1"), v);
		assertNotNull(v = Version.parseVersion("format(n.?n?):1"));
		assertEquals(Version.parseVersion("raw:1"), v);

		try {
			Version.parseVersion("format(n?):a");
			fail("Uncaught error: format(n?):a");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		// with []
		assertNotNull(v = Version.parseVersion("format([n]):1"));
		assertEquals(Version.parseVersion("raw:1"), v);

		assertNotNull(v = Version.parseVersion("format(s[n]):a"));
		assertEquals(Version.parseVersion("raw:'a'"), v);
		assertNotNull(v = Version.parseVersion("format(n[.][n]):1"));
		assertEquals(Version.parseVersion("raw:1"), v);

		try {
			Version.parseVersion("format([n]):a");
			fail("Uncaught error: format([n]):a");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}

		// with {0,1}
		assertNotNull(v = Version.parseVersion("format(n{0,1}):1"));
		assertEquals(Version.parseVersion("raw:1"), v);

		assertNotNull(v = Version.parseVersion("format(sn{0,1}):a"));
		assertEquals(Version.parseVersion("raw:'a'"), v);

		assertNotNull(v = Version.parseVersion("format(n.?n{0,}):1"));
		assertEquals(Version.parseVersion("raw:1"), v);

		try {
			Version.parseVersion("format(n{0,1}):a");
			fail("Uncaught error: format(n{0,1}):a");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void test1M() {
		// n+ == n{1,}
		Version v = Version.parseVersion("format((nd?)+):1.2.3");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:1.2.3"), v);

		assertNotNull(v = Version.parseVersion("format(n+):1"));
		assertEquals(Version.parseVersion("raw:1"), v);

		try {
			Version.parseVersion("format(n+):");
			fail("Uncaught error: format(n+):");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format(n+):a");
			fail("Uncaught error: format(n+):a");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		// with {1,}
		assertNotNull(v = Version.parseVersion("format((nd?){1,}):1.2.3"));
		assertEquals(Version.parseVersion("raw:1.2.3"), v);

		assertNotNull(v = Version.parseVersion("format(n{1,}):1"));
		assertEquals(Version.parseVersion("raw:1"), v);

		try {
			Version.parseVersion("format(n{1,}):");
			fail("Uncaught error: format(n{1,}):");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format(n{1,}):a");
			fail("Uncaught error: format(n{1,}):a");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}

	}

	public void test0M() {
		// n* == n{0,}
		Version v = Version.parseVersion("format((nd?)*):1.2.3");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:1.2.3"), v);

		assertNotNull(v = Version.parseVersion("format(n*):1"));
		assertEquals(Version.parseVersion("raw:1"), v);

		assertNotNull(v = Version.parseVersion("format(sn*):a"));
		assertEquals(Version.parseVersion("raw:'a'"), v);

		try {
			Version.parseVersion("format(n*):a");
			fail("Uncaught error: format(n*):a");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		// with {0,}
		assertNotNull(v = Version.parseVersion("format((nd?){0,}):1.2.3"));
		assertEquals(Version.parseVersion("raw:1.2.3"), v);

		assertNotNull(v = Version.parseVersion("format(n{0,}):1"));
		assertEquals(Version.parseVersion("raw:1"), v);

		assertNotNull(v = Version.parseVersion("format(sn{0,}):a"));
		assertEquals(Version.parseVersion("raw:'a'"), v);

		try {
			Version.parseVersion("format(n{0,}):a");
			fail("Uncaught error: format(n{0,}):a");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}

	}

	public void testExact() {
		// n{1}
		Version v = Version.parseVersion("format((nd?){3}):1.2.3");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:1.2.3"), v);

		assertNotNull(v = Version.parseVersion("format(n{1}):1"));
		assertEquals(Version.parseVersion("raw:1"), v);

		try {
			Version.parseVersion("format(n{1}):");
			fail("Uncaught error: format(n{1}):");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format((nd?){3}):1.2");
			fail("Uncaught error: format((nd?){3}):1.2");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format(n{1}):a");
			fail("Uncaught error: format(n{1}):a");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testAtLeast() {
		// n{>1,}
		Version v = null;
		assertNotNull(v = Version.parseVersion("format((nd?){2,}):1.2.3"));
		assertEquals(Version.parseVersion("raw:1.2.3"), v);
		assertNotNull(v = Version.parseVersion("format((nd?){3,}):1.2.3"));
		assertEquals(Version.parseVersion("raw:1.2.3"), v);

		assertNotNull(v = Version.parseVersion("format(n{1,}):1"));
		assertEquals(Version.parseVersion("raw:1"), v);

		try {
			Version.parseVersion("format(n{2,}):1");
			fail("Uncaught error: format(n{1,}):1");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format((nd?){3,}):1.2");
			fail("Uncaught error: format(n{3,1}):1.2");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}

	}

	public void testAtMost() {
		Version v = null;
		assertNotNull(v = Version.parseVersion("format((nd?){2,3}):1.2.3"));
		assertEquals(Version.parseVersion("raw:1.2.3"), v);

		assertNotNull(v = Version.parseVersion("format((nd?){2,3}):1.2"));
		assertEquals(Version.parseVersion("raw:1.2"), v);

		try {
			Version.parseVersion("format(n{2,3}):1");
			fail("Uncaught error: format(n{2,3}):1");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format(n{2,3}):1.2.3.4");
			fail("Uncaught error: format(n{2,3}):1.2.3.4");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testZeroExact() {
		// Should not have entered a n{0} as it is meaningless.
		try {
			Version.parseVersion("format(n{0}):");
			fail("Uncaught error: format(n{0}):");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format(n{0,0}):");
			fail("Uncaught error: format(n{0,0}):");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}

	}

	public void testMinGreaterThanMax() {
		try {
			Version.parseVersion("format((nd?){3,2}):1.2.3");
			fail("Uncaught error: format((nd?){3,2}):1.2.3");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testUnbalancedBraceR() {
		try {
			Version.parseVersion("format((nd?){3,2):1.2.3");
			fail("Uncaught error: format((nd?){3,2):1.2.3");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testNegativeRange() {
		try {
			Version.parseVersion("format((nd?){-1,2}):1.2.3");
			fail("Uncaught error: format((nd?){-1,2}):1.2.3");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format((nd?){1,-2}):1.2.3");
			fail("Uncaught error: format((nd?){1,-2}):1.2.3");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testStringRange() {
		try {
			Version.parseVersion("format((nd?){a,2}):1.2.3");
			fail("Uncaught error: format((nd?){a,2}):1.2.3");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format((nd?){1,a}):1.2.3");
			fail("Uncaught error: format((nd?){1,a}):1.2.3");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}
}
