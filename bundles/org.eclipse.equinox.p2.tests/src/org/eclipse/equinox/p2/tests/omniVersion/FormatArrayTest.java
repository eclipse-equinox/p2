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
 * Tests format(<>) - arrays.
 * 
 */
public class FormatArrayTest extends TestCase {
	public void testEmptyArray() {
		try {
			Version.parseVersion("format(<>q):''");
			fail("Uncaught error: empty array group is not allowed:");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testEmptyArrayBecauseContentIsOptional() {
		try {
			Version.parseVersion("format(<n?>q):''");
			fail("Uncaught error: produces an empty vector");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testOptionalArray() {
		Version v = Version.parseVersion("format(<n>?S):abc");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:'abc'"), v);

		assertNotNull(v = Version.parseVersion("format(<n>?S):1abc"));
		assertEquals(Version.parseVersion("raw:<1>.'abc'"), v);

	}

	public void testNumericArray() {
		Version v = Version.parseVersion("format(<(n.?)+>):1.2.3.4.5.6.7.8.9");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:<1.2.3.4.5.6.7.8.9>"), v);
	}

	public void testStringArray() {
		Version v = Version.parseVersion("format(<(S=[^.];d?)+>):a.b.c.d.e.f.g.h.i");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:<'a'.'b'.'c'.'d'.'e'.'f'.'g'.'h'.'i'>"), v);
	}

	public void testNestedArray() {
		Version v = Version.parseVersion("format(<n.<n.n>.n>):1.2.3.4");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:<1.<2.3>.4>"), v);

		assertNotNull(v = Version.parseVersion("format(<n.<n.<n>>.n>):1.2.3.4"));
		assertEquals(Version.parseVersion("raw:<1.<2.<3>>.4>"), v);
	}

}
