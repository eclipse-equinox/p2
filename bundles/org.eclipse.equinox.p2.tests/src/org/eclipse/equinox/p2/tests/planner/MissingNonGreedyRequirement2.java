/*******************************************************************************
 *  Copyright (c) 2008, 2017 IBM Corporation and others.
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

import java.util.Set;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.director.Explanation;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.p2.engine.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.director.PlannerStatus;
import org.eclipse.equinox.internal.provisional.p2.director.RequestStatus;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class MissingNonGreedyRequirement2 extends AbstractProvisioningTest {
	private IInstallableUnit a1;
	private IInstallableUnit c1;
	private IProfile profile;
	private IPlanner planner;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		c1 = createIU("C", Version.create("1.0.0"), true);

		IRequirement[] reqB = new IRequirement[2];
		reqB[0] = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", VersionRange.emptyRange, null, false, false, false);
		reqB[1] = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "C", VersionRange.emptyRange, null, true, false, true);
		a1 = createIU("A", Version.create("1.0.0"), reqB);

		createTestMetdataRepository(new IInstallableUnit[] {a1, c1});

		profile = createProfile("TestProfile." + getName());
		planner = createPlanner();
	}

	public void testInstall() {
		//The planner returns an error because the requirement from A on B is non greedy and no one brings in B.
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(a1);
		IProvisioningPlan plan = planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.ERROR, plan.getStatus().getSeverity());
	}

	public void testExplanation() {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(a1);
		ProvisioningPlan plan = (ProvisioningPlan) planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.ERROR, plan.getStatus().getSeverity());
		RequestStatus requestStatus = ((PlannerStatus) plan.getStatus()).getRequestStatus();
		Set<Explanation> explanation = requestStatus.getExplanations();
		assertFalse(explanation.isEmpty());
		assertTrue(requestStatus.getConflictsWithInstalledRoots().contains(a1));
		assertEquals(Explanation.MISSING_REQUIREMENT, requestStatus.getShortExplanation());

	}
}
