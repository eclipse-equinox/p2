package org.eclipse.equinox.p2.tests.planner;

import junit.framework.*;

public class AllExplanation extends TestCase {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		suite.addTestSuite(ExplanationDeepConflict.class);
		suite.addTestSuite(ExplanationForOptionalDependencies.class);
		suite.addTestSuite(ExplanationForPartialInstallation.class);
		suite.addTestSuite(ExplanationSeveralConflictingRoots.class);
		suite.addTestSuite(MissingDependency.class);
		suite.addTestSuite(MissingNonGreedyRequirement.class);
		suite.addTestSuite(MissingNonGreedyRequirement2.class);
		suite.addTestSuite(MultipleSingleton.class);
		suite.addTestSuite(PatchTest10.class);
		suite.addTestSuite(PatchTest12.class);
		return suite;
	}

}
