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
 * Tests format(q)
 * smart quoted string - matches a quoted alphanumeric string where the quote is determined by the first 
 * character of the string segment. The quote must be a non alphanumeric character, and the string
 * must be delimited by the same character except brackets and parenthesises (i.e. (), {}, [], <>) which are
 * handled as pairs, thus 'q' matches "<andrea-doria>" and produces a single string segment with the text 'andrea-doria'. 
 * A non-quoted sequence of characters are not matched by 'q'.
 * 
 */
public class FormatQTest extends TestCase {
	public void testQuoteFormatParsing() {
		Version aVer = Version.parseVersion("raw:'a'");
		assertNotNull(aVer);

		Version v = null;
		assertNotNull(v = Version.parseVersion("format(q):'a'"));
		assertEquals(aVer, v);
		assertNotNull(v = Version.parseVersion("format(q):\"a\""));
		assertEquals(aVer, v);
		assertNotNull(v = Version.parseVersion("format(q):=a="));
		assertEquals(aVer, v);
		assertNotNull(v = Version.parseVersion("format(q):#a#"));
		assertEquals(aVer, v);
		assertNotNull(v = Version.parseVersion("format(q):!a!"));
		assertEquals(aVer, v);
		assertNotNull(v = Version.parseVersion("format(q):|a|"));
		assertEquals(aVer, v);

	}

	public void testQUnbalancedQuoteR() {
		try {
			Version.parseVersion("format(q):'a");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testQUnbalancedQuoteL() {
		try {
			Version.parseVersion("format(q):a'");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testQuotedFormatPairsParsing() {
		Version aVer = Version.parseVersion("raw:'a'");
		assertNotNull(aVer);
		Version v = null;
		assertNotNull(v = Version.parseVersion("format(q):(a)"));
		assertEquals(aVer, v);
		assertNotNull(v = Version.parseVersion("format(q):<a>"));
		assertEquals(aVer, v);
		assertNotNull(v = Version.parseVersion("format(q):[a]"));
		assertEquals(aVer, v);
		assertNotNull(v = Version.parseVersion("format(q):{a}"));
		assertEquals(aVer, v);
	}

	public void testQUnbalancedPair1() {
		try {
			Version.parseVersion("format(q):(a");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format(q):a)");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format(q):(a(");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format(q):)a)");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testQUnbalancedPair2() {
		try {
			Version.parseVersion("format(q):[a");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format(q):a]");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format(q):[a[");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format(q):]a]");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testQUnbalancedPair3() {
		try {
			Version.parseVersion("format(q):<a");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format(q):a>");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format(q):<a<");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format(q):>a>");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testQUnbalancedPair4() {
		try {
			Version.parseVersion("format(q):{a");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format(q):a}");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format(q):{a{");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format(q):}a}");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testQUnbalancedPair5() {
		// not all erroneous permutations tested - only principle that open and close must be from "matching pair"
		// it should be enough to cover a faulty implementation
		try {
			Version.parseVersion("format(q):(a}");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format(q):[a}");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format(q):{a]");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format(q):<a)");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testExact() {
		Version v = Version.parseVersion("format(q={4};q):<123a>\"bc456'def'\"");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:'123a'.\"bc456'def'\""), v);

		assertNotNull(v = Version.parseVersion("format(q={2};q):<ab><c>"));
		assertEquals(Version.parseVersion("raw:'ab'.'c'"), v);

		assertNotNull(v = Version.parseVersion("format(q={2};S):'12'3"));
		assertEquals(Version.parseVersion("raw:'12'.'3'"), v);

		assertNotNull(v = Version.parseVersion("format(q={4};q={1};q={3};):<123a>'b'(c45)"));
		assertEquals(Version.parseVersion("raw:'123a'.'b'.'c45'"), v);

		assertNotNull(v = Version.parseVersion("format(q={2};.q={1};qq={3};):<12>.<3>'456'<abc>"));
		assertEquals(Version.parseVersion("raw:'12'.'3'.'456'.'abc'"), v);

		try {
			Version.parseVersion("format(q={4};.q):123.(abc456)'def'");
			fail("Uncaught error: quoted string is longer than 4");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format(q={4};q):<123>(abc456'def')");
			fail("Uncaught error: quoted string is shorter than 4");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testAtLeast() {
		Version v = Version.parseVersion("format(q={2,};):(123abc456'def')");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:\"123abc456'def'\""), v);

		assertNotNull(v = Version.parseVersion("format(q={2,};):<123abc456'def'>"));
		assertEquals(Version.parseVersion("raw:\"123abc456'def'\""), v);

		assertNotNull(v = Version.parseVersion("format(q={2,};.q):(123a).(bc456'def')"));
		assertEquals(Version.parseVersion("raw:'123a'.\"bc456'def'\""), v);

		assertNotNull(v = Version.parseVersion("format(q={2,};q):<123a>(bc456'def')"));
		assertEquals(Version.parseVersion("raw:'123a'.\"bc456'def'\""), v);

		try {
			Version.parseVersion("format(q={2,};.q):1.abc456'def'");
			fail("Uncaught error: first segment is shorter than 2");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format(q={2,};q):<1>(abc456'def')");
			fail("Uncaught error: firt segment is shorter than 2");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format(q={2,};.q={10,};):(12).(abc456'd')");
			fail("Uncaught error: last segment is shorter than 10");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format(q={2,};.q={10,};):<12>.abc456'd'");
			fail("Uncaught error: second segment is not quoted");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testAtMost() {
		// exact request
		Version v = Version.parseVersion("format(q={1,3};q={1,2};q):<123><ab><c456'def'>");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:'123'.'ab'.\"c456'def'\""), v);

		// request for more than available
		assertNotNull(v = Version.parseVersion("format(q={1,4};q={1,4};q):<123><abc4><56'def'>"));
		assertEquals(Version.parseVersion("raw:'123'.'abc4'.\"56'def'\""), v);

		try {
			// fails because first segment is shorter
			Version.parseVersion("format(q={2,3};q):<1><abc456'def'>");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			// fails because there are trailing characters after 'c'
			Version.parseVersion("format(q={2,3};q={2,3};):<12><abc456'd'>");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

}
