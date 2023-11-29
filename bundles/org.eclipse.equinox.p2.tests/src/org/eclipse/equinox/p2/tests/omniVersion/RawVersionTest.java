/*******************************************************************************
 * Copyright (c) 2009, 2016 Cloudsmith Inc. and others.
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

import org.eclipse.equinox.internal.p2.metadata.VersionVector;
import org.eclipse.equinox.p2.metadata.Version;
import org.junit.Test;

/**
 * Tests the OmniVersion raw version format.
 */
public class RawVersionTest extends VersionTesting {

	@Test
	public void testBasicParsing() {

		// should parse without exception
		assertNotNull(Version.create("raw:1"));
		assertNotNull(Version.create("raw:1.0"));
		assertNotNull(Version.create("raw:1.0.0"));
		assertNotNull(Version.create("raw:1.0.0.9"));
		assertNotNull(Version.create("raw:1.0.0.'r12345'"));
		assertNotNull(Version.create("raw:1.0.0.'r12345.hello'"));

		assertNotNull(Version.create("raw:1.0.m"));
		assertNotNull(Version.create("raw:1.0.M"));

		assertNotNull(Version.create("raw:1.0.M"));
		assertNotNull(Version.create("raw:1.0.-M"));

	}

	@Test
	public void testEnumParsing() {
		assertThrows("Parsing succeeded but enum had no ordinal indicator", IllegalArgumentException.class,
				() -> Version.create("raw:{blue,green,yellow}"));
		Version.create("raw:{blue,^green,yellow}");
	}

	@Test
	public void testEnumCompare() {
		Version v1 = Version.create("raw:{blue,^green,yellow}");
		Version v2 = Version.create("raw:{^blue,green,yellow}");
		assertTrue(v1.compareTo(v2) > 0);
		assertTrue(v2.compareTo(v1) < 0);

		v1 = Version.create("raw:{blue,green,^yellow}");
		v2 = Version.create("raw:{blue,green,^yellow}");
		assertEquals(v1, v2);

		v1 = Version.create("raw:{blue,^green}");
		v2 = Version.create("raw:{^blue,green,yellow}");
		assertTrue(v1.compareTo(v2) > 0);
		assertTrue(v2.compareTo(v1) < 0);

		v1 = Version.create("raw:{blue,^green,yelllow}");
		v2 = Version.create("raw:{green,^yellow}");
		assertTrue(v1.compareTo(v2) < 0);
		assertTrue(v2.compareTo(v1) > 0);

		// v1 knows that blue is less than yellow.
		// v2 will not have a different opinion
		v1 = Version.create("raw:{^blue,green,yellow}");
		v2 = Version.create("raw:{green,^yellow}");
		assertTrue(v1.compareTo(v2) < 0);
		assertTrue(v2.compareTo(v1) > 0);

		// v1 knows that blue is less than yellow.
		// v2 knows that yellow is less than blue
		// Conflict! so v1 wins on green > blue
		v1 = Version.create("raw:{green,^yellow,blue}");
		v2 = Version.create("raw:{^blue,green,yellow}");
		assertTrue(v1.compareTo(v2) > 0);
		assertTrue(v2.compareTo(v1) < 0);

		// Conflict again, but this time v2 wins since it
		// has more elements
		v1 = Version.create("raw:{green,^yellow,blue}");
		v2 = Version.create("raw:{^blue,green,yellow,purple}");
		assertTrue(v1.compareTo(v2) < 0);
		assertTrue(v2.compareTo(v1) > 0);
	}

