/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;

import java.util.Set;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.director.Explanation;
import org.eclipse.equinox.internal.p2.engine.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class MissingDependency extends AbstractProvisioningTest {
	IInstallableUnit a1;
	IInstallableUnit b1;
	private IProfile profile;
	private IPlanner planner;

	protected void setUp() throws Exception {
		super.setUp();
		a1 = createIU("A", Version.create("1.0.0"), true);

		//Missing non optional dependency
		IRequirement[] req = new IRequirement[1];
		req[0] = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "C", VersionRange.emptyRange, null, false, false, true);
		b1 = createIU("B", Version.create("1.0.0"), req);

		createTestMetdataRepository(new IInstallableUnit[] {a1, b1});

		profile = createProfile("TestProfile." + getName());
		planner = createPlanner();
	}

	public void testContradiction() {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {a1, b1});
		IProvisioningPlan plan = planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.ERROR, plan.getStatus().getSeverity());
	}

	public void testExplanation() {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {a1, b1});
		ProvisioningPlan plan = (ProvisioningPlan) planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.ERROR, plan.getStatus().getSeverity());
		RequestStatus requestStatus = ((PlannerStatus) plan.getStatus()).getRequestStatus();
		Set explanation = requestStatus.getExplanations();
		// System.out.println(explanation);
		assertEquals(2, explanation.size());
		Set rootConflictingIUs = requestStatus.getConflictsWithInstalledRoots();
		// System.out.println(rootConflictingIUs);
		assertEquals(1, rootConflictingIUs.size());
		assertTrue(rootConflictingIUs.contains(b1));
		assertFalse(rootConflictingIUs.contains(a1));
		assertEquals(Explanation.MISSING_REQUIREMENT, requestStatus.getShortExplanation());
	}
}
