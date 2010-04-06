package org.eclipse.equinox.p2.tests.planner;

import junit.framework.*;

public class AllMetaReqTests extends TestCase {
	public static Test suite() {
		TestSuite suite = new TestSuite(AllMetaReqTests.class.getName());
		suite.addTestSuite(AgentPlanTestInExternalInstance.class);
		suite.addTestSuite(AgentPlanTestInExternalInstanceForCohostedMode.class);
		suite.addTestSuite(AgentPlanTestInRunningInstance.class);
		return suite;
	}

}
