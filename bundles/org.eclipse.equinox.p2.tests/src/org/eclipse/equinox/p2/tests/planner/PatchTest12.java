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
import org.eclipse.equinox.p2.planner.ProfileInclusionRules;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class PatchTest12 extends AbstractProvisioningTest {
	IInstallableUnit a1;
	IInstallableUnit b1;
	IInstallableUnit b2;
	IInstallableUnitPatch p1;

	IProfile profile1;
	IPlanner planner;
	IEngine engine;

	protected void setUp() throws Exception {
		super.setUp();
		a1 = createIU("A", Version.create("1.0.0"), new IRequirement[] {MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.0.0, 1.1.0)"), null, false, true, true)});
		b1 = createIU("B", Version.createOSGi(1, 0, 0), true);
		b2 = createIU("B", Version.createOSGi(1, 2, 0), true);
		IRequirementChange change = MetadataFactory.createRequirementChange(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", VersionRange.emptyRange, null, false, false, false), MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.4.0, 1.5.0)"), null, false, true, true));
		p1 = createIUPatch("P", Version.create("1.0.0"), true, new IRequirementChange[] {change}, new IRequirement[][] {{MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", VersionRange.emptyRange, null, false, false)}}, null);

		createTestMetdataRepository(new IInstallableUnit[] {a1, b1, b2, p1});

		profile1 = createProfile("TestProfile." + getName());
		planner = createPlanner();
		engine = createEngine();
	}

	public void testInstallBogusInstallFilterInPatch() {
		//Try to install a1 and p1 optionally
		//p1 ends up being not installed because the new requirements that it sets on A for B can not be met (no B are matching).
		//the only thing that ends up being installed are A 1.0.0 and B 1.0.0
		ProfileChangeRequest req2 = new ProfileChangeRequest(profile1);
		req2.addInstallableUnits(new IInstallableUnit[] {a1, p1});
		req2.setInstallableUnitInclusionRules(p1, ProfileInclusionRules.createOptionalInclusionRule(p1));
		IProvisioningPlan plan2 = planner.getProvisioningPlan(req2, null, null);
		assertTrue(IStatus.ERROR != plan2.getStatus().getSeverity());
		assertNoOperand(plan2, p1);
		assertNoOperand(plan2, b2);
		assertNoOperand(plan2, p1);
		assertInstallOperand(plan2, a1);
		assertInstallOperand(plan2, b1);

		//Try to install a1 and p1. This should fail because the patch adds an invalid filter 
		ProfileChangeRequest req3 = new ProfileChangeRequest(profile1);
		req3.addInstallableUnits(new IInstallableUnit[] {a1, p1});
		IProvisioningPlan plan3 = planner.getProvisioningPlan(req3, null, null);
		assertTrue(IStatus.ERROR == plan3.getStatus().getSeverity());

	}

	public void testExplanation1() {
		ProfileChangeRequest req3 = new ProfileChangeRequest(profile1);
		req3.addInstallableUnits(new IInstallableUnit[] {a1, p1});
		ProvisioningPlan plan3 = (ProvisioningPlan) planner.getProvisioningPlan(req3, null, null);
		assertTrue(IStatus.ERROR == plan3.getStatus().getSeverity());
		final RequestStatus requestStatus = ((PlannerStatus) plan3.getStatus()).getRequestStatus();
		Set conflictRootIUs = requestStatus.getConflictsWithInstalledRoots();
		assertTrue(conflictRootIUs.contains(p1));
		assertEquals(Explanation.MISSING_REQUIREMENT, requestStatus.getShortExplanation());
	}
}
