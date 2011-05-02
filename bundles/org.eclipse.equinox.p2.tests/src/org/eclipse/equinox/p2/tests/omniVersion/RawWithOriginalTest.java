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

/**
 * Tests inclusion of original version string in the raw format.
 * The tests in this class does not fully test the various "format(rules)" only that the sequence "raw/format():original"
 * works, and that errors at the top level are caught.
 * 
 */
public class RawWithOriginalTest extends VersionTesting {

	public void testRawWithUnknownFormat() {
		Version v = Version.parseVersion("raw:1.0/:silver.moon");
		assertNotNull(v);
		assertEquals(v, Version.parseVersion("raw:1.0"));
	}

	public void testRawWithUnknownFormatToString() {
		assertEquals("raw:1.0/:silver.moon", Version.parseVersion("raw:1.0/:silver.moon").toString());
	}

	public void testRawWithUnknownFormatSerialized() {
		assertSerialized(Version.parseVersion("raw:1.0/:silver.moon"));
		assertEquals("raw:1.0/:silver.moon", getSerialized(Version.create("raw:1.0/:silver.moon")).toString());

	}

	public void testRawWithSimpleFormat() {
		Version v = Version.parseVersion("raw:1.0/format(n.n):1.0");
		assertNotNull(v);
		assertEquals(v, Version.parseVersion("raw:1.0"));
	}

	public void testRawWithSimpleFormatToString() {
		assertEquals("raw:1.0/format(n.n):1.0", Version.parseVersion("raw:1.0/format(n.n):1.0").toString());
	}

	public void testRawWithSimpleFormatSerialized() {
		assertSerialized(Version.parseVersion("raw:1.0/format(n.n):1.0"));
		assertEquals("raw:1.0/format(n.n):1.0", getSerialized(Version.create("raw:1.0/format(n.n):1.0")).toString());
	}

	public void testOriginalStatedButMissing() {
		try {
			Version.parseVersion("raw:1.0/");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testOriginalAndUnknownStatedButMissing() {
		try {
			Version.parseVersion("raw:1.0/:");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testOriginalIllegalFormat() {
		try {
			Version.parseVersion("raw:1.0/foo:");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testOriginalIllegalFormat2() {
		try {
			Version.parseVersion("raw:1.0/100:");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testOriginalIllegalFormat3() {
		try {
			Version.parseVersion("raw:1.0/'format':");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testOriginalIllegalFormat4() {
		try {
			Version.parseVersion("raw:1.0//1.0");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testOriginalIllegalFormat5() {
		try {
			Version.parseVersion("raw:1.0/format:");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testOriginalFormatUnbalancedLeft() {
		try {
			Version.parseVersion("raw:1.0/formatn.n):");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testOriginalFormatUnbalancedRight() {
		try {
			Version.parseVersion("raw:1.0/format(n.n:1.0");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testOriginalFormatOriginalMissing() {
		try {
			Version.parseVersion("raw:1.0/format(n.n):");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}
}