	@Test
	public void testEnumCompareWithOther() {
		Version v1 = Version.create("raw:{blue,^green,yellow}");
		Version v2 = Version.create("raw:'green'");

		// Enum is always greater than String
		assertTrue(v1.compareTo(v2) > 0);
		assertTrue(v2.compareTo(v1) < 0);

		// Enum is always greater than String
		v2 = Version.create("raw:m");
		assertTrue(v1.compareTo(v2) > 0);
		assertTrue(v2.compareTo(v1) < 0);

		// Enum is always greater than MIN
		v2 = Version.create("raw:-M");
		assertTrue(v1.compareTo(v2) > 0);
		assertTrue(v2.compareTo(v1) < 0);

		// Enum is always less than Integer
		v2 = Version.create("raw:0");
		assertTrue(v1.compareTo(v2) < 0);
		assertTrue(v2.compareTo(v1) > 0);

		// Enum is always less than a vector
		v2 = Version.create("raw:<'foo'>");
		assertTrue(v1.compareTo(v2) < 0);
		assertTrue(v2.compareTo(v1) > 0);

		// Enum is always less than MAX
		v2 = Version.create("raw:M");
		assertTrue(v1.compareTo(v2) < 0);
		assertTrue(v2.compareTo(v1) > 0);
	}

	@Test
	public void testSerialize() {
		Version v = null;
		// should parse without exception
		assertNotNull(v = Version.create("raw:1"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:1.0"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:1.0.0"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:1.0.0.9"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:1.0.0.'r12345'"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:1.0.0.'r12345.hello'"));
		assertSerialized(v);

		assertNotNull(v = Version.create("raw:1.0.m"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:1.0.M"));
		assertSerialized(v);

		assertNotNull(v = Version.create("raw:1.0.M"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:1.0.-M"));
		assertSerialized(v);

		assertNotNull(v = Version.create("raw:0"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:0.1.2.3.4.5.6.7.8.9"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:0.-1.-2.-3.-4.-5.-6.-7.-8.-9"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:123456789.-1234567890"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:123456789.-1234567890"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:m"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:M"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:-M"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:1.m"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:1.M"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:1.-M"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:'a'"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:\"a\""));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:'ab'"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:'abcdefghijklmnopqrstuvwxyz0123456789'"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:'-_!\"#$%&/()=?+*;,:.'"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:\"'\""));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:'\"'"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:{green,^blue,yellow}"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:1.0.0.{dev,^alpha,beta}"));
		assertSerialized(v);

	}

	@Test
	public void testVersionString() {
		Version v = null;
		String s = null;
		// should parse without exception
		assertNotNull(v = Version.create(s = "raw:1"));
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:1.0"));
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:1.0.0"));
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:1.0.0.9"));
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:1.0.0.'r12345'"));
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:1.0.0.'r12345.hello'"));
		assertEquals(s, v.toString());

		assertNotNull(v = Version.create(s = "raw:1.0.m"));
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:1.0.M"));
		assertEquals(s, v.toString());

		assertNotNull(v = Version.create(s = "raw:1.0.M"));
		assertEquals(s, v.toString());

		// -M is normalized
		assertNotNull(v = Version.create("raw:1.0.-M"));
		s = "raw:1.0";
		assertEquals(s, v.toString());

		assertNotNull(v = Version.create(s = "raw:0"));
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:0.1.2.3.4.5.6.7.8.9"));
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:0.-1.-2.-3.-4.-5.-6.-7.-8.-9"));
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:123456789.-1234567890"));
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:123456789.-1234567890"));
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:m"));
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:M"));
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:-M"));
		assertEquals("0.0.0", v.toString());
		assertNotNull(v = Version.create(s = "raw:1.m"));
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:1.M"));
		assertEquals(s, v.toString());
		// -M is normalized
		assertNotNull(v = Version.create("raw:1.-M"));
		s = "raw:1";
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:'a'"));
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:\"a\""));
		// " is normalized to '
		s = "raw:'a'";
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:'ab'"));
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:'abcdefghijklmnopqrstuvwxyz0123456789'"));
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'"));
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:'-_!\"#$%&/()=?+*;,:.'"));
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:\"'\""));
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:'\"'"));
		assertEquals(s, v.toString());

	}

