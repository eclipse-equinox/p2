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

import org.eclipse.equinox.p2.metadata.IVersionFormat;
import org.eclipse.equinox.p2.metadata.Version;

/**
 * Test common patterns:
 * - Triplet
 * - Mozilla
 * - RPM
 * - JSR277 (proposed version handling as documented Dec 30, 2008).
 * 
 */
public class CommonPatternsTest extends VersionTesting {
	public static String TRIPLET_FORMAT_STRING = "n=0;[.n=0;[.n=0;]][dS=m;]";
	public static String MOZ_PREFIX = "format((<N=0;?s=m;?N=0;?s=m;?>(.<N=0;?s=m;?N=0;?s=m;?>)*)=p<0.m.0.m>;):";
	public static String TRIPLE_PREFIX = "format(" + TRIPLET_FORMAT_STRING + "):";
	public static String RPM_PREFIX = "format(<[n=0;:]a(d=[^a-zA-Z0-9@_-];?a)*>[-n[dS=!;]]):";
	public static String JSR277_PREFIX = "format(n(.n=0;){0,3}[-S=m;]):";

	public void testMozillaPattern() {

		// 1.-1
		Version v1 = Version.parseVersion(MOZ_PREFIX + "1.-1");

		// < 1 == 1. == 1.0 == 1.0.0
		Version v2 = Version.parseVersion(MOZ_PREFIX + "1");
		Version v2a = Version.parseVersion(MOZ_PREFIX + "1.");
		Version v2b = Version.parseVersion(MOZ_PREFIX + "1.0");
		Version v2c = Version.parseVersion(MOZ_PREFIX + "1.0.0");

		assertOrder(v1, v2);
		assertEquals(v2, v2a);
		assertEquals(v2a, v2b);
		assertEquals(v2b, v2c);

		// < 1.1a < 1.1aa < 1.1ab < 1.1b < 1.1c
		Version v3 = Version.parseVersion(MOZ_PREFIX + "1.1a");
		Version v4 = Version.parseVersion(MOZ_PREFIX + "1.1aa");
		Version v5 = Version.parseVersion(MOZ_PREFIX + "1.1ab");
		Version v6 = Version.parseVersion(MOZ_PREFIX + "1.1b");
		Version v7 = Version.parseVersion(MOZ_PREFIX + "1.1c");

		assertOrder(v2c, v3);
		assertOrder(v3, v4);
		assertOrder(v4, v5);
		assertOrder(v5, v6);
		assertOrder(v6, v7);

		// < 1.1pre == 1.1pre0 == 1.0+
		Version v8 = Version.parseVersion(MOZ_PREFIX + "1.1pre");
		Version v8a = Version.parseVersion(MOZ_PREFIX + "1.1pre0");

		assertOrder(v7, v8);
		assertEquals(v8, v8a);

		/* NOT SUPPORTED BY OMNI VERSION: Version v8b = Version.parseVersion(MOZ_PREFIX +"1.0+"); */

		// < 1.1pre1a < 1.1pre1aa < 1.1pre1b < 1.1pre1
		Version v9 = Version.parseVersion(MOZ_PREFIX + "1.1pre1a");
		Version v10 = Version.parseVersion(MOZ_PREFIX + "1.1pre1aa");
		Version v11 = Version.parseVersion(MOZ_PREFIX + "1.1pre1b");
		Version v12 = Version.parseVersion(MOZ_PREFIX + "1.1pre1");

		assertOrder(v8a, v9);
		assertOrder(v9, v10);
		assertOrder(v10, v11);
		assertOrder(v11, v12);

		// < 1.1pre2
		Version v13 = Version.parseVersion(MOZ_PREFIX + "1.1pre2");
		assertOrder(v12, v13);

		// < 1.1pre10
		Version v14 = Version.parseVersion(MOZ_PREFIX + "1.1pre10");
		assertOrder(v13, v14);

		//< 1.1.-1
		Version v15 = Version.parseVersion(MOZ_PREFIX + "1.1.-1");
		assertOrder(v14, v15);

		// < 1.1 == 1.1.0 == 1.1.00
		Version v16 = Version.parseVersion(MOZ_PREFIX + "1.1");
		Version v16a = Version.parseVersion(MOZ_PREFIX + "1.1.0");
		Version v16b = Version.parseVersion(MOZ_PREFIX + "1.1.00");

		assertOrder(v15, v16);
		assertEquals(v16, v16a);
		assertEquals(v16a, v16b);

		// < 1.10
		Version v17 = Version.parseVersion(MOZ_PREFIX + "1.10");
		assertOrder(v16a, v17);

		// < 1.* < 1.*.1
		/* NOT SUPPORTED BY OMNIVERSION: Version v18 = Version.parseVersion(MOZ_PREFIX +"1.1a"); */

		// < 2.0
		Version v18 = Version.parseVersion(MOZ_PREFIX + "2.0");
		assertOrder(v17, v18);
	}

	public void testMozillaPatternToString() {
		String test = MOZ_PREFIX + "1.1pre1aa";
		assertEquals(MOZ_PREFIX, Version.parseVersion(test).getFormat().toString() + ':');
	}

