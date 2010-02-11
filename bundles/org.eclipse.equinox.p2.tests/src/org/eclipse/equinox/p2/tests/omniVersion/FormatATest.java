/*******************************************************************************
 * Copyright (c) 2009 Cloudsmith Inc. and others.
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
 * Tests the format(a) rule.
 *
 */
public class FormatATest extends TestCase {
	public void testNumeric() {
		Version v = Version.parseVersion("format(a):1");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:1"), v);
	}

	public void testString() {
		Version v = Version.parseVersion("format(a):a");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:'a'"), v);
	}

	public void testSequenceOfAuto() {
		Version v = Version.parseVersion("format(aaaa):123abc456def");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:123.'abc'.456.'def'"), v);
	}

	/**
	 * Test that exact delimits a on count and type.
	 */
	public void testExact() {
		Version v = Version.parseVersion("format(a={3};aaa={3};):123abc456def");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:123.'abc'.456.'def'"), v);

		assertNotNull(v = Version.parseVersion("format(a={2};a):abc"));
		assertEquals(Version.parseVersion("raw:'ab'.'c'"), v);

		assertNotNull(v = Version.parseVersion("format(a={2};a):123"));
		assertEquals(Version.parseVersion("raw:12.3"), v);

		try {
			// should fail because first segment is delimited after 2 chars
			Version.parseVersion("format(a={4};aaa={3};):12.3abc456def");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			// should fail because first segment is delimited by type change after 3 chars
			Version.parseVersion("format(a={4};aaa={3};):123abc456def");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			// should fail because first segment is delimited by type change after 3 chars
			Version.parseVersion("format(a={4};aaa={3};):xyz123abc456");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}

	}

	/**
	 * Test that unbound upper range delimits a on change of type.
	 */
	public void testAtLeast() {
		Version v = Version.parseVersion("format(a={2,};aaa={2,};):123abc456def");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:123.'abc'.456.'def'"), v);

		try {
			Version.parseVersion("format(a={2,};aaa={2,};):1abc456def");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format(a={2,};aaa={2,};):12abc456d");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	/**
	 * test that at-most follows same rules as exact for the min range 
	 */
	public void testAtMost() {
		Version v = Version.parseVersion("format(a={1,3};aaa={1,3};):123abc456def");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:123.'abc'.456.'def'"), v);

		// change of type is delimiter 
		assertNotNull(v = Version.parseVersion("format(a={1,2};aaaa={1,2};a):123abc456def"));
		assertEquals(Version.parseVersion("raw:12.3.'abc'.456.'de'.'f'"), v);

		try {
			Version.parseVersion("format(a={2,3};aaa={1,2};):1abc456def");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format(a={2,3};aaa={2,3};):12abc456d");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format(a={4,5};aaa={1,4};):123abc456def");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}
}
