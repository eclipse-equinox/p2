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
import org.eclipse.equinox.p2.metadata.*;

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

	public void testEnum() {
		Version v1 = Version.parseVersion("format(aa={alpha,beta,gamma};a):12beta2");
		Version v2 = Version.parseVersion("raw:12.{alpha,^beta,gamma}.2");
		assertEquals(v1, v2);
	}

	public void testEnumOptional() {
		// When enum is optional, test that parser falls back to string when enum isn't matched
		Version v1 = Version.parseVersion("format(aa={alpha,beta,gamma}?;a):12foo2");
		Version v2 = Version.parseVersion("raw:12.'foo'.2");
		assertEquals(v1, v2);

		try {
			Version.parseVersion("format(aa={alpha,beta,gamma};a):12foo2");
			fail("bad enum was not detected");
		} catch (IllegalArgumentException e) {
			// Expected
		}

	}

	public void testEnumIgnoreCase() {
		// When enum is optional, test that parser falls back to string when enum isn't matched
		Version v1 = Version.parseVersion("format(aa={Alpha,Beta,Gamma}i;a):12beta2");
		Version v2 = Version.parseVersion("raw:12.{alpha,^beta,gamma}.2");
		assertEquals(v1, v2);

		v1 = Version.parseVersion("format(aa={alpha,beta,gamma}i;a):12BETA2");
		v2 = Version.parseVersion("raw:12.{alpha,^beta,gamma}.2");
		assertEquals(v1, v2);

		try {
			Version.parseVersion("format(aa={alpha,beta,gamma};a):12BETA2");
			fail("enum case sensitivity ignored");
		} catch (IllegalArgumentException e) {
			// Expected
		}

	}

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
		try {
			v1 = Version.parseVersion("format(aa={alpha,beta,gamma};a):12gammafoo2");
			fail("Enum begins pattern resolved although begins was not specified");
		} catch (IllegalArgumentException e) {
			// Expected
		}

		// this one must fail too even though it's optional because
		// it falls back to string and '#' is not a legal string character
		try {
			Version.parseVersion("format(aa={#,$,%}?;aa?):12#foo2");
			fail("Enum fallback to string with illegal string character");
		} catch (IllegalArgumentException e) {
			// Expected
		}

		// This however, should work
		v1 = Version.parseVersion("format(aa={#,$,%}b;aa):12#foo2");
		v2 = Version.parseVersion("raw:12.{^#,$,%}.'foo'.2");
		assertEquals(v1, v2);
	}

	public void testEnumFormatToString() {
		Version v1 = Version.parseVersion("format(aa={alpha,beta,gamma};a):12beta2");
		assertEquals(v1.toString(), "raw:12.{alpha,^beta,gamma}.2/format(aa={alpha,beta,gamma};a):12beta2");
	}

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