	@Test
	public void testIntegerParsing() {

		// should parse without exception
		Version v = Version.create("raw:0");
		assertNotNull(v);
		assertEquals(v.getSegment(0), Integer.valueOf(0));

		// single digits
		v = Version.create("raw:0.1.2.3.4.5.6.7.8.9");
		assertNotNull(v);
		for (int i = 0; i < 10; i++)
			assertEquals(v.getSegment(i), Integer.valueOf(i));

		// negative single digits
		v = Version.create("raw:0.-1.-2.-3.-4.-5.-6.-7.-8.-9");
		assertNotNull(v);
		for (int i = 0; i < 10; i++)
			assertEquals(v.getSegment(i), Integer.valueOf(-i));

		// some larger numbers
		v = Version.create("raw:123456789.-1234567890");
		assertNotNull(v);
		assertEquals(v.getSegment(0), Integer.valueOf(123456789));
		assertEquals(v.getSegment(1), Integer.valueOf(-1234567890));
	}

	@Test
	public void testWhiteSpaceExceptions() {
		assertThrows("space not allowed 1", IllegalArgumentException.class, () -> Version.create("raw: 0 "));
		assertThrows("space not allowed 2", IllegalArgumentException.class,
				() -> Version.create("raw:0 .1  . 'a'.   'b c d'. 4. 5. 6.   7. 8 .  9"));

		assertThrows("space not allowed in array 1", IllegalArgumentException.class,
				() -> Version.create("raw:< 1.2.3>"));
		assertThrows("space not allowed in array 2", IllegalArgumentException.class,
				() -> Version.create("raw:<1.2.3 >"));
		assertThrows("Uncaught error: space between minus and number in negative", IllegalArgumentException.class,
				() -> Version.create("raw:1.- 1"));
	}

	@Test
	public void testMaxParsing() {

		assertNotNull(Version.create("raw:m"));
		assertNotNull(Version.create("raw:M"));
		assertNotNull(Version.create("raw:-M"));

		assertNotNull(Version.create("raw:1.m"));
		assertNotNull(Version.create("raw:1.M"));
		assertNotNull(Version.create("raw:1.-M"));
	}

	@Test
	public void testStringParsing() {
		Version v = Version.create("raw:'a'");
		assertNotNull(v);
		assertEquals(v.getSegment(0), "a");

		assertNotNull(v = Version.create("raw:\"a\""));
		assertEquals(v.getSegment(0), "a");

		assertNotNull(v = Version.create("raw:'ab'"));
		assertEquals(v.getSegment(0), "ab");

		assertNotNull(v = Version.create("raw:'abcdefghijklmnopqrstuvwxyz0123456789'"));
		assertEquals(v.getSegment(0), "abcdefghijklmnopqrstuvwxyz0123456789");

		assertNotNull(v = Version.create("raw:'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'"));
		assertEquals(v.getSegment(0), "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");

		assertNotNull(v = Version.create("raw:'-_!\"#$%&/()=?+*;,:.'"));
		assertEquals(v.getSegment(0), "-_!\"#$%&/()=?+*;,:.");

		assertNotNull(v = Version.create("raw:\"'\""));
		assertEquals(v.getSegment(0), "'");

		assertNotNull(v = Version.create("raw:'\"'"));
		assertEquals(v.getSegment(0), "\"");
	}

	@Test
	public void testEmptyStringParsing() {
		Version v = Version.create("raw:''");
		assertNotNull(v);
		assertEquals(v.getSegment(0), "");

		v = Version.create("raw:\"\"");
		assertNotNull(v);
		assertEquals(v.getSegment(0), "");
	}

	@Test
	public void testStringConcatenation() {
		Version v = Version.create("raw:'ab''cd'");
		assertNotNull(v);
		assertEquals(v.getSegment(0), "abcd");

		v = Version.create("raw:'ab'\"cd\"");
		assertNotNull(v);
		assertEquals(v.getSegment(0), "abcd");

		v = Version.create("raw:\"ab\"\"cd\"");
		assertNotNull(v);
		assertEquals(v.getSegment(0), "abcd");
	}

