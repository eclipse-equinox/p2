/*******************************************************************************
 *  Copyright (c) 2005, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import java.io.File;
import org.eclipse.equinox.internal.p2.engine.SimpleProfileRegistry;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class Bug262580 extends AbstractProvisioningTest {
	public void testRevertFeaturePatch() {

		File testData = getTestData("test data bug 262580", "testData/bug262580");
		File tempFolder = getTempFolder();
		copy("0.2", testData, tempFolder);

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
