/*******************************************************************************
 * Copyright (c) 2009, 2010 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *     IBM - Ongoing development
 *******************************************************************************/

package org.eclipse.equinox.p2.tests.omniVersion;

import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;

/**
 * Tests ranges of versions specified with osgi (default) version format.
 *
 */
public class OSGiRangeTest extends VersionTesting {
	public void testSingleVersionRange() {
		VersionRange range;
		range = new VersionRange("[1.0.0, 1.0.0.-)");
		assertEquals("0.1", Version.parseVersion("1.0"), range.getMinimum());
		assertTrue("0.9", !range.isIncluded(Version.parseVersion("0.9")));
		assertTrue("1.0", range.isIncluded(Version.parseVersion("1")));
		assertTrue("1.1", range.isIncluded(Version.parseVersion("1.0")));
		assertTrue("1.2", range.isIncluded(Version.parseVersion("1.0.0")));
		assertTrue("2.1", !range.isIncluded(Version.parseVersion("1.0.0.0")));
		assertTrue("2.2", !range.isIncluded(Version.parseVersion("1.0.1")));
		assertTrue("2.3", !range.isIncluded(Version.parseVersion("1.1")));
		assertTrue("2.4", !range.isIncluded(Version.parseVersion("2")));
	}

	public void testInvertedRange() {
		try {
			new VersionRange("[2.0.0, 1.0.0]");
			fail("Inverted range is not allowed");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testGreaterThan() {
		// any version equal or greater than 1.0 is ok 
		VersionRange lowerBound = new VersionRange("1.0.0");
		assertTrue("1.0", !lowerBound.isIncluded(Version.parseVersion("0.9")));
		assertTrue("1.1", lowerBound.isIncluded(Version.parseVersion("1.0")));
		assertTrue("1.2", lowerBound.isIncluded(Version.parseVersion("1.9.9.x")));
		assertTrue("1.3", lowerBound.isIncluded(Version.parseVersion("999.999.999.foo")));
		assertTrue("2.0", lowerBound.isIncluded(Version.parseVersion("raw:M")));
		assertTrue("2.1", lowerBound.isIncluded(Version.parseVersion("raw:2147483647.2147483647.2147483647.0")));

	}

	public void testLowerThan() {
		// any version lower than 2.0 is ok 		
		VersionRange upperBound = new VersionRange("[0,2.0)");
		assertTrue("1.0", upperBound.isIncluded(Version.parseVersion("0.0")));
		assertTrue("1.1", upperBound.isIncluded(Version.parseVersion("0.9")));
		assertTrue("1.2", upperBound.isIncluded(Version.parseVersion("1.0")));
		assertTrue("1.3", upperBound.isIncluded(Version.parseVersion("1.9.9.x")));
		assertTrue("1.4", !upperBound.isIncluded(Version.parseVersion("2.0")));
		assertTrue("1.5", !upperBound.isIncluded(Version.parseVersion("2.1")));
	}

	public void testRangeStrings() {
		VersionRange v = null;

		v = new VersionRange("1.0.0");
		assertEquals("1.0.0", v.toString());
		v = new VersionRange("[1.0.0,2.0.0]");
		assertEquals("[1.0.0,2.0.0]", v.toString());
		v = new VersionRange("(1.0.0,2.0.0]");
		assertEquals("(1.0.0,2.0.0]", v.toString());
		v = new VersionRange("[1.0.0,2.0.0)");
		assertEquals("[1.0.0,2.0.0)", v.toString());
		v = new VersionRange("(1.0.0,2.0.0)");
		assertEquals("(1.0.0,2.0.0)", v.toString());

		v = new VersionRange("1.0.0.abcdef");
		assertEquals("1.0.0.abcdef", v.toString());
		v = new VersionRange("[1.0.0.abcdef,2.0.0.abcdef]");
		assertEquals("[1.0.0.abcdef,2.0.0.abcdef]", v.toString());
		v = new VersionRange("(1.0.0.abcdef,2.0.0.abcdef]");
		assertEquals("(1.0.0.abcdef,2.0.0.abcdef]", v.toString());
		v = new VersionRange("[1.0.0.abcdef,2.0.0.abcdef)");
		assertEquals("[1.0.0.abcdef,2.0.0.abcdef)", v.toString());
		v = new VersionRange("(1.0.0.abcdef,2.0.0.abcdef)");
		assertEquals("(1.0.0.abcdef,2.0.0.abcdef)", v.toString());
	}

	/**
	 * Tests that null values passed to the {@link VersionRange} constructor
	 * are not interpreted as MIN/MAX versions.
	 */
	public void testNullConstructor() {
		VersionRange range = new VersionRange(null);
		assertEquals("1.0", range.getMinimum(), Version.emptyVersion);
		assertEquals("1.1", range.getMaximum(), Version.MAX_VERSION);

		range = new VersionRange(null, true, null, true);
		assertEquals("2.0", range.getMinimum(), Version.emptyVersion);
		assertEquals("2.1", range.getMaximum(), Version.MAX_VERSION);
	}

	public void testSerialize() {
		VersionRange v = null;

		v = new VersionRange("1.0.0");
		assertSerialized(v);
		v = new VersionRange("[1.0.0,2.0.0]");
		assertSerialized(v);
		v = new VersionRange("(1.0.0,2.0.0]");
		assertSerialized(v);
		v = new VersionRange("[1.0.0,2.0.0)");
		assertSerialized(v);
		v = new VersionRange("(1.0.0,2.0.0)");
		assertSerialized(v);

		v = new VersionRange("1.0.0.abcdef");
		assertSerialized(v);
		v = new VersionRange("[1.0.0.abcdef,2.0.0.abcdef]");
		assertSerialized(v);
		v = new VersionRange("(1.0.0.abcdef,2.0.0.abcdef]");
		assertSerialized(v);
		v = new VersionRange("[1.0.0.abcdef,2.0.0.abcdef)");
		assertSerialized(v);
		v = new VersionRange("(1.0.0.abcdef,2.0.0.abcdef)");
		assertSerialized(v);
	}
}
