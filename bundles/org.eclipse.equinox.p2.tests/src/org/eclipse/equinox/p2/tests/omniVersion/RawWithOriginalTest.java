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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import org.eclipse.equinox.p2.metadata.Version;
import org.junit.Test;

/**
 * Tests inclusion of original version string in the raw format.
 * The tests in this class does not fully test the various "format(rules)" only that the sequence "raw/format():original"
 * works, and that errors at the top level are caught.
 *
 */
public class RawWithOriginalTest extends VersionTesting {

	@Test
	public void testRawWithUnknownFormat() {
		Version v = Version.parseVersion("raw:1.0/:silver.moon");
		assertNotNull(v);
		assertEquals(v, Version.parseVersion("raw:1.0"));
	}

	@Test
	public void testRawWithUnknownFormatToString() {
		assertEquals("raw:1.0/:silver.moon", Version.parseVersion("raw:1.0/:silver.moon").toString());
	}

	@Test
	public void testRawWithUnknownFormatSerialized() {
		assertSerialized(Version.parseVersion("raw:1.0/:silver.moon"));
		assertEquals("raw:1.0/:silver.moon", getSerialized(Version.create("raw:1.0/:silver.moon")).toString());

	}

	@Test
	public void testRawWithSimpleFormat() {
		Version v = Version.parseVersion("raw:1.0/format(n.n):1.0");
		assertNotNull(v);
		assertEquals(v, Version.parseVersion("raw:1.0"));
	}

	@Test
	public void testRawWithSimpleFormatToString() {
		assertEquals("raw:1.0/format(n.n):1.0", Version.parseVersion("raw:1.0/format(n.n):1.0").toString());
	}

	@Test
	public void testRawWithSimpleFormatSerialized() {
		assertSerialized(Version.parseVersion("raw:1.0/format(n.n):1.0"));
		assertEquals("raw:1.0/format(n.n):1.0", getSerialized(Version.create("raw:1.0/format(n.n):1.0")).toString());
	}

	@Test
	public void testOriginalStatedButMissing() {
			assertThrows(IllegalArgumentException.class, () -> Version.parseVersion("raw:1.0/"));
	}

	@Test
	public void testOriginalAndUnknownStatedButMissing() {
		assertThrows(IllegalArgumentException.class, () -> Version.parseVersion("raw:1.0/:"));
	}

	@Test
	public void testOriginalIllegalFormat() {
		assertThrows(IllegalArgumentException.class, () -> Version.parseVersion("raw:1.0/foo:"));
	}

	@Test
	public void testOriginalIllegalFormat2() {
		assertThrows(IllegalArgumentException.class, () -> Version.parseVersion("raw:1.0/100:"));
	}

	@Test
	public void testOriginalIllegalFormat3() {
		assertThrows(IllegalArgumentException.class, () -> Version.parseVersion("raw:1.0/'format':"));
	}

	@Test
	public void testOriginalIllegalFormat4() {
		assertThrows(IllegalArgumentException.class, () -> Version.parseVersion("raw:1.0//1.0"));
	}

	@Test
	public void testOriginalIllegalFormat5() {
		assertThrows(IllegalArgumentException.class, () -> Version.parseVersion("raw:1.0/format:"));
	}

	@Test
	public void testOriginalFormatUnbalancedLeft() {
		assertThrows(IllegalArgumentException.class, () -> Version.parseVersion("raw:1.0/formatn.n):"));
	}

	@Test
	public void testOriginalFormatUnbalancedRight() {
		assertThrows(IllegalArgumentException.class, () -> Version.parseVersion("raw:1.0/format(n.n:1.0"));
	}

	@Test
	public void testOriginalFormatOriginalMissing() {
		assertThrows(IllegalArgumentException.class, () ->Version.parseVersion("raw:1.0/format(n.n):"));
	}
}
