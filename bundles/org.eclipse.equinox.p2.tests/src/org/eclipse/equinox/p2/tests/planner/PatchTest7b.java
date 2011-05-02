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

public class PatchTest7b extends AbstractProvisioningTest {
	IInstallableUnit a1;
	IInstallableUnit b1;
	IInstallableUnit c1;
	IInstallableUnit x1;
	IInstallableUnit y1;
	IInstallableUnit y2;
	IInstallableUnit f1;

	IInstallableUnitPatch p1;
	IInstallableUnitPatch pp1;

	IProfile profile1;
	IPlanner planner;
	IEngine engine;

	protected void setUp() throws Exception {
		super.setUp();
		x1 = createIU("X", Version.createOSGi(1, 2, 0), true);
		y1 = createIU("Y", Version.createOSGi(1, 0, 0), true);
		y2 = createIU("Y", Version.createOSGi(1, 2, 0), true);
		a1 = createIU("A", Version.create("1.0.0"), new IRequirement[] {MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "X", new VersionRange("[1.0.0, 1.1.0)"), null, false, true)});
		b1 = createIU("B", Version.create("1.0.0"), new IRequirement[] {MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "X", new VersionRange("[1.0.0, 1.1.0)"), null, false, true)});
		c1 = createIU("C", Version.create("1.0.0"), new IRequirement[] {MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "Y", new VersionRange("[1.0.0, 1.1.0)"), null, false, true)});

		IRequirement[] req = new IRequirement[3];
		req[2] = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[1.0.0, 1.1.0)"), null, false, true);
		req[1] = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.0.0, 1.1.0)"), null, false, true);
		req[0] = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "C", new VersionRange("[1.0.0, 1.1.0)"), null, false, true);
		f1 = createIU("F", Version.createOSGi(1, 0, 0), req);

		IRequirementChange changeX = MetadataFactory.createRequirementChange(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "X", VersionRange.emptyRange, null, false, false, false), MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "X", new VersionRange("[1.1.0, 1.3.0)"), null, false, false, true));
		IRequirement[][] scope = new IRequirement[0][0]; //new Requirement[][] {{MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", VersionRange.emptyRange, null, false, false, false)}};
		p1 = createIUPatch("P", Version.create("1.0.0"), null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, ITouchpointType.NONE, NO_TP_DATA, false, null, new IRequirementChange[] {changeX}, scope, null, new IRequirement[0]);

		IRequirementChange changeY = MetadataFactory.createRequirementChange(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "Y", VersionRange.emptyRange, null, false, false, false), MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "Y", new VersionRange("[1.1.0, 1.3.0)"), null, false, false, true));
		IRequirement[][] scopePP = new IRequirement[0][0]; //new Requirement[][] {{MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "C", VersionRange.emptyRange, null, false, false, false)}};
		pp1 = createIUPatch("PP", Version.create("1.0.0"), null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, ITouchpointType.NONE, NO_TP_DATA, false, null, new IRequirementChange[] {changeY}, scopePP, null, new IRequirement[0]);

		createTestMetdataRepository(new IInstallableUnit[] {a1, b1, c1, x1, y1, y2, f1, p1, pp1});
		//		createTestMetdataRepository(new IInstallableUnit[] {c1, y1, y2, f1, pp1});

		profile1 = createProfile("TestProfile." + getName());
		planner = createPlanner();
		engine = createEngine();
	}

	public void testGeneralScope() {
		//Confirm that f1 can't be installed
		//		ProfileChangeRequest req1 = new ProfileChangeRequest(profile1);
		//		req1.addInstallableUnits(new IInstallableUnit[] {f1});
		//		ProvisioningPlan plan1 = planner.getProvisioningPlan(req1, null, null);
		//		assertEquals(IStatus.ERROR, plan1.getStatus().getSeverity());

		//Verify that the installation of f1 and p1 succeed
		ProfileChangeRequest req2 = new ProfileChangeRequest(profile1);
		req2.addInstallableUnits(new IInstallableUnit[] {f1, p1});
		IProvisioningPlan plan2 = planner.getProvisioningPlan(req2, null, null);
		assertEquals(IStatus.OK, plan2.getStatus().getSeverity());
		assertInstallOperand(plan2, f1);
		assertInstallOperand(plan2, a1);
		assertInstallOperand(plan2, b1);
		assertInstallOperand(plan2, c1);
		assertInstallOperand(plan2, x1);
		assertInstallOperand(plan2, y1);
		assertInstallOperand(plan2, p1);

	}
}