	public void testTripletPattern() {
		Version v1 = Version.parseVersion(TRIPLE_PREFIX + "1");
		Version v1a = Version.parseVersion(TRIPLE_PREFIX + "1.0");
		Version v1b = Version.parseVersion(TRIPLE_PREFIX + "1.0.0");
		assertEquals(v1, v1a);
		assertEquals(v1a, v1b);

		Version v2 = Version.parseVersion(TRIPLE_PREFIX + "1.0.0.a");
		assertOrder(v2, v1); // yes 1.0.0.a is OLDER

		Version v3 = Version.parseVersion(TRIPLE_PREFIX + "1.1");
		assertOrder(v1b, v3);

		Version v4 = Version.parseVersion(TRIPLE_PREFIX + "1.1.0.a");
		assertOrder(v4, v3); // yes 1.1.0.a is OLDER

		Version v5 = Version.parseVersion(TRIPLE_PREFIX + "2");
		assertOrder(v3, v5);

		Version v6 = Version.parseVersion(TRIPLE_PREFIX + "1.1-FC1");
		assertOrder(v6, v3);
	}

	public void testTripletPatternToString() {
		String test = TRIPLE_PREFIX + "1.0-FC1";
		assertEquals(TRIPLE_PREFIX, Version.parseVersion(test).getFormat().toString() + ':');
	}

	public void testTripletPatternToOSGi() throws Exception {
		IVersionFormat triplet = Version.compile(TRIPLET_FORMAT_STRING);
		assertEquals(Version.createOSGi(1, 0, 0), triplet.parse("1.0.0." + IVersionFormat.DEFAULT_MIN_STRING_TRANSLATION));
		assertEquals(Version.create("1.0.0." + IVersionFormat.DEFAULT_MAX_STRING_TRANSLATION), triplet.parse("1.0.0"));
		assertEquals(Version.createOSGi(1, 0, 0, IVersionFormat.DEFAULT_MAX_STRING_TRANSLATION), Version.create("raw:1.0.0.m"));
		assertEquals(triplet.parse("1.0"), Version.create("raw:1.0.0.m"));
		assertEquals(triplet.parse("1.0." + IVersionFormat.DEFAULT_MIN_STRING_TRANSLATION), Version.create("raw:1.0.0.''"));
		assertEquals(Version.createOSGi(1, 0, 0), Version.create("raw:1.0.0.''"));
	}

	public void testMinTranslation() throws Exception {
		IVersionFormat format = Version.compile("n=0;[.n=0;[.n=0;]][dS=m{!};]");
		assertEquals(Version.create("raw:1.0.0.''"), format.parse("1.0.0.!"));
	}

	public void testMaxTranslation() throws Exception {
		IVersionFormat format = Version.compile("n=0;[.n=0;[.n=0;]][dS=''{~,4};]");
		assertEquals(Version.create("raw:1.0.0.m"), format.parse("1.0.0.~~~~"));
	}

	// TODO: Not clear what a missing RPM EPOCH (i.e. first '.n:' should be interpreted as
	public void testRPMPattern() {
		Version v1 = Version.parseVersion(RPM_PREFIX + "33:1.2.3a-23/i386");
		assertEquals(Version.parseVersion("raw:<33.1.2.3.'a'>.23"), v1);
		Version v2 = Version.parseVersion(RPM_PREFIX + "34:1");
		assertOrder(v1, v2);

		Version v3 = Version.parseVersion(RPM_PREFIX + "33:1.2.3b");
		assertOrder(v1, v3);
		Version v11 = Version.parseVersion(RPM_PREFIX + "1-1");
		Version v12 = Version.parseVersion(RPM_PREFIX + "1-2");
		Version v13 = Version.parseVersion(RPM_PREFIX + "1.0");
		Version v14 = Version.parseVersion(RPM_PREFIX + "1.1");
		assertOrder(v11, v12);
		assertOrder(v12, v13);

		assertOrder(v11, v13);
		assertOrder(v12, v13);

		assertOrder(v11, v14);
		assertOrder(v12, v14);
		assertOrder(v13, v14);
	}

	public void testRPMPatternToString() {
		String test = RPM_PREFIX + "33:1.2.3a-23/i386";
		assertEquals(RPM_PREFIX, Version.parseVersion(test).getFormat().toString() + ':');
	}

	/**
	 * JSR277 works like triplet, but has 4 elements. The last qualifier can be used without specifying the preceding
	 * three segments.
	 */
	public void testJsr277Pattern() {
		Version v1 = Version.parseVersion(JSR277_PREFIX + "1");
		Version v1a = Version.parseVersion(JSR277_PREFIX + "1.0");
		Version v1b = Version.parseVersion(JSR277_PREFIX + "1.0.0");
		Version v1c = Version.parseVersion(JSR277_PREFIX + "1.0.0.0");
		assertEquals(v1, v1a);
		assertEquals(v1a, v1b);
		assertEquals(v1b, v1c);

		Version v2 = Version.parseVersion(JSR277_PREFIX + "1-a");
		Version v2a = Version.parseVersion(JSR277_PREFIX + "1.0-a");
		Version v2b = Version.parseVersion(JSR277_PREFIX + "1.0.0-a");
		Version v2c = Version.parseVersion(JSR277_PREFIX + "1.0.0.0-a");
		assertOrder(v2, v1); // yes 1.0.0.a is OLDER
		assertEquals(v2, v2a);
		assertEquals(v2a, v2b);
		assertEquals(v2b, v2c);

		Version v3 = Version.parseVersion(JSR277_PREFIX + "1.1");
		assertOrder(v1b, v3);

		Version v4 = Version.parseVersion(JSR277_PREFIX + "1.1.0-a");
		assertOrder(v4, v3); // yes 1.1.0.a is OLDER

		Version v5 = Version.parseVersion(JSR277_PREFIX + "2");
		assertOrder(v3, v5);
	}

	public void testJsr277PatternToString() {
		String test = JSR277_PREFIX + "1.0.0.0-a";
		assertEquals(JSR277_PREFIX, Version.parseVersion(test).getFormat().toString() + ':');
	}
}
