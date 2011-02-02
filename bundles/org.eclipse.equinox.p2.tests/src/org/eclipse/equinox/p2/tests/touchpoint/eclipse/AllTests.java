/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.touchpoint.eclipse;

import junit.framework.*;

/**
 * Performs all automated touchpoint tests.
 */
public class AllTests extends TestCase {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		suite.addTestSuite(EclipseTouchpointTest.class);
		suite.addTestSuite(AddJVMArgumentActionTest.class);
		suite.addTestSuite(AddProgramArgumentActionTest.class);
		suite.addTestSuite(AddProgramPropertyActionTest.class);
		suite.addTestSuite(AddRepositoryActionTest.class);
		suite.addTestSuite(AddSourceBundleActionTest.class);
		suite.addTestSuite(CheckTrustActionTest.class);
		suite.addTestSuite(ChmodActionTest.class);
		suite.addTestSuite(CollectActionTest.class);
		suite.addTestSuite(InstallBundleActionTest.class);
		suite.addTestSuite(InstallFeatureActionTest.class);
		suite.addTestSuite(JVMArgumentActionLogicTest.class);
		suite.addTestSuite(LinkActionTest.class);
		suite.addTestSuite(MarkStartedActionTest.class);
		suite.addTestSuite(PathUtilTest.class);
		suite.addTestSuite(RemoveJVMArgumentActionTest.class);
		suite.addTestSuite(RemoveProgramArgumentActionTest.class);
		suite.addTestSuite(RemoveProgramPropertyActionTest.class);
		suite.addTestSuite(RemoveRepositoryActionTest.class);
		suite.addTestSuite(RemoveSourceBundleActionTest.class);
		suite.addTestSuite(SetFrameworkDependentPropertyActionTest.class);
		suite.addTestSuite(SetFrameworkIndependentPropertyActionTest.class);
		suite.addTestSuite(SetLauncherNameActionTest.class);
		suite.addTestSuite(SetProgramPropertyActionTest.class);
		suite.addTestSuite(SetStartLevelActionTest.class);
		suite.addTestSuite(UninstallBundleActionTest.class);
		suite.addTestSuite(UninstallFeatureActionTest.class);
		return suite;
	}
}