	@Test
	public void testStringToString() {
		// string is normalized
		assertEquals("raw:'abcd'", Version.create("raw:'ab''cd'").toString());

		// string is normalized
		assertEquals("raw:'abcd'", Version.create("raw:'ab'\"cd\"").toString());

		// string is normalized
		assertEquals("raw:'abcd'", Version.create("raw:\"ab\"\"cd\"").toString());

		assertEquals("raw:\"'\"", Version.create("raw:\"'\"").toString());

		assertEquals("raw:'\"'", Version.create("raw:'\"'").toString());

		// quotes are normalized - default ' should be used until " is needed and vice
		// versa.
		assertEquals("raw:'abc\"xxx\"and '\"'yyy'\"", Version.create("raw:'abc\"xxx\"'\"and 'yyy'\"").toString());

	}

	@Test
	public void testArrayParsing() {
		Version v = null;
		assertNotNull(v = Version.create("raw:<1>"));
		assertEquals(v.getSegment(0), new VersionVector(new Comparable[] { Integer.valueOf(1) }, null));

		assertNotNull(v = Version.create("raw:<1.0>"));
		assertEquals(v.getSegment(0),
				new VersionVector(new Comparable[] { Integer.valueOf(1), Integer.valueOf(0) }, null));

		assertNotNull(v = Version.create("raw:<'a'>"));
		assertEquals(v.getSegment(0), new VersionVector(new Comparable[] { "a" }, null));

		assertNotNull(v = Version.create("raw:<'a'.'b'>"));
		assertEquals(v.getSegment(0), new VersionVector(new Comparable[] { "a", "b" }, null));

		assertNotNull(v = Version.create("raw:<'a'.'b''c'>"));
		assertEquals(v.getSegment(0), new VersionVector(new Comparable[] { "a", "bc" }, null));

		assertNotNull(v = Version.create("raw:<1.2.-M>"));
		assertEquals(v.getSegment(0),
				new VersionVector(new Comparable[] { Integer.valueOf(1), Integer.valueOf(2) }, null));

		assertNotNull(v = Version.create("raw:<1.2.m>"));
		assertEquals(v.getSegment(0), new VersionVector(
				new Comparable[] { Integer.valueOf(1), Integer.valueOf(2), VersionVector.MAXS_VALUE }, null));

		assertNotNull(v = Version.create("raw:<1.2.M>"));
		assertEquals(v.getSegment(0), new VersionVector(
				new Comparable[] { Integer.valueOf(1), Integer.valueOf(2), VersionVector.MAX_VALUE }, null));

		assertNotNull(v = Version.create("raw:<<1>>"));
		assertEquals(v.getSegment(0), new VersionVector(
				new Comparable[] { new VersionVector(new Comparable[] { Integer.valueOf(1) }, null) }, null));

		assertNotNull(v = Version.create("raw:<<1.<2>>>"));
		assertEquals(v.getSegment(0),
				new VersionVector(
						new Comparable[] {
								new VersionVector(
										new Comparable[] { Integer.valueOf(1),
												new VersionVector(new Comparable[] { Integer.valueOf(2) }, null) },
										null) },
						null));

	}

