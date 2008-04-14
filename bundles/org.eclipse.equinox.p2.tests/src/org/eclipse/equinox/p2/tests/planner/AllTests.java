/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import junit.framework.*;

/**
 * Performs all automated planner tests.
 */
public class AllTests extends TestCase {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		suite.addTestSuite(AddIUProperty.class);
		suite.addTestSuite(AllOptional.class);
		suite.addTestSuite(AnotherSingleton.class);
		suite.addTestSuite(Bug207319.class);
		suite.addTestSuite(DependencyOnSelf.class);
		suite.addTestSuite(DropinsScenario.class);
		suite.addTestSuite(GreedyRequirement.class);
		suite.addTestSuite(InclusionRuleTest.class);
		suite.addTestSuite(InclusionRuleTest2.class);
		suite.addTestSuite(IUWithFilter.class);
		suite.addTestSuite(IUWithFilter2.class);
		suite.addTestSuite(MinimalInstall.class);
		suite.addTestSuite(MissingDependency.class);
		suite.addTestSuite(MissingDependency2.class);
		suite.addTestSuite(MissingDependency3.class);
		suite.addTestSuite(MissingNonGreedyRequirement.class);
		suite.addTestSuite(MissingNonGreedyRequirement2.class);
		suite.addTestSuite(MissingOptional.class);
		suite.addTestSuite(MissingOptionalNonGreedyRequirement.class);
		suite.addTestSuite(MissingOptionalWithDependencies.class);
		suite.addTestSuite(MissingOptionalWithDependencies2.class);
		suite.addTestSuite(MissingOptionalWithDependencies3.class);
		suite.addTestSuite(MultipleProvider.class);
		suite.addTestSuite(MultipleSingleton.class);
		suite.addTestSuite(NoRequirements.class);
		suite.addTestSuite(SimpleOptionalTest.class);
		suite.addTestSuite(SimpleOptionalTest2.class);
		suite.addTestSuite(SimpleOptionalTest3.class);
		suite.addTestSuite(SimpleSingleton.class);
		suite.addTestSuite(UninstallEverything.class);
		return suite;
	}
}