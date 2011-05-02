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

public class PatchTest11 extends AbstractProvisioningTest {
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
		IRequirementChange change = MetadataFactory.createRequirementChange(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", VersionRange.emptyRange, null, false, false, false), MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.1.0, 1.3.0)"), "(foo=bar)", false, false, true));
		p1 = createIUPatch("P", Version.create("1.0.0"), true, new IRequirementChange[] {change}, new IRequirement[][] {{MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", VersionRange.emptyRange, null, false, false)}}, null);

		createTestMetdataRepository(new IInstallableUnit[] {a1, b1, b2, p1});

		profile1 = createProfile("TestProfile." + getName());
		planner = createPlanner();
		engine = createEngine();
	}

	public void testInstallBogusInstallFilterInPatch() {
		//P1 changes the requirement from A on B to be filtered (the filter evaluates to false): A requires B [1.0.0, 1.0.0] becomes when the patch is applied A requires B [1.1.0, 1.3.0) if "foo=bar"
		//The result is a bit counter intuitive but correct. Only A1 and P1 are installed. No be get installed since the dependency on B is filtered out. 
		ProfileChangeRequest req2 = new ProfileChangeRequest(profile1);
		req2.addInstallableUnits(new IInstallableUnit[] {a1, p1});
		req2.setInstallableUnitInclusionRules(p1, ProfileInclusionRules.createOptionalInclusionRule(p1));
		IProvisioningPlan plan2 = planner.getProvisioningPlan(req2, null, null);
		assertTrue(IStatus.ERROR != plan2.getStatus().getSeverity());
		assertNoOperand(plan2, b1);
		assertNoOperand(plan2, b2);
		assertInstallOperand(plan2, a1);
		assertInstallOperand(plan2, p1);
	}
}
