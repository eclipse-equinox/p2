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

public class PatchTest3 extends AbstractProvisioningTest {
	IInstallableUnit f1;
	IInstallableUnit f2;
	IInstallableUnit a1;
	IInstallableUnit a3;
	IInstallableUnit b1, b2;
	IInstallableUnitPatch p1;

	IProfile profile1;
	IPlanner planner;
	IEngine engine;

	protected void setUp() throws Exception {
		super.setUp();
		f1 = createIU("F", Version.createOSGi(1, 0, 0), new IRequirement[] {MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[1.0.0, 1.1.0]"), null, false, false, true)});
		f2 = createIU("F", Version.createOSGi(2, 0, 0), new IRequirement[] {MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[3.0.0, 4.0.0)"), null, false, false, true)});
		IProvidedCapability[] cap = new IProvidedCapability[] {MetadataFactory.createProvidedCapability("foo", "bar", Version.createOSGi(1, 0, 0))};
		a1 = createIU("A", Version.create("1.0.0"), null, new IRequirement[] {MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.0.0, 1.1.0)"), null, false, false)}, cap, NO_PROPERTIES, ITouchpointType.NONE, NO_TP_DATA, false, null, null);
		a3 = createIU("A", Version.createOSGi(3, 0, 0), true);
		b1 = createIU("B", Version.createOSGi(1, 0, 0), true);
		b2 = createIU("B", Version.createOSGi(2, 0, 0), true);
		IRequirementChange change = MetadataFactory.createRequirementChange(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", VersionRange.emptyRange, null, false, false, false), MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[2.0.0, 2.1.0)"), null, false, false, true));
		IRequirement lifeCycle = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "F", new VersionRange("[1.0.0, 1.1.0)"), null, false, false, false);
		p1 = createIUPatch("P", Version.create("1.0.0"), true, new IRequirementChange[] {change}, new IRequirement[][] {{MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[1.0.0, 1.1.0]"), null, false, false, false)}}, lifeCycle);
		createTestMetdataRepository(new IInstallableUnit[] {a1, b1, b2, p1, a3, f1, f2});

		profile1 = createProfile("TestProfile." + getName());
		planner = createPlanner();
		engine = createEngine();
	}

	public void testCompleteScenario() {
		//	Install a1
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
		assertProfileContainsAll("B1 is missing", profile1, new IInstallableUnit[] {f1});

		//Install p1, this should cause b1 to be uninstalled and b2 to be used instead
		ProfileChangeRequest req2 = new ProfileChangeRequest(profile1);
		req2.addInstallableUnits(new IInstallableUnit[] {p1});
		req2.setInstallableUnitInclusionRules(p1, ProfileInclusionRules.createOptionalInclusionRule(p1));
		IProvisioningPlan plan2 = planner.getProvisioningPlan(req2, null, null);
		assertTrue(IStatus.ERROR != plan2.getStatus().getSeverity());
		assertInstallOperand(plan2, p1);
		assertInstallOperand(plan2, b2);
		engine.perform(plan2, null);
		assertProfileContainsAll("A1 is missing", profile1, new IInstallableUnit[] {a1});
		assertProfileContainsAll("B2 is missing", profile1, new IInstallableUnit[] {b2});
		assertProfileContainsAll("P1 is missing", profile1, new IInstallableUnit[] {p1});
		assertProfileContainsAll("P1 is missing", profile1, new IInstallableUnit[] {f1});

		//Try to uninstall p1, this causes b1 to come back and b2 to go away
		ProfileChangeRequest req3 = new ProfileChangeRequest(profile1);
		req3.removeInstallableUnits(new IInstallableUnit[] {p1});
		IProvisioningPlan plan3 = planner.getProvisioningPlan(req3, null, null);
		assertTrue(IStatus.ERROR != plan3.getStatus().getSeverity());
		assertInstallOperand(plan3, b1);
		assertUninstallOperand(plan3, b2);
		assertUninstallOperand(plan3, p1);

		//Try to uninstall f should not be blocked by p1 since it is installed optionally
		ProfileChangeRequest req4 = new ProfileChangeRequest(profile1);
		req4.removeInstallableUnits(new IInstallableUnit[] {f1});
		IProvisioningPlan plan4 = planner.getProvisioningPlan(req4, null, null);
		assertTrue(IStatus.ERROR != plan4.getStatus().getSeverity());
		assertUninstallOperand(plan4, b2);
		assertUninstallOperand(plan4, a1);
		assertUninstallOperand(plan4, f1);
		assertUninstallOperand(plan4, p1);

		//update from f1 to f2. This should cause p1 to go away
		ProfileChangeRequest req5 = new ProfileChangeRequest(profile1);
		req5.removeInstallableUnits(new IInstallableUnit[] {f1});
		req5.addInstallableUnits(new IInstallableUnit[] {f2});
		IProvisioningPlan plan5 = planner.getProvisioningPlan(req5, null, null);
		assertTrue(IStatus.ERROR != plan5.getStatus().getSeverity());
		assertUninstallOperand(plan5, b2);
		assertUninstallOperand(plan5, a1);
		assertUninstallOperand(plan5, f1);
		assertUninstallOperand(plan5, p1);
		assertInstallOperand(plan5, f2);
		assertInstallOperand(plan5, a3);
		engine.perform(plan5, null);
		assertProfileContainsAll("", profile1, new IInstallableUnit[] {f2, a3});

	}

}
