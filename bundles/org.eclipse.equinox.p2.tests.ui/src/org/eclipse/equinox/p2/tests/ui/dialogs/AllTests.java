/*******************************************************************************
 *  Copyright (c) 2008, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.dialogs;

import junit.framework.*;

/**
 * Performs all UI wizard and dialog tests.
 */
public class AllTests extends TestCase {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		suite.addTestSuite(InstallWizardTest.class);
		suite.addTestSuite(InstalledSoftwarePageTest.class);
		suite.addTestSuite(InstallWithRemediationTest.class);
		suite.addTestSuite(InstallationHistoryPageTest.class);
		suite.addTestSuite(UpdateWizardTest.class);
		suite.addTestSuite(UninstallWizardTest.class);
		suite.addTestSuite(RepositoryManipulationPageTest.class);
		suite.addTestSuite(IUPropertyPagesTest.class);
		suite.addTestSuite(PreferencePagesTest.class);
		return suite;
	}
}
