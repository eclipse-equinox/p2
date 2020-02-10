/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.touchpoint.eclipse;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Performs all automated touchpoint tests.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
		EclipseTouchpointTest.class, AddJVMArgumentActionTest.class, AddProgramArgumentActionTest.class,
		AddProgramPropertyActionTest.class, AddRepositoryActionTest.class, AddSourceBundleActionTest.class,
		CheckTrustActionTest.class, ChmodActionTest.class, CollectActionTest.class, InstallBundleActionTest.class,
		InstallFeatureActionTest.class, JVMArgumentActionLogicTest.class, LinkActionTest.class,
		MarkStartedActionTest.class, PathUtilTest.class, RemoveJVMArgumentActionTest.class,
		RemoveProgramArgumentActionTest.class, RemoveProgramPropertyActionTest.class, RemoveRepositoryActionTest.class,
		RemoveSourceBundleActionTest.class, SetFrameworkDependentPropertyActionTest.class,
		SetFrameworkIndependentPropertyActionTest.class, SetLauncherNameActionTest.class,
		SetProgramPropertyActionTest.class, SetStartLevelActionTest.class, UninstallBundleActionTest.class,
		UninstallFeatureActionTest.class
})
public class AllTests {
	// test suite
}
