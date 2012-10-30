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
package org.eclipse.equinox.p2.tests.ui;

import junit.framework.*;

/**
 * This is the master test suite for all automated provisioning UI tests. It runs every test
 * that is suitable for running in an automated fashion as part of a build. 
 */
public class AutomatedTests extends TestCase {
	public static Test suite() {
		TestSuite suite = new TestSuite(AutomatedTests.class.getName());
		suite.addTest(org.eclipse.equinox.p2.tests.ui.operations.AllTests.suite());
		suite.addTest(org.eclipse.equinox.p2.tests.ui.query.AllTests.suite());
		suite.addTest(org.eclipse.equinox.p2.tests.ui.actions.AllTests.suite());
		suite.addTest(org.eclipse.equinox.p2.tests.ui.dialogs.AllTests.suite());
		suite.addTest(org.eclipse.equinox.p2.tests.ui.misc.AllTests.suite());
		suite.addTest(org.eclipse.equinox.p2.tests.ui.repohandling.AllTests.suite());
		suite.addTest(org.eclipse.equinox.p2.tests.importexport.AllTests.suite());
		return suite;
	}
}
