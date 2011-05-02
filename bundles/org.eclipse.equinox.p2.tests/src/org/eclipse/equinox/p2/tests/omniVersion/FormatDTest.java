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
 * Tests format(d) and explicit delimiter chars and strings.
 */
public class FormatDTest extends TestCase {
	/**
	 * Definition of default set of delimiters
	 */
	private static char[] s_delim = { //
	0x20, // ' '
			0x21, // ! 
			0x22, // #
			0x23, // "
			0x24, // '$'
			0x25, // '%'
			0x26, // '&'
			0x27, // '
			0x28, // '('
			0x29, // ')'
			0x2a, // '*'
			0x2b, // '+'
			0x2c, // 'ï¿½'
			0x2d, // '-'
			0x2e, //'.'
			0x2f, // '/'
			0x3a, // ':'
			0x3b, // ';'
			0x3c, // '<'
			0x3d, // '='
			0x3e, // '>' 
			0x3f, // '?'
			0x40, // @   <--- TODO: Debatable - is @ a delimiter of part of a string?
			0x5b, // [
			0x5c, // \
			0x5d, // }
			0x5e, // ^
			0x5f, // _
			0x7b, // {
			0x7c, // |
			0x7d, // }
			0x7e, // ~
	};

	public void testNumericWithDefaultSet() {
		Version v = null;
		String formatString = "format(ndn):";
		Integer one = Integer.valueOf(1);
		Integer two = Integer.valueOf(2);
		for (int i = 0; i < s_delim.length; i++) {
			StringBuffer buf = new StringBuffer();
			buf.append(formatString);
			buf.append("1");
			buf.append(s_delim[i]);
			buf.append("2");
			v = Version.parseVersion(buf.toString());
			assertNotNull(v);
			assertEquals(one, v.getSegment(0));
			assertEquals(two, v.getSegment(1));
		}
	}

	public void testStringWithDefaultSet() {
		Version v = null;
		String formatString = "format(sds):";
		String one = "abc";
		String two = "def";
		for (int i = 0; i < s_delim.length; i++) {
			StringBuffer buf = new StringBuffer();
			buf.append(formatString);
			buf.append(one);
			buf.append(s_delim[i]);
			buf.append(two);
			v = Version.parseVersion(buf.toString());
			assertNotNull(v);
			assertEquals(one, v.getSegment(0));
			assertEquals(two, v.getSegment(1));
		}
	}

	public void testAccepted() {
		Version v = Version.parseVersion("format((d=[A-Z];n){3}):A1B22C333");
		assertNotNull(v);
		assertEquals(Integer.valueOf(1), v.getSegment(0));
		assertEquals(Integer.valueOf(22), v.getSegment(1));
		assertEquals(Integer.valueOf(333), v.getSegment(2));

		assertNotNull(v = Version.parseVersion("format((d=[ABZ];S=[^ABZ];){3}):Aa1Bb22Zc333"));
		assertEquals("a1", v.getSegment(0));
		assertEquals("b22", v.getSegment(1));
		assertEquals("c333", v.getSegment(2));

		assertNotNull(v = Version.parseVersion("format((d=[A-Za-z];+n)+):Aa1Bb22Zc333"));
		assertEquals(Integer.valueOf(1), v.getSegment(0));
		assertEquals(Integer.valueOf(22), v.getSegment(1));
		assertEquals(Integer.valueOf(333), v.getSegment(2));

		// note that \\ below results in one \ because of java String parsing
		assertNotNull(v = Version.parseVersion("format((d=[\\\\[\\]];n)+):[1\\22]333"));
		assertEquals(Integer.valueOf(1), v.getSegment(0));
		assertEquals(Integer.valueOf(22), v.getSegment(1));
		assertEquals(Integer.valueOf(333), v.getSegment(2));
	}

	public void testRejected() {
		Version v = null;
		assertNotNull(v = Version.parseVersion("format((d=[^.:];S=[a-z0-9];){3}):/a1;b22=c333"));
		assertEquals("a1", v.getSegment(0));
		assertEquals("b22", v.getSegment(1));
		assertEquals("c333", v.getSegment(2));
	}

	public void testExplicit() {
		Version v = null;
		assertNotNull(v = Version.parseVersion("format('epoch='n';''major='n';''minor='n';'):epoch=1;major=22;minor=333;"));
		assertEquals(Integer.valueOf(1), v.getSegment(0));
		assertEquals(Integer.valueOf(22), v.getSegment(1));
		assertEquals(Integer.valueOf(333), v.getSegment(2));

		assertNotNull(v = Version.parseVersion("format('epoch='S=[^.];d'major='S=[^.];d'minor='S):epoch=a1ma.major=b22mi.minor=c333"));
		assertEquals("a1ma", v.getSegment(0));
		assertEquals("b22mi", v.getSegment(1));
		assertEquals("c333", v.getSegment(2));

	}

	public void testCounted() {
		// repeated d, char count d, and counted d are equal
		Version v1 = Version.parseVersion("format(dddn):///1");
		Version v2 = Version.parseVersion("format(d{3}n):///1");
		assertNotNull(v1);
		assertNotNull(v2);
		Integer one = Integer.valueOf(1);
		assertEquals(one, v1.getSegment(0));
		assertEquals(one, v2.getSegment(0));

	}

	public void testIllegalCharCount() {
		try {
			Version.parseVersion("format(d={3};n):///1");
			fail("Uncaught error: char count can not be used with 'd'");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}

	}

	public void testIllegalAsPad() {
		try {
			Version.parseVersion("format(nd=pm;n):1.0");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testIllegalWithDefault() {
		try {
			Version.parseVersion("format(nd='a';n):1.0");
			fail("Uncaught error: 'd' can not have a default value");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	/**
	 * Ignore of d is illegal as d is already ignored.
	 */
	public void testIllegalIgnore() {
		try {
			Version.parseVersion("format(nd=!;n):1.0");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}
}
