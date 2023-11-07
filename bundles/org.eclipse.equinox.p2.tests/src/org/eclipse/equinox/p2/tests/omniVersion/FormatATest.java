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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.equinox.p2.metadata.IVersionFormat;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionFormatException;
import org.junit.Test;

/**
 * Tests the format(a) rule.
 */
public class FormatATest {
	@Test
	public void testNumeric() {
		Version v = Version.parseVersion("format(a):1");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:1"), v);
	}

	@Test
	public void testString() {
		Version v = Version.parseVersion("format(a):a");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:'a'"), v);
	}

	@Test
	public void testSequenceOfAuto() {
		Version v = Version.parseVersion("format(aaaa):123abc456def");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:123.'abc'.456.'def'"), v);
	}

	/**
	 * Test that exact delimits a on count and type.
	 */
	@Test
	public void testExact() {
		Version v = Version.parseVersion("format(a={3};aaa={3};):123abc456def");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:123.'abc'.456.'def'"), v);

		assertNotNull(v = Version.parseVersion("format(a={2};a):abc"));
		assertEquals(Version.parseVersion("raw:'ab'.'c'"), v);

		assertNotNull(v = Version.parseVersion("format(a={2};a):123"));
		assertEquals(Version.parseVersion("raw:12.3"), v);

		// should fail because first segment is delimited after 2 chars
		assertThrows(IllegalArgumentException.class,
				() -> Version.parseVersion("format(a={4};aaa={3};):12.3abc456def"));
		// should fail because first segment is delimited by type change after 3 chars
		assertThrows(IllegalArgumentException.class, () -> Version.parseVersion("format(a={4};aaa={3};):123abc456def"));
		// should fail because first segment is delimited by type change after 3 chars
		assertThrows(IllegalArgumentException.class, () -> Version.parseVersion("format(a={4};aaa={3};):xyz123abc456"));
	}

	/**
	 * Test that unbound upper range delimits a on change of type.
	 */
	@Test
	public void testAtLeast() {
		Version v = Version.parseVersion("format(a={2,};aaa={2,};):123abc456def");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:123.'abc'.456.'def'"), v);

		assertThrows(IllegalArgumentException.class, () -> Version.parseVersion("format(a={2,};aaa={2,};):1abc456def"));
		assertThrows(IllegalArgumentException.class, () -> Version.parseVersion("format(a={2,};aaa={2,};):12abc456d"));
	}

	/**
	 * test that at-most follows same rules as exact for the min range
	 */
	@Test
	public void testAtMost() {
		Version v = Version.parseVersion("format(a={1,3};aaa={1,3};):123abc456def");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:123.'abc'.456.'def'"), v);

		// change of type is delimiter
		assertNotNull(v = Version.parseVersion("format(a={1,2};aaaa={1,2};a):123abc456def"));
		assertEquals(Version.parseVersion("raw:12.3.'abc'.456.'de'.'f'"), v);

		assertThrows(IllegalArgumentException.class,
				() -> Version.parseVersion("format(a={2,3};aaa={1,2};):1abc456def"));
		assertThrows(IllegalArgumentException.class,
				() -> Version.parseVersion("format(a={2,3};aaa={2,3};):12abc456d"));
		assertThrows(IllegalArgumentException.class,
				() -> Version.parseVersion("format(a={4,5};aaa={1,4};):123abc456def"));
	}

	@Test
	public void testEnum() {
		Version v1 = Version.parseVersion("format(aa={alpha,beta,gamma};a):12beta2");
		Version v2 = Version.parseVersion("raw:12.{alpha,^beta,gamma}.2");
		assertEquals(v1, v2);
	}

	@Test
	public void testEnumOptional() {
		// When enum is optional, test that parser falls back to string when enum isn't
		// matched
		Version v1 = Version.parseVersion("format(aa={alpha,beta,gamma}?;a):12foo2");
		Version v2 = Version.parseVersion("raw:12.'foo'.2");
		assertEquals(v1, v2);

		assertThrows("bad enum was not detected", IllegalArgumentException.class,
				() -> Version.parseVersion("format(aa={alpha,beta,gamma};a):12foo2"));

	}

