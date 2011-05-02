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
 * Tests format(s), and format(S)
 * 
 * a string group matching any character except any following 
 * explicit/optional delimiter. Use processing rules =[]; or =[^] to define the set of allowed characters.
 * 
 */
public class FormatSTest extends TestCase {
	public void testStringAcceptDigit() {
		Version v = Version.parseVersion("format(S):1");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:'1'"), v);
		try {
			Version.parseVersion("format(s):1");
			fail("Uncaught error: s should not accept digits");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testStringAcceptAlpha() {
		Version v = Version.parseVersion("format(s):a");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:'a'"), v);

		assertNotNull(v = Version.parseVersion("format(S):a"));
		assertEquals(Version.parseVersion("raw:'a'"), v);
	}

	public void testStringDelimitedByNumeric() {
		Version v = Version.parseVersion("format(sn):foobar123");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:'foobar'.123"), v);
		try {
			Version.parseVersion("format(Sn):foobar123");
			fail("Uncaught error: S should eat entire string, no n found at the end");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testStringWithSpace() {
		Version v;

		assertNotNull(v = Version.parseVersion("format(S=[^0-9];n):foo bar123"));
		assertEquals(Version.parseVersion("raw:'foo bar'.123"), v);

		// Test 's' with attempt to include 'space' and delimiters
		//
		try {
			Version.parseVersion("format(s=[^];n):foo bar123");
			fail("Uncaught error: format(s) can not match non letters (space).");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testStringDelimitedByDelimiter() {
		Version v = Version.parseVersion("format(s.n):foobar.123");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:'foobar'.123"), v);

		assertNotNull(v = Version.parseVersion("format(S=[^.];.n):foobar.123"));
		assertEquals(Version.parseVersion("raw:'foobar'.123"), v);
	}

	public void testStringDelimitedByExplicitDelimiter() {
		Version v = Version.parseVersion("format(s=[^r];d=[r];n):foobar123");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:'fooba'.123"), v);
	}

	public void testStringWithAllowedSet() {
		Version v = Version.parseVersion("format(s=[a-z];sn):fooBAR123");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:'foo'.'BAR'.123"), v);
	}

	public void testStringWithDisallowedSet() {
		Version v = Version.parseVersion("format(s=[^a-z];sn):FOObar123");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:'FOO'.'bar'.123"), v);
	}

	public void testExact() {
		Version v = Version.parseVersion("format(S={4};S):123abc456'def'");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:'123a'.\"bc456'def'\""), v);

		assertNotNull(v = Version.parseVersion("format(s={2};s):abc"));
		assertEquals(Version.parseVersion("raw:'ab'.'c'"), v);

		assertNotNull(v = Version.parseVersion("format(S={2};S):abc"));
		assertEquals(Version.parseVersion("raw:'ab'.'c'"), v);

		assertNotNull(v = Version.parseVersion("format(S={2};S):123"));
		assertEquals(Version.parseVersion("raw:'12'.'3'"), v);

		assertNotNull(v = Version.parseVersion("format(S={4};S={1};S={3};):123abc45"));
		assertEquals(Version.parseVersion("raw:'123a'.'b'.'c45'"), v);

		assertNotNull(v = Version.parseVersion("format(S={2};.S={1};s={3};):12.3abc"));
		assertEquals(Version.parseVersion("raw:'12'.'3'.'abc'"), v);

		try {
			Version.parseVersion("format(s={4};.s):aaa.abc456'def'");
			fail("Uncaught error: first segment is less than 4 chars long");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format(s={4};.s):123.abc456'def'");
			fail("Uncaught error: first segment has digits");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format(S={4}=[^.];.S):123.abc456'def'");
			fail("Uncaught error: first segment has only 3 characters");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}

	}

	/**
	 * Test that unbound upper range is just a limit on lower range. Upper delimiter must be a delimiter.
	 */
	public void testAtLeast() {
		Version v = Version.parseVersion("format(S={2,};):123abc456'def'");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:\"123abc456'def'\""), v);

		assertNotNull(v = Version.parseVersion("format(S={2,};=[^.];.S):123a.bc456'def'"));
		assertEquals(Version.parseVersion("raw:'123a'.\"bc456'def'\""), v);

		try {
			Version.parseVersion("format(s={2,};.S):a.abc456'def'");
			fail("uncaught error: first segment is shorter than 2");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format(s={2,};.s={10,};):aa.abcd");
			fail("Uncaught error: secont segment too short");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testAtMost() {
		Version v = Version.parseVersion("format(S={1,3};S={1,2};S):123abc456'def'");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:'123'.'ab'.\"c456'def'\""), v);

		// delimited by delimiter
		assertNotNull(v = Version.parseVersion("format(S={1,4};=[^.];.S={1,4};.S):123.abc4.56'def'"));
		assertEquals(Version.parseVersion("raw:'123'.'abc4'.\"56'def'\""), v);

		try {
			// fails because of delimiter after one char
			Version.parseVersion("format(s={2,3};s):a.abc456'def'");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			// fails because there are trailing characters after 'c'
			Version.parseVersion("format(s={2,3};.S={2,3};):aa.abc456'd'");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}
}
