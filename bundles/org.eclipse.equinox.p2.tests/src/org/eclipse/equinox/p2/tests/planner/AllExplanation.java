package org.eclipse.equinox.p2.tests.planner;

import junit.framework.*;

public class AllExplanation extends TestCase {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		suite.addTestSuite(ExplanationDeepConflict.class);
		suite.addTestSuite(ExplanationForOptionalDependencies.class);
		suite.addTestSuite(ExplanationForPartialInstallation.class);
		suite.addTestSuite(ExplanationSeveralConflictingRoots.class);
		return suite;
	}

}
