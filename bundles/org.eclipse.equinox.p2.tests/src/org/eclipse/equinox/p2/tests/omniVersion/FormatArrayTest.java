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
 * Tests format(<>) - arrays.
 */
public class FormatArrayTest {
	@Test
	public void testEmptyArray() {
		assertThrows("Uncaught error: empty array group is not allowed:", IllegalArgumentException.class,
				() -> Version.parseVersion("format(<>q):''"));
	}

	@Test
	public void testEmptyArrayBecauseContentIsOptional() {
		assertThrows("Uncaught error: produces an empty vector",
				IllegalArgumentException.class,
				() -> Version.parseVersion("format(<n?>q):''"));
	}

	@Test
	public void testOptionalArray() {
		Version v = Version.parseVersion("format(<n>?S):abc");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:'abc'"), v);

		assertNotNull(v = Version.parseVersion("format(<n>?S):1abc"));
		assertEquals(Version.parseVersion("raw:<1>.'abc'"), v);

	}

	@Test
	public void testNumericArray() {
		Version v = Version.parseVersion("format(<(n.?)+>):1.2.3.4.5.6.7.8.9");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:<1.2.3.4.5.6.7.8.9>"), v);
	}

	@Test
	public void testStringArray() {
		Version v = Version.parseVersion("format(<(S=[^.];d?)+>):a.b.c.d.e.f.g.h.i");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:<'a'.'b'.'c'.'d'.'e'.'f'.'g'.'h'.'i'>"), v);
	}

	@Test
	public void testNestedArray() {
		Version v = Version.parseVersion("format(<n.<n.n>.n>):1.2.3.4");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:<1.<2.3>.4>"), v);

		assertNotNull(v = Version.parseVersion("format(<n.<n.<n>>.n>):1.2.3.4"));
		assertEquals(Version.parseVersion("raw:<1.<2.<3>>.4>"), v);
	}

}
