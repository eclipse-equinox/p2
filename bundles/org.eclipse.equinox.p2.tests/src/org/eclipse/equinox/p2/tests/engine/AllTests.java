/*******************************************************************************
 *  Copyright (c) 2007, 2012 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.engine;

import junit.framework.*;

/**
 * Performs all engine tests.
 */
public class AllTests extends TestCase {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		suite.addTestSuite(CertificateCheckerTest.class);
		suite.addTestSuite(DownloadManagerTest.class);
		suite.addTestSuite(InstructionParserTest.class);
		suite.addTestSuite(EngineTest.class);
		suite.addTestSuite(PhaseApplicabilityTest.class);
		suite.addTestSuite(PhaseSetTest.class);
		suite.addTestSuite(PhaseTest.class);
		suite.addTestSuite(ParameterizedProvisioningActionTest.class);
		suite.addTestSuite(ProfileMetadataRepositoryTest.class);
		suite.addTestSuite(ProfileTest.class);
		suite.addTestSuite(ProfilePreferencesTest.class);
		suite.addTestSuite(ProfileRegistryTest.class);
		suite.addTestSuite(ProvisioningContextTest.class);
		suite.addTestSuite(SurrogateProfileHandlerTest.class);
		suite.addTestSuite(ActionManagerTest.class);
		suite.addTestSuite(TouchpointManagerTest.class);
		suite.addTestSuite(TouchpointTest.class);
		suite.addTestSuite(ProvisioningEventTest.class);
		suite.addTestSuite(VariableTest.class);
		suite.addTestSuite(VariableTest2.class);
		suite.addTestSuite(VariableTest3.class);
		suite.addTestSuite(DebugHelperTest.class);
		return suite;
	}

}
