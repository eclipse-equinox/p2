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
 * Tests processing rules not tested elsewhere, and combinations of processing rules.
 *
 */
public class FormatProcessingTest extends TestCase {

	public void testIgnore() {
		Version v = Version.parseVersion("format(n=!;.n.n):100.1.2");
		assertNotNull(v);
		assertEquals(Integer.valueOf(1), v.getSegment(0));
		assertEquals(Integer.valueOf(2), v.getSegment(1));
	}

	public void testDefaultArrayWithPad() {
		Version v = Version.parseVersion("format(s.?<n.n>=<1.0pm>;=p10;?):alpha");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:'alpha'.<1.0pm>"), v);

		assertNotNull(v = Version.parseVersion("format(s.?<n.n>=<1.0pm>;=p10;?):alpha.1.2"));
		assertEquals(Version.parseVersion("raw:'alpha'.<1.2p10>"), v);
	}

	public void testDefaultValues() {
		Version v = Version.parseVersion("format(n.[n=1;].?[s='foo';].?[a='bar';].?[a=2;]):9.");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:9.1.'foo'.'bar'.2"), v);
	}

	public void testArrayDefaultValues() {
		Version v = null;
		assertNotNull(v = Version.parseVersion("format(n.<n.n>=<1.0>;?):9."));
		assertEquals(Version.parseVersion("raw:9.<1.0>"), v);

		// array parses, so individual defaults are used
		assertNotNull(v = Version.parseVersion("format(n.<n=3;?.?n=4;?>=<1.0>;?):9."));
		assertEquals("individual defaults should be used", Version.parseVersion("raw:9.<3.4>"), v);

		// array does not parse, individual values are not used
		assertNotNull(v = Version.parseVersion("format(n.<n=3;?.n=4;?>=<1.0>;?):9."));
		assertEquals("individual defaults should not be used", Version.parseVersion("raw:9.<1.0>"), v);
	}

	public void testOtherTypeAsDefault() {
		Version v = null;
		assertNotNull(v = Version.parseVersion("format(s=123;?n):1"));
		assertEquals("#1.1", Version.parseVersion("raw:123.1"), v);

		assertNotNull(v = Version.parseVersion("format(s=M;?n):1"));
		assertEquals("#1.2", Version.parseVersion("raw:M.1"), v);

		assertNotNull(v = Version.parseVersion("format(s=-M;?n):1"));
		assertEquals("#1.3", Version.parseVersion("raw:-M.1"), v);

		assertNotNull(v = Version.parseVersion("format(s=<1.2>;?n):1"));
		assertEquals("#1.4", Version.parseVersion("raw:<1.2>.1"), v);

		assertNotNull(v = Version.parseVersion("format(n='abc';?s):a"));
		assertEquals("#2.1", Version.parseVersion("raw:'abc'.'a'"), v);

		assertNotNull(v = Version.parseVersion("format(n=M;?s):a"));
		assertEquals("#2.2", Version.parseVersion("raw:M.'a'"), v);

		assertNotNull(v = Version.parseVersion("format(n=-M;?s):a"));
		assertEquals("#2.3", Version.parseVersion("raw:-M.'a'"), v);

		assertNotNull(v = Version.parseVersion("format(n=<'a'.'b'>;?n):1"));
		assertEquals("#2.4", Version.parseVersion("raw:<'a'.'b'>.1"), v);

		assertNotNull(v = Version.parseVersion("format(<n>='abc';?s):a"));
		assertEquals("#3.1", Version.parseVersion("raw:'abc'.'a'"), v);

		assertNotNull(v = Version.parseVersion("format(<n>=M;?s):a"));
		assertEquals("#3.2", Version.parseVersion("raw:M.'a'"), v);

		assertNotNull(v = Version.parseVersion("format(<n>=-M;?s):a"));
		assertEquals("#3.3", Version.parseVersion("raw:-M.'a'"), v);

		assertNotNull(v = Version.parseVersion("format(<n>=123;?s):a"));
		assertEquals("#3.4", Version.parseVersion("raw:123.'a'"), v);

	}

	/**
	 * A processing rule can only be applied once to the preceding element.
	 * (These tests check if the same processing can be applied twice).
	 */
	public void testSameMoreThanOnce() {
		try {
			Version.parseVersion("format(n=!;=!;.n):1.2");
			fail("error not detected:2 x =!;");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format(s=[abc];=[123];.n):abc123.2");
			fail("error not detected:2 x =[];");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format(nd=[^:];=[^:];n):1.2");
			fail("error not detected:2x [^];");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format(n={1,3};={1,3};.n):1.2");
			fail("error not detected:2x ={ };");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format(n=0;=1;.n):1.2");
			fail("error not detected:2x =default value");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format((n.n)=pm;=pm;):1.2");
			fail("error not detected:2x =pm;");

		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	/**
	 * Tests that it is not allowed to have both =[]; and =[^] at the same time.
	 */
	public void testSetNotSet() {
		try {
			Version.parseVersion("format(nd=[a-z];=[^.:];n):1.2");
			fail("error not detected: =[];=[^];");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	/**
	 * Pad can only be combined with default value.
	 */
	public void testBadPadCombinations() {
		try {
			Version.parseVersion("format((n.n)=pm;=[abc];):1.2");
			fail("error not detected: =p; =[];");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format((n.n)=pm;=[^.:];):1.2");
			fail("error not detected: =p; =[];");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format((n.n)=pm;={1,3};):1.2");
			fail("error not detected: =p; ={};");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format((n.n)=pm;=!;):1.2");
			fail("error not detected: =p; =!;");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testNonPaddable() {
		try {
			Version.parseVersion("format(n=pm;):1");
			fail("error not detected: n=p;");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format(N=pm;):1");
			fail("error not detected: n=p;");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format(s=pm;):a");
			fail("error not detected: s=p;");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format(S=pm;):a");
			fail("error not detected: S=p;");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format(a=pm;):a");
			fail("error not detected: a=p;");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format(d=pm;):a");
			fail("error not detected: d=p;");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format(q=pm;):a");
			fail("error not detected: q=p;");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format(r=pm;):a");
			fail("error not detected: q=p;");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format('x'=pm;n):x1");
			fail("error not detected: 'x'=p;");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format(.=pm;n):x1");
			fail("error not detected: .=p;");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		try {
			Version.parseVersion("format(p=pm;n):x1");
			fail("error not detected: p=p;");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}

	}

}
