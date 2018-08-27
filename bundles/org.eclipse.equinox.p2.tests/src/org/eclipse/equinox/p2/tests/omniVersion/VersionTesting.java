/*******************************************************************************
 * Copyright (c) 2009, 2017 Cloudsmith Inc. and others.
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

import java.io.*;
import junit.framework.TestCase;
import org.eclipse.equinox.internal.p2.metadata.VersionVector;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;

/**
 * Base class for version testing. Adds useful assert methods.
 *
 */
public class VersionTesting extends TestCase {
	/**
	 * Asserts that the versionString version is included in the range.
	 * @param message
	 * @param range
	 * @param versionString
	 */
	public void assertIncludedInRange(String message, VersionRange range, String versionString) {
		assertTrue(message, range.isIncluded(Version.parseVersion(versionString)));
	}

	/**
	 * Asserts that the versionString version is not included in the range.
	 * @param message
	 * @param range
	 * @param versionString
	 */
	public void assertNotIncludedInRange(String message, VersionRange range, String versionString) {
		assertFalse(message, range.isIncluded(Version.parseVersion(versionString)));
	}

	/**
	 * A strict assertion of order.
	 * asserts that b > a, a < b, a !=b, b != a
	 * @param a
	 * @param b
	 */
	public static void assertOrder(Object a, Object b) {
		if (!(a instanceof Comparable && b instanceof Comparable))
			fail("can not assert order on non Comparable instances");
		// fully test comparison
		if (((Comparable) a).compareTo(b) > 0)
			fail("a > b");
		else if (((Comparable) b).compareTo(a) < 0)
			fail("b < a");
		else if (((Comparable) b).compareTo(a) == 0)
			fail("b == a");
		else if (((Comparable) a).compareTo(b) == 0)
			fail("a == b");

		assertTrue(true);
	}

	public static void assertPad(Version v, String rawVersionString) {
		assertNotNull(v);
		Comparable cmp = null;
		if (rawVersionString != null) {
			Version v2 = Version.create(rawVersionString);
			assertNotNull(v2);
			assertTrue(v2.getSegmentCount() == 1);
			cmp = v2.getSegment(0);
		}
		assertEquals(v.getPad(), cmp);
	}

	public void assertPadPad(Version v, String rawVersionString) {
		// TODO Auto-generated method stub
		assertNotNull(v);
		Comparable pad = v.getPad();
		assertTrue(pad instanceof VersionVector);
		Comparable cmp = null;
		if (rawVersionString != null) {
			Version v2 = Version.create(rawVersionString);
			assertNotNull(v2);
			assertTrue(v2.getSegmentCount() == 1);
			cmp = v2.getSegment(0);
		}
		assertEquals(((VersionVector) pad).getPad(), cmp);
	}

	/**
	 * Asserts serialization of a VersionRange instance.
	 * @param v
	 */
	public static void assertSerialized(VersionRange range) {
		VersionRange serialized = getSerialized(range);
		assertEquals(range, serialized);
		assertEquals(range.toString(), serialized.toString());
	}

	public static VersionRange getSerialized(VersionRange range) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (ObjectOutputStream os = new ObjectOutputStream(out);) {
			os.writeObject(range);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			fail("close of output stream failed");
		}

		ObjectInputStream is = null;
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		try {
			is = new ObjectInputStream(in);
		} catch (IOException e) {
			fail("Can not create object input stream");
		}
		VersionRange range2 = null;
		try {
			range2 = (VersionRange) is.readObject();
		} catch (IOException e) {
			fail("IO failure reading version range");
		} catch (ClassNotFoundException e) {
			fail("ClassNotFountException");
		}
		return range2;
	}

	/**
	 * Asserts serialization of a Version instance.
	 * @param v
	 */
	public static void assertSerialized(Version v) {
		Version serialized = getSerialized(v);
		assertEquals(v, serialized);
		assertEquals(v.toString(), serialized.toString());
	}

	public static Version getSerialized(Version v) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (ObjectOutputStream os = new ObjectOutputStream(out);) {
			os.writeObject(v);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			fail("close of output stream failed");
		}

		ObjectInputStream is = null;
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		try {
			is = new ObjectInputStream(in);
		} catch (IOException e) {
			fail("Can not create object input stream");
		}
		Version v2 = null;
		try {
			v2 = (Version) is.readObject();
		} catch (IOException e) {
			fail("IO failure reading version range");
		} catch (ClassNotFoundException e) {
			fail("ClassNotFountException");
		}
		return v2;

	}
}