	@Test
	public void testArraySerialize() {
		Version v = null;
		assertNotNull(v = Version.create("raw:<1>"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:<1.0>"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:<'a'>"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:<'a'.'b'>"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:<'a'.'b''c'>"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:<1.2.-M>"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:<1.2.m>"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:<1.2.M>"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:<<1>>"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:<<1.<2>>>"));
		assertSerialized(v);

	}

	@Test
	public void testArraytoString() {
		Version v = null;
		String s = null;
		assertNotNull(v = Version.create(s = "raw:<1>"));
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:<1.0>"));
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:<'a'>"));
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:<'a'.'b'>"));
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:<'a'.'bc'>"));
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create("raw:<1.2.-M>"));
		s = "raw:<1.2>"; // is normalized
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:<1.2.m>"));
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:<1.2.M>"));
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:<<1>>"));
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:<<1.<2>>>"));
		assertEquals(s, v.toString());
	}

	@Test
	public void testArrayOrder() {
		Version v1 = Version.create("raw:<1.0.0>");
		Version v2 = Version.create("raw:<1.1.0>");

		Version v3 = Version.create("raw:<1.0.0>.<1.0.0>");
		Version v4 = Version.create("raw:<1.0.0>.<1.0.0>.'a'");
		Version v5 = Version.create("raw:<1.0.0>.<1.0.1>");
		Version v6 = Version.create("raw:<2.0.0>");

		assertOrder(v1, v2);
		assertOrder(v3, v4);
		assertOrder(v4, v5);
		assertOrder(v5, v6);
	}

	@Test
	public void testPadParsing1() {
		Version v = null;
		assertNotNull(v = Version.create("raw:1.0p0"));
		assertPad(v, "raw:0");
		assertNotNull(v = Version.create("raw:1.0p'foo'"));
		assertPad(v, "raw:'foo'");
		assertNotNull(v = Version.create("raw:1.0p<0>"));
		assertPad(v, "raw:<0>");
		assertNotNull(v = Version.create("raw:1.0p<'foo'>"));
		assertPad(v, "raw:<'foo'>");
		assertNotNull(v = Version.create("raw:1.0pm"));
		assertPad(v, "raw:m");
		assertNotNull(v = Version.create("raw:1.0pM"));
		assertPad(v, "raw:M");
		assertNotNull(v = Version.create("raw:1.0p-M"));
		assertEquals(v.getPad(), null);
		assertNotNull(v = Version.create("raw:1.0p<m>"));
		assertPad(v, "raw:<m>");
		assertNotNull(v = Version.create("raw:1.0p<M>"));
		assertPad(v, "raw:<M>");
		assertNotNull(v = Version.create("raw:1.0p<-M>"));
		assertPad(v, "raw:<-M>");
		assertNotNull(v = Version.create("raw:1.0p<1.0.0.'r12345'.m>"));
		assertPad(v, "raw:<1.0.0.'r12345'.m>");
	}

	@Test
	public void testPadSerialize() {
		Version v = null;
		assertNotNull(v = Version.create("raw:1.0p0"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:1.0p'foo'"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:1.0p<0>"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:1.0p<'foo'>"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:1.0pm"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:1.0pM"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:1.0p-M"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:1.0p<m>"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:1.0p<M>"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:1.0p<-M>"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:1.0p<1.0.0.'r12345'.m>"));
		assertSerialized(v);
	}

	@Test
	public void testPadtoString() {
		Version v = null;
		String s = null;
		assertNotNull(v = Version.create("raw:1.0p0"));
		s = "raw:1p0"; // normalized
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:1.0p'foo'"));
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:1.0p<0>"));
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:1.0p<'foo'>"));
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:1.0pm"));
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:1.0pM"));
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create("raw:1.0p-M"));
		s = "raw:1.0"; // normalized
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:1.0p<m>"));
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:1.0p<M>"));
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:1.0p<-M>"));
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:1.0p<1.0.0.'r12345'.m>"));
		assertEquals(s, v.toString());
	}

	/**
	 * Test parsing of a pad with an array that in turn has padding.
	 */
	@Test
	public void testNestedPadParsing() {
		Version v = null;
		assertNotNull(v = Version.create("raw:1.0p<0p0>"));
		assertPad(v, "raw:<0p0>");
		assertPadPad(v, "raw:0");
		assertNotNull(v = Version.create("raw:1.0p<0p'foo'>"));
		assertPad(v, "raw:<0p'foo'>");
		assertPadPad(v, "raw:'foo'");
		assertNotNull(v = Version.create("raw:1.0p<0p<0>>"));
		assertPad(v, "raw:<0p<0>>");
		assertPadPad(v, "raw:<0>");
		assertNotNull(v = Version.create("raw:1.0p<0p<'foo'>>"));
		assertPad(v, "raw:<0p<'foo'>>");
		assertPadPad(v, "raw:<'foo'>");

		assertNotNull(v = Version.create("raw:1.0p<0pm>"));
		assertPad(v, "raw:<0pm>");
		assertPadPad(v, "raw:m");

		assertNotNull(v = Version.create("raw:1.0p<0pM>"));
		assertPad(v, "raw:<0pM>");
		assertPadPad(v, "raw:M");

		assertNotNull(v = Version.create("raw:1.0p<0p-M>"));
		assertPad(v, "raw:<0p-M>");
		assertPadPad(v, null);
		assertEquals(((VersionVector) v.getPad()).getPad(), null);

		assertNotNull(v = Version.create("raw:1.0p<0p<m>>"));
		assertPad(v, "raw:<0p<m>>");
		assertPadPad(v, "raw:<m>");

		assertNotNull(v = Version.create("raw:1.0p<0pM>"));
		assertPad(v, "raw:<0pM>");
		assertPadPad(v, "raw:M");

		assertNotNull(v = Version.create("raw:1.0p<0p-M>"));
		assertPad(v, "raw:<0p-M>");
		assertPadPad(v, null);
	}

