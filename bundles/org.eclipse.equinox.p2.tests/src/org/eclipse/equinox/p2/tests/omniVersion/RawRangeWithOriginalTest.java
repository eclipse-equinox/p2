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

import org.eclipse.equinox.internal.p2.metadata.VersionFormat;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.junit.Test;

/**
 * Tests inclusion of original version range string in raw format.
 * The tests in this class does not fully test the various "format(rules)" only that the sequence
 * "raw RANGE/format():ORIGINAL RANGE" works, and that errors at the top level are caught.
 */
public class RawRangeWithOriginalTest extends VersionTesting {
	@Test
	public void testRawWithUnknownFormat() {
		VersionRange v = new VersionRange("raw:[1.0,2.0]/:sailor.moon.R,sailor.moon.S");
		assertEquals(v, new VersionRange("raw:[1.0,2.0]"));
	}

	@Test
	public void testRawWithUnknownFormatToString() {
		assertEquals("raw:[1.0,2.0]/:sailor.moon.R,sailor.moon.S", new VersionRange("raw:[1.0,2.0]/:sailor.moon.R,sailor.moon.S").toString());
	}

	@Test
	public void testRawWithUnknownFormatSerialized() {
		assertSerialized(new VersionRange("raw:[1.0,2.0]/:sailor.moon.R,sailor.moon.S"));
		assertEquals("raw:[1.0,2.0]/:sailor.moon.R,sailor.moon.S", getSerialized(new VersionRange("raw:[1.0,2.0]/:sailor.moon.R,sailor.moon.S")).toString());
	}

	@Test
	public void testRawWithSimpleFormat() {
		VersionRange v = new VersionRange("raw:[1.0,2.0]/format(n.n):[1.0,2.0]");
		assertEquals(v, new VersionRange("raw:[1.0,2.0]"));
	}

	@Test
	public void testRawWithSimpleFormatToString() {
		// range brackets are normalized in toString - not needed in original
		assertEquals("raw:[1.0,2.0]/format(n.n):1.0,2.0", new VersionRange("raw:[1.0,2.0]/format(n.n):[1.0,2.0]").toString());
	}

	@Test
	public void testSimpleFormatToString() {
		// range brackets are normalized in toString - not needed in original
		assertEquals("raw:[1.0,2.0]/format(n.n):1.0,2.0", new VersionRange("format(n.n):[1.0,2.0]").toString());
	}

	@Test
	public void testRawWithSimpleFormatSerialized() {
		assertSerialized(new VersionRange("raw:[1.0,2.0]/format(n.n):[1.0,2.0]"));
		// range brackets are normalized in toString - not needed in original
		assertEquals("raw:[1.0,2.0]/format(n.n):1.0,2.0", getSerialized(new VersionRange("raw:[1.0,2.0]/format(n.n):[1.0,2.0]")).toString());
	}

	@Test
	public void testOriginalStatedButMissing() {
			assertThrows(IllegalArgumentException.class, () -> new VersionRange("raw:[1.0,2.0]/"));
	}

	@Test
	public void testOriginalAndUnknownStatedButMissing() {
		assertThrows(IllegalArgumentException.class, () -> new VersionRange("raw:[1.0,2.0]/:"));
	}

	@Test
	public void testOriginalIllegalFormat() {
		assertThrows(IllegalArgumentException.class, () -> new VersionRange("raw:[1.0,2.0]/foo:"));
	}

	@Test
	public void testOriginalIllegalFormat2() {
		assertThrows(IllegalArgumentException.class, () -> new VersionRange("raw:[1.0,2.0]/100:"));
	}

	@Test
	public void testOriginalIllegalFormat3() {
		assertThrows(IllegalArgumentException.class, () -> new VersionRange("raw:[1.0,2.0]/'format':"));
	}

	@Test
	public void testOriginalIllegalFormat4() {
		assertThrows(IllegalArgumentException.class, () -> new VersionRange("raw:[1.0,2.0]//1.0"));
	}

	@Test
	public void testOriginalIllegalFormat5() {
		assertThrows(IllegalArgumentException.class, () -> new VersionRange("raw:[1.0,2.0]/format:"));
	}

	@Test
	public void testOriginalFormatUnbalancedLeft() {
		assertThrows(IllegalArgumentException.class, () -> new VersionRange("raw:[1.0,2.0]/formatn.n):"));
	}

