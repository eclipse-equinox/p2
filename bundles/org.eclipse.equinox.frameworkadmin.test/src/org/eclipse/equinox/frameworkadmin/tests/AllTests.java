/*******************************************************************************
 *  Copyright (c) 2008, 2013 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Ericsson AB - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.frameworkadmin.tests;


import junit.framework.*;

/**
 * Performs all automated director tests.
 */
public class AllTests extends TestCase {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		suite.addTestSuite(Bug196525.class);
		suite.addTestSuite(Bug258370.class);
		suite.addTestSuite(CleanupTest.class);
		suite.addTestSuite(FrameworkExtensionTest.class);
		suite.addTestSuite(LauncherConfigLocationTest.class);
		suite.addTestSuite(LauncherDataTest.class);
		suite.addTestSuite(ManipulatorTests.class);
		suite.addTestSuite(NoConfigurationValueInEclipseIni.class);
		suite.addTestSuite(NoRenamingLauncherIni.class);
		suite.addTestSuite(OSGiVersionChange.class);
		suite.addTestSuite(ParserUtilsTest.class);
		suite.addTestSuite(ReaderTest1.class);
		suite.addTestSuite(ReaderTest2.class);
		suite.addTestSuite(ReaderTest3.class);
		suite.addTestSuite(ReaderTest4.class);
		suite.addTestSuite(ReaderTest5.class);
		suite.addTestSuite(ReaderTestBug267850.class);
		suite.addTestSuite(ReaderTestBug285935.class);
		suite.addTestSuite(RelativePathTest.class);
		suite.addTestSuite(RemovingABundle.class);
		suite.addTestSuite(RemovingAllBundles.class);
		suite.addTestSuite(RenamingLauncherIni.class);
		suite.addTestSuite(SharedConfigurationTest.class);
		suite.addTestSuite(SimpleConfiguratorComingAndGoing.class);
		suite.addTestSuite(SimpleConfiguratorTest.class);
		suite.addTestSuite(TestEclipseDataArea.class);
		suite.addTestSuite(TestRunningInstance.class);
		suite.addTestSuite(TestVMArg.class);
		suite.addTestSuite(UtilsTest.class);
		return suite;
	}

}