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
 * Tests {n.m} in different combinations and the special +?*
 */
public class MultiplicityTest {
	@Test
	public void test01() {
		// n? == [n] == n{0,1}
		Version v = Version.parseVersion("format(n?):1");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:1"), v);

		assertNotNull(v = Version.parseVersion("format(n[.n]):1"));
		assertEquals(Version.parseVersion("raw:1"), v);
		assertNotNull(v = Version.parseVersion("format(n.?n?):1"));
		assertEquals(Version.parseVersion("raw:1"), v);

		assertThrows("Uncaught error: format(n?):a", IllegalArgumentException.class,
				() -> Version.parseVersion("format(n?):a"));
		// with []
		assertNotNull(v = Version.parseVersion("format([n]):1"));
		assertEquals(Version.parseVersion("raw:1"), v);

		assertNotNull(v = Version.parseVersion("format(s[n]):a"));
		assertEquals(Version.parseVersion("raw:'a'"), v);
		assertNotNull(v = Version.parseVersion("format(n[.][n]):1"));
		assertEquals(Version.parseVersion("raw:1"), v);

		assertThrows("Uncaught error: format([n]):a", IllegalArgumentException.class,
				() -> Version.parseVersion("format([n]):a"));

		// with {0,1}
		assertNotNull(v = Version.parseVersion("format(n{0,1}):1"));
		assertEquals(Version.parseVersion("raw:1"), v);

		assertNotNull(v = Version.parseVersion("format(sn{0,1}):a"));
		assertEquals(Version.parseVersion("raw:'a'"), v);

		assertNotNull(v = Version.parseVersion("format(n.?n{0,}):1"));
		assertEquals(Version.parseVersion("raw:1"), v);

		assertThrows("Uncaught error: format(n{0,1}):a", IllegalArgumentException.class,
				() -> Version.parseVersion("format(n{0,1}):a"));
	}

	@Test
	public void test1M() {
		// n+ == n{1,}
		Version v = Version.parseVersion("format((nd?)+):1.2.3");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:1.2.3"), v);

		assertNotNull(v = Version.parseVersion("format(n+):1"));
		assertEquals(Version.parseVersion("raw:1"), v);

		assertThrows("Uncaught error: format(n+):", IllegalArgumentException.class,
				() -> Version.parseVersion("format(n+):"));
		assertThrows("Uncaught error: format(n+):a", IllegalArgumentException.class,
				() -> Version.parseVersion("format(n+):a"));
		// with {1,}
		assertNotNull(v = Version.parseVersion("format((nd?){1,}):1.2.3"));
		assertEquals(Version.parseVersion("raw:1.2.3"), v);

		assertNotNull(v = Version.parseVersion("format(n{1,}):1"));
		assertEquals(Version.parseVersion("raw:1"), v);

		assertThrows("Uncaught error: format(n{1,}):", IllegalArgumentException.class,
				() -> Version.parseVersion("format(n{1,}):"));
		assertThrows("Uncaught error: format(n{1,}):a", IllegalArgumentException.class,
				() -> Version.parseVersion("format(n{1,}):a"));

	}

	@Test
	public void test0M() {
		// n* == n{0,}
		Version v = Version.parseVersion("format((nd?)*):1.2.3");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:1.2.3"), v);

		assertNotNull(v = Version.parseVersion("format(n*):1"));
		assertEquals(Version.parseVersion("raw:1"), v);

		assertNotNull(v = Version.parseVersion("format(sn*):a"));
		assertEquals(Version.parseVersion("raw:'a'"), v);

		assertThrows("Uncaught error: format(n*):a", IllegalArgumentException.class,
				() -> Version.parseVersion("format(n*):a"));
		// with {0,}
		assertNotNull(v = Version.parseVersion("format((nd?){0,}):1.2.3"));
		assertEquals(Version.parseVersion("raw:1.2.3"), v);

		assertNotNull(v = Version.parseVersion("format(n{0,}):1"));
		assertEquals(Version.parseVersion("raw:1"), v);

		assertNotNull(v = Version.parseVersion("format(sn{0,}):a"));
		assertEquals(Version.parseVersion("raw:'a'"), v);

		assertThrows("Uncaught error: format(n{0,}):a", IllegalArgumentException.class,
				() -> Version.parseVersion("format(n{0,}):a"));

	}

