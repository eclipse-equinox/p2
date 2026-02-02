/*******************************************************************************
 *  Copyright (c) 2005, 2026 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import java.io.File;
import java.io.IOException;
import org.eclipse.equinox.internal.p2.engine.SimpleProfileRegistry;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class Bug262580 extends AbstractProvisioningTest {
	public void testRevertFeaturePatch() throws IOException {

		File testData = getTestData("test data bug 262580", "testData/bug262580");
		File tempFolder = getTempFolder();
		copy(testData, tempFolder);

		SimpleProfileRegistry testRregistry = new SimpleProfileRegistry(getAgent(), tempFolder, null, false);
		IProfile currentProfile = testRregistry.getProfile("Bug262580");
		IProfile revertProfile = testRregistry.getProfile("Bug262580", 1233157854281L);
		assertNotNull(currentProfile);
		assertNotNull(revertProfile);
		IPlanner planner = createPlanner();

		IProvisioningPlan plan = planner.getDiffPlan(currentProfile, revertProfile, getMonitor());
		assertTrue(plan.getStatus().isOK());
	}
}
