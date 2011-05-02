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

public class PatchTest10 extends AbstractProvisioningTest {
	IInstallableUnit a1;
	IInstallableUnit b1;
	IInstallableUnit b2;
	IInstallableUnit b3;
	IInstallableUnitPatch p1;
	IInstallableUnitPatch pp1;

	IProfile profile1;
	IPlanner planner;
	IEngine engine;

	protected void setUp() throws Exception {
		super.setUp();
		a1 = createIU("A", Version.create("1.0.0"), new IRequirement[] {MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.0.0, 1.1.0)"), null, false, true)});
		b1 = createIU("B", Version.createOSGi(1, 0, 0), true);
		b2 = createIU("B", Version.createOSGi(1, 2, 0), true);
		b3 = createIU("B", Version.createOSGi(1, 3, 0), true);
		IRequirementChange change = MetadataFactory.createRequirementChange(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", VersionRange.emptyRange, null, false, false, false), MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.2.0, 1.2.0]"), null, false, false, true));
		p1 = createIUPatch("P", Version.create("1.0.0"), true, new IRequirementChange[] {change}, new IRequirement[][] {{MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", VersionRange.emptyRange, null, false, false)}}, null);

		IRequirementChange anotherChangeB = MetadataFactory.createRequirementChange(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", VersionRange.emptyRange, null, false, false, false), MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.3.0, 1.3.0]"), null, false, false, true));
		pp1 = createIUPatch("PP", Version.create("1.0.0"), true, new IRequirementChange[] {anotherChangeB}, new IRequirement[][] {{MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", VersionRange.emptyRange, null, false, false)}}, null);

		createTestMetdataRepository(new IInstallableUnit[] {a1, b1, b2, b3, p1, pp1});

		profile1 = createProfile("TestProfile." + getName());
		planner = createPlanner();
		engine = createEngine();
	}

	public void testConflictingPatches() {
		//a1 and p1 can be installed together  
		ProfileChangeRequest req1 = new ProfileChangeRequest(profile1);
		req1.addInstallableUnits(new IInstallableUnit[] {a1, p1});
		IProvisioningPlan plan1 = planner.getProvisioningPlan(req1, null, null);
		assertEquals(IStatus.OK, plan1.getStatus().getSeverity());
		assertInstallOperand(plan1, a1);
		assertInstallOperand(plan1, p1);
		assertInstallOperand(plan1, b2);

		//a1 and pp1 can be installed together
		ProfileChangeRequest req2 = new ProfileChangeRequest(profile1);
		req2.addInstallableUnits(new IInstallableUnit[] {a1, pp1});
		IProvisioningPlan plan2 = planner.getProvisioningPlan(req2, null, null);
		assertEquals(IStatus.OK, plan2.getStatus().getSeverity());
		assertInstallOperand(plan2, a1);
		assertInstallOperand(plan2, pp1);
		assertInstallOperand(plan2, b3);

		//a1, p1, and pp1 can not be installed together
		ProfileChangeRequest req3 = new ProfileChangeRequest(profile1);
		req3.addInstallableUnits(new IInstallableUnit[] {a1, p1, pp1});
		IProvisioningPlan plan3 = planner.getProvisioningPlan(req3, null, null);
		assertEquals(IStatus.ERROR, plan3.getStatus().getSeverity());
	}

	public void testExplanation() {
		ProfileChangeRequest req3 = new ProfileChangeRequest(profile1);
		req3.addInstallableUnits(new IInstallableUnit[] {a1, p1, pp1});
		IProvisioningPlan plan3 = planner.getProvisioningPlan(req3, null, null);
		assertEquals(IStatus.ERROR, plan3.getStatus().getSeverity());
		// Set problems = plan3.getRequestStatus().getConflictsWithInstalledRoots();
		// System.out.println(problems);
		// System.out.println(plan3.getRequestStatus().getExplanations());
		//		assertEquals(3, plan3.getRequestStatus().getExplanations().size());
		//		assertTrue(plan3.getRequestStatus().getExplanations().contains(a1));
		//		assertTrue(plan3.getRequestStatus().getExplanations().contains(p1));
		//		assertTrue(plan3.getRequestStatus().getExplanations().contains(pp1));
	}
}
