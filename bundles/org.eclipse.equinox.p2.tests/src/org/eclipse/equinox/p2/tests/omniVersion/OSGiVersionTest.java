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
 * Tests versions specified with default OSGi version strings and tests OSGi compatibility
 * for versions specified using raw.
 *
 */
public class OSGiVersionTest extends VersionTesting {

	public void testBasicParsing() {

		// should parse without exception
		assertNotNull(Version.parseVersion("1"));
		assertNotNull(Version.parseVersion("1.0"));
		assertNotNull(Version.parseVersion("1.0.0"));
		assertNotNull(Version.parseVersion("1.0.0.9"));
		assertNotNull(Version.parseVersion("1.0.0.r12345"));
		assertNotNull(Version.parseVersion("1.0.0.r12345_hello"));
	}

	public void testOSGiStrings() {

		Version v = Version.parseVersion(null);
		assertEquals("0.0.0", v.toString());
		assertNotNull(v = Version.parseVersion(""));
		assertEquals("0.0.0", v.toString());
		assertNotNull(v = Version.parseVersion("1"));
		assertEquals("1.0.0", v.toString());
		assertNotNull(v = Version.parseVersion("1.0"));
		assertEquals("1.0.0", v.toString());
		assertNotNull(v = Version.parseVersion("1.0.0"));
		assertEquals("1.0.0", v.toString());
		assertNotNull(v = Version.parseVersion("1.0.0.9"));
		assertEquals("1.0.0.9", v.toString());
		assertNotNull(v = Version.parseVersion("1.0.0.r12345"));
		assertEquals("1.0.0.r12345", v.toString());
		assertNotNull(v = Version.parseVersion("1.0.0.r12345_hello"));
		assertEquals("1.0.0.r12345_hello", v.toString());
	}

	public void testSerialize() {

		Version v = null;
		assertNotNull(v = Version.parseVersion("1"));
		assertSerialized(v);
		assertNotNull(v = Version.parseVersion("1.0"));
		assertSerialized(v);
		assertNotNull(v = Version.parseVersion("1.0.0"));
		assertSerialized(v);
		assertNotNull(v = Version.parseVersion("1.0.0.9"));
		assertSerialized(v);
		assertNotNull(v = Version.parseVersion("1.0.0.r12345"));
		assertSerialized(v);
		assertNotNull(v = Version.parseVersion("1.0.0.r12345_hello"));
		assertSerialized(v);
	}

	public void testNegativeFirstValue() {
		try {
			Version.parseVersion("-1");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testPeriodInQualifier() {
		try {
			Version.parseVersion("1.0.0.sailor.moon");
			fail("Uncaught exception: period is not allowed in osgi qualifier");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}

	}

	public void testNegativeSecondValue() {
		try {
			Version.parseVersion("1.-1");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testNegativeThirdValue() {
		try {
			Version.parseVersion("1.0.-1");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testEmptyFourthValue() {
		try {
			Version.parseVersion("1.0.0.");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testStringFirstValue() {
		try {
			Version.parseVersion("a");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testStringSecondValue() {
		try {
			Version.parseVersion("1.a");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testStringThirdValue() {
		try {
			Version.parseVersion("1.0.a");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testSinglePeriod() {
		try {
			Version.parseVersion(".");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testTwoPeriods() {
		try {
			Version.parseVersion("..");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
	}

	public void testThreePeriods() {
		try {
			Version.parseVersion("...");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}

	}

	public void testEquality() {
		// should parse without exception
		Version v1 = Version.parseVersion("1");
		Version v2 = Version.parseVersion("1.0");
		Version v3 = Version.parseVersion("1.0.0");
		Version v4 = Version.parseVersion("1.0.0.9");
		Version v5 = Version.parseVersion("1.0.0.r12345");

		assertEquals(v1, v2);
		assertEquals(v1, v3);
		assertEquals(v2, v3);
		assertOrder(v3, v4); // i.e. not equal
		assertOrder(v4, v5); // i.e. not equal

	}

	public void testVersionCompare() {
		// should parse without exception
		Version v1 = Version.parseVersion("1");
		Version v2 = Version.parseVersion("1.0.1");
		Version v3 = Version.parseVersion("1.1");
		Version v4 = Version.parseVersion("1.1.1");
		Version v5 = Version.parseVersion("1.1.1.-");
		Version v6 = Version.parseVersion("1.2");
		Version v7 = Version.parseVersion("2");
		Version v8 = Version.parseVersion("10.0");

		assertOrder(v1, v2);
		assertOrder(v2, v3);
		assertOrder(v3, v4);
		assertOrder(v4, v5);
		assertOrder(v5, v6);
		assertOrder(v6, v7);
		assertOrder(v7, v8);

	}

	public void testCompatability() {
		Version v = Version.parseVersion("raw:1.2.3.'foo'");
		assertNotNull(v);
		assertTrue("a raw:1.2.3.'foo' compatible with OSGi", v.isOSGiCompatible());

		assertNotNull(v = Version.parseVersion("raw:1.2.3"));
		assertTrue("a raw:1.2.3 compatible with OSGi", v.isOSGiCompatible());

		assertNotNull(v = Version.parseVersion("raw:1.2.3p''"));
		assertFalse("a raw:1.2.3p'' not compatible with OSGi", v.isOSGiCompatible());

		assertNotNull(v = Version.parseVersion("raw:1.2.3.4"));
		assertFalse("a raw (4th is int) not compatible with OSGi", v.isOSGiCompatible());

		assertNotNull(v = Version.parseVersion("raw:1.2.3.'foo'.'bar'"));
		assertFalse("a raw (5 elements) not compatible with OSGi", v.isOSGiCompatible());

		assertNotNull(v = Version.parseVersion("raw:1.2"));
		assertFalse("a raw (only 2 elements) not compatible with OSGi", v.isOSGiCompatible());

		assertNotNull(v = Version.parseVersion("raw:1.2.3.' %@'"));
		assertFalse("a raw (illegal chars in qualifier) not compatible with OSGi", v.isOSGiCompatible());

	}

}
