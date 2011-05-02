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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class PatchTest8 extends AbstractProvisioningTest {
	IInstallableUnit a1;
	IInstallableUnit a2;
	IInstallableUnit b1;
	IInstallableUnit b2;
	IInstallableUnit c2;
	IInstallableUnit f1;

	IInstallableUnitPatch p1;
	IInstallableUnitPatch r1;

	IProfile profile1;
	IPlanner planner;
	IEngine engine;

	protected void setUp() throws Exception {
		super.setUp();
		a1 = createIU("A", Version.createOSGi(1, 0, 0), true);
		a2 = createIU("A", Version.create("2.0.0"), true);
		b1 = createIU("B", Version.create("1.0.0"), true);
		b2 = createIU("B", Version.create("2.0.0"), true);
		c2 = createIU("C", Version.create("2.0.0"), true);

		IRequirement[] req = new IRequirement[3];
		req[0] = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[1.0.0, 1.1.0)"), null, false, true);
		req[1] = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.0.0, 1.1.0)"), null, false, true);
		req[2] = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "C", new VersionRange("[2.0.0, 3.1.0)"), null, false, true);
		f1 = createIU("F", Version.createOSGi(1, 0, 0), req);

		IRequirementChange changeA = MetadataFactory.createRequirementChange(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", VersionRange.emptyRange, null, false, false, false), MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[2.0.0, 3.0.0)"), null, false, false, true));
		IRequirement[][] scope = new IRequirement[][] {{MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "F", VersionRange.emptyRange, null, false, false, false)}};
		p1 = createIUPatch("P", Version.create("1.0.0"), null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, ITouchpointType.NONE, NO_TP_DATA, false, null, new IRequirementChange[] {changeA}, scope, null, new IRequirement[0]);

		IRequirementChange changeB = MetadataFactory.createRequirementChange(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", VersionRange.emptyRange, null, false, false, false), MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[2.0.0, 3.0.0)"), null, false, false, true));
		IRequirement[][] scopePP = new IRequirement[][] {{MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "F", VersionRange.emptyRange, null, false, false, false)}};
		r1 = createIUPatch("R", Version.create("1.0.0"), null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, ITouchpointType.NONE, NO_TP_DATA, false, null, new IRequirementChange[] {changeB}, scopePP, null, new IRequirement[0]);

		createTestMetdataRepository(new IInstallableUnit[] {a1, a2, b1, b2, c2, f1, p1, r1});

		profile1 = createProfile("TestProfile." + getName());
		planner = createPlanner();
		engine = createEngine();
	}

	public void testOneIUWithMultiplePatchesApplyingOnIt() {
		//				//Confirm that f1 can't be installed
		//				ProfileChangeRequest req1 = new ProfileChangeRequest(profile1);
		//				req1.addInstallableUnits(new IInstallableUnit[] {f1});
		//				ProvisioningPlan plan1 = planner.getProvisioningPlan(req1, null, null);
		//				assertEquals(IStatus.ERROR, plan1.getStatus().getSeverity());
		//		
		//				//Verify that the installation of f1 and p1 succeed
		//				ProfileChangeRequest req2 = new ProfileChangeRequest(profile1);
		//				req2.addInstallableUnits(new IInstallableUnit[] {f1, p1});
		//				ProvisioningPlan plan2 = planner.getProvisioningPlan(req2, null, null);
		//				assertEquals(IStatus.WARNING, plan2.getStatus().getSeverity());
		//				assertInstallOperand(plan2, f1);
		//				assertInstallOperand(plan2, a1);
		//				assertInstallOperand(plan2, b1);
		//				assertInstallOperand(plan2, c1);
		//				assertInstallOperand(plan2, x1);
		//				assertInstallOperand(plan2, y1);
		//				assertInstallOperand(plan2, p1);

		//Verify that the installation of f1 and p1 succeed
		ProfileChangeRequest req3 = new ProfileChangeRequest(profile1);
		req3.addInstallableUnits(new IInstallableUnit[] {f1, p1, r1});
		IProvisioningPlan plan3 = planner.getProvisioningPlan(req3, null, null);
		assertEquals(IStatus.OK, plan3.getStatus().getSeverity());
	}
}