	@Test
	public void testEnumIgnoreCase() {
		// When enum is optional, test that parser falls back to string when enum isn't
		// matched
		Version v1 = Version.parseVersion("format(aa={Alpha,Beta,Gamma}i;a):12beta2");
		Version v2 = Version.parseVersion("raw:12.{alpha,^beta,gamma}.2");
		assertEquals(v1, v2);

		v1 = Version.parseVersion("format(aa={alpha,beta,gamma}i;a):12BETA2");
		v2 = Version.parseVersion("raw:12.{alpha,^beta,gamma}.2");
		assertEquals(v1, v2);

		assertThrows("enum case sensitivity ignored", IllegalArgumentException.class,
				() -> Version.parseVersion("format(aa={alpha,beta,gamma};a):12BETA2"));

	}

	@Test
	public void testEnumBegins() {
		// When enum has 'begins', test that parser doesn't go too far
		Version v1 = Version.parseVersion("format(aa={alpha,beta,gamma}b;aa):12gammafoo2");
		Version v2 = Version.parseVersion("raw:12.{alpha,beta,^gamma}.'foo'.2");
		assertEquals(v1, v2);

		// When enum is optional, test that parser doesn't make attempts to resolve
		v1 = Version.parseVersion("format(aa={alpha,beta,gamma}?;a):12gammafoo2");
		v2 = Version.parseVersion("raw:12.'gammafoo'.2");
		assertEquals(v1, v2);

		// unless it's also a begins
		v1 = Version.parseVersion("format(aa={alpha,beta,gamma}b?;aa):12gammafoo2");
		v2 = Version.parseVersion("raw:12.{alpha,beta,^gamma}.'foo'.2");
		assertEquals(v1, v2);

		// if it's not optional nor begins, it should fail
		assertThrows(
				IllegalArgumentException.class,
				() -> Version.parseVersion("format(aa={alpha,beta,gamma};a):12gammafoo2"));

		// this one must fail too even though it's optional because
		// it falls back to string and '#' is not a legal string character
		assertThrows(IllegalArgumentException.class, () -> Version.parseVersion("format(aa={#,$,%}?;aa?):12#foo2"));

		// This however, should work
		v1 = Version.parseVersion("format(aa={#,$,%}b;aa):12#foo2");
		v2 = Version.parseVersion("raw:12.{^#,$,%}.'foo'.2");
		assertEquals(v1, v2);
	}

	@Test
	public void testEnumFormatToString() {
		Version v1 = Version.parseVersion("format(aa={alpha,beta,gamma};a):12beta2");
		assertEquals(v1.toString(), "raw:12.{alpha,^beta,gamma}.2/format(aa={alpha,beta,gamma};a):12beta2");
	}

	@Test
	public void testPHPVersion() {
		IVersionFormat phpFormat = null;
		try {
			phpFormat = Version.compile("n(d=[_+.-];?a={dev,alpha=a,beta=b,RC=rc,#,pl=p}?;)*");
		} catch (VersionFormatException e) {
			fail(e.getMessage());
		}
		Version v1 = phpFormat.parse("1.2.3");
		Version v2 = Version.parseVersion("raw:1.2.3");
		assertEquals(v1, v2);

		v1 = phpFormat.parse("1.2.alpha2");
		v2 = phpFormat.parse("1.2.a2");
		assertEquals(v1, v2);

		v1 = phpFormat.parse("1.2.beta3");
		v2 = phpFormat.parse("1.2.#2");
		assertTrue(v1.compareTo(v2) < 0);
		assertTrue(v2.compareTo(v1) > 0);

		v1 = Version.parseVersion("raw:1.2.{dev,a,b,RC,^#,p}.2");
		assertEquals(v1, v2);

		v1 = Version.parseVersion("raw:1.2.{dev,a,b,^rc,#,p}.2");
		assertTrue(v1.compareTo(v2) < 0);
		assertTrue(v2.compareTo(v1) > 0);

		v1 = Version.parseVersion("raw:1.2.{dev,a,b,rc,#,^p}.2");
		assertTrue(v1.compareTo(v2) > 0);
		assertTrue(v2.compareTo(v1) < 0);
	}
}
