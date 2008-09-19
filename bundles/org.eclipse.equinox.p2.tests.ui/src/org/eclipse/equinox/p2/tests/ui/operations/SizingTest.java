/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.operations;

import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;
import org.eclipse.equinox.internal.provisional.p2.engine.phases.Sizing;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * 
 */
public class SizingTest extends AbstractProvisioningTest {
	public void testEmptySizing() {
		String profileId = "testEmptySizing";
		IProfile profile = createProfile(profileId);
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		ProvisioningPlan plan = null;
		try {
			plan = ProvisioningUtil.getProvisioningPlan(request, new ProvisioningContext(), getMonitor());
		} catch (ProvisionException e) {
			fail("0.99", e);
			return;
		}
		Sizing sizing = null;
		try {
			sizing = ProvisioningUtil.getSizeInfo(plan, profileId, getMonitor());
		} catch (ProvisionException e) {
			fail("1.99", e);
		}
		assertEquals("1.0", 0, sizing.getDiskSize());
		assertEquals("1.1", 0, sizing.getDlSize());
	}

	/**
	 * Tests a simple sizing operation with an IU containing no artifacts
	 */
	public void testSimpleSizing() {
		IInstallableUnit f1 = createIU("f1", DEFAULT_VERSION, true);
		String profileId = "testSimpleSizing";
		IProfile profile = createProfile(profileId);
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(new IInstallableUnit[] {f1});
		ProvisioningPlan plan = null;
		try {
			plan = ProvisioningUtil.getProvisioningPlan(request, new ProvisioningContext(), getMonitor());
		} catch (ProvisionException e) {
			fail("0.99", e);
			return;
		}
		Sizing sizing = null;
		try {
			sizing = ProvisioningUtil.getSizeInfo(plan, profileId, getMonitor());
		} catch (ProvisionException e) {
			fail("1.99", e);
		}
		assertEquals("1.0", 0, sizing.getDiskSize());
		assertEquals("1.1", 0, sizing.getDlSize());
	}
}
