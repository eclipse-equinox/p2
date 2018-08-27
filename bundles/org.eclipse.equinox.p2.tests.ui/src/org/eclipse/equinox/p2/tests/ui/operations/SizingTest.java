/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
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
package org.eclipse.equinox.p2.tests.ui.operations;

import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;

import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.tests.ui.AbstractProvisioningUITest;

/**
 * 
 */
public class SizingTest extends AbstractProvisioningUITest {
	public void testEmptySizing() {
		String profileId = "testEmptySizing";
		IProfile testProfile = createProfile(profileId);
		ProfileChangeRequest request = new ProfileChangeRequest(testProfile);
		IProvisioningPlan plan = null;
		plan = getPlanner(getSession().getProvisioningAgent()).getProvisioningPlan(request, new ProvisioningContext(getAgent()), getMonitor());
		long size = ProvUI.SIZE_NOTAPPLICABLE;
		size = ProvUI.getSize(getEngine(), plan, new ProvisioningContext(getAgent()), getMonitor());
		assertEquals("1.0", 0, size);
	}

	/**
	 * Tests a simple sizing operation with an IU containing no artifacts
	 */
	public void testSimpleSizing() {
		IInstallableUnit f1 = createIU("f1", DEFAULT_VERSION, true);
		String profileId = "testSimpleSizing";
		IProfile testProfile = createProfile(profileId);
		ProfileChangeRequest request = new ProfileChangeRequest(testProfile);
		request.add(f1);
		IProvisioningPlan plan = null;
		plan = getPlanner(getSession().getProvisioningAgent()).getProvisioningPlan(request, new ProvisioningContext(getAgent()), getMonitor());
		long size = ProvUI.SIZE_NOTAPPLICABLE;
		size = ProvUI.getSize(getEngine(), plan, new ProvisioningContext(getAgent()), getMonitor());
		assertEquals("1.0", 0, size);
	}
}
