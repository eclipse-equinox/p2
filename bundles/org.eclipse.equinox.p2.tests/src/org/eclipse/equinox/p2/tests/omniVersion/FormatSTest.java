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
 * Tests format(s), and format(S)
 *
 * a string group matching any character except any following explicit/optional
 * delimiter. Use processing rules =[]; or =[^] to define the set of allowed
 * characters.
 */
public class FormatSTest {
	@Test
	public void testStringAcceptDigit() {
		Version v = Version.parseVersion("format(S):1");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:'1'"), v);
		assertThrows("Uncaught error: s should not accept digits", IllegalArgumentException.class,
				() -> Version.parseVersion("format(s):1"));
	}

	@Test
	public void testStringAcceptAlpha() {
		Version v = Version.parseVersion("format(s):a");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:'a'"), v);

		assertNotNull(v = Version.parseVersion("format(S):a"));
		assertEquals(Version.parseVersion("raw:'a'"), v);
	}

	@Test
	public void testStringDelimitedByNumeric() {
		Version v = Version.parseVersion("format(sn):foobar123");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:'foobar'.123"), v);
		assertThrows("Uncaught error: S should eat entire string, no n found at the end",
				IllegalArgumentException.class, () -> Version.parseVersion("format(Sn):foobar123"));
	}

	@Test
	public void testStringWithSpace() {
		Version v;

		assertNotNull(v = Version.parseVersion("format(S=[^0-9];n):foo bar123"));
		assertEquals(Version.parseVersion("raw:'foo bar'.123"), v);

		// Test 's' with attempt to include 'space' and delimiters
		assertThrows("Uncaught error: format(s) can not match non letters (space).", IllegalArgumentException.class,
				() -> Version.parseVersion("format(s=[^];n):foo bar123"));
	}

	@Test
	public void testStringDelimitedByDelimiter() {
		Version v = Version.parseVersion("format(s.n):foobar.123");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:'foobar'.123"), v);

		assertNotNull(v = Version.parseVersion("format(S=[^.];.n):foobar.123"));
		assertEquals(Version.parseVersion("raw:'foobar'.123"), v);
	}

	@Test
	public void testStringDelimitedByExplicitDelimiter() {
		Version v = Version.parseVersion("format(s=[^r];d=[r];n):foobar123");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:'fooba'.123"), v);
	}

	@Test
	public void testStringWithAllowedSet() {
		Version v = Version.parseVersion("format(s=[a-z];sn):fooBAR123");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:'foo'.'BAR'.123"), v);
	}

	@Test
	public void testStringWithDisallowedSet() {
		Version v = Version.parseVersion("format(s=[^a-z];sn):FOObar123");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:'FOO'.'bar'.123"), v);
	}

	@Test
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

		assertThrows("Uncaught error: first segment is less than 4 chars long", IllegalArgumentException.class,
				() -> Version.parseVersion("format(s={4};.s):aaa.abc456'def'"));
		assertThrows("Uncaught error: first segment has digits", IllegalArgumentException.class,
				() -> Version.parseVersion("format(s={4};.s):123.abc456'def'"));
		assertThrows("Uncaught error: first segment has only 3 characters", IllegalArgumentException.class,
				() -> Version.parseVersion("format(S={4}=[^.];.S):123.abc456'def'"));

	}

	/**
	 * Test that unbound upper range is just a limit on lower range. Upper delimiter
	 * must be a delimiter.
	 */
	@Test
	public void testAtLeast() {
		Version v = Version.parseVersion("format(S={2,};):123abc456'def'");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:\"123abc456'def'\""), v);

		assertNotNull(v = Version.parseVersion("format(S={2,};=[^.];.S):123a.bc456'def'"));
		assertEquals(Version.parseVersion("raw:'123a'.\"bc456'def'\""), v);

		assertThrows("Uncaught error: first segment is shorter than 2", IllegalArgumentException.class,
				() -> Version.parseVersion("format(s={2,};.S):a.abc456'def'"));
		assertThrows("Uncaught error: second segment too short", IllegalArgumentException.class,
				() -> Version.parseVersion("format(s={2,};.s={10,};):aa.abcd"));
	}

	@Test
	public void testAtMost() {
		Version v = Version.parseVersion("format(S={1,3};S={1,2};S):123abc456'def'");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:'123'.'ab'.\"c456'def'\""), v);

		// delimited by delimiter
		assertNotNull(v = Version.parseVersion("format(S={1,4};=[^.];.S={1,4};.S):123.abc4.56'def'"));
		assertEquals(Version.parseVersion("raw:'123'.'abc4'.\"56'def'\""), v);

		// fails because of delimiter after one char
		assertThrows(IllegalArgumentException.class, () -> Version.parseVersion("format(s={2,3};s):a.abc456'def'"));
		// fails because there are trailing characters after 'c'
		assertThrows(IllegalArgumentException.class,
				() -> Version.parseVersion("format(s={2,3};.S={2,3};):aa.abc456'd'"));
	}
}