	@Test
	public void testNestedPadSerialize() {
		Version v = null;
		assertNotNull(v = Version.create("raw:1.0p<0p0>"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:1.0p<0p'foo'>"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:1.0p<0p<0>>"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:1.0p<0p<'foo'>>"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:1.0p<0pm>"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:1.0p<0pM>"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:1.0p<0p-M>"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:1.0p<0p<m>>"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:1.0p<0pM>"));
		assertSerialized(v);
		assertNotNull(v = Version.create("raw:1.0p<0p-M>"));
		assertSerialized(v);
	}

	@Test
	public void testNestedPadtoString() {
		Version v = null;
		String s = null;
		assertNotNull(v = Version.create(s = "raw:1.0p<0p0>"));
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:1.0p<0p'foo'>"));
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:1.0p<0p<0>>"));
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:1.0p<0p<'foo'>>"));
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:1.0p<0pm>"));
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:1.0p<0pM>"));
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create("raw:1.0p<0p-M>"));
		s = "raw:1.0p<0>"; // normalized
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:1.0p<0p<m>>"));
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create(s = "raw:1.0p<0pM>"));
		assertEquals(s, v.toString());
		assertNotNull(v = Version.create("raw:1.0p<0p-M>"));
		s = "raw:1.0p<0>"; // normalized
		assertEquals(s, v.toString());
	}

	/**
	 * Tests that: 1p-M < 1.0.0 < 1.0.0p0 == 1p0 < 1.1 < 1.1.1 < 1p1 == 1.1p1 < 1pM
	 */
	@Test
	public void testPadOrder() {
		Version v1 = Version.create("raw:1p-M");
		Version v2 = Version.create("raw:1.0.0");
		Version v3 = Version.create("raw:1.0.0p0");
		Version v4 = Version.create("raw:1p0");
		Version v5 = Version.create("raw:1.1");
		Version v6 = Version.create("raw:1.1.1");
		Version v7 = Version.create("raw:1p1");
		Version v8 = Version.create("raw:1.1p1");
		Version v9 = Version.create("raw:1pM");

		assertOrder(v1, v2);
		assertOrder(v2, v3);
		assertEquals(v3, v4);
		assertOrder(v4, v5);
		assertOrder(v5, v6);
		assertOrder(v6, v7);
		assertEquals(v7, v8);
		assertOrder(v8, v9);
	}

	@Test
	public void testPadTypeOrder() {
		Version v0 = Version.create("raw:1p-M");
		Version v1 = Version.create("raw:1p'a'");
		Version v2 = Version.create("raw:1p<0>");
		Version v3 = Version.create("raw:1.0.0");
		Version v4 = Version.create("raw:1p0");
		Version v5 = Version.create("raw:1p1");
		Version v6 = Version.create("raw:1pM");
		assertOrder(v0, v1);
		assertOrder(v1, v2);
		assertOrder(v2, v3);
		assertOrder(v3, v4);
		assertOrder(v4, v5);
		assertOrder(v5, v6);
	}

	/**
	 * Test that a / is not prematurely taken as the separator between raw and
	 * original when in a string
	 */
	@Test
	public void testOriginalTerminator() {
		Version v = Version.create("raw:'/'");
		assertNotNull(v);
		assertEquals(v.getSegment(0), "/");

		v = Version.create("raw:\"/\"");
		assertNotNull(v);
		assertEquals(v.getSegment(0), "/");
	}

	@Test
	public void testEmptyInput() {
		// should parse with exception
		assertThrows(IllegalArgumentException.class, () -> Version.create("raw:"));
	}

	@Test
	public void testNewLine() {
		// should parse with exception
		assertThrows(IllegalArgumentException.class, () -> Version.create("raw:1.'\n'.2"));
	}

	@Test
	public void testSpaceInInt() {
		// should parse with exception
		assertThrows(IllegalArgumentException.class, () -> Version.create("raw:1 2.2"));
	}

	@Test
	public void testFloatingPointl() {
		// should parse with exception
		assertThrows(IllegalArgumentException.class, () -> Version.create("raw:1,2.2"));
	}

	@Test
	public void testScientific() {
		// should parse with exception
		assertThrows(IllegalArgumentException.class, () -> Version.create("raw:1E3"));
	}

	@Test
	public void testHex() {
		// should parse with exception
		assertThrows("Uncaught error: hexadecimal not allowed", IllegalArgumentException.class, () ->Version.create("raw:0xABCD"));
	}

	@Test
	public void testUnbalancedSingleQuoteRight() {
		assertThrows("Uncaught error: unbalanced sngle quote", IllegalArgumentException.class,
				() -> Version.create("raw:'unbalanced"));
	}

	@Test
	public void testMixedQuotes1() {
		assertThrows("Uncaught error: mixed quotes", IllegalArgumentException.class,
				() -> Version.create("raw:1.\"unbalanced'.10"));
	}

	@Test
	public void testMixedQuotes2() {
		assertThrows("Uncaught error: mixed quotes", IllegalArgumentException.class,
				() -> Version.create("raw:1.'unbalanced\".10"));
	}

	@Test
	public void testUnbalancedDoubleQuoteRight() {
		assertThrows(IllegalArgumentException.class, () -> Version.create("raw:\"unbalanced"));
	}

	@Test
	public void testUnbalancedArrayRight() {
		assertThrows(IllegalArgumentException.class, () -> Version.create("raw:<1.2.3"));
	}

	@Test
	public void testUnbalancedArrayLeft() {
		assertThrows(IllegalArgumentException.class, () -> Version.create("raw:1.2.3>"));
	}

	@Test
	public void testBadDecimalInteger() {
		assertThrows(IllegalArgumentException.class, () -> Version.create("raw:12af"));
	}
    @Test
	public void testUnquotedStringFirstValue() {
		assertThrows(IllegalArgumentException.class, () -> Version.create("raw:a"));
	}

	@Test
	public void testUnquotedStringSecondValue() {
		assertThrows(IllegalArgumentException.class, () -> Version.create("raw:1.a"));
	}

	@Test
	public void testSinglePeriod() {
		assertThrows(IllegalArgumentException.class, () -> Version.create("raw:."));
	}

	@Test
	public void testTwoPeriods() {
		assertThrows(IllegalArgumentException.class, () -> Version.create("raw:.."));
	}

	@Test
	public void testThreePeriods() {
		assertThrows(IllegalArgumentException.class, () -> Version.create("raw:..."));

	}

	@Test
	public void testPadNotLast() {
		assertThrows(IllegalArgumentException.class, () -> Version.create("raw:p10.10"));
	}

	@Test
	public void testEmptyPad() {
		assertThrows(IllegalArgumentException.class, () -> Version.create("raw:10p"));
	}

	@Test
	public void testPadWithNull() {
		assertThrows(IllegalArgumentException.class, () -> Version.create("raw:10p."));
	}

	@Test
	public void testWrongPadSeparator() {
		assertThrows(IllegalArgumentException.class, () -> Version.create("raw:10.p0"));
	}

	@Test
	public void testMultiplePadElements() {
		assertThrows(IllegalArgumentException.class, () -> Version.create("raw:10p1.2"));
	}

	@Test
	public void testUnbalancedPadElementsSQ() {
		assertThrows(IllegalArgumentException.class, () -> Version.create("raw:10p'abc"));
	}

	@Test
	public void testUnbalancedPadElementsDQ() {
		assertThrows(IllegalArgumentException.class, () -> Version.create("raw:10p\"abc"));
	}

	@Test
	public void testUnbalancedPadArrayElementsRight() {
		assertThrows(IllegalArgumentException.class, () -> Version.create("raw:10p<10"));
	}

	@Test
	public void testUnbalancedPadArrayElementsLeft() {
		assertThrows(IllegalArgumentException.class, () -> Version.create("raw:10p10>"));
	}

	@Test
	public void testOrder() {
		Version v1 = Version.create("raw:1");
		Version v2 = Version.create("raw:1.0");
		Version v3 = Version.create("raw:1.0.0");
		Version v4 = Version.create("raw:1.0.0.'9'");
		Version v5 = Version.create("raw:1.0.0.'r12345'");

		assertOrder(v1, v2);
		assertOrder(v2, v3);
		assertOrder(v3, v4);
		assertOrder(v4, v5);
	}

	@Test
	public void testTypeOrder() {
		// '' < 'z' < m < [0} < [M] < 0 < M
		Version v0 = Version.create("raw:-M");
		Version v1 = Version.create("raw:''");
		Version v2 = Version.create("raw:'z'");
		Version v3 = Version.create("raw:m");
		Version v4 = Version.create("raw:<0>");
		Version v5 = Version.create("raw:<M>");
		Version v6 = Version.create("raw:0");
		Version v7 = Version.create("raw:M");

		assertOrder(v0, v1);
		assertOrder(v1, v2);
		assertOrder(v2, v3);
		assertOrder(v3, v4);
		assertOrder(v4, v5);
		assertOrder(v5, v6);
		assertOrder(v6, v7);
	}

	@Test
	public void testTypeOrder2() {
		// '' < 'z' < m < [0] < [M] < 0 < M
		Version v0 = Version.create("raw:0.-M");
		Version v1 = Version.create("raw:0.''");
		Version v2 = Version.create("raw:0.'z'");
		Version v3 = Version.create("raw:0.m");
		Version v4 = Version.create("raw:0.<0>");
		Version v5 = Version.create("raw:0.<M>");
		Version v6 = Version.create("raw:0.0");
		Version v7 = Version.create("raw:0.M");

		assertOrder(v0, v1);
		assertOrder(v1, v2);
		assertOrder(v2, v3);
		assertOrder(v3, v4);
		assertOrder(v4, v5);
		assertOrder(v5, v6);
		assertOrder(v6, v7);
	}

	@Test
	public void testShorterIsOlder() {
		Version v1 = Version.create("raw:1.0");
		Version v2 = Version.create("raw:1.0.0");
		Version v3 = Version.create("raw:1.0.0.0");

		Version v4 = Version.create("raw:'a'");
		Version v5 = Version.create("raw:'a'.'b'.'b'");
		Version v6 = Version.create("raw:'a'.'b'.'b'.'b'");

		Version v7 = Version.create("raw:<1>");
		Version v8 = Version.create("raw:<1>.<0>.<0>");
		Version v9 = Version.create("raw:<1>.<0>.<0>.<0>");

		assertOrder(v1, v2);
		assertOrder(v2, v3);

		assertOrder(v4, v5);
		assertOrder(v5, v6);

		assertOrder(v7, v8);
		assertOrder(v8, v9);
	}

	@Test
	public void testNumericVersionOrder() {
		Version v1 = Version.create("1");
		Version v2 = Version.create("1.0.1");
		Version v3 = Version.create("1.1");
		Version v4 = Version.create("1.1.1");
		Version v5 = Version.create("1.1.1.-");
		Version v6 = Version.create("1.2");
		Version v7 = Version.create("2");
		Version v8 = Version.create("10.0");

		assertOrder(v1, v2);
		assertOrder(v2, v3);
		assertOrder(v3, v4);
		assertOrder(v4, v5);
		assertOrder(v5, v6);
		assertOrder(v6, v7);
		assertOrder(v7, v8);

	}

}
