/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.director;

import junit.framework.*;

/**
 * Performs all automated director tests.
 */
public class AllTests extends TestCase {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		suite.addTestSuite(AutomatedDirectorTest.class);
		suite.addTestSuite(Bug203637.class);
		suite.addTestSuite(OperationGenerationTest.class);
		suite.addTestSuite(OracleTest.class);
		suite.addTestSuite(OracleTest2.class);
		suite.addTestSuite(PickerTest.class);
		suite.addTestSuite(RecommendationTest.class);
		suite.addTestSuite(ResolutionHelperTest.class);
		suite.addTestSuite(RollbackTest.class);
		suite.addTestSuite(SingletonTest.class);
		suite.addTestSuite(UninstallTest.class);
		suite.addTestSuite(UpdateTest.class);
		return suite;
	}

}
