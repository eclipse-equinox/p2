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
import org.eclipse.equinox.p2.planner.ProfileInclusionRules;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class PatchTest4 extends AbstractProvisioningTest {
	IInstallableUnit f1;
	IInstallableUnit a1;
	IInstallableUnit b1;
	IInstallableUnit b2;
	IInstallableUnit c1;
	IInstallableUnit d1;
	IInstallableUnit d2;

	IInstallableUnitPatch p1;
	IInstallableUnitPatch p2;

	IProfile profile1;
	IPlanner planner;
	IEngine engine;

	protected void setUp() throws Exception {
		super.setUp();
		f1 = createIU("F", Version.createOSGi(1, 0, 0), new IRequirement[] {MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", VersionRange.emptyRange, null, false, false, true), MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "C", VersionRange.emptyRange, null, false, false, true)});
		a1 = createIU("A", Version.create("1.0.0"), new IRequirement[] {MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.0.0, 1.1.0)"), null, false, false, true)});
		b1 = createIU("B", Version.createOSGi(1, 0, 0), true);
		c1 = createIU("C", Version.create("1.0.0"), new IRequirement[] {MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "D", new VersionRange("[1.0.0, 1.1.0)"), null, false, false, true)});
		d1 = createIU("D", Version.createOSGi(1, 0, 0), true);
		b2 = createIU("B", Version.createOSGi(2, 0, 0), new IRequirement[] {MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "D", new VersionRange("[2.0.0, 3.1.0)"), null, false, false, true)});
		d2 = createIU("D", Version.createOSGi(2, 0, 0), true);

		IRequirementChange changeA = MetadataFactory.createRequirementChange(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", VersionRange.emptyRange, null, false, false, false), MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[2.0.0, 2.1.0)"), null, false, false, true));
		IRequirementChange changeC = MetadataFactory.createRequirementChange(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "D", VersionRange.emptyRange, null, false, false, false), MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "D", new VersionRange("[2.0.0, 2.1.0)"), null, false, false, true));

		IRequirement lifeCycle = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "F", VersionRange.emptyRange, null, false, false, false);
		IRequirement[][] scope = new IRequirement[][] { {MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[1.0.0, 1.1.0]"), null, false, false, false)}, {MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "C", new VersionRange("[1.0.0, 1.1.0]"), null, false, false, false)}};
		p1 = createIUPatch("P", Version.create("1.0.0"), true, new IRequirementChange[] {changeA, changeC}, scope, lifeCycle);
		p2 = createIUPatch("P", Version.create("2.0.0"), true, new IRequirementChange[] {changeA, changeC}, new IRequirement[0][0], lifeCycle);
		createTestMetdataRepository(new IInstallableUnit[] {f1, a1, b1, b2, c1, d1, d2, p1});

		profile1 = createProfile("TestProfile." + getName());
		planner = createPlanner();
		engine = createEngine();
	}

	public void testCompleteScenario() {
		//	Install f1
		ProfileChangeRequest req1 = new ProfileChangeRequest(profile1);
		req1.addInstallableUnits(new IInstallableUnit[] {f1});
		IProvisioningPlan plan1 = planner.getProvisioningPlan(req1, null, null);
		assertEquals(IStatus.OK, plan1.getStatus().getSeverity());
		assertInstallOperand(plan1, b1);
		assertInstallOperand(plan1, a1);
		assertInstallOperand(plan1, f1);
		engine.perform(plan1, null);
		assertProfileContainsAll("A1 is missing", profile1, new IInstallableUnit[] {a1});
		assertProfileContainsAll("B1 is missing", profile1, new IInstallableUnit[] {b1});
		assertProfileContainsAll("C1 is missing", profile1, new IInstallableUnit[] {c1});
		assertProfileContainsAll("D1 is missing", profile1, new IInstallableUnit[] {d1});

		//Install p1, this will cause C2 and D2 to be installed
		ProfileChangeRequest req2 = new ProfileChangeRequest(profile1);
		req2.addInstallableUnits(new IInstallableUnit[] {p1});
		req2.setInstallableUnitInclusionRules(p1, ProfileInclusionRules.createOptionalInclusionRule(p1));
		IProvisioningPlan plan2 = planner.getProvisioningPlan(req2, null, null);
		assertTrue(IStatus.ERROR != plan2.getStatus().getSeverity());
		assertInstallOperand(plan2, b2);
		assertInstallOperand(plan2, d2);
		engine.perform(plan2, null);
		assertProfileContainsAll("A1 is missing", profile1, new IInstallableUnit[] {a1});
		assertProfileContainsAll("B2 is missing", profile1, new IInstallableUnit[] {b2});
		assertProfileContainsAll("P1 is missing", profile1, new IInstallableUnit[] {p1});
		assertProfileContainsAll("C1 is missing", profile1, new IInstallableUnit[] {c1});
		assertProfileContainsAll("D2 is missing", profile1, new IInstallableUnit[] {d2});
		assertProfileContainsAll("F1 is missing", profile1, new IInstallableUnit[] {f1});
	}

	public void OfftestCompleteScenario2() {
		//This test when no scopes are specified
		//	Install f1
		ProfileChangeRequest req1 = new ProfileChangeRequest(profile1);
		req1.addInstallableUnits(new IInstallableUnit[] {f1});
		IProvisioningPlan plan1 = planner.getProvisioningPlan(req1, null, null);
		assertEquals(IStatus.OK, plan1.getStatus().getSeverity());
		assertInstallOperand(plan1, b1);
		assertInstallOperand(plan1, a1);
		assertInstallOperand(plan1, f1);
		engine.perform(plan1, null);
		assertProfileContainsAll("A1 is missing", profile1, new IInstallableUnit[] {a1});
		assertProfileContainsAll("B1 is missing", profile1, new IInstallableUnit[] {b1});
		assertProfileContainsAll("C1 is missing", profile1, new IInstallableUnit[] {c1});
		assertProfileContainsAll("D1 is missing", profile1, new IInstallableUnit[] {d1});

		//Install p2, this should cause b1 to be uninstalled and b2 to be used instead
		ProfileChangeRequest req2 = new ProfileChangeRequest(profile1);
		req2.addInstallableUnits(new IInstallableUnit[] {p2});
		req2.setInstallableUnitInclusionRules(p1, ProfileInclusionRules.createOptionalInclusionRule(p1));
		IProvisioningPlan plan2 = planner.getProvisioningPlan(req2, null, null);
		assertTrue(IStatus.ERROR != plan2.getStatus().getSeverity());
		assertInstallOperand(plan2, b2);
		assertInstallOperand(plan2, d2);
		engine.perform(plan2, null);
		assertProfileContainsAll("A1 is missing", profile1, new IInstallableUnit[] {a1});
		assertProfileContainsAll("B2 is missing", profile1, new IInstallableUnit[] {b2});
		assertProfileContainsAll("P1 is missing", profile1, new IInstallableUnit[] {p1});
		assertProfileContainsAll("C1 is missing", profile1, new IInstallableUnit[] {c1});
		assertProfileContainsAll("D2 is missing", profile1, new IInstallableUnit[] {d2});
		assertProfileContainsAll("F1 is missing", profile1, new IInstallableUnit[] {f1});
	}
}
