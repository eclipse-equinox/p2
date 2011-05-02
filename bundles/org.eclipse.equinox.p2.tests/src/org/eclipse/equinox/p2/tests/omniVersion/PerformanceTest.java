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

import junit.framework.TestCase;
import org.eclipse.equinox.p2.metadata.Version;

/**
 * Simple performance comparison between OSGi version implementation and Omni Version.
 * Tests performance of creating version instances using 4 values, as well as string parsing.
 * Tests comparison of versions.
 * 
 * Aprox 10000 instances are created.
 * Comparison compares all instances against all other (i.e. about 10 milj).
 * 
 */
public class PerformanceTest extends TestCase {
	static final int MUL = 24;

	static final String qualifierTemplate = "r20090112-12345-abcdefghijklmnopqrstuvwxyz"; // longer than MUL chars

	public void testStringCreationPerformance() {
		// Ensure that classes are loaded etc.
		Version.MAX_VERSION.compareTo(Version.emptyVersion);
		org.osgi.framework.Version.emptyVersion.compareTo(org.osgi.framework.Version.emptyVersion);

		// Create all versions in string format
		String[] strings = createStrings();

		long start = System.currentTimeMillis();
		for (int idx = 0; idx < 100; ++idx)
			osgiVersionCreateFromString(strings);
		long osgiTime = System.currentTimeMillis() - start;

		start = System.currentTimeMillis();
		for (int idx = 0; idx < 100; ++idx)
			omniVersionCreateFromString(strings);
		long omniTime = System.currentTimeMillis() - start;
		outputResult("String creation", 100 * MUL * MUL * MUL, osgiTime, omniTime);
		// System.out.printf("String creation: osgi=%d, omni=%d, factor=%.2f\n", osgiTime, omniTime, factor(omniTime, osgiTime));
	}

	public void testCreationPerformance() {
		// Ensure that classes are loaded etc.
		Version.MAX_VERSION.compareTo(Version.emptyVersion);
		org.osgi.framework.Version.emptyVersion.compareTo(org.osgi.framework.Version.emptyVersion);

		long start = System.currentTimeMillis();
		for (int idx = 0; idx < 100; ++idx)
			osgiVersionCreate();
		long osgiTime = System.currentTimeMillis() - start;

		start = System.currentTimeMillis();
		for (int idx = 0; idx < 100; ++idx)
			omniVersionCreate();
		long omniTime = System.currentTimeMillis() - start;
		outputResult("Creation", 100 * MUL * MUL * MUL, osgiTime, omniTime);

		// System.out.printf("Creation: osgi=%d, omni=%d, factor=%f2\n", osgiTime, omniTime, factor(omniTime, osgiTime));
	}

	public void testComparePerformance() {
		Version[] omniVersions = createOmniVersions();
		org.osgi.framework.Version osgiVersions[] = createOsgiVersions();

		long start = System.currentTimeMillis();
		osgiVersionCompare(osgiVersions);
		long osgiTime = System.currentTimeMillis() - start;

		start = System.currentTimeMillis();
		omniVersionCompare(omniVersions);
		long omniTime = System.currentTimeMillis() - start;
		long units = MUL * MUL * MUL * MUL * MUL * MUL;
		outputResult("Compare", units, osgiTime, omniTime);

		//System.out.printf("Compare (%d comparisons): osgi=%d, omni=%d\n, factor=%d", units, osgiTime, omniTime, omniTime / osgiTime);
	}

	public void testEqualsPerformance() {
		Version[] omniVersions = createOmniVersions();
		org.osgi.framework.Version osgiVersions[] = createOsgiVersions();

		long start = System.currentTimeMillis();
		osgiVersionEquals(osgiVersions);
		long osgiTime = System.currentTimeMillis() - start;

		start = System.currentTimeMillis();
		omniVersionEquals(omniVersions);
		long omniTime = System.currentTimeMillis() - start;
		long units = MUL * MUL * MUL * MUL * MUL * MUL;
		outputResult("Equals", units, osgiTime, omniTime);

		//System.out.printf("Equals (%d comparisons): osgi=%d, omni=%d, factor=%d\n", units, osgiTime, omniTime, omniTime / osgiTime);
	}

	public void testToStringPerformance() {
		Version[] omniVersions = createOmniVersions();
		org.osgi.framework.Version osgiVersions[] = createOsgiVersions();

		long start = System.currentTimeMillis();
		for (int idx = 0; idx < 100; ++idx)
			osgiVersionToString(osgiVersions);
		long osgiTime = System.currentTimeMillis() - start;

		start = System.currentTimeMillis();
		for (int idx = 0; idx < 100; ++idx)
			omniVersionToString(omniVersions);
		long omniTime = System.currentTimeMillis() - start;
		long units = 100 * MUL * MUL * MUL;
		outputResult("To String", units, osgiTime, omniTime);

		//System.out.printf("toString (%d versions): osgi=%d, omni=%d\n", units, osgiTime, omniTime);
	}

