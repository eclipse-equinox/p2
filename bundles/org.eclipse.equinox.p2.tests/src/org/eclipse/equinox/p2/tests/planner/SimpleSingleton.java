/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.director.Explanation;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.p2.engine.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.director.PlannerStatus;
import org.eclipse.equinox.internal.provisional.p2.director.RequestStatus;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class SimpleSingleton extends AbstractProvisioningTest {
	IInstallableUnit a1;
	IInstallableUnit a2;
	IInstallableUnit a3;
	IInstallableUnit a4;
	IInstallableUnit u, v, w, x, y, z;

	IPlanner planner;
	IProfile profile;

	protected void setUp() throws Exception {
		super.setUp();
		a1 = createIU("A", Version.create("1.0.0"), true);

		a2 = createIU("A", Version.create("2.0.0"), true);

		IRequirement c1 = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[1.0.0, 1.0.0]"), null, false, false);
		IRequirement c2 = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[2.0.0, 2.0.0]"), null, false, false);
		y = createIU("Y", Version.createOSGi(2, 0, 0), new IRequirement[] {c1, c2});

		createTestMetdataRepository(new IInstallableUnit[] {a1, a2, y});

		profile = createProfile("TestProfile." + getName());
		planner = createPlanner();

	}

	public void test1() {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {y});
		ProvisioningPlan provisioningPlan = (ProvisioningPlan) planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.ERROR, provisioningPlan.getStatus().getSeverity());
		assertNull(provisioningPlan.getFutureState());
		assertNotNull(((PlannerStatus) provisioningPlan.getStatus()).getPlannedState());
	}

	public void testExplanation() {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {y});
		ProvisioningPlan plan = (ProvisioningPlan) planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.ERROR, plan.getStatus().getSeverity());
		final RequestStatus requestStatus = ((PlannerStatus) plan.getStatus()).getRequestStatus();
		assertEquals(Explanation.VIOLATED_SINGLETON_CONSTRAINT, requestStatus.getShortExplanation());
		assertTrue(requestStatus.getConflictsWithInstalledRoots().contains(y));
	}
}
