/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.operations;

import junit.framework.*;

/**
 * Performs all UI operation tests.
 */
public class AllTests extends TestCase {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		suite.addTestSuite(ColocatedRepositoryTrackerTest.class);
		suite.addTestSuite(SizingTest.class);
		suite.addTestSuite(InstallOperationTests.class);
		suite.addTestSuite(UpdateOperationTests.class);
		suite.addTestSuite(UninstallOperationTests.class);
		return suite;
	}
}
