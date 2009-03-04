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

import junit.framework.*;

/**
 * Tests the OmniVersion implementation of Version and VersionRange.
 * 
 */
public class AllTests extends TestCase {
	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		suite.addTestSuite(CommonPatternsTest.class);
		suite.addTestSuite(FormatArrayTest.class);
		suite.addTestSuite(FormatATest.class);
		suite.addTestSuite(FormatDTest.class);
		suite.addTestSuite(FormatNTest.class);
		suite.addTestSuite(FormatProcessingTest.class);
		suite.addTestSuite(FormatPTest.class);
		suite.addTestSuite(FormatQTest.class);
		suite.addTestSuite(FormatRTest.class);
		suite.addTestSuite(FormatSTest.class);
		suite.addTestSuite(FormatTest.class);
		suite.addTestSuite(FormatRangeTest.class);
		suite.addTestSuite(MultiplicityTest.class);
		suite.addTestSuite(OSGiRangeTest.class);
		suite.addTestSuite(OSGiVersionTest.class);
		suite.addTestSuite(RawRangeTest.class);
		suite.addTestSuite(RawRangeWithOriginalTest.class);
		suite.addTestSuite(RawVersionTest.class);
		suite.addTestSuite(RawWithOriginalTest.class);
		suite.addTestSuite(IntersectionTest.class);

		return suite;
	}

}
