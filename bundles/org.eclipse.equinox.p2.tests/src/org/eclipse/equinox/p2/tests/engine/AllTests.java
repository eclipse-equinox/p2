/*******************************************************************************
 *  Copyright (c) 2007, 2012 IBM Corporation and others.
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
package org.eclipse.equinox.p2.tests.engine;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * Performs all engine tests.
 */
@Suite
@SelectClasses({
		CertificateCheckerTest.class, DownloadManagerTest.class, InstructionParserTest.class, EngineTest.class,
		PhaseApplicabilityTest.class, PhaseSetTest.class, PhaseTest.class, ParameterizedProvisioningActionTest.class,
		ProfileMetadataRepositoryTest.class, ProfileTest.class, ProfilePreferencesTest.class, ProfileRegistryTest.class,
		ProvisioningContextTest.class, SurrogateProfileHandlerTest.class, ActionManagerTest.class,
		TouchpointManagerTest.class, TouchpointTest.class, ProvisioningEventTest.class, VariableTest.class,
		VariableTest2.class, VariableTest3.class, DebugHelperTest.class
})
public class AllTests {
// test suite
}
