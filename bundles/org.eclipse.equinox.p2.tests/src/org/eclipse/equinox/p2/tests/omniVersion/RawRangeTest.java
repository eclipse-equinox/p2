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

import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;

/**
 * Tests version ranges specified using raw.
 *
 */
public class RawRangeTest extends VersionTesting {
	public void testEmptyRange() {
		VersionRange range = new VersionRange("raw:''");
		assertIncludedInRange("#1", range, "raw:'a'");

		try {
			new VersionRange("raw:");
			fail("Uncaught error: a raw range can not be empty.");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testRangeDelimitersInStrings() {
		VersionRange range = null;
		range = new VersionRange("raw:['one\\,\\ two','three\\,\\ \\[and\\]\\ four']");
		assertIncludedInRange("#1", range, "raw:'one, two'");
		assertIncludedInRange("#2", range, "raw:'three, [and] four'");
	}

	public void testRangeDelimitersInStringstoString() {
		VersionRange range = null;
		String s = null;
		range = new VersionRange(s = "raw:['one\\,\\ two','three\\,\\ \\[and\\]\\ four']");
		assertEquals(s, range.toString());
	}

	public void testSingleVersionRange() {
		VersionRange range;
		range = new VersionRange("raw:[1.0.0, 1.0.0.'-')");
		assertEquals("0.1", Version.parseVersion("raw:1.0.0"), range.getMinimum());
		assertEquals("0.2", Version.parseVersion("raw:1.0.0.'-'"), range.getMaximum());

		assertNotIncludedInRange("0.9", range, "raw:0.9");
		assertNotIncludedInRange("1.0", range, "raw:1"); // this is not osgi versions 1 is before than 1.0
		assertNotIncludedInRange("1.1", range, "raw:1.0"); // this is not osgi, version 1.0 is before 1.0.0
		assertIncludedInRange("1.2", range, "raw:1.0.0");
		assertNotIncludedInRange("2.1", range, "raw:1.0.0.'0'");
		assertNotIncludedInRange("2.2", range, "raw:1.0.1");
		assertNotIncludedInRange("2.3", range, "raw:1.1");
		assertNotIncludedInRange("2.4", range, "raw:2");
	}

	public void testInvertedRange() {
		try {
			new VersionRange("raw:[2.0.0, 1.0.0]");
			fail("Inverted range is not allowed");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testGreaterThan() {
		// any version equal or greater than 1.0.0 is ok 
		VersionRange lowerBound = new VersionRange("raw:1.0.0");
		assertNotIncludedInRange("1.0", lowerBound, "raw:0.9.0");
		assertIncludedInRange("1.1", lowerBound, "raw:1.0.0");
		assertIncludedInRange("1.2", lowerBound, "raw:1.9.9.'x'");
		assertIncludedInRange("1.3", lowerBound, "raw:999.999.999.'foo'");
		assertIncludedInRange("1.3", lowerBound, "raw:M.M.M.m");
	}

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
