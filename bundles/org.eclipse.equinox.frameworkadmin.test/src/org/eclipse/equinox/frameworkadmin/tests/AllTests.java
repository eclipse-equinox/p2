/*******************************************************************************
 *  Copyright (c) 2008, 2013 IBM Corporation and others.
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
 *     Ericsson AB - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.frameworkadmin.tests;


import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Performs all automated director tests.
 */
@RunWith(Suite.class)				
@Suite.SuiteClasses({	
	Bug196525.class,
	Bug258370.class,
	CleanupTest.class,
	FrameworkExtensionTest.class,
	LauncherConfigLocationTest.class,
	LauncherDataTest.class,
	ManipulatorTests.class,
	NoConfigurationValueInEclipseIni.class,
	NoRenamingLauncherIni.class,
	OSGiVersionChange.class,
	ParserUtilsTest.class,
	ReaderTest1.class,
	ReaderTest2.class,
	ReaderTest3.class,
	ReaderTest4.class,
	ReaderTest5.class,
	ReaderTestBug267850.class,
	ReaderTestBug285935.class,
	RelativePathTest.class,
	RemovingABundle.class,
	RemovingAllBundles.class,
	RenamingLauncherIni.class,
	SharedConfigurationTest.class,
	SimpleConfiguratorComingAndGoing.class,
	SimpleConfiguratorTest.class,
	TestEclipseDataArea.class,
	TestRunningInstance.class,
	TestVMArg.class,
	UtilsTest.class
})
public class AllTests {
	// SuiteClasses
}