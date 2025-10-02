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
import static org.junit.Assert.assertThrows;

import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.junit.Test;

/**
 * Tests version ranges specified using raw.
 */
public class RawRangeTest extends VersionTesting {

	private static Version ONE = Version.parseVersion("raw:1");
	private static Version TWO = Version.parseVersion("raw:2");

	@Test
	public void testEmptyRange() {
		VersionRange range = new VersionRange("raw:''");
		assertIncludedInRange("#1", range, "raw:'a'");

		assertThrows("Uncaught error: a raw range can not be empty.", IllegalArgumentException.class,
				() -> new VersionRange("raw:"));
	}

	@Test
	public void testRangeDelimitersInStrings() {
		VersionRange range = null;
		range = new VersionRange("raw:['one\\,\\ two','three\\,\\ \\[and\\]\\ four']");
		assertIncludedInRange("#1", range, "raw:'one, two'");
		assertIncludedInRange("#2", range, "raw:'three, [and] four'");
	}

	@Test
	public void testRangeDelimitersInStringstoString() {
		VersionRange range = null;
		String s = null;
		range = new VersionRange(s = "raw:['one\\,\\ two','three\\,\\ \\[and\\]\\ four']");
		assertEquals(s, range.toString());
	}

	@Test
	public void testSingleVersionRange() {
		VersionRange range;
		range = new VersionRange("raw:[1.0.0, 1.0.0.'-')");
		assertEquals("Range minimum should be raw:1.0.0", Version.parseVersion("raw:1.0.0"), range.getMinimum());
		assertEquals("Range maximum should be raw:1.0.0.'-'", Version.parseVersion("raw:1.0.0.'-'"), range.getMaximum());

		assertNotIncludedInRange("raw:0.9 should not be in range", range, "raw:0.9");
		assertNotIncludedInRange("raw:1 should not be in range (not OSGi, 1 is before 1.0)", range, "raw:1"); // this is not osgi versions 1 is before than 1.0
		assertNotIncludedInRange("raw:1.0 should not be in range (not OSGi, 1.0 is before 1.0.0)", range, "raw:1.0"); // this is not osgi, version 1.0 is before 1.0.0
		assertIncludedInRange("raw:1.0.0 should be in range", range, "raw:1.0.0");
		assertNotIncludedInRange("raw:1.0.0.'0' should not be in range", range, "raw:1.0.0.'0'");
		assertNotIncludedInRange("raw:1.0.1 should not be in range", range, "raw:1.0.1");
		assertNotIncludedInRange("raw:1.1 should not be in range", range, "raw:1.1");
		assertNotIncludedInRange("raw:2 should not be in range", range, "raw:2");
	}

	@Test
	public void testInvertedRange() {
		assertThrows("\"Inverted range is not allowed\"", IllegalArgumentException.class,
				() -> new VersionRange("raw:[2.0.0, 1.0.0]"));
	}

	@Test
	public void testGreaterThan() {
		// any version equal or greater than 1.0.0 is ok
		VersionRange lowerBound = new VersionRange("raw:1.0.0");
		assertNotIncludedInRange("1.0", lowerBound, "raw:0.9.0");
		assertIncludedInRange("1.1", lowerBound, "raw:1.0.0");
		assertIncludedInRange("1.2", lowerBound, "raw:1.9.9.'x'");
		assertIncludedInRange("1.3", lowerBound, "raw:999.999.999.'foo'");
		assertIncludedInRange("1.3", lowerBound, "raw:M.M.M.m");
	}

	@Test
	public void testGreaterThanSmallest() {
		// any version equal or greater than -M' (empty string) is ok
		VersionRange lowerBound = new VersionRange("raw:-M");
		assertIncludedInRange("#1", lowerBound, "raw:-M");
		assertIncludedInRange("#1.1", lowerBound, "raw:''");
		assertIncludedInRange("#1.1", lowerBound, "raw:m");
		assertIncludedInRange("#2", lowerBound, "raw:0.9.0");
		assertIncludedInRange("#3", lowerBound, "raw:1.0.0");
		assertIncludedInRange("#4", lowerBound, "raw:1.9.9.'x'");
		assertIncludedInRange("#5", lowerBound, "raw:999.999.999.'foo'");
		assertIncludedInRange("#6", lowerBound, "raw:M.M.M.m");
		assertIncludedInRange("#7", lowerBound, "raw:M");
		assertIncludedInRange("#8", lowerBound, "raw:MpM");
	}

