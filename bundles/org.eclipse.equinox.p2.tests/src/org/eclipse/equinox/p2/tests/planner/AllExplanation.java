/*******************************************************************************
 *  Copyright (c) 2005, 2010 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import junit.framework.*;

public class AllExplanation extends TestCase {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllExplanation.class.getName());
		suite.addTestSuite(ExplanationDeepConflict.class);
		suite.addTestSuite(ExplanationForOptionalDependencies.class);
		suite.addTestSuite(ExplanationForPartialInstallation.class);
		suite.addTestSuite(ExplanationLargeConflict.class);
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