	public static void osgiVersionToString(org.osgi.framework.Version versions[]) {
		// compare every version against all other versions
		for (int i = 0; i < MUL * MUL * MUL; i++)
			versions[i].toString();
	}

	public static void omniVersionToString(Version versions[]) {
		// compare every version against all other versions
		for (int i = 0; i < MUL * MUL * MUL; i++)
			versions[i].toString();
	}

	public static void omniVersionCreate() {
		for (int i = 0; i < MUL; i++)
			for (int j = 0; j < MUL; j++)
				for (int k = 0; k < MUL; k++)
					Version.createOSGi(i, j, k, qualifierTemplate);
	}

	public static void omniVersionCompare(Version versions[]) {
		//compare every version against all other versions
		for (int i = 0; i < MUL * MUL * MUL; i++)
			for (int j = 0; j < MUL * MUL * MUL; j++)
				versions[i].compareTo(versions[j]);
	}

	public static void omniVersionEquals(Version versions[]) {
		//compare every version against all other versions
		for (int i = 0; i < MUL * MUL * MUL; i++)
			for (int j = 0; j < MUL * MUL * MUL; j++)
				versions[i].equals(versions[j]);
	}

	public static void omniVersionCreateFromString(String[] strings) {
		int x = 0;
		for (int i = 0; i < MUL; i++)
			for (int j = 0; j < MUL; j++)
				for (int k = 0; k < MUL; k++)
					Version.create(strings[x++]);
	}

	public static void osgiVersionCompare(org.osgi.framework.Version versions[]) {
		// compare every version against all other versions
		for (int i = 0; i < MUL * MUL * MUL; i++)
			for (int j = 0; j < MUL * MUL * MUL; j++)
				versions[i].compareTo(versions[j]);
	}

	public static void osgiVersionEquals(org.osgi.framework.Version versions[]) {
		// compare every version against all other versions
		for (int i = 0; i < MUL * MUL * MUL; i++)
			for (int j = 0; j < MUL * MUL * MUL; j++)
				versions[i].equals(versions[j]);
	}

	public static void osgiVersionCreate() {
		for (int i = 0; i < MUL; i++)
			for (int j = 0; j < MUL; j++)
				for (int k = 0; k < MUL; k++)
					new org.osgi.framework.Version(i, j, k, qualifierTemplate);
	}

	public static void osgiVersionCreateFromString(String[] strings) {
		int x = 0;
		for (int i = 0; i < MUL; i++)
			for (int j = 0; j < MUL; j++)
				for (int k = 0; k < MUL; k++)
					org.osgi.framework.Version.parseVersion(strings[x++]);
	}

	/**
	 * Create a set of different versions. The execution of this method does not take part
	 * in the time measurement
	 */
	private static Version[] createOmniVersions() {
		Version versions[] = new Version[MUL * MUL * MUL];
		int x = 0;
		for (int i = 0; i < MUL; i++)
			for (int j = 0; j < MUL; j++)
				for (int k = 0; k < MUL; k++)
					versions[x++] = Version.createOSGi(i, j, k, qualifierTemplate.substring(0, k + 1));
		return versions;
	}

	/**
	 * Create a set of different versions. The execution of this method does not take part
	 * in the time measurement
	 */
	private static org.osgi.framework.Version[] createOsgiVersions() {
		org.osgi.framework.Version versions[] = new org.osgi.framework.Version[MUL * MUL * MUL];
		int x = 0;
		for (int i = 0; i < MUL; i++)
			for (int j = 0; j < MUL; j++)
				for (int k = 0; k < MUL; k++)
					versions[x++] = new org.osgi.framework.Version(i, j, k, qualifierTemplate.substring(0, k + 1));
		return versions;
	}

	/**
	 * Create a set of different version strings. The execution of this method does not take part
	 * in the time measurement
	 */
	private static String[] createStrings() {
		String[] strings = new String[MUL * MUL * MUL];
		StringBuffer buf = new StringBuffer(100);
		int x = 0;
		for (int i = 0; i < MUL; i++)
			for (int j = 0; j < MUL; j++)
				for (int k = 0; k < MUL; k++) {
					buf.setLength(0);
					buf.append(i);
					buf.append(".");
					buf.append(j);
					buf.append(".");
					buf.append(k);
					buf.append(".");
					buf.append(qualifierTemplate.substring(0, k + 1));
					strings[x++] = buf.toString();
				}
		return strings;
	}

	private static double factor(long osgiTime, long omniTime) {
		double osgi = osgiTime;
		double omni = omniTime;
		return osgi / omni;
	}

	private static void outputResult(String message, long units, long osgiTime, long omniTime) {
		System.out.printf("%s (units %d): osgi=%d [%.2fus/unit], omni=%d [%.2fus/unit], factor=%.2f\n", message, units, osgiTime, perUnit(osgiTime, units), omniTime, perUnit(omniTime, units), factor(omniTime, osgiTime));
	}

	private static double perUnit(long timeMillisec, long units) {
		double time = (timeMillisec * 1000);
		double u = units;
		return time / u;
	}
}
