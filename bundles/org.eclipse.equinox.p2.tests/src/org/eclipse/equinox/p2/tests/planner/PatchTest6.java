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
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class PatchTest6 extends AbstractProvisioningTest {
	IInstallableUnit a1;
	IInstallableUnit b1;
	IInstallableUnit c1;
	IInstallableUnit d1;

	IInstallableUnitPatch p1;
	IInstallableUnitPatch pp1;

	IProfile profile1;
	IPlanner planner;
	IEngine engine;

	protected void setUp() throws Exception {
		super.setUp();
		a1 = createIU("A", Version.create("1.0.0"), new IRequirement[] {MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.0.0, 1.1.0)"), null, false, true)});
		b1 = createIU("B", Version.createOSGi(1, 2, 0), true);

		c1 = createIU("C", Version.create("1.0.0"), new IRequirement[] {MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "D", new VersionRange("[1.0.0, 1.1.0)"), null, false, true)});
		d1 = createIU("D", Version.createOSGi(1, 2, 0), true);

		IRequirementChange changeA = MetadataFactory.createRequirementChange(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", VersionRange.emptyRange, null, false, false, false), MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.1.0, 1.3.0)"), null, false, false, true));
		IRequirement[][] scopeP1 = new IRequirement[][] {{MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", VersionRange.emptyRange, null, false, false, false)}};
		IRequirement[] reqOnPP = new IRequirement[] {MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "PP", new VersionRange("[1.0.0, 2.0.0)"), null, false, false, true)};
		p1 = createIUPatch("P", Version.create("1.0.0"), null, reqOnPP, NO_PROVIDES, NO_PROPERTIES, ITouchpointType.NONE, NO_TP_DATA, false, null, new IRequirementChange[] {changeA}, scopeP1, null, new IRequirement[0]);

		IRequirementChange changeC = MetadataFactory.createRequirementChange(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "D", VersionRange.emptyRange, null, false, false, false), MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "D", new VersionRange("[1.1.0, 1.3.0)"), null, false, false, true));
		IRequirement[][] scopePP1 = new IRequirement[][] {{MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "C", VersionRange.emptyRange, null, false, false, false)}};
		pp1 = createIUPatch("PP", Version.create("1.0.0"), true, new IRequirementChange[] {changeC}, scopePP1, null);
		createTestMetdataRepository(new IInstallableUnit[] {a1, b1, c1, d1, p1, pp1});

		profile1 = createProfile("TestProfile." + getName());
		planner = createPlanner();
		engine = createEngine();
	}

	public void testInstall() {
		//Confirm that a1 and c1 can't be installed
		ProfileChangeRequest req1 = new ProfileChangeRequest(profile1);
		req1.addInstallableUnits(new IInstallableUnit[] {a1, c1});
		IProvisioningPlan plan1 = planner.getProvisioningPlan(req1, null, null);
		assertEquals(IStatus.ERROR, plan1.getStatus().getSeverity());

		//Verify that the installation of c1 and pp1 succeed
		ProfileChangeRequest req2 = new ProfileChangeRequest(profile1);
		req2.addInstallableUnits(new IInstallableUnit[] {c1, pp1});
		IProvisioningPlan plan2 = planner.getProvisioningPlan(req2, null, null);
		assertEquals(IStatus.OK, plan2.getStatus().getSeverity());

		//Verify that p1 can be installed alone (kind of meaningless)
		ProfileChangeRequest req3 = new ProfileChangeRequest(profile1);
		req3.addInstallableUnits(new IInstallableUnit[] {p1});
		IProvisioningPlan plan3 = planner.getProvisioningPlan(req3, null, null);
		assertEquals(IStatus.OK, plan3.getStatus().getSeverity());

		//Install a1 and p1. 
		ProfileChangeRequest req4 = new ProfileChangeRequest(profile1);
		req4.addInstallableUnits(new IInstallableUnit[] {a1, p1});
		IProvisioningPlan plan4 = planner.getProvisioningPlan(req4, null, null);
		assertEquals(IStatus.OK, plan4.getStatus().getSeverity());
		assertInstallOperand(plan4, a1);
		assertInstallOperand(plan4, p1);
		assertInstallOperand(plan4, pp1);
		assertInstallOperand(plan4, b1);

		//Install a1, c1 and p1. 
		ProfileChangeRequest req5 = new ProfileChangeRequest(profile1);
		req5.addInstallableUnits(new IInstallableUnit[] {a1, c1, p1});
		IProvisioningPlan plan5 = planner.getProvisioningPlan(req5, null, null);
		assertEquals(IStatus.OK, plan5.getStatus().getSeverity());
		assertInstallOperand(plan4, a1);
		assertInstallOperand(plan4, p1);
		assertInstallOperand(plan4, b1);
		assertInstallOperand(plan4, pp1);
		assertInstallOperand(plan5, d1);
		assertInstallOperand(plan5, c1);
	}

	public void testExplanation1() {
		//Confirm that a1 and c1 can't be installed
		ProfileChangeRequest req1 = new ProfileChangeRequest(profile1);
		req1.addInstallableUnits(new IInstallableUnit[] {a1, c1});
		ProvisioningPlan plan1 = (ProvisioningPlan) planner.getProvisioningPlan(req1, null, null);
		assertEquals(IStatus.ERROR, plan1.getStatus().getSeverity());
		final RequestStatus requestStatus = ((PlannerStatus) plan1.getStatus()).getRequestStatus();
		assertEquals(Explanation.MISSING_REQUIREMENT, requestStatus.getShortExplanation());
		Set conflictingRoots = requestStatus.getConflictsWithInstalledRoots();
		assertEquals(1, conflictingRoots.size());
		assertTrue(conflictingRoots.contains(a1) || conflictingRoots.contains(c1));
	}
}
