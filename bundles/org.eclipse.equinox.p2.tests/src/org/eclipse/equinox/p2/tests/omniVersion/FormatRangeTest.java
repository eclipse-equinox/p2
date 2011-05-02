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
 * Tests ranges using format(xxx) version strings.
 *
 */
public class FormatRangeTest extends VersionTesting {
	public static String OSGI_PREFIX = "format(n[.n=0;[.n=0;[.S=[A-Za-z0-9_-];]]]):";

	public void testRangeWithDefaultValues() {
		VersionRange range = new VersionRange(OSGI_PREFIX + "0");
		assertIncludedInRange("#1", range, OSGI_PREFIX + "0");
		assertIncludedInRange("#2", range, OSGI_PREFIX + "0.0");
		assertIncludedInRange("#3", range, OSGI_PREFIX + "0.0.0");
	}

	public void testEmptyRange() {
		try {
			new VersionRange(OSGI_PREFIX);
			fail("Uncught error: range can not be empty");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testRangeDelimitersInVersionString() {
		VersionRange range = new VersionRange("format(S):[one\\,\\ two,three\\,\\ \\[and\\]\\ four]");
		assertIncludedInRange("#1", range, "format(S):one, two");
		assertIncludedInRange("#1", range, "format(S):three, [and] four");
	}

	public void testSingleVersionRange() {
		VersionRange range = new VersionRange(OSGI_PREFIX + "[1.0.0, 1.0.0.x)");
		assertEquals("0.1", Version.parseVersion(OSGI_PREFIX + "1.0.0"), range.getMinimum());
		assertEquals("0.2", Version.parseVersion(OSGI_PREFIX + "1.0.0.x"), range.getMaximum());

		assertNotIncludedInRange("0.9", range, OSGI_PREFIX + "0.9");
		assertIncludedInRange("1.2", range, OSGI_PREFIX + "1.0.0");
		assertNotIncludedInRange("2.1", range, OSGI_PREFIX + "1.0.0.z");
		assertNotIncludedInRange("2.2", range, OSGI_PREFIX + "1.0.1");
		assertNotIncludedInRange("2.3", range, OSGI_PREFIX + "1.1");
		assertNotIncludedInRange("2.4", range, OSGI_PREFIX + "2");
	}

	public void testGreaterThan() {
		// any version equal or greater than 1.0.0 is ok 
		VersionRange lowerBound = new VersionRange(OSGI_PREFIX + "1.0.0");
		assertNotIncludedInRange("1.0", lowerBound, OSGI_PREFIX + "0.9.0");
		assertIncludedInRange("1.1", lowerBound, OSGI_PREFIX + "1.0.0");
		assertIncludedInRange("1.2", lowerBound, OSGI_PREFIX + "1.9.9.x");
		assertIncludedInRange("1.3", lowerBound, OSGI_PREFIX + "999.999.999.foo");
	}

	public void testGreaterThanMinimum() {
		// any version equal or greater than Version.emptyVersion is ok 
		VersionRange lowerBound = new VersionRange("raw:-M");
		assertIncludedInRange("0.1", lowerBound, "raw:-M");
		assertIncludedInRange("1.0", lowerBound, OSGI_PREFIX + "0.9.0");
		assertIncludedInRange("1.1", lowerBound, OSGI_PREFIX + "1.0.0");
		assertIncludedInRange("1.2", lowerBound, OSGI_PREFIX + "1.9.9.x");
		assertIncludedInRange("1.3", lowerBound, OSGI_PREFIX + "999.999.999.foo");
	}

	public void testLowerThan() {
		// any version lower than 2.0 is ok 		
		VersionRange upperBound = new VersionRange(OSGI_PREFIX + "[0, 2.0.0)");
		assertIncludedInRange("1.0", upperBound, OSGI_PREFIX + "0.0");
		assertIncludedInRange("1.1", upperBound, OSGI_PREFIX + "0.9");
		assertIncludedInRange("1.2", upperBound, OSGI_PREFIX + "1.0");
		assertIncludedInRange("1.3", upperBound, OSGI_PREFIX + "1.9.9.x");
		assertNotIncludedInRange("1.4", upperBound, OSGI_PREFIX + "2.0");
		assertNotIncludedInRange("1.5", upperBound, OSGI_PREFIX + "2.1");
	}
}