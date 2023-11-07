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
 * Tests format(n) and format(N)
 */
public class FormatNTest {
	@Test
	public void testNonNegative() {
		Version v = Version.parseVersion("format(n):1");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:1"), v);

		assertNotNull(v = Version.parseVersion("format(n):0"));
		assertEquals(Version.parseVersion("raw:0"), v);

		assertThrows("negative number in 'n' format", IllegalArgumentException.class,
				() -> Version.parseVersion("format(n):-1"));
	}

	@Test
	public void testNegativeValues() {
		Version v = Version.parseVersion("format(N):-1");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:-1"), v);

		assertNotNull(v = Version.parseVersion("format(N):1"));
		assertEquals(Version.parseVersion("raw:1"), v);

		assertNotNull(v = Version.parseVersion("format(N):0"));
		assertEquals(Version.parseVersion("raw:0"), v);
	}

	@Test
	public void testLeadingZeros() {
		Version v = Version.parseVersion("format(n):000001");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:1"), v);

		assertNotNull(v = Version.parseVersion("format(N):-000001"));
		assertEquals(Version.parseVersion("raw:-1"), v);
	}

	@Test
	public void testExact() {
		Version v = Version.parseVersion("format(n={2};n={2};):1122");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:11.22"), v);

		assertNotNull(v = Version.parseVersion("format(N={4};N={1};):-1234"));
		assertEquals(Version.parseVersion("raw:-123.4"), v);

		assertNotNull(v = Version.parseVersion("format(N={4};N={3};):-001234"));
		assertEquals(Version.parseVersion("raw:-1.234"), v);

		assertThrows(IllegalArgumentException.class, () -> Version.parseVersion("format(n={2};.;n={2};):1.2"));
		assertThrows(IllegalArgumentException.class, () -> Version.parseVersion("format(n={2};.;n={2};):111.2222"));
	}

	@Test
	public void testAtLeast() {
		Version v = Version.parseVersion("format(n={2,};.n={2,};):111.22222");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:111.22222"), v);
		assertThrows(IllegalArgumentException.class, () -> Version.parseVersion("format(n={2,};.;n={2};):111.2"));
	}

	@Test
	public void testAtMost() {
		Version v = Version.parseVersion("format(n={2,3};.n={2,3};):111.22");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:111.22"), v);
		assertThrows(IllegalArgumentException.class, () -> Version.parseVersion("format(n={2,3};.n={2,3};):111.2222"));
		assertThrows(IllegalArgumentException.class, () -> Version.parseVersion("format(n={2,3};.n={2,3};):1.222"));
	}

	@Test
	public void testNIsGreedy() {
		assertThrows(IllegalArgumentException.class, () -> Version.parseVersion("format(nn):1010"));
	}
}
