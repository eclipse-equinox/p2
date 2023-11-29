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
import static org.junit.Assert.assertTrue;

import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.junit.Test;

/**
 * Tests intersection of VersionRanges.
 * - non overlapping ranges
 * - straddle lower or upper bound
 * - equal range
 * - same lower bound - upper inside
 * - same upper bound - lower inside
 * - inside
 *
 * Tests made with both inclusive and non inclusive values.
 */
public class IntersectionTest {
	@Test
	public void testIntersectsEmpty() {
		VersionRange a = new VersionRange("raw:[1.0.0,3.0.0]");
		VersionRange b = new VersionRange("raw:[4.0.0,6.0.0]");
		assertTrue("Non overlapping ranges a/b should be empty #1", a.intersect(b) == null);
		assertTrue("Non overlapping ranges b/a should be empty #2", b.intersect(a) == null);

		a = new VersionRange("raw:[1.0.0,3.0.0]");
		b = new VersionRange("raw:(3.0.0,6.0.0]");
		assertTrue("Non overlapping ranges a/b should be empty #3", a.intersect(b) == null);
		assertTrue("Non overlapping ranges b/a should be empty #4", b.intersect(a) == null);

		a = new VersionRange("raw:[1.0.0,3.0.0)");
		b = new VersionRange("raw:[3.0.0,6.0.0]");
		assertTrue("Non overlapping ranges a/b should be empty #5", a.intersect(b) == null);
		assertTrue("Non overlapping ranges b/a should be empty #6", b.intersect(a) == null);
	}

	@Test
	public void testStraddleBoundary() {
		VersionRange a = new VersionRange("raw:[1.0.0,3.0.0]");
		VersionRange b = new VersionRange("raw:[2.0.0,6.0.0]");
		VersionRange r = a.intersect(b);
		assertEquals("#1.1", Version.parseVersion("raw:2.0.0"), r.getMinimum());
		assertEquals("#1.2", Version.parseVersion("raw:3.0.0"), r.getMaximum());
		assertTrue("#1.3", r.getIncludeMaximum());
		assertTrue("#1.4", r.getIncludeMinimum());

		r = b.intersect(a);
		assertEquals("#2.1", Version.parseVersion("raw:2.0.0"), r.getMinimum());
		assertEquals("#2.2", Version.parseVersion("raw:3.0.0"), r.getMaximum());
		assertTrue("#2.3", r.getIncludeMaximum());
		assertTrue("#2.4", r.getIncludeMinimum());

		a = new VersionRange("raw:[1.0.0,3.0.0)");
		b = new VersionRange("raw:(2.0.0,6.0.0]");
		r = a.intersect(b);
		assertEquals("#3.1", Version.parseVersion("raw:2.0.0"), r.getMinimum());
		assertEquals("#3.2", Version.parseVersion("raw:3.0.0"), r.getMaximum());
		assertTrue("#3.3", !r.getIncludeMaximum());
		assertTrue("#3.4", !r.getIncludeMinimum());

		r = b.intersect(a);
		assertEquals("#4.1", Version.parseVersion("raw:2.0.0"), r.getMinimum());
		assertEquals("#4.2", Version.parseVersion("raw:3.0.0"), r.getMaximum());
		assertTrue("#4.3", !r.getIncludeMaximum());
		assertTrue("#4.4", !r.getIncludeMinimum());

	}

	@Test
	public void testEqualRanges() {
		VersionRange a = new VersionRange("raw:[1.0.0,3.0.0]");
		VersionRange b = new VersionRange("raw:[1.0.0,3.0.0]");
		VersionRange r = a.intersect(b);
		assertEquals("#1.1", Version.parseVersion("raw:1.0.0"), r.getMinimum());
		assertEquals("#1.2", Version.parseVersion("raw:3.0.0"), r.getMaximum());
		assertTrue("#1.3", r.getIncludeMaximum());
		assertTrue("#1.4", r.getIncludeMinimum());
		r = b.intersect(a);
		assertEquals("#1.5", Version.parseVersion("raw:1.0.0"), r.getMinimum());
		assertEquals("#1.6", Version.parseVersion("raw:3.0.0"), r.getMaximum());
		assertTrue("#1.7", r.getIncludeMaximum());
		assertTrue("#1.8", r.getIncludeMinimum());

		a = new VersionRange("raw:(1.0.0,3.0.0)");
		b = new VersionRange("raw:(1.0.0,3.0.0)");
		r = a.intersect(b);
		assertEquals("#2.1", Version.parseVersion("raw:1.0.0"), r.getMinimum());
		assertEquals("#2.2", Version.parseVersion("raw:3.0.0"), r.getMaximum());
		assertTrue("#2.3", !r.getIncludeMaximum());
		assertTrue("#2.4", !r.getIncludeMinimum());
		r = b.intersect(a);
		assertEquals("#2.5", Version.parseVersion("raw:1.0.0"), r.getMinimum());
		assertEquals("#2.6", Version.parseVersion("raw:3.0.0"), r.getMaximum());
		assertTrue("#2.7", !r.getIncludeMaximum());
		assertTrue("#2.8", !r.getIncludeMinimum());
	}