	@Test
	public void testOriginalFormatUnbalancedRight() {
		assertThrows(IllegalArgumentException.class, () -> new VersionRange("raw:[1.0,2.0]/format(n.n:1.0"));
	}

	@Test
	public void testOriginalFormatOriginalMissing() {
		assertThrows(IllegalArgumentException.class, () -> new VersionRange("raw:[1.0,2.0]/format(n.n):"));
	}

	@Test
	public void testGreaterThan() {
		// any version equal or greater than 1.0.0 is ok
		VersionRange lowerBound = new VersionRange("raw:2.1.0.M/format(n[.n=0;[.n=0;]][d?S=M;]):2.1");
		assertNotIncludedInRange("1.0", lowerBound, "raw:2.0.9");
		assertIncludedInRange("1.1", lowerBound, "raw:2.2");
		assertIncludedInRange("1.3", lowerBound, "raw:999.999.999.'foo'");
	}

	@Test
	public void testMinBoundary() {
		String rangeString = "raw:[-M,2.1.0.M]/format(n[.n=0;[.n=0;]][d?S=M;]):-M,2.1";
		VersionRange range = new VersionRange(rangeString);
		assertEquals(rangeString, range.toString());

		VersionRange range1 = new VersionRange(range.getMinimum(), range.getIncludeMinimum(), range.getMaximum(), range.getIncludeMaximum());
		assertEquals(range1, range);

		VersionRange range2 = new VersionRange(null, true, range.getMaximum(), range.getIncludeMaximum());
		assertEquals(range2, range);
	}

	@Test
	public void testOSGiMinBoundary() {
		String rangeString = "raw:[-M,2.1.0.'']/format(" + VersionFormat.OSGI_FORMAT_STRING + "):-M,2.1.0";
		VersionRange range = new VersionRange(rangeString);

		VersionRange range1 = new VersionRange("[0.0.0,2.1.0]");
		assertEquals(range1, range);

		assertEquals("[0.0.0,2.1.0]", range.toString());

		VersionRange range2 = new VersionRange(null, true, range.getMaximum(), range.getIncludeMaximum());
		assertEquals(range2, range);
	}

	@Test
	public void testMaxBoundary() {
		String rangeString = "raw:[2.1.0.M,MpM]/format(n[.n=0;[.n=0;]][d?S=M;]):2.1,MpM";
		VersionRange range = new VersionRange(rangeString);
		assertEquals("raw:2.1.0.M/format(n[.n=0;[.n=0;]][d?S=M;]):2.1", range.toString());

		VersionRange range1 = new VersionRange(range.getMinimum(), range.getIncludeMinimum(), range.getMaximum(), range.getIncludeMaximum());
		assertEquals(range1, range);

		VersionRange range2 = new VersionRange(range.getMinimum(), true, null, true);
		assertEquals(range2, range);
	}

	@Test
	public void testRecreateUsingMaxUpper() {
		Version v = Version.create("format(n[.n=0;[.n=0;]][d?S=M;]):2.1");
		VersionRange range = new VersionRange(v, true, null, true);
		Version min = range.getMinimum();
		Version max = range.getMaximum();
		VersionRange range2 = new VersionRange(min, true, max, true);
		assertEquals(range2, range);
	}

	@Test
	public void testRecreateUsingMinLower() {
		Version v = Version.create("format(n[.n=0;[.n=0;]][d?S=M;]):2.1");
		VersionRange range = new VersionRange(null, true, v, true);
		Version min = range.getMinimum();
		Version max = range.getMaximum();
		VersionRange range2 = new VersionRange(min, true, max, true);
		assertEquals(range2, range);
	}

	@Test
	public void testOSGiMaxBoundary() {
		String rangeString = "raw:[2.1.0.'',MpM]/format(" + VersionFormat.OSGI_FORMAT_STRING + "):2.1.0,MpM";
		VersionRange range = new VersionRange(rangeString);

		VersionRange range1 = new VersionRange("2.1.0");
		assertEquals(range1, range);

		assertEquals("2.1.0", range.toString());

		VersionRange range2 = new VersionRange(range.getMinimum(), true, null, true);
		assertEquals(range2, range);
	}
}
