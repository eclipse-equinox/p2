/*******************************************************************************
 * Copyright (c) 2009 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.p2.tests.omniVersion;

import org.eclipse.equinox.internal.provisional.p2.core.VersionRange;

/**
 * Tests inclusion of original version range string in raw format.
 * The tests in this class does not fully test the various "format(rules)" only that the sequence
 * "raw RANGE/format():ORIGINAL RANGE" works, and that errors at the top level are caught.
 * 
 */
public class RawRangeWithOriginalTest extends VersionTesting {

	public void testRawWithUnknownFormat() {
		VersionRange v = new VersionRange("raw:[1.0,2.0]/:sailor.moon.R,sailor.moon.S");
		assertEquals(v, new VersionRange("raw:[1.0,2.0]"));
	}

	public void testRawWithUnknownFormatToString() {
		assertEquals("raw:[1.0,2.0]/:sailor.moon.R,sailor.moon.S", new VersionRange("raw:[1.0,2.0]/:sailor.moon.R,sailor.moon.S").toString());
	}

	public void testRawWithUnknownFormatSerialized() {
		assertSerialized(new VersionRange("raw:[1.0,2.0]/:sailor.moon.R,sailor.moon.S"));
		assertEquals("raw:[1.0,2.0]/:sailor.moon.R,sailor.moon.S", getSerialized(new VersionRange("raw:[1.0,2.0]/:sailor.moon.R,sailor.moon.S")).toString());
	}

	public void testRawWithSimpleFormat() {
		VersionRange v = new VersionRange("raw:[1.0,2.0]/format(n.n):[1.0,2.0]");
		assertEquals(v, new VersionRange("raw:[1.0,2.0]"));
	}

	public void testRawWithSimpleFormatToString() {
		// range brackets are normalized in toString - not needed in original
		assertEquals("raw:[1.0,2.0]/format(n.n):1.0,2.0", new VersionRange("raw:[1.0,2.0]/format(n.n):[1.0,2.0]").toString());
	}

	public void testRawWithSimpleFormatSerialized() {
		assertSerialized(new VersionRange("raw:[1.0,2.0]/format(n.n):[1.0,2.0]"));
		// range brackets are normalized in toString - not needed in original
		assertEquals("raw:[1.0,2.0]/format(n.n):1.0,2.0", getSerialized(new VersionRange("raw:[1.0,2.0]/format(n.n):[1.0,2.0]")).toString());
	}

	public void testOriginalStatedButMissing() {
		try {
			new VersionRange("raw:[1.0,2.0]/");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testOriginalAndUnknownStatedButMissing() {
		try {
			new VersionRange("raw:[1.0,2.0]/:");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testOriginalIllegalFormat() {
		try {
			new VersionRange("raw:[1.0,2.0]/foo:");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testOriginalIllegalFormat2() {
		try {
			new VersionRange("raw:[1.0,2.0]/100:");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testOriginalIllegalFormat3() {
		try {
			new VersionRange("raw:[1.0,2.0]/'format':");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testOriginalIllegalFormat4() {
		try {
			new VersionRange("raw:[1.0,2.0]//1.0");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testOriginalIllegalFormat5() {
		try {
			new VersionRange("raw:[1.0,2.0]/format:");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testOriginalFormatUnbalancedLeft() {
		try {
			new VersionRange("raw:[1.0,2.0]/formatn.n):");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testOriginalFormatUnbalancedRight() {
		try {
			new VersionRange("raw:[1.0,2.0]/format(n.n:1.0");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testOriginalFormatOriginalMissing() {
		try {
			new VersionRange("raw:[1.0,2.0]/format(n.n):");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}
}