	@Test
	public void testPartialEqualLower() {
		VersionRange a = new VersionRange("raw:[1.0.0,3.0.0]");
		VersionRange b = new VersionRange("raw:[1.0.0,2.0.0]");
		VersionRange r = a.intersect(b);
		assertEquals("#1.1", Version.parseVersion("raw:1.0.0"), r.getMinimum());
		assertEquals("#1.2", Version.parseVersion("raw:2.0.0"), r.getMaximum());
		assertTrue("#1.3", r.getIncludeMaximum());
		assertTrue("#1.4", r.getIncludeMinimum());
		r = b.intersect(a);
		assertEquals("#1.4", Version.parseVersion("raw:1.0.0"), r.getMinimum());
		assertEquals("#1.6", Version.parseVersion("raw:2.0.0"), r.getMaximum());
		assertTrue("#1.7", r.getIncludeMaximum());
		assertTrue("#1.8", r.getIncludeMinimum());

		b = new VersionRange("raw:[1.0.0,2.0.0)");
		r = a.intersect(b);
		assertEquals("#2.1", Version.parseVersion("raw:1.0.0"), r.getMinimum());
		assertEquals("#2.2", Version.parseVersion("raw:2.0.0"), r.getMaximum());
		assertTrue("#2.3", !r.getIncludeMaximum());
		assertTrue("#2.4", r.getIncludeMinimum());
		r = b.intersect(a);
		assertEquals("#2.4", Version.parseVersion("raw:1.0.0"), r.getMinimum());
		assertEquals("#2.6", Version.parseVersion("raw:2.0.0"), r.getMaximum());
		assertTrue("#2.7", !r.getIncludeMaximum());
		assertTrue("#2.8", r.getIncludeMinimum());

	}

	@Test
	public void testPartialEqualUpper() {
		VersionRange a = new VersionRange("raw:[1.0.0,3.0.0]");
		VersionRange b = new VersionRange("raw:[2.0.0,3.0.0]");
		VersionRange r = a.intersect(b);
		assertEquals("#1.1", Version.parseVersion("raw:2.0.0"), r.getMinimum());
		assertEquals("#1.2", Version.parseVersion("raw:3.0.0"), r.getMaximum());
		assertTrue("#1.3", r.getIncludeMaximum());
		assertTrue("#1.4", r.getIncludeMinimum());
		r = b.intersect(a);
		assertEquals("#1.4", Version.parseVersion("raw:2.0.0"), r.getMinimum());
		assertEquals("#1.6", Version.parseVersion("raw:3.0.0"), r.getMaximum());
		assertTrue("#1.7", r.getIncludeMaximum());
		assertTrue("#1.8", r.getIncludeMinimum());

		b = new VersionRange("raw:(2.0.0,3.0.0]");
		r = a.intersect(b);
		assertEquals("#2.1", Version.parseVersion("raw:2.0.0"), r.getMinimum());
		assertEquals("#2.2", Version.parseVersion("raw:3.0.0"), r.getMaximum());
		assertTrue("#2.3", r.getIncludeMaximum());
		assertTrue("#2.4", !r.getIncludeMinimum());
		r = b.intersect(a);
		assertEquals("#2.4", Version.parseVersion("raw:2.0.0"), r.getMinimum());
		assertEquals("#2.6", Version.parseVersion("raw:3.0.0"), r.getMaximum());
		assertTrue("#2.7", r.getIncludeMaximum());
		assertTrue("#2.8", !r.getIncludeMinimum());
	}

	@Test
	public void testFullyInside() {
		VersionRange a = new VersionRange("raw:[1.0.0,3.0.0]");
		VersionRange b = new VersionRange("raw:[2.0.0,2.5.0]");
		VersionRange r = a.intersect(b);
		assertEquals("#1.1", Version.parseVersion("raw:2.0.0"), r.getMinimum());
		assertEquals("#1.2", Version.parseVersion("raw:2.5.0"), r.getMaximum());
		assertTrue("#1.3", r.getIncludeMaximum());
		assertTrue("#1.4", r.getIncludeMinimum());
		r = b.intersect(a);
		assertEquals("#1.5", Version.parseVersion("raw:2.0.0"), r.getMinimum());
		assertEquals("#1.6", Version.parseVersion("raw:2.5.0"), r.getMaximum());
		assertTrue("#1.7", r.getIncludeMaximum());
		assertTrue("#1.8", r.getIncludeMinimum());

		b = new VersionRange("raw:(2.0.0,2.5.0)");
		r = a.intersect(b);
		assertEquals("#2.1", Version.parseVersion("raw:2.0.0"), r.getMinimum());
		assertEquals("#2.2", Version.parseVersion("raw:2.5.0"), r.getMaximum());
		assertTrue("#2.3", !r.getIncludeMaximum());
		assertTrue("#2.4", !r.getIncludeMinimum());

		r = b.intersect(a);
		assertEquals("#2.5", Version.parseVersion("raw:2.0.0"), r.getMinimum());
		assertEquals("#2.6", Version.parseVersion("raw:2.5.0"), r.getMaximum());
		assertTrue("#2.7", !r.getIncludeMaximum());
		assertTrue("#2.8", !r.getIncludeMinimum());
	}
}