	@Test
	public void testLowerThan() {
		// any version lower than 2.0 is ok
		VersionRange upperBound = new VersionRange("raw:[0, 2.0)");
		assertIncludedInRange("1.0", upperBound, "raw:0.0");
		assertIncludedInRange("1.1", upperBound, "raw:0.9");
		assertIncludedInRange("1.2", upperBound, "raw:1.0");
		assertIncludedInRange("1.3", upperBound, "raw:1.9.9.'x'");
		assertNotIncludedInRange("1.4", upperBound, "raw:2.0");
		assertNotIncludedInRange("1.5", upperBound, "raw:2.1");
	}

	@Test
	public void testExplicitLowerAndUpperBound() {
		assertBounds("raw:[1,2)", true, ONE, TWO, false);
		assertBounds("raw:[1,2]", true, ONE, TWO, true);
	}

	@Test
	public void testNoLowerBound() {
		assertBounds("raw:(,1)", true, Version.emptyVersion, ONE, false);
		assertBounds("raw:[,1)", true, Version.emptyVersion, ONE, false);
	}

	@Test
	public void testNoUpperBound() {
		assertBounds("raw:[1,)", true, ONE, Version.MAX_VERSION, true);
		assertBounds("raw:[1,]", true, ONE, Version.MAX_VERSION, true);
	}

	@Test
	public void testNoLowerAndUpperBound() {
		assertBounds("raw:(,)", true, Version.emptyVersion, Version.MAX_VERSION, true);
		assertBounds("raw:[,]", true, Version.emptyVersion, Version.MAX_VERSION, true);
	}

	@Test
	public void testSerialize() {
		VersionRange v = null;

		v = new VersionRange("raw:1.0.0");
		assertSerialized(v);
		v = new VersionRange("raw:[1.0.0,2.0.0]");
		assertSerialized(v);
		v = new VersionRange("raw:(1.0.0,2.0.0]");
		assertSerialized(v);
		v = new VersionRange("raw:[1.0.0,2.0.0)");
		assertSerialized(v);
		v = new VersionRange("raw:(1.0.0,2.0.0)");
		assertSerialized(v);

		v = new VersionRange("raw:1.0.0.'abcdef'");
		assertSerialized(v);
		v = new VersionRange("raw:[1.0.0.'abcdef',2.0.0.'abcdef']");
		assertSerialized(v);
		v = new VersionRange("raw:(1.0.0.'abcdef',2.0.0.'abcdef']");
		assertSerialized(v);
		v = new VersionRange("raw:[1.0.0.'abcdef',2.0.0.'abcdef')");
		assertSerialized(v);
		v = new VersionRange("raw:(1.0.0.'abcdef',2.0.0.'abcdef')");
		assertSerialized(v);
	}

	@Test
	public void testToString() {
		VersionRange v = null;
		String s = null;
		v = new VersionRange(s = "raw:1.0.0");
		assertEquals(s, v.toString());
		v = new VersionRange(s = "raw:[1.0.0,2.0.0]");
		assertEquals(s, v.toString());
		v = new VersionRange(s = "raw:(1.0.0,2.0.0]");
		assertEquals(s, v.toString());
		v = new VersionRange(s = "raw:[1.0.0,2.0.0)");
		assertEquals(s, v.toString());
		v = new VersionRange(s = "raw:(1.0.0,2.0.0)");
		assertEquals(s, v.toString());

		v = new VersionRange(s = "raw:1.0.0.'abcdef'");
		assertEquals(s, v.toString());
		v = new VersionRange(s = "raw:[1.0.0.'abcdef',2.0.0.'abcdef']");
		assertEquals(s, v.toString());
		v = new VersionRange(s = "raw:(1.0.0.'abcdef',2.0.0.'abcdef']");
		assertEquals(s, v.toString());
		v = new VersionRange(s = "raw:[1.0.0.'abcdef',2.0.0.'abcdef')");
		assertEquals(s, v.toString());
		v = new VersionRange(s = "raw:(1.0.0.'abcdef',2.0.0.'abcdef')");
		assertEquals(s, v.toString());
	}

}
