package org.eclipse.equinox.p2.tests.sat4j.smoke;

import junit.framework.*;

public class AllTests extends TestCase {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		suite.addTestSuite(SmokeTestSAT4J.class);
		return suite;
	}
}
