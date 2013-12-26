/*******************************************************************************
 * Copyright (c) 2013 Ericsson AB and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *     Ericsson AB - initial API and implementation
 *     Red Hat, Inc. - fragments support
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.sharedinstall;

import junit.framework.*;

/**
 * To run the shared install tests, you must perform some manual setup steps:
 * 1) Download the platform runtime binary zip (latest build or the one you want to test).
 * 2) Set the following system property to the file system path of the binary zip. For example:
 * 
 * -Dorg.eclipse.equinox.p2.reconciler.tests.platform.archive=c:/tmp/eclipse-platform-3.4-win32.zip
 */
public class AllTests extends TestCase {
	protected static final boolean WINDOWS = java.io.File.separatorChar == '\\';

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		suite.addTest(BaseChange.suite());
		if (!WINDOWS) { //don't run on windows until I will get windows machine.
			suite.addTest(BaseChangeExtendedConfigured.suite());
			suite.addTest(BaseChangeExtendedConflicts.suite());
		}
		suite.addTest(BaseChangeWithoutUserChange.suite());
		suite.addTest(Cancellation.suite());
		suite.addTest(DoubleBaseChange.suite());
		suite.addTest(InitialSharedInstall.suite());
		suite.addTest(InstallInUserSpace.suite());
		suite.addTest(MultipleChanges.suite());
		suite.addTestSuite(NeedsMigration.class);
		suite.addTestSuite(PreviousConfigurationFinderTest.class);
		suite.addTest(TestInitialRun.suite());
		suite.addTestSuite(SharedProfilePreferencesTest.class);
		suite.addTestSuite(SharedProfilePreferencesTestWithDataInUser.class);
		return suite;
	}
}