	@Test
	public void testExact() {
		// n{1}
		Version v = Version.parseVersion("format((nd?){3}):1.2.3");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:1.2.3"), v);

		assertNotNull(v = Version.parseVersion("format(n{1}):1"));
		assertEquals(Version.parseVersion("raw:1"), v);

		assertThrows("Uncaught error: format(n{1}):", IllegalArgumentException.class,
				() -> Version.parseVersion("format(n{1}):"));
		assertThrows("Uncaught error: format((nd?){3}):1.2", IllegalArgumentException.class,
				() -> Version.parseVersion("format((nd?){3}):1.2"));
		assertThrows("Uncaught error: format(n{1}):a", IllegalArgumentException.class,
				() -> Version.parseVersion("format(n{1}):a"));
	}

	@Test
	public void testAtLeast() {
		// n{>1,}
		Version v = null;
		assertNotNull(v = Version.parseVersion("format((nd?){2,}):1.2.3"));
		assertEquals(Version.parseVersion("raw:1.2.3"), v);
		assertNotNull(v = Version.parseVersion("format((nd?){3,}):1.2.3"));
		assertEquals(Version.parseVersion("raw:1.2.3"), v);

		assertNotNull(v = Version.parseVersion("format(n{1,}):1"));
		assertEquals(Version.parseVersion("raw:1"), v);

		assertThrows("Uncaught error: format(n{1,}):1", IllegalArgumentException.class,
				() -> Version.parseVersion("format(n{2,}):1"));
		assertThrows("Uncaught error: format(n{3,1}):1.2", IllegalArgumentException.class,
				() -> Version.parseVersion("format((nd?){3,}):1.2"));

	}

	@Test
	public void testAtMost() {
		Version v = null;
		assertNotNull(v = Version.parseVersion("format((nd?){2,3}):1.2.3"));
		assertEquals(Version.parseVersion("raw:1.2.3"), v);

		assertNotNull(v = Version.parseVersion("format((nd?){2,3}):1.2"));
		assertEquals(Version.parseVersion("raw:1.2"), v);

		assertThrows("Uncaught error: format(n{2,3}):1", IllegalArgumentException.class,
				() -> Version.parseVersion("format(n{2,3}):1"));
		assertThrows("Uncaught error: format(n{2,3}):1.2.3.4", IllegalArgumentException.class,
				() -> Version.parseVersion("format(n{2,3}):1.2.3.4"));
	}

	@Test
	public void testZeroExact() {
		// Should not have entered a n{0} as it is meaningless.
		assertThrows("Uncaught error: format(n{0}):", IllegalArgumentException.class,
				() -> Version.parseVersion("format(n{0}):"));
		assertThrows("Uncaught error: fformat(n{0,0}):", IllegalArgumentException.class,
				() -> Version.parseVersion("format(n{0,0}):"));

	}

	@Test
	public void testMinGreaterThanMax() {
		assertThrows("Uncaught error: format((nd?){3,2}):1.2.3", IllegalArgumentException.class,
				() -> Version.parseVersion("format((nd?){3,2}):1.2.3"));
	}

	@Test
	public void testUnbalancedBraceR() {
		assertThrows("Uncaught error: format((nd?){3,2):1.2.3",
				IllegalArgumentException.class,
				() -> Version.parseVersion("format((nd?){3,2):1.2.3"));
	}

	@Test
	public void testNegativeRange() {
		assertThrows("Uncaught error: format((nd?){-1,2}):1.2.3", IllegalArgumentException.class,
				() -> Version.parseVersion("format((nd?){-1,2}):1.2.3"));
		assertThrows("Uncaught error: format((nd?){1,-2}):1.2.3", IllegalArgumentException.class,
				() -> Version.parseVersion("format((nd?){1,-2}):1.2.3"));
	}

	@Test
	public void testStringRange() {
		assertThrows("Uncaught error: format((nd?){a,2}):1.2.3", IllegalArgumentException.class,
				() -> Version.parseVersion("format((nd?){a,2}):1.2.3"));
		assertThrows("Uncaught error: format((nd?){1,a}):1.2.3", IllegalArgumentException.class,
				() -> Version.parseVersion("format((nd?){1,a}):1.2.3"));
	}
}
