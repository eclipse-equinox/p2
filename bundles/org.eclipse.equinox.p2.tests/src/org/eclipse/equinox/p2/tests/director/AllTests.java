/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
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
		suite.addTestSuite(ReplacePlanTest.class);
		suite.addTestSuite(RollbackTest.class);
		suite.addTestSuite(SingletonTest.class);
		suite.addTestSuite(UninstallTest.class);
		suite.addTestSuite(UpdateTest.class);
		suite.addTestSuite(IUListFormatterTest.class);
		suite.addTestSuite(DirectorApplicationTest.class);
		return suite;
	}

}
