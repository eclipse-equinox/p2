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

import java.io.*;
import junit.framework.TestCase;
import org.eclipse.equinox.internal.provisional.p2.core.*;

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

	//	/**
	//	 * Assert that pad of v is the same as the single element raw version string
	//	 * @param v
	//	 * @param rawVersionString
	//	 */
	//	public static void assertPad(VersionVector v, String rawVersionString) {
	//		assertNotNull(v);
	//		assertNotNull(rawVersionString);
	//		Version v2 = Version.parseVersion(rawVersionString);
	//		assertNotNull(v2);
	//
	//		assertEquals(v.getPad(), v2);
	//	}

	/**
	 * Assert that pad of v is the same as the single element raw version string
	 * @param v
	 * @param rawVersionString
	 */
	public static void assertPad(Comparable v, String rawVersionString) {
		assertTrue(v instanceof VersionVector);
		assertNotNull(rawVersionString);
		Version v2 = Version.parseVersion(rawVersionString);
		assertNotNull(v2);

		assertEquals(((VersionVector) v).getPad(), v2.getSegment(0));
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
		ObjectOutputStream os = null;
		try {
			os = new ObjectOutputStream(out);
		} catch (IOException e) {
			e.printStackTrace();
			fail("IOException creating ObjectOutputStream");
		}
		try {
			os.writeObject(range);

		} catch (NotSerializableException e) {
			fail("Impl of VersionRange is wrong - it is not serializeable");
		} catch (IOException e) {
			e.printStackTrace();
			fail("write of objectfailed");
		}
		try {
			os.close();
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
		ObjectOutputStream os = null;
		try {
			os = new ObjectOutputStream(out);
		} catch (IOException e) {
			fail("IOException creating ObjectOutputStream");
		}
		try {
			os.writeObject(v);
		} catch (NotSerializableException e) {
			e.printStackTrace();
			fail("Impl of Version is wrong - it is not serializeable");
		} catch (IOException e) {
			fail("write of objectfailed");
		}
		try {
			os.close();
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
